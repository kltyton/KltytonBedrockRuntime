package com.kltyton.bedrockruntime.client.particle.molang;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import com.kltyton.bedrockruntime.client.particle.access.IItemUseTimer;
import com.kltyton.bedrockruntime.client.particle.access.IMolangEntityState;
import com.kltyton.bedrockruntime.particle.network.MolangEntityStatePacketS2C;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;

/** Applies initial entity query state within the packet bundle that contains its spawn. */
public final class MolangEntityStateInbox {
    private static final ThreadLocal<Deque<Scope>> SCOPES = new ThreadLocal<>();

    private MolangEntityStateInbox() {}

    public static Scope enterBundle() {
        Deque<Scope> scopes = SCOPES.get();
        if (scopes == null) {
            scopes = new ArrayDeque<>();
            SCOPES.set(scopes);
        }
        Scope scope = new Scope();
        scopes.push(scope);
        return scope;
    }

    public static void receive(ClientLevel level, MolangEntityStatePacketS2C packet) {
        if ((packet.flags() & MolangEntityStatePacketS2C.INITIAL) != 0) {
            Deque<Scope> scopes = SCOPES.get();
            if (scopes != null && !scopes.isEmpty()) {
                scopes.peek().pending.computeIfAbsent(level, ignored -> new HashMap<>()).put(packet.entityId(), packet);
            }
            return;
        }
        Entity entity = level.getEntity(packet.entityId());
        if (entity != null && entity.getUUID().equals(packet.entityUuid())) {
            apply(entity, packet);
        }
    }

    public static void applyInitial(ClientLevel level, Entity entity) {
        MolangEntityStatePacketS2C packet = removeInitial(level, entity.getId(), entity.getUUID());
        if (packet != null) apply(entity, packet);
    }

    private static void apply(Entity entity, MolangEntityStatePacketS2C packet) {
        ((IMolangEntityState) entity).kltyton_bedrock_runtime$setMolangEntityState(packet.flags(), packet.recoveryTarget().orElse(null));
        if (entity instanceof IItemUseTimer timer) {
            timer.kltyton_bedrock_runtime$setRemoteItemUseState(
                    (packet.flags() & MolangEntityStatePacketS2C.ITEM_USE_ACTIVE) != 0, packet.itemUseRemaining());
        }
    }

    public static void forgetInitial(ClientLevel level, int entityId, UUID entityUuid) {
        removeInitial(level, entityId, entityUuid);
    }

    private static MolangEntityStatePacketS2C removeInitial(ClientLevel level, int entityId, UUID entityUuid) {
        Deque<Scope> scopes = SCOPES.get();
        if (scopes == null) return null;
        for (Scope scope : scopes) {
            Map<Integer, MolangEntityStatePacketS2C> pending = scope.pending.get(level);
            if (pending == null) continue;
            MolangEntityStatePacketS2C packet = pending.get(entityId);
            if (packet != null && packet.entityUuid().equals(entityUuid)) {
                pending.remove(entityId);
                return packet;
            }
        }
        return null;
    }

    public static final class Scope implements AutoCloseable {
        private final Map<ClientLevel, Map<Integer, MolangEntityStatePacketS2C>> pending = new IdentityHashMap<>();

        private Scope() {}

        @Override
        public void close() {
            Deque<Scope> scopes = SCOPES.get();
            if (scopes == null || scopes.peek() != this) {
                throw new IllegalStateException("Entity state bundles must close in stack order");
            }
            scopes.pop();
            if (scopes.isEmpty()) SCOPES.remove();
        }
    }
}
