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
package decode.fleetsync2;

import instrument.Instrumentable;
import instrument.tap.Tap;
import instrument.tap.stream.BinaryTap;
import instrument.tap.stream.FloatTap;

import java.util.ArrayList;
import java.util.List;

import sample.Listener;
import source.Source.SampleType;
import alias.AliasList;
import bits.MessageFramer;
import bits.SyncPattern;
import decode.Decoder;
import decode.DecoderType;
import dsp.filter.Filters;
import dsp.filter.FloatFIRFilter;
import dsp.filter.FloatHalfBandFilter;
import dsp.fsk.FSK2Decoder;
import dsp.fsk.FSK2Decoder.Output;

/**
 * Fleetsync II Decoder - 1200 baud 2FSK decoder that can process 48k sample rate
 * complex or floating point samples and output fully framed Fleetsync II
 * messages
 */
public class Fleetsync2Decoder extends Decoder implements Instrumentable
{
	/* Decimated sample rate ( 48,000 / 2 = 24,000 ) feeding the decoder */
	private static final int sDECIMATED_SAMPLE_RATE = 24000;
	
	/* Baud or Symbol Rate */
	private static final int sSYMBOL_RATE = 1200;

	/* Message length - 5 x REVS + 16 x SYNC + 8 x 64Bit Blocks */
    private static final int sMESSAGE_LENGTH = 537;
    
    /* Instrumentation Taps */
    private ArrayList<Tap> mAvailableTaps;
	private static final String INSTRUMENT_INPUT = 
			"Tap Point: Float Input";
	private static final String INSTRUMENT_BANDPASS_FILTER_TO_FSK2_DEMOD = 
			"Tap Point: Bandpass Filter > < FSK2 Decoder";
	private static final String INSTRUMENT_FSK2_DECODER_TO_MESSAGE_FRAMER = 
			"Tap Point: FSK2 Decoder > < Message Framer";
	
    private FSK2Decoder mFSKDecoder;
    private FloatHalfBandFilter mDecimationFilter;
    private FloatFIRFilter mBandPassFilter;
    private MessageFramer mMessageFramer;
    private Fleetsync2MessageProcessor mMessageProcessor;
    
    public Fleetsync2Decoder( AliasList aliasList )
	{
    	super( SampleType.FLOAT );
    	
        mDecimationFilter = new FloatHalfBandFilter( 
        		Filters.FIR_HALF_BAND_31T_ONE_EIGHTH_FCO, 1.0002 );
        addFloatListener( mDecimationFilter );

        mBandPassFilter = new FloatFIRFilter( 
        		Filters.FIRBP_1200FSK_24000FS.getCoefficients(), 1.02 );
        mDecimationFilter.setListener( mBandPassFilter );

        mFSKDecoder = new FSK2Decoder( sDECIMATED_SAMPLE_RATE, 
        					sSYMBOL_RATE, Output.INVERTED );
        mBandPassFilter.setListener( mFSKDecoder );

        mMessageFramer = new MessageFramer( 
        		SyncPattern.FLEETSYNC2.getPattern(), sMESSAGE_LENGTH );
        mFSKDecoder.setListener( mMessageFramer );
        
        mMessageProcessor = new Fleetsync2MessageProcessor( aliasList );
        mMessageFramer.addMessageListener( mMessageProcessor );
        mMessageProcessor.addMessageListener( this );
	}
    
    public void dispose()
    {
    	super.dispose();
    	
    	mDecimationFilter.dispose();
    	mBandPassFilter.dispose();
    	mFSKDecoder.dispose();
    	mMessageFramer.dispose();
    	mMessageProcessor.dispose();
    }

	@Override
    public DecoderType getType()
    {
	    return DecoderType.FLEETSYNC2;
    }

	/**
	 * Returns a float listener interface for connecting this decoder to a 
	 * float stream provider
	 */
	public Listener<Float> getFloatReceiver()
	{
		return (Listener<Float>)mDecimationFilter;
	}
	
	@Override
    public List<Tap> getTaps()
    {
		if( mAvailableTaps == null )
		{
			mAvailableTaps = new ArrayList<Tap>();
			
			mAvailableTaps.add( new FloatTap( INSTRUMENT_INPUT, 0, 1.0f ) );
			mAvailableTaps.add( new FloatTap( 
					INSTRUMENT_BANDPASS_FILTER_TO_FSK2_DEMOD, 0, 0.5f ) );
			mAvailableTaps.addAll( mFSKDecoder.getTaps() );
			mAvailableTaps.add( new BinaryTap( 
					INSTRUMENT_FSK2_DECODER_TO_MESSAGE_FRAMER, 0, 0.025f ) );
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
				FloatTap inputTap = (FloatTap)tap;
				addFloatListener( inputTap );
				break;
			case INSTRUMENT_BANDPASS_FILTER_TO_FSK2_DEMOD:
				FloatTap bpTap = (FloatTap)tap;
				mBandPassFilter.setListener( bpTap );
				bpTap.setListener( mFSKDecoder );
				break;
			case INSTRUMENT_FSK2_DECODER_TO_MESSAGE_FRAMER:
				BinaryTap decoderTap = (BinaryTap)tap;
				mFSKDecoder.setListener( decoderTap );
				decoderTap.setListener( mMessageFramer );
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
				removeFloatListener( inputTap );
				break;
			case INSTRUMENT_BANDPASS_FILTER_TO_FSK2_DEMOD:
				mBandPassFilter.setListener( mFSKDecoder );
				break;
			case INSTRUMENT_FSK2_DECODER_TO_MESSAGE_FRAMER:
				mFSKDecoder.setListener( mMessageFramer );
		        break;
		}
    }
}
