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
package module.decode.ltrnet;

import instrument.Instrumentable;
import instrument.tap.Tap;
import instrument.tap.TapGroup;

import java.util.ArrayList;
import java.util.List;

import message.MessageDirection;
import module.decode.Decoder;
import module.decode.DecoderType;
import sample.Listener;
import sample.real.IUnFilteredRealBufferListener;
import sample.real.RealBuffer;
import alias.AliasList;
import bits.MessageFramer;
import bits.SyncPattern;
import dsp.fsk.LTRFSKDecoder;

public class LTRNetDecoder extends Decoder 
			implements IUnFilteredRealBufferListener, Instrumentable
{
	public static final int LTR_STANDARD_MESSAGE_LENGTH = 40;
	private LTRFSKDecoder mLTRFSKDecoder;
	private MessageFramer mLTRMessageFramer;
	private LTRNetMessageProcessor mLTRMessageProcessor;

    private List<TapGroup> mAvailableTaps;
    
    /**
     * LTR-Net Decoder.  Decodes unfiltered (e.g. demodulated but with no DC or
     * audio filtering) samples and produces LTR-Net messages.
     */
	public LTRNetDecoder( DecodeConfigLTRNet config, AliasList aliasList )
	{
		mLTRFSKDecoder = new LTRFSKDecoder();

		
		if( config.getMessageDirection() == MessageDirection.OSW )
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

		mLTRMessageProcessor = new LTRNetMessageProcessor( 
				config.getMessageDirection(), aliasList );
		
		mLTRMessageFramer.addMessageListener( mLTRMessageProcessor );
		
		mLTRMessageProcessor.setMessageListener( mMessageBroadcaster );
	}

	@Override
	public DecoderType getDecoderType()
	{
		return DecoderType.LTR_NET;
	}

	@Override
	public Listener<RealBuffer> getUnFilteredRealBufferListener()
	{
		return mLTRFSKDecoder;
	}

    @Override
    public List<TapGroup> getTapGroups()
    {
        if( mAvailableTaps == null )
        {
            mAvailableTaps = new ArrayList<TapGroup>();
            
            mAvailableTaps.addAll( mLTRFSKDecoder.getTapGroups() );
        }

        return mAvailableTaps;
    }

    @Override
    public void registerTap( Tap tap )
    {
        mLTRFSKDecoder.registerTap( tap );
    }

    @Override
    public void unregisterTap( Tap tap )
    {
        mLTRFSKDecoder.unregisterTap( tap );
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
