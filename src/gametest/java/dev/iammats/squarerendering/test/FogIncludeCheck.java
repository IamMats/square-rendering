package dev.iammats.squarerendering.test;

import com.mojang.renderpearl.api.pipeline.ShaderSource;
import java.io.IOException;
import java.io.UncheckedIOException;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.Identifier;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.shaderc.ShadercIncludeResult;

/** Checks real Minecraft and Sodium resources after the native shader-include caching hook. */
final class FogIncludeCheck {
  private FogIncludeCheck() {}

  static void run(ClientGameTestContext context, boolean enabled) {
    context.runOnClient(
        client -> {
          var resources = client.getResourceManager();
          var includes = ShaderManager.listAllIncludes(resources);
          try {
            for (String namespace : new String[] {"minecraft", "sodium"}) {
              var id = Identifier.fromNamespaceAndPath(namespace, "fog.glsl");
              var include = includes.get(id);
              if (include == null) {
                throw new AssertionError("Missing fog include: " + id);
              }
              var result = ShadercIncludeResult.create(include.includeResultPtr());
              // RenderPearl stores length-delimited UTF-8, without a terminating NUL byte.
              String cached =
                  MemoryUtil.memUTF8(
                      MemoryUtil.memGetAddress(result.address() + ShadercIncludeResult.CONTENT),
                      Math.toIntExact(result.content_length()));
              String original =
                  resources
                      .getResourceOrThrow(id.withPrefix("shaders/include/"))
                      .readAllAsString();
              if (!enabled) {
                if (!cached.equals(original)) {
                  throw new AssertionError("Disabled fog differs from the original: " + id);
                }
                continue;
              }
              boolean minecraft = namespace.equals("minecraft");
              String square =
                  minecraft
                      ? "max(abs(pos.x), abs(pos.z))"
                      : "max(abs(position.x), abs(position.z))";
              String circular = minecraft ? "length(pos.xz)" : "length(position.xz)";
              String spherical = minecraft ? "length(pos)" : "length(position)";
              if (!cached.contains(square)
                  || cached.contains(circular)
                  || !cached.contains(spherical)) {
                throw new AssertionError("Incorrect square/environmental fog include: " + id);
              }
            }
          } catch (IOException exception) {
            throw new UncheckedIOException(exception);
          } finally {
            includes.values().forEach(ShaderSource.CachedIncludeSource::close);
          }
        });
  }
}
