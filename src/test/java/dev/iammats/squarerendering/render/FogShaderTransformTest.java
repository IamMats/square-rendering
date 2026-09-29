package dev.iammats.squarerendering.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FogShaderTransformTest {
  @Test
  void sodiumChangesOnlyRenderDistanceAndPreservesSphericalFog() {
    String original = "return vec2(max(length(position.xz), abs(position.y)), length(position));";
    String transformed = FogShaderTransform.sodium(original, true);
    assertTrue(transformed.contains("max(abs(position.x), abs(position.z))"));
    assertTrue(transformed.endsWith("length(position));"));
    assertFalse(transformed.contains("length(position.xz)"));
    assertEquals(transformed, FogShaderTransform.sodium(transformed, true));
    assertEquals(original, FogShaderTransform.sodium(original, false));
  }

  @Test
  void minecraftChangesOnlyCylindricalDistance() {
    String original =
        "float fog_spherical_distance(vec3 pos) { return length(pos); }\n"
            + "float fog_cylindrical_distance(vec3 pos) { float distXZ = length(pos.xz); "
            + "float distY = abs(pos.y); return max(distXZ, distY); }";
    String transformed = FogShaderTransform.minecraft(original, true);
    assertTrue(transformed.contains("return length(pos);"));
    assertTrue(transformed.contains("return max(distXZ, distY);"));
    assertTrue(transformed.contains("float distXZ = max(abs(pos.x), abs(pos.z));"));
    assertEquals(transformed, FogShaderTransform.minecraft(transformed, true));
    assertEquals(original, FogShaderTransform.minecraft(original, false));
    assertNull(FogShaderTransform.minecraft(null, true));
  }

  @Test
  void unrelatedShaderSourceIsUnchanged() {
    String original = "void main() { gl_Position = vec4(1.0); }";
    assertEquals(original, FogShaderTransform.minecraft(original, true));
    assertEquals(original, FogShaderTransform.sodium(original, true));
  }
}
