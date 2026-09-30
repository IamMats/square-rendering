package dev.iammats.squarerendering.client.mixin;

import dev.iammats.squarerendering.client.RenderBounds;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.VisibleChunkCollector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Filters render-list collection from both the main and fallback terrain trees. */
@Mixin(value = VisibleChunkCollector.class, remap = false)
public abstract class VisibleChunkCollectorMixin {
  @Inject(
      method = "visit(III)V",
      at = @At("HEAD"),
      cancellable = true)
  private void squareRenderingFilterSection(int x, int y, int z, CallbackInfo ci) {
    if (!RenderBounds.includes(x, y, z)) {
      ci.cancel();
    }
  }
}
