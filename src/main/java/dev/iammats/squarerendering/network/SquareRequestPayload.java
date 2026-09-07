package dev.iammats.squarerendering.network;

import dev.iammats.squarerendering.SquareRendering;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Requests square tracking without increasing the client's advertised view distance. */
@SuppressWarnings("null")
public record SquareRequestPayload(boolean enabled) implements CustomPacketPayload {
  public static final Type<SquareRequestPayload> TYPE =
      new Type<>(SquareRendering.id("request_v1"));
  public static final StreamCodec<RegistryFriendlyByteBuf, SquareRequestPayload> CODEC =
      StreamCodec.composite(
          ByteBufCodecs.BOOL, SquareRequestPayload::enabled, SquareRequestPayload::new);

  @Override
  public Type<SquareRequestPayload> type() {
    return TYPE;
  }
}
