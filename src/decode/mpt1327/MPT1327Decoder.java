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
package decode.mpt1327;

import instrument.Instrumentable;
import instrument.tap.Tap;
import instrument.tap.stream.BinaryTap;
import instrument.tap.stream.FloatTap;

import java.util.ArrayList;
import java.util.List;

import sample.Broadcaster;
import source.Source.SampleType;
import alias.AliasList;
import bits.MessageFramer;
import bits.SyncPattern;
import decode.Decoder;
import decode.DecoderType;
import dsp.filter.DCRemovalFilter;
import dsp.filter.FilterFactory;
import dsp.filter.Filters;
import dsp.filter.FloatFIRFilter;
import dsp.filter.FloatHalfBandFilter;
import dsp.filter.Window.WindowType;
import dsp.fsk.FSK2Decoder;
import dsp.fsk.FSK2Decoder.Output;
import dsp.nbfm.NBFMDemodulator;

/**
 * MPT1327 Decoder - 1200 baud 2FSK decoder that can process 48k sample rate
 * complex or floating point samples and output fully framed MPT1327 control and
 * traffic messages.
 */
public class MPT1327Decoder extends Decoder implements Instrumentable
{
	/* Determines how quickly the DC remove filter responds */
	private static final double sDC_REMOVAL_RATIO = 0.95;

	/* Decimated sample rate ( 48,000 / 2 = 24,000 ) feeding the decoder */
	private static final int sDECIMATED_SAMPLE_RATE = 24000;
	
	/* Baud or Symbol Rate */
	private static final int sSYMBOL_RATE = 1200;

	/* Message length -- longest possible message is: 
	 *   4xREVS + 16xSYNC + 64xADD1 + 64xDCW1 + 64xDCW2 + 64xDCW3 + 64xDCW4 */
    private static final int sMESSAGE_LENGTH = 350;
    
    /* Instrumentation Taps */
	private static final String INSTRUMENT_INPUT = "Tap Point: Input";
	private static final String INSTRUMENT_HB1_FILTER_TO_LOW_PASS = 
			"Tap Point: Half-band Decimation Filter > < Low Pass Filter";
	private static final String INSTRUMENT_LOW_PASS_TO_DECODER = 
			"Tap Point: Low Pass Filter > < Decoder";
	private static final String INSTRUMENT_DECODER_TO_FRAMER = 
			"Tap Point: Decoder > < Sync Detect/Message Framer";
    private List<Tap> mAvailableTaps;
    
	private NBFMDemodulator mNBFMDemodulator;
	private FloatHalfBandFilter mDecimationFilter;
	private FloatFIRFilter mLowPassFilter;
	private DCRemovalFilter mDCRemovalFilter; 
	private FSK2Decoder mFSKDecoder;
	private Broadcaster<Boolean> mSymbolBroadcaster;
    private MessageFramer mControlMessageFramer;
    private MessageFramer mTrafficMessageFramer;
    private MPT1327MessageProcessor mMessageProcessor;

    public MPT1327Decoder( SampleType sampleType, 
    					   AliasList aliasList,
    					   Sync sync )
	{
    	super( sampleType );

		/* If we're receiving complex samples, do FM demodulation and DC removal
		 * and feed the output back to this decoder */
		if( mSourceSampleType == SampleType.COMPLEX )
		{
			/* I/Q low pass filtering narrow band FM demodulator */
			mNBFMDemodulator = new NBFMDemodulator( 
				FilterFactory.getLowPass( 48000, 4000, 73, WindowType.HAMMING ), 
			    1.0002 );
			
			this.addComplexListener( mNBFMDemodulator );
			
			/* DC removal filter */
			mDCRemovalFilter = new DCRemovalFilter( sDC_REMOVAL_RATIO );
			
			if( mNBFMDemodulator != null )
			{
				mNBFMDemodulator.addListener( mDCRemovalFilter );
				mDCRemovalFilter.setListener( this.getFloatReceiver() );
			}
		}

		/* Demodulated float sample processing */

		/* Decimation filter - 48000 / 2 = 24000 output */
		mDecimationFilter = new FloatHalfBandFilter( 
				Filters.FIR_HALF_BAND_31T_ONE_EIGHTH_FCO, 1.0002 );
		addFloatListener( mDecimationFilter );
		
		/* Low pass filter: 2kHz to pass the 1200 & 1800 Hz FSK  */
		mLowPassFilter = new FloatFIRFilter( 
				FilterFactory.getLowPass( 24000, //sampleRate, 
										  2000,  //passFrequency, 
										  4000,  //stopFrequency, 
										  60,    //attenuation, 
										  WindowType.HANNING, //windowType, 
										  true )  //forceOddLength
					, 2.0 ); //Gain
		mDecimationFilter.setListener( mLowPassFilter );

		/* 2FSK Decoder with output inverted */
		mFSKDecoder = new FSK2Decoder( 24000, 1200, Output.INVERTED );
		mLowPassFilter.setListener( mFSKDecoder );
		
		mSymbolBroadcaster = new Broadcaster<Boolean>();
        mFSKDecoder.setListener( mSymbolBroadcaster );

        /* Message framer for control channel messages */
        mControlMessageFramer = 
        		new MessageFramer( sync.getControlSyncPattern().getPattern(), 
        						   sMESSAGE_LENGTH );
        mSymbolBroadcaster.addListener( mControlMessageFramer );

        /* Message framer for traffic channel massages */
        mTrafficMessageFramer = 
        		new MessageFramer( sync.getTrafficSyncPattern().getPattern(), 
        								sMESSAGE_LENGTH );
        mSymbolBroadcaster.addListener( mTrafficMessageFramer );

        /* Fully decoded and framed messages processor */
        mMessageProcessor = new MPT1327MessageProcessor( aliasList );
        mMessageProcessor.addMessageListener( this );
        mControlMessageFramer.addMessageListener( mMessageProcessor );
        mTrafficMessageFramer.addMessageListener( mMessageProcessor );
	}

