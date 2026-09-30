package dev.iammats.squarerendering.client;

import net.fabricmc.loader.api.FabricLoader;

/** Checks the client dependency without resolving any Sodium classes on the server. */
public final class ClientRequirements {
  private ClientRequirements() {}

  /** Checks the precise Sodium version targeted by the mixins. */
  public static boolean hasSupportedSodium() {
    return FabricLoader.getInstance()
        .getModContainer("sodium")
        .map(
            mod ->
                mod.getMetadata().getVersion().getFriendlyString().split("\\+")[0].equals("0.9.2"))
        .orElse(false);
  }

  /** Reports an actionable dependency error during client initialization. */
  public static void verify() {
    if (!hasSupportedSodium()) {
      throw new IllegalStateException(
          "Square Rendering requires Sodium 0.9.2 for Minecraft 26.3 on the client. "
              + "Install that Sodium version alongside Square Rendering. "
              + "Dedicated servers do not need Sodium.");
    }
  }
}
