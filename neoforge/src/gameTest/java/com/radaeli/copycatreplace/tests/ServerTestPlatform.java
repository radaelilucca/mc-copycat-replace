package com.radaeli.copycatreplace.tests;
@net.neoforged.fml.common.EventBusSubscriber(modid="copycat_replace_tests")
public final class ServerTestPlatform {
    @net.neoforged.bus.api.SubscribeEvent public static void tick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post e) { MultiplayerValidation.tick(e.getServer()); }
}