	@Override
    public DecoderType getType()
    {
	    return DecoderType.MPT1327;
    }
	
	/**
	 * Cleanup method
	 */
	public void dispose()
	{
		super.dispose();

		mSymbolBroadcaster.dispose();

		mControlMessageFramer.dispose();

		if( mDCRemovalFilter != null )
		{
			mDCRemovalFilter.dispose();
		}

		if( mNBFMDemodulator != null )
		{
			mNBFMDemodulator.dispose();
		}
		
		mFSKDecoder.dispose();

		mDecimationFilter.dispose();

		mLowPassFilter.dispose();
		
		mMessageProcessor.dispose();

		
		mTrafficMessageFramer.dispose();
	}

	/* Instrumentation Taps */
	@Override
    public List<Tap> getTaps()
    {
		if( mAvailableTaps == null )
		{
			mAvailableTaps = new ArrayList<Tap>();
			
			mAvailableTaps.add( 
				new FloatTap( INSTRUMENT_INPUT, 0, 1.0f ) );
			mAvailableTaps.add( 
					new FloatTap( INSTRUMENT_HB1_FILTER_TO_LOW_PASS, 0, 0.5f ) );
			mAvailableTaps.add( 
					new FloatTap( INSTRUMENT_LOW_PASS_TO_DECODER, 0, 0.5f ) );
			mAvailableTaps.add( 
					new BinaryTap( INSTRUMENT_DECODER_TO_FRAMER, 0, 0.025f ) );

			/* Add the taps from the FSK decoder */
			mAvailableTaps.addAll( mFSKDecoder.getTaps() );
		}
		
	    return mAvailableTaps;
    }

	@Override
    public void addTap( Tap tap )
    {
		/* Send request to decoder */
		mFSKDecoder.addTap( tap );
		
		switch( tap.getName() )
		{
			case INSTRUMENT_INPUT:
				FloatTap inputTap = (FloatTap)tap;
				if( mNBFMDemodulator != null )
				{
					mNBFMDemodulator.addListener( inputTap );
				}
				else
				{
					addFloatListener( inputTap );
				}
				break;
			case INSTRUMENT_HB1_FILTER_TO_LOW_PASS:
				FloatTap hb1Tap = (FloatTap)tap;
				mDecimationFilter.setListener( hb1Tap );
				hb1Tap.setListener( mLowPassFilter );
				break;
			case INSTRUMENT_LOW_PASS_TO_DECODER:
				FloatTap lowTap = (FloatTap)tap;
				mLowPassFilter.setListener( lowTap );
				lowTap.setListener( mFSKDecoder );
				break;
			case INSTRUMENT_DECODER_TO_FRAMER:
				BinaryTap decoderTap = (BinaryTap)tap;
				mFSKDecoder.setListener( decoderTap );
				decoderTap.setListener( mSymbolBroadcaster );
		        break;
		}
    }

	@Override
    public void removeTap( Tap tap )
    {
		mFSKDecoder.removeTap( tap );
		
		switch( tap.getName() )
		{
			case INSTRUMENT_INPUT:
				FloatTap inputTap = (FloatTap)tap;
				if( mNBFMDemodulator != null )
				{
					mNBFMDemodulator.removeListener( inputTap );
				}
				else
				{
					removeFloatListener( inputTap );
				}
				break;
			case INSTRUMENT_HB1_FILTER_TO_LOW_PASS:
				mDecimationFilter.setListener( mLowPassFilter );
				break;
			case INSTRUMENT_LOW_PASS_TO_DECODER:
				mLowPassFilter.setListener( mFSKDecoder );
				break;
			case INSTRUMENT_DECODER_TO_FRAMER:
		        mFSKDecoder.setListener( mSymbolBroadcaster );
		        break;
		}
    }

	public enum Sync
	{
		NORMAL( "Normal", 
				SyncPattern.MPT1327_CONTROL, 
				SyncPattern.MPT1327_TRAFFIC ),
		FRENCH( "French", 
				SyncPattern.MPT1327_CONTROL_FRENCH, 
				SyncPattern.MPT1327_TRAFFIC );
		
		private String mLabel;
		private SyncPattern mControlSyncPattern;
		private SyncPattern mTrafficSyncPattern;

		private Sync( String label, 
					  SyncPattern controlPattern, 
					  SyncPattern trafficPattern )
		{
			mLabel = label;
			mControlSyncPattern = controlPattern;
			mTrafficSyncPattern = trafficPattern;
		}
		
		public String getLabel()
		{
			return mLabel;
		}
		
		public SyncPattern getControlSyncPattern()
		{
			return mControlSyncPattern;
		}
		
		public SyncPattern getTrafficSyncPattern()
		{
			return mTrafficSyncPattern;
		}
		
		public String toString()
		{
			return getLabel();
		}
	}
}
