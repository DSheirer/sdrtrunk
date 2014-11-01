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
package decode.p25;

import instrument.Instrumentable;
import instrument.tap.Tap;

import java.util.List;

import sample.real.RealSampleListener;
import source.Source.SampleType;
import alias.AliasList;
import decode.Decoder;
import decode.DecoderType;
import dsp.filter.ComplexFIRFilter;
import dsp.filter.FilterFactory;
import dsp.filter.FloatFIRFilter;
import dsp.filter.Window.WindowType;
import dsp.fsk.C4FMDecoder;
import dsp.nbfm.FMDiscriminator;

public class P25Decoder extends Decoder implements Instrumentable
{
	private ComplexFIRFilter mBasebandFilter = new ComplexFIRFilter( 
		FilterFactory.getLowPass( 48000, 6250, 73, WindowType.HAMMING ), 1.0 );
	private FMDiscriminator mFMDiscriminator = new FMDiscriminator( 1 );
//	private FloatFIRFilter mAudioFilter = new FloatFIRFilter( FilterFactory
//			.getLowPass( 48000, 6000, 73, WindowType.HAMMING ), 1.0 );
	private C4FMDecoder mC4FMDecoder = new C4FMDecoder();
	private AliasList mAliasList;
	
	public P25Decoder( SampleType sampleType, AliasList aliasList )
	{
		super( sampleType );
		
		mAliasList = aliasList;
		
		/**
		 * Only setup a demod chain if we're receiving complex samples.  If
		 * we're receiving demodulated samples, they'll be handled the same 
		 * way as we handle the output of the demodulator.
		 */
		if( mSourceSampleType == SampleType.COMPLEX )
		{
			/**
			 * The Decoder super class is both a Complex listener and a float
			 * listener.  So, we add the demod to listen to the incoming 
			 * quadrature samples, and we wire the output of the demod right
			 * back to this class, so we can receive the demodulated output
			 * to process
			 */
			this.addComplexListener( mBasebandFilter );
			mBasebandFilter.setListener( mFMDiscriminator );
			mFMDiscriminator.setListener( getRealReceiver() );
//			mFMDiscriminator.setListener( mAudioFilter );
//			mAudioFilter.setListener( getRealReceiver() );
		}

		addRealSampleListener( mC4FMDecoder );
	}

	@Override
    public DecoderType getType()
    {
	    return DecoderType.P25_PHASE1;
    }

	@Override
    public List<Tap> getTaps()
    {
		return mC4FMDecoder.getTaps();
    }

	@Override
    public void addTap( Tap tap )
    {
		mC4FMDecoder.addTap( tap );
    }

	@Override
    public void removeTap( Tap tap )
    {
		mC4FMDecoder.removeTap( tap );
    }

	@Override
    public void addUnfilteredRealSampleListener( RealSampleListener listener )
    {
	    throw new IllegalArgumentException( "unfiltered real sample provider not implemented in P25 Decoder" );
    }
}
