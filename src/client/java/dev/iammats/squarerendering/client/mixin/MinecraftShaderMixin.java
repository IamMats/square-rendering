package dev.iammats.squarerendering.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.iammats.squarerendering.client.SquareRenderingClient;
import dev.iammats.squarerendering.render.FogShaderTransform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Changes terrain-distance fog for both vanilla and Sodium render pipelines. */
@Mixin(targets = "net.minecraft.client.renderer.ShaderManager$CompilationCache")
public abstract class MinecraftShaderMixin {
  @ModifyReturnValue(method = "getShaderSource", at = @At("RETURN"))
  private String squareRenderingTransformFog(String original) {
    if (original == null) {
      return null;
    }
    boolean enabled = SquareRenderingClient.isEnabled();
    return FogShaderTransform.sodium(FogShaderTransform.minecraft(original, enabled), enabled);
  }
}
