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
import instrument.tap.stream.DibitTap;
import instrument.tap.stream.FloatTap;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.real.RealSampleListener;
import source.Source.SampleType;
import source.tuner.DirectFrequencyController;
import source.tuner.FrequencyChangeListener;
import alias.AliasList;
import decode.Decoder;
import decode.DecoderType;
import dsp.filter.ComplexFIRFilter;
import dsp.filter.FilterFactory;
import dsp.filter.FloatFIRFilter;
import dsp.filter.Window.WindowType;
import dsp.fsk.C4FMSlicer;
import dsp.fsk.C4FMSymbolFilter;
import dsp.fsk.P25MessageFramer;
import dsp.gain.DirectGainControl;
import dsp.nbfm.FMDiscriminator;
import dsp.nbfm.FilteringNBFMDemodulator;

public class P25Decoder extends Decoder 
			implements DirectFrequencyController, Instrumentable
{
	private final static Logger mLog = LoggerFactory.getLogger( P25Decoder.class );

    /* Instrumentation Taps */
	private static final String INSTRUMENT_INPUT_TO_AGC = "Tap Point: Input to AGC";
	private static final String INSTRUMENT_AGC_TO_SYMBOL_FILTER = "Tap Point: AGC to Symbol Filter";
	private static final String INSTRUMENT_SYMBOL_FILTER_TO_SLICER = "Tap Point: Symbol Filter to Slicer";
	private static final String INSTRUMENT_SLICER_OUTPUT = "Tap Point: Slicer Output";
    private List<Tap> mAvailableTaps;
	
	private ComplexFIRFilter mBasebandFilter = new ComplexFIRFilter( 
		FilterFactory.getLowPass( 48000, 6000, 7000, 48, WindowType.HANNING, true ), 1.0 );
	private FMDiscriminator mDemodulator = new FMDiscriminator( 1 );
	private FloatFIRFilter mAudioFilter = new FloatFIRFilter( 
		FilterFactory.getLowPass( 48000, 3000, 4000, 48, WindowType.HAMMING, true ), 1.0 );
	
	private DirectGainControl mGainControl = 
						new DirectGainControl( 15.0f, 0.1f, 25.0f, 0.3f );
	private C4FMSymbolFilter mSymbolFilter;
	private C4FMSlicer mSlicer = new C4FMSlicer();
	private P25MessageFramer mNormalFramer;
	private P25MessageFramer mInvertedFramer;
	private P25MessageProcessor mMessageProcessor;

	private AliasList mAliasList;
	
	public P25Decoder( SampleType sampleType, AliasList aliasList )
	{
		super( sampleType );
		
		mAliasList = aliasList;
		
		mSymbolFilter = new C4FMSymbolFilter( mGainControl );

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

		addRealSampleListener( mAudioFilter );
		mAudioFilter.setListener( mGainControl );
		mGainControl.setListener( mSymbolFilter );
		
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
		if( mAvailableTaps == null )
		{
			mAvailableTaps = new ArrayList<Tap>();

			mAvailableTaps.add( new FloatTap( INSTRUMENT_INPUT_TO_AGC, 0, 1.0f ) );
			mAvailableTaps.add( new FloatTap( INSTRUMENT_AGC_TO_SYMBOL_FILTER, 0, 1.0f ) );
			mAvailableTaps.add( new FloatTap( INSTRUMENT_SYMBOL_FILTER_TO_SLICER, 0, 0.1f ) );
			mAvailableTaps.add( new DibitTap( INSTRUMENT_SLICER_OUTPUT, 0, 0.1f ) );
			
			mAvailableTaps.addAll( mSymbolFilter.getTaps() );
		}
		
		return mAvailableTaps;
    }

	@Override
    public void addTap( Tap tap )
    {
		mSymbolFilter.addTap( tap );
		
		switch( tap.getName() )
		{
			case INSTRUMENT_INPUT_TO_AGC:
				FloatTap inputAGC = (FloatTap)tap;
				removeRealListener( mGainControl );
				addRealSampleListener( inputAGC );
				inputAGC.setListener( mGainControl );
				break;
			case INSTRUMENT_AGC_TO_SYMBOL_FILTER:
				FloatTap agcSymbol = (FloatTap)tap;
				mGainControl.setListener( agcSymbol );
				agcSymbol.setListener( mSymbolFilter );
				break;
			case INSTRUMENT_SYMBOL_FILTER_TO_SLICER:
				FloatTap symbolSlicer = (FloatTap)tap;
				mSymbolFilter.setListener( symbolSlicer );
				symbolSlicer.setListener( mSlicer );
				break;
			case INSTRUMENT_SLICER_OUTPUT:
				DibitTap slicer = (DibitTap)tap;
				mSlicer.addListener( slicer );
				break;
		}
    }

	@Override
    public void removeTap( Tap tap )
    {
		mSymbolFilter.removeTap( tap );
		
		switch( tap.getName() )
		{
			case INSTRUMENT_INPUT_TO_AGC:
				FloatTap inputAGC = (FloatTap)tap;
				removeRealListener( inputAGC );
				addRealSampleListener( mGainControl );
				break;
			case INSTRUMENT_AGC_TO_SYMBOL_FILTER:
				mGainControl.setListener( mSymbolFilter );
				break;
			case INSTRUMENT_SYMBOL_FILTER_TO_SLICER:
				mSymbolFilter.setListener( mSlicer );
				break;
			case INSTRUMENT_SLICER_OUTPUT:
				DibitTap slicerTap = (DibitTap)tap;
				mSlicer.removeListener( slicerTap );
				break;
		}
    }

	@Override
    public void addUnfilteredRealSampleListener( RealSampleListener listener )
    {
	    throw new IllegalArgumentException( "unfiltered real sample provider "
	    		+ "not implemented in P25 Decoder" );
    }

	/**
	 * Registers a frequency change listener to receive frequency correction
	 * events from the symbol filter
	 */
	@Override
	public void setListener( FrequencyChangeListener listener )
	{
		mSymbolFilter.addListener( listener );
	}

	@Override
	public long getFrequencyCorrection()
	{
		return mSymbolFilter.getFrequencyCorrection();
	}
}
