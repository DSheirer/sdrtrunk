/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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

import com.google.common.base.Joiner;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.audio.codec.mbe.AmbeAudioModule;
import io.github.dsheirer.audio.squelch.SquelchState;
import io.github.dsheirer.audio.squelch.SquelchStateEvent;
import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierUpdateNotification;
import io.github.dsheirer.identifier.IdentifierUpdateProvider;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.tone.P25ToneIdentifier;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.p25.phase2.message.EncryptionSynchronizationSequence;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.PushToTalk;
import io.github.dsheirer.module.decode.p25.phase2.timeslot.AbstractVoiceTimeslot;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.sample.Listener;
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

    private Listener<IdentifierUpdateNotification> mIdentifierUpdateNotificationListener;
    private SquelchStateListener mSquelchStateListener = new SquelchStateListener();
    private ToneMetadataProcessor mToneMetadataProcessor = new ToneMetadataProcessor();
    private int mTimeslot;
    private Queue<AbstractVoiceTimeslot> mQueuedAudioTimeslots = new ArrayDeque<>();
    private boolean mEncryptedCallStateEstablished = false;
    private boolean mEncryptedCall = false;

    public P25P2AudioModule(UserPreferences userPreferences, int timeslot, AliasList aliasList)
    {
        super(userPreferences, aliasList);
        mTimeslot = timeslot;
        getIdentifierCollection().setTimeslot(timeslot);
    }

    protected int getTimeslot()
    {
        return mTimeslot;
    }

    @Override
    public Listener<SquelchStateEvent> getSquelchStateListener()
    {
        return mSquelchStateListener;
    }

    /**
     * Resets this audio module upon completion of an audio call to prepare for the next call.  This method is
     * controlled by the squelch state listener and squelch state is controlled by the P25P2DecoderState.
     */
    @Override
    public void reset()
    {
        //Explicitly clear FROM identifiers to ensure previous call TONE identifiers are cleared.
        mIdentifierCollection.remove(Role.FROM);

        mToneMetadataProcessor.reset();
        mQueuedAudioTimeslots.clear();

        //Reset encrypted call handling flags
        mEncryptedCallStateEstablished = false;
        mEncryptedCall = false;
    }

    @Override
    public void start()
    {
        reset();
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
            else if(message instanceof EncryptionSynchronizationSequence && message.isValid())
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
        if(hasAudioCodec())
        {
            for(BinaryMessage voiceFrame: voiceFrames)
            {
                byte[] voiceFrameBytes = voiceFrame.getBytes();

                try
                {
                    IAudioWithMetadata audioWithMetadata = getAudioCodec().getAudioWithMetadata(voiceFrameBytes);
                    addAudio(audioWithMetadata.getAudio());
                    processMetadata(audioWithMetadata);
                }
                catch(Exception e)
                {
                    mLog.error("Error synthesizing AMBE audio - continuing [" + e.getLocalizedMessage() + "]");
                }
            }
        }
    }

    /**
     * Processes optional metadata that can be included with decoded audio (ie dtmf, tones, knox, etc.) so that the
     * tone metadata can be converted into a FROM identifier and included with any call segment.
     */
    private void processMetadata(IAudioWithMetadata audioWithMetadata)
    {
        if(audioWithMetadata.hasMetadata())
        {
            //JMBE only places 1 entry in the map, but for consistency we'll process the map entry set
            for(Map.Entry<String,String> entry: audioWithMetadata.getMetadata().entrySet())
            {
                //Each metadata map entry contains a tone-type (key) and tone (value)
                Identifier metadataIdentifier = mToneMetadataProcessor.process(entry.getKey(), entry.getValue());

                if(metadataIdentifier != null)
                {
                    broadcast(metadataIdentifier);
                }
            }
        }
        else
        {
            mToneMetadataProcessor.closeMetadata();
        }
    }

    /**
     * Broadcasts the identifier to a registered listener
     */
    private void broadcast(Identifier identifier)
    {
        if(mIdentifierUpdateNotificationListener != null)
        {
            mIdentifierUpdateNotificationListener.receive(new IdentifierUpdateNotification(identifier,
                IdentifierUpdateNotification.Operation.ADD, getTimeslot()));
        }
    }

    /**
     * Registers the listener to receive identifier updates
     */
    @Override
    public void setIdentifierUpdateListener(Listener<IdentifierUpdateNotification> listener)
    {
        mIdentifierUpdateNotificationListener = listener;
    }

    /**
     * Unregisters a listener from receiving identifier updates
     */
    @Override
    public void removeIdentifierUpdateListener()
    {
        mIdentifierUpdateNotificationListener = null;
    }

    /**
     * Process AMBE audio frame tone metadata.  Tracks the count of sequential frames containing tone metadata to
     * provide a list of each unique tone and a time duration (milliseconds) for the tone.  Tones are concatenated into
     * a comma separated list and included as call segment metadata.
     */
    public class ToneMetadataProcessor
    {
        private List<ToneMetadata> mToneMetadata = new ArrayList<>();
        private ToneMetadata mCurrentToneMetadata;

        /**
         * Resets or clears any accumulated call tone metadata to prepare for the next call.
         */
        public void reset()
        {
            mToneMetadata.clear();
        }

        /**
         * Process the tone metadata
         * @param type of tone
         * @param value of tone
         * @return an identifier with the accumulated tone metadata set
         */
        public Identifier process(String type, String value)
        {
            if(type == null || value == null)
            {
                return null;
            }

            if(mCurrentToneMetadata != null && mCurrentToneMetadata.matches(type, value))
            {
                mCurrentToneMetadata.incrementCount();
            }
            else
            {
                mCurrentToneMetadata = new ToneMetadata(type, value);
                mToneMetadata.add(mCurrentToneMetadata);
                mCurrentToneMetadata.incrementCount();
            }

            return P25ToneIdentifier.create(Joiner.on(",").join(mToneMetadata));
        }

        /**
         * Closes current tone metadata when there is no metadata for the current audio frame.
         */
        public void closeMetadata()
        {
            mCurrentToneMetadata = null;
        }
    }

    /**
     * Metadata about the tone type being transmitted and the duration.
     *
     * Note: each AMBE frame is 20 milliseconds in duration, so total duration is the count of 20 ms tone frames
     * of the same metadata type.
     */
    public class ToneMetadata
    {
        private String mType;
        private String mValue;
        private int mCount;

        /**
         * Constructs an instance
         * @param type of tone
         * @param value of the tone
         */
        public ToneMetadata(String type, String value)
        {
            mType = type;
            mValue = value;
            mCount = 0;
        }

        /**
         * Indicates if this tone metadata matches the tone metadata represented by the arguments
         * @param type of tone
         * @param value of tone
         * @return true if they match
         */
        public boolean matches(String type, String value)
        {
            return type != null && value != null && type.matches(mType) && value.matches(mValue);
        }

        /**
         * Type of tone
         */
        public String getType()
        {
            return mType;
        }

        /**
         * Value of tone
         */
        public String getValue()
        {
            return mValue;
        }

        /**
         * Number of times this tone has occurred sequentially
         */
        public int getCount()
        {
            return mCount;
        }

        /**
         * Increments the count of the number of sequential frames that contained the tone
         */
        public void incrementCount()
        {
            mCount++;
        }

        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder();
            sb.append(mType);
            sb.append(" ").append(mValue);
            sb.append(" (").append(mCount * 20).append("ms)");
            return sb.toString();
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
            if(event.getTimeslot() == getTimeslot())
            {
                if(event.getSquelchState() == SquelchState.SQUELCH)
                {
                    closeAudioSegment();
                    reset();
                }
            }
        }
    }
}
