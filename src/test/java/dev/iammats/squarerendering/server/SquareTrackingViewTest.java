package dev.iammats.squarerendering.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.iammats.squarerendering.geometry.ChunkSquare;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SquareTrackingViewTest {
  @BeforeAll
  static void bootstrapMinecraft() {
    SharedConstants.tryDetectVersion();
    Bootstrap.bootStrap();
  }

  @Test
  void deliversTheWholeSquareAndItsMeshingBorder() {
    SquareTrackingView view = square(0, 0, 16);
    assertTrue(view.contains(16, 16, false));
    assertFalse(view.contains(17, 17, false));
    assertTrue(view.contains(17, 17));
    assertFalse(view.contains(18, 0));
    assertEquals(1225, columns(view).size());
    assertFalse(ChunkTrackingView.of(new ChunkPos(0, 0), 16).contains(16, 16));
  }

  @Test
  void movingOneChunkOnlyReplacesOneStrip() {
    Set<ChunkPos> added = new HashSet<>();
    Set<ChunkPos> removed = new HashSet<>();
    SquareTrackingView.difference(square(0, 0, 16), square(1, 0, 16), added::add, removed::add);
    assertEquals(35, added.size());
    assertEquals(35, removed.size());
    assertTrue(added.stream().allMatch(pos -> pos.x == 18));
    assertTrue(removed.stream().allMatch(pos -> pos.x == -17));
  }

  @Test
  void differencesPreserveUnchangedChunksAcrossAllTransitions() {
    ChunkTrackingView[] views = {
      ChunkTrackingView.EMPTY,
      square(0, 0, 16),
      square(-1, -1, 16),
      square(0, 0, 4),
      square(2000, -2000, 16),
      ChunkTrackingView.of(new ChunkPos(0, 0), 16)
    };
    for (ChunkTrackingView previous : views) {
      for (ChunkTrackingView next : views) {
        Set<ChunkPos> oldChunks = columns(previous);
        Set<ChunkPos> newChunks = columns(next);
        Set<ChunkPos> added = new HashSet<>();
        Set<ChunkPos> removed = new HashSet<>();
        SquareTrackingView.difference(
            previous, next, pos -> assertTrue(added.add(pos)), pos -> assertTrue(removed.add(pos)));
        Set<ChunkPos> expectedAdded = new HashSet<>(newChunks);
        expectedAdded.removeAll(oldChunks);
        Set<ChunkPos> expectedRemoved = new HashSet<>(oldChunks);
        expectedRemoved.removeAll(newChunks);
        assertEquals(expectedAdded, added);
        assertEquals(expectedRemoved, removed);
      }
    }
  }

  private static SquareTrackingView square(int x, int z, int radius) {
    return new SquareTrackingView(new ChunkSquare(x, z, radius));
  }

  private static Set<ChunkPos> columns(ChunkTrackingView view) {
    Set<ChunkPos> result = new HashSet<>();
    view.forEach(pos -> assertTrue(result.add(pos)));
    return result;
  }
}
