package dev.iammats.squarerendering.test;

import dev.iammats.squarerendering.SquareRendering;
import dev.iammats.squarerendering.client.RenderBounds;
import dev.iammats.squarerendering.client.SquareRenderingClient;
import dev.iammats.squarerendering.geometry.ChunkSquare;
import dev.iammats.squarerendering.server.SquareTrackingView;
import net.caffeinemc.mods.sodium.client.config.ConfigManager;
import net.caffeinemc.mods.sodium.client.config.structure.BooleanOption;
import net.caffeinemc.mods.sodium.client.config.structure.OptionPage;
import net.caffeinemc.mods.sodium.client.gui.VideoSettingsScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.MixinEnvironment;

/** Exercises the real client, integrated server, networking, and shader reload paths. */
public final class SquareRenderingGameTest implements FabricClientGameTest {
  @Override
  public void runTest(ClientGameTestContext context) {
    if (System.getProperty("square-rendering.tests.server") != null) {
      ExternalServerCheck.run(context);
      return;
    }
    context.runOnClient(
        client -> {
          client.options.renderDistance().set(16);
          client.options.simulationDistance().set(8);
          SquareRenderingClient.setEnabled(true);
        });
    context.setScreen(
        () ->
            VideoSettingsScreen.createScreen(
                null,
                (OptionPage)
                    ConfigManager.CONFIG.getModOptions().stream()
                        .filter(mod -> mod.configId().equals("square-rendering"))
                        .findFirst()
                        .orElseThrow()
                        .pages()
                        .getFirst()));
    context.waitTicks(2);
    context.takeScreenshot("sodium-settings");
    context.runOnClient(
        client -> {
          var option = option();
          option.modifyValue(false);
          check(SquareRenderingClient.isEnabled(), "Pending changes were applied early");
          ConfigManager.CONFIG.resetAllOptionsFromBindings();
          check(option.getValidatedValue(), "Undo failed to discard a pending change");
          option.modifyValue(false);
          option.resetToDefault();
          check(option.getValidatedValue(), "Reset failed to select the enabled default");
        });
    context.setScreen(() -> null);

    try (var world = context.worldBuilder().create()) {
      TestServerContext server = world.getServer();
      server.runCommand("gamemode spectator @a");
      server.runCommand("tp @a 0.5 5 0.5 -45 15");
      awaitSquare(context, 16);
      awaitServerShape(context, server, true, 16);
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
      awaitServerShape(context, server, false, 16);
      context.waitFor(
          client -> client.level.getChunk(16, 16, ChunkStatus.FULL, false) == null, 200);
      context.takeScreenshot("original-distance-16");

      toggle(context, true);
      awaitSquare(context, 16);
      server.runCommand("tp @a -16.5 5 -16.5 -45 15");
      context.waitFor(
          client -> client.player.chunkPosition().x == -2 && client.player.chunkPosition().z == -2);
      awaitSquare(context, 16);
      awaitServerShape(context, server, true, 16);

      context.runOnClient(
          client -> {
            client.options.renderDistance().set(4);
            client.options.broadcastOptions();
          });
      awaitSquare(context, 4);
      awaitServerShape(context, server, true, 4);

      server.runCommand("execute in minecraft:the_nether run tp @a 0.5 80 0.5");
      context.waitFor(
          client -> client.level.dimension().equals(net.minecraft.world.level.Level.NETHER));
      awaitSquare(context, 4);
      awaitServerShape(context, server, true, 4);
      server.runCommand("execute in minecraft:overworld run tp @a 0.5 5 0.5");
      context.waitFor(
          client -> client.level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD));
      awaitSquare(context, 4);
      checkEnvironmentalFog(context, server);
      final var oldPlayer = context.computeOnClient(client -> client.player);
      server.runCommand("kill @a");
      context.waitFor(client -> client.player.isDeadOrDying());
      context.runOnClient(client -> client.player.respawn());
      context.waitFor(client -> client.player != oldPlayer && !client.player.isDeadOrDying());
      context.setScreen(() -> null);
      awaitSquare(context, 4);
      awaitServerShape(context, server, true, 4);
      MixinEnvironment.getCurrentEnvironment().audit();
    }

