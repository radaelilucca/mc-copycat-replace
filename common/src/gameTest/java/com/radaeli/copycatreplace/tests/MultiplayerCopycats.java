package com.radaeli.copycatreplace.tests;
import com.copycatsplus.copycats.CCBlocks;
import com.copycatsplus.copycats.content.copycat.slab.CopycatSlabBlock;
import com.copycatsplus.copycats.foundation.copycat.multistate.IMultiStateCopycatBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.item.*;
final class MultiplayerCopycats {
    static void prepare(Level level,BlockPos pos) {
        var state=CCBlocks.COPYCAT_SLAB.getDefaultState().setValue(CopycatSlabBlock.SLAB_TYPE,SlabType.DOUBLE);
        level.setBlockAndUpdate(pos,state);
        var be=(IMultiStateCopycatBlockEntity)level.getBlockEntity(pos);
        boolean first=true;
        for(String part:be.getMaterialItemStorage().getAllProperties()) {
            be.setMaterial(part,Blocks.STONE.defaultBlockState());
            be.setConsumedItem(part,first?new ItemStack(Items.STONE):ItemStack.EMPTY); first=false;
        }
    }
    static boolean all(Level level,BlockPos pos,Block block) {
        return level.getBlockEntity(pos) instanceof IMultiStateCopycatBlockEntity be && be.getMaterialItemStorage().getMaterialMap().values().stream().allMatch(s->s.is(block));
    }
    static boolean empty(Level level,BlockPos pos) {
        return level.getBlockEntity(pos) instanceof IMultiStateCopycatBlockEntity be && be.getMaterialItemStorage().getAllProperties().stream().noneMatch(be.getMaterialItemStorage()::hasCustomMaterial);
    }
}
