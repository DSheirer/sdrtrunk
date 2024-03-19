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
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.p25.phase2.message.EncryptionSynchronizationSequence;
import io.github.dsheirer.module.decode.p25.phase2.message.P25P2Message;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacMessage;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.EndPushToTalk;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.GroupVoiceChannelUserAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.GroupVoiceChannelUserExtended;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.MacStructure;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.PushToTalk;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.TelephoneInterconnectVoiceChannelUser;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.UnitToUnitVoiceChannelUserAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.UnitToUnitVoiceChannelUserExtended;
import io.github.dsheirer.module.decode.p25.phase2.timeslot.AbstractVoiceTimeslot;
import io.github.dsheirer.preference.UserPreferences;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * P25 Phase 2 AMBE Frame recorder generates P25 call sequence recordings containing JSON representations of audio
 * frames, optional encryption and call identifiers.
 */
public class P25P2CallSequenceRecorder extends MBECallSequenceRecorder
{
    private final static Logger mLog = LoggerFactory.getLogger(P25P2CallSequenceRecorder.class);

    private static final String PROTOCOL = "APCO25-PHASE2";

    private TimeslotCallSequenceProcessor mTimeslot0Processor = new TimeslotCallSequenceProcessor(0);
    private TimeslotCallSequenceProcessor mTimeslot1Processor = new TimeslotCallSequenceProcessor(1);

    /**
     * Constructs a P25-Phase2 MBE call sequence recorder.
     *
     * @param userPreferences to obtain the recording directory
     * @param channelFrequency for the channel to record
     */
    public P25P2CallSequenceRecorder(UserPreferences userPreferences, long channelFrequency, String system, String site)
    {
        super(userPreferences, channelFrequency, system, site);
    }

    /**
     * Stops and flushes any partial frame sequence from the processors
     */
    @Override
    public void stop()
    {
        mTimeslot0Processor.flush();
        mTimeslot1Processor.flush();
    }

    /**
     * Primary message interface for receiving frames and metadata messages to record
     */
    @Override
    public void receive(IMessage message)
    {
        if(message instanceof P25P2Message)
        {
            P25P2Message p25p2 = (P25P2Message)message;

            if(p25p2.isValid())
            {
                switch(p25p2.getTimeslot())
                {
                    case 0:
                        mTimeslot0Processor.process(p25p2);
                        break;
                    case 1:
                        mTimeslot1Processor.process(p25p2);
                        break;
                }
            }
        }
    }

    /**
     * Timeslot call sequence processor
     */
    class TimeslotCallSequenceProcessor
    {
        private int mTimeslot;
        private MBECallSequence mCallSequence;

        /**
         * Constructs a processor for the specified channel number / timeslot
         * @param timeslot
         */
        public TimeslotCallSequenceProcessor(int timeslot)
        {
            mTimeslot = timeslot;
        }

        /**
         * Timeslot for this processor
         */
        public int getTimeslot()
        {
            return mTimeslot;
        }

        /**
         * Flushes any partial call sequence
         */
        public void flush()
        {
            if(mCallSequence != null)
            {
                writeCallSequence(mCallSequence, "TS" + getTimeslot());
                mCallSequence = null;
            }
        }

        /**
         * Processes any P25 Phase 2 message
         */
        public void process(P25P2Message message)
        {
            if(message instanceof AbstractVoiceTimeslot)
            {
                process((AbstractVoiceTimeslot)message);
            }
            else if(message instanceof MacMessage)
            {
                process((MacMessage)message);
            }
            else if (message instanceof EncryptionSynchronizationSequence)
            {
                processEss((EncryptionSynchronizationSequence)message);
            }
        }

