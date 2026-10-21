package com.radaeli.copycatreplace.tests;
import com.mojang.blaze3d.platform.InputConstants;
import com.radaeli.copycatreplace.client.ConnectedBulkIndicator;
import com.simibubi.create.content.decoration.copycat.CopycatBlockEntity;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.core.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import java.util.*;
import static com.radaeli.copycatreplace.tests.MultiplayerValidation.*;
public final class MultiplayerClientValidation {
    private static final Minecraft MC=Minecraft.getInstance();
    private static final InputConstants.Key KEY=InputConstants.getKey(298,0);
    private static String lastPhase="",clicked="";
    private static boolean connecting,finished;
    private static int ticks;
    public static void tick() {
        String role=System.getProperty("copycat_replace.multiplayer",""); if(role.isEmpty() || finished) return;
        try {
            if(role.equals("Observer")) {
                Arrays.stream(MC.options.keyMappings).filter(k->k.getName().equals("key.copycat_replace.bulk_replace")).findFirst().ifPresent(k -> {
                    k.setKey(InputConstants.UNKNOWN); k.setDown(false); KeyMapping.resetMapping();
                });
            }
            if(!read("result.txt").isEmpty()) { finished=true; MC.stop(); return; }
            String phase=read("phase");
            if(MC.player==null) {
                if(phase.equals("disconnect")) return;
                if(MC.screen!=null && MC.screen.getClass().getSimpleName().contains("AccessibilityOnboarding")) MC.setScreen(new TitleScreen());
                if(!connecting && (MC.screen instanceof TitleScreen || phase.equals("reconnect"))) {
                    MC.options.renderDistance().set(3); MC.options.simulationDistance().set(5); MC.options.guiScale().set(2); MC.options.pauseOnLostFocus=false;
                    MC.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
                    connecting=true; ClientTestPlatform.connect();
                }
                return;
            }
            connecting=false;
            if(!phase.equals(lastPhase)) { lastPhase=phase; ticks=0; }
            if(++ticks<25) return;
            boolean actor=role.equals("Actor");
            BlockPos target=phase.startsWith("multipart")?MULTIPART:TARGET;
            Vec3 v=Vec3.atLowerCornerOf(target).add(.5,.25,.5).subtract(MC.player.getEyePosition());
            MC.player.setYRot((float)Math.toDegrees(Math.atan2(-v.x,v.z))); MC.player.setXRot((float)-Math.toDegrees(Math.atan2(v.y,Math.sqrt(v.x*v.x+v.z*v.z))));
            var hit=new BlockHitResult(Vec3.atLowerCornerOf(target).add(.5,.25,.5),Direction.UP,target,false); MC.hitResult=hit;
            var mapping=Arrays.stream(MC.options.keyMappings).filter(k->k.getName().equals("key.copycat_replace.bulk_replace")).findFirst().orElseThrow();
            if(!actor) {
                // Ignore physical desktop shortcuts in the observer window during automation.
                mapping.setKey(InputConstants.UNKNOWN); mapping.setDown(false); KeyMapping.resetMapping();
                if(phase.equals("apply") && material(Blocks.STONE)) marker("observer.state","stone");
                if(phase.equals("replace") && material(Blocks.OAK_PLANKS)) marker("observer.state","oak");
                if(phase.equals("remove") && empty()) marker("observer.state","empty");
                if(phase.equals("reload") && limit()==1) marker("observer.state","limit1");
                if(phase.equals("multipart_replace") && MultiplayerCopycats.all(MC.level,MULTIPART,Blocks.OAK_PLANKS)) marker("observer.state","multipart_oak");
                if(phase.equals("multipart_remove") && MultiplayerCopycats.empty(MC.level,MULTIPART)) marker("observer.state","multipart_empty");
                if(ticks==30) capture("observer-"+phase+".png");
                return;
            }
            mapping.setKey(KEY); KeyMapping.resetMapping();
            if(phase.equals("reconnect")) { KeyMapping.set(KEY,false); if(limit()==1) marker("actor.state","reconnected"); return; }
            if(phase.equals("reload")) { if(limit()==1) marker("actor.state","limit1"); return; }
            if(phase.equals("release")) { KeyMapping.set(KEY,false); MC.setScreen(new PauseScreen(true)); marker("actor.state","screen"); return; }
            if(phase.equals("press_again")) { MC.setScreen(null); KeyMapping.set(KEY,true); return; }
            if(phase.equals("disconnect")) { ClientTestPlatform.disconnect(); KeyMapping.set(KEY,false); return; }
            if(!Set.of("apply","replace","remove","multipart_replace","multipart_remove").contains(phase)) return;
            KeyMapping.set(KEY,true);
            if(ticks<50 || clicked.equals(phase)) return;
            boolean removing=phase.endsWith("remove");
            if(removing && !MC.player.getOffhandItem().isEmpty()) return;
            if(phase.equals("apply") && (!MC.player.getOffhandItem().is(Items.STONE) || limit()!=3)) return;
            if(phase.contains("replace") && !MC.player.getOffhandItem().is(Items.OAK_PLANKS)) return;
            capture("actor-"+phase+".png");
            MC.gameMode.useItemOn(MC.player,InteractionHand.MAIN_HAND,hit); clicked=phase;
        } catch(Throwable e) { e.printStackTrace(); marker("result.txt","FAIL client "+role+" "+lastPhase+"\n"+e); finished=true; MC.stop(); }
    }
    private static int limit() {
        try { var f=ConnectedBulkIndicator.class.getDeclaredField("serverMaximum"); f.setAccessible(true); return f.getInt(null); }
        catch(Exception e) { throw new RuntimeException(e); }
    }
    private static boolean material(net.minecraft.world.level.block.Block block) {
        for(int i=0;i<3;i++) if(!(MC.level.getBlockEntity(TARGET.east(i)) instanceof CopycatBlockEntity be) || !be.getMaterial().is(block)) return false;
        return true;
    }
    private static boolean empty() {
        for(int i=0;i<3;i++) if(!(MC.level.getBlockEntity(TARGET.east(i)) instanceof CopycatBlockEntity be) || be.hasCustomMaterial()) return false;
        return true;
    }
    private static void capture(String name) { MC.getToasts().clear(); Screenshot.grab(MC.gameDirectory,name,MC.getMainRenderTarget(),ignored->{}); }
}
