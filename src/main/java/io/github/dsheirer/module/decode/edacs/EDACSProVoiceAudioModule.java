/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */

// Reference: DSD-FME provoice.c, sdrtrunk P25P1AudioModule
package io.github.dsheirer.module.decode.edacs;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.audio.codec.mbe.JmbeAudioModule;
import io.github.dsheirer.audio.squelch.SquelchState;
import io.github.dsheirer.audio.squelch.SquelchStateEvent;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.edacs.message.EDACSProVoiceMessage;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Audio module for EDACS ProVoice traffic channels. Receives
 * {@link EDACSProVoiceMessage} instances from the
 * {@link EDACSProVoiceDecoder}, decodes each of the 4 IMBE 7100
 * voice frames via the JMBE {@code PROVOICE} codec, and emits 80 ms
 * of 8 kHz PCM audio per ProVoice frame.
 */
public class EDACSProVoiceAudioModule extends JmbeAudioModule
{
    private final static Logger mLog = LoggerFactory.getLogger(EDACSProVoiceAudioModule.class);
    private static final String PROVOICE_CODEC = "PROVOICE";

    private final SquelchStateListener mSquelchStateListener = new SquelchStateListener();
    private final EDACSProVoiceEncryptionTracker mEncryptionTracker = new EDACSProVoiceEncryptionTracker();
    private int mDecodeErrors = 0;
    private long mLastDecodeErrorLogTimestamp = 0;

    public EDACSProVoiceAudioModule(UserPreferences userPreferences, AliasList aliasList)
    {
        super(userPreferences, aliasList, DEFAULT_TIMESLOT);
    }

    @Override
    protected String getCodecName()
    {
        return PROVOICE_CODEC;
    }

    @Override
    public Listener<SquelchStateEvent> getSquelchStateListener()
    {
        return mSquelchStateListener;
    }

    @Override
    public void reset()
    {
        getIdentifierCollection().clear();
        mEncryptionTracker.reset();
    }

    @Override
    public void start()
    {
    }

    @Override
    public void receive(IMessage message)
    {
        if(hasAudioCodec() && message instanceof EDACSProVoiceMessage pv && pv.isValid())
        {
            //Encrypted voice is never played: neither a frame whose IV field is encrypted (before the call has enough
            //consecutive encrypted frames to be called encrypted) nor any frame once the call is found encrypted.
            if(mEncryptionTracker.process(pv) || pv.isEncrypted())
            {
                return;
            }

            for(byte[] frame : pv.getPackedImbe7100Frames())
            {
                try
                {
                    addAudio(getAudioCodec().getAudio(frame));
                }
                catch(Exception e)
                {
                    mDecodeErrors++;
                    logDecodeError(e);
                }
            }
        }
    }

    /**
     * Logs decode errors at most once every 10 seconds.
     */
    private void logDecodeError(Exception exception)
    {
        long now = System.currentTimeMillis();

        if(now - mLastDecodeErrorLogTimestamp > 10000)
        {
            mLastDecodeErrorLogTimestamp = now;
            mLog.warn("ProVoice IMBE decode errors: {} latest: {}", mDecodeErrors, exception.getMessage());
        }
    }

    public class SquelchStateListener implements Listener<SquelchStateEvent>
    {
        @Override
        public void receive(SquelchStateEvent event)
        {
            if(event.getSquelchState() == SquelchState.SQUELCH)
            {
                closeAudioSegment();
                mEncryptionTracker.reset();
            }
        }
    }
}
