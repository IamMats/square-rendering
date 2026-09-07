package dev.iammats.squarerendering.geometry;

/** An inclusive square of chunk columns, independent of camera position and rotation. */
public record ChunkSquare(int centerX, int centerZ, int radius) {
  /** Rejects negative radii instead of creating an inverted footprint. */
  public ChunkSquare {
    if (radius < 0) {
      throw new IllegalArgumentException("Chunk radius must be nonnegative");
    }
  }

  /** Tests inclusive membership using long arithmetic for coordinate differences. */
  public boolean contains(int x, int z) {
    return Math.abs((long) x - centerX) <= radius && Math.abs((long) z - centerZ) <= radius;
  }

  /** Adds the single chunk of neighbor data needed to mesh the visible edge. */
  public ChunkSquare withBorder() {
    return new ChunkSquare(centerX, centerZ, Math.addExact(radius, 1));
  }

  /** Returns the total number of columns, including the center. */
  public long columnCount() {
    long width = 2L * radius + 1;
    return width * width;
  }

  /** Visits each column exactly once. */
  public void forEach(ChunkConsumer consumer) {
    rectangle(centerX - radius, centerZ - radius, centerX + radius, centerZ + radius, consumer);
  }

  /** Visits only columns in this square that are absent from the other square. */
  public void forEachOutside(ChunkSquare other, ChunkConsumer consumer) {
    int minX = centerX - radius;
    int maxX = centerX + radius;
    int minZ = centerZ - radius;
    int maxZ = centerZ + radius;
    int overlapMinX = Math.max(minX, other.centerX - other.radius);
    int overlapMaxX = Math.min(maxX, other.centerX + other.radius);
    int overlapMinZ = Math.max(minZ, other.centerZ - other.radius);
    int overlapMaxZ = Math.min(maxZ, other.centerZ + other.radius);
    if (overlapMinX > overlapMaxX || overlapMinZ > overlapMaxZ) {
      forEach(consumer);
      return;
    }

    rectangle(minX, minZ, overlapMinX - 1, maxZ, consumer);
    rectangle(overlapMaxX + 1, minZ, maxX, maxZ, consumer);
    rectangle(overlapMinX, minZ, overlapMaxX, overlapMinZ - 1, consumer);
    rectangle(overlapMinX, overlapMaxZ + 1, overlapMaxX, maxZ, consumer);
  }

  private static void rectangle(int minX, int minZ, int maxX, int maxZ, ChunkConsumer consumer) {
    for (int x = minX; x <= maxX; x++) {
      for (int z = minZ; z <= maxZ; z++) {
        consumer.accept(x, z);
      }
    }
  }

  /** Receives primitive chunk coordinates without allocating position objects. */
  @FunctionalInterface
  public interface ChunkConsumer {
    /** Accepts one column's X and Z chunk coordinates. */
    void accept(int x, int z);
  }
}
