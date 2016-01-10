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
package module.decode.lj1200;

import instrument.Instrumentable;
import instrument.tap.Tap;
import instrument.tap.TapGroup;
import instrument.tap.stream.BinaryTap;
import instrument.tap.stream.FloatBufferTap;
import instrument.tap.stream.FloatTap;

import java.util.ArrayList;
import java.util.List;

import module.decode.Decoder;
import module.decode.DecoderType;
import sample.Broadcaster;
import sample.Listener;
import sample.real.IFilteredRealBufferListener;
import sample.real.RealBuffer;
import sample.real.RealSampleListener;
import alias.AliasList;
import bits.MessageFramer;
import bits.SyncPattern;
import dsp.filter.Filters;
import dsp.filter.fir.real.RealFIRFilter_RB_RB;
import dsp.filter.halfband.real.HalfBandFilter_RB_RB;
import dsp.fsk.FSK2Decoder;
import dsp.fsk.FSK2Decoder.Output;

/**
 * LJ1200 - 1200 baud 2FSK decoder
 */
public class LJ1200Decoder extends Decoder implements IFilteredRealBufferListener, 
			Instrumentable
{
	/* Decimated sample rate ( 48,000 / 2 = 24,000 ) feeding the decoder */
	private static final int DECIMATED_SAMPLE_RATE = 24000;
	
	/* Baud or Symbol Rate */
	private static final int SYMBOL_RATE = 1200;

	/* Message length - 16-bit sync plus 64 bit message */
    private static final int MESSAGE_LENGTH = 80;
    
    /* Instrumentation Taps */
    private ArrayList<TapGroup> mAvailableTaps;
	private static final String INSTRUMENT_BANDPASS_FILTER_TO_FSK2_DEMOD = 
			"Tap Point: Bandpass Filter > < FSK2 Decoder";
	private static final String INSTRUMENT_FSK2_DECODER_TO_MESSAGE_FRAMER = 
			"Tap Point: FSK2 Decoder > < Message Framer";
	
    private FSK2Decoder mFSKDecoder;
    private Broadcaster<Boolean> mFSKDecoderBroadcaster = 
    					new Broadcaster<Boolean>();
    
    private HalfBandFilter_RB_RB mDecimationFilter;
    private RealFIRFilter_RB_RB mBandPassFilter;
    private MessageFramer mTowerMessageFramer;
    private MessageFramer mTransponderMessageFramer;
    private LJ1200MessageProcessor mMessageProcessor;
    
    public LJ1200Decoder( AliasList aliasList )
	{
        mDecimationFilter = new HalfBandFilter_RB_RB( 
    		Filters.FIR_HALF_BAND_31T_ONE_EIGHTH_FCO.getCoefficients(), 1.0f, true );

        mBandPassFilter = new RealFIRFilter_RB_RB( 
        		Filters.FIRBP_1200FSK_24000FS.getCoefficients(), 1.02f );
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
        mMessageProcessor.setMessageListener( mMessageBroadcaster );
	}
    
	@Override
	public DecoderType getDecoderType()
	{
		return DecoderType.LJ_1200;
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

	/**
	 * Returns a float listener interface for connecting this decoder to a 
	 * float stream provider
	 */
	public RealSampleListener getRealReceiver()
	{
		return (RealSampleListener)mDecimationFilter;
	}
	
	@Override
    public List<TapGroup> getTapGroups()
    {
		if( mAvailableTaps == null )
		{
			mAvailableTaps = new ArrayList<TapGroup>();

			TapGroup group = new TapGroup( "LJ-1200 Decoder" );
			
			group.add( new FloatTap( 
					INSTRUMENT_BANDPASS_FILTER_TO_FSK2_DEMOD, 0, 0.5f ) );
			group.add( new BinaryTap( 
					INSTRUMENT_FSK2_DECODER_TO_MESSAGE_FRAMER, 0, 0.025f ) );

			mAvailableTaps.add( group );
			mAvailableTaps.addAll( mFSKDecoder.getTapGroups() );
		}
		
	    return mAvailableTaps;
    }

	@Override
    public void registerTap( Tap tap )
    {
		mFSKDecoder.registerTap( tap );

		switch( tap.getName() )
		{
			case INSTRUMENT_BANDPASS_FILTER_TO_FSK2_DEMOD:
				FloatBufferTap bpTap = (FloatBufferTap)tap;
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
    public void unregisterTap( Tap tap )
    {
		mFSKDecoder.unregisterTap( tap );

		switch( tap.getName() )
		{
			case INSTRUMENT_BANDPASS_FILTER_TO_FSK2_DEMOD:
				mBandPassFilter.setListener( mFSKDecoder );
				break;
			case INSTRUMENT_FSK2_DECODER_TO_MESSAGE_FRAMER:
				mFSKDecoder.setListener( mTowerMessageFramer );
		        break;
		}
    }

	@Override
	public Listener<RealBuffer> getFilteredRealBufferListener()
	{
		return mDecimationFilter;
	}

	@Override
	public void reset()
	{
	}

	@Override
	public void start()
	{
	}

	@Override
	public void stop()
	{
	}
}
