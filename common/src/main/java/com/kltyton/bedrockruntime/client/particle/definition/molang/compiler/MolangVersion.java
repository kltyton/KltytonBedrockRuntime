package com.kltyton.bedrockruntime.client.particle.definition.molang.compiler;

public record MolangVersion(int major, int minor, int patch, boolean prerelease) implements Comparable<MolangVersion> {
    public static final MolangVersion LATEST = new MolangVersion(Integer.MAX_VALUE, 0, 0);

    public MolangVersion(int major, int minor, int patch) {
        this(major, minor, patch, false);
    }

    public MolangVersion {
        if (major < 0 || minor < 0 || patch < 0)
            throw new IllegalArgumentException("Negative Molang engine version");
    }

    public boolean atLeast(int major, int minor, int patch) {
        return compareTo(new MolangVersion(major, minor, patch)) >= 0;
    }

    @Override
    public int compareTo(MolangVersion other) {
        int result = Integer.compare(major, other.major);
        if (result == 0) result = Integer.compare(minor, other.minor);
        if (result == 0) result = Integer.compare(patch, other.patch);
        return result != 0 ? result : Boolean.compare(other.prerelease, prerelease);
    }
}
