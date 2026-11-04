package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.query;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EquipmentSlot;
import org.jetbrains.annotations.Nullable;
import com.kltyton.bedrockruntime.client.particle.api.MolangInstance;
import com.kltyton.bedrockruntime.particle.BedrockParticles;
import com.kltyton.bedrockruntime.client.particle.definition.molang.BedrockItemIdentifiers;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.MathValue;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value.ActorAccess;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.value.MolangValueEquality;
import com.kltyton.bedrockruntime.client.particle.definition.molang.compiler.function.MathFunction;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Resolves item-name queries against available Java entity and local-player item stacks. */
public final class ItemNameQuery extends MathFunction {
    public enum Kind { IS_NAME_ANY, GET_EQUIPPED_NAME }
    private static final Set<String> REPORTED_UNAVAILABLE = ConcurrentHashMap.newKeySet();

    private final Kind kind;
    private final boolean bedrockItemNames;
    private final MathValue[] arguments;

    public ItemNameQuery(Kind kind, MathValue... arguments) {
        this(kind, false, arguments);
    }

    @org.jetbrains.annotations.ApiStatus.Internal
    public ItemNameQuery(Kind kind, boolean bedrockItemNames, MathValue... arguments) {
        super(arguments);
        this.kind = kind;
        this.bedrockItemNames = bedrockItemNames;
        this.arguments = arguments.clone();
    }

    @Override
    public Object getRaw(MolangInstance instance) {
        if (kind == Kind.GET_EQUIPPED_NAME) {
            if (arguments.length == 2 && arguments[1].get(instance) != 0.0
                    && ActorAccess.hasEmptyBaseRenderContext(instance)) return MolangValueEquality.defaultIdentifier();
            if (instance.getAttachedEntity() == null) return MolangValueEquality.defaultIdentifier();
            MathValue hand = arguments.length == 0 ? null : arguments[0];
            ItemStack stack = resolveHandStack(instance, hand);
            return stack == null ? MolangValueEquality.defaultIdentifier() : itemPath(stack);
        }
        return get(instance);
    }

    @Override
    public double compute(MolangInstance instance) {
        if (kind == Kind.GET_EQUIPPED_NAME) {
            getRaw(instance);
            return 0.0;
        }

        if (arguments.length < 2) {
            reportInvalid("item name query requires a slot and at least one name");
            return 0.0;
        }
        if (instance.getAttachedEntity() == null) {
            reportUnavailable("entity");
            return 0.0;
        }
        String slotName = arguments[0].getString(instance);
        Slot slot = resolveSlot(slotName);
        if (slot == null) {
            reportUnavailable(String.valueOf(slotName));
            return 0.0;
        }
        int index = -1;
        int candidateStart = 1;
        Object second = arguments[1].getRaw(instance);
        if (second instanceof Number number) {
            float numeric = number.floatValue();
            index = numeric >= 0x1.0p31F || numeric < -0x1.0p31F || Float.isNaN(numeric)
                    ? Integer.MIN_VALUE : (int) numeric;
            candidateStart = 2;
        } else if (!(second instanceof String)) {
            reportInvalid("item name must be a string");
            return 0.0;
        }
        if (candidateStart >= arguments.length) {
            return 0.0;
        }
        ItemStack stack = resolveSlotStack(instance, slot, index);
        if (stack == null) {
            reportUnavailable(slotName);
            return 0.0;
        }
        var identifier = stack.isEmpty() ? null : BuiltInRegistries.ITEM.getKey(stack.getItem());
        String javaName = identifier == null ? "" : identifier.toString();
        String bedrockName = identifier == null ? "" : (bedrockItemNames
                ? BedrockItemIdentifiers.bedrockPackName(identifier) : BedrockItemIdentifiers.bedrockName(identifier)).toString();
        boolean acceptJavaName = !bedrockItemNames || identifier == null
                || !BedrockItemIdentifiers.hasAmbiguousBedrockMeaning(identifier);
        for (int i = candidateStart; i < arguments.length; i++) {
            String candidate = i == 1 ? (String) second : arguments[i].getString(instance);
            if (candidate == null) {
                reportInvalid("item name must be a string");
                return 0.0;
            }
            if (MolangValueEquality.sameString(bedrockName, candidate)
                    || acceptJavaName && MolangValueEquality.sameString(javaName, candidate)) return 1.0;
        }
        return 0.0;
    }

    private String itemPath(ItemStack stack) {
        if (stack.isEmpty()) return "";
        var identifier = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return bedrockItemNames ? BedrockItemIdentifiers.bedrockEquippedItemPath(identifier)
                : BedrockItemIdentifiers.equippedItemPath(identifier);
    }

