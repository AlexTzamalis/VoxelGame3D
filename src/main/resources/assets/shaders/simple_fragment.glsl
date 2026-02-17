#version 330 core

// Input from vertex shader
in vec2 texCoord;
in vec3 tintColor;
in vec3 normal;
in vec3 fragPos;

// Output color
out vec4 fragColor;

// Uniforms
uniform bool useTexture;
uniform sampler2D textureSampler;
uniform vec3 lightDirection;
uniform vec3 ambientColor;

void main() {
    vec4 baseColor;
    
    if (useTexture) {
        baseColor = texture(textureSampler, texCoord);
        
        // Discard fully transparent pixels
        if (baseColor.a < 0.1) {
            discard;
        }
        
        // Apply tint color (for grass, leaves, water, etc.)
        // Tint multiplies with texture color
        baseColor.rgb *= tintColor;
    } else {
        baseColor = vec4(tintColor, 1.0);
    }
    
    // Simple directional lighting
    vec3 norm = normalize(normal);
    vec3 lightDir = normalize(lightDirection);
    
    // Ambient lighting
    vec3 ambient = ambientColor * baseColor.rgb;
    
    // Diffuse lighting (sun)
    float diff = max(dot(norm, lightDir), 0.0);
    vec3 diffuse = diff * baseColor.rgb;
    
    // Combine lighting
    vec3 result = ambient + diffuse * 0.6;
    
    fragColor = vec4(result, baseColor.a);
}
