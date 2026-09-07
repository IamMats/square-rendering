package dev.iammats.squarerendering.client;

import dev.iammats.squarerendering.geometry.ChunkSquare;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.viewport.CameraTransform;
import net.minecraft.client.Minecraft;

/** Render-thread snapshot shared by all of Sodium's terrain collection paths. */
public final class RenderBounds {
  private static ChunkSquare square;
  private static CameraTransform camera;
  private static float searchDistance;

  private RenderBounds() {}

  /** Captures player-centered bounds before Sodium updates or submits any terrain meshes. */
  public static void capture(double cameraX, double cameraY, double cameraZ) {
    var player = Minecraft.getInstance().player;
    if (!SquareRenderingClient.isEnabled() || player == null) {
      clear();
      return;
    }
    var center = player.chunkPosition();
    int radius = SquareRenderingClient.effectiveRadius();
    square = new ChunkSquare(center.x, center.z, radius);
    camera = new CameraTransform(cameraX, cameraY, cameraZ);

    // Include the farthest possible corner, the model margin, and displaced third-person cameras.
    double farX =
        Math.max(
            Math.abs((center.x - radius) * 16.0 - 1 - cameraX),
            Math.abs((center.x + radius + 1) * 16.0 + 1 - cameraX));
    double farZ =
        Math.max(
            Math.abs((center.z - radius) * 16.0 - 1 - cameraZ),
            Math.abs((center.z + radius + 1) * 16.0 + 1 - cameraZ));
    searchDistance = (float) Math.hypot(farX, farZ) + 1.0f;
  }

  /** Clears the snapshot when disabled or outside a world. */
  public static void clear() {
    square = null;
    camera = null;
  }

  /** Returns whether a square is active for the current render frame. */
  public static boolean active() {
    return square != null && SquareRenderingClient.isEnabled();
  }

  /** Returns a conservative traversal radius containing every corner of the square. */
  public static float searchDistance() {
    return searchDistance;
  }

  /** Tests horizontal membership without changing block entities' own vertical limits. */
  public static boolean includesColumn(int x, int z) {
    return !active() || square.contains(x, z);
  }

  /** Tests exact column membership and Sodium's original vertical distance limit. */
  public static boolean includes(RenderSection section) {
    if (!active()) {
      return true;
    }
    if (!square.contains(section.getChunkX(), section.getChunkZ())) {
      return false;
    }
    // Match Sodium's original padded-box vertical distance test despite the wider search radius.
    int originY = section.getOriginY() - camera.intY;
    int nearestY = Math.min(Math.max(0, originY - 1), originY + 17);
    return Math.abs(nearestY - camera.fracY) < square.radius() * 16.0f;
  }
}
