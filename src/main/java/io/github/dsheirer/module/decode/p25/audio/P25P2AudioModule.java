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

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.audio.codec.mbe.AmbeAudioModule;
import io.github.dsheirer.audio.squelch.SquelchState;
import io.github.dsheirer.audio.squelch.SquelchStateEvent;
import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.identifier.IdentifierUpdateNotification;
import io.github.dsheirer.identifier.IdentifierUpdateProvider;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.tone.AmbeTone;
import io.github.dsheirer.identifier.tone.P25ToneIdentifier;
import io.github.dsheirer.identifier.tone.Tone;
import io.github.dsheirer.identifier.tone.ToneIdentifier;
import io.github.dsheirer.identifier.tone.ToneIdentifierMessage;
import io.github.dsheirer.identifier.tone.ToneSequence;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.IMessageProvider;
import io.github.dsheirer.module.decode.p25.phase2.message.EncryptionSynchronizationSequence;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacMessage;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.MacStructure;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.PushToTalk;
import io.github.dsheirer.module.decode.p25.phase2.timeslot.AbstractVoiceTimeslot;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.protocol.Protocol;
import io.github.dsheirer.sample.Listener;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import jmbe.iface.IAudioWithMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class P25P2AudioModule extends AmbeAudioModule implements IdentifierUpdateProvider, IMessageProvider
{
    private final static Logger mLog = LoggerFactory.getLogger(P25P2AudioModule.class);

    private Listener<IdentifierUpdateNotification> mIdentifierUpdateNotificationListener;
    private SquelchStateListener mSquelchStateListener = new SquelchStateListener();
    private ToneMetadataProcessor mToneMetadataProcessor = new ToneMetadataProcessor();
    private Queue<AbstractVoiceTimeslot> mQueuedAudioTimeslots = new ArrayDeque<>();
    private boolean mEncryptedCallStateEstablished = false;
    private boolean mEncryptedCall = false;
    private Listener<IMessage> mMessageListener;

    public P25P2AudioModule(UserPreferences userPreferences, int timeslot, AliasList aliasList)
    {
        super(userPreferences, aliasList, timeslot);
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
            if(message instanceof AbstractVoiceTimeslot abstractVoiceTimeslot)
            {
                if(mEncryptedCallStateEstablished)
                {
                    if(!mEncryptedCall)
                    {
                        processAudio(abstractVoiceTimeslot.getVoiceFrames(), message.getTimestamp());
                    }
                }
                else
                {
                    //Queue audio timeslots until we can determine if the audio is encrypted or not
                    mQueuedAudioTimeslots.offer(abstractVoiceTimeslot);
                }
            }
            else if(message instanceof MacMessage macMessage && message.isValid())
            {
                MacStructure macStructure = macMessage.getMacStructure();

                if(macStructure instanceof PushToTalk pushToTalk)
                {
                    mEncryptedCallStateEstablished = true;
                    mEncryptedCall = pushToTalk.isEncrypted();
                    //There should not be any pending voice timeslots to process since the PTT message is the first in
                    //the audio call sequence.
                    clearPendingVoiceTimeslots();
                }
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

    /**
     * Clears/deletes any pending voice timeslots
     */
    private void clearPendingVoiceTimeslots()
    {
        mQueuedAudioTimeslots.clear();
    }

    /**
     * Process the audio voice frames
     * @param voiceFrames to process
     * @param timestamp of the carrier message
     */
    private void processAudio(List<BinaryMessage> voiceFrames, long timestamp)
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
                    processMetadata(audioWithMetadata, timestamp);
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
    private void processMetadata(IAudioWithMetadata audioWithMetadata, long timestamp)
    {
        if(audioWithMetadata.hasMetadata())
        {
            //JMBE only places 1 entry in the map, but for consistency we'll process the map entry set
            for(Map.Entry<String,String> entry: audioWithMetadata.getMetadata().entrySet())
            {
                //Each metadata map entry contains a tone-type (key) and tone (value)
                ToneIdentifier toneIdentifier = mToneMetadataProcessor.process(entry.getKey(), entry.getValue());

                if(toneIdentifier != null)
                {
                    broadcast(toneIdentifier, timestamp);
                }
            }
        }
        else
        {
            mToneMetadataProcessor.closeMetadata();
        }
    }

    /**
     * Broadcasts the identifier to a registered listener and creates a new AMBE tone identifier message when tones are
     * present to send to the alias action manager
     */
    private void broadcast(ToneIdentifier identifier, long timestamp)
    {
        if(mIdentifierUpdateNotificationListener != null)
        {
            mIdentifierUpdateNotificationListener.receive(new IdentifierUpdateNotification(identifier,
                IdentifierUpdateNotification.Operation.ADD, getTimeslot()));
        }

        if(mMessageListener != null)
        {
            StringBuilder sb = new StringBuilder();
            sb.append("P25.2 Timeslot ");
            sb.append(getTimeslot());
            sb.append("Audio Tone Sequence Decoded: ");
            sb.append(identifier.toString());

            mMessageListener.receive(new ToneIdentifierMessage(Protocol.APCO25_PHASE2, getTimeslot(), timestamp,
                    identifier, sb.toString()));
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
     * Registers a message listener to receive AMBE tone identifier messages.
     * @param listener to register
     */
    @Override
    public void setMessageListener(Listener<IMessage> listener)
    {
        mMessageListener = listener;
    }

    /**
     * Removes the message listener
     */
    @Override
    public void removeMessageListener()
    {
        mMessageListener = null;
    }

    /**
     * Process AMBE audio frame tone metadata.  Tracks the count of sequential frames containing tone metadata to
     * provide a list of each unique tone and a time duration (milliseconds) for the tone.  Tones are concatenated into
     * a comma separated list and included as call segment metadata.
     */
    public class ToneMetadataProcessor
    {
        private List<Tone> mTones = new ArrayList<>();
        private Tone mCurrentTone;

        /**
         * Resets or clears any accumulated call tone sequences to prepare for the next call.
         */
        public void reset()
        {
            mTones.clear();
        }

        /**
         * Process the tone metadata
         * @param type of tone
         * @param value of tone
         * @return an identifier with the accumulated tone metadata set
         */
        public ToneIdentifier process(String type, String value)
        {
            if(type == null || value == null)
            {
                return null;
            }

            AmbeTone tone = AmbeTone.fromValues(type, value);

            if(tone == AmbeTone.INVALID)
            {
                return null;
            }

            if(mCurrentTone == null || mCurrentTone.getAmbeTone() != tone)
            {
                mCurrentTone = new Tone(tone);
                mTones.add(mCurrentTone);
            }

            mCurrentTone.incrementDuration();

            return P25ToneIdentifier.create(new ToneSequence(new ArrayList<>(mTones)));
        }

        /**
         * Closes current tone metadata when there is no metadata for the current audio frame.
         */
        public void closeMetadata()
        {
            mCurrentTone = null;
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
