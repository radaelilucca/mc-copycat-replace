package com.radaeli.copycatreplace.tests;
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="copycat_replace_tests")
public final class ServerTestPlatform {
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void tick(net.minecraftforge.event.TickEvent.ServerTickEvent e) { if(e.phase==net.minecraftforge.event.TickEvent.Phase.END) MultiplayerValidation.tick(e.getServer()); }
}
