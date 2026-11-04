package com.kltyton.bedrockruntime.client.particle.runtime.geckolib;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;

import java.util.ArrayDeque;
import java.util.Deque;

/** Associates GeckoLib's collected particle markers with their animation sampling time. */
public final class AnimationMarkerScope implements AutoCloseable {
    private static final ThreadLocal<Deque<AnimationMarkerScope>> SCOPES = new ThreadLocal<>();
    private final DoubleArrayList samples = new DoubleArrayList();
    private final boolean reverse;
    private int next;

    private AnimationMarkerScope(boolean reverse) {
        this.reverse = reverse;
    }

    public static AnimationMarkerScope enter(boolean reverse) {
        Deque<AnimationMarkerScope> scopes = SCOPES.get();
        if (scopes == null) {
            scopes = new ArrayDeque<>();
            SCOPES.set(scopes);
        }
        AnimationMarkerScope scope = new AnimationMarkerScope(reverse);
        scopes.push(scope);
        return scope;
    }

    public static void record(int markerCount, double startTime, double endTime) {
        AnimationMarkerScope scope = current();
        double time = scope.reverse ? startTime : endTime;
        for (int i = 0; i < markerCount; i++) scope.samples.add(time);
    }

    public static double nextSample() {
        AnimationMarkerScope scope = current();
        int index = scope.reverse ? scope.samples.size() - 1 - scope.next++ : scope.next++;
        return scope.samples.getDouble(index);
    }

    private static AnimationMarkerScope current() {
        Deque<AnimationMarkerScope> scopes = SCOPES.get();
        if (scopes == null || scopes.isEmpty()) throw new IllegalStateException("Missing animation marker sampling scope");
        return scopes.peek();
    }

    @Override
    public void close() {
        Deque<AnimationMarkerScope> scopes = SCOPES.get();
        if (scopes == null || scopes.peek() != this) throw new IllegalStateException("Animation marker scopes must close in stack order");
        scopes.pop();
        if (scopes.isEmpty()) SCOPES.remove();
    }
}
