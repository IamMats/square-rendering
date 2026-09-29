package dev.iammats.squarerendering.test;

import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Screenshot;

/** Captures completed frames without rendering again after Sodium's end-of-frame cleanup. */
final class GameTestScreenshots {
  private GameTestScreenshots() {}

  static void take(ClientGameTestContext context, String name) {
    var saved = new CompletableFuture<Void>();
    context.runOnClient(
        client -> {
          var directory = client.gameDirectory.toPath().resolve("screenshots");
          Screenshot.takeScreenshot(
              client.getMainRenderTarget(),
              image -> {
                try (image) {
                  Files.createDirectories(directory);
                  image.writeToFile(directory.resolve(name + ".png"));
                  saved.complete(null);
                } catch (Exception exception) {
                  saved.completeExceptionally(exception);
                }
              });
        });
    context.waitFor(client -> saved.isDone());
    saved.join();
  }
}
