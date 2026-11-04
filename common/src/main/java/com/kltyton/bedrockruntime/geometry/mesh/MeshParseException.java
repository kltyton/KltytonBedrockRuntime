package com.kltyton.bedrockruntime.geometry.mesh;

import java.util.Objects;

public final class MeshParseException extends IllegalArgumentException {
    private final String resource;
    private final String bone;
    private final String detail;

    public MeshParseException(String resource, String bone, String detail) {
        super(message(resource, bone, detail));
        this.resource = resource == null ? "" : resource;
        this.bone = bone == null ? "" : bone;
        this.detail = detail == null ? "" : detail;
    }

    public MeshParseException(String resource, String bone, String detail, Throwable cause) {
        super(message(resource, bone, detail), cause);
        this.resource = resource == null ? "" : resource;
        this.bone = bone == null ? "" : bone;
        this.detail = detail == null ? "" : detail;
    }

    public String resource() {
        return resource;
    }

    public String bone() {
        return bone;
    }

    public String detail() {
        return detail;
    }

    private static String message(String resource, String bone, String detail) {
        return "poly_mesh[" + Objects.toString(resource, "") + "/" + Objects.toString(bone, "") + "]: "
                + Objects.toString(detail, "invalid mesh");
    }
}
