/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

package io.github.dsheirer.module.decode.p25.audio;


import io.github.dsheirer.audio.codec.mbe.MBECallSequence;
import io.github.dsheirer.audio.codec.mbe.MBECallSequenceRecorder;
import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.identifier.encryption.EncryptionKeyIdentifier;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.P25P1Message;
import io.github.dsheirer.module.decode.p25.phase1.message.hdu.HDUMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.hdu.HeaderData;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.LinkControlWord;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.motorola.LCMotorolaGroupRegroupVoiceChannelUpdate;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.motorola.LCMotorolaGroupRegroupVoiceChannelUser;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCGroupVoiceChannelUpdateExplicit;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCGroupVoiceChannelUser;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCTelephoneInterconnectVoiceChannelUser;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCUnitToUnitVoiceChannelUser;
import io.github.dsheirer.module.decode.p25.phase1.message.ldu.EncryptionSyncParameters;
import io.github.dsheirer.module.decode.p25.phase1.message.ldu.LDU1Message;
import io.github.dsheirer.module.decode.p25.phase1.message.ldu.LDU2Message;
import io.github.dsheirer.module.decode.p25.phase1.message.ldu.LDUMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.tdu.TDULCMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.tdu.TDUMessage;
import io.github.dsheirer.preference.UserPreferences;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * P25 Phase 1 IMBE Frame recorder generates P25 call sequence recordings containing JSON representations of audio
 * frames, optional encryption and call identifiers.
 */
public class P25P1CallSequenceRecorder extends MBECallSequenceRecorder
{
    private final static Logger mLog = LoggerFactory.getLogger(P25P1CallSequenceRecorder.class);

    public static final String PROTOCOL = "APCO25-PHASE1";

    private MBECallSequence mCallSequence;

    /**
     * Constructs a P25-Phase2 MBE call sequence recorder.
     *
     * @param userPreferences to obtain the recording directory
     * @param channelFrequency for the channel to record
     * @param system defined by the user
     * @param site defined by the user
     */
    public P25P1CallSequenceRecorder(UserPreferences userPreferences, long channelFrequency, String system, String site)
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
        if(message instanceof P25P1Message)
        {
            P25P1Message p25 = (P25P1Message)message;

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
    public void process(P25P1Message message)
    {
        if(message instanceof LDUMessage)
        {
            process((LDUMessage)message);
        }
        else if(message instanceof TDULCMessage)
        {
            process((TDULCMessage)message);
        }
        else if(message instanceof TDUMessage)
        {
            flush();
        }
        else if (message instanceof HDUMessage)
        {
            process((HDUMessage)message);
        }
    }

    private void process(TDULCMessage tdulc)
    {
        process(tdulc.getLinkControlWord());
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

    private void process(LinkControlWord lcw)
    {
        if(lcw.isValid() && mCallSequence != null)
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
                case MOTOROLA_GROUP_REGROUP_VOICE_CHANNEL_USER:
                    LCMotorolaGroupRegroupVoiceChannelUser mpgvcu = (LCMotorolaGroupRegroupVoiceChannelUser)lcw;
                    mCallSequence.setFromIdentifier(mpgvcu.getSourceAddress().toString());
                    mCallSequence.setToIdentifier(mpgvcu.getSupergroupAddress().toString());
                    mCallSequence.setCallType(CALL_TYPE_GROUP);
                    break;
                case MOTOROLA_GROUP_REGROUP_VOICE_CHANNEL_UPDATE:
                    LCMotorolaGroupRegroupVoiceChannelUpdate mpgvcup = (LCMotorolaGroupRegroupVoiceChannelUpdate)lcw;
                    mCallSequence.setToIdentifier(mpgvcup.getSupergroupAddress().toString());
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

    private void process(LDU1Message ldu1Message)
    {
        process(ldu1Message.getLinkControlWord());
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

    private void process(HDUMessage hduMessage)
    {
        if(mCallSequence == null)
        {
            mCallSequence = new MBECallSequence(PROTOCOL);
        }

        HeaderData hd = hduMessage.getHeaderData();

        if (hd.isEncryptedAudio())
        {
            mCallSequence.setEncrypted(true);
            Phase2EncryptionSyncParameters esp = new Phase2EncryptionSyncParameters((EncryptionKeyIdentifier)hd.getEncryptionKey(), hd.getMessageIndicator());
            mCallSequence.setEncryptionSyncParameters(esp);
        }
    }
}
