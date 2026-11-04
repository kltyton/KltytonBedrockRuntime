package com.kltyton.bedrockruntime.client.particle.bedrock;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import net.minecraft.util.GsonHelper;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.StringReader;

public final class BedrockJson {
    private BedrockJson() {
    }

    public static JsonObject parse(Reader source) throws IOException {
        return GsonHelper.parse(new StringReader(filter(source)));
    }

    public static JsonArray parseArray(Reader source) throws IOException {
        return GsonHelper.parseArray(new StringReader(filter(source)));
    }

    private static String filter(Reader source) throws IOException {
        PushbackReader reader = new PushbackReader(source, 1);
        StringBuilder filtered = new StringBuilder();
        boolean string = false;
        boolean escaped = false;
        int next;
        while ((next = reader.read()) != -1) {
            char c = (char) next;
            if (string) {
                filtered.append(c);
                if (escaped) escaped = false;
                else if (c == '\\') escaped = true;
                else if (c == '"') string = false;
            } else if (c == '"') {
                string = true;
                filtered.append(c);
            } else if (c == '/') {
                int following = reader.read();
                if (following == '/') {
                    while ((following = reader.read()) != -1 && following != '\n' && following != '\r') {
                    }
                    if (following != -1) filtered.append((char) following);
                } else if (following == '*') {
                    boolean closed = false;
                    int previous = 0;
                    filtered.append(' ');
                    while ((following = reader.read()) != -1) {
                        if (previous == '*' && following == '/') {
                            closed = true;
                            break;
                        }
                        if (following == '\n' || following == '\r') filtered.append((char) following);
                        previous = following;
                    }
                    if (!closed) throw new com.google.gson.JsonParseException("Unterminated JSON block comment");
                } else {
                    filtered.append(c);
                    if (following != -1) reader.unread(following);
                }
            } else {
                filtered.append(c);
            }
        }
        return filtered.toString();
    }
}
