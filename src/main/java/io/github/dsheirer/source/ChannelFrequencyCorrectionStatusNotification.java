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

package io.github.dsheirer.source;

import io.github.dsheirer.source.tuner.channel.TunerChannelSource;

/**
 * Notification of the current tuner, channel and decoder frequency correction state.
 */
public class ChannelFrequencyCorrectionStatusNotification extends SourceEvent
{
    private long mChannelCorrection;
    private long mTunerCorrection;
    private double mTunerPPM;
    private boolean mAutoPPM;

    /**
     * Constructs an instance
     * @param source tuner channel
     * @param decoderCorrection from the channel decoder
     * @param channelCorrection currently applied frequency correction
     * @param tunerPPM current tuner PPM value
     * @param tunerCorrection current tuner frequency correction value.
     * @param autoPPM indicating if tuner autoPPM is enabled.
     */
    private ChannelFrequencyCorrectionStatusNotification(TunerChannelSource source, long decoderCorrection,
                                                         long channelCorrection, double tunerPPM, long tunerCorrection,
                                                         boolean autoPPM)
    {
        super(SourceEvent.Event.NOTIFICATION_CHANNEL_FREQUENCY_CORRECTION_STATUS, source, decoderCorrection, "Status Report");
        mChannelCorrection = channelCorrection;
        mTunerCorrection = tunerCorrection;
        mTunerPPM = tunerPPM;
        mAutoPPM = autoPPM;
    }

    public boolean isAutoPPM()
    {
        return mAutoPPM;
    }

    /**
     * Current tuner frequency correction value
     */
    public long getTunerCorrection()
    {
        return mTunerCorrection;
    }

    /**
     * Current tuner PPM setting
     */
    public double getTunerPPM()
    {
        return mTunerPPM;
    }

    /**
     * Current channel correction value
     */
    public long getChannelCorrection()
    {
        return mChannelCorrection;
    }

    /**
     * Correction applied by the decoder which is the requested change (from the decoder)
     */
    public long getDecoderCorrection()
    {
        return getValue().longValue();
    }

    /**
     * Utility method to create an instance
     * @param source tuner channel
     * @param decoderCorrection from the decoder
     * @param channelCorrection currently applied to the channel tuner source
     * @param tunerPPM reported by the tuner
     * @param tunerCorrection in Hertz reported by the tuner
     * @param autoPPM indicating the enabled state of the tuner frequency correction manager
     * @return new instance
     */
    public static ChannelFrequencyCorrectionStatusNotification create(TunerChannelSource source, long decoderCorrection,
                                                                      long channelCorrection, double tunerPPM,
                                                                      long tunerCorrection, boolean autoPPM)
    {
        return new ChannelFrequencyCorrectionStatusNotification(source, decoderCorrection, channelCorrection,
                tunerPPM, tunerCorrection, autoPPM);
    }
}
