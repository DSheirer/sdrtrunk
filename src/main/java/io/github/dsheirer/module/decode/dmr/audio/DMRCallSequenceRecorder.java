/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.module.decode.dmr.audio;


import io.github.dsheirer.audio.codec.mbe.MBECallSequence;
import io.github.dsheirer.audio.codec.mbe.MBECallSequenceRecorder;
import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.dmr.message.DMRMessage;
import io.github.dsheirer.module.decode.dmr.message.data.header.VoiceHeader;
import io.github.dsheirer.module.decode.dmr.message.data.lc.full.FullLCMessage;
import io.github.dsheirer.module.decode.dmr.message.data.lc.full.GroupVoiceChannelUser;
import io.github.dsheirer.module.decode.dmr.message.data.lc.full.UnitToUnitVoiceChannelUser;
import io.github.dsheirer.module.decode.dmr.message.data.lc.full.hytera.HyteraGroupVoiceChannelUser;
import io.github.dsheirer.module.decode.dmr.message.data.lc.full.hytera.HyteraUnitToUnitVoiceChannelUser;
import io.github.dsheirer.module.decode.dmr.message.data.lc.full.motorola.CapacityPlusWideAreaVoiceChannelUser;
import io.github.dsheirer.module.decode.dmr.message.data.lc.full.motorola.MotorolaGroupVoiceChannelUser;
import io.github.dsheirer.module.decode.dmr.message.data.terminator.Terminator;
import io.github.dsheirer.module.decode.dmr.message.voice.VoiceMessage;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.sample.Listener;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DMR AMBE Frame recorder generates call sequence recordings containing JSON representations of audio
 * frames, optional encryption and call identifiers.
 */
public class DMRCallSequenceRecorder extends MBECallSequenceRecorder
{
    private final static Logger mLog = LoggerFactory.getLogger(DMRCallSequenceRecorder.class);
    private static final String PROTOCOL = "DMR";
    private TimeslotProcessor mTimeslotProcessor1 = new TimeslotProcessor();
    private TimeslotProcessor mTimeslotProcessor2 = new TimeslotProcessor();

    /**
     * Constructs a DMR MBE call sequence recorder.
     *
     * @param userPreferences to obtain the recording directory
     * @param channelFrequency for the channel to record
     * @param system defined by the user
     * @param site defined by the user
     */
    public DMRCallSequenceRecorder(UserPreferences userPreferences, long channelFrequency, String system, String site)
    {
        super(userPreferences, channelFrequency, system, site);
    }

    /**
     * Stops and flushes any partial frame sequence from the processors
     */
    @Override
    public void stop()
    {
        mTimeslotProcessor1.flush();
        mTimeslotProcessor2.flush();
    }

    /**
     * Primary message interface for receiving frames and metadata messages to record
     */
    @Override
    public void receive(IMessage message)
    {
        if(message instanceof DMRMessage dmr)
        {
            if(dmr.isValid())
            {
                switch(dmr.getTimeslot())
                {
                    case 1:
                        mTimeslotProcessor1.receive(dmr);
                        break;
                    case 2:
                        mTimeslotProcessor2.receive(dmr);
                        break;
                }
            }
        }
    }

    /**
     * Processes DMR messages for a specific timeslot
     */
    private class TimeslotProcessor implements Listener<DMRMessage>
    {
        private MBECallSequence mCallSequence;
        private Identifier mFromIdentifier;
        private Identifier mToIdentifier;
        private String mCallType;
        private boolean mEncrypted = false;

        /**
         * Flushes any partial call sequence
         */
        public void flush()
        {
            if(mCallSequence != null)
            {
                writeCallSequence(mCallSequence);
            }

            mCallSequence = null;
            mToIdentifier = null;
            mFromIdentifier = null;
            mCallType = null;
            mEncrypted = false;
        }

        /**
         * Processes any DMR audio and terminator messages
         */
        @Override
        public void receive(DMRMessage message)
        {
            if(message instanceof VoiceMessage voiceMessage)
            {
                process(voiceMessage);
            }
            else if(message instanceof VoiceHeader voiceHeader)
            {
                process(voiceHeader);
            }
            else if(message instanceof FullLCMessage fullLCMessage)
            {
                process(fullLCMessage);
            }
            else if(message instanceof Terminator terminator)
            {
                //One last attempt to get the to/from identifiers from the terminator's link control message
                if(mCallSequence != null && terminator.getLCMessage() instanceof FullLCMessage flc)
                {
                    process(flc);
                }

                flush();
            }
        }

        /**
         * Process voice header message
         */
        private void process(VoiceHeader voiceHeader)
        {
            if(voiceHeader.getLCMessage() instanceof FullLCMessage flc)
            {
                process(flc);
            }
        }

        /**
         * Processes Voice messages
         */
        private void process(VoiceMessage voiceMessage)
        {
            if(mCallSequence == null)
            {
                mCallSequence = new MBECallSequence(PROTOCOL);
                mCallSequence.setFromIdentifier(mFromIdentifier);
                mCallSequence.setToIdentifier(mToIdentifier);
                mCallSequence.setCallType(mCallType);
                mCallSequence.setEncrypted(mEncrypted);
            }

            List<byte[]> voiceFrames = voiceMessage.getAMBEFrames();

            long baseTimestamp = voiceMessage.getTimestamp();

            for(byte[] frame : voiceFrames)
            {
                BinaryMessage frameBits = BinaryMessage.from(frame);
                mCallSequence.addVoiceFrame(baseTimestamp, frameBits.toHexString());

                //Voice frames are 20 milliseconds each, so we increment the timestamp by 20 for each one
                baseTimestamp += 20;
            }
        }

