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
package module.decode.passport;

import instrument.Instrumentable;
import instrument.tap.Tap;
import instrument.tap.TapGroup;

import java.util.ArrayList;
import java.util.List;

import module.decode.Decoder;
import module.decode.DecoderType;
import module.decode.config.DecodeConfiguration;
import sample.Listener;
import sample.real.IUnFilteredRealBufferListener;
import sample.real.RealBuffer;
import alias.AliasList;
import bits.MessageFramer;
import bits.SyncPattern;
import dsp.fsk.LTRFSKDecoder;

public class PassportDecoder extends Decoder 
	implements IUnFilteredRealBufferListener, Instrumentable
{
	public static final int PASSPORT_MESSAGE_LENGTH = 68;
	public static final int PASSPORT_SYNC_LENGTH = 9;

	private LTRFSKDecoder mPassportFSKDecoder;
	private MessageFramer mPassportMessageFramer;
	private PassportMessageProcessor mPassportMessageProcessor;

    private List<TapGroup> mAvailableTaps;

    /**
     * Passport Decoder.  Decodes unfiltered (e.g. demodulated but with no DC or
     * audio filtering) samples and produces Passport messages.
     */
	public PassportDecoder( DecodeConfiguration config, AliasList aliasList )
	{
		mPassportFSKDecoder = new LTRFSKDecoder();

		mPassportMessageFramer = 
				new MessageFramer( SyncPattern.PASSPORT.getPattern(),
						PASSPORT_MESSAGE_LENGTH );

		mPassportFSKDecoder.addListener( mPassportMessageFramer );

		mPassportMessageProcessor = new PassportMessageProcessor( aliasList );
		mPassportMessageFramer.addMessageListener( mPassportMessageProcessor );
		mPassportMessageProcessor.setMessageListener( mMessageBroadcaster );
	}

	@Override
	public Listener<RealBuffer> getUnFilteredRealBufferListener()
	{
		return mPassportFSKDecoder;
	}

	@Override
	public DecoderType getDecoderType()
	{
		return DecoderType.PASSPORT;
	}

	@Override
    public List<TapGroup> getTapGroups()
    {
		if( mAvailableTaps == null )
		{
			mAvailableTaps = new ArrayList<>();
			
			mAvailableTaps.addAll( mPassportFSKDecoder.getTapGroups() );
		}

		return mAvailableTaps;
    }

	@Override
    public void registerTap( Tap tap )
    {
		mPassportFSKDecoder.registerTap( tap );
    }

	@Override
    public void unregisterTap( Tap tap )
    {
		mPassportFSKDecoder.unregisterTap( tap );
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
