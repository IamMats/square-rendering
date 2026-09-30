package dev.iammats.squarerendering.client.mixin;

import com.mojang.renderpearl.api.pipeline.ShaderSource;
import dev.iammats.squarerendering.client.SquareRenderingClient;
import dev.iammats.squarerendering.render.FogShaderTransform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Changes shared fog includes before RenderPearl caches them for shader compilation. */
@Mixin(ShaderSource.CachedIncludeSource.class)
public abstract class MinecraftShaderMixin {
  @ModifyVariable(method = "create", at = @At("HEAD"), argsOnly = true)
  private static String squareRenderingTransformFog(String original) {
    boolean enabled = SquareRenderingClient.isEnabled();
    return FogShaderTransform.sodium(FogShaderTransform.minecraft(original, enabled), enabled);
  }
}
