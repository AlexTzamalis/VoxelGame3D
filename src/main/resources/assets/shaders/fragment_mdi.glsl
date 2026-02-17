#version 430 core

// Input from vertex shader
in vec3 fragPos;
in vec2 texCoord;
in vec3 normal;
in vec2 light; // blockLight, skyLight
in float lodFactor; // LOD transition factor
in float depthBias; // Depth bias (already applied in vertex shader)

// Output color
out vec4 fragColor;

// Uniforms
uniform bool useTexture;
uniform sampler2D textureSampler;
uniform vec3 lightDirection;
uniform vec3 ambientColor;
uniform bool enableLODTransitions; // Enable smooth LOD transitions

void main() {
    vec4 baseColor;
    
    if (useTexture) {
        baseColor = texture(textureSampler, texCoord);
        
        // Discard fully transparent pixels
        if (baseColor.a < 0.1) {
            discard;
        }
    } else {
        baseColor = vec4(1.0, 1.0, 1.0, 1.0); // Default white if no texture
    }
    
    // Simple directional lighting
    vec3 norm = normalize(normal);
    vec3 lightDir = normalize(lightDirection);
    
    // Ambient lighting
    vec3 ambient = ambientColor * baseColor.rgb;
    
    // Diffuse lighting (sun)
    float diff = max(dot(norm, lightDir), 0.0);
    vec3 diffuse = diff * baseColor.rgb;
    
    // Apply block light and sky light
    // Convert light values (0-15) to intensity (0-1)
    float blockLightIntensity = light.x / 15.0;
    float skyLightIntensity = light.y / 15.0;
    
    // Combine lighting with block/sky light
    vec3 result = ambient + diffuse * 0.6;
    result += baseColor.rgb * blockLightIntensity * 0.3; // Block light contribution
    result += baseColor.rgb * skyLightIntensity * 0.5;  // Sky light contribution
    
    // Apply LOD transition effects (optional dithering/fading)
    if (enableLODTransitions && lodFactor > 0.0) {
        // Optional: Apply slight desaturation for distant chunks
        float saturation = 1.0 - lodFactor * 0.2; // Slight desaturation
        float gray = dot(result, vec3(0.299, 0.587, 0.114));
        result = mix(vec3(gray), result, saturation);
        
        // Optional: Apply distance fog effect
        // This could be enhanced with actual fog calculations
    }
    
    fragColor = vec4(result, baseColor.a);
}

