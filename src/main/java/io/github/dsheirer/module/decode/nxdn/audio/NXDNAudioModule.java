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
package io.github.dsheirer.module.decode.nxdn.audio;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.audio.codec.mbe.AmbeAudioModule;
import io.github.dsheirer.audio.squelch.SquelchState;
import io.github.dsheirer.audio.squelch.SquelchStateEvent;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.nxdn.layer3.call.Audio;
import io.github.dsheirer.module.decode.nxdn.layer3.call.Disconnect;
import io.github.dsheirer.module.decode.nxdn.layer3.call.TransmissionRelease;
import io.github.dsheirer.module.decode.nxdn.layer3.call.VoiceCall;
import io.github.dsheirer.module.decode.nxdn.layer3.scch.Information1;
import io.github.dsheirer.module.decode.nxdn.layer3.scch.InitializationVectorPart1;
import io.github.dsheirer.module.decode.nxdn.layer3.type.AudioCodec;
import io.github.dsheirer.module.decode.nxdn.layer3.type.CipherType;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.sample.Listener;
import java.util.ArrayList;
import java.util.List;
import jmbe.frame.CorrectedAMBEFrame;

/**
 * NXDN AMBE audio module
 */
public class NXDNAudioModule extends AmbeAudioModule
{
    private final SquelchStateListener mSquelchStateListener = new SquelchStateListener();
    private final List<Audio> mCachedAudioMessages = new ArrayList<>();
    private boolean mEncryptedCall = false;
    private boolean mEncryptedCallStateEstablished = false;
    private AudioCodec mAudioCodec;

    /**
     * Constructs an instance
     * @param userPreferences component
     * @param aliasList for the current channel
     */
    public NXDNAudioModule(UserPreferences userPreferences, AliasList aliasList)
    {
        super(userPreferences, aliasList, 0);
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

    /**
     * Processes audio and layer 3 messages to decode audio and to determine the encrypted status of a call event.
     */
    public void receive(IMessage message)
    {
        if(hasAudioCodec())
        {
            if(message instanceof Audio audio)
            {
                if(mEncryptedCallStateEstablished)
                {
                    processAudio(audio);
                }
                else
                {
                    //Cache audio until we can determine the encryption state
                    mCachedAudioMessages.add(audio);
                }
            }
            else if(message.isValid())
            {
                if(message instanceof VoiceCall voiceCall)
                {
                    mEncryptedCall = voiceCall.getEncryptionKeyIdentifier().isEncrypted();
                    mEncryptedCallStateEstablished = true;
                    mAudioCodec = voiceCall.getCallOption().getCodec();
                    processCachedAudio();
                }
                //Note: VoiceCallWithOptionalLocation subclasses (request, response, assignment, etc.) don't carry the
                //cipher type and key ID, so they can't be used to establish the encryption state.
                else if(!mEncryptedCallStateEstablished && message instanceof Information1 info1 &&
                        info1.getLICH().hasAudio())
                {
                    //Type-D only sends the VOICE CALL message at the start of the call.  Use the SCCH call info or
                    //initialization vector from a voice frame to establish the encryption state when we join a call
                    //late, or when the VOICE CALL message was missed.  Type-D only supports AMBE+ half-rate.
                    mEncryptedCall = info1 instanceof InitializationVectorPart1 ||
                            info1.getCipherType() != CipherType.UNENCRYPTED;
                    mEncryptedCallStateEstablished = true;
                    mAudioCodec = AudioCodec.HALF_RATE;
                    processCachedAudio();
                }
                else if(message instanceof Disconnect || message instanceof TransmissionRelease)
                {
                    closeAudioSegment();
                    mCachedAudioMessages.clear();
                    mEncryptedCall = false;
                    mEncryptedCallStateEstablished = false;
                }
            }
        }
    }

    /**
     * Processes any cached audio frames that were pending an encryption state determination.
     */
    private void processCachedAudio()
    {
        for(Audio audio : mCachedAudioMessages)
        {
            processAudio(audio);
        }

        mCachedAudioMessages.clear();
    }

    /**
     * Processes an audio packet by decoding the IMBE audio frames and rebroadcasting them as PCM audio packets.
     */
    private void processAudio(Audio audio)
    {
        if(!mEncryptedCall && mAudioCodec != null && mAudioCodec.equals(AudioCodec.HALF_RATE)) //Full rate not yet supported
        {
            List<byte[]> frames = audio.getAudioFrames();

            //Only error correct the frames in sdrtrunk when the JMBE library (version 3+) can synthesize from them
            List<CorrectedAMBEFrame> correctedFrames = isCorrectedFrameSupported() ? audio.getCorrectedAudioFrames() : null;

            for(int x = 0; x < frames.size(); x++)
            {
                addAudio(getAudio(frames.get(x), correctedFrames != null ? correctedFrames.get(x) : null));
            }
        }
        else
        {
            //Encrypted audio processing not supported
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
                closeAudioSegment();
                mEncryptedCallStateEstablished = false;
                mEncryptedCall = false;
                mCachedAudioMessages.clear();
            }
        }
    }
}
