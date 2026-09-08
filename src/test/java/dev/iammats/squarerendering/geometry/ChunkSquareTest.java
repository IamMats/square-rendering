package dev.iammats.squarerendering.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ChunkSquareTest {
  @ParameterizedTest
  @ValueSource(ints = {0, 2, 4, 16, 32})
  void includesEveryColumnAndExcludesSupportBorder(int radius) {
    ChunkSquare square = new ChunkSquare(-3, 7, radius);
    Set<String> columns = columns(square);
    assertEquals((2 * radius + 1) * (2 * radius + 1), columns.size());
    assertEquals(columns.size(), square.columnCount());
    for (int x = -1; x <= 1; x += 2) {
      for (int z = -1; z <= 1; z += 2) {
        assertTrue(square.contains(-3 + x * radius, 7 + z * radius));
        assertFalse(square.contains(-3 + x * (radius + 1), 7 + z * radius));
      }
    }
    assertEquals(1089, new ChunkSquare(0, 0, 16).columnCount());
  }

  @Test
  void negativeChunkBoundariesUseFloorDivision() {
    assertEquals(-1, Math.floorDiv(-1, 16));
    assertEquals(-1, Math.floorDiv(-16, 16));
    assertEquals(-2, Math.floorDiv(-17, 16));
    ChunkSquare before = new ChunkSquare(Math.floorDiv(-16, 16), 0, 16);
    ChunkSquare after = new ChunkSquare(Math.floorDiv(-17, 16), 0, 16);
    assertTrue(before.contains(15, 0));
    assertFalse(after.contains(15, 0));
    assertTrue(after.contains(-18, 0));
  }

  @Test
  void membershipDoesNotOverflowWhenCoordinatesAreFarApart() {
    assertFalse(new ChunkSquare(Integer.MIN_VALUE, 0, 16).contains(Integer.MAX_VALUE, 0));
  }

  @Test
  void stripDifferencesMatchSetSubtractionAcrossMovementTeleportsAndResizing() {
    Random random = new Random(12111);
    for (int trial = 0; trial < 200; trial++) {
      ChunkSquare oldSquare =
          new ChunkSquare(random.nextInt(101) - 50, random.nextInt(101) - 50, random.nextInt(33));
      ChunkSquare newSquare =
          new ChunkSquare(random.nextInt(101) - 50, random.nextInt(101) - 50, random.nextInt(33));
      Set<String> expected = columns(oldSquare);
      expected.removeAll(columns(newSquare));
      Set<String> actual = new HashSet<>();
      oldSquare.forEachOutside(newSquare, (x, z) -> assertTrue(actual.add(x + "," + z)));
      assertEquals(expected, actual);
    }
  }

  private static Set<String> columns(ChunkSquare square) {
    Set<String> columns = new HashSet<>();
    square.forEach((x, z) -> assertTrue(columns.add(x + "," + z)));
    return columns;
  }
}
