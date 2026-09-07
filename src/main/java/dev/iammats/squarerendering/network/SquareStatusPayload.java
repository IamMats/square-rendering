package dev.iammats.squarerendering.network;

import dev.iammats.squarerendering.SquareRendering;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Reports the radius permitted by the server, excluding the invisible support border. */
 @SuppressWarnings("null")
public record SquareStatusPayload(boolean enabled, int radius) implements CustomPacketPayload {
  public static final Type<SquareStatusPayload> TYPE = new Type<>(SquareRendering.id("status_v1"));
  public static final StreamCodec<RegistryFriendlyByteBuf, SquareStatusPayload> CODEC =
      StreamCodec.composite(
          ByteBufCodecs.BOOL,
          SquareStatusPayload::enabled,
          ByteBufCodecs.VAR_INT,
          SquareStatusPayload::radius,
          SquareStatusPayload::new);

  @Override
  public Type<SquareStatusPayload> type() {
    return TYPE;
  }
}
