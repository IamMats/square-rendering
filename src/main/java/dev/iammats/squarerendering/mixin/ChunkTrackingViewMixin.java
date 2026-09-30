package dev.iammats.squarerendering.mixin;

import dev.iammats.squarerendering.server.SquareTrackingView;
import java.util.function.Consumer;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevents vanilla's unknown-view fallback from resending unchanged square chunks. */
@Mixin(ChunkTrackingView.class)
public interface ChunkTrackingViewMixin {
  @Inject(method = "difference", at = @At("HEAD"), cancellable = true)
  private static void squareRenderingDifference(
      ChunkTrackingView previous,
      ChunkTrackingView next,
      Consumer<ChunkPos> added,
      Consumer<ChunkPos> removed,
      CallbackInfo ci) {
    if (previous instanceof SquareTrackingView || next instanceof SquareTrackingView) {
      SquareTrackingView.difference(previous, next, added, removed);
      ci.cancel();
    }
  }
}
