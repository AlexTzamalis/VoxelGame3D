#version 330 core

// Vertex attributes
layout (location = 0) in vec3 aPosition;
layout (location = 1) in vec2 aTexCoord;

// Output to fragment shader
out vec3 vertexColor;
out vec2 texCoord;

// Uniforms
uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;
uniform mat4 modelMatrix;
uniform vec3 color;

void main() {
    // Pass color to fragment shader
    vertexColor = color;
    
    // Pass texture coordinates
    texCoord = aTexCoord;
    
    // Calculate final position
    gl_Position = projectionMatrix * viewMatrix * modelMatrix * vec4(aPosition, 1.0);
}

