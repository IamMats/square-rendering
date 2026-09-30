package dev.iammats.squarerendering.server;

import dev.iammats.squarerendering.network.SquareStatusPayload;

/** Connection preferences stored on the server player, never in the client's config singleton. */
public interface SquarePlayerState {
  /** Returns whether this player has requested square tracking. */
  boolean squareRenderingEnabled();

  /** Updates this player's preference on the server thread. */
  void squareRenderingSetEnabled(boolean enabled);

  /** Returns the last acknowledged state, or null before the initial acknowledgment. */
  SquareStatusPayload squareRenderingLastStatus();

  /** Remembers the last status to avoid sending redundant packets every tick. */
  void squareRenderingSetLastStatus(SquareStatusPayload status);
}
