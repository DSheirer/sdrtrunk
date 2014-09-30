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
package source.tuner.hackrf;


import javax.xml.bind.annotation.XmlAttribute;

import source.tuner.TunerConfiguration;
import source.tuner.TunerType;
import source.tuner.hackrf.HackRFTunerController.HackRFLNAGain;
import source.tuner.hackrf.HackRFTunerController.HackRFSampleRate;
import source.tuner.hackrf.HackRFTunerController.HackRFVGAGain;

public class HackRFTunerConfiguration extends TunerConfiguration
{
	private HackRFSampleRate mSampleRate = HackRFSampleRate.RATE2_016MHZ;
	private HackRFLNAGain mLNAGain = HackRFLNAGain.GAIN_0;
	private HackRFVGAGain mVGAGain = HackRFVGAGain.GAIN_10;
	private double mFrequencyCorrection = 0.0d;

	/**
	 * Default constructor for JAXB
	 */
	public HackRFTunerConfiguration()
	{
		this( "Default" );
	}
	
	public HackRFTunerConfiguration( String name )
	{
		super( name );
	}
	
	@Override
    public TunerType getTunerType()
    {
	    return TunerType.HACKRF;
    }

	@XmlAttribute( name = "lna_gain" )
	public HackRFLNAGain getLNAGain()
	{
		return mLNAGain;
	}

	public void setLNAGain( HackRFLNAGain lnaGain )
	{
		mLNAGain = lnaGain;
	}

	@XmlAttribute( name = "vga_gain" )
	public HackRFVGAGain getVGAGain()
	{
		return mVGAGain;
	}

	public void setVGAGain( HackRFVGAGain vgaGain )
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
	public HackRFSampleRate getSampleRate()
	{
		return mSampleRate;
	}

	public void setSampleRate( HackRFSampleRate sampleRate )
	{
		mSampleRate = sampleRate;
	}
}
