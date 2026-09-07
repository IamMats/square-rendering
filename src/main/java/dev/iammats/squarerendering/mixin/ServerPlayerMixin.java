package dev.iammats.squarerendering.mixin;

import dev.iammats.squarerendering.network.SquareStatusPayload;
import dev.iammats.squarerendering.server.SquarePlayerState;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Stores session preferences alongside the server player. */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin implements SquarePlayerState {
  @Unique private boolean squareRenderingEnabled;
  @Unique private SquareStatusPayload squareRenderingLastStatus;

  @Override
  public boolean squareRenderingEnabled() {
    return squareRenderingEnabled;
  }

  @Override
  public void squareRenderingSetEnabled(boolean enabled) {
    squareRenderingEnabled = enabled;
  }

  @Override
  public SquareStatusPayload squareRenderingLastStatus() {
    return squareRenderingLastStatus;
  }

  @Override
  public void squareRenderingSetLastStatus(SquareStatusPayload status) {
    squareRenderingLastStatus = status;
  }
}
