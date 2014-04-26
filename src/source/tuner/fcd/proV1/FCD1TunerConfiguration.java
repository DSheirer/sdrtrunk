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
package source.tuner.fcd.proV1;

import javax.xml.bind.annotation.XmlAttribute;

import source.tuner.TunerConfiguration;
import source.tuner.TunerType;
import source.tuner.fcd.proV1.FCD1TunerController.LNAEnhance;
import source.tuner.fcd.proV1.FCD1TunerController.LNAGain;
import source.tuner.fcd.proV1.FCD1TunerController.MixerGain;

public class FCD1TunerConfiguration extends TunerConfiguration
{
	private double mFrequencyCorrection = 22.0d;
	private double mInphaseDCCorrection = 0.0d;
	private double mQuadratureDCCorrection = 0.0d;
	private double mPhaseCorrection = 0.0d;
	private double mGainCorrection = 0.0d;
	
	private LNAGain mLNAGain = LNAGain.LNA_GAIN_PLUS_20_0;
	private LNAEnhance mLNAEnhance = LNAEnhance.LNA_ENHANCE_OFF;
	private MixerGain mMixerGain = MixerGain.MIXER_GAIN_PLUS_12_0;

	/**
	 * Default constructor for JAXB
	 */
	public FCD1TunerConfiguration()
	{
		this( "Default" );
	}
	
	public FCD1TunerConfiguration( String name )
	{
		super( name );
	}
	
	@Override
    public TunerType getTunerType()
    {
	    return TunerType.FUNCUBE_DONGLE_PRO;
    }

	public LNAGain getLNAGain()
	{
		return mLNAGain;
	}
	
	public void setLNAGain( LNAGain gain )
	{
		mLNAGain = gain;
	}
	
	@XmlAttribute( name = "lna_gain" )
	public LNAEnhance getLNAEnhance()
	{
		return mLNAEnhance;
	}
	
	public void setLNAEnhance( LNAEnhance enhance )
	{
		mLNAEnhance = enhance;
	}
	
	@XmlAttribute( name = "mixer_gain" )
	public MixerGain getMixerGain()
	{
		return mMixerGain;
	}
	
	public void setMixerGain( MixerGain gain )
	{
		mMixerGain = gain;
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

	@XmlAttribute( name = "inphase_dc_correction" )
	public double getInphaseDCCorrection()
	{
		return mInphaseDCCorrection;
	}

	public void setInphaseDCCorrection( double value )
	{
		mInphaseDCCorrection = value;
	}

	@XmlAttribute( name = "quadrature_dc_correction" )
	public double getQuadratureDCCorrection()
	{
		return mQuadratureDCCorrection;
	}

	public void setQuadratureDCCorrection( double value )
	{
		mQuadratureDCCorrection = value;
	}

	@XmlAttribute( name = "phase_correction" )
	public double getPhaseCorrection()
	{
		return mPhaseCorrection;
	}

	public void setPhaseCorrection( double value )
	{
		mPhaseCorrection = value;
	}

	@XmlAttribute( name = "gain_correction" )
	public double getGainCorrection()
	{
		return mGainCorrection;
	}

	public void setGainCorrection( double value )
	{
		mGainCorrection = value;
	}
}
