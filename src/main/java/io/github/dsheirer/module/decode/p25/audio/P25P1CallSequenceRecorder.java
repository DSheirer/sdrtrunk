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

package io.github.dsheirer.module.decode.p25.audio;


import io.github.dsheirer.audio.codec.mbe.MBECallSequence;
import io.github.dsheirer.audio.codec.mbe.MBECallSequenceRecorder;
import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.P25Message;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.LinkControlWord;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.motorola.LCMotorolaPatchGroupVoiceChannelUpdate;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.motorola.LCMotorolaPatchGroupVoiceChannelUser;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCGroupVoiceChannelUpdateExplicit;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCGroupVoiceChannelUser;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCTelephoneInterconnectVoiceChannelUser;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCUnitToUnitVoiceChannelUser;
import io.github.dsheirer.module.decode.p25.phase1.message.ldu.EncryptionSyncParameters;
import io.github.dsheirer.module.decode.p25.phase1.message.ldu.LDU1Message;
import io.github.dsheirer.module.decode.p25.phase1.message.ldu.LDU2Message;
import io.github.dsheirer.module.decode.p25.phase1.message.ldu.LDUMessage;
import io.github.dsheirer.preference.UserPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * P25 Phase 1 IMBE Frame recorder generates P25 call sequence recordings containing JSON representations of audio
 * frames, optional encryption and call identifiers.
 */
public class P25P1CallSequenceRecorder extends MBECallSequenceRecorder
{
    private final static Logger mLog = LoggerFactory.getLogger(P25P1CallSequenceRecorder.class);

    private static final String PROTOCOL = "APCO25-PHASE1";

    private MBECallSequence mCallSequence;

    /**
     * Constructs a P25-Phase2 MBE call sequence recorder.
     *
     * @param userPreferences to obtain the recording directory
     * @param channelFrequency for the channel to record
     */
    public P25P1CallSequenceRecorder(UserPreferences userPreferences, long channelFrequency)
    {
        super(userPreferences, channelFrequency);
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
        if(message instanceof P25Message)
        {
            P25Message p25 = (P25Message)message;

            if(p25.isValid())
            {
                process(p25);
            }
        }
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
     * Processes any P25 Phase 1 message
     */
    public void process(P25Message message)
    {
        if(message instanceof LDUMessage)
        {
            process((LDUMessage)message);
        }
    }

    /**
     * Processes Voice LDU messages
     */
    private void process(LDUMessage lduMessage)
    {
        if(mCallSequence == null)
        {
            mCallSequence = new MBECallSequence(PROTOCOL);
        }

        if(lduMessage instanceof LDU1Message)
        {
            process((LDU1Message)lduMessage);
        }
        else if(lduMessage instanceof LDU2Message)
        {
            process((LDU2Message)lduMessage);
        }

        List<byte[]> voiceFrames = lduMessage.getIMBEFrames();

        long baseTimestamp = lduMessage.getTimestamp();

        for(byte[] frame : voiceFrames)
        {
            BinaryMessage frameBits = BinaryMessage.from(frame);
            mCallSequence.addVoiceFrame(baseTimestamp, frameBits.toHexString());

            //Voice frames are 20 milliseconds each, so we increment the timestamp by 20 for each one
            baseTimestamp += 20;
        }

    }

    private void process(LDU1Message ldu1Message)
    {
        LinkControlWord lcw = ldu1Message.getLinkControlWord();

        if(lcw.isValid())
        {
            switch(lcw.getOpcode())
            {
                case GROUP_VOICE_CHANNEL_USER:
                    LCGroupVoiceChannelUser gvcu = (LCGroupVoiceChannelUser)lcw;
                    mCallSequence.setFromIdentifier(gvcu.getSourceAddress().toString());
                    mCallSequence.setToIdentifier(gvcu.getGroupAddress().toString());
                    mCallSequence.setCallType(CALL_TYPE_GROUP);
                    break;
                case UNIT_TO_UNIT_VOICE_CHANNEL_USER:
                    LCUnitToUnitVoiceChannelUser uuvcu = (LCUnitToUnitVoiceChannelUser)lcw;
                    mCallSequence.setFromIdentifier(uuvcu.getSourceAddress().toString());
                    mCallSequence.setToIdentifier(uuvcu.getTargetAddress().toString());
                    mCallSequence.setCallType(CALL_TYPE_INDIVIDUAL);
                    break;
                case GROUP_VOICE_CHANNEL_UPDATE_EXPLICIT:
                    LCGroupVoiceChannelUpdateExplicit gvcue = (LCGroupVoiceChannelUpdateExplicit)lcw;
                    mCallSequence.setToIdentifier(gvcue.getGroupAddress().toString());
                    mCallSequence.setCallType(CALL_TYPE_GROUP);
                    break;
                case TELEPHONE_INTERCONNECT_VOICE_CHANNEL_USER:
                    LCTelephoneInterconnectVoiceChannelUser tivcu = (LCTelephoneInterconnectVoiceChannelUser)lcw;
                    mCallSequence.setToIdentifier(tivcu.getAddress().toString());
                    mCallSequence.setCallType(CALL_TYPE_TELEPHONE_INTERCONNECT);
                    break;
                case MOTOROLA_PATCH_GROUP_VOICE_CHANNEL_USER:
                    LCMotorolaPatchGroupVoiceChannelUser mpgvcu = (LCMotorolaPatchGroupVoiceChannelUser)lcw;
                    mCallSequence.setFromIdentifier(mpgvcu.getSourceAddress().toString());
                    mCallSequence.setToIdentifier(mpgvcu.getGroupAddress().toString());
                    mCallSequence.setCallType(CALL_TYPE_GROUP);
                    break;
                case MOTOROLA_PATCH_GROUP_VOICE_CHANNEL_UPDATE:
                    LCMotorolaPatchGroupVoiceChannelUpdate mpgvcup = (LCMotorolaPatchGroupVoiceChannelUpdate)lcw;
                    mCallSequence.setToIdentifier(mpgvcup.getPatchGroup().toString());
                    mCallSequence.setCallType(CALL_TYPE_GROUP);
                    break;
                case CALL_TERMINATION_OR_CANCELLATION:
                case MOTOROLA_TALK_COMPLETE:
                    writeCallSequence(mCallSequence);
                    mCallSequence = null;
                    break;
            }
        }
    }

    private void process(LDU2Message ldu2Message)
    {
        EncryptionSyncParameters parameters = ldu2Message.getEncryptionSyncParameters();

        if(parameters.isValid() && parameters.isEncryptedAudio())
        {
            mCallSequence.setEncrypted(true);
            mCallSequence.setEncryptionSyncParameters(parameters);
        }
    }
}
