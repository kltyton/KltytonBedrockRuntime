package com.kltyton.bedrockruntime.client.particle.api;

import com.kltyton.bedrockruntime.client.particle.definition.molang.VariableTable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class AddDefaultVariableEvent {
    @FunctionalInterface
    public interface Callback {
        void onAddDefaultVariables(AddDefaultVariableEvent event);
    }

    /** Lightweight loader-neutral invoker used by the entity/block-entity mixins. */
    public static final Event EVENT = new Event();

    private final VariableTable table;

    public AddDefaultVariableEvent(VariableTable vt) {
        this.table = vt;
    }

    public VariableTable getTable() {
        return table;
    }

    public static final class Event {
        private final List<Callback> listeners = new CopyOnWriteArrayList<>();

        private Event() {
        }

        public Callback invoker() {
            return event -> {
                for (Callback listener : listeners) {
                    listener.onAddDefaultVariables(event);
                }
            };
        }

        public void register(Callback listener) {
            listeners.add(listener);
        }

        public void unregister(Callback listener) {
            listeners.remove(listener);
        }
    }

    public static class Entity extends AddDefaultVariableEvent {
        private final net.minecraft.world.entity.Entity entity;

        public Entity(VariableTable vt, net.minecraft.world.entity.Entity entity) {
            super(vt);
            this.entity = entity;
        }

        public net.minecraft.world.entity.Entity getEntity() {
            return entity;
        }
    }

    public static class BlockEntity extends AddDefaultVariableEvent {
        private final net.minecraft.world.level.block.entity.BlockEntity entity;

        public BlockEntity(VariableTable vt, net.minecraft.world.level.block.entity.BlockEntity entity) {
            super(vt);
            this.entity = entity;
        }

        public net.minecraft.world.level.block.entity.BlockEntity getEntity() {
            return entity;
        }
    }
}
