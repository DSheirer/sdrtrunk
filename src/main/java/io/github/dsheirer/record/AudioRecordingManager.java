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

package io.github.dsheirer.record;

import io.github.dsheirer.audio.call.AudioSegment;
import io.github.dsheirer.audio.call.Call;
import io.github.dsheirer.audio.call.CompletedCall;
import io.github.dsheirer.audio.call.ICallEventListener;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.integer.IntegerIdentifier;
import io.github.dsheirer.identifier.string.StringIdentifier;
import io.github.dsheirer.identifier.tone.Tone;
import io.github.dsheirer.identifier.tone.ToneIdentifier;
import io.github.dsheirer.identifier.tone.ToneSequence;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.util.StringUtils;
import io.github.dsheirer.util.ThreadPool;
import io.github.dsheirer.util.TimeStamp;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Monitors audio segments and upon completion records any audio segments that have been flagged as recordable
 */
@Component("audioRecordingManager")
public class AudioRecordingManager implements ICallEventListener
{
    private final static Logger mLog = LoggerFactory.getLogger(AudioRecordingManager.class);
    @Resource
    private UserPreferences mUserPreferences;
    private LinkedTransferQueue<CompletedCall> mCompletedCallQueue = new LinkedTransferQueue<>();
    private ScheduledFuture<?> mQueueProcessorHandle;
    private int mUnknownAudioRecordingIndex = 1;
    private int mDuplicateAudioRecordingSuffix = 1;
    private String mPreviousRecordingPath = null;

    /**
     * Constructs an instance
     */
    public AudioRecordingManager()
    {
    }

    /**
     * Starts the manager and begins audio segment recording.
     */
    @PostConstruct
    public void start()
    {
        if(mQueueProcessorHandle == null)
        {
            mQueueProcessorHandle = ThreadPool.SCHEDULED.scheduleAtFixedRate(new CompletedCallProcessor(),
                0, 1, TimeUnit.SECONDS);
        }
    }

    /**
     * Stops the manager and records any remaining queued audio segments.
     */
    @PreDestroy
    public void stop()
    {
        if(mQueueProcessorHandle != null)
        {
            mQueueProcessorHandle.cancel(true);
            processCompletedCalls();
            mQueueProcessorHandle = null;
        }
    }

    @Override
    public void added(Call call) {} //Not implemented

    @Override
    public void updated(Call call) {} //Not implemented

    @Override
    public void deleted(Call call) {} //Not implemented

    /**
     * Implements the ICallEventListener interface to receive completed audio segments/calls and enqueue them for
     * recording
     * @param call that was completed.
     * @param audioSegment for the call
     */
    @Override
    public void completed(Call call, AudioSegment audioSegment)
    {
        //Only enqueue if the audio segment is marked recordable by one of the identifier aliases.
        if(audioSegment.recordAudioProperty().get())
        {
            mCompletedCallQueue.add(new CompletedCall(call, audioSegment));
        }
        else
        {
            audioSegment.removeLease(getClass().toString());
        }
    }

    /**
     * Processes any queued calls/audio segments
     */
    private void processCompletedCalls()
    {
        RecordFormat recordFormat = mUserPreferences.getRecordPreference().getAudioRecordFormat();
        CompletedCall completedCall = mCompletedCallQueue.poll();

        while(completedCall != null)
        {
            if(!(completedCall.audioSegment().isDuplicate() &&
                mUserPreferences.getCallManagementPreference().isDuplicateRecordingSuppressionEnabled()))
            {
                Path source = Path.of(completedCall.call().getFile());
                Path target = getAudioRecordingPath(completedCall.audioSegment().getIdentifierCollection(), recordFormat);

                try
                {
                    Files.copy(source, target);
                }
                catch(IOException ioe)
                {
                    mLog.error("Error recording audio segment to [" + target + "]");
                }
            }

            completedCall.audioSegment().removeLease(getClass().toString());

            //Grab the next one to record
            completedCall = mCompletedCallQueue.poll();
        }
    }

    /**
     * Base path to recordings folder
     * @return recording base path
     */
    public Path getRecordingBasePath()
    {
        return mUserPreferences.getDirectoryPreference().getDirectoryRecording();
    }

