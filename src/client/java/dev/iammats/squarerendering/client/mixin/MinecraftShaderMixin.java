package dev.iammats.squarerendering.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.iammats.squarerendering.client.SquareRenderingClient;
import dev.iammats.squarerendering.render.FogShaderTransform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Changes terrain-distance fog for vanilla pipelines such as block entities and entities. */
@Mixin(targets = "net.minecraft.client.renderer.ShaderManager$CompilationCache")
public abstract class MinecraftShaderMixin {
  @ModifyReturnValue(method = "getShaderSource", at = @At("RETURN"))
  private String squareRenderingTransformFog(String original) {
    return FogShaderTransform.minecraft(original, SquareRenderingClient.isEnabled());
  }
}
