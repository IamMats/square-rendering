package dev.iammats.squarerendering.server;

import dev.iammats.squarerendering.geometry.ChunkSquare;
import java.util.function.Consumer;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.world.level.ChunkPos;

/** Immutable tracking shape; the additional border supplies neighbors for client meshing. */
@SuppressWarnings("null")
public record SquareTrackingView(ChunkSquare visibleSquare) implements ChunkTrackingView {
  /** Returns the transmitted footprint, including its invisible support border. */
  public ChunkSquare deliverySquare() {
    return visibleSquare.withBorder();
  }

  /** Returns the center for the client's chunk cache packet. */
  public ChunkPos center() {
    return new ChunkPos(visibleSquare.centerX(), visibleSquare.centerZ());
  }

  @Override
  public boolean contains(int x, int z, boolean includeEdge) {
    int radius = visibleSquare.radius() + (includeEdge ? 1 : 0);
    return Math.abs((long) x - visibleSquare.centerX()) <= radius
        && Math.abs((long) z - visibleSquare.centerZ()) <= radius;
  }

  @Override
  public void forEach(Consumer<ChunkPos> consumer) {
    deliverySquare().forEach((x, z) -> consumer.accept(new ChunkPos(x, z)));
  }

  /** Applies only actual membership changes, including transitions to and from vanilla views. */
  public static void difference(
      ChunkTrackingView previous,
      ChunkTrackingView next,
      Consumer<ChunkPos> added,
      Consumer<ChunkPos> removed) {
    if (previous.equals(next)) {
      return;
    }
    if (previous instanceof SquareTrackingView oldSquare
        && next instanceof SquareTrackingView newSquare) {
      ChunkSquare oldDelivery = oldSquare.deliverySquare();
      ChunkSquare newDelivery = newSquare.deliverySquare();
      oldDelivery.forEachOutside(newDelivery, (x, z) -> removed.accept(new ChunkPos(x, z)));
      newDelivery.forEachOutside(oldDelivery, (x, z) -> added.accept(new ChunkPos(x, z)));
      return;
    }
    previous.forEach(
        pos -> {
          if (!next.contains(pos)) {
            removed.accept(pos);
          }
        });
    next.forEach(
        pos -> {
          if (!previous.contains(pos)) {
            added.accept(pos);
          }
        });
  }
}
