/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.module.decode.am;

import io.github.dsheirer.dsp.am.SquelchingAMDemodulator;
import io.github.dsheirer.dsp.gain.AudioGainAndDcFilter;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.analog.SquelchingAnalogDecoder;

/**
 * Analog AM radio decoder module with integrated squelching control
 */
public class AMDecoder extends SquelchingAnalogDecoder
{
    private static final float DEMODULATOR_GAIN = 150.0f;
    private static final float SQUELCH_ALPHA_DECAY = 0.0004f;
    private static final float MINIMUM_GAIN = 0.5f;
    private static final float MAXIMUM_GAIN = 16.0f;
    private static final float OBJECTIVE_AUDIO_AMPLITUDE = 0.75f;
    private AudioGainAndDcFilter mAGC = new AudioGainAndDcFilter(MINIMUM_GAIN, MAXIMUM_GAIN, OBJECTIVE_AUDIO_AMPLITUDE);

    /**
     * Constructs an instance
     * @param config for the AM channel.
     */
    public AMDecoder(DecodeConfigAM config)
    {
        super(config, new SquelchingAMDemodulator(DEMODULATOR_GAIN, SQUELCH_ALPHA_DECAY,
                config.getSquelchThreshold(), config.isSquelchAutoTrack()));
    }

    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.AM;
    }

    /**
     * Overrides the broadcast of the final demodulated audio samples so that we can apply AGC
     * @param demodulatedSamples to broadcast
     */
    @Override
    protected void broadcast(float[] demodulatedSamples)
    {
        //Apply audio gain and rebroadcast
        super.broadcast(mAGC.process(demodulatedSamples));
    }

    /**
     * Overrides the notify call start so that we can reset the AGC
     */
    @Override
    protected void notifyCallStart()
    {
        mAGC.reset();
        super.notifyCallStart();
    }
}