        /**
         * Process full link control messages to extract call details
         */
        private void process(FullLCMessage message)
        {
            if(message.isValid())
            {
                switch(message.getOpcode())
                {
                    case FULL_MOTOROLA_GROUP_VOICE_CHANNEL_USER:
                        if(message instanceof MotorolaGroupVoiceChannelUser cpvcu)
                        {
                            if(mCallSequence == null)
                            {
                                mFromIdentifier = cpvcu.getRadio();
                                mToIdentifier = cpvcu.getTalkgroup();
                                mEncrypted = cpvcu.getServiceOptions().isEncrypted();
                                mCallType = CALL_TYPE_GROUP;
                            }
                            else
                            {
                                mCallSequence.setFromIdentifier(cpvcu.getRadio());
                                mCallSequence.setToIdentifier(cpvcu.getTalkgroup());
                                mCallSequence.setEncrypted(cpvcu.getServiceOptions().isEncrypted());
                                mCallSequence.setCallType(CALL_TYPE_GROUP);
                            }
                        }
                        break;
                    case FULL_CAPACITY_PLUS_WIDE_AREA_VOICE_CHANNEL_USER:
                        if(message instanceof CapacityPlusWideAreaVoiceChannelUser cpwavcu)
                        {
                            if(mCallSequence == null)
                            {
                                mToIdentifier = cpwavcu.getTalkgroup();
                                mEncrypted = cpwavcu.getServiceOptions().isEncrypted();
                                mCallType = CALL_TYPE_GROUP;
                            }
                            else
                            {
                                mCallSequence.setToIdentifier(cpwavcu.getTalkgroup());
                                mCallSequence.setEncrypted(cpwavcu.getServiceOptions().isEncrypted());
                                mCallSequence.setCallType(CALL_TYPE_GROUP);
                            }
                        }
                        break;
                    case FULL_HYTERA_GROUP_VOICE_CHANNEL_USER:
                        if(message instanceof HyteraGroupVoiceChannelUser hgvcu)
                        {
                            if(mCallSequence == null)
                            {
                                mToIdentifier = hgvcu.getTalkgroup();
                                mFromIdentifier = hgvcu.getSourceRadio();
                                mCallType = CALL_TYPE_GROUP;
                                mEncrypted = hgvcu.isEncrypted();
                            }
                            else
                            {
                                mCallSequence.setToIdentifier(hgvcu.getTalkgroup());
                                mCallSequence.setFromIdentifier(hgvcu.getSourceRadio());
                                mCallSequence.setCallType(CALL_TYPE_GROUP);
                                mCallSequence.setEncrypted(hgvcu.isEncrypted());
                            }
                        }
                        break;
                    case FULL_HYTERA_UNIT_TO_UNIT_VOICE_CHANNEL_USER:
                        if(message instanceof HyteraUnitToUnitVoiceChannelUser huuvcu)
                        {
                            if(mCallSequence == null)
                            {
                                mToIdentifier = huuvcu.getTargetRadio();
                                mFromIdentifier = huuvcu.getSourceRadio();
                                mCallType = CALL_TYPE_INDIVIDUAL;
                                mEncrypted = huuvcu.isEncrypted();
                            }
                            else
                            {
                                mCallSequence.setToIdentifier(huuvcu.getTargetRadio());
                                mCallSequence.setFromIdentifier(huuvcu.getSourceRadio());
                                mCallSequence.setCallType(CALL_TYPE_INDIVIDUAL);
                                mCallSequence.setEncrypted(huuvcu.isEncrypted());
                            }
                        }
                        break;
                    case FULL_STANDARD_GROUP_VOICE_CHANNEL_USER:
                        if(message instanceof GroupVoiceChannelUser gvcu)
                        {
                            if(mCallSequence == null)
                            {
                                mFromIdentifier = gvcu.getRadio();
                                mToIdentifier = gvcu.getTalkgroup();
                                mCallType = CALL_TYPE_GROUP;
                                mEncrypted = gvcu.getServiceOptions().isEncrypted();
                            }
                            else
                            {
                                mCallSequence.setFromIdentifier(gvcu.getRadio());
                                mCallSequence.setToIdentifier(gvcu.getTalkgroup());
                                mCallSequence.setCallType(CALL_TYPE_GROUP);
                                mCallSequence.setEncrypted(gvcu.getServiceOptions().isEncrypted());
                            }
                        }
                        break;
                    case FULL_STANDARD_UNIT_TO_UNIT_VOICE_CHANNEL_USER:
                        if(message instanceof UnitToUnitVoiceChannelUser uuvcu)
                        {
                            if(mCallSequence == null)
                            {
                                mFromIdentifier = uuvcu.getSourceRadio();
                                mToIdentifier = uuvcu.getTargetRadio();
                                mCallType = CALL_TYPE_INDIVIDUAL;
                                mEncrypted = uuvcu.getServiceOptions().isEncrypted();
                            }
                            else
                            {
                                mCallSequence.setFromIdentifier(uuvcu.getSourceRadio());
                                mCallSequence.setToIdentifier(uuvcu.getTargetRadio());
                                mCallSequence.setCallType(CALL_TYPE_INDIVIDUAL);
                                mCallSequence.setEncrypted(uuvcu.getServiceOptions().isEncrypted());
                            }
                        }
                        break;
                }
            }
        }
    }
}
