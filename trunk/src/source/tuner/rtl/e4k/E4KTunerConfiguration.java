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
package source.tuner.rtl.e4k;

import javax.xml.bind.annotation.XmlAttribute;

import source.tuner.TunerConfiguration;
import source.tuner.TunerType;
import source.tuner.rtl.RTL2832TunerController.SampleRate;
import source.tuner.rtl.e4k.E4KTunerController.E4KEnhanceGain;
import source.tuner.rtl.e4k.E4KTunerController.E4KGain;
import source.tuner.rtl.e4k.E4KTunerController.E4KLNAGain;
import source.tuner.rtl.e4k.E4KTunerController.E4KMixerGain;

public class E4KTunerConfiguration extends TunerConfiguration
{
	private E4KGain mMasterGain = E4KGain.AUTOMATIC;
	private E4KMixerGain mMixerGain = E4KMixerGain.AUTOMATIC;
	private E4KLNAGain mLNAGain = E4KLNAGain.AUTOMATIC;
	private E4KEnhanceGain mEnhanceGain = E4KEnhanceGain.AUTOMATIC;
	private double mFrequencyCorrection = 8.9d;
	private SampleRate mSampleRate = SampleRate.RATE_1_200MHZ;

	/**
	 * Default constructor for JAXB
	 */
	public E4KTunerConfiguration()
	{
		this( "Default" );
	}
	
	public E4KTunerConfiguration( String name )
	{
		super( name );
	}
	
	@Override
    public TunerType getTunerType()
    {
	    return TunerType.ELONICS_E4000;
    }

	@XmlAttribute( name="master_gain" )
	public E4KGain getMasterGain()
	{
		return mMasterGain;
	}

	public void setMasterGain( E4KGain gain )
	{
		mMasterGain = gain;
	}

	@XmlAttribute( name="mixer_gain" )
	public E4KMixerGain getMixerGain()
	{
		return mMixerGain;
	}

	public void setMixerGain( E4KMixerGain mixerGain )
	{
		mMixerGain = mixerGain;
	}

	@XmlAttribute( name="lna_gain" )
	public E4KLNAGain getLNAGain()
	{
		return mLNAGain;
	}

	public void setLNAGain( E4KLNAGain lnaGain )
	{
		mLNAGain = lnaGain;
	}

	@XmlAttribute( name="enhance_gain" )
	public E4KEnhanceGain getEnhanceGain()
	{
		return mEnhanceGain;
	}

	public void setEnhanceGain( E4KEnhanceGain enhanceGain )
	{
		mEnhanceGain = enhanceGain;
	}

	@XmlAttribute( name="frequency_correction" )
	public double getFrequencyCorrection()
	{
		return mFrequencyCorrection;
	}
	
	public void setFrequencyCorrection( double value )
	{
		mFrequencyCorrection = value;
	}

	@XmlAttribute( name="sample_rate" )
	public SampleRate getSampleRate()
	{
		return mSampleRate;
	}

	public void setSampleRate( SampleRate sampleRate )
	{
		mSampleRate = sampleRate;
	}

}
