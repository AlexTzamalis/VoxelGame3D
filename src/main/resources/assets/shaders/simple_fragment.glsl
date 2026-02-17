#version 330 core

// Input from vertex shader
in vec3 vertexColor;
in vec2 texCoord;

// Output color
out vec4 fragColor;

// Uniforms
uniform bool useTexture;
uniform sampler2D textureSampler;

void main() {
    if (useTexture) {
        fragColor = texture(textureSampler, texCoord);
    } else {
        fragColor = vec4(vertexColor, 1.0);
    }
}

