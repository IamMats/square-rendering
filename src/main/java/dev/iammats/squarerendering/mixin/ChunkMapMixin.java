package dev.iammats.squarerendering.mixin;

import dev.iammats.squarerendering.geometry.ChunkSquare;
import dev.iammats.squarerendering.network.SquareStatusPayload;
import dev.iammats.squarerendering.server.SquarePlayerState;
import dev.iammats.squarerendering.server.SquareTrackingView;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Selects a tracking view per player while retaining vanilla packet delivery. */
@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin {
  @Shadow @Final private ServerLevel level;

  @Shadow
  abstract int getPlayerViewDistance(ServerPlayer player);

  @Shadow
  private void applyChunkTrackingView(ServerPlayer player, ChunkTrackingView view) {
    throw new AssertionError();
  }

  @SuppressWarnings("null")
  @Inject(method = "updateChunkTracking", at = @At("HEAD"), cancellable = true)
  private void squareRenderingUpdateTracking(ServerPlayer player, CallbackInfo ci) {
    SquarePlayerState state = (SquarePlayerState) player;
    int radius = getPlayerViewDistance(player);
    SquareStatusPayload status = new SquareStatusPayload(state.squareRenderingEnabled(), radius);
    if (!status.equals(state.squareRenderingLastStatus())
        && ServerPlayNetworking.canSend(player, SquareStatusPayload.TYPE)) {
      ServerPlayNetworking.send(player, status);
      state.squareRenderingSetLastStatus(status);
    }
    if (!state.squareRenderingEnabled()) {
      return;
    }
    ChunkPos center = player.chunkPosition();
    SquareTrackingView next = new SquareTrackingView(new ChunkSquare(center.x, center.z, radius));
    if (!next.equals(player.getChunkTrackingView())) {
      applyChunkTrackingView(player, next);
    }
    ci.cancel();
  }

  @SuppressWarnings("null")
  @Inject(method = "applyChunkTrackingView", at = @At("HEAD"))
  private void squareRenderingSendCenter(
      ServerPlayer player, ChunkTrackingView next, CallbackInfo ci) {
    if (player.level() != level || !(next instanceof SquareTrackingView square)) {
      return;
    }
    ChunkTrackingView previous = player.getChunkTrackingView();
    ChunkPos oldCenter = null;
    if (previous instanceof SquareTrackingView oldSquare) {
      oldCenter = oldSquare.center();
    } else if (previous instanceof ChunkTrackingView.Positioned positioned) {
      oldCenter = positioned.center();
    }
    ChunkPos center = square.center();
    if (!center.equals(oldCenter)) {
      player.connection.send(new ClientboundSetChunkCacheCenterPacket(center.x, center.z));
    }
  }
}
