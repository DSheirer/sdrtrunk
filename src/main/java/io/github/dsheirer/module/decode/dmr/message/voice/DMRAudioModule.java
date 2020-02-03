/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */
package io.github.dsheirer.module.decode.dmr.message.voice;

import io.github.dsheirer.audio.codec.mbe.AmbeAudioModule;
import io.github.dsheirer.audio.codec.mbe.ImbeAudioModule;
import io.github.dsheirer.audio.squelch.SquelchState;
import io.github.dsheirer.audio.squelch.SquelchStateEvent;
import io.github.dsheirer.dsp.gain.NonClippingGain;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.hdu.HDUMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.ldu.LDU1Message;
import io.github.dsheirer.module.decode.p25.phase1.message.ldu.LDU2Message;
import io.github.dsheirer.module.decode.p25.phase1.message.ldu.LDUMessage;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.ReusableAudioPacket;

public class DMRAudioModule extends AmbeAudioModule
{
    private boolean mEncryptedCall = false;
    private boolean mEncryptedCallStateEstablished = false;

    private SquelchStateListener mSquelchStateListener = new SquelchStateListener();
    private NonClippingGain mGain = new NonClippingGain(5.0f, 0.95f);
    private LDU1Message mCachedLDU1Message = null;

    public DMRAudioModule(UserPreferences userPreferences)
    {
        super(userPreferences);
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
    }

    @Override
    public void start()
    {

    }

    @Override
    public void stop()
    {
        if(hasAudioPacketListener())
        {
            ReusableAudioPacket endAudioPacket = getAudioPacketQueue().getEndAudioBuffer();
            endAudioPacket.resetAttributes();
            endAudioPacket.setAudioChannelId(getAudioChannelId());
            endAudioPacket.setIdentifierCollection(getIdentifierCollection().copyOf());
            endAudioPacket.incrementUserCount();
            getAudioPacketListener().receive(endAudioPacket);
        }
    }

    /**
     * Processes call header (HDU) and voice frame (LDU1/LDU2) messages to decode audio and to determine the
     * encrypted audio status of a call event. Only the HDU and LDU2 messages convey encrypted call status. If an
     * LDU1 message is received without a preceding HDU message, then the LDU1 message is cached until the first
     * LDU2 message is received and the encryption state can be determined. Both the LDU1 and the LDU2 message are
     * then processed for audio if the call is unencrypted.
     */
    public void receive(IMessage message)
    {
        if(hasAudioCodec() && hasAudioPacketListener())
        {
            if(mEncryptedCallStateEstablished)
            {
                if(message instanceof LDUMessage)
                {
                    processAudio((LDUMessage)message);
                }
            }
            else
            {
                if(message instanceof HDUMessage)
                {
                    mEncryptedCallStateEstablished = true;
                    mEncryptedCall = ((HDUMessage)message).getHeaderData().isEncryptedAudio();
                }
                else if(message instanceof LDU1Message)
                {
                    //When we receive an LDU1 message without first receiving the HDU message, cache the LDU1 Message
                    //until we can determine the encrypted call state from the next LDU2 message
                    mCachedLDU1Message = (LDU1Message)message;
                }
                else if(message instanceof LDU2Message)
                {
                    mEncryptedCallStateEstablished = true;
                    LDU2Message ldu2 = (LDU2Message)message;
                    mEncryptedCall = ldu2.getEncryptionSyncParameters().isEncryptedAudio();

                    if(mCachedLDU1Message != null)
                    {
                        processAudio(mCachedLDU1Message);
                        mCachedLDU1Message = null;
                    }

                    processAudio(ldu2);
                }
            }
        }
    }

    /**
     * Processes an audio packet by decoding the AMBE audio frames and rebroadcasting them as PCM audio packets.
     */
    private void processAudio(LDUMessage ldu)
    {
        if(!mEncryptedCall)
        {
            for(byte[] frame : ldu.getIMBEFrames())
            {
                float[] audio = getAudioCodec().getAudio(frame);

                audio = mGain.apply(audio);

                ReusableAudioPacket audioPacket = getAudioPacketQueue().getBuffer(audio.length);
                audioPacket.resetAttributes();
                audioPacket.setAudioChannelId(getAudioChannelId());
                audioPacket.setIdentifierCollection(getIdentifierCollection().copyOf());
                audioPacket.loadAudioFrom(audio);

                getAudioPacketListener().receive(audioPacket);
            }
        }
        else
        {
            //Encrypted audio processing not implemented
        }
    }

    /**
     * Wrapper for squelch state to process end of call actions.  At call end the encrypted call state established
     * flag is reset so that the encrypted audio state for the next call can be properly detected and we send an
     * END audio packet so that downstream processors like the audio recorder can properly close out a call sequence.
     */
    public class SquelchStateListener implements Listener<SquelchStateEvent>
    {
        @Override
        public void receive(SquelchStateEvent event)
        {
            if(event.getSquelchState() == SquelchState.SQUELCH)
            {
                if(hasAudioPacketListener())
                {
                    ReusableAudioPacket endAudioPacket = getAudioPacketQueue().getEndAudioBuffer();
                    endAudioPacket.resetAttributes();
                    endAudioPacket.setAudioChannelId(getAudioChannelId());
                    endAudioPacket.setIdentifierCollection(getIdentifierCollection().copyOf());
                    endAudioPacket.incrementUserCount();
                    getAudioPacketListener().receive(endAudioPacket);
                }

                mEncryptedCallStateEstablished = false;
                mEncryptedCall = false;
                mCachedLDU1Message = null;
            }
        }
    }
}
