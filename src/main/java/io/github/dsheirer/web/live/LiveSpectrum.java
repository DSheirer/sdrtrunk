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

package io.github.dsheirer.web.live;

import io.github.dsheirer.buffer.INativeBuffer;
import io.github.dsheirer.source.tuner.TunerController;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.spectrum.ComplexDftProcessor;
import io.github.dsheirer.spectrum.DFTSize;
import io.github.dsheirer.spectrum.converter.ComplexDecibelConverter;
import io.github.dsheirer.spectrum.converter.DFTResultsConverter;
import io.github.dsheirer.web.api.ApiException;
import io.github.dsheirer.web.api.RuntimeViews;
import io.github.dsheirer.web.media.BinaryFrames;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Spectrum frames for the spectrum topic.  The server runs one DFT per tuner and FFT size, however many clients watch
 * it, at the highest frame rate any of them asked for; each client receives frames at its own rate.  Frames are
 * binary (see {@link BinaryFrames}) and a waiting frame is replaced by a newer one, so a slow client skips frames
 * rather than falling behind.
 *
 * Each DFT costs CPU on a small host, so the count of sources is limited.
 */
class LiveSpectrum
{
    private static final Logger LOGGER = LoggerFactory.getLogger(LiveSpectrum.class);
    static final int MAX_SOURCES = 4;
    static final int MAX_FRAME_RATE = 30;
    static final int DEFAULT_FRAME_RATE = 15;
    private static final String TUNER_PREFIX = "tuner:";

    private final TunerManager mTunerManager;
    private final Map<String,Source> mSources = new HashMap<>();

    /**
     * Constructs an instance
     * @param tunerManager for the tuners, or null when tuners aren't available
     */
    LiveSpectrum(TunerManager tunerManager)
    {
        mTunerManager = tunerManager;
    }

    /**
     * Subscription parameters
     * @param tunerKey key of the tuner from the tuner list
     */
    record Parameters(String tunerKey, DFTSize size, int frameRate) {}

    /**
     * Parses and checks the subscription parameters: source (tuner:{key}), fftSize (512 to 32768) and frameRate (1 to
     * 30, default 15).
     */
    Parameters parse(java.util.function.Function<String,String> parameter)
    {
        String source = parameter.apply("source");

        if(source == null || !source.startsWith(TUNER_PREFIX))
        {
            throw ApiException.invalid("source", "must be tuner:{key} with a key from the tuner list");
        }

        DFTSize size = DFTSize.FFT04096;
        String fftSize = parameter.apply("fftSize");

        if(fftSize != null)
        {
            size = Arrays.stream(DFTSize.values()).filter(value -> String.valueOf(value.getSize()).equals(fftSize))
                .findFirst().orElseThrow(() -> ApiException.invalid("fftSize", "must be one of " +
                    Arrays.stream(DFTSize.values()).map(DFTSize::getSize).toList()));
        }

        int frameRate = DEFAULT_FRAME_RATE;
        String rate = parameter.apply("frameRate");

        if(rate != null)
        {
            try
            {
                frameRate = Integer.parseInt(rate);
            }
            catch(NumberFormatException nfe)
            {
                frameRate = -1;
            }

            if(frameRate < 1 || frameRate > MAX_FRAME_RATE)
            {
                throw ApiException.invalid("frameRate", "must be from 1 to " + MAX_FRAME_RATE);
            }
        }

        return new Parameters(source.substring(TUNER_PREFIX.length()), size, frameRate);
    }

    /**
     * Starts sending frames to the subscription.
     * @throws ApiException 404 for an unknown tuner, 409 when it isn't running, 429 when too many sources run
     */
    synchronized void add(LiveConnection connection, Subscription subscription, Parameters parameters)
    {
        remove(subscription);

        if(mTunerManager == null)
        {
            throw ApiException.unavailable("Tuners aren't available");
        }

        DiscoveredTuner tuner = mTunerManager.getDiscoveredTunerModel().getDiscoveredTuners().stream()
            .filter(candidate -> RuntimeViews.tunerKey(candidate).equals(parameters.tunerKey())).findFirst()
            .orElseThrow(() -> ApiException.notFound("No tuner with key " + parameters.tunerKey()));

        if(!tuner.hasTuner() || !tuner.isAvailable())
        {
            throw ApiException.conflict("The tuner isn't running");
        }

        String key = parameters.tunerKey() + "|" + parameters.size().getSize();
        Source source = mSources.get(key);

        if(source == null)
        {
            if(mSources.size() >= MAX_SOURCES)
            {
                throw ApiException.tooManyRequests("The server already computes " + MAX_SOURCES + " spectrum " +
                    "sources; watch one of them or try later");
            }

            source = new Source(key, parameters.tunerKey(), tuner.getTuner().getTunerController(), parameters.size());
            mSources.put(key, source);
        }

        subscription.setParameters(parameters);
        source.add(new Target(connection, subscription, 1000 / parameters.frameRate()));
    }

