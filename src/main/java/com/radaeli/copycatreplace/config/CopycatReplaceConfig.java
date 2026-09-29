package com.radaeli.copycatreplace.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Global settings shared by all worlds in a mod instance. */
public final class CopycatReplaceConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue MAX_CONNECTED_BLOCKS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        MAX_CONNECTED_BLOCKS = builder
                .comment("Maximum number of connected Copycat block positions affected by one bulk action.")
                .translation("copycat_replace.config.max_connected_blocks")
                .defineInRange("max_connected_blocks", 64, 1, Integer.MAX_VALUE);
        SPEC = builder.build();
    }

    private CopycatReplaceConfig() {
    }
}
