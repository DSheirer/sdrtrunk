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

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.real.RealSampleListener;
import source.Source.SampleType;
import alias.AliasList;
import decode.Decoder;
import decode.DecoderType;
import dsp.filter.ComplexFIRFilter;
import dsp.filter.FilterFactory;
import dsp.filter.Window.WindowType;
import dsp.fsk.P25MessageFramer;
import dsp.fsk.C4FMSlicer;
import dsp.fsk.C4FMSymbolFilter;
import dsp.nbfm.FMDiscriminator;

public class P25Decoder extends Decoder implements Instrumentable
{
	private final static Logger mLog = LoggerFactory.getLogger( P25Decoder.class );

	private ComplexFIRFilter mBasebandFilter = new ComplexFIRFilter( 
			FilterFactory.getLowPass( 48000, 5000, 31, WindowType.HAMMING ), 1.0 );

	private FMDiscriminator mDemodulator = new FMDiscriminator( 1 );
	private C4FMSymbolFilter mSymbolFilter = new C4FMSymbolFilter();
	private C4FMSlicer mSlicer = new C4FMSlicer();
	private P25MessageFramer mNormalFramer;
	private P25MessageFramer mInvertedFramer;
	private P25MessageProcessor mMessageProcessor;

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
			mBasebandFilter.setListener( mDemodulator );
			mDemodulator.setListener( getRealReceiver() );
		}

		addRealSampleListener( mSymbolFilter );
		
		mSymbolFilter.setListener( mSlicer );
		
		mMessageProcessor = new P25MessageProcessor( mAliasList );
		mMessageProcessor.addMessageListener( this );

        mNormalFramer = new P25MessageFramer( 
                FrameSync.P25_PHASE1.getSync(), 64, false, mAliasList );
        mSlicer.addListener( mNormalFramer );
        mNormalFramer.setListener( mMessageProcessor );

        mInvertedFramer = new P25MessageFramer( 
                FrameSync.P25_PHASE1_INVERTED.getSync(), 64, true, mAliasList );
        mSlicer.addListener( mInvertedFramer );
        mInvertedFramer.setListener( mMessageProcessor );
		
	}

	@Override
    public DecoderType getType()
    {
	    return DecoderType.P25_PHASE1;
    }

	@Override
    public List<Tap> getTaps()
    {
		return new ArrayList<Tap>();
    }

	@Override
    public void addTap( Tap tap )
    {
    }

	@Override
    public void removeTap( Tap tap )
    {
    }

	@Override
    public void addUnfilteredRealSampleListener( RealSampleListener listener )
    {
	    throw new IllegalArgumentException( "unfiltered real sample provider "
	    		+ "not implemented in P25 Decoder" );
    }
}
