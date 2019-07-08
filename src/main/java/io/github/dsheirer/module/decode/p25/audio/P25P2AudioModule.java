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

import io.github.dsheirer.audio.codec.mbe.AmbeAudioModule;
import io.github.dsheirer.audio.squelch.SquelchState;
import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierUpdateNotification;
import io.github.dsheirer.identifier.IdentifierUpdateProvider;
import io.github.dsheirer.identifier.tone.P25CallProgressIdentifier;
import io.github.dsheirer.identifier.tone.P25DtmfIdentifier;
import io.github.dsheirer.identifier.tone.P25KnoxIdentifier;
import io.github.dsheirer.identifier.tone.P25ToneIdentifier;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.p25.phase2.message.EncryptionSynchronizationSequence;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.PushToTalk;
import io.github.dsheirer.module.decode.p25.phase2.timeslot.AbstractVoiceTimeslot;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.ReusableAudioPacket;
import jmbe.iface.IAudioWithMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class P25P2AudioModule extends AmbeAudioModule implements IdentifierUpdateProvider
{
    private final static Logger mLog = LoggerFactory.getLogger(P25P2AudioModule.class);

    private static final String METADATA_TYPE_DTMF = "DTMF";
    private static final String METADATA_TYPE_KNOX = "KNOX";
    private static final String METADATA_TYPE_TONE = "TONE";
    private static final String METADATA_TYPE_CALL_PROGRESS = "CALL PROGRESS";

    private Listener<IdentifierUpdateNotification> mIdentifierUpdateNotificationListener;
    private MetadataProcessor mDtmfMetadataProcessor;
    private MetadataProcessor mKnoxMetadataProcessor;
    private MetadataProcessor mToneMetadataProcessor;
    private MetadataProcessor mCallProcessMetadataProcessor;
    private int mTimeslot;
    private Queue<AbstractVoiceTimeslot> mQueuedAudioTimeslots = new ArrayDeque<>();
    private boolean mEncryptedCallStateEstablished = false;
    private boolean mEncryptedCall = false;

    public P25P2AudioModule(UserPreferences userPreferences, int timeslot)
    {
        //TODO: how do we get the identifier updates attached to the correct timeslot??
        super(userPreferences);
        mTimeslot = timeslot;
    }

    private int getTimeslot()
    {
        return mTimeslot;
    }

    @Override
    public Listener<SquelchState> getSquelchStateListener()
    {
        return null;
    }

    @Override
    public void reset()
    {
        mCallProcessMetadataProcessor = null;
        mDtmfMetadataProcessor = null;
        mKnoxMetadataProcessor = null;
        mToneMetadataProcessor = null;
        mQueuedAudioTimeslots.clear();
    }

    @Override
    public void start()
    {
        reset();
    }

    @Override
    public void stop()
    {

    }

    /**
     * Primary message processing method for processing voice timeslots and Push-To-Talk MAC messages
     *
     * Audio timeslots are temporarily queued until a determination of the encrypted state of the call is determined
     * and then all queued audio is processed through to the end of the call.  Encryption state is determined either
     * by the PTT MAC message or by processing the ESS fragments from the Voice2 and Voice4 timeslots.
     *
     * @param message to process
     */
    @Override
    public void receive(IMessage message)
    {
        if(message.getTimeslot() == getTimeslot())
        {
            if(message instanceof AbstractVoiceTimeslot)
            {
                AbstractVoiceTimeslot abstractVoiceTimeslot = (AbstractVoiceTimeslot)message;

                if(mEncryptedCallStateEstablished)
                {
                    if(!mEncryptedCall)
                    {
                        processAudio(abstractVoiceTimeslot.getVoiceFrames());
                    }
                    else
                    {
                        mLog.debug("*****IGNORING ENCRYPTED AUDIO CALL *****");
                    }
                }
                else
                {
                    //Queue audio timeslots until we can determine if the audio is encrypted or not
                    mQueuedAudioTimeslots.offer(abstractVoiceTimeslot);
                }
            }
            else if(message instanceof PushToTalk && message.isValid())
            {
                mEncryptedCallStateEstablished = true;
                mEncryptedCall = ((PushToTalk)message).isEncrypted();

                //There should not be any pending voice timeslots to process since the PTT message is the first in
                //the audio call sequence
            }
            else if(message instanceof EncryptionSynchronizationSequence)
            {
                mEncryptedCallStateEstablished = true;
                mEncryptedCall = ((EncryptionSynchronizationSequence)message).isEncrypted();
                processPendingVoiceTimeslots();
            }
        }
    }

    /**
     * Drains and processes any audio timeslots that have been queued pending determination of encrypted call status
     */
    private void processPendingVoiceTimeslots()
    {
        AbstractVoiceTimeslot timeslot = mQueuedAudioTimeslots.poll();

        while(timeslot != null)
        {
            receive(timeslot);
            timeslot = mQueuedAudioTimeslots.poll();
        }
    }

    private void processAudio(List<BinaryMessage> voiceFrames)
    {
        if(hasAudioCodec() && hasAudioPacketListener())
        {
            for(BinaryMessage voiceFrame: voiceFrames)
            {
                byte[] voiceFrameBytes = voiceFrame.getBytes();

                IAudioWithMetadata audioWithMetadata = getAudioCodec().getAudioWithMetadata(voiceFrameBytes);

                ReusableAudioPacket audioPacket = getAudioPacketQueue().getBuffer(audioWithMetadata.getAudio().length);
                audioPacket.resetAttributes();
                audioPacket.setAudioChannelId(getAudioChannelId());
                audioPacket.setIdentifierCollection(getIdentifierCollection().copyOf());
                audioPacket.loadAudioFrom(audioWithMetadata.getAudio());
                getAudioPacketListener().receive(audioPacket);

                processMetadata(audioWithMetadata);
            }
        }
    }

    /**
     * Processes optional metadata that can be included with decoded audio (ie dtmf, tones, knox, etc.)
     */
    private void processMetadata(IAudioWithMetadata audioWithMetadata)
    {
        if(mIdentifierUpdateNotificationListener != null)
        {
            if(audioWithMetadata.hasMetadata())
            {
                Map<String,String> metadata = audioWithMetadata.getMetadata();

                if(metadata.containsKey(METADATA_TYPE_DTMF))
                {
                    String dtmf = metadata.get(METADATA_TYPE_DTMF);

                    if(dtmf != null)
                    {
                        if(mDtmfMetadataProcessor == null)
                        {
                            mDtmfMetadataProcessor = new MetadataProcessor();
                        }

                        List<String> dtmfTones = mDtmfMetadataProcessor.process(dtmf);
                        broadcast(P25DtmfIdentifier.create(dtmfTones));
                    }
                }
                else if(metadata.containsKey(METADATA_TYPE_KNOX))
                {
                    String knox = metadata.get(METADATA_TYPE_KNOX);

                    if(knox != null)
                    {
                        if(mKnoxMetadataProcessor == null)
                        {
                            mKnoxMetadataProcessor = new MetadataProcessor();
                        }

                        List<String> knoxTones = mKnoxMetadataProcessor.process(knox);
                        broadcast(P25KnoxIdentifier.create(knoxTones));
                    }
                }
                else if(metadata.containsKey(METADATA_TYPE_TONE))
                {
                    String tone = metadata.get(METADATA_TYPE_TONE);

                    if(tone != null)
                    {
                        if(mToneMetadataProcessor == null)
                        {
                            mToneMetadataProcessor = new MetadataProcessor();
                        }

                        List<String> tones = mToneMetadataProcessor.process(tone);
                        broadcast(P25ToneIdentifier.create(tones));
                    }
                }
                else if(metadata.containsKey(METADATA_TYPE_CALL_PROGRESS))
                {
                    String callProgressTone = metadata.get(METADATA_TYPE_CALL_PROGRESS);

                    if(callProgressTone != null)
                    {
                        if(mCallProcessMetadataProcessor == null)
                        {
                            mCallProcessMetadataProcessor = new MetadataProcessor();
                        }

                        List<String> tones = mCallProcessMetadataProcessor.process(callProgressTone);
                        broadcast(P25CallProgressIdentifier.create(tones));
                    }
                }
            }
            else
            {
                if(mDtmfMetadataProcessor != null)
                {
                    mDtmfMetadataProcessor.noMetadata();
                }
                if(mKnoxMetadataProcessor != null)
                {
                    mKnoxMetadataProcessor.noMetadata();
                }
                if(mCallProcessMetadataProcessor != null)
                {
                    mCallProcessMetadataProcessor.noMetadata();
                }
                if(mToneMetadataProcessor != null)
                {
                    mToneMetadataProcessor.noMetadata();
                }
            }
        }
    }

    private void broadcast(Identifier identifier)
    {
        if(mIdentifierUpdateNotificationListener != null)
        {
            mIdentifierUpdateNotificationListener.receive(new IdentifierUpdateNotification(identifier,
                IdentifierUpdateNotification.Operation.ADD));
        }
    }

    @Override
    public void setIdentifierUpdateListener(Listener<IdentifierUpdateNotification> listener)
    {
        mIdentifierUpdateNotificationListener = listener;
    }

    @Override
    public void removeIdentifierUpdateListener()
    {
        mIdentifierUpdateNotificationListener = null;
    }

    /**
     * Processes metadata string values to provide a de-duplicated list of metadata values.
     *
     * This is necessary for metadata such as DTMF where a tone can span multiple 20ms voice frames but is intended
     * to be represented as a single tone.  An empty (no-metadata) frame causes the repeat checker to stop and allow
     * the next metadata value to be added to the list.
     */
    public class MetadataProcessor
    {
        private List<String> mValues = new ArrayList<>();
        private String mLastInput;

        public List<String> process(String value)
        {
            if(value != null)
            {
                if(mLastInput != null && mLastInput.equalsIgnoreCase(value))
                {
                    //Suppress the repeat and return the current value
                    return mValues;
                }
                else
                {
                    mLastInput = value;
                    mValues.add(value);
                    return mValues;
                }
            }

            return mValues;
        }

        public void noMetadata()
        {
            mLastInput = null;
        }
    }

    /**
     * Wrapper for squelch state to process end of call actions.  At call end the encrypted call state established
     * flag is reset so that the encrypted audio state for the next call can be properly detected and we send an
     * END audio packet so that downstream processors like the audio recorder can properly close out a call sequence.
     */
    public class SquelchStateListener implements Listener<SquelchState>
    {
        @Override
        public void receive(SquelchState state)
        {
            if(state == SquelchState.SQUELCH)
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

                reset();
            }
        }
    }

}
