package dev.iammats.squarerendering.test;

import dev.iammats.squarerendering.client.SquareRenderingClient;
import dev.iammats.squarerendering.network.SquareRequestPayload;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.world.level.chunk.status.ChunkStatus;

/** Tests optional support against a separately launched dedicated server. */
final class ExternalServerCheck {
  private ExternalServerCheck() {}

  static void run(ClientGameTestContext context) {
    boolean installed = FabricLoader.getInstance().isModLoaded("square-rendering");
    boolean supported = !Boolean.getBoolean("square-rendering.tests.unsupportedServer");
    context.runOnClient(
        client -> {
          client.options.renderDistance().set(16);
          if (installed) {
            SquareRenderingClient.setEnabled(true);
          }
          String address = System.getProperty("square-rendering.tests.server");
          var data = new ServerData("Compatibility test", address, ServerData.Type.OTHER);
          ConnectScreen.startConnecting(
              client.screen, client, ServerAddress.parseString(address), data, false, null);
        });
    context.waitFor(
        client -> client.player != null && client.level != null && client.screen == null, 6000);
    context.waitFor(
        client -> {
          int radius = client.options.getEffectiveRenderDistance();
          var center = client.player.chunkPosition();
          if (radius < 8) {
            throw new AssertionError("Use a test server view-distance of at least 8");
          }
          for (int x = -radius - 1; x <= radius + 1; x++) {
            for (int z = -radius - 1; z <= radius + 1; z++) {
              if ((installed && supported)
                  || ChunkTrackingView.isWithinDistance(0, 0, radius, x, z, true)) {
                if (client.level.getChunk(center.x + x, center.z + z, ChunkStatus.FULL, false)
                    == null) {
                  return false;
                }
              }
            }
          }
          return true;
        },
        6000);
    context.runOnClient(
        client -> {
          int radius = client.options.getEffectiveRenderDistance();
          var center = client.player.chunkPosition();
          boolean cornerLoaded =
              client.level.getChunk(center.x + radius, center.z + radius, ChunkStatus.FULL, false)
                  != null;
          if (cornerLoaded != (installed && supported)) {
            throw new AssertionError(
                "Unexpected corner delivery for optional client/server support");
          }
          if (installed && ClientPlayNetworking.canSend(SquareRequestPayload.TYPE) != supported) {
            throw new AssertionError("Incorrect server capability detection");
          }
          if (client.options.renderDistance().get() != 16) {
            throw new AssertionError("Mod changed the client's normal requested distance");
          }
        });
    context.takeScreenshot("external-client-" + installed + "-server-" + supported);
    context.runOnClient(client -> client.disconnectFromWorld(Component.literal("Test complete")));
    context.waitFor(client -> client.level == null);
    context.setScreen(TitleScreen::new);
  }
}
