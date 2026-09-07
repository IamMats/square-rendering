package dev.iammats.squarerendering.client;

import dev.iammats.squarerendering.SquareRendering;
import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.option.OptionFlag;
import net.caffeinemc.mods.sodium.api.config.option.OptionImpact;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.minecraft.network.chat.Component;

/** Uses Sodium's public settings API, including its normal apply and discard behavior. */
@SuppressWarnings("null")
public final class SodiumOptions implements ConfigEntryPoint {
  @Override
  public void registerConfigLate(ConfigBuilder builder) {
    var option =
        builder
            .createBooleanOption(SquareRendering.id("enabled"))
            .setName(Component.translatable("square-rendering.option.enabled"))
            .setTooltip(Component.translatable("square-rendering.option.enabled.tooltip"))
            .setBinding(SquareRenderingClient::setEnabled, SquareRenderingClient::isEnabled)
            .setStorageHandler(SquareRenderingClient::save)
            .setDefaultValue(true)
            .setImpact(OptionImpact.HIGH)
            .setApplyHook(state -> SquareRenderingClient.sendPreference())
            .setFlags(OptionFlag.REQUIRES_ASSET_RELOAD);
    var group = builder.createOptionGroup().addOption(option);
    var page =
        builder
            .createOptionPage()
            .setName(Component.translatable("square-rendering.options.title"))
            .addOptionGroup(group);
    builder.registerOwnModOptions().addPage(page);
  }
}
