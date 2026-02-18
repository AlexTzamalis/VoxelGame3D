#version 430 core

// Packed vertex attributes (as integers, 4 ints per vertex, 16 bytes stride)
layout (location = 0) in int aPositionPacked;    // Position packed as int (offset 0)
layout (location = 1) in int aUVPacked;          // UV packed as int (offset 4)
layout (location = 2) in int aNormalPacked;     // Normal packed as int (offset 8)
layout (location = 3) in int aLightPacked;       // Light packed as int (offset 12)

// Output to fragment shader
out vec3 fragPos;
out vec2 texCoord;
out vec3 normal;
out vec2 light; // blockLight, skyLight
out float lodFactor; // LOD transition factor (0 = full detail, 1 = simplified)
out float depthBias; // Depth bias for LOD transitions (prevent z-fighting)

// Uniforms
uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;
uniform mat4 modelMatrix;
uniform vec3 cameraPosition;  // For LOD distance calculation
uniform float lodTransitionDistance; // Distance where LOD transitions occur

// Unpacking constants (must match VertexPacker.java)
const float POS_SCALE = 1024.0;
const float POS_INV_SCALE = 1.0 / POS_SCALE;
const float UV_SCALE = 65535.0;
const float UV_INV_SCALE = 1.0 / UV_SCALE;

/**
 * Unpacks position from packed integer.
 * Uses GLSL 4.3+ bitwise operations for efficient extraction.
 */
vec3 unpackPosition(int packed) {
    // Extract using bitwise operations (GLSL 4.0+ supports these on integers)
    int ix = packed & 0x3FF;                   // Bits 0-9
    int iy = (packed >> 10) & 0x3FF;           // Bits 10-19
    int iz = (packed >> 20) & 0x3FF;           // Bits 20-29
    
    return vec3(
        float(ix) * POS_INV_SCALE,
        float(iy) * POS_INV_SCALE,
        float(iz) * POS_INV_SCALE
    );
}

/**
 * Unpacks UV coordinates from packed integer.
 * Uses GLSL 4.3+ bitwise operations for efficient extraction.
 */
vec2 unpackUV(int packed) {
    // Extract using bitwise operations
    int iu = packed & 0xFFFF;                 // Lower 16 bits
    int iv = (packed >> 16) & 0xFFFF;         // Upper 16 bits
    
    return vec2(
        float(iu) * UV_INV_SCALE,
        float(iv) * UV_INV_SCALE
    );
}

/**
 * Unpacks normal from packed integer using octahedral decoding.
 * Uses GLSL 4.3+ bitwise operations for efficient extraction.
 */
vec3 unpackNormal(int packed) {
    // Extract using bitwise operations
    int ix = packed & 0x3FF;                   // Bits 0-9
    int iy = (packed >> 10) & 0x3FF;           // Bits 10-19
    
    // Convert back to -1 to 1 range
    float x = (float(ix) / 511.5) - 1.0;
    float y = (float(iy) / 511.5) - 1.0;
    
    // Octahedral decoding
    float nx = x;
    float ny = y;
    float nz = 1.0 - abs(x) - abs(y);
    
    if (nz < 0.0) {
        float sx = x >= 0.0 ? 1.0 : -1.0;
        float sy = y >= 0.0 ? 1.0 : -1.0;
        nx = (1.0 - abs(y)) * sx;
        ny = (1.0 - abs(x)) * sy;
    }
    
    // Reconstruct z
    nz = 1.0 - abs(nx) - abs(ny);
    if (nz < 0.0) {
        nz = 0.0;
    }
    
    // Normalize
    vec3 n = vec3(nx, ny, nz);
    float len = length(n);
    if (len > 0.0001) {
        n /= len;
    }
    
    return n;
}

/**
 * Unpacks light values from packed integer.
 * Uses GLSL 4.3+ bitwise operations for efficient extraction.
 */
vec2 unpackLight(int packed) {
    // Extract using bitwise operations
    int blockLight = packed & 0x0F;            // Lower 4 bits
    int skyLight = (packed >> 4) & 0x0F;       // Upper 4 bits (bits 4-7)
    
    return vec2(float(blockLight), float(skyLight));
}

void main() {
    // Unpack vertex attributes
    vec3 position = unpackPosition(aPositionPacked);
    texCoord = unpackUV(aUVPacked);
    normal = unpackNormal(aNormalPacked);
    light = unpackLight(aLightPacked);
    
    // Calculate world position
    vec4 worldPos = modelMatrix * vec4(position, 1.0);
    fragPos = worldPos.xyz;
    
    // Transform normal to world space
    normal = mat3(transpose(inverse(modelMatrix))) * normal;
    
    // Calculate distance from camera for LOD transitions
    float distanceFromCamera = length(fragPos - cameraPosition);
    
    // Determine if this is a simplified chunk (baseInstance = 1)
    // We use gl_InstanceID, but since we're using baseInstance in the draw command,
    // we need to check if we're in a simplified chunk region
    // For now, we'll use distance-based LOD factor
    float lodDistance = lodTransitionDistance;
    float lodFactorValue = clamp((distanceFromCamera - lodDistance * 0.8) / (lodDistance * 0.4), 0.0, 1.0);
    lodFactor = lodFactorValue;
    
    // Apply depth bias for simplified chunks to prevent z-fighting
    // Simplified chunks get a small depth bias to render slightly behind full chunks
    depthBias = lodFactorValue * 0.0001; // Small bias for LOD transitions
    
    // Calculate final position with depth bias
    vec4 clipPos = projectionMatrix * viewMatrix * worldPos;
    clipPos.z += depthBias * clipPos.w; // Apply depth bias in clip space
    gl_Position = clipPos;
}