    // Fabric's test dedicated server runs in this client JVM. A separate runServer smoke test
    // is needed to verify the dedicated-only classpath without Sodium.
    try (var server = context.worldBuilder().createServer()) {
      try (var connection = server.connect()) {
        server.runCommand("gamemode spectator @a");
        server.runCommand("tp @a 0.5 5 0.5 -45 15");
        awaitSquare(context, 4);
        awaitServerShape(context, server, true, 4);
        server.runOnServer(instance -> instance.getPlayerList().setViewDistance(3));
        awaitSquare(context, 3);
        awaitServerShape(context, server, true, 3);
        toggle(context, false);
        awaitServerShape(context, server, false, 3);
      }
    }
  }

  private static void toggle(ClientGameTestContext context, boolean enabled) {
    var reload =
        context.computeOnClient(
            client -> {
              ConfigManager.CONFIG.resetAllOptionsFromBindings();
              option().modifyValue(enabled);
              ConfigManager.CONFIG.applyAllOptions();
              check(
                  SquareRenderingClient.isEnabled() == enabled, "Apply did not update the binding");
              return client.delayTextureReload();
            });
    context.waitFor(client -> reload.isDone(), 1200);
    reload.join();
    context.waitFor(
        client -> client.getOverlay() == null && client.levelRenderer.hasRenderedAllSections(),
        6000);
    context.waitTicks(4);
  }

  private static BooleanOption option() {
    return (BooleanOption) ConfigManager.CONFIG.getOption(SquareRendering.id("enabled"));
  }

  private static void checkEnvironmentalFog(
      ClientGameTestContext context, TestServerContext server) {
    server.runCommand("gamemode creative @a");
    server.runCommand("fill -3 0 -3 3 8 3 minecraft:water");
    context.waitFor(
        client -> client.gameRenderer.getMainCamera().getFluidInCamera() == FogType.WATER);
    context.waitTicks(20);
    context.takeScreenshot("water-fog");
    // Use a separate pool: replacing water directly lets block updates turn lava into stone.
    server.runCommand("fill 29 0 -3 35 8 3 minecraft:lava");
    server.runCommand("tp @a 32.5 5 0.5 -45 15");
    context.waitFor(
        client -> client.gameRenderer.getMainCamera().getFluidInCamera() == FogType.LAVA);
    context.waitTicks(20);
    context.takeScreenshot("lava-fog");
    server.runCommand("fill -4 0 -4 4 10 4 minecraft:air");
    server.runCommand("fill 28 0 -4 36 10 4 minecraft:air");
    server.runCommand("tp @a 0.5 5 0.5 -45 15");
    server.runCommand("effect give @a minecraft:blindness 30 0 true");
    context.waitFor(client -> client.player.hasEffect(MobEffects.BLINDNESS));
    context.waitTicks(20);
    context.takeScreenshot("blindness-fog");
    server.runCommand("effect clear @a");
    server.runCommand("effect give @a minecraft:darkness 30 0 true");
    context.waitFor(client -> client.player.hasEffect(MobEffects.DARKNESS));
    context.waitTicks(40);
    context.takeScreenshot("darkness-fog");
    server.runCommand("effect clear @a");
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

  private static void awaitServerShape(
      ClientGameTestContext context, TestServerContext server, boolean enabled, int radius) {
    // Client distance changes take effect locally before the server receives the options packet.
    for (int tick = 0; tick < 200; tick++) {
      boolean matches =
          server.computeOnServer(
              instance -> {
                var player = instance.getPlayerList().getPlayers().getFirst();
                ChunkTrackingView view = player.getChunkTrackingView();
                if ((view instanceof SquareTrackingView) != enabled) {
                  return false;
                }
                if (view instanceof SquareTrackingView square) {
                  var center = player.chunkPosition();
                  return square.visibleSquare().equals(new ChunkSquare(center.x, center.z, radius));
                }
                return true;
              });
      if (matches) {
        return;
      }
      context.waitTick();
    }
    throw new AssertionError(
        "Server did not reach requested tracking shape/radius: " + enabled + "/" + radius);
  }

  private static void check(boolean condition, String message) {
    if (!condition) {
      throw new AssertionError(message);
    }
  }
}
