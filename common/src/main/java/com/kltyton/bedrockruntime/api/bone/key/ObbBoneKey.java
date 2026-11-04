package com.kltyton.bedrockruntime.api.bone.key;

/**
 * CN: 一个视觉模型 cube 的稳定身份。骨骼名允许包含原版 ModelPart 的完整层级路径。
 * EN: Stable identity of one visual-model cube. The bone name may contain a full vanilla ModelPart path.
 */
public record ObbBoneKey(String source, String name, int cubeIndex) implements Comparable<ObbBoneKey> {
    public ObbBoneKey {
        source = source == null ? "" : source;
        name = name == null ? "" : name;
        if (cubeIndex < 0) {
            throw new IllegalArgumentException("cubeIndex must be non-negative");
        }
    }

    @Override
    public int compareTo(ObbBoneKey other) {
        int sourceResult = source.compareTo(other.source);
        if (sourceResult != 0) {
            return sourceResult;
        }
        int nameResult = name.compareTo(other.name);
        return nameResult != 0 ? nameResult : Integer.compare(cubeIndex, other.cubeIndex);
    }

    public String displayName() {
        return source + ":" + name + "[" + cubeIndex + "]";
    }
}
