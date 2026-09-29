package dev.iammats.squarerendering.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.iammats.squarerendering.client.SquareRenderingClient;
import dev.iammats.squarerendering.render.FogShaderTransform;
import net.caffeinemc.mods.sodium.client.gl.shader.ShaderLoader;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Transforms the original fog source anew for each shader reload. */
@Mixin(value = ShaderLoader.class, remap = false)
public abstract class SodiumShaderLoaderMixin {
  @ModifyReturnValue(method = "getShaderSource", at = @At("RETURN"))
  private static String squareRenderingTransformFog(String original, Identifier location) {
    if (location.getNamespace().equals("sodium") && location.getPath().equals("include/fog.glsl")) {
      return FogShaderTransform.sodium(original, SquareRenderingClient.isEnabled());
    }
    return original;
  }
}
