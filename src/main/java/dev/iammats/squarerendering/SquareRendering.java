package dev.iammats.squarerendering;

import dev.iammats.squarerendering.mixin.ChunkMapAccess;
import dev.iammats.squarerendering.network.SquareRequestPayload;
import dev.iammats.squarerendering.network.SquareStatusPayload;
import dev.iammats.squarerendering.server.SquarePlayerState;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Shared entry point. This class must remain loadable on a dedicated server without Sodium. */
public final class SquareRendering implements ModInitializer {
  public static final String MOD_ID = "square-rendering";
  public static final Logger LOGGER = LoggerFactory.getLogger("Square Rendering");

  /** Creates an identifier in this mod's namespace. */
  public static Identifier id(String path) {
    Objects.requireNonNull(path, "path must not be null");
    return Identifier.fromNamespaceAndPath(MOD_ID, path);
  }

  @Override
   @SuppressWarnings("null")
  public void onInitialize() {
    PayloadTypeRegistry.playC2S().register(SquareRequestPayload.TYPE, SquareRequestPayload.CODEC);
    PayloadTypeRegistry.playS2C().register(SquareStatusPayload.TYPE, SquareStatusPayload.CODEC);
    ServerPlayNetworking.registerGlobalReceiver(
        SquareRequestPayload.TYPE,
        (payload, context) -> {
          SquarePlayerState state = (SquarePlayerState) context.player();
          state.squareRenderingSetEnabled(payload.enabled());
          state.squareRenderingSetLastStatus(null);
          ((ChunkMapAccess) context.player().level().getChunkSource().chunkMap)
              .squareRenderingUpdateTracking(context.player());
        });
    ServerPlayerEvents.COPY_FROM.register(
        (oldPlayer, newPlayer, alive) ->
            ((SquarePlayerState) newPlayer)
                .squareRenderingSetEnabled(
                    ((SquarePlayerState) oldPlayer).squareRenderingEnabled()));
    LOGGER.info("Square Rendering server support initialized");
  }
}
