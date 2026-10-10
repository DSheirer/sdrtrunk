package io.github.dsheirer.module.decode.config;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public abstract class DecodeConfigJMBE extends DecodeConfiguration
{
    private boolean mAGC = true;
    private float mToneGain = 1.0f;
    private float mNoiseGain = 1.0f;

    /**
     * Automatic Gain Control (AGC) for the JMBE audio codec
     * @return is AGC enabled.
     */
    @JacksonXmlProperty(isAttribute = true, localName = "jmbeAGC", namespace = "http://www.w3.org/2001/XMLSchema-instance")
    public boolean isAGC()
    {
        return mAGC;
    }

    /**
     * Sets the AGC enabled state for the JMBE audio codec
     * @param enabled true to enable AGC
     */
    public void setAGC(boolean enabled)
    {
        mAGC = enabled;
    }

    /**
     * Tone generation gain (1.0 default) for the JMBE audio codec
     * @return gain in range 0.0 (no tones) to 1.0 (default gain).
     */
    @JacksonXmlProperty(isAttribute = true, localName = "jmbeToneGain", namespace = "http://www.w3.org/2001/XMLSchema-instance")
    public float getToneGain()
    {
        return mToneGain;
    }

    /**
     * Sets the tone generation gain for the JMBE audio codec
     * @param gain in range 0.0 (no tones) to 1.0 (default gain)
     */
    public void setToneGain(float gain)
    {
        if(gain > 1.0f)
        {
            mToneGain = 1.0f;
        }
        else if(gain < 0)
        {
            mToneGain = 0;
        }
        else
        {
            mToneGain = gain;
        }
    }

    /**
     * Sets the comfort noise insertion gain for the JMBE audio codec
     * @return noise gain in range 0.0 (no noise) to 1.0 (default noise)
     */
    @JacksonXmlProperty(isAttribute = true, localName = "jmbeNoiseGain", namespace = "http://www.w3.org/2001/XMLSchema-instance")
    public float getNoiseGain()
    {
        return mNoiseGain;
    }

    /**
     * Sets the comfort noise insertion gain for the JMBE audio codec
     * @param gain noise gain in range 0.0 (no noise) to 1.0 (default noise)
     */
    public void setNoiseGain(float gain)
    {
        if(gain > 1.0f)
        {
            mNoiseGain = 1.0f;
        }
        else if(gain < 0)
        {
            mNoiseGain = 0;
        }
        else
        {
            mNoiseGain = gain;
        }
    }
}
