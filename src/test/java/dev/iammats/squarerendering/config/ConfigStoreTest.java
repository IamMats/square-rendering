package dev.iammats.squarerendering.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ConfigStoreTest {
  @TempDir Path directory;

  @Test
  void createsDefaultAndPersistsBothToggleValues() throws IOException {
    Path path = directory.resolve("nested/square-rendering.json");
    ConfigStore store = new ConfigStore(path);
    assertTrue(store.load());
    store.createIfMissing(true);
    store.save(false);
    assertFalse(new ConfigStore(path).load());
    store.createIfMissing(true);
    assertFalse(store.load());
    store.save(true);
    assertTrue(new ConfigStore(path).load());
    try (var files = Files.list(path.getParent())) {
      assertEquals(1, files.count());
    }
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "{broken",
        "[]",
        "null",
        "{\"enabled\":\"false\"}",
        "{\"enabled\":null}",
        "{\"enabled\":0}"
      })
  void malformedConfigFallsBackWithoutOverwritingOriginal(String text) throws IOException {
    Path path = directory.resolve("square-rendering.json");
    Files.writeString(path, text);
    ConfigStore store = new ConfigStore(path);
    assertTrue(store.load());
    store.createIfMissing(true);
    assertEquals(text, Files.readString(path));
  }

  @Test
  void missingOptionDefaultsToEnabled() throws IOException {
    Path path = directory.resolve("square-rendering.json");
    Files.writeString(path, "{\"unrelated\":42}");
    assertTrue(new ConfigStore(path).load());
  }
}
