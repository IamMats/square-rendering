package dev.iammats.squarerendering.client;

import dev.iammats.squarerendering.SquareRendering;
import dev.iammats.squarerendering.config.ConfigStore;
import dev.iammats.squarerendering.network.SquareRequestPayload;
import dev.iammats.squarerendering.network.SquareStatusPayload;
import java.io.IOException;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

/** Owns the local preference and negotiates optional server support. */
@SuppressWarnings("null")
public final class SquareRenderingClient implements ClientModInitializer {
  private static final ConfigStore STORE =
      new ConfigStore(FabricLoader.getInstance().getConfigDir().resolve("square-rendering.json"));
  private static volatile boolean enabled = STORE.load();
  private static int approvedRadius = -1;

  public static boolean isEnabled() {
    return enabled;
  }

  public static void setEnabled(boolean value) {
    enabled = value;
  }

  /** Combines the selected distance with normal server limits and the optional acknowledgment. */
  public static int effectiveRadius() {
    int radius = Minecraft.getInstance().options.getEffectiveRenderDistance();
    return approvedRadius >= 0 ? Math.min(radius, approvedRadius) : radius;
  }

  /** Persists the applied preference through Sodium's storage handler. */
  public static void save() {
    try {
      STORE.save(enabled);
    } catch (IOException exception) {
      SquareRendering.LOGGER.error("Could not save the Square Rendering preference", exception);
    }
  }

  /** Sends a preference only to servers advertising this mod's channel. */
  public static void sendPreference() {
    if (ClientPlayNetworking.canSend(SquareRequestPayload.TYPE)) {
      ClientPlayNetworking.send(new SquareRequestPayload(enabled));
    }
  }

  @Override
  public void onInitializeClient() {
    ClientRequirements.verify();
    try {
      STORE.createIfMissing(enabled);
    } catch (IOException exception) {
      SquareRendering.LOGGER.warn("Could not create Square Rendering config", exception);
    }
    ClientPlayNetworking.registerGlobalReceiver(
        SquareStatusPayload.TYPE,
        (payload, context) -> {
          if (payload.radius() >= 2 && payload.radius() <= 32) {
            approvedRadius = payload.radius();
            context.client().levelRenderer.needsUpdate();
          }
        });
    ClientPlayConnectionEvents.JOIN.register(
        (handler, sender, client) -> {
          approvedRadius = -1;
          sendPreference();
        });
    ClientPlayConnectionEvents.DISCONNECT.register(
        (handler, client) -> {
          approvedRadius = -1;
          RenderBounds.clear();
        });
  }
}
