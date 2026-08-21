package com.radaeli.copycatreplace;

import com.mojang.logging.LogUtils;
import com.radaeli.copycatreplace.interaction.CopycatReplaceHandler;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/** Entry point for Create: Copycat Replace!. */
@Mod(CopycatReplace.MOD_ID)
public final class CopycatReplace {
    public static final String MOD_ID = "copycat_replace";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CopycatReplace(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, CopycatReplaceHandler::onRightClickBlock);
        LOGGER.info("Create: Copycat Replace! initialized");
    }
}
