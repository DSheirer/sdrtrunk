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
package decode.mdc1200;

import instrument.Instrumentable;
import instrument.tap.Tap;
import instrument.tap.stream.BinaryTap;
import instrument.tap.stream.FloatTap;

import java.util.ArrayList;
import java.util.List;

import source.Source.SampleType;
import alias.AliasList;
import bits.MessageFramer;
import bits.SyncPattern;
import decode.Decoder;
import decode.DecoderType;
import dsp.NRZDecoder;
import dsp.filter.Filters;
import dsp.filter.FloatFIRFilter;
import dsp.filter.FloatHalfBandFilter;
import dsp.fsk.FSK2Decoder;
import dsp.fsk.FSK2Decoder.Output;

/**
 * MDC1200 Decoder - 1200 baud 2FSK decoder that can process 48k sample rate
 * complex or floating point samples and output fully framed MDC1200 messages
 */
public class MDCDecoder extends Decoder implements Instrumentable
{
	/* Decimated sample rate ( 48,000 / 2 = 24,000 ) feeding the decoder */
	private static final int sDECIMATED_SAMPLE_RATE = 24000;
	
	/* Baud or Symbol Rate */
	private static final int sSYMBOL_RATE = 1200;

	/* Message length */
    private static final int sMESSAGE_LENGTH = 304;
    
    /* Instrumentation Taps */
    private ArrayList<Tap> mAvailableTaps;
	private static final String INSTRUMENT_INPUT = 
			"Tap Point: Float Input";
	private static final String INSTRUMENT_BANDPASS_FILTER_TO_FSK2_DEMOD = 
			"Tap Point: Bandpass Filter > < FSK2 Decoder";
	private static final String INSTRUMENT_FSK2_DECODER_TO_NRZ_DECODER = 
			"Tap Point: FSK2 Decoder > < NRZI Decoder";
	private static final String INSTRUMENT_NRZI_DECODER_TO_MESSAGE_FRAMER = 
			"Tap Point: NRZ Decoder > < Message Framer";
	
    private FSK2Decoder mFSKDecoder;
    private FloatHalfBandFilter mDecimationFilter;
    private FloatFIRFilter mBandPassFilter;
    private NRZDecoder mNRZDecoder;
    private MessageFramer mMessageFramer;
    private MDCMessageProcessor mMessageProcessor;
    
    public MDCDecoder( AliasList aliasList )
	{
    	super( SampleType.FLOAT );

    	/* Decimation filter: 48000 / 2 = 24000 */
    	mDecimationFilter = new FloatHalfBandFilter( 
        		Filters.FIR_HALF_BAND_31T_ONE_EIGHTH_FCO, 1.0002 );
        addFloatListener( mDecimationFilter );

        /* Bandpass filter - predefined */
        mBandPassFilter = new FloatFIRFilter( 
        		Filters.FIRBP_1200FSK_24000FS.getCoefficients(), 1.02 );
        mDecimationFilter.setListener( mBandPassFilter );

        /* 2FSK Decoder */
    	mFSKDecoder = new FSK2Decoder( sDECIMATED_SAMPLE_RATE, 
				   sSYMBOL_RATE, Output.NORMAL );
        mBandPassFilter.setListener( mFSKDecoder );

        /* NRZ Decoder */
        mNRZDecoder = new NRZDecoder( NRZDecoder.MODE_INVERTED );
        mFSKDecoder.setListener( mNRZDecoder );

        /* Message Framer */
        mMessageFramer = new MessageFramer( SyncPattern.MDC1200.getPattern(), 
				sMESSAGE_LENGTH );
        mNRZDecoder.setListener( mMessageFramer );

        /* Message Processor */
        mMessageProcessor = new MDCMessageProcessor( aliasList );
        mMessageProcessor.addMessageListener( this );
        mMessageFramer.addMessageListener( mMessageProcessor );
	}
    
    public void dispose()
    {
    	super.dispose();
    	
    	mDecimationFilter.dispose();
    	mBandPassFilter.dispose();
    	mFSKDecoder.dispose();
    	mNRZDecoder.dispose();
    	mMessageFramer.dispose();
    	mMessageProcessor.dispose();
    }

	@Override
    public DecoderType getType()
    {
	    return DecoderType.MDC1200;
    }

	/* Instrumentation */
	@Override
    public List<Tap> getTaps()
    {
		if( mAvailableTaps == null )
		{
			mAvailableTaps = new ArrayList<Tap>();
			
			mAvailableTaps.add( 
				new FloatTap( INSTRUMENT_INPUT, 0, 1.0f ) );
			mAvailableTaps.add( 
				new FloatTap( INSTRUMENT_BANDPASS_FILTER_TO_FSK2_DEMOD, 0, 0.5f ) );
			mAvailableTaps.addAll( mFSKDecoder.getTaps() );
			mAvailableTaps.add( 
				new BinaryTap( INSTRUMENT_FSK2_DECODER_TO_NRZ_DECODER, 0, 0.025f ) );
			mAvailableTaps.add( 
				new BinaryTap( INSTRUMENT_NRZI_DECODER_TO_MESSAGE_FRAMER, 0, 0.025f ) );
		}
		
	    return mAvailableTaps;
    }

	@Override
    public void addTap( Tap tap )
    {
		mFSKDecoder.addTap( tap );

		switch( tap.getName() )
		{
			case INSTRUMENT_INPUT:
				addFloatListener( (FloatTap)tap ); 
				break;
			case INSTRUMENT_BANDPASS_FILTER_TO_FSK2_DEMOD:
				/* wire the tap between the bandpass filter and the demod */
				FloatTap floatTap = (FloatTap)tap; 
		        mBandPassFilter.setListener( floatTap );
		        floatTap.setListener( mFSKDecoder );
				break;
			case INSTRUMENT_FSK2_DECODER_TO_NRZ_DECODER:
				/* wire between FSK2 decoder and NRZ decoder */
				BinaryTap binTap = (BinaryTap)tap;
				mFSKDecoder.setListener( binTap );
				binTap.setListener( mNRZDecoder );
				break;
			case INSTRUMENT_NRZI_DECODER_TO_MESSAGE_FRAMER:
				/* wire between demod and framer */
				BinaryTap binInvTap = (BinaryTap)tap;
				mNRZDecoder.setListener( binInvTap );
				binInvTap.setListener( mMessageFramer );
				break;
			default:
		}
    }

	@Override
    public void removeTap( Tap tap )
    {
		mFSKDecoder.removeTap( tap );
		
		switch( tap.getName() )
		{
			case INSTRUMENT_INPUT:
				removeFloatListener( (FloatTap)tap );
				break;
			case INSTRUMENT_BANDPASS_FILTER_TO_FSK2_DEMOD:
		        mBandPassFilter.setListener( mFSKDecoder );
				break;
			case INSTRUMENT_FSK2_DECODER_TO_NRZ_DECODER:
				mFSKDecoder.setListener( mNRZDecoder );
				break;
			case INSTRUMENT_NRZI_DECODER_TO_MESSAGE_FRAMER:
				mNRZDecoder.setListener( mMessageFramer );
		        break;
			default:
		}
    }
}
