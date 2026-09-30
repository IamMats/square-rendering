package dev.iammats.squarerendering.mixin;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Refreshes tracking immediately when a preference packet arrives. */
@Mixin(ChunkMap.class)
public interface ChunkMapAccess {
  /** Runs vanilla's tracking update, including this mod's per-player shape selection. */
  @Invoker("updateChunkTracking")
  void squareRenderingUpdateTracking(ServerPlayer player);
}
