package com.kltyton.bedrockruntime.client.particle.runtime;

import com.kltyton.bedrockruntime.client.particle.api.event.ParticleEvents;

import com.google.common.collect.Iterables;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.client.particle.api.IEventNode;
import com.kltyton.bedrockruntime.client.particle.api.IMolangParticleInstance;
import com.kltyton.bedrockruntime.client.particle.api.IParticleComponent;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.api.ParticlePresetLoadedEvent;
import com.kltyton.bedrockruntime.client.particle.definition.DefinedParticleEffect;
import com.kltyton.bedrockruntime.client.particle.definition.component.*;
import com.kltyton.bedrockruntime.client.particle.definition.description.DescriptionMaterial;
import com.kltyton.bedrockruntime.client.particle.definition.curve.ParticleCurve;
import com.kltyton.bedrockruntime.client.particle.definition.event.NodeMolangExp;
import com.kltyton.bedrockruntime.client.particle.definition.event.EventResolver;
import com.kltyton.bedrockruntime.client.particle.definition.molang.FloatMolangExp;
import com.kltyton.bedrockruntime.client.particle.definition.molang.MolangExp;
import com.kltyton.bedrockruntime.client.particle.definition.molang.VariableTable;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MolangParser;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value.Variable;
import com.kltyton.bedrockruntime.client.particle.runtime.render.BedrockParticleRenderTypes;

import java.util.*;

import static com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MolangQueries.applyPrefixAliases;

public class ParticlePreset {
    public DefinedParticleEffect effect;
    public ParticleRenderType renderType;
    /// Optional native group supplied through ParticlePresetLoadedEvent for custom particle rendering.
    public @Nullable ParticleRenderType customRenderGroup;
    public FaceCameraMode facingCameraMode;
    public float minSpeedThresholdSqr;
    public boolean environmentLighting;
    public ParticleLifeTimeEvents lifeTimeEvents;
    public List<ParticleMotionCollision.Event> collisionEvents = List.of();
    public float invTextureWidth;
    public float invTextureHeight;
    public boolean motionDynamic;

    public VariableTable vars;
    public @Nullable ParticleInitialization initialization;

    /// For custom preset data
    protected Map<Class<?>, Object> tickets;

    public ParticlePreset(DefinedParticleEffect effect) {
        this.effect = effect;
        DescriptionMaterial material = effect.description.parameters().material();
        this.customRenderGroup = material.getRenderGroup();
        this.renderType = BedrockParticleRenderTypes.forMaterial(material, effect.usesTerrainAtlas());
        if (effect.components.get(ParticleAppearanceBillboard.ID) instanceof ParticleAppearanceBillboard component) {
            this.facingCameraMode = FaceCameraMode.fromComponent(component.faceCameraMode());
            this.minSpeedThresholdSqr = component.direction() == ParticleAppearanceBillboard.Direction.DEFAULT
                    ? 0.01F : Mth.square(component.direction().minSpeedThreshold());
            this.invTextureWidth = 1.0F / component.uv().texturewidth();
            this.invTextureHeight = 1.0F / component.uv().textureheight();
        } else {
            this.facingCameraMode = FaceCameraMode.DO_NOTHING;
            this.minSpeedThresholdSqr = 0;
            this.invTextureWidth = 1;
            this.invTextureHeight = 1;
        }
        this.environmentLighting = effect.components.containsValue(ParticleAppearanceLighting.INSTANCE);
        this.lifeTimeEvents = (ParticleLifeTimeEvents) effect.components.get(ParticleLifeTimeEvents.ID);
        ParticleMotionCollision motionCollision = (ParticleMotionCollision) effect.components.get(ParticleMotionCollision.ID);
        this.motionDynamic = effect.components.containsKey(ParticleMotionDynamic.ID);

        VariableTable table = new VariableTable(addDefaultVariables(), null);
        MolangParser parser = new MolangParser(table, effect.molangVersion(), effect.usesBedrockItemNames());
        if (motionCollision != null) {
            this.collisionEvents = motionCollision.events();
            for (ParticleMotionCollision.Event event : collisionEvents) {
                Map<String, IEventNode> nodes = EventResolver.resolve(effect.events, event.event());
                for (IEventNode node : nodes.values()) {
                    if (node instanceof NodeMolangExp exp) {
                        exp.compile(parser);
                    }
                }
            }
        }
        for (Map.Entry<String, ParticleCurve> entry : effect.orderedCurves) {
            String curveKey = entry.getKey();
            ParticleCurve curve = entry.getValue();
            curve.input.compile(parser);
            curve.horizontalRange.compile(parser);
            curve.nodes.either.ifRight(exps -> {
                for (FloatMolangExp exp : exps) {
                    exp.compile(parser);
                }
            });
            String name = applyPrefixAliases(entry.getKey().toLowerCase(java.util.Locale.ROOT), "variable.", "v.");
            table.table.put(name, new Variable(name, 0.0));
        }

        for (IParticleComponent component : Iterables.concat(effect.orderedParticleEarlyComponents, effect.orderedParticleComponents)) {
            assert component != null;
            for (MolangExp exp : component.getAllMolangExp()) {
                exp.compile(parser);
            }
        }
        this.vars = table;
        this.initialization = (ParticleInitialization) effect.components.get(ParticleInitialization.ID);
        ParticleEvents.post(new ParticlePresetLoadedEvent(this));
    }

    public void prepareVariables(IMolangParticleInstance instance) {
        VariableTable table = instance.getVars();
        table.setValue("variable.particle_lifetime", instance.tickLifetime());
        table.setValue("variable.particle_age", instance.tickAge());
        table.setValue("variable.particle_random_1", instance.getRandom1());
        table.setValue("variable.particle_random_2", instance.getRandom2());
        table.setValue("variable.particle_random_3", instance.getRandom3());
        table.setValue("variable.particle_random_4", instance.getRandom4());
        for (Map.Entry<String, ParticleCurve> entry : effect.orderedCurves) {
            String name = applyPrefixAliases(entry.getKey().toLowerCase(java.util.Locale.ROOT), "variable.", "v.");
            entry.getValue().evaluate(instance, name);
        }
    }

    public <T> void setTicket(Class<T> clazz, T value) {
        if (tickets == null) this.tickets = new HashMap<>();
        tickets.put(clazz, value);
    }

    @SuppressWarnings("unchecked")
    public <T> @Nullable T getTicket(Class<T> clazz) {
        return tickets == null ? null : (T) tickets.get(clazz);
    }

    private static Hashtable<String, Variable> addDefaultVariables() {
        Hashtable<String, Variable> table = new Hashtable<>();
        table.computeIfAbsent("variable.particle_age", s -> new Variable(s, MolangInstance::tickAge));
        table.computeIfAbsent("variable.particle_lifetime", s -> new Variable(s, MolangInstance::tickLifetime));
        table.computeIfAbsent("variable.particle_random_1", s -> new Variable(s, MolangInstance::getRandom1));
        table.computeIfAbsent("variable.particle_random_2", s -> new Variable(s, MolangInstance::getRandom2));
        table.computeIfAbsent("variable.particle_random_3", s -> new Variable(s, MolangInstance::getRandom3));
        table.computeIfAbsent("variable.particle_random_4", s -> new Variable(s, MolangInstance::getRandom4));
        return table;
    }
}
