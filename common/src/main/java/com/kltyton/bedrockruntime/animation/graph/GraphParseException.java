package com.kltyton.bedrockruntime.animation.graph;

/**
 * Parse error carrying the JSON path that failed.
 */
public final class GraphParseException extends RuntimeException {
    private final String path;

    public GraphParseException(String path, String message) {
        super("Invalid animation graph at " + path + ": " + message);
        this.path = path;
    }

    public GraphParseException(String path, String message, Throwable cause) {
        super("Invalid animation graph at " + path + ": " + message, cause);
        this.path = path;
    }

    public String path() {
        return path;
    }
}
