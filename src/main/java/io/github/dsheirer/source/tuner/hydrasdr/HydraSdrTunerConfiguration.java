/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
 * Copyright (C) 2026 Benjamin Vernoux
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
package io.github.dsheirer.source.tuner.hydrasdr;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;

/**
 * Configuration for HydraSDR native tuner.
 *
 * Uses the unified gain API from libhydrasdr - individual gain type values
 * are stored directly rather than preset enums.
 */
public class HydraSdrTunerConfiguration extends TunerConfiguration
{
	private int mSampleRate = HydraSdrTunerController.DEFAULT_SAMPLE_RATE;
	private int mLnaGain = HydraSdrTunerController.LNA_GAIN_DEFAULT;
	private int mMixerGain = HydraSdrTunerController.MIXER_GAIN_DEFAULT;
	private int mVgaGain = HydraSdrTunerController.VGA_GAIN_DEFAULT;
	private int mLinearityGain = 14;
	private int mSensitivityGain = 0;
	private boolean mLnaAgc = false;
	private boolean mMixerAgc = false;
	private boolean mBiasT = false;
	private int mGainMode = 0; /* 0=linearity, 1=sensitivity, 2=custom */

	/**
	 * Default constructor for JAXB
	 */
	public HydraSdrTunerConfiguration()
	{
		super(HydraSdrTunerController.FALLBACK_MIN_FREQUENCY_HZ,
			HydraSdrTunerController.FALLBACK_MAX_FREQUENCY_HZ);
	}

	@Override
	@JacksonXmlProperty(isAttribute = true, localName = "type",
		namespace = "http://www.w3.org/2001/XMLSchema-instance")
	public TunerType getTunerType()
	{
		return TunerType.HYDRASDR;
	}

	public HydraSdrTunerConfiguration(String uniqueID)
	{
		super(uniqueID);
	}

	@JacksonXmlProperty(isAttribute = true, localName = "sample_rate")
	public int getSampleRate()
	{
		return mSampleRate;
	}

	public void setSampleRate(int sampleRate)
	{
		mSampleRate = sampleRate;
	}

	@JacksonXmlProperty(isAttribute = true, localName = "lna_gain")
	public int getLnaGain()
	{
		return mLnaGain;
	}

	public void setLnaGain(int gain)
	{
		mLnaGain = gain;
	}

	@JacksonXmlProperty(isAttribute = true, localName = "mixer_gain")
	public int getMixerGain()
	{
		return mMixerGain;
	}

	public void setMixerGain(int gain)
	{
		mMixerGain = gain;
	}

	@JacksonXmlProperty(isAttribute = true, localName = "vga_gain")
	public int getVgaGain()
	{
		return mVgaGain;
	}

	public void setVgaGain(int gain)
	{
		mVgaGain = gain;
	}

	@JacksonXmlProperty(isAttribute = true, localName = "linearity_gain")
	public int getLinearityGain()
	{
		return mLinearityGain;
	}

	public void setLinearityGain(int gain)
	{
		mLinearityGain = gain;
	}

	@JacksonXmlProperty(isAttribute = true, localName = "sensitivity_gain")
	public int getSensitivityGain()
	{
		return mSensitivityGain;
	}

	public void setSensitivityGain(int gain)
	{
		mSensitivityGain = gain;
	}

	@JacksonXmlProperty(isAttribute = true, localName = "lna_agc")
	public boolean isLnaAgc()
	{
		return mLnaAgc;
	}

	public void setLnaAgc(boolean enabled)
	{
		mLnaAgc = enabled;
	}

	@JacksonXmlProperty(isAttribute = true, localName = "mixer_agc")
	public boolean isMixerAgc()
	{
		return mMixerAgc;
	}

	public void setMixerAgc(boolean enabled)
	{
		mMixerAgc = enabled;
	}

	@JacksonXmlProperty(isAttribute = true, localName = "bias_t")
	public boolean isBiasT()
	{
		return mBiasT;
	}

	public void setBiasT(boolean enabled)
	{
		mBiasT = enabled;
	}

	@JacksonXmlProperty(isAttribute = true, localName = "gain_mode")
	public int getGainMode()
	{
		return mGainMode;
	}

	public void setGainMode(int gainMode)
	{
		mGainMode = gainMode;
	}
}
