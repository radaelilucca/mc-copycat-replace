package com.radaeli.copycatreplace.tests;

import com.radaeli.copycatreplace.config.CopycatReplaceConfig;
import com.radaeli.copycatreplace.network.BulkReplaceMode;
import com.radaeli.copycatreplace.service.MaterialReplacement;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.decoration.copycat.CopycatBlockEntity;
import net.minecraft.core.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import java.nio.file.*;

/** Test-only coordinator. Actions and world updates still travel over real Minecraft connections. */
public final class MultiplayerValidation {
    public static final BlockPos TARGET = new BlockPos(0,65,2), MULTIPART = TARGET.east(5);
    private static String phase = "waiting";
    private static int ticks, doneTicks;
    private static ServerPlayer departed;
    public static Path directory() { return Path.of(System.getProperty("copycat_replace.coordinator", "build/multiplayer")); }
    public static void marker(String name, String value) {
        try { Files.createDirectories(directory()); Files.writeString(directory().resolve(name),value); }
        catch(Exception e) { throw new RuntimeException(e); }
    }
    public static String read(String name) {
        try { Path p=directory().resolve(name); return Files.exists(p) ? Files.readString(p) : ""; }
        catch(Exception e) { throw new RuntimeException(e); }
    }
    private static boolean ack(String value) { return read("observer.state").equals(value); }
    private static void phase(String value) { phase=value; marker("phase",value); System.out.println("COPYCAT MULTIPLAYER: " + value); }
    public static void tick(MinecraftServer server) {
        if(!Boolean.getBoolean("copycat_replace.multiplayerServer")) return;
        if(phase.equals("done")) { if(++doneTicks>80) server.halt(false); return; }
        try {
            if(++ticks>7200) throw new IllegalStateException("Timed out in " + phase);
            ServerPlayer actor=server.getPlayerList().getPlayerByName("CopycatActor");
            ServerPlayer observer=server.getPlayerList().getPlayerByName("CopycatObserver");
            if(phase.equals("waiting")) {
                if(actor==null || observer==null) return;
                for(int x=-3;x<10;x++) for(int z=-3;z<6;z++) actor.serverLevel().setBlockAndUpdate(new BlockPos(x,64,z),Blocks.STONE.defaultBlockState());
                for(int i=0;i<4;i++) actor.serverLevel().setBlockAndUpdate(TARGET.east(i),AllBlocks.COPYCAT_STEP.getDefaultState());
                for(ServerPlayer p : new ServerPlayer[]{actor,observer}) {
                    p.setGameMode(GameType.CREATIVE); p.teleportTo(p==actor?.5:3.5,65,-.5);
                    p.getInventory().clearContent();
                }
                actor.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(AllItems.WRENCH.get()));
                equip(actor,new ItemStack(Items.STONE,8));
                if (CopycatReplaceConfig.MAX_CONNECTED_BLOCKS.get() != 3) throw new IllegalStateException("Coordinator must configure limit 3 before startup");
                TestPlatform.syncLimit(actor); TestPlatform.syncLimit(observer);
                phase("apply"); return;
            }
            if(phase.equals("reconnect")) {
                if(actor!=null && read("actor.state").equals("reconnected") && !BulkReplaceMode.isDown(actor)) {
                    marker("result.txt","PASS\nTwo real clients; apply/replace/remove; remote updates; per-player key; config reload; active-key logout/reconnect; optional multipart actions.\n");
                    phase("done");
                }
                return;
            }
            if(phase.equals("disconnect")) {
                if(actor==null) {
                    if(BulkReplaceMode.isDown(departed)) throw new IllegalStateException("Logout retained modifier state");
                    phase("reconnect");
                }
                return;
            }
            if(actor==null || observer==null) throw new IllegalStateException("Unexpected player disconnect in " + phase);
            if(BulkReplaceMode.isDown(observer)) throw new IllegalStateException("Actor modifier leaked to observer");
            switch(phase) {
                case "apply" -> {
                    if(material(actor,Blocks.STONE,3) && ack("stone")) {
                        if(!BulkReplaceMode.isDown(actor)) throw new IllegalStateException("Apply bypassed modifier channel");
                        equip(actor,new ItemStack(Items.OAK_PLANKS,8)); phase("replace");
                    }
                }
                case "replace" -> { if(material(actor,Blocks.OAK_PLANKS,3) && ack("oak")) { equip(actor,ItemStack.EMPTY); phase("remove"); } }
                case "remove" -> {
                    if(empty(actor) && ack("empty")) {
                        // The external coordinator edits the real file, exercising the normal watcher.
                        phase("reload");
                    }
                }
                case "reload" -> {
                    if(read("actor.state").equals("limit1") && ack("limit1")) {
                        if(TestPlatform.hasCopycats()) { MultiplayerCopycats.prepare(actor.serverLevel(),MULTIPART); equip(actor,new ItemStack(Items.OAK_PLANKS,8)); phase("multipart_replace"); }
                        else phase("release");
                    }
                }
                case "multipart_replace" -> {
                    if(MultiplayerCopycats.all(actor.serverLevel(),MULTIPART,Blocks.OAK_PLANKS) && ack("multipart_oak")) { equip(actor,ItemStack.EMPTY); phase("multipart_remove"); }
                }
                case "multipart_remove" -> { if(MultiplayerCopycats.empty(actor.serverLevel(),MULTIPART) && ack("multipart_empty")) phase("release"); }
                case "release" -> { if(!BulkReplaceMode.isDown(actor) && read("actor.state").equals("screen")) phase("press_again"); }
                case "press_again" -> { if(BulkReplaceMode.isDown(actor)) { departed=actor; phase("disconnect"); } }
            }
        } catch(Throwable e) {
            e.printStackTrace(); marker("result.txt","FAIL " + phase + "\n" + e); phase("done");
        }
    }
    private static void equip(ServerPlayer p,ItemStack stack) { p.setItemInHand(InteractionHand.OFF_HAND,stack); p.inventoryMenu.broadcastChanges(); }
    private static boolean material(ServerPlayer p,net.minecraft.world.level.block.Block block,int n) {
        for(int i=0;i<n;i++) if(!(p.serverLevel().getBlockEntity(TARGET.east(i)) instanceof CopycatBlockEntity be) || !be.getMaterial().is(block)) return false;
        return true;
    }
    private static boolean empty(ServerPlayer p) {
        for(int i=0;i<3;i++) if(!(p.serverLevel().getBlockEntity(TARGET.east(i)) instanceof CopycatBlockEntity be) || be.hasCustomMaterial()) return false;
        return true;
    }
}
