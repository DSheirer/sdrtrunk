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
import instrument.tap.stream.ComplexTap;
import instrument.tap.stream.DibitTap;
import instrument.tap.stream.FloatTap;
import instrument.tap.stream.QPSKTap;

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
import dsp.gain.ComplexAutomaticGainControl;
import dsp.gain.DirectGainControl;
import dsp.nbfm.FMDiscriminator;
import dsp.psk.CQPSKDemodulator;
import dsp.symbol.FrameSync;

public class P25Decoder extends Decoder 
			implements DirectFrequencyController, Instrumentable
{
	private final static Logger mLog = LoggerFactory.getLogger( P25Decoder.class );

    /* Instrumentation Taps */
	private static final String INSTRUMENT_COMPLEX_INPUT = "Tap Point: Complex Input";
	private static final String INSTRUMENT_BASEBAND_FILTER_OUTPUT = "Tap Point: Baseband Filter Output";

	private static final String INSTRUMENT_AGC_OUTPUT = "Tap Point: AGC Output";
	private static final String INSTRUMENT_QPSK_DEMODULATOR_OUTPUT = "Tap Point: CQPSK Demodulator Output";
	private static final String INSTRUMENT_CQPSK_SLICER_OUTPUT = "Tap Point: CQPSK Slicer Output";

	private static final String INSTRUMENT_REAL_INPUT = "Tap Point: Real Input";
	private static final String INSTRUMENT_DGC_OUTPUT = "Tap Point: DGC Output";
	private static final String INSTRUMENT_C4FM_SYMBOL_FILTER_OUTPUT = "Tap Point: Symbol Filter Output";
	private static final String INSTRUMENT_C4FM_SLICER_OUTPUT = "Tap Point: C4FM Slicer Output";

	private List<Tap> mAvailableTaps;

    /* Demods */
	private FMDiscriminator mFMDemodulator;
	private CQPSKDemodulator mCQPSKDemodulator;
    
    /* Gain */
	private ComplexAutomaticGainControl mAGC;
	private DirectGainControl mDGC;

	/* Filters */
	private ComplexFIRFilter mBasebandFilter;
	private ComplexFIRFilter mRootRaisedCosineFilter;
	private FloatFIRFilter mAudioFilter;
	private C4FMSymbolFilter mSymbolFilter;
	
	/* Slicers */
	private C4FMSlicer mC4FMSlicer;
	private CQPSKSlicer mCQPSKSlicer;
	
	/* Message framers and handlers */
	private P25MessageFramer mNormalFramer;
	private P25MessageFramer mInvertedFramer;
	private P25MessageProcessor mMessageProcessor;

	private AliasList mAliasList;
	private Modulation mModulation;
	
	public P25Decoder( SampleType sampleType, 
			   		   Modulation modulation, 
					   AliasList aliasList )
	{
		super( sampleType );
		mModulation = modulation;
		
		mAliasList = aliasList;
		
		mMessageProcessor = new P25MessageProcessor( mAliasList );
		mMessageProcessor.addMessageListener( this );

        mNormalFramer = new P25MessageFramer( 
                FrameSync.P25_PHASE1.getSync(), 64, false, mAliasList );
        mNormalFramer.setListener( mMessageProcessor );

        mInvertedFramer = new P25MessageFramer( 
                FrameSync.P25_PHASE1_INVERTED.getSync(), 64, true, mAliasList );
        mInvertedFramer.setListener( mMessageProcessor );

        /* Setup demodulation chains based on sample type (real or complex) and 
         * modulation (C4FM or CQPSK) */
		if( mSourceSampleType == SampleType.COMPLEX )
		{
			mBasebandFilter = new ComplexFIRFilter( FilterFactory.getLowPass( 
					48000, 6500, 8000, 48, WindowType.HANNING, true ), 1.0 );
			
			if( modulation == Modulation.CQPSK )
			{
				this.addComplexListener( mBasebandFilter );
				
				mAGC = new ComplexAutomaticGainControl();
				mBasebandFilter.setListener( mAGC );
				
				mCQPSKDemodulator = new CQPSKDemodulator();
				mAGC.setListener( mCQPSKDemodulator );
				
				mCQPSKSlicer = new CQPSKSlicer();
				mCQPSKDemodulator.setListener( mCQPSKSlicer );
				
				mCQPSKSlicer.addListener( mNormalFramer );
				mCQPSKSlicer.addListener( mInvertedFramer );
			}
			else /* C4FM */
			{
				this.addComplexListener( mBasebandFilter );
				
				mFMDemodulator = new FMDiscriminator( 1.0f );
				mBasebandFilter.setListener( mFMDemodulator );
				
				/* Route output of the fm demod back to this channel so that we
				 * can process the output as if it were coming from any other
				 * real sample source */
				mFMDemodulator.setListener( getRealReceiver() );
			}
		}

		/* Processing chain for real samples & demodulated complex samples */
		if( modulation == Modulation.C4FM )
		{
			mAudioFilter = new FloatFIRFilter( FilterFactory.getLowPass( 48000, 
					3000, 4000, 48, WindowType.HAMMING, true ), 1.0 );			
			addRealSampleListener( mAudioFilter );
			
			mDGC = new DirectGainControl( 15.0f, 0.1f, 35.0f, 0.3f );
			mAudioFilter.setListener( mDGC );
			
			mSymbolFilter = new C4FMSymbolFilter( mDGC );
			mDGC.setListener( mSymbolFilter );
			
			mC4FMSlicer = new C4FMSlicer();
			mSymbolFilter.setListener( mC4FMSlicer );
			
	        mC4FMSlicer.addListener( mNormalFramer );
	        mC4FMSlicer.addListener( mInvertedFramer );
		}
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

			if( mSourceSampleType == SampleType.COMPLEX )
			{
				mAvailableTaps.add( new ComplexTap( INSTRUMENT_COMPLEX_INPUT, 0, 1.0f ) );
				mAvailableTaps.add( new ComplexTap( INSTRUMENT_BASEBAND_FILTER_OUTPUT, 0, 1.0f ) );
			}
			else
			{
				mAvailableTaps.add( new FloatTap( INSTRUMENT_REAL_INPUT, 0, 1.0f ) );
			}
			
			if( mModulation == Modulation.C4FM )
			{
				mAvailableTaps.add( new FloatTap( INSTRUMENT_DGC_OUTPUT, 0, 1.0f ) );
				mAvailableTaps.add( new FloatTap( INSTRUMENT_C4FM_SYMBOL_FILTER_OUTPUT, 0, 0.1f ) );
				mAvailableTaps.add( new DibitTap( INSTRUMENT_C4FM_SLICER_OUTPUT, 0, 0.1f ) );

				if( mSymbolFilter != null )
				{
					mAvailableTaps.addAll( mSymbolFilter.getTaps() );
				}
			}
			else
			{
				mAvailableTaps.add( new ComplexTap( INSTRUMENT_AGC_OUTPUT, 0, 1.0f ) );
				mAvailableTaps.add( new QPSKTap( INSTRUMENT_QPSK_DEMODULATOR_OUTPUT, 0, 1.0f ) );
				mAvailableTaps.add( new DibitTap( INSTRUMENT_CQPSK_SLICER_OUTPUT, 0, 0.1f ) );
			}
		}
		
		return mAvailableTaps;
    }

	@Override
    public void addTap( Tap tap )
    {
		if( mSymbolFilter != null )
		{
			mSymbolFilter.addTap( tap );
		}
		
		switch( tap.getName() )
		{
			case INSTRUMENT_COMPLEX_INPUT:
				addComplexListener( (ComplexTap)tap );
				break;
			case INSTRUMENT_BASEBAND_FILTER_OUTPUT:
				ComplexTap baseband = (ComplexTap)tap;
				mBasebandFilter.setListener( baseband );
				baseband.setListener( mAGC );
				break;
			case INSTRUMENT_AGC_OUTPUT:
				ComplexTap agcSymbol = (ComplexTap)tap;
				mAGC.setListener( agcSymbol );
				agcSymbol.setListener( mCQPSKDemodulator );
				break;
			case INSTRUMENT_QPSK_DEMODULATOR_OUTPUT:
				QPSKTap qpsk = (QPSKTap)tap;
				mCQPSKDemodulator.setListener( qpsk );
				qpsk.setListener( mCQPSKSlicer );
				break;
			case INSTRUMENT_CQPSK_SLICER_OUTPUT:
				mCQPSKSlicer.addListener( (DibitTap)tap );
				break;
			case INSTRUMENT_REAL_INPUT:
				FloatTap inputAGC = (FloatTap)tap;
				removeRealListener( mDGC );
				addRealSampleListener( inputAGC );
				inputAGC.setListener( mDGC );
				break;
			case INSTRUMENT_DGC_OUTPUT:
				FloatTap dgcSymbol = (FloatTap)tap;
				mDGC.setListener( dgcSymbol );
				dgcSymbol.setListener( mSymbolFilter );
				break;
			case INSTRUMENT_C4FM_SYMBOL_FILTER_OUTPUT:
				FloatTap symbolSlicer = (FloatTap)tap;
				mSymbolFilter.setListener( symbolSlicer );
				symbolSlicer.setListener( mC4FMSlicer );
				break;
			case INSTRUMENT_C4FM_SLICER_OUTPUT:
				DibitTap slicer = (DibitTap)tap;
				
				if( mC4FMSlicer != null )
				{
					mC4FMSlicer.addListener( slicer );
				}
				
				if( mCQPSKSlicer != null )
				{
					mCQPSKSlicer.addListener( slicer );
				}
				break;
		}
    }

	@Override
    public void removeTap( Tap tap )
    {
		if( mSymbolFilter != null )
		{
			mSymbolFilter.removeTap( tap );
		}
		
		switch( tap.getName() )
		{
			case INSTRUMENT_COMPLEX_INPUT:
				removeComplexListener( (ComplexTap)tap );
				break;
			case INSTRUMENT_BASEBAND_FILTER_OUTPUT:
				mBasebandFilter.setListener( mAGC );
				break;
			case INSTRUMENT_AGC_OUTPUT:
				mAGC.setListener( mCQPSKDemodulator );
				break;
			case INSTRUMENT_QPSK_DEMODULATOR_OUTPUT:
				mCQPSKDemodulator.setListener( mCQPSKSlicer );
				break;
			case INSTRUMENT_CQPSK_SLICER_OUTPUT:
				mCQPSKSlicer.removeListener( (DibitTap)tap );
				break;
			case INSTRUMENT_REAL_INPUT:
				FloatTap inputAGC = (FloatTap)tap;
				removeRealListener( inputAGC );
				addRealSampleListener( mDGC );
				break;
			case INSTRUMENT_DGC_OUTPUT:
				mDGC.setListener( mSymbolFilter );
				break;
			case INSTRUMENT_C4FM_SYMBOL_FILTER_OUTPUT:
				mSymbolFilter.setListener( mC4FMSlicer );
				break;
			case INSTRUMENT_C4FM_SLICER_OUTPUT:
				DibitTap slicerTap = (DibitTap)tap;
				
				if( mC4FMSlicer != null )
				{
					mC4FMSlicer.removeListener( slicerTap );
				}
				
				if( mCQPSKSlicer != null )
				{
					mCQPSKSlicer.removeListener( slicerTap );
				}
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
		if( mSymbolFilter != null )
		{
			mSymbolFilter.addListener( listener );
		}
	}

	@Override
	public long getFrequencyCorrection()
	{
		if( mSymbolFilter != null )
		{
			return mSymbolFilter.getFrequencyCorrection();
		}
		
		return 0;
	}
	
	public enum Modulation
	{ 
		C4FM( "C4FM" ), 
		CQPSK( "LSM SIMULCAST" );
		
		private String mLabel;
		
		private Modulation( String label )
		{
			mLabel = label;
		}
		
		public String getLabel()
		{
			return mLabel;
		}
		
		public String toString()
		{
			return getLabel();
		}
	};
	
}
