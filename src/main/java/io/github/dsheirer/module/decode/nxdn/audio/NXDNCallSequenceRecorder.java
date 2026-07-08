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


import io.github.dsheirer.audio.codec.mbe.MBECallSequence;
import io.github.dsheirer.audio.codec.mbe.MBECallSequenceRecorder;
import io.github.dsheirer.audio.codec.mbe.VoiceFrame;
import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.nxdn.NXDNMessage;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNLayer3Message;
import io.github.dsheirer.module.decode.nxdn.layer3.call.Audio;
import io.github.dsheirer.module.decode.nxdn.layer3.call.VoiceCall;
import io.github.dsheirer.module.decode.nxdn.layer3.call.VoiceCallInitializationVector;
import io.github.dsheirer.module.decode.nxdn.layer3.scch.CallInProgressDestinationInfo2;
import io.github.dsheirer.module.decode.nxdn.layer3.scch.CallInProgressDestinationInfo4;
import io.github.dsheirer.module.decode.nxdn.layer3.scch.CallInProgressSourceID;
import io.github.dsheirer.module.decode.nxdn.layer3.scch.CallInfo;
import io.github.dsheirer.preference.UserPreferences;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NXDN AMBE+ Frame recorder generates call sequence recordings containing JSON representations of audio
 * frames, optional encryption and call identifiers.
 */
public class NXDNCallSequenceRecorder extends MBECallSequenceRecorder
{
    private final static Logger mLog = LoggerFactory.getLogger(NXDNCallSequenceRecorder.class);
    public static final String PROTOCOL = "NXDN";
    private MBECallSequence mCallSequence;

    /**
     * Constructs an instance
     *
     * @param userPreferences to obtain the recording directory
     * @param channelFrequency for the channel to record
     * @param system defined by the user
     * @param site defined by the user
     */
    public NXDNCallSequenceRecorder(UserPreferences userPreferences, long channelFrequency, String system, String site)
    {
        super(userPreferences, channelFrequency, system, site);
    }

    /**
     * Stops and flushes any partial frame sequence from the processors
     */
    @Override
    public void stop()
    {
        flush();
    }

    /**
     * Primary message interface for receiving frames and metadata messages to record
     */
    @Override
    public void receive(IMessage message)
    {
        if(message instanceof NXDNMessage nxdn)
        {
            if(nxdn.isValid())
            {
                if(nxdn instanceof Audio audio)
                {
                    process(audio);
                }
                else if(nxdn instanceof NXDNLayer3Message layer3)
                {
                    process(layer3);
                }
            }
        }
    }

    /**
     * Gets and optionally creates a new call sequence.
     */
    private MBECallSequence getCallSequence()
    {
        if(mCallSequence == null)
        {
            mCallSequence = new MBECallSequence(PROTOCOL);
        }

        return mCallSequence;
    }


    /**
     * Flushes any partial call sequence
     */
    public void flush()
    {
        if(mCallSequence != null)
        {
            writeCallSequence(mCallSequence);
            mCallSequence = null;
        }
    }

    /**
     * Processes any NXDN layer 3 message
     */
    public void process(NXDNLayer3Message layer3)
    {
        switch(layer3.getMessageType())
        {
            case TRAFFIC_IN_01_CC_VOICE_CALL:
            case TRAFFIC_OUT_01_CC_VOICE_CALL:
            case TYPE_D_IN_01_CC_VOICE_CALL:
            case TYPE_D_OUT_01_CC_VOICE_CALL:
                if(layer3 instanceof VoiceCall vc)
                {
                    getCallSequence().setCallType(vc.getCallType().toString());
                    getCallSequence().setFromIdentifier(vc.getSource());
                    getCallSequence().setToIdentifier(vc.getDestination());

                    if(vc.getEncryptionKeyIdentifier().isEncrypted())
                    {
                        getCallSequence().setEncrypted(true);
                    }
                }
                break;
            case TRAFFIC_IN_03_CC_VOICE_CALL_INITIALIZATION_VECTOR:
            case TRAFFIC_OUT_03_CC_VOICE_CALL_INITIALIZATION_VECTOR:
            case TYPE_D_IN_03_CC_VOICE_CALL_INITIALIZATION_VECTOR:
            case TYPE_D_OUT_03_CC_VOICE_CALL_INITIALIZATION_VECTOR:
                if(layer3 instanceof VoiceCallInitializationVector iv)
                {
                    //TODO: implement superframes with IV injected into each
                }
                break;
            case TRAFFIC_OUT_07_CC_TRANSMISSION_RELEASE_EXTENSION:
            case TRAFFIC_OUT_08_CC_TRANSMISSION_RELEASE:
            case TYPE_D_OUT_07_CC_TRANSMISSION_RELEASE_EXTENSION:
            case TYPE_D_OUT_08_CC_TRANSMISSION_RELEASE:
                writeCallSequence(mCallSequence);
                mCallSequence = null;
                break;
            case TYPE_D_SCCH_IN_INFO_4_CALL_IN_PROGRESS_DESTINATION:
            case TYPE_D_SCCH_OUT_INFO_4_CALL_IN_PROGRESS_DESTINATION:
                if(layer3 instanceof CallInProgressDestinationInfo4 info4)
                {
                    getCallSequence().setToIdentifier(info4.getDestination());
                }
                break;
            case TYPE_D_SCCH_IN_INFO_3_CALL_IN_PROGRESS_SOURCE:
            case TYPE_D_SCCH_OUT_INFO_3_CALL_IN_PROGRESS_SOURCE:
                if(layer3 instanceof CallInProgressSourceID source)
                {
                    getCallSequence().setFromIdentifier(source.getSource());
                }
                break;
            case TYPE_D_SCCH_IN_INFO_2_CALL_IN_PROGRESS_DESTINATION:
            case TYPE_D_SCCH_OUT_INFO_2_CALL_IN_PROGRESS_DESTINATION:
                if(layer3 instanceof CallInProgressDestinationInfo2 info2)
                {
                    getCallSequence().setToIdentifier(info2.getDestination());
                }
                break;
            case TYPE_D_SCCH_IN_INFO_1_CALL_INFO:
            case TYPE_D_SCCH_OUT_INFO_1_CALL_INFO:
                if(layer3 instanceof CallInfo ci && ci.getEncryptionKey().isEncrypted())
                {
                    getCallSequence().setEncrypted(true);
                }
            break;
        }
    }

    /**
     * Process audio messages
     */
    private void process(Audio audio)
    {
        List<byte[]> voiceFrames = audio.getAudioFrames();
        long timestamp = audio.getTimestamp();

        for(int frame = 0; frame < voiceFrames.size(); frame++)
        {
            if(frame == 0)
            {
                BinaryMessage frameBits = BinaryMessage.from(voiceFrames.get(frame));
                VoiceFrame voiceFrame = new VoiceFrame(timestamp, frameBits.toHexString());

                if(audio.hasSACCHFragment())
                {
                    voiceFrame.setTag(audio.getSACCHFragment().getStructure().toString());
                }

                getCallSequence().add(voiceFrame);
                //Voice frames are 20 milliseconds each, so we increment the timestamp by 20 for each one
                timestamp += 20;
            }
            else
            {
                BinaryMessage frameBits = BinaryMessage.from(voiceFrames.get(frame));
                getCallSequence().addVoiceFrame(timestamp, frameBits.toHexString());
                //Voice frames are 20 milliseconds each, so we increment the timestamp by 20 for each one
                timestamp += 20;
            }
        }
    }
}
