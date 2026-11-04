#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

in float vertexDistance;
in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec4 texel = texture(Sampler0, texCoord0);
    if (texel.a < 0.5) {
        discard;
    }
    vec4 color = vec4(texel.rgb * vertexColor.rgb * ColorModulator.rgb, 1.0);
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
