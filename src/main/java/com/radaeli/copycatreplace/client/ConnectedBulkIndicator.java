package com.radaeli.copycatreplace.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.radaeli.copycatreplace.CopycatReplace;
import com.radaeli.copycatreplace.config.CopycatReplaceConfig;
import com.radaeli.copycatreplace.service.BulkMaterialReplacement;
import com.radaeli.copycatreplace.service.BulkMaterialReplacement.PreviewEstimate;
import com.simibubi.create.content.equipment.wrench.WrenchItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private static List<LineSegment> highlightedLines = List.of();

    private ConnectedBulkIndicator() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null
                || !CopycatReplaceClientKeys.BULK_REPLACE.isDown()) {
            ticksSinceRefresh = REFRESH_INTERVAL_TICKS;
            estimateText = Component.translatable("hud.copycat_replace.estimated_blocks", "--");
            highlightedLines = List.of();
            if (minecraft.player == null || minecraft.level == null) {
                serverMaximum = -1;
            }
            return;
        }

        if (serverMaximum < 1) {
            estimateText = Component.translatable("hud.copycat_replace.estimated_blocks", "--");
            highlightedLines = List.of();
            return;
        }

        if (++ticksSinceRefresh < REFRESH_INTERVAL_TICKS) {
            return;
        }
        ticksSinceRefresh = 0;

        if (!(minecraft.hitResult instanceof BlockHitResult hit)) {
            setPreview(minecraft.level, 0, List.of());
            return;
        }

        Player player = minecraft.player;
        BlockState state = minecraft.level.getBlockState(hit.getBlockPos());
        boolean removing = isWrenchOnly(player);
        ItemStack materialStack = selectMaterial(player);
        if (!removing && materialStack.isEmpty()) {
            setPreview(minecraft.level, 0, List.of());
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
        setPreview(minecraft.level, estimate.count(), estimate.positions());
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
                || highlightedLines.isEmpty()
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
        for (LineSegment segment : highlightedLines) {
            lines.addVertex(poseStack.last(), segment.x1() / 16.0F, segment.y1() / 16.0F,
                            segment.z1() / 16.0F)
                    .setColor(0.24F, 0.78F, 0.92F, 0.42F)
                    .setNormal(poseStack.last(), 0.0F, 1.0F, 0.0F);
            lines.addVertex(poseStack.last(), segment.x2() / 16.0F, segment.y2() / 16.0F,
                            segment.z2() / 16.0F)
                    .setColor(0.24F, 0.78F, 0.92F, 0.42F)
                    .setNormal(poseStack.last(), 0.0F, 1.0F, 0.0F);
        }
        buffers.endBatch(lineType);
        poseStack.popPose();
    }

    public static void setServerMaximum(int maximum) {
        serverMaximum = Math.max(1, Math.min(CopycatReplaceConfig.MAX_CONNECTED_BLOCKS_LIMIT, maximum));
        ticksSinceRefresh = REFRESH_INTERVAL_TICKS;
    }

    private static void setPreview(Level level, int count, List<BlockPos> positions) {
        estimateText = Component.translatable("hud.copycat_replace.estimated_blocks", count);
        highlightedLines = createOutline(level, positions);
    }

    /** Builds the outline from each block's actual collision shape at 1/16-block resolution. */
    private static List<LineSegment> createOutline(Level level, List<BlockPos> positions) {
        List<ShapeBox> boxes = new ArrayList<>();
        Map<FacePlane, List<ShapeBox>> minBoundaries = new HashMap<>();
        Map<FacePlane, List<ShapeBox>> maxBoundaries = new HashMap<>();
        for (BlockPos pos : positions) {
            BlockState state = level.getBlockState(pos);
            for (AABB shape : state.getShape(level, pos, CollisionContext.empty()).toAabbs()) {
                ShapeBox box = ShapeBox.from(pos, shape);
                boxes.add(box);
                for (int axis = 0; axis < 3; axis++) {
                    minBoundaries.computeIfAbsent(new FacePlane(axis, box.min(axis)), ignored -> new ArrayList<>())
                            .add(box);
                    maxBoundaries.computeIfAbsent(new FacePlane(axis, box.max(axis)), ignored -> new ArrayList<>())
                            .add(box);
                }
            }
        }

        Map<FacePlane, Set<FaceCell>> faces = new HashMap<>();
        for (ShapeBox box : boxes) {
            for (Direction direction : Direction.values()) {
                int axis = direction.getAxis().ordinal();
                boolean positive = direction.getAxisDirection() == Direction.AxisDirection.POSITIVE;
                int coordinate = positive ? box.max(axis) : box.min(axis);
                FacePlane plane = new FacePlane(axis, coordinate);
                List<ShapeBox> adjacent = (positive ? minBoundaries : maxBoundaries)
                        .getOrDefault(plane, List.of());
                int minU = box.minU(axis);
                int maxU = box.maxU(axis);
                int minV = box.minV(axis);
                int maxV = box.maxV(axis);
                Set<FaceCell> cells = faces.computeIfAbsent(plane, ignored -> new HashSet<>());
                for (int u = minU; u < maxU; u++) {
                    for (int v = minV; v < maxV; v++) {
                        boolean covered = false;
                        for (ShapeBox other : adjacent) {
                            if (other == box) {
                                continue;
                            }
                            if (other.minU(axis) <= u && other.maxU(axis) >= u + 1
                                    && other.minV(axis) <= v && other.maxV(axis) >= v + 1) {
                                covered = true;
                                break;
                            }
                        }
                        if (!covered) {
                            cells.add(new FaceCell(u, v));
                        }
                    }
                }
            }
        }

        Set<LineSegment> outline = new HashSet<>();
        for (Map.Entry<FacePlane, Set<FaceCell>> facePlane : faces.entrySet()) {
            FacePlane plane = facePlane.getKey();
            Set<FaceCell> cells = facePlane.getValue();
            for (FaceCell cell : cells) {
                if (!cells.contains(new FaceCell(cell.u() - 1, cell.v()))) {
                    outline.add(plane.edgeU(cell.u(), cell.v()));
                }
                if (!cells.contains(new FaceCell(cell.u() + 1, cell.v()))) {
                    outline.add(plane.edgeU(cell.u(), cell.v() + 1));
                }
                if (!cells.contains(new FaceCell(cell.u(), cell.v() - 1))) {
                    outline.add(plane.edgeV(cell.u(), cell.v()));
                }
                if (!cells.contains(new FaceCell(cell.u(), cell.v() + 1))) {
                    outline.add(plane.edgeV(cell.u() + 1, cell.v()));
                }
            }
        }
        return List.copyOf(outline);
    }

    private record FacePlane(int axis, int plane) {
        private LineSegment edgeU(int u, int v) {
            return switch (axis) {
                case 0 -> new LineSegment(plane, u, v, plane, u + 1, v);
                case 1 -> new LineSegment(u, plane, v, u + 1, plane, v);
                default -> new LineSegment(u, v, plane, u + 1, v, plane);
            };
        }

        private LineSegment edgeV(int u, int v) {
            return switch (axis) {
                case 0 -> new LineSegment(plane, u, v, plane, u, v + 1);
                case 1 -> new LineSegment(u, plane, v, u, plane, v + 1);
                default -> new LineSegment(u, v, plane, u, v + 1, plane);
            };
        }
    }

    private record FaceCell(int u, int v) {
    }

    private record ShapeBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        private static ShapeBox from(BlockPos pos, AABB shape) {
            return new ShapeBox(
                    pos.getX() * 16 + (int) Math.round(shape.minX * 16.0),
                    pos.getY() * 16 + (int) Math.round(shape.minY * 16.0),
                    pos.getZ() * 16 + (int) Math.round(shape.minZ * 16.0),
                    pos.getX() * 16 + (int) Math.round(shape.maxX * 16.0),
                    pos.getY() * 16 + (int) Math.round(shape.maxY * 16.0),
                    pos.getZ() * 16 + (int) Math.round(shape.maxZ * 16.0)
            );
        }

        private int min(int axis) {
            return switch (axis) {
                case 0 -> minX;
                case 1 -> minY;
                default -> minZ;
            };
        }

        private int max(int axis) {
            return switch (axis) {
                case 0 -> maxX;
                case 1 -> maxY;
                default -> maxZ;
            };
        }

        private int minU(int normalAxis) {
            return switch (normalAxis) {
                case 0 -> minY;
                case 1, 2 -> minX;
                default -> throw new IllegalArgumentException("Unknown axis: " + normalAxis);
            };
        }

        private int maxU(int normalAxis) {
            return switch (normalAxis) {
                case 0 -> maxY;
                case 1, 2 -> maxX;
                default -> throw new IllegalArgumentException("Unknown axis: " + normalAxis);
            };
        }

        private int minV(int normalAxis) {
            return switch (normalAxis) {
                case 0, 1 -> minZ;
                case 2 -> minY;
                default -> throw new IllegalArgumentException("Unknown axis: " + normalAxis);
            };
        }

        private int maxV(int normalAxis) {
            return switch (normalAxis) {
                case 0, 1 -> maxZ;
                case 2 -> maxY;
                default -> throw new IllegalArgumentException("Unknown axis: " + normalAxis);
            };
        }
    }

    private record LineSegment(int x1, int y1, int z1, int x2, int y2, int z2) {
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
