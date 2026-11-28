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
package io.github.dsheirer.source.tuner.manager;

import io.github.dsheirer.dsp.filter.channelizer.ols.OverlapSaveChannelManager;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.TunerController;
import io.github.dsheirer.source.tuner.channel.ChannelSpecification;
import io.github.dsheirer.source.tuner.channel.TunerChannel;
import io.github.dsheirer.source.tuner.channel.TunerChannelSource;
import io.github.dsheirer.source.tuner.frequency.TunerFrequencyErrorManager;
import java.util.SortedSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Channel source manager for the overlap-save (fast convolution) channelizer.  It manages the tuner's center
 * frequency and provides overlap-save tuner channel sources.  Overlap-save channels can sit at any offset, so the
 * center frequency only has to keep the channels inside the tuner's usable bandwidth and clear of any unusable
 * center (DC) region.
 *
 * Sample rates that the overlap-save channelizer doesn't support (OverlapSaveChannelizer.isSupported()) are served by
 * a polyphase channel source manager instead, which is logged.
 */
public class OverlapSaveChannelSourceManager extends ChannelSourceManager
{
    private static final Logger LOGGER = LoggerFactory.getLogger(OverlapSaveChannelSourceManager.class);
    /**
     * Step of the search for a center frequency that fits the channels and avoids the unusable center region.
     */
    private static final long CENTER_SEARCH_STEP = 1000;
    private final TunerController mTunerController;
    private final OverlapSaveChannelManager mChannelManager;
    private PolyphaseChannelSourceManager mPolyphaseFallback;
    private double mLoggedFallbackSampleRate;

    /**
     * Constructs an instance
     * @param tunerController with a center tuned frequency that will be managed by this instance
     */
    public OverlapSaveChannelSourceManager(TunerController tunerController)
    {
        mTunerController = tunerController;
        mChannelManager = new OverlapSaveChannelManager(tunerController);
        mChannelManager.addSourceEventListener(this::process);
        mTunerController.addListener(mChannelManager);
    }

    @Override
    public TunerFrequencyErrorManager getTunerFrequencyErrorManager()
    {
        return mTunerController.getTunerFrequencyErrorManager();
    }

    @Override
    public String getStateDescription()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Overlap-Save Channel Source Manager");
        sb.append("\n\tTuner Controller Frequency: ").append(mTunerController.getFrequency());
        sb.append("\n\t").append(mChannelManager.getStateDescription());

        if(mPolyphaseFallback != null)
        {
            sb.append("\n\tFallback: ").append(mPolyphaseFallback.getStateDescription());
        }

