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
package io.github.dsheirer.module.decode;

import io.github.dsheirer.message.AbstractMessage;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.IMessageListener;
import io.github.dsheirer.message.SyncLossMessage;
import io.github.dsheirer.module.Module;
import io.github.dsheirer.sample.Listener;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Running decode counts for each control and conventional channel, keyed by the channel's tuner owner.  Counts only
 * increase; callers compare two samples to measure an interval.
 */
public final class DecodeQuality
{
    private static final Map<String, Counter> COUNTERS = new ConcurrentHashMap<>();
    private static final Map<String, Double> SIGNAL_LEVELS = new ConcurrentHashMap<>();

    private DecodeQuality()
    {
    }

    /**
     * Decode counts at one moment.
     *
     * @param valid messages that passed their integrity checks
     * @param invalid messages that failed their integrity checks
     * @param correctedBits error-corrected bits across the valid messages
     * @param syncLossBits bits processed without a sync or a decoded message
     */
    public record Sample(long valid, long invalid, long correctedBits, long syncLossBits)
    {
        public static final Sample EMPTY = new Sample(0, 0, 0, 0);

        public Sample minus(Sample other)
        {
            return new Sample(valid - other.valid, invalid - other.invalid, correctedBits - other.correctedBits,
                syncLossBits - other.syncLossBits);
        }

        public Sample plus(Sample other)
        {
            return new Sample(valid + other.valid, invalid + other.invalid, correctedBits + other.correctedBits,
                syncLossBits + other.syncLossBits);
        }

        public double correctedBitsPerMessage()
        {
            return valid == 0 ? 0 : (double)correctedBits / valid;
        }
    }

    /**
     * Current counts for the owner, or empty when the owner hasn't decoded anything.
     */
    public static Sample get(String owner)
    {
        Counter counter = owner == null ? null : COUNTERS.get(owner);
        return counter == null ? Sample.EMPTY : counter.sample();
    }

    /**
     * Records the owner's latest signal level in dBFS.
     */
    public static void setSignalLevel(String owner, double level)
    {
        SIGNAL_LEVELS.put(owner, level);
    }

    /**
     * Latest signal level in dBFS for the owner, or null when it hasn't been measured.
     */
    public static Double getSignalLevel(String owner)
    {
        return owner == null ? null : SIGNAL_LEVELS.get(owner);
    }

    /**
     * Creates a processing chain module that counts the channel's decoded messages under the owner.
     */
    public static Module monitor(String owner)
    {
        return new Monitor(COUNTERS.computeIfAbsent(owner, key -> new Counter()));
    }

    private static class Counter
    {
        private final LongAdder mValid = new LongAdder();
        private final LongAdder mInvalid = new LongAdder();
        private final LongAdder mCorrectedBits = new LongAdder();
        private final LongAdder mSyncLossBits = new LongAdder();

        private void record(IMessage message)
        {
            if(message instanceof SyncLossMessage syncLoss)
            {
                mSyncLossBits.add(syncLoss.getBitsProcessed());
            }
            else if(message.isValid())
            {
                mValid.increment();

                if(message instanceof AbstractMessage abstractMessage && abstractMessage.getMessage() != null)
                {
                    mCorrectedBits.add(Math.max(0, abstractMessage.getMessage().getCorrectedBitCount()));
                }
            }
            else
            {
                mInvalid.increment();
            }
        }

        private Sample sample()
        {
            return new Sample(mValid.sum(), mInvalid.sum(), mCorrectedBits.sum(), mSyncLossBits.sum());
        }
    }

    private static class Monitor extends Module implements IMessageListener
    {
        private final Counter mCounter;

        private Monitor(Counter counter)
        {
            mCounter = counter;
        }

        @Override
        public Listener<IMessage> getMessageListener()
        {
            return message -> {
                if(message != null)
                {
                    mCounter.record(message);
                }
            };
        }

        @Override
        public void reset()
        {
        }

        @Override
        public void start()
        {
        }

        @Override
        public void stop()
        {
        }
    }
}
