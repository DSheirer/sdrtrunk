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
package ua.in.smartjava.module.decode.fleetsync2;

import ua.in.smartjava.instrument.Instrumentable;
import ua.in.smartjava.instrument.tap.Tap;
import ua.in.smartjava.instrument.tap.TapGroup;
import ua.in.smartjava.instrument.tap.stream.BinaryTap;
import ua.in.smartjava.instrument.tap.stream.FloatBufferTap;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import ua.in.smartjava.module.decode.Decoder;
import ua.in.smartjava.module.decode.DecoderType;
import ua.in.smartjava.sample.Listener;
import ua.in.smartjava.sample.real.IFilteredRealBufferListener;
import ua.in.smartjava.sample.real.RealBuffer;
import ua.in.smartjava.alias.AliasList;
import ua.in.smartjava.bits.MessageFramer;
import ua.in.smartjava.bits.SyncPattern;
import ua.in.smartjava.dsp.filter.Filters;
import ua.in.smartjava.dsp.filter.fir.real.RealFIRFilter_RB_RB;
import ua.in.smartjava.dsp.filter.halfband.real.HalfBandFilter_RB_RB;
import ua.in.smartjava.dsp.fsk.FSK2Decoder;
import ua.in.smartjava.dsp.fsk.FSK2Decoder.Output;

/**
 * Fleetsync II Decoder - 1200 baud 2FSK decoder that can process 48k ua.in.smartjava.sample rate
 * floating point samples and output fully framed Fleetsync II messages
 */
public class Fleetsync2Decoder extends Decoder implements IFilteredRealBufferListener,
			Instrumentable
{
	/* Decimated ua.in.smartjava.sample rate ( 48,000 / 2 = 24,000 ) feeding the decoder */
	private static final int sDECIMATED_SAMPLE_RATE = 24000;
	
	/* Baud or Symbol Rate */
	private static final int sSYMBOL_RATE = 1200;

	/* Message length - 5 x REVS + 16 x SYNC + 8 x 64Bit Blocks */
    private static final int sMESSAGE_LENGTH = 537;
    
    /* Instrumentation Taps */
    private ArrayList<TapGroup> mAvailableTaps;
	private static final String INSTRUMENT_BANDPASS_FILTER_TO_FSK2_DEMOD = 
			"Tap Point: Bandpass Filter > < FSK2 Decoder";
	private static final String INSTRUMENT_FSK2_DECODER_TO_MESSAGE_FRAMER = 
			"Tap Point: FSK2 Decoder > < Message Framer";
	
    private FSK2Decoder mFSKDecoder;
    private HalfBandFilter_RB_RB mDecimationFilter;
    private RealFIRFilter_RB_RB mBandPassFilter;
    private MessageFramer mMessageFramer;
    private Fleetsync2MessageProcessor mMessageProcessor;
    
    public Fleetsync2Decoder( AliasList aliasList )
	{
        mDecimationFilter = new HalfBandFilter_RB_RB( 
    		Filters.FIR_HALF_BAND_31T_ONE_EIGHTH_FCO.getCoefficients(), 1.0f, true );
        
        mBandPassFilter = new RealFIRFilter_RB_RB( 
        		Filters.FIRBP_1200FSK_24000FS.getCoefficients(), 1.02f );

        mDecimationFilter.setListener( mBandPassFilter );

        mFSKDecoder = new FSK2Decoder( sDECIMATED_SAMPLE_RATE, 
        					sSYMBOL_RATE, Output.INVERTED );
        mBandPassFilter.setListener( mFSKDecoder );

        mMessageFramer = new MessageFramer( 
        		SyncPattern.FLEETSYNC2.getPattern(), sMESSAGE_LENGTH );
        mFSKDecoder.setListener( mMessageFramer );
        
        mMessageProcessor = new Fleetsync2MessageProcessor( aliasList );
        mMessageFramer.addMessageListener( mMessageProcessor );
        
        mMessageProcessor.setMessageListener( this );
	}
    
    public void dispose()
    {
    	super.dispose();
    	
    	mBandPassFilter.dispose();
    	mDecimationFilter.dispose();
    	mFSKDecoder.dispose();
    	mMessageFramer.dispose();
    	mMessageProcessor.dispose();
    }

	@Override
	public DecoderType getDecoderType()
	{
		return DecoderType.FLEETSYNC2;
	}

	@Override
    public List<TapGroup> getTapGroups()
    {
		if( mAvailableTaps == null )
		{
			mAvailableTaps = new ArrayList<TapGroup>();

			TapGroup group = new TapGroup( "Fleetsync 2 Decoder" );
			
			group.add( new FloatBufferTap( 
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
				decoderTap.setListener( mMessageFramer );
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
				mFSKDecoder.setListener( mMessageFramer );
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
	public void start( ScheduledExecutorService executor )
	{
	}

	@Override
	public void stop()
	{
	}
}