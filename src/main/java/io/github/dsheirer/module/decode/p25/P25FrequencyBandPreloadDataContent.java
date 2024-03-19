package io.github.dsheirer.module.decode.p25;

import io.github.dsheirer.controller.channel.event.PreloadDataContent;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBand;

import java.util.Collection;

/**
 * Frequency bands (aka Identifier Update) to preload into a traffic channel.
 */
public class P25FrequencyBandPreloadDataContent extends PreloadDataContent<Collection<IFrequencyBand>>
{
    /**
     * Constructs an instance
     *
     * @param data to preload
     */
    public P25FrequencyBandPreloadDataContent(Collection<IFrequencyBand> data)
    {
        super(data);
    }
}