    private static void reportUnavailable(String slot) {
        if (REPORTED_UNAVAILABLE.add(slot)) {
            BedrockParticles.LOGGER.warn("No client item data available for Molang slot {}", slot);
        }
    }

    private static void reportInvalid(String reason) {
        if (REPORTED_UNAVAILABLE.add("argument:" + reason)) {
            BedrockParticles.LOGGER.warn("Invalid Molang item query argument: {}", reason);
        }
    }

    private static @Nullable ItemStack resolveHandStack(MolangInstance instance, @Nullable MathValue handArgument) {
        EquipmentSlot slot = nameHandSlot(handArgument, instance);
        if (slot == null) return null;
        if (!(instance.getAttachedEntity() instanceof LivingEntity entity)) return ItemStack.EMPTY;
        return entity.getItemBySlot(slot);
    }

    private static @Nullable EquipmentSlot nameHandSlot(@Nullable MathValue argument, MolangInstance instance) {
        if (argument == null) return EquipmentSlot.MAINHAND;
        Object value = argument.getRaw(instance);
        if (value instanceof Number number) {
            float numeric = number.floatValue();
            if (numeric > -1.0F && numeric < 1.0F) return EquipmentSlot.MAINHAND;
            if (numeric >= 1.0F && numeric < 2.0F) return EquipmentSlot.OFFHAND;
        } else if (value instanceof String name) {
            if (MolangValueEquality.sameString(name, "main_hand")) return EquipmentSlot.MAINHAND;
            if (MolangValueEquality.sameString(name, "off_hand")) return EquipmentSlot.OFFHAND;
        }
        reportInvalid("hand slot " + value);
        return null;
    }

    private static Slot resolveSlot(String name) {
        if (name == null) return null;
        return switch (name) {
            case "slot.weapon.mainhand" -> Slot.MAIN_HAND;
            case "slot.weapon.offhand" -> Slot.OFF_HAND;
            case "slot.armor.head" -> Slot.HEAD;
            case "slot.armor.chest" -> Slot.CHEST;
            case "slot.armor.legs" -> Slot.LEGS;
            case "slot.armor.feet" -> Slot.FEET;
            case "slot.armor.body" -> Slot.BODY;
            case "slot.hotbar" -> Slot.HOTBAR;
            case "slot.inventory" -> Slot.INVENTORY;
            default -> null;
        };
    }

    private static ItemStack resolveSlotStack(MolangInstance instance, Slot slot, int index) {
        if (slot == Slot.MAIN_HAND || slot == Slot.OFF_HAND || slot == Slot.HEAD
                || slot == Slot.CHEST || slot == Slot.LEGS || slot == Slot.FEET || slot == Slot.BODY) {
            if (!(instance.getAttachedEntity() instanceof LivingEntity entity)) return null;
            return switch (slot) {
                case MAIN_HAND -> entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND);
                case OFF_HAND -> entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND);
                case HEAD -> entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
                case CHEST -> entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
                case LEGS -> entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS);
                case FEET -> entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET);
                case BODY -> entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.BODY);
                default -> null;
            };
        }

        if (slot == Slot.HOTBAR && (index < 0 || index >= Inventory.SELECTION_SIZE)
                || slot == Slot.INVENTORY && index < 0) return ItemStack.EMPTY;
        if (slot == Slot.HOTBAR && !(instance.getAttachedEntity() instanceof Player)) return ItemStack.EMPTY;
        if (!(instance.getAttachedEntity() instanceof LocalPlayer player)) return null;
        Inventory inventory = player.getInventory();
        return switch (slot) {
            case HOTBAR -> inventory.getItem(index);
            case INVENTORY -> index < Inventory.INVENTORY_SIZE - Inventory.SELECTION_SIZE
                    ? inventory.getItem(index + Inventory.SELECTION_SIZE) : ItemStack.EMPTY;
            default -> null;
        };
    }

    private enum Slot { MAIN_HAND, OFF_HAND, HEAD, CHEST, LEGS, FEET, BODY, HOTBAR, INVENTORY }

    @Override public String getName() {
        return kind == Kind.IS_NAME_ANY ? "query.is_item_name_any" : "query.get_equipped_item_name";
    }

    @Override public int getMinArgs() { return 0; }
    @Override public MathValue[] getArgs() { return arguments.clone(); }
    @Override public boolean isMutable(MathValue... values) { return true; }

}
