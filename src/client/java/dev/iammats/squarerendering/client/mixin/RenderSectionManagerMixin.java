package dev.iammats.squarerendering.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.iammats.squarerendering.client.RenderBounds;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Widens traversal only; the collector still enforces the exact visible footprint. */
@Mixin(value = RenderSectionManager.class, remap = false)
public abstract class RenderSectionManagerMixin {
  @Inject(method = "prepareFrame", at = @At("HEAD"))
  private void squareRenderingCaptureFrame(Vector3dc cameraPosition, CallbackInfo ci) {
    RenderBounds.capture(cameraPosition.x(), cameraPosition.y(), cameraPosition.z());
  }

  @ModifyReturnValue(method = "getSearchDistance", at = @At("RETURN"))
  private float squareRenderingCoverCorners(float original) {
    return RenderBounds.active() ? RenderBounds.searchDistance() : original;
  }
}
