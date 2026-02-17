#version 330 core

// Vertex attributes
layout (location = 0) in vec3 aPosition;
layout (location = 1) in vec2 aTexCoord;
layout (location = 2) in vec3 aNormal;
layout (location = 3) in vec3 aTint;

// Output to fragment shader
out vec2 texCoord;
out vec3 tintColor;
out vec3 normal;
out vec3 fragPos;

// Uniforms
uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;
uniform mat4 modelMatrix;

void main() {
    // Pass texture coordinates
    texCoord = aTexCoord;
    
    // Pass tint color for biome coloring (grass, leaves, etc.)
    tintColor = aTint;
    
    // Pass normal for lighting
    normal = aNormal;
    
    // Calculate world position for lighting
    vec4 worldPos = modelMatrix * vec4(aPosition, 1.0);
    fragPos = worldPos.xyz;
    
    // Calculate final position
    gl_Position = projectionMatrix * viewMatrix * worldPos;
}
