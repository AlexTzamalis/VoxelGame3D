#version 330 core

// Input from vertex shader
in vec3 fragPos;
in vec2 texCoord;
in vec3 normal;

// Output color
out vec4 fragColor;

// Uniforms
uniform vec3 objectColor;
uniform vec3 lightColor;
uniform vec3 lightPos;
uniform vec3 viewPos;
uniform bool useTexture;
uniform sampler2D textureSampler;

// Ambient light intensity
const float ambientStrength = 0.3;

// Specular parameters
const float specularStrength = 0.5;
const int shininess = 32;

void main() {
    // Normalize the normal vector
    vec3 norm = normalize(normal);
    
    // Calculate light direction
    vec3 lightDir = normalize(lightPos - fragPos);
    
    // Ambient lighting
    vec3 ambient = ambientStrength * lightColor;
    
    // Diffuse lighting
    float diff = max(dot(norm, lightDir), 0.0);
    vec3 diffuse = diff * lightColor;
    
    // Specular lighting (Blinn-Phong)
    vec3 viewDir = normalize(viewPos - fragPos);
    vec3 halfwayDir = normalize(lightDir + viewDir);
    float spec = pow(max(dot(norm, halfwayDir), 0.0), shininess);
    vec3 specular = specularStrength * spec * lightColor;
    
    // Combine lighting
    vec3 lighting = ambient + diffuse + specular;
    
    // Get base color (texture or solid color)
    vec3 baseColor;
    if (useTexture) {
        baseColor = texture(textureSampler, texCoord).rgb;
    } else {
        baseColor = objectColor;
    }
    
    // Final color
    vec3 result = lighting * baseColor;
    fragColor = vec4(result, 1.0);
}


