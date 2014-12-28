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
package decode.lj1200;

import instrument.Instrumentable;
import instrument.tap.Tap;
import instrument.tap.stream.BinaryTap;
import instrument.tap.stream.FloatTap;

import java.util.ArrayList;
import java.util.List;

import sample.Broadcaster;
import sample.real.RealSampleListener;
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
 * LJ1200 - 1200 baud 2FSK decoder
 */
public class LJ1200Decoder extends Decoder implements Instrumentable
{
	/* Decimated sample rate ( 48,000 / 2 = 24,000 ) feeding the decoder */
	private static final int DECIMATED_SAMPLE_RATE = 24000;
	
	/* Baud or Symbol Rate */
	private static final int SYMBOL_RATE = 1200;

	/* Message length - 16-bit sync plus 64 bit message */
    private static final int MESSAGE_LENGTH = 80;
    
    /* Instrumentation Taps */
    private ArrayList<Tap> mAvailableTaps;
	private static final String INSTRUMENT_INPUT = 
			"Tap Point: Float Input";
	private static final String INSTRUMENT_BANDPASS_FILTER_TO_FSK2_DEMOD = 
			"Tap Point: Bandpass Filter > < FSK2 Decoder";
	private static final String INSTRUMENT_FSK2_DECODER_TO_MESSAGE_FRAMER = 
			"Tap Point: FSK2 Decoder > < Message Framer";
	
    private FSK2Decoder mFSKDecoder;
    private Broadcaster<Boolean> mFSKDecoderBroadcaster = 
    					new Broadcaster<Boolean>();
    
    private FloatHalfBandFilter mDecimationFilter;
    private FloatFIRFilter mBandPassFilter;
    private MessageFramer mTowerMessageFramer;
    private MessageFramer mTransponderMessageFramer;
    private LJ1200MessageProcessor mMessageProcessor;
    
    public LJ1200Decoder( AliasList aliasList )
	{
    	super( SampleType.REAL );
    	
        mDecimationFilter = new FloatHalfBandFilter( 
        		Filters.FIR_HALF_BAND_31T_ONE_EIGHTH_FCO, 1.0002 );
        addRealSampleListener( mDecimationFilter );

        mBandPassFilter = new FloatFIRFilter( 
        		Filters.FIRBP_1200FSK_24000FS.getCoefficients(), 1.02 );
        mDecimationFilter.setListener( mBandPassFilter );

        mFSKDecoder = new FSK2Decoder( DECIMATED_SAMPLE_RATE, 
        					SYMBOL_RATE, Output.INVERTED );
        mBandPassFilter.setListener( mFSKDecoder );

        mFSKDecoder.setListener( mFSKDecoderBroadcaster );

        mTowerMessageFramer = new MessageFramer( 
        		SyncPattern.LJ1200.getPattern(), MESSAGE_LENGTH );
        mTransponderMessageFramer = new MessageFramer( 
        		SyncPattern.LJ1200_TRANSPONDER.getPattern(), MESSAGE_LENGTH );
        mFSKDecoderBroadcaster.addListener( mTowerMessageFramer );
        mFSKDecoderBroadcaster.addListener( mTransponderMessageFramer );
        
        mMessageProcessor = new LJ1200MessageProcessor( aliasList );
        mTowerMessageFramer.addMessageListener( mMessageProcessor );
        mTransponderMessageFramer.addMessageListener( mMessageProcessor );
        mMessageProcessor.addMessageListener( this );
	}
    
    public void dispose()
    {
    	super.dispose();
    	
    	mDecimationFilter.dispose();
    	mBandPassFilter.dispose();
    	mFSKDecoder.dispose();
    	mTowerMessageFramer.dispose();
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
	public RealSampleListener getRealReceiver()
	{
		return (RealSampleListener)mDecimationFilter;
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
				addRealSampleListener( inputTap );
				break;
			case INSTRUMENT_BANDPASS_FILTER_TO_FSK2_DEMOD:
				FloatTap bpTap = (FloatTap)tap;
				mBandPassFilter.setListener( bpTap );
				bpTap.setListener( mFSKDecoder );
				break;
			case INSTRUMENT_FSK2_DECODER_TO_MESSAGE_FRAMER:
				BinaryTap decoderTap = (BinaryTap)tap;
				mFSKDecoder.setListener( decoderTap );
				decoderTap.setListener( mTowerMessageFramer );
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
				removeRealListener( inputTap );
				break;
			case INSTRUMENT_BANDPASS_FILTER_TO_FSK2_DEMOD:
				mBandPassFilter.setListener( mFSKDecoder );
				break;
			case INSTRUMENT_FSK2_DECODER_TO_MESSAGE_FRAMER:
				mFSKDecoder.setListener( mTowerMessageFramer );
		        break;
		}
    }

	@Override
    public void addUnfilteredRealSampleListener( RealSampleListener listener )
    {
		//Not implemented
    }
}
