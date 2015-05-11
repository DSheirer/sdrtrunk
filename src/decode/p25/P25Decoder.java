/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014,2015 Dennis Sheirer
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
import source.tuner.frequency.FrequencyCorrectionControl;
import alias.AliasList;
import controller.ResourceManager;
import decode.Decoder;
import decode.DecoderType;
import decode.p25.audio.P25AudioOutput;
import dsp.filter.ComplexFIRFilter;
import dsp.filter.FilterFactory;
import dsp.filter.FloatFIRFilter;
import dsp.filter.Window.WindowType;
import dsp.gain.ComplexFeedForwardGainControl;
import dsp.gain.DirectGainControl;
import dsp.nbfm.FMDiscriminator;
import dsp.psk.CQPSKDemodulator;
import dsp.psk.QPSKStarSlicer;

public class P25Decoder extends Decoder implements Instrumentable
{
	private final static Logger mLog = LoggerFactory.getLogger( P25Decoder.class );
	
	private final static int MAXIMUM_FREQUENCY_CORRECTION = 3000; //Hertz, +/-

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
	private ComplexFeedForwardGainControl mAGC;
	private DirectGainControl mDGC;

	/* Filters */
	private ComplexFIRFilter mBasebandFilter;
	private ComplexFIRFilter mRootRaisedCosineFilter;
	private FloatFIRFilter mAudioFilter;
	private C4FMSymbolFilter mSymbolFilter;
	
	/* Slicers */
	private C4FMSlicer mC4FMSlicer;
	private QPSKStarSlicer mCQPSKSlicer;
	
	/* Message framers and processors */
	private P25MessageFramer mMessageFramer;
	private P25MessageProcessor mMessageProcessor;
	
	/* Audio */
	private P25AudioOutput mAudioOutput;

	private AliasList mAliasList;
	private Modulation mModulation;
	
	public P25Decoder( ResourceManager resourceManager,
					   SampleType sampleType, 
			   		   Modulation modulation, 
					   AliasList aliasList )
	{
		super( sampleType );
		mModulation = modulation;
		
		mAliasList = aliasList;
		
		mMessageProcessor = new P25MessageProcessor( mAliasList );
		mMessageProcessor.addMessageListener( this );

		if( modulation == Modulation.CQPSK )
		{
			/* Provide message framer with reference to the demod so that it 
			 * can detect and issue corrections for costas phase lock errors */
			mCQPSKDemodulator = new CQPSKDemodulator();
	        mMessageFramer = new P25MessageFramer( mAliasList, mCQPSKDemodulator );
		}
		else
		{
	        mMessageFramer = new P25MessageFramer( mAliasList );
		}
		
        mMessageFramer.setListener( mMessageProcessor );

        /* Setup demodulation chains based on sample type (real or complex) and 
         * modulation (C4FM or CQPSK) */
		if( mSourceSampleType == SampleType.COMPLEX )
		{
			if( modulation == Modulation.CQPSK )
			{
				mBasebandFilter = new ComplexFIRFilter( FilterFactory.getLowPass( 
						48000, 7250, 8000, 60, WindowType.HANNING, true ), 1.0 );
				
				this.addComplexListener( mBasebandFilter );

				mAGC = new ComplexFeedForwardGainControl( 32 );
				mBasebandFilter.setListener( mAGC );

				/* Root raised cosine filter using 34 symbol periods and 10
				 * samples per symbol with a roll-off (alpha) value of 0.2.
				 * This should produce a filter with 341 coefficients. */
				mRootRaisedCosineFilter = new ComplexFIRFilter( FilterFactory
						.getRootRaisedCosine( 10, 34, 0.2 ), 1.0 );
				mAGC.setListener( mRootRaisedCosineFilter );

				mRootRaisedCosineFilter.setListener( mCQPSKDemodulator );
				
				mCQPSKSlicer = new QPSKStarSlicer();
				mCQPSKDemodulator.setListener( mCQPSKSlicer );
				
				mCQPSKSlicer.addListener( mMessageFramer );
			}
			else /* C4FM */
			{
				mBasebandFilter = new ComplexFIRFilter( FilterFactory.getLowPass( 
						48000, 6750, 7500, 60, WindowType.HANNING, true ), 1.0 );
				
				this.addComplexListener( mBasebandFilter );

				mFMDemodulator = new FMDiscriminator( 1.0f );
				mBasebandFilter.setListener( mFMDemodulator );
				
				/* Route output of the FM demod back to this channel so that we
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
			
			mFrequencyCorrection = new FrequencyCorrectionControl( 
					MAXIMUM_FREQUENCY_CORRECTION );
			
			mSymbolFilter = new C4FMSymbolFilter( mDGC, mFrequencyCorrection );
			mDGC.setListener( mSymbolFilter );
			
			mC4FMSlicer = new C4FMSlicer();
			mSymbolFilter.setListener( mC4FMSlicer );
			
	        mC4FMSlicer.addListener( mMessageFramer );
		}
		
		mAudioOutput = new P25AudioOutput( resourceManager );
		mMessageProcessor.addMessageListener( mAudioOutput );
	}
	
	public P25AudioOutput getAudioOutput()
	{
		return mAudioOutput;
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
				mAvailableTaps.addAll( mCQPSKDemodulator.getTaps() );
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
		
		if( mCQPSKDemodulator != null )
		{
			mCQPSKDemodulator.addTap( tap );
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
		
		if( mCQPSKDemodulator != null )
		{
			mCQPSKDemodulator.addTap( tap );
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
				
//				if( mCQPSKSlicer != null )
//				{
//					mCQPSKSlicer.removeListener( slicerTap );
//				}
				break;
		}
    }

	@Override
    public void addUnfilteredRealSampleListener( RealSampleListener listener )
    {
	    throw new IllegalArgumentException( "unfiltered real sample provider "
	    		+ "not implemented in P25 Decoder" );
    }

	public enum Modulation
	{ 
		CQPSK( "LSM SIMULCAST", "LSM" ),
		C4FM( "C4FM", "C4FM" );
		
		private String mLabel;
		private String mShortLabel;
		
		private Modulation( String label, String shortLabel )
		{
			mLabel = label;
			mShortLabel = shortLabel;
		}
		
		public String getLabel()
		{
			return mLabel;
		}
		
		public String getShortLabel()
		{
			return mShortLabel;
		}
		
		public String toString()
		{
			return getLabel();
		}
	};
}
