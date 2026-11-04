package com.kltyton.bedrockruntime.api.geckolib.entity;

import com.kltyton.bedrockruntime.api.geckolib.state.GeoObbAnimationState;
import com.kltyton.bedrockruntime.api.state.runtime.ObbEntityState;

import java.util.WeakHashMap;

/**
 * CN: Geo 动画状态和临时攻击窗口的弱引用运行时表；骨骼属性本身由 ObbEntityState 管理。
 * EN: Weak runtime table for Geo animation state and temporary attack windows; ObbEntityState owns bone attributes.
 */
final class GeoBoneHitboxState {
    private static final Object LOCK = new Object();
    private static final WeakHashMap<GeoBoneHitboxEntity, State> STATES = new WeakHashMap<>();

    private GeoBoneHitboxState() {
    }

    static void bindTemporaryWindow(GeoBoneHitboxEntity entity, GeoObbAnimationState animationState) {
        synchronized (LOCK) {
            State state = state(entity);
            GeoObbAnimationState requested = normalize(animationState);
            state.temporaryAnimationState = requested.active() ? requested : state.currentAnimationState;
        }
    }

    static void clearTemporaryWindow(GeoBoneHitboxEntity entity) {
        synchronized (LOCK) {
            state(entity).temporaryAnimationState = GeoObbAnimationState.NONE;
        }
    }

    static GeoObbAnimationState animationState(GeoBoneHitboxEntity entity) {
        synchronized (LOCK) {
            return state(entity).currentAnimationState;
        }
    }

    /** CN: 临时攻击窗口结束时返回 true。EN: Returns true when the temporary attack window has ended. */
    static boolean updateAnimationState(GeoBoneHitboxEntity entity, GeoObbAnimationState animationState) {
        return updateAnimationStates(entity, java.util.List.of(normalize(animationState)));
    }

    static boolean updateAnimationStates(GeoBoneHitboxEntity entity, java.util.List<GeoObbAnimationState> animationStates) {
        synchronized (LOCK) {
            State state = state(entity);
            state.currentAnimations = java.util.List.copyOf(animationStates);
            state.currentAnimationState = animationStates.stream().filter(GeoObbAnimationState::active).findFirst().orElse(GeoObbAnimationState.NONE);
            if (!temporaryWindowEnded(state)) {
                return false;
            }
            state.temporaryAnimationState = GeoObbAnimationState.NONE;
            return true;
        }
    }

    private static boolean temporaryWindowEnded(State state) {
        GeoObbAnimationState temporary = state.temporaryAnimationState;
        if (!temporary.active()) {
            return false;
        }
        GeoObbAnimationState current = state.currentAnimations.stream()
                .filter(animation -> animation.controllerName().equals(temporary.controllerName())).findFirst()
                .orElse(GeoObbAnimationState.NONE);
        boolean ended = !current.active()
                || current.finished()
                || !temporary.animationKey().equals(current.animationKey())
                || current.animationTimeSeconds() + 0.05 < temporary.animationTimeSeconds();
        if (!ended) { state.temporaryAnimationState = current; }
        return ended;
    }

    private static State state(GeoBoneHitboxEntity entity) {
        return STATES.computeIfAbsent(entity, ignored -> new State());
    }

    private static GeoObbAnimationState normalize(GeoObbAnimationState state) {
        return state == null ? GeoObbAnimationState.NONE : state;
    }

    private static final class State {
        private java.util.List<GeoObbAnimationState> currentAnimations = java.util.List.of();
        private GeoObbAnimationState temporaryAnimationState = GeoObbAnimationState.NONE;
        private GeoObbAnimationState currentAnimationState = GeoObbAnimationState.NONE;
    }
}
