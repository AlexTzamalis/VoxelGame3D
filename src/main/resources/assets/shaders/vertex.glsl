#version 330 core

// Vertex attributes
layout (location = 0) in vec3 aPosition;
layout (location = 1) in vec2 aTexCoord;
layout (location = 2) in vec3 aNormal;

// Output to fragment shader
out vec3 fragPos;
out vec2 texCoord;
out vec3 normal;

// Uniforms
uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;
uniform mat4 modelMatrix;

void main() {
    // Calculate world position
    vec4 worldPos = modelMatrix * vec4(aPosition, 1.0);
    fragPos = worldPos.xyz;
    
    // Pass texture coordinates
    texCoord = aTexCoord;
    
    // Transform normal to world space
    normal = mat3(transpose(inverse(modelMatrix))) * aNormal;
    
    // Calculate final position
    gl_Position = projectionMatrix * viewMatrix * worldPos;
}

