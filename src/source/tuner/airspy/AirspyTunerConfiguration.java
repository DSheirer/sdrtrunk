/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2015 Dennis Sheirer
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
package source.tuner.airspy;

import javax.xml.bind.annotation.XmlAttribute;

import source.tuner.TunerConfiguration;
import source.tuner.TunerType;
import source.tuner.airspy.AirspyTunerController.Gain;

public class AirspyTunerConfiguration extends TunerConfiguration
{
	private int mSampleRate = AirspyTunerController.DEFAULT_SAMPLE_RATE.getRate();
	private Gain mGain = AirspyTunerController.LINEARITY_GAIN_DEFAULT;
	private int mIFGain = AirspyTunerController.IF_GAIN_DEFAULT;
	private int mMixerGain = AirspyTunerController.MIXER_GAIN_DEFAULT;
	private int mLNAGain = AirspyTunerController.LNA_GAIN_DEFAULT;
	
	private boolean mMixerAGC = false;
	private boolean mLNAAGC = false;

	/**
	 * Default constructor for JAXB
	 */
	public AirspyTunerConfiguration()
	{
		this( "Default" );
	}
	
	public AirspyTunerConfiguration( String name )
	{
		super( name );
	}
	
	@Override
    public TunerType getTunerType()
    {
	    return TunerType.AIRSPY_R820T;
    }

	@XmlAttribute( name = "sample_rate" )
	public int getSampleRate()
	{
		return mSampleRate;
	}
	
	public void setSampleRate( int sampleRate )
	{
		mSampleRate = sampleRate;
	}
	
	@XmlAttribute( name = "gain" )
	public Gain getGain()
	{
		return mGain;
	}
	
	public void setGain( Gain gain )
	{
		mGain = gain;
	}
	
	@XmlAttribute( name = "if_gain" )
	public int getIFGain()
	{
		return mIFGain;
	}
	
	public void setIFGain( int gain )
	{
		mIFGain = gain;
	}
	
	@XmlAttribute( name = "mixer_gain" )
	public int getMixerGain()
	{
		return mMixerGain;
	}
	
	public void setMixerGain( int gain )
	{
		mMixerGain = gain;
	}

	@XmlAttribute( name = "lna_gain" )
	public int getLNAGain()
	{
		return mLNAGain;
	}
	
	public void setLNAGain( int gain )
	{
		mLNAGain = gain;
	}

	@XmlAttribute( name = "mixer_agc" )
	public boolean isMixerAGC()
	{
		return mMixerAGC;
	}
	
	public void setMixerAGC( boolean enabled )
	{
		mMixerAGC = enabled;
	}

	@XmlAttribute( name = "lna_agc" )
	public boolean isLNAAGC()
	{
		return mLNAAGC;
	}
	
	public void setLNAAGC( boolean enabled )
	{
		mLNAAGC = enabled;
	}
}
