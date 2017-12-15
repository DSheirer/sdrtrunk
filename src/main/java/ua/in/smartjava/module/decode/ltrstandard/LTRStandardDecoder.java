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
package ua.in.smartjava.module.decode.ltrstandard;

import java.util.concurrent.ScheduledExecutorService;

import ua.in.smartjava.message.MessageDirection;
import ua.in.smartjava.module.decode.Decoder;
import ua.in.smartjava.module.decode.DecoderType;
import ua.in.smartjava.sample.Listener;
import ua.in.smartjava.sample.real.IUnFilteredRealBufferListener;
import ua.in.smartjava.sample.real.RealBuffer;
import ua.in.smartjava.alias.AliasList;
import ua.in.smartjava.bits.MessageFramer;
import ua.in.smartjava.bits.SyncPattern;
import ua.in.smartjava.dsp.fsk.LTRFSKDecoder;

public class LTRStandardDecoder extends Decoder implements IUnFilteredRealBufferListener
{
	public static final int LTR_STANDARD_MESSAGE_LENGTH = 40;

	private LTRFSKDecoder mLTRFSKDecoder;
	private MessageFramer mLTRMessageFramer;
	private LTRStandardMessageProcessor mLTRMessageProcessor;
    
    /**
     * LTR Decoder.  Decodes unfiltered (e.g. demodulated but with no DC or
     * ua.in.smartjava.audio filtering) samples and produces LTR Standard messages.
     */
	public LTRStandardDecoder( AliasList aliasList,
							   MessageDirection direction )
	{
		mLTRFSKDecoder = new LTRFSKDecoder();

		if( direction == MessageDirection.OSW )
		{
			mLTRMessageFramer = 
					new MessageFramer( SyncPattern.LTR_STANDARD_OSW.getPattern(),
							LTR_STANDARD_MESSAGE_LENGTH );
		}
		else
		{
			mLTRMessageFramer = 
					new MessageFramer( SyncPattern.LTR_STANDARD_ISW.getPattern(),
							LTR_STANDARD_MESSAGE_LENGTH );
		}
		
		mLTRFSKDecoder.addListener( mLTRMessageFramer );

		mLTRMessageProcessor = new LTRStandardMessageProcessor( direction, aliasList );
		mLTRMessageFramer.addMessageListener( mLTRMessageProcessor );
		mLTRMessageProcessor.setMessageListener( this );
	}

	@Override
	public DecoderType getDecoderType()
	{
		return DecoderType.LTR_STANDARD;
	}

	@Override
	public Listener<RealBuffer> getUnFilteredRealBufferListener()
	{
		return mLTRFSKDecoder;
	}

	@Override
	public void reset()
	{
		mLTRMessageFramer.reset();
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
