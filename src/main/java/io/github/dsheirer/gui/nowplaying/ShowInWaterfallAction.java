/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */
package io.github.dsheirer.gui.nowplaying;

import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.ChannelProcessingManager;
import io.github.dsheirer.module.ProcessingChain;
import io.github.dsheirer.source.Source;
import io.github.dsheirer.source.config.SourceConfigTuner;
import io.github.dsheirer.source.config.SourceConfigTunerMultipleFrequency;
import io.github.dsheirer.source.config.SourceConfiguration;
import io.github.dsheirer.source.tuner.Tuner;
import io.github.dsheirer.source.tuner.TunerEvent;
import io.github.dsheirer.source.tuner.channel.TunerChannelSource;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.ui.DiscoveredTunerModel;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Now Playing channel table right-click action: shows the tuner that carries a channel in the main spectral display
 * (waterfall), zoomed in and centred on the channel's frequency.
 */
public class ShowInWaterfallAction
{
    private static final Logger mLog = LoggerFactory.getLogger(ShowInWaterfallAction.class);
    private final ChannelProcessingManager mChannelProcessingManager;
    private final TunerManager mTunerManager;

    /**
     * Constructs an instance
     * @param channelProcessingManager for the running processing chains
     * @param tunerManager for the tuners, or null
     */
    public ShowInWaterfallAction(ChannelProcessingManager channelProcessingManager, TunerManager tunerManager)
    {
        mChannelProcessingManager = channelProcessingManager;
        mTunerManager = tunerManager;
    }

    /**
     * Indicates if the channel has a tuner source and can be shown in the waterfall.
     * @param channel to check
     * @return true if the channel's source is a tuner
     */
    public static boolean isSupported(Channel channel)
    {
        SourceConfiguration sourceConfig = channel.getSourceConfiguration();
        return sourceConfig instanceof SourceConfigTuner || sourceConfig instanceof SourceConfigTunerMultipleFrequency;
    }