        return sb.toString();
    }

    @Override
    public void stopAllChannels()
    {
        mChannelManager.stopAllChannels();

        if(mPolyphaseFallback != null)
        {
            mPolyphaseFallback.stopAllChannels();
        }
    }

    @Override
    public void setErrorMessage(String errorMessage)
    {
        mChannelManager.setErrorMessage(errorMessage);

        if(mPolyphaseFallback != null)
        {
            mPolyphaseFallback.setErrorMessage(errorMessage);
        }
    }

    @Override
    public SortedSet<TunerChannel> getTunerChannels()
    {
        SortedSet<TunerChannel> channels = mChannelManager.getTunerChannels();

        if(mPolyphaseFallback != null)
        {
            channels.addAll(mPolyphaseFallback.getTunerChannels());
        }

        return channels;
    }

    @Override
    public int getTunerChannelCount()
    {
        return mChannelManager.getTunerChannelCount() +
            (mPolyphaseFallback != null ? mPolyphaseFallback.getTunerChannelCount() : 0);
    }

    @Override
    public TunerChannelSource getSource(TunerChannel tunerChannel, ChannelSpecification channelSpecification,
                                        String threadName)
    {
        mTunerController.getLock().lock();

        try
        {
            //The sample rate is locked while channels are sourced, so all channels come from the same channelizer
            if(mChannelManager.getTunerChannelCount() == 0 &&
                ((mPolyphaseFallback != null && mPolyphaseFallback.getTunerChannelCount() > 0) ||
                !mChannelManager.isSupportedSampleRate()))
            {
                return getPolyphaseFallback().getSource(tunerChannel, channelSpecification, threadName);
            }

            if(!mTunerController.canTune(tunerChannel.getMinFrequency()) ||
                !mTunerController.canTune(tunerChannel.getMaxFrequency()))
            {
                return null;
            }

            SortedSet<TunerChannel> tunerChannels = mChannelManager.getTunerChannels();
            tunerChannels.add(tunerChannel);
            long centerFrequency = getCenterFrequency(tunerChannels, mTunerController.getFrequency());

            if(centerFrequency == CenterFrequencyCalculator.INVALID_FREQUENCY)
            {
                return null;
            }

            if(centerFrequency != mTunerController.getFrequency())
            {
                mTunerController.setFrequency(centerFrequency);
            }

            return mChannelManager.getChannel(tunerChannel, channelSpecification, threadName);
        }
        catch(SourceException se)
        {
            LOGGER.error("Error while updating tuner controller center frequency - unable to allocate new tuner " +
                "channel", se);
        }
        catch(Exception e)
        {
            LOGGER.error("Error getting source", e);
        }
        finally
        {
            mTunerController.getLock().unlock();
        }

        return null;
    }

    /**
     * Polyphase source manager for sample rates that the overlap-save channelizer doesn't support, created when first
     * needed.
     */
    private PolyphaseChannelSourceManager getPolyphaseFallback()
    {
        double sampleRate = mTunerController.getSampleRate();

        if(mLoggedFallbackSampleRate != sampleRate)
        {
            LOGGER.info("Overlap-save channelizer doesn't support sample rate [" + sampleRate + "] (it needs a whole " +
                "number of 40 Hz bins with no prime factors other than 2, 3 and 5) - using the polyphase channelizer");
            mLoggedFallbackSampleRate = sampleRate;
        }

        if(mPolyphaseFallback == null)
        {
            mPolyphaseFallback = new PolyphaseChannelSourceManager(mTunerController);
            mPolyphaseFallback.addSourceEventListener(this::broadcast);
        }

        return mPolyphaseFallback;
    }

    /**
     * Calculates a center frequency for the channel set: the current frequency if every channel fits, otherwise the
     * middle of the channel set, otherwise the nearest frequency to the middle where every channel fits.
     * @param channels to fit
     * @param currentCenterFrequency of the tuner
     * @return center frequency or CenterFrequencyCalculator.INVALID_FREQUENCY if the channels can't fit
     */
    protected long getCenterFrequency(SortedSet<TunerChannel> channels, long currentCenterFrequency)
    {
        if(channels.isEmpty() || isValidCenterFrequency(channels, currentCenterFrequency))
        {
            return currentCenterFrequency;
        }

        long minimum = channels.first().getMinFrequency();
        long maximum = channels.last().getMaxFrequency();

        if(maximum - minimum > mTunerController.getUsableBandwidth())
        {
            return CenterFrequencyCalculator.INVALID_FREQUENCY;
        }

        long middle = (minimum + maximum) / 2;
        long range = mTunerController.getUsableHalfBandwidth();

        for(long shift = 0; shift <= range; shift += CENTER_SEARCH_STEP)
        {
            if(isValidCenterFrequency(channels, middle + shift))
            {
                return middle + shift;
            }

            if(shift > 0 && isValidCenterFrequency(channels, middle - shift))
            {
                return middle - shift;
            }
        }

        return CenterFrequencyCalculator.INVALID_FREQUENCY;
    }

    /**
     * Indicates if every channel fits the tuner's usable bandwidth at the candidate center frequency and avoids any
     * unusable center region.
     */
    private boolean isValidCenterFrequency(SortedSet<TunerChannel> channels, long centerFrequency)
    {
        long half = mTunerController.getUsableHalfBandwidth();
        long middle = mTunerController.getMiddleUnusableHalfBandwidth();
        boolean avoidMiddle = mTunerController.hasMiddleUnusableBandwidth();

        for(TunerChannel channel : channels)
        {
            if(channel.getMinFrequency() < centerFrequency - half || channel.getMaxFrequency() > centerFrequency + half)
            {
                return false;
            }

            if(avoidMiddle && channel.overlaps(centerFrequency - middle, centerFrequency + middle))
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Processes channel count change notifications from the channel manager.
     */
    @Override
    public void process(SourceEvent sourceEvent)
    {
        switch(sourceEvent.getEvent())
        {
            case NOTIFICATION_CHANNEL_COUNT_CHANGE:
                broadcast(sourceEvent);
                //Lock the frequency and sample rate controls on the tuner controller while channels are sourced
                mTunerController.setLockedSampleRate(getTunerChannelCount() > 0);
                break;
            default:
                LOGGER.info("Unrecognized source event: " + sourceEvent);
                break;
        }
    }
}