    /**
     * Stops sending frames to the subscription.
     */
    synchronized void remove(Subscription subscription)
    {
        for(Source source: List.copyOf(mSources.values()))
        {
            if(source.remove(subscription) && source.isEmpty())
            {
                source.dispose();
                mSources.remove(source.mKey);
            }
        }
    }

    /**
     * Stops the sources of a tuner that was removed or stopped, and tells their subscribers.
     */
    synchronized void tunerStopped(String tunerKey)
    {
        for(Source source: List.copyOf(mSources.values()))
        {
            if(source.mTunerKey.equals(tunerKey))
            {
                for(Target target: List.copyOf(source.mTargets.values()))
                {
                    target.connection().getSubscriptions().remove(target.subscription().getId());
                    target.connection().sendError(target.subscription().getId(),
                        ApiException.conflict("The tuner stopped"));
                }

                source.dispose();
                mSources.remove(source.mKey);
            }
        }
    }

    /**
     * Stops every source.
     */
    synchronized void dispose()
    {
        mSources.values().forEach(Source::dispose);
        mSources.clear();
    }

    /**
     * Count of running DFT sources
     */
    synchronized int getSourceCount()
    {
        return mSources.size();
    }

    private record Target(LiveConnection connection, Subscription subscription, long intervalMs)
    {
    }

    /**
     * One DFT on one tuner
     */
    private static class Source extends DFTResultsConverter
    {
        private final String mKey;
        private final String mTunerKey;
        private final TunerController mController;
        private final ComplexDftProcessor<INativeBuffer> mProcessor = new ComplexDftProcessor<>();
        private final Map<Subscription,Target> mTargets = new ConcurrentHashMap<>();
        private final Map<Subscription,Long> mNextDue = new ConcurrentHashMap<>();

        Source(String key, String tunerKey, TunerController controller, DFTSize size)
        {
            mKey = key;
            mTunerKey = tunerKey;
            mController = controller;
            mProcessor.setDFTSize(size);
            mProcessor.addConverter(this);
            mController.addBufferListener(mProcessor);
        }

        void add(Target target)
        {
            mTargets.put(target.subscription(), target);
            updateFrameRate();
        }

        boolean remove(Subscription subscription)
        {
            boolean removed = mTargets.remove(subscription) != null;
            mNextDue.remove(subscription);

            if(removed && !mTargets.isEmpty())
            {
                updateFrameRate();
            }

            return removed;
        }

        boolean isEmpty()
        {
            return mTargets.isEmpty();
        }

        private void updateFrameRate()
        {
            long fastest = mTargets.values().stream().mapToLong(Target::intervalMs).min().orElse(1000);
            int rate = (int)Math.max(1, Math.min(MAX_FRAME_RATE, 1000 / fastest));

            if(rate != mProcessor.getFrameRate())
            {
                mProcessor.setFrameRateNoSave(rate);
            }
        }

        @Override
        public void dispose()
        {
            mController.removeBufferListener(mProcessor);
            mProcessor.dispose();
            mTargets.clear();
            super.dispose();
        }

        /**
         * DFT results from the processor thread
         */
        @Override
        public void receive(float[] results)
        {
            long now = System.currentTimeMillis();
            BinaryFrames.QuantizedBins bins = null;

            for(Target target: mTargets.values())
            {
                if(target.connection().isClosed())
                {
                    continue;
                }

                Long due = mNextDue.get(target.subscription());

                if(due != null && due > now)
                {
                    continue;
                }

                if(bins == null)
                {
                    bins = BinaryFrames.QuantizedBins.of(ComplexDecibelConverter.convert(results));
                }

                mNextDue.put(target.subscription(), now + target.intervalMs() - 5);

                try
                {
                    target.connection().sendBinary(target.subscription(), "spectrum", BinaryFrames.spectrum(
                        target.subscription().getId(), now, mController.getFrequency(),
                        (long)mController.getSampleRate(), bins));
                }
                catch(Exception e)
                {
                    LOGGER.debug("Unable to send a spectrum frame", e);
                }
            }
        }
    }
}
