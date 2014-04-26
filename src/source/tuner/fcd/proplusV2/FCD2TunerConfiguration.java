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
package source.tuner.fcd.proplusV2;

import javax.xml.bind.annotation.XmlAttribute;

import source.tuner.TunerConfiguration;
import source.tuner.TunerType;

public class FCD2TunerConfiguration extends TunerConfiguration
{
	private boolean mGainLNA = true;
	private boolean mGainMixer = true;
	private double mFrequencyCorrection = -2.2d;

	/**
	 * Default constructor for JAXB
	 */
	public FCD2TunerConfiguration()
	{
		this( "Default" );
	}
	
	public FCD2TunerConfiguration( String name )
	{
		super( name );
	}
	
	@Override
    public TunerType getTunerType()
    {
	    return TunerType.FUNCUBE_DONGLE_PRO_PLUS;
    }

	@XmlAttribute( name = "lna_gain" )
	public boolean getGainLNA()
	{
		return mGainLNA;
	}
	
	public void setGainLNA( boolean gain )
	{
		mGainLNA = gain;
	}
	
	@XmlAttribute( name = "mixer_gain" )
	public boolean getGainMixer()
	{
		return mGainMixer;
	}
	
	public void setGainMixer( boolean gain )
	{
		mGainMixer = gain;
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
}
