package dev.iammats.squarerendering.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.iammats.squarerendering.client.RenderBounds;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import java.util.SortedSet;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps globally rendered block entities, such as beacons, inside the visible square. */
@Mixin(value = SodiumWorldRenderer.class, remap = false)
public abstract class SodiumWorldRendererMixin {
  @Inject(method = "extractBlockEntity", at = @At("HEAD"), cancellable = true)
  private void squareRenderingLimitBlockEntities(
      BlockEntity blockEntity,
      PoseStack poseStack,
      Camera camera,
      float tickDelta,
      Long2ObjectMap<SortedSet<BlockDestructionProgress>> progression,
      LevelRenderState levelRenderState,
      CallbackInfo ci) {
    var position = blockEntity.getBlockPos();
    if (!RenderBounds.includesColumn(position.getX() >> 4, position.getZ() >> 4)) {
      ci.cancel();
    }
  }
}
