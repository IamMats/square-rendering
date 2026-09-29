package dev.iammats.squarerendering.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Stores the client preference without sharing mutable state with an integrated server. */
public final class ConfigStore {
  private static final Logger LOGGER = LoggerFactory.getLogger("Square Rendering");
  private final Path path;

  /** Creates a store at the given configuration file. */
  public ConfigStore(Path path) {
    this.path = path;
  }

  /** Loads the preference, preserving malformed files and defaulting to enabled. */
  public boolean load() {
    if (!Files.exists(path)) {
      return true;
    }
    try {
      JsonElement parsed = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8));
      if (!parsed.isJsonObject()) {
        throw new IllegalArgumentException("Expected a JSON object");
      }
      JsonObject object = parsed.getAsJsonObject();
      if (!object.has("enabled")) {
        return true;
      }
      JsonElement enabled = object.get("enabled");
      if (!enabled.isJsonPrimitive() || !enabled.getAsJsonPrimitive().isBoolean()) {
        throw new IllegalArgumentException("enabled must be a boolean");
      }
      return enabled.getAsBoolean();
    } catch (IOException | RuntimeException exception) {
      LOGGER.warn(
          "Cannot read {}; using enabled=true and preserving the original file", path, exception);
      return true;
    }
  }

  /** Writes first-run defaults without replacing an existing configuration. */
  public void createIfMissing(boolean enabled) throws IOException {
    if (!Files.exists(path)) {
      save(enabled);
    }
  }

  /** Replaces the configuration atomically when supported by the filesystem. */
  public void save(boolean enabled) throws IOException {
    Path directory = path.toAbsolutePath().getParent();
    Files.createDirectories(directory);
    Path temporary = Files.createTempFile(directory, "square-rendering-", ".tmp");
    try {
      Files.writeString(
          temporary, "{\n  \"enabled\": " + enabled + "\n}\n", StandardCharsets.UTF_8);
      try {
        Files.move(
            temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException exception) {
        Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
      }
    } finally {
      Files.deleteIfExists(temporary);
    }
  }
}
