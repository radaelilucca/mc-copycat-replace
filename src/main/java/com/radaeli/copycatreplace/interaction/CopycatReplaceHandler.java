package com.radaeli.copycatreplace.interaction;

import com.radaeli.copycatreplace.service.CopycatReplacementTarget;
import com.radaeli.copycatreplace.service.MaterialReplacement;
import com.simibubi.create.content.equipment.wrench.WrenchItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Handles wrench plus off-hand material interactions before Create's wrench does. */
public final class CopycatReplaceHandler {
    private CopycatReplaceHandler() {
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        Player player = event.getEntity();
        if (player.isShiftKeyDown() || !player.mayBuild()) {
            return;
        }

        HandSelection selection = selectHands(player);
        if (selection == null) {
            return;
        }

        Level level = event.getLevel();
        BlockState state = level.getBlockState(event.getPos());
        CopycatReplacementTarget target = MaterialReplacement.findTarget(
                level,
                event.getPos(),
                state,
                event.getHitVec()
        );
        if (target == null) {
            return;
        }

        // Consume even invalid replacement attempts so the wrench cannot remove
        // or rotate the existing material as a side effect.
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide()));

        if (!level.isClientSide()) {
            target.replaceMaterial(player, selection.materialHand(), selection.materialStack());
        }
    }

    private static HandSelection selectHands(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        boolean wrenchInMainHand = mainHand.getItem() instanceof WrenchItem;
        boolean wrenchInOffHand = offHand.getItem() instanceof WrenchItem;

        if (wrenchInMainHand && !wrenchInOffHand && !offHand.isEmpty()) {
            return new HandSelection(InteractionHand.OFF_HAND, offHand);
        }
        if (wrenchInOffHand && !wrenchInMainHand && !mainHand.isEmpty()) {
            return new HandSelection(InteractionHand.MAIN_HAND, mainHand);
        }
        return null;
    }

    private record HandSelection(InteractionHand materialHand, ItemStack materialStack) {
    }
}
