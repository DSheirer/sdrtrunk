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
        mAudioGain.setDecayRate(2);
    }

    @Override
    public float[] filter(float[] audio)
    {
        return mAudioGain.process(audio);
    }
}
