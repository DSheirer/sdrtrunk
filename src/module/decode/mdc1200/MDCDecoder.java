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
package module.decode.mdc1200;

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
import sample.Listener;
import sample.real.IFilteredRealBufferListener;
import sample.real.RealBuffer;
import alias.AliasList;
import bits.MessageFramer;
import bits.SyncPattern;
import dsp.NRZDecoder;
import dsp.filter.Filters;
import dsp.filter.fir.real.RealFIRFilter_RB_RB;
import dsp.filter.halfband.real.HalfBandFilter_RB_RB;
import dsp.fsk.FSK2Decoder;
import dsp.fsk.FSK2Decoder.Output;

/**
 * MDC1200 Decoder - 1200 baud 2FSK decoder that can process 48k sample rate
 * complex or floating point samples and output fully framed MDC1200 messages
 */
public class MDCDecoder extends Decoder implements IFilteredRealBufferListener, 
												   Instrumentable
{
	/* Decimated sample rate ( 48,000 / 2 = 24,000 ) feeding the decoder */
	private static final int sDECIMATED_SAMPLE_RATE = 24000;
	
	/* Baud or Symbol Rate */
	private static final int sSYMBOL_RATE = 1200;

	/* Message length */
    private static final int sMESSAGE_LENGTH = 304;
    
    /* Instrumentation Taps */
    private ArrayList<TapGroup> mAvailableTaps;
	private static final String INSTRUMENT_BANDPASS_FILTER_TO_FSK2_DEMOD = 
			"Tap Point: Bandpass Filter > < FSK2 Decoder";
	private static final String INSTRUMENT_FSK2_DECODER_TO_NRZ_DECODER = 
			"Tap Point: FSK2 Decoder > < NRZI Decoder";
	private static final String INSTRUMENT_NRZI_DECODER_TO_MESSAGE_FRAMER = 
			"Tap Point: NRZ Decoder > < Message Framer";
	
    private FSK2Decoder mFSKDecoder;
    private HalfBandFilter_RB_RB mDecimationFilter;
    private RealFIRFilter_RB_RB mBandPassFilter;
    private NRZDecoder mNRZDecoder;
    private MessageFramer mMessageFramer;
    private MDCMessageProcessor mMessageProcessor;
    
    public MDCDecoder( AliasList aliasList )
	{
    	/* Decimation filter: 48000 / 2 = 24000 */
    	mDecimationFilter = new HalfBandFilter_RB_RB( 
    		Filters.FIR_HALF_BAND_31T_ONE_EIGHTH_FCO.getCoefficients(), 1.0f, true );

        /* Bandpass filter - predefined */
        mBandPassFilter = new RealFIRFilter_RB_RB( 
        		Filters.FIRBP_1200FSK_24000FS.getCoefficients(), 1.02f );
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
        mMessageFramer.addMessageListener( mMessageProcessor );
        mMessageProcessor.addMessageListener( mMessageBroadcaster );
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

	/* Instrumentation */
	@Override
    public List<TapGroup> getTapGroups()
    {
		if( mAvailableTaps == null )
		{
			mAvailableTaps = new ArrayList<TapGroup>();

			TapGroup group = new TapGroup( "MDC-1200 Decoder" );
			
			group.add( 
				new FloatTap( INSTRUMENT_BANDPASS_FILTER_TO_FSK2_DEMOD, 0, 0.5f ) );
			group.add( 
				new BinaryTap( INSTRUMENT_FSK2_DECODER_TO_NRZ_DECODER, 0, 0.025f ) );
			group.add( 
				new BinaryTap( INSTRUMENT_NRZI_DECODER_TO_MESSAGE_FRAMER, 0, 0.025f ) );
			
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
				/* wire the tap between the bandpass filter and the demod */
				FloatBufferTap floatTap = (FloatBufferTap)tap; 
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
    public void unregisterTap( Tap tap )
    {
		mFSKDecoder.unregisterTap( tap );
		
		switch( tap.getName() )
		{
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

	@Override
	public DecoderType getDecoderType()
	{
		return DecoderType.MDC1200;
	}
	
	@Override
	public Listener<RealBuffer> getFilteredRealBufferListener()
	{
		return mDecimationFilter;
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