        /**
         * Processes Voice timeslots
         */
        private void process(AbstractVoiceTimeslot voiceTimeslot)
        {
            if(mCallSequence == null)
            {
                mCallSequence = new MBECallSequence(PROTOCOL);
            }

            List<BinaryMessage> voiceFrames = voiceTimeslot.getVoiceFrames();

            long baseTimestamp = voiceTimeslot.getTimestamp();

            for(BinaryMessage frame : voiceFrames)
            {
                mCallSequence.addVoiceFrame(baseTimestamp, frame.toHexString());

                //Voice frames are 20 milliseconds each, so we increment the timestamp by 20 for each one
                baseTimestamp += 20;
            }
        }

        /**
         * Processes a MAC message
         */
        private void process(MacMessage macMessage)
        {
            switch(macMessage.getMacPduType())
            {
                case MAC_1_PTT:
                case MAC_4_ACTIVE:
                    process(macMessage.getMacStructure(), true);
                    break;
                case MAC_2_END_PTT:
                case MAC_6_HANGTIME:
                    process(macMessage.getMacStructure(), false);
                    break;
                case MAC_3_IDLE:
                    flush();
                    break;
            }

        }

        /**
         * Processes a mac structure message to obtain from/to identifiers and to optionally close the call sequence
         * when the MAC pdu indicates that the call sequence is no longer active
         *
         * @param mac
         * @param isActive
         */
        private void process(MacStructure mac, boolean isActive)
        {
            if(mCallSequence == null && isActive)
            {
                mCallSequence = new MBECallSequence(PROTOCOL);
            }

            if(mCallSequence != null)
            {
                switch(mac.getOpcode())
                {
                    case PUSH_TO_TALK:
                        processPTT(mac);
                        break;
                    case END_PUSH_TO_TALK:
                        processEndPTT(mac);
                        break;
                    case TDMA_01_GROUP_VOICE_CHANNEL_USER_ABBREVIATED:
                        processGVCUA(mac);
                        break;
                    case TDMA_02_UNIT_TO_UNIT_VOICE_CHANNEL_USER_ABBREVIATED:
                        processUTUVCU(mac);
                        break;
                    case TDMA_03_TELEPHONE_INTERCONNECT_VOICE_CHANNEL_USER:
                        processTIVCU(mac);
                        break;
                    case TDMA_21_GROUP_VOICE_CHANNEL_USER_EXTENDED:
                        processGVCUE(mac);
                        break;
                    case TDMA_22_UNIT_TO_UNIT_VOICE_CHANNEL_USER_EXTENDED:
                        processUTUVCUE(mac);
                        break;
                }

                if(!isActive)
                {
                    writeCallSequence(mCallSequence, "TS" + getTimeslot());
                    mCallSequence = null;
                }
            }
        }

        private void processUTUVCUE(MacStructure mac) {
            if(mac instanceof UnitToUnitVoiceChannelUserExtended)
            {
                UnitToUnitVoiceChannelUserExtended uuvcue = (UnitToUnitVoiceChannelUserExtended)mac;
                mCallSequence.setFromIdentifier(uuvcue.getSource().toString());
                mCallSequence.setToIdentifier(uuvcue.getTargetAddress().toString());
                mCallSequence.setCallType(CALL_TYPE_INDIVIDUAL);
                if(uuvcue.getServiceOptions().isEncrypted())
                {
                    mCallSequence.setEncrypted(true);
                }
            }
            else
            {
                mLog.warn("Expected unit-2-unit voice channel user extended but found: " + mac.getClass());
            }
        }

        private void processGVCUE(MacStructure mac) {
            if(mac instanceof GroupVoiceChannelUserExtended)
            {
                GroupVoiceChannelUserExtended gvcue = (GroupVoiceChannelUserExtended)mac;
                mCallSequence.setFromIdentifier(gvcue.getSource().toString());
                mCallSequence.setToIdentifier(gvcue.getGroupAddress().toString());
                mCallSequence.setCallType(CALL_TYPE_GROUP);
                if(gvcue.getServiceOptions().isEncrypted())
                {
                    mCallSequence.setEncrypted(true);
                }
            }
            else
            {
                mLog.warn("Expected group voice channel user extended but found: " + mac.getClass());
            }
        }

