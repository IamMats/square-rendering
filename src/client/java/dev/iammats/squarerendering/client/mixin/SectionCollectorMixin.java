package dev.iammats.squarerendering.client.mixin;

import dev.iammats.squarerendering.client.RenderBounds;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.SectionCollector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Covers graph traversal, tree traversal, and immediate mesh-result collection. */
@Mixin(value = SectionCollector.class, remap = false)
public abstract class SectionCollectorMixin {
  @Inject(
      method = "visit(Lnet/caffeinemc/mods/sodium/client/render/chunk/RenderSection;I)V",
      at = @At("HEAD"),
      cancellable = true)
  private void squareRenderingFilterSection(RenderSection section, int flags, CallbackInfo ci) {
    if (!RenderBounds.includes(section)) {
      ci.cancel();
    }
  }
}
