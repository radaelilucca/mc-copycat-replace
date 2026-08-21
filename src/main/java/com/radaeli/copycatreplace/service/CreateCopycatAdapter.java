package com.radaeli.copycatreplace.service;

import com.simibubi.create.content.decoration.copycat.CopycatBlock;
import com.simibubi.create.content.decoration.copycat.CopycatBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Bridges the material replacement operation to Create's native copycats. */
public final class CreateCopycatAdapter {
    private CreateCopycatAdapter() {
    }

    public static CopycatReplacementTarget find(Level level, BlockPos pos, BlockState state, BlockHitResult hit) {
        if (!(state.getBlock() instanceof CopycatBlock copycatBlock)) {
            return null;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof CopycatBlockEntity copycatBlockEntity)) {
            return null;
        }

        return (player, materialHand, materialStack) -> replace(
                copycatBlock,
                copycatBlockEntity,
                level,
                pos,
                state,
                hit,
                player,
                materialHand,
                materialStack
        );
    }

    private static void replace(
            CopycatBlock copycatBlock,
            CopycatBlockEntity blockEntity,
            Level level,
            BlockPos pos,
            BlockState copycatState,
            BlockHitResult hit,
            Player player,
            InteractionHand materialHand,
            ItemStack materialStack
    ) {
        BlockState accepted = copycatBlock.getAcceptedBlockState(level, pos, materialStack, hit.getDirection());
        if (accepted == null) {
            return;
        }

        accepted = copycatBlock.prepareMaterial(
                level,
                pos,
                copycatState,
                player,
                materialHand,
                hit,
                accepted
        );
        if (accepted == null || accepted.equals(blockEntity.getMaterial())) {
            return;
        }

        ItemStack previousMaterialItem = blockEntity.getConsumedItem().copy();

        // setMaterial is the synchronizing operation in Create. Store the new
        // consumed item first so the same update contains the complete state.
        blockEntity.setConsumedItem(materialStack);
        blockEntity.setMaterial(accepted);

        if (!player.isCreative()) {
            materialStack.shrink(1);
            if (materialStack.isEmpty()) {
                player.setItemInHand(materialHand, ItemStack.EMPTY);
            }
            if (!previousMaterialItem.isEmpty()) {
                player.getInventory().placeItemBackInInventory(previousMaterialItem);
            }
        }

        level.playSound(
                null,
                pos,
                accepted.getSoundType().getPlaceSound(),
                SoundSource.BLOCKS,
                1.0F,
                0.75F
        );
    }
}