        private void processTIVCU(MacStructure mac) {
            if(mac instanceof TelephoneInterconnectVoiceChannelUser)
            {
                TelephoneInterconnectVoiceChannelUser tivcu = (TelephoneInterconnectVoiceChannelUser)mac;
                mCallSequence.setToIdentifier(tivcu.getTargetAddress().toString());
                mCallSequence.setCallType(CALL_TYPE_TELEPHONE_INTERCONNECT);
                if(tivcu.getServiceOptions().isEncrypted())
                {
                    mCallSequence.setEncrypted(true);
                }
            }
            else
            {
                mLog.warn("Expected telephone interconnect voice channel user abbreviated but found: " + mac.getClass());
            }
        }

        private void processUTUVCU(MacStructure mac) {
            if(mac instanceof UnitToUnitVoiceChannelUserAbbreviated)
            {
                UnitToUnitVoiceChannelUserAbbreviated uuvcua = (UnitToUnitVoiceChannelUserAbbreviated)mac;
                mCallSequence.setFromIdentifier(uuvcua.getSourceAddress().toString());
                mCallSequence.setToIdentifier(uuvcua.getTargetAddress().toString());
                mCallSequence.setCallType(CALL_TYPE_INDIVIDUAL);
                if(uuvcua.getServiceOptions().isEncrypted())
                {
                    mCallSequence.setEncrypted(true);
                }
            }
            else
            {
                mLog.warn("Expected unit-2-unit voice channel user abbreviated but found: " + mac.getClass());
            }
        }

        private void processGVCUA(MacStructure mac) {
            if(mac instanceof GroupVoiceChannelUserAbbreviated)
            {
                GroupVoiceChannelUserAbbreviated gvcua = (GroupVoiceChannelUserAbbreviated)mac;
                mCallSequence.setFromIdentifier(gvcua.getSourceAddress().toString());
                mCallSequence.setToIdentifier(gvcua.getGroupAddress().toString());
                mCallSequence.setCallType(CALL_TYPE_GROUP);
                if(gvcua.getServiceOptions().isEncrypted())
                {
                    mCallSequence.setEncrypted(true);
                }
            }
            else
            {
                mLog.warn("Expected group voice channel user abbreviated but found: " + mac.getClass());
            }
        }

        private void processEndPTT(MacStructure mac) {
            if(mac instanceof EndPushToTalk)
            {
                EndPushToTalk eptt = (EndPushToTalk)mac;

                String source = eptt.getSourceAddress().toString();

                if(source != null && !source.contentEquals("16777215"))
                {
                    mCallSequence.setFromIdentifier(source);
                }
                mCallSequence.setToIdentifier(eptt.getGroupAddress().toString());
                writeCallSequence(mCallSequence, "TS" + getTimeslot());
                mCallSequence = null;
            }
            else
            {
                mLog.warn("Expected End push-to-talk structure but found: " + mac.getClass());
            }
        }

        private void processPTT(MacStructure mac) {
            if(mac instanceof PushToTalk)
            {
                PushToTalk ptt = (PushToTalk)mac;

                if(mCallSequence == null)
                {
                    mCallSequence = new MBECallSequence(PROTOCOL);
                }
                mCallSequence.setFromIdentifier(ptt.getSourceAddress().toString());
                mCallSequence.setToIdentifier(ptt.getGroupAddress().toString());

                if(ptt.isEncrypted())
                {
                    mCallSequence.setEncrypted(true);
                    mCallSequence.setEncryptionSyncParameters(ptt.getEncryptionSyncParameters());
                }
            }
            else
            {
                mLog.warn("Expected push-to-talk structure but found: " + mac.getClass());
            }
        }

        private void processEss(EncryptionSynchronizationSequence ess)
        {
            if(mCallSequence == null)
            {
                mCallSequence = new MBECallSequence(PROTOCOL);
            }
            if (ess.isEncrypted())
            {
                mCallSequence.setEncrypted(true);
                mCallSequence.setEncryptionSyncParameters(ess);
            }
        }
    }
}