    /**
     * Provides a formatted audio recording filename to use as the final audio filename.
     */
    private Path getAudioRecordingPath(IdentifierCollection identifierCollection, RecordFormat recordFormat)
    {
        StringBuilder sb = new StringBuilder();

        if(identifierCollection != null)
        {
            Identifier system = identifierCollection.getIdentifier(IdentifierClass.CONFIGURATION, Form.SYSTEM, Role.ANY);

            if(system != null)
            {
                sb.append(((StringIdentifier)system).getValue()).append("_");
            }

            Identifier site = identifierCollection.getIdentifier(IdentifierClass.CONFIGURATION, Form.SITE, Role.ANY);

            if(site != null)
            {
                sb.append(((StringIdentifier)site).getValue()).append("_");
            }

            Identifier channel = identifierCollection.getIdentifier(IdentifierClass.CONFIGURATION, Form.CHANNEL, Role.ANY);

            if(channel != null)
            {
                sb.append(((StringIdentifier)channel).getValue()).append("_");
            }

            Identifier to = identifierCollection.getIdentifier(IdentifierClass.USER, Form.TALKGROUP, Role.TO);

            if(to != null)
            {
                String toValue = ((IntegerIdentifier)to).getValue().toString().replace(":", "");
                sb.append("_TO_").append(toValue);
            }
            else
            {
                List<Identifier> toIdentifiers = identifierCollection.getIdentifiers(Role.TO);

                if(!toIdentifiers.isEmpty())
                {
                    sb.append("_TO_").append(toIdentifiers.get(0));
                }
            }

            Identifier from = identifierCollection.getIdentifier(IdentifierClass.USER, Form.RADIO, Role.FROM);

            if(from != null)
            {
                String fromValue = ((IntegerIdentifier)from).getValue().toString().replace(":", "");
                sb.append("_FROM_").append(fromValue);
            }
            else
            {
                List<Identifier> fromIdentifiers = identifierCollection.getIdentifiers(Role.FROM);

                if(!fromIdentifiers.isEmpty())
                {
                    for(Identifier identifier: fromIdentifiers)
                    {
                        if(identifier.getForm() != Form.TONE)
                        {
                            sb.append("_FROM_").append(identifier);
                            break;
                        }
                    }
                }
            }

            List<Identifier> toneIdentifiers = identifierCollection.getIdentifiers(IdentifierClass.USER, Form.TONE);

            if(!toneIdentifiers.isEmpty())
            {
                try
                {
                    Identifier identifier = toneIdentifiers.get(0);

                    if(identifier instanceof ToneIdentifier)
                    {
                        ToneIdentifier toneIdentifier = (ToneIdentifier)identifier;
                        ToneSequence toneSequence = toneIdentifier.getValue();

                        if(toneSequence.hasTones())
                        {
                            sb.append("_TONES");

                            for(Tone tone: toneIdentifier.getValue().getTones())
                            {
                                String label = tone.getAmbeTone().toString();
                                label = label.replace("TONE", "").trim();
                                label = label.replace(" ", "_");
                                sb.append("_").append(label);
                            }
                        }
                    }
                }
                catch(Exception e)
                {
                    mLog.error("Error appending tones to audio recording filename");
                }
            }
        }
        else
        {
            sb.append("audio_recording_no_metadata_").append(mUnknownAudioRecordingIndex++);

            if(mUnknownAudioRecordingIndex < 0)
            {
                mUnknownAudioRecordingIndex = 1;
            }
        }

        StringBuilder sbFinal = new StringBuilder();
        sbFinal.append(TimeStamp.getTimeStamp("_"));

        //Remove any illegal filename characters
        String cleaned = StringUtils.replaceIllegalCharacters(sb.toString());

        //Ensure total length doesn't exceed 255 characters.  Allow room for timestamp, versioning and extension.
        int maxLength = 255 - sbFinal.length() - ("_V" + mDuplicateAudioRecordingSuffix).length() -
            recordFormat.getExtension().length();

        if(cleaned.length() > maxLength)
        {
            cleaned = cleaned.substring(0, maxLength);
        }

        sbFinal.append(cleaned);

        if(mPreviousRecordingPath != null && mPreviousRecordingPath.contentEquals(sbFinal.toString()))
        {
            sbFinal.append("_V").append(mDuplicateAudioRecordingSuffix++);
        }
        else
        {
            mDuplicateAudioRecordingSuffix = 2;
            mPreviousRecordingPath = sbFinal.toString();
        }

        sbFinal.append(recordFormat.getExtension());

        return getRecordingBasePath().resolve(sbFinal.toString());
    }

    /**
     * Threaded queue processor to process/record each completed call
     */
    public class CompletedCallProcessor implements Runnable
    {
        @Override
        public void run()
        {
            try
            {
                processCompletedCalls();
            }
            catch(Throwable t)
            {
                mLog.error("Error while processing queued audio segments to recordings", t);
            }
        }
    }
}