    /**
     * Attempts to show the tuner serving a channel in the main spectral display (waterfall).
     *
     * Resolution order matters here. A channel's configured preferred tuner is only a request - if
     * that tuner has no spare bandwidth, or the channel's frequency falls outside its current tuned
     * range, the tuner manager silently sources the channel from a different tuner ("Unable to
     * source channel [x] from preferred tuner [y] - searching for another tuner"). Trusting the
     * preferred tuner name in that case displays a tuner that is not carrying the channel, which
     * shows an unrelated noise floor and no channel column at the requested frequency.
     *
     * So the live processing chain's tuner channel source is authoritative whenever the channel is
     * running. The configured preferred tuner is used only as a fallback for a channel that is not
     * currently decoding, and even then only when that tuner's tuned range actually covers the
     * channel frequency.
     *
     * @param channel to show in waterfall
     */
    public void showChannelInWaterfall(Channel channel)
    {
        if(mTunerManager == null)
        {
            mLog.warn("TunerManager not available - cannot show channel in waterfall");
            return;
        }

        DiscoveredTunerModel discoveredTunerModel = mTunerManager.getDiscoveredTunerModel();
        SourceConfiguration sourceConfig = channel.getSourceConfiguration();
        Tuner tuner = null;
        long channelFrequency = 0;

        //First: the live source. This is the only authoritative answer for a running channel, both
        //for which tuner is carrying it and for which frequency it is actually on - a trunked
        //traffic channel or a multiple-frequency channel is often not on the configured frequency.
        ProcessingChain processingChain = mChannelProcessingManager.getProcessingChain(channel);

        if(processingChain != null)
        {
            Source source = processingChain.getSource();

            if(source instanceof TunerChannelSource)
            {
                channelFrequency = ((TunerChannelSource)source).getFrequency();
                tuner = getTunerServingFrequency(discoveredTunerModel, channelFrequency);
            }
        }

        //Fall back to the configured frequency when the channel isn't running.
        if(channelFrequency == 0)
        {
            if(sourceConfig instanceof SourceConfigTuner)
            {
                channelFrequency = ((SourceConfigTuner)sourceConfig).getFrequency();
            }
            else if(sourceConfig instanceof SourceConfigTunerMultipleFrequency)
            {
                List<Long> frequencies = ((SourceConfigTunerMultipleFrequency)sourceConfig).getFrequencies();

                if(frequencies != null && !frequencies.isEmpty())
                {
                    channelFrequency = frequencies.get(0);
                }
            }
        }

        //Second: the configured preferred tuner, accepted only if it can actually show the channel.
        if(tuner == null)
        {
            String preferredTunerName = null;

            if(sourceConfig instanceof SourceConfigTuner)
            {
                preferredTunerName = ((SourceConfigTuner)sourceConfig).getPreferredTuner();
            }
            else if(sourceConfig instanceof SourceConfigTunerMultipleFrequency)
            {
                preferredTunerName = ((SourceConfigTunerMultipleFrequency)sourceConfig).getPreferredTuner();
            }

            if(preferredTunerName != null)
            {
                DiscoveredTuner discoveredTuner = mTunerManager.getDiscoveredTuner(preferredTunerName);

                if(discoveredTuner != null && discoveredTuner.hasTuner())
                {
                    if(channelFrequency == 0 || isFrequencyWithinTunerBandwidth(discoveredTuner.getTuner(),
                            channelFrequency))
                    {
                        tuner = discoveredTuner.getTuner();
                    }
                    else
                    {
                        mLog.debug("Preferred tuner [" + preferredTunerName + "] is not currently tuned to cover " +
                                "channel frequency [" + channelFrequency + "] - looking for the tuner that is");
                    }
                }
            }
        }

        //Third: any tuner whose current tuned range covers the channel frequency.
        if(tuner == null && channelFrequency > 0)
        {
            tuner = getTunerServingFrequency(discoveredTunerModel, channelFrequency);
        }

        if(tuner != null)
        {
            if(channelFrequency > 0)
            {
                discoveredTunerModel.broadcast(new TunerEvent(tuner,
                    TunerEvent.Event.REQUEST_MAIN_SPECTRAL_DISPLAY, channelFrequency));
            }
            else
            {
                discoveredTunerModel.broadcast(new TunerEvent(tuner,
                    TunerEvent.Event.REQUEST_MAIN_SPECTRAL_DISPLAY));
            }
        }
        else
        {
            mLog.debug("Unable to identify a tuner currently covering channel [" + channel.getName() +
                    "] frequency [" + channelFrequency + "] - nothing to show in the waterfall");
        }
    }

    /**
     * Identifies the tuner whose current center frequency and sample rate span the specified
     * frequency.
     *
     * @param discoveredTunerModel containing the available tuners
     * @param frequency to locate, in hertz
     * @return the tuner covering the frequency, or null
     */
    private Tuner getTunerServingFrequency(DiscoveredTunerModel discoveredTunerModel, long frequency)
    {
        if(frequency <= 0)
        {
            return null;
        }

        for(DiscoveredTuner discoveredTuner : discoveredTunerModel.getAvailableTuners())
        {
            if(discoveredTuner.hasTuner() && isFrequencyWithinTunerBandwidth(discoveredTuner.getTuner(), frequency))
            {
                return discoveredTuner.getTuner();
            }
        }

        return null;
    }

    /**
     * Indicates if the specified frequency falls within the tuner's currently tuned bandwidth.
     *
     * @param tuner to check
     * @param frequency to test, in hertz
     * @return true if the tuner is presently able to display the frequency
     */
    private boolean isFrequencyWithinTunerBandwidth(Tuner tuner, long frequency)
    {
        try
        {
            return covers(tuner.getTunerController().getFrequency(), tuner.getTunerController().getSampleRate(),
                frequency);
        }
        catch(Exception ex)
        {
            mLog.error("Error checking tuner frequency", ex);
            return false;
        }
    }

    /**
     * Indicates if a tuner at the center frequency and sample rate spans the frequency.
     * @param tunerFrequency tuner center frequency, in hertz
     * @param sampleRate tuner sample rate, in hertz
     * @param frequency to test, in hertz
     * @return true if the frequency falls within the tuned bandwidth
     */
    static boolean covers(long tunerFrequency, double sampleRate, long frequency)
    {
        long halfBandwidth = (long)(sampleRate / 2.0);
        return frequency >= tunerFrequency - halfBandwidth && frequency <= tunerFrequency + halfBandwidth;
    }
}
