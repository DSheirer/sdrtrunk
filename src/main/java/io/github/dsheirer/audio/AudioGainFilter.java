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

package io.github.dsheirer.audio;

import io.github.dsheirer.dsp.gain.AudioGainAndDcFilter;

/**
 * This is a wrapper for the AudioGainAndDcFilter class so it can be used with the AudioModule filter list.
 * Usually instantiated in the DecoderFactory
 */
public class AudioGainFilter extends AbstractAudioFilter
{
    AudioGainAndDcFilter mAudioGain;

    public AudioGainFilter(float minGain, float maxGain, float objectiveAmplitude)
    {
        mAudioGain = new AudioGainAndDcFilter(minGain, maxGain, objectiveAmplitude);
        mAudioGain.setDecayRate(2);     // to remove pumping effect
        mAudioGain.reset();
    }

    @Override
    public float[] filter(float[] audio)
    {
        return mAudioGain.process(audio);
    }
}
