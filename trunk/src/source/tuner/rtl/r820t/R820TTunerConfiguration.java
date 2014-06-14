/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014 Dennis Sheirer
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package source.tuner.rtl.r820t;

import javax.xml.bind.annotation.XmlAttribute;

import source.tuner.TunerConfiguration;
import source.tuner.TunerType;
import source.tuner.rtl.RTL2832TunerController.SampleRate;
import source.tuner.rtl.r820t.R820TTunerController.R820TGain;
import source.tuner.rtl.r820t.R820TTunerController.R820TLNAGain;
import source.tuner.rtl.r820t.R820TTunerController.R820TMixerGain;
import source.tuner.rtl.r820t.R820TTunerController.R820TVGAGain;

public class R820TTunerConfiguration extends TunerConfiguration
{
	private R820TGain mMasterGain = R820TGain.GAIN_327;
	private R820TMixerGain mMixerGain = R820TMixerGain.GAIN_105;
	private R820TLNAGain mLNAGain = R820TLNAGain.GAIN_222;
	private R820TVGAGain mVGAGain = R820TVGAGain.GAIN_210;
	private double mFrequencyCorrection = 0.0d;
	private SampleRate mSampleRate = SampleRate.RATE_0_912MHZ;

	/**
	 * Default constructor for JAXB
	 */
	public R820TTunerConfiguration()
	{
		this( "Default" );
	}
	
	public R820TTunerConfiguration( String name )
	{
		super( name );
	}
	
	@Override
    public TunerType getTunerType()
    {
	    return TunerType.RAFAELMICRO_R820T;
    }

	@XmlAttribute( name = "master_gain" )
	public R820TGain getMasterGain()
	{
		return mMasterGain;
	}

	public void setMasterGain( R820TGain gain )
	{
		mMasterGain = gain;
	}

	@XmlAttribute( name = "mixer_gain" )
	public R820TMixerGain getMixerGain()
	{
		return mMixerGain;
	}

	public void setMixerGain( R820TMixerGain mixerGain )
	{
		mMixerGain = mixerGain;
	}

	@XmlAttribute( name = "lna_gain" )
	public R820TLNAGain getLNAGain()
	{
		return mLNAGain;
	}

	public void setLNAGain( R820TLNAGain lnaGain )
	{
		mLNAGain = lnaGain;
	}

	@XmlAttribute( name = "vga_gain" )
	public R820TVGAGain getVGAGain()
	{
		return mVGAGain;
	}

	public void setVGAGain( R820TVGAGain vgaGain )
	{
		mVGAGain = vgaGain;
	}

	@XmlAttribute( name = "frequency_correction" )
	public double getFrequencyCorrection()
	{
		return mFrequencyCorrection;
	}
	
	public void setFrequencyCorrection( double value )
	{
		mFrequencyCorrection = value;
	}

	@XmlAttribute( name = "sample_rate" )
	public SampleRate getSampleRate()
	{
		return mSampleRate;
	}

	public void setSampleRate( SampleRate sampleRate )
	{
		mSampleRate = sampleRate;
	}
}
