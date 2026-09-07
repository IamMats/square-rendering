package dev.iammats.squarerendering.render;

/** Narrow source transformations that leave spherical environmental fog untouched. */
public final class FogShaderTransform {
  private static final String SODIUM_CYLINDER = "max(length(position.xz), abs(position.y))";
  private static final String SODIUM_SQUARE =
      "max(max(abs(position.x), abs(position.z)), abs(position.y))";
  private static final String VANILLA_CYLINDER = "float distXZ = length(pos.xz);";
  private static final String VANILLA_SQUARE = "float distXZ = max(abs(pos.x), abs(pos.z));";

  private FogShaderTransform() {}

  /** Changes only the render-distance component of Sodium's fog distance pair. */
  public static String sodium(String source, boolean enabled) {
    return enabled ? source.replace(SODIUM_CYLINDER, SODIUM_SQUARE) : source;
  }

  /** Changes the Minecraft fog include after preprocessing, preserving missing shader results. */
  public static String minecraft(String source, boolean enabled) {
    return enabled && source != null ? source.replace(VANILLA_CYLINDER, VANILLA_SQUARE) : source;
  }
}
