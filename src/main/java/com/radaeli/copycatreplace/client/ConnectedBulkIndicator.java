package com.radaeli.copycatreplace.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.radaeli.copycatreplace.CopycatReplace;
import com.radaeli.copycatreplace.config.CopycatReplaceConfig;
import com.radaeli.copycatreplace.service.BulkMaterialReplacement;
import com.radaeli.copycatreplace.service.BulkMaterialReplacement.PreviewEstimate;
import com.simibubi.create.content.equipment.wrench.WrenchItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.List;

/** Shows the connected-action key state and a bounded client-side estimate. */
@EventBusSubscriber(modid = CopycatReplace.MOD_ID, value = Dist.CLIENT)
public final class ConnectedBulkIndicator {
    private static final int REFRESH_INTERVAL_TICKS = 5;
    private static final Component ACTIVE_TEXT =
            Component.translatable("hud.copycat_replace.connected_active");

    private static Component estimateText =
            Component.translatable("hud.copycat_replace.estimated_blocks", "--");
    private static int ticksSinceRefresh = REFRESH_INTERVAL_TICKS;
    private static int serverMaximum = -1;
    private static List<AABB> highlightedBoxes = List.of();

    private ConnectedBulkIndicator() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null
                || !CopycatReplaceClientKeys.BULK_REPLACE.isDown()) {
            ticksSinceRefresh = REFRESH_INTERVAL_TICKS;
            estimateText = Component.translatable("hud.copycat_replace.estimated_blocks", "--");
            highlightedBoxes = List.of();
            if (minecraft.player == null || minecraft.level == null) {
                serverMaximum = -1;
            }
            return;
        }

        if (serverMaximum < 1) {
            estimateText = Component.translatable("hud.copycat_replace.estimated_blocks", "--");
            highlightedBoxes = List.of();
            return;
        }

        if (++ticksSinceRefresh < REFRESH_INTERVAL_TICKS) {
            return;
        }
        ticksSinceRefresh = 0;

        if (!(minecraft.hitResult instanceof BlockHitResult hit)) {
            setPreview(0, List.of());
            return;
        }

        Player player = minecraft.player;
        BlockState state = minecraft.level.getBlockState(hit.getBlockPos());
        boolean removing = isWrenchOnly(player);
        ItemStack materialStack = selectMaterial(player);
        if (!removing && materialStack.isEmpty()) {
            setPreview(0, List.of());
            return;
        }

        PreviewEstimate estimate = BulkMaterialReplacement.previewConnected(
                minecraft.level,
                hit.getBlockPos(),
                state,
                hit,
                player,
                materialStack,
                removing,
                serverMaximum
        );
        setPreview(estimate.count(), estimate.positions());
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null
                || !CopycatReplaceClientKeys.BULK_REPLACE.isDown()) {
            return;
        }

        var graphics = event.getGuiGraphics();
        int right = graphics.guiWidth() - 8;
        int firstY = 8;
        int secondY = firstY + minecraft.font.lineHeight + 2;
        String active = ACTIVE_TEXT.getString();
        String estimate = estimateText.getString();
        graphics.drawString(
                minecraft.font,
                ACTIVE_TEXT,
                Math.max(8, right - minecraft.font.width(active)),
                firstY,
                0xFFFFFF,
                true
        );
        graphics.drawString(
                minecraft.font,
                estimateText,
                Math.max(8, right - minecraft.font.width(estimate)),
                secondY,
                0xFFFFFF,
                true
        );
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS
                || highlightedBoxes.isEmpty()
                || !CopycatReplaceClientKeys.BULK_REPLACE.isDown()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);

        var buffers = minecraft.renderBuffers().bufferSource();
        RenderType lineType = RenderType.lines();
        VertexConsumer lines = buffers.getBuffer(lineType);
        for (AABB box : highlightedBoxes) {
            LevelRenderer.renderLineBox(
                    poseStack,
                    lines,
                    box,
                    0.24F,
                    0.78F,
                    0.92F,
                    0.42F
            );
        }
        buffers.endBatch(lineType);
        poseStack.popPose();
    }

    public static void setServerMaximum(int maximum) {
        serverMaximum = Math.max(1, Math.min(CopycatReplaceConfig.MAX_CONNECTED_BLOCKS_LIMIT, maximum));
        ticksSinceRefresh = REFRESH_INTERVAL_TICKS;
    }

    private static void setPreview(int count, List<BlockPos> positions) {
        estimateText = Component.translatable("hud.copycat_replace.estimated_blocks", count);
        highlightedBoxes = positions.stream()
                .map(pos -> new AABB(pos).inflate(0.002D))
                .toList();
    }

    private static boolean isWrenchOnly(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        boolean wrenchInMain = mainHand.getItem() instanceof WrenchItem;
        boolean wrenchInOff = offHand.getItem() instanceof WrenchItem;
        return (wrenchInMain && !wrenchInOff && offHand.isEmpty())
                || (wrenchInOff && !wrenchInMain && mainHand.isEmpty());
    }

    private static ItemStack selectMaterial(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        boolean wrenchInMain = mainHand.getItem() instanceof WrenchItem;
        boolean wrenchInOff = offHand.getItem() instanceof WrenchItem;
        if (wrenchInMain && !wrenchInOff && !offHand.isEmpty()) {
            return offHand;
        }
        if (wrenchInOff && !wrenchInMain && !mainHand.isEmpty()) {
            return mainHand;
        }
        return ItemStack.EMPTY;
    }
}
