package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.query;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.MathFunction;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value.ContextValue;
import com.kltyton.bedrockruntime.client.particle.runtime.attachment.WithBlockParticleEmitter;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ItemBlockTagQuery extends MathFunction {
    public enum Kind {
        ALL("query.all_tags", true, false),
        ANY("query.any_tag", false, false),
        EQUIPPED_ALL("query.equipped_item_all_tags", true, true),
        EQUIPPED_ANY("query.equipped_item_any_tag", false, true);

        private final String name;
        private final boolean all;
        private final boolean equipped;

        Kind(String name, boolean all, boolean equipped) {
            this.name = name;
            this.all = all;
            this.equipped = equipped;
        }
    }

    private static final Map<String, TagKey<Item>> SULFUR_TAGS = Map.ofEntries(
            Map.entry("sulfur_cube_archetype_bouncy", TagKey.create(Registries.ITEM, Identifier.withDefaultNamespace("sulfur_cube_archetype/bouncy"))),
            Map.entry("sulfur_cube_archetype_regular", TagKey.create(Registries.ITEM, Identifier.withDefaultNamespace("sulfur_cube_archetype/regular"))),
            Map.entry("sulfur_cube_archetype_slow_flat", TagKey.create(Registries.ITEM, Identifier.withDefaultNamespace("sulfur_cube_archetype/slow_flat"))),
            Map.entry("sulfur_cube_archetype_fast_flat", TagKey.create(Registries.ITEM, Identifier.withDefaultNamespace("sulfur_cube_archetype/fast_flat"))),
            Map.entry("sulfur_cube_archetype_light", TagKey.create(Registries.ITEM, Identifier.withDefaultNamespace("sulfur_cube_archetype/light"))),
            Map.entry("sulfur_cube_archetype_fast_sliding", TagKey.create(Registries.ITEM, Identifier.withDefaultNamespace("sulfur_cube_archetype/fast_sliding"))),
            Map.entry("sulfur_cube_archetype_slow_sliding", TagKey.create(Registries.ITEM, Identifier.withDefaultNamespace("sulfur_cube_archetype/slow_sliding"))),
            Map.entry("sulfur_cube_archetype_sticky", TagKey.create(Registries.ITEM, Identifier.withDefaultNamespace("sulfur_cube_archetype/sticky"))),
            Map.entry("sulfur_cube_archetype_high_resistance", TagKey.create(Registries.ITEM, Identifier.withDefaultNamespace("sulfur_cube_archetype/high_resistance"))),
            Map.entry("sulfur_cube_archetype_explosive", TagKey.create(Registries.ITEM, Identifier.withDefaultNamespace("sulfur_cube_archetype/explosive"))),
            Map.entry("sulfur_cube_archetype_hot", TagKey.create(Registries.ITEM, Identifier.withDefaultNamespace("sulfur_cube_archetype/hot")))
    );
    private static final ContextValue RENDERED_ITEM = new ContextValue("context.rendered_item");
    private static final Set<String> REPORTED_INVALID_TAGS = ConcurrentHashMap.newKeySet();
    private final Kind kind;
    private final MathValue[] arguments;

    public ItemBlockTagQuery(Kind kind, MathValue... arguments) {
        super(arguments);
        this.kind = kind;
        this.arguments = arguments.clone();
    }

    @Override
    public double compute(MolangInstance instance) {
        ItemStack item;
        BlockState block = null;
        int firstTag = 0;
        if (kind.equipped) {
            var actor = instance.getAttachedEntity();
            if (actor == null) return 0.0;
            if (arguments.length < 2) return contentError("requires an equipment slot and at least one tag");
            EquipmentSlot slot = equipmentSlot(arguments[0].getString(instance));
            if (slot == null) return contentError("requires a valid equipment slot");
            item = actor instanceof LivingEntity entity ? entity.getItemBySlot(slot) : ItemStack.EMPTY;
            if (item.isEmpty()) return kind.all ? 1.0 : 0.0;
            firstTag = 1;
        } else {
            if (arguments.length == 0) return contentError("requires at least one tag");
            item = instance.getReferencedItem();
            if (item == null) {
                Object value = RENDERED_ITEM.resolveProvided(instance);
                if (value instanceof ItemStack stack) item = stack;
                block = attachedBlock(instance);
            }
            if (item == null && block == null) return kind.all ? 1.0 : 0.0;
        }

        for (int i = firstTag; i < arguments.length; i++) {
            String name = arguments[i].getString(instance);
            Identifier id = name == null ? null : Identifier.tryParse(name);
            if (name == null && REPORTED_INVALID_TAGS.add(kind.name)) {
                BedrockParticles.LOGGER.warn("Molang {} requires string tag names", kind.name);
            }
            boolean itemMatches = item == null || id != null && matchesItemTag(item, id);
            boolean blockMatches = block == null || id != null && block.is(TagKey.create(Registries.BLOCK, id));
            boolean matches = kind.all ? itemMatches && blockMatches
                    : item != null && itemMatches || block != null && blockMatches;
            if (kind.all && !matches) return 0.0;
            if (!kind.all && matches) return 1.0;
        }
        return kind.all ? 1.0 : 0.0;
    }

    private static boolean matchesItemTag(ItemStack item, Identifier id) {
        if (item.is(TagKey.create(Registries.ITEM, id))) return true;
        if (!id.getNamespace().equals("minecraft")) return false;
        return switch (id.getPath()) {
            case "is_pickaxe" -> item.is(ItemTags.PICKAXES);
            case "is_axe" -> item.is(ItemTags.AXES);
            case "is_hoe" -> item.is(ItemTags.HOES);
            case "is_shovel" -> item.is(ItemTags.SHOVELS);
            case "is_sword" -> item.is(ItemTags.SWORDS) || item.is(Items.MACE);
            case "digger" -> isDigger(item);
            case "is_tool" -> isDigger(item) || item.is(ItemTags.SWORDS) || item.is(Items.MACE);
            case "is_spear" -> item.is(ItemTags.SPEARS);
            case "is_shears" -> item.is(Items.SHEARS);
            case "is_trident" -> item.is(Items.TRIDENT);
            case "arrow" -> item.is(Items.ARROW);
            case "boat" -> item.is(ItemTags.BOATS);
            case "door" -> item.is(ItemTags.DOORS);
            case "sign" -> item.is(ItemTags.SIGNS) || item.is(ItemTags.HANGING_SIGNS);
            case "hanging_sign" -> item.is(ItemTags.HANGING_SIGNS);
            case "is_armor" -> item.is(ItemTags.HEAD_ARMOR) || item.is(ItemTags.CHEST_ARMOR)
                    || item.is(ItemTags.LEG_ARMOR) || item.is(ItemTags.FOOT_ARMOR) || item.is(Items.ELYTRA);
            case "egg" -> item.is(ItemTags.EGGS);
            case "harness" -> item.is(ItemTags.HARNESSES);
            case "is_fish" -> item.is(ItemTags.FISHES);
            case "is_meat" -> item.is(ItemTags.MEAT) || item.is(Items.RABBIT_STEW);
            case "decorated_pot_sherds" -> item.is(ItemTags.DECORATED_POT_INGREDIENTS);
            case "trimmable_armors" -> item.is(ItemTags.TRIMMABLE_ARMOR);
            case "sulfur_cube_archetype_bouncy", "sulfur_cube_archetype_regular",
                    "sulfur_cube_archetype_slow_flat", "sulfur_cube_archetype_fast_flat",
                    "sulfur_cube_archetype_light", "sulfur_cube_archetype_fast_sliding",
                    "sulfur_cube_archetype_slow_sliding", "sulfur_cube_archetype_sticky",
                    "sulfur_cube_archetype_high_resistance", "sulfur_cube_archetype_explosive",
                    "sulfur_cube_archetype_hot" -> item.is(SULFUR_TAGS.get(id.getPath()));
            case "wooden_tier", "stone_tier", "copper_tier", "iron_tier", "golden_tier",
                    "diamond_tier", "netherite_tier", "chainmail_tier", "leather_tier" ->
                    matchesVanillaTier(item, id.getPath());
            default -> false;
        };
    }

    private static boolean matchesVanillaTier(ItemStack item, String tag) {
        if (tag.equals("diamond_tier") && item.is(Items.MACE)) return true;
        Identifier itemId = BuiltInRegistries.ITEM.getKey(item.getItem());
        if (!itemId.getNamespace().equals("minecraft")) return false;
        String tier = tag.substring(0, tag.length() - "_tier".length());
        String prefix = tier + "_";
        if (!itemId.getPath().startsWith(prefix)) return false;
        String kind = itemId.getPath().substring(prefix.length());
        return switch (kind) {
            case "axe", "hoe", "pickaxe", "shovel", "spear", "sword" ->
                    !tier.equals("chainmail") && !tier.equals("leather");
            case "helmet", "chestplate", "leggings", "boots" ->
                    !tier.equals("wooden") && !tier.equals("stone");
            case "horse_armor", "nautilus_armor" -> tier.equals("netherite");
            default -> false;
        };
    }

    private static boolean isDigger(ItemStack item) {
        return item.is(ItemTags.PICKAXES) || item.is(ItemTags.AXES)
                || item.is(ItemTags.HOES) || item.is(ItemTags.SHOVELS);
    }

    private double contentError(String reason) {
        if (REPORTED_INVALID_TAGS.add(kind.name + ':' + reason)) {
            BedrockParticles.LOGGER.warn("Molang {} {}", kind.name, reason);
        }
        return 0.0;
    }

    private static @Nullable EquipmentSlot equipmentSlot(@Nullable String name) {
        if (name == null) return null;
        return switch (name) {
            case "slot.weapon.mainhand" -> EquipmentSlot.MAINHAND;
            case "slot.weapon.offhand" -> EquipmentSlot.OFFHAND;
            case "slot.armor.head" -> EquipmentSlot.HEAD;
            case "slot.armor.chest" -> EquipmentSlot.CHEST;
            case "slot.armor.legs" -> EquipmentSlot.LEGS;
            case "slot.armor.feet" -> EquipmentSlot.FEET;
            case "slot.armor.body" -> EquipmentSlot.BODY;
            default -> null;
        };
    }

    private static @Nullable BlockState attachedBlock(MolangInstance instance) {
        var emitter = instance.getEmitter();
        if (emitter == null) return null;
        var blockEntity = emitter.getAttachedBlock();
        if (blockEntity != null && !blockEntity.isRemoved()) return blockEntity.getBlockState();
        if (emitter instanceof WithBlockParticleEmitter blockEmitter) {
            WithBlockParticleEmitter.BlockData data = blockEmitter.getBlockData();
            if (data != null) {
                BlockPos position = data.pos();
                if (instance.getLevel().isLoaded(position)) return instance.getLevel().getBlockState(position);
            }
        }
        return null;
    }

    @Override public String getName() { return kind == null ? "query.all_tags" : kind.name; }
    @Override public int getMinArgs() { return 0; }
    @Override public MathValue[] getArgs() { return arguments.clone(); }
    @Override public boolean isMutable(MathValue... values) { return true; }
}
