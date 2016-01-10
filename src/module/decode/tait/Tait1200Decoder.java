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
package module.decode.tait;

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
import alias.AliasList;
import bits.MessageFramer;
import bits.SyncPattern;
import dsp.filter.Filters;
import dsp.filter.fir.real.RealFIRFilter_RB_RB;
import dsp.filter.halfband.real.HalfBandFilter_RB_RB;
import dsp.fsk.FSK2Decoder;
import dsp.fsk.FSK2Decoder.Output;

/**
 * TAIT 1200 - 1200 baud 2FSK decoder
 */
public class Tait1200Decoder extends Decoder implements IFilteredRealBufferListener,
					Instrumentable
{
	/* Decimated sample rate ( 48,000 / 2 = 24,000 ) feeding the decoder */
	private static final int DECIMATED_SAMPLE_RATE = 24000;
	
	/* Baud or Symbol Rate */
	private static final int SYMBOL_RATE = 1200;

	/* Message length ... */
    private static final int MESSAGE_LENGTH = 440;
    
    /* Instrumentation Taps */
    private ArrayList<TapGroup> mAvailableTaps;
	private static final String INSTRUMENT_BANDPASS_FILTER_TO_FSK2_DEMOD = 
			"Tap Point: Bandpass Filter > < FSK2 Decoder";
	private static final String INSTRUMENT_FSK2_DECODER_TO_MESSAGE_FRAMER = 
			"Tap Point: FSK2 Decoder > < Message Framer";
	
    private FSK2Decoder mFSKDecoder;
    
    private HalfBandFilter_RB_RB mDecimationFilter;
    private RealFIRFilter_RB_RB mBandPassFilter;
    private MessageFramer mMessageFramerGPS;
    private MessageFramer mMessageFramerANI;
    private Broadcaster<Boolean> mFSKBroadcaster = new Broadcaster<Boolean>();
    private Tait1200GPSMessageProcessor mMessageAProcessor;
    private Tait1200ANIMessageProcessor mMessageBProcessor;
    
    public Tait1200Decoder( AliasList aliasList )
	{
        mDecimationFilter = new HalfBandFilter_RB_RB( 
    		Filters.FIR_HALF_BAND_31T_ONE_EIGHTH_FCO.getCoefficients(), 1.0002f, true );

        mBandPassFilter = new RealFIRFilter_RB_RB( 
        		Filters.FIRBP_1200FSK_24000FS.getCoefficients(), 1.02f );
        mDecimationFilter.setListener( mBandPassFilter );

        mFSKDecoder = new FSK2Decoder( DECIMATED_SAMPLE_RATE, 
        					SYMBOL_RATE, Output.INVERTED );
        mBandPassFilter.setListener( mFSKDecoder );


        mFSKDecoder.setListener( mFSKBroadcaster );

        mMessageFramerGPS = new MessageFramer( 
        		SyncPattern.TAIT_CCDI_GPS_MESSAGE.getPattern(), MESSAGE_LENGTH );
        mMessageFramerANI = new MessageFramer( 
        		SyncPattern.TAIT_SELCAL_MESSAGE.getPattern(), MESSAGE_LENGTH );
        
        mFSKBroadcaster.addListener( mMessageFramerGPS );
        mFSKBroadcaster.addListener( mMessageFramerANI );

        mMessageAProcessor = new Tait1200GPSMessageProcessor( aliasList );
        mMessageBProcessor = new Tait1200ANIMessageProcessor( aliasList );
        
        mMessageFramerGPS.addMessageListener( mMessageAProcessor );
        mMessageFramerANI.addMessageListener( mMessageBProcessor );
        
        mMessageAProcessor.setMessageListener( mMessageBroadcaster );
        mMessageBProcessor.setMessageListener( mMessageBroadcaster );
	}
    
    public void dispose()
    {
    	super.dispose();
    	
    	mDecimationFilter.dispose();
    	mBandPassFilter.dispose();
    	mFSKDecoder.dispose();
    	mMessageFramerGPS.dispose();
    	mMessageFramerANI.dispose();
    	mMessageAProcessor.dispose();
    	mMessageBProcessor.dispose();
    }

	@Override
    public List<TapGroup> getTapGroups()
    {
		if( mAvailableTaps == null )
		{
			mAvailableTaps = new ArrayList<>();

			TapGroup group = new TapGroup( "Tait 1200 Decoder" );
			
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
				mFSKBroadcaster.addListener( decoderTap );
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
				mFSKBroadcaster.removeListener( (BinaryTap)tap );
		        break;
		}
    }

	@Override
	public Listener<RealBuffer> getFilteredRealBufferListener()
	{
		return mDecimationFilter;
	}

	@Override
	public DecoderType getDecoderType()
	{
		return DecoderType.TAIT_1200;
	}

	@Override
	public void reset()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void start()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void stop()
	{
		// TODO Auto-generated method stub
		
	}
}
