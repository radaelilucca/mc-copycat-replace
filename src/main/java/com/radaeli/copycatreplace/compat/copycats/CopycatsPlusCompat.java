package com.radaeli.copycatreplace.compat.copycats;

import com.copycatsplus.copycats.foundation.copycat.ICopycatBlock;
import com.copycatsplus.copycats.foundation.copycat.ICopycatBlockEntity;
import com.copycatsplus.copycats.foundation.copycat.multistate.IMultiStateCopycatBlock;
import com.copycatsplus.copycats.foundation.copycat.multistate.IMultiStateCopycatBlockEntity;
import com.copycatsplus.copycats.foundation.copycat.multistate.MaterialItemStorage;
import com.radaeli.copycatreplace.service.CopycatReplacementTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Optional integration. This class must only be loaded after checking mod presence. */
public final class CopycatsPlusCompat {
    public static final String MOD_ID = "copycats";

    private CopycatsPlusCompat() {
    }

    public static CopycatReplacementTarget find(Level level, BlockPos pos, BlockState state, BlockHitResult hit) {
        if (state.getBlock() instanceof IMultiStateCopycatBlock multiStateBlock) {
            IMultiStateCopycatBlockEntity blockEntity = multiStateBlock.getCopycatBlockEntity(level, pos);
            if (blockEntity == null) {
                return null;
            }

            String property = multiStateBlock.getPropertyFromInteraction(state, level, pos, hit, true);
            if (!multiStateBlock.partExists(state, property)
                    || !blockEntity.getMaterialItemStorage().getAllProperties().contains(property)) {
                return null;
            }

            return (player, materialHand, materialStack) -> replaceMultiState(
                    multiStateBlock,
                    blockEntity,
                    property,
                    level,
                    pos,
                    state,
                    hit,
                    player,
                    materialHand,
                    materialStack
            );
        }

        if (state.getBlock() instanceof ICopycatBlock copycatBlock) {
            ICopycatBlockEntity blockEntity = copycatBlock.getCopycatBlockEntity(level, pos);
            if (blockEntity == null) {
                return null;
            }

            return (player, materialHand, materialStack) -> replaceSingleState(
                    copycatBlock,
                    blockEntity,
                    level,
                    pos,
                    state,
                    hit,
                    player,
                    materialHand,
                    materialStack
            );
        }

        return null;
    }

    private static void replaceSingleState(
            ICopycatBlock copycatBlock,
            ICopycatBlockEntity blockEntity,
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
        blockEntity.setMaterial(accepted);
        blockEntity.setConsumedItem(materialStack);

        updateInventory(player, materialHand, materialStack, previousMaterialItem, false);
        playPlaceSound(level, pos, accepted);
    }

    private static void replaceMultiState(
            IMultiStateCopycatBlock copycatBlock,
            IMultiStateCopycatBlockEntity blockEntity,
            String property,
            Level level,
            BlockPos pos,
            BlockState copycatState,
            BlockHitResult hit,
            Player player,
            InteractionHand materialHand,
            ItemStack materialStack
    ) {
        BlockState accepted = copycatBlock.getAcceptedBlockState(
                property,
                level,
                pos,
                materialStack,
                hit.getDirection()
        );
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
        if (accepted == null) {
            return;
        }

        MaterialItemStorage storage = blockEntity.getMaterialItemStorage();
        MaterialItemStorage.MaterialItem currentEntry = storage.getMaterialItem(property);
        BlockState previousMaterial = currentEntry.material();
        if (accepted.equals(previousMaterial)) {
            return;
        }

        // Changing only the orientation/state of the same material keeps its
        // existing paid item token and does not touch the player's inventory.
        if (accepted.getBlock() == previousMaterial.getBlock()) {
            blockEntity.setMaterial(property, accepted);
            playPlaceSound(level, pos, accepted);
            return;
        }

        ItemStack previousMaterialItem = currentEntry.consumedItem().copy();
        ItemStack refund = transferPreviousToken(
                blockEntity,
                storage,
                property,
                previousMaterial,
                previousMaterialItem
        );
        boolean newMaterialAlreadyPaid = storage.getAllConsumedItems().stream()
                .anyMatch(stack -> stack.getItem() == materialStack.getItem());

        blockEntity.setMaterial(property, accepted);
        blockEntity.setConsumedItem(
                property,
                newMaterialAlreadyPaid ? ItemStack.EMPTY : materialStack
        );

        updateInventory(player, materialHand, materialStack, refund, newMaterialAlreadyPaid);
        playPlaceSound(level, pos, accepted);
    }

    private static ItemStack transferPreviousToken(
            IMultiStateCopycatBlockEntity blockEntity,
            MaterialItemStorage storage,
            String replacedProperty,
            BlockState previousMaterial,
            ItemStack previousMaterialItem
    ) {
        if (previousMaterialItem.isEmpty()) {
            return ItemStack.EMPTY;
        }

        for (String otherProperty : storage.getAllProperties()) {
            if (otherProperty.equals(replacedProperty)) {
                continue;
            }

            MaterialItemStorage.MaterialItem otherEntry = storage.getMaterialItem(otherProperty);
            if (otherEntry.material().getBlock() == previousMaterial.getBlock()
                    && otherEntry.consumedItem().isEmpty()) {
                blockEntity.setConsumedItem(otherProperty, previousMaterialItem);
                return ItemStack.EMPTY;
            }
        }

        return previousMaterialItem;
    }

    private static void updateInventory(
            Player player,
            InteractionHand materialHand,
            ItemStack materialStack,
            ItemStack previousMaterialItem,
            boolean newMaterialAlreadyPaid
    ) {
        if (player.isCreative()) {
            return;
        }

        if (!newMaterialAlreadyPaid) {
            materialStack.shrink(1);
            if (materialStack.isEmpty()) {
                player.setItemInHand(materialHand, ItemStack.EMPTY);
            }
        }
        if (!previousMaterialItem.isEmpty()) {
            player.getInventory().placeItemBackInInventory(previousMaterialItem);
        }
    }

    private static void playPlaceSound(Level level, BlockPos pos, BlockState material) {
        level.playSound(
                null,
                pos,
                material.getSoundType().getPlaceSound(),
                SoundSource.BLOCKS,
                1.0F,
                0.75F
        );
    }
}
