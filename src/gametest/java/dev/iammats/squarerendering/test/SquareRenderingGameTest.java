package dev.iammats.squarerendering.test;

import dev.iammats.squarerendering.client.RenderBounds;
import dev.iammats.squarerendering.client.SquareRenderingClient;
import dev.iammats.squarerendering.geometry.ChunkSquare;
import dev.iammats.squarerendering.server.SquareTrackingView;
import net.caffeinemc.mods.sodium.client.gui.VideoSettingsScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.MixinEnvironment;

/** Exercises the real client, integrated server, networking, and shader reload paths. */
public final class SquareRenderingGameTest implements FabricClientGameTest {
  @Override
  public void runTest(ClientGameTestContext context) {
    context.runOnClient(
        client -> {
          client.options.renderDistance().set(16);
          client.options.simulationDistance().set(8);
          SquareRenderingClient.setEnabled(true);
        });
    context.setScreen(() -> VideoSettingsScreen.createScreen(null));
    context.waitTicks(2);
    context.takeScreenshot("sodium-settings");
    context.setScreen(() -> null);

    try (var world = context.worldBuilder().create()) {
      TestServerContext server = world.getServer();
      server.runCommand("gamemode spectator @a");
      server.runCommand("tp @a 0.5 5 0.5 -45 15");
      awaitSquare(context, 16);
      assertServerShape(server, true, 16);
      context.runOnClient(
          client -> {
            check(client.options.simulationDistance().get() == 8, "Simulation distance changed");
            check(RenderBounds.includesColumn(16, 16), "Corner excluded by render bounds");
            check(!RenderBounds.includesColumn(17, 17), "Support border leaked into rendering");
          });
      context.waitTicks(40);
      context.takeScreenshot("square-distance-16");

      server.runCommand("setblock 256 0 256 minecraft:gold_block");
      context.waitFor(
          client -> client.level.getBlockState(new BlockPos(256, 0, 256)).is(Blocks.GOLD_BLOCK),
          200);

      context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_BACK));
      context.waitTicks(2);
      context.takeScreenshot("square-third-person");
      context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));

      toggle(context, false);
      assertServerShape(server, false, 16);
      context.waitFor(
          client -> client.level.getChunk(16, 16, ChunkStatus.FULL, false) == null, 200);
      context.takeScreenshot("original-distance-16");

      toggle(context, true);
      awaitSquare(context, 16);
      server.runCommand("tp @a -16.5 5 -16.5 -45 15");
      awaitSquare(context, 16);
      assertServerShape(server, true, 16);

      context.runOnClient(
          client -> {
            client.options.renderDistance().set(4);
            client.options.broadcastOptions();
          });
      awaitSquare(context, 4);
      assertServerShape(server, true, 4);
      MixinEnvironment.getCurrentEnvironment().audit();
    }

    // The spawned dedicated server uses Loom's server classpath, without Sodium's client
    // dependency.
    try (var server = context.worldBuilder().createServer()) {
      try (var connection = server.connect()) {
        server.runCommand("gamemode spectator @a");
        server.runCommand("tp @a 0.5 5 0.5 -45 15");
        awaitSquare(context, 4);
        assertServerShape(server, true, 4);
        toggle(context, false);
        assertServerShape(server, false, 4);
      }
    }
  }

  private static void toggle(ClientGameTestContext context, boolean enabled) {
    var reload =
        context.computeOnClient(
            client -> {
              SquareRenderingClient.setEnabled(enabled);
              SquareRenderingClient.save();
              SquareRenderingClient.sendPreference();
              return client.reloadResourcePacks();
            });
    context.waitFor(client -> reload.isDone(), 1200);
    reload.join();
    context.waitTicks(4);
  }

  private static void awaitSquare(ClientGameTestContext context, int radius) {
    context.waitFor(
        client -> {
          if (client.player == null || SquareRenderingClient.effectiveRadius() != radius) {
            return false;
          }
          var center = client.player.chunkPosition();
          for (int x = -radius - 1; x <= radius + 1; x++) {
            for (int z = -radius - 1; z <= radius + 1; z++) {
              if (client.level.getChunk(center.x + x, center.z + z, ChunkStatus.FULL, false)
                  == null) {
                return false;
              }
            }
          }
          return true;
        },
        6000);
    context.waitTicks(2);
  }

  private static void assertServerShape(TestServerContext server, boolean enabled, int radius) {
    server.runOnServer(
        instance -> {
          var player = instance.getPlayerList().getPlayers().getFirst();
          ChunkTrackingView view = player.getChunkTrackingView();
          check((view instanceof SquareTrackingView) == enabled, "Wrong server tracking shape");
          if (view instanceof SquareTrackingView square) {
            var center = player.chunkPosition();
            check(
                square.visibleSquare().equals(new ChunkSquare(center.x, center.z, radius)),
                "Wrong square center or radius");
          }
        });
  }

  private static void check(boolean condition, String message) {
    if (!condition) {
      throw new AssertionError(message);
    }
  }
}
