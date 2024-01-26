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

package io.github.dsheirer.audio.call;

import com.google.common.base.Joiner;
import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.audio.AudioFormats;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.configuration.ChannelNameConfigurationIdentifier;
import io.github.dsheirer.identifier.configuration.DecoderTypeConfigurationIdentifier;
import io.github.dsheirer.identifier.configuration.FrequencyConfigurationIdentifier;
import io.github.dsheirer.identifier.configuration.SiteConfigurationIdentifier;
import io.github.dsheirer.identifier.configuration.SystemConfigurationIdentifier;
import io.github.dsheirer.log.LoggingSuppressor;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.record.wave.AudioMetadata;
import io.github.dsheirer.record.wave.AudioMetadataUtils;
import io.github.dsheirer.record.wave.WaveWriter;
import io.github.dsheirer.sample.ConversionUtils;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages an active audio segment through to completion and provides options for refreshing the associated call
 * object.
 */
public class ActiveAudioSegment
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ActiveAudioSegment.class);
    private static final LoggingSuppressor LOGGING_SUPPRESSOR = new LoggingSuppressor(LOGGER);
    private static final SimpleDateFormat DIRECTORY_SDF = new SimpleDateFormat("yyyyMMdd");
    private static final SimpleDateFormat FILE_NAME_SDF = new SimpleDateFormat("yyyyMMdd_HHmmss");
    private static int sFileNameCounter = 1;

    private AudioSegment mAudioSegment;
    private Call mCall;
    private UserPreferences mUserPreferences;
    private WaveWriter mWaveWriter;
    private Path mPath;
    private int mAudioBufferPointer = 0;

    /**
     * Constructs an instance
     * @param audioSegment
     * @param call
     * @param userPreferences for lookup of recording directory
     */
    public ActiveAudioSegment(AudioSegment audioSegment, Call call, UserPreferences userPreferences)
    {
        mAudioSegment = audioSegment;
        mAudioSegment.addLease(getClass().toString());
        mCall = call;
        mUserPreferences = userPreferences;

        mPath = getAudioRecordingPath();
        mCall.setFile(mPath.toString());
    }

    /**
     * Performs disposal workflow.
     */
    public void dispose()
    {
        if(mWaveWriter != null)
        {
            closeAudio();
        }
        mCall.setComplete(true);
        mAudioSegment.removeLease(getClass().toString());
        mAudioSegment = null;
        mCall = null;
    }

    /**
     * Audio Segment that is active.
     * @return audio segment
     */
    public AudioSegment getAudioSegment()
    {
        return mAudioSegment;
    }

    /**
     * Call entity associated with this audio segment
     * @return call entity
     */
    public Call getCall()
    {
        return mCall;
    }

    /**
     * Indicates if this audio segment is active and continues to receive audio buffer and identifier updates.
     * @return true if active or false if the audio segment is complete.
     */
    public boolean isComplete()
    {
        return getAudioSegment().isComplete();
    }

    /**
     * Attempts to update/refresh the call entity with details from the audio segment and returns true if any details in
     * the call entity were updated or false otherwise.
     * @return status of the update, true if updated or false if no update.
     */
    public boolean update()
    {
        boolean updated = false;
        updated |= updateTo(mCall, mAudioSegment.getIdentifierCollection().getToIdentifier(), getAudioSegment().getAliasList());
        updated |= updateFrom(mCall, mAudioSegment.getIdentifierCollection().getFromIdentifier(), getAudioSegment().getAliasList());
        updated |= updateDuration(mCall, mAudioSegment.getDuration());
        updated |= updateFlags(mCall, mAudioSegment);
        updated |= processAudio();
        return updated;
    }

    /**
     * Generates a unique audio recording file path.
     * @return audio recording path.
     */
    private Path getAudioRecordingPath()
    {
        Date now = new Date();
        String dailyFolder = DIRECTORY_SDF.format(now);
        Path recordingDirectory = mUserPreferences.getDirectoryPreference().getDirectoryCalls()
                .resolve(dailyFolder);

        if(!Files.exists(recordingDirectory))
        {
            try
            {
                Files.createDirectories(recordingDirectory);
            }
            catch(IOException ioe)
            {
                LOGGING_SUPPRESSOR.error(dailyFolder, 1, "Unable to create daily calls recording " +
                        "folder [" + dailyFolder + "]", ioe);
                return null;
            }
        }

        int recordingNumber = sFileNameCounter++;
        String fileName = FILE_NAME_SDF.format(now) + "_call_" + recordingNumber + ".wav";
        return recordingDirectory.resolve(fileName);
    }

    /**
     * Writes any buffered audio to the wave recording.
     * @return true if the call was updated with the audio recording path.
     */
    private boolean processAudio()
    {
        boolean updated = false;

        if(mAudioSegment.hasAudio())
        {
            if(mWaveWriter == null)
            {
                try
                {
                    mWaveWriter = new WaveWriter(AudioFormats.PCM_SIGNED_8000_HZ_16_BIT_MONO, mPath);
                    updated = true;
                }
                catch(IOException ioe)
                {
                    LOGGER.error("Error creating wave file: " + mPath);
                }
            }

            List<float[]> audioBuffers = mAudioSegment.getAudioBuffers();
            int size = audioBuffers.size();

            if(mWaveWriter != null && size > mAudioBufferPointer)
            {
                for(int x = mAudioBufferPointer; x < audioBuffers.size(); x++)
                {
                    try
                    {
                        mWaveWriter.writeData(ConversionUtils.convertToSigned16BitSamples(audioBuffers.get(x)));
                    }
                    catch(IOException ioe)
                    {
                        LOGGING_SUPPRESSOR.error("Call Wave Writer", 3, "Error while recording call audio", ioe);
                    }
                }

                mAudioBufferPointer = size;
            }
        }

        return updated;
    }

    /**
     * Writes remaining audio and metadata to wave file and closes the recorder.
     */
    public void closeAudio()
    {
        processAudio();

        if(mWaveWriter != null)
        {
            Map<AudioMetadata,String> metadataMap = AudioMetadataUtils.getMetadataMap(mAudioSegment.getIdentifierCollection(),
                    mAudioSegment.getAliasList());

            ByteBuffer listChunk = AudioMetadataUtils.getLISTChunk(metadataMap);
            byte[] id3Bytes = AudioMetadataUtils.getMP3ID3(metadataMap);
            ByteBuffer id3Chunk = AudioMetadataUtils.getID3Chunk(id3Bytes);
            try
            {
                mWaveWriter.writeMetadata(listChunk, id3Chunk);
            }
            catch(IOException ioe)
            {
                LOGGING_SUPPRESSOR.error("Metadata Write Error", 3,
                        "Error writing call ID3 metadata to recording", ioe);
            }

            try
            {
                mWaveWriter.close();
            }
            catch(IOException ioe)
            {
                LOGGING_SUPPRESSOR.error("Recording Close Error", 3, "Error closing audio wave recording", ioe);
            }

            mWaveWriter = null;
        }
    }

    /**
     * Utility method to create an initial call instance from an Audio Segment.  Note: this static method is used by
     * an external database controller so that the call event can be stored in the database before
     * @param audioSegment
     * @return
     */
    public static Call create(AudioSegment audioSegment)
    {
        if(audioSegment == null)
        {
            return null;
        }

        Call call = new Call();

        call.setEventTime(audioSegment.getStartTimestamp());

        Identifier decoder = audioSegment.getIdentifierCollection().getIdentifier(IdentifierClass.CONFIGURATION,
                Form.DECODER_TYPE, Role.ANY);

        if(decoder instanceof DecoderTypeConfigurationIdentifier dtci)
        {
            call.setProtocol(dtci.getValue().toString());
        }

        Identifier systemId = audioSegment.getIdentifierCollection()
                .getIdentifier(IdentifierClass.CONFIGURATION, Form.SYSTEM, Role.ANY);

        if(systemId instanceof SystemConfigurationIdentifier sci)
        {
            call.setSystem(sci.getValue());
        }

        Identifier siteId = audioSegment.getIdentifierCollection()
                .getIdentifier(IdentifierClass.CONFIGURATION, Form.SITE, Role.ANY);

        if(siteId instanceof SiteConfigurationIdentifier siteci)
        {
            call.setSite(siteci.getValue());
        }

        Identifier channelId = audioSegment.getIdentifierCollection()
                .getIdentifier(IdentifierClass.CONFIGURATION, Form.CHANNEL, Role.ANY);

        if(channelId instanceof ChannelNameConfigurationIdentifier cnci)
        {
            call.setChannel(cnci.getValue());
        }

        Identifier frequency = audioSegment.getIdentifierCollection()
                .getIdentifier(IdentifierClass.CONFIGURATION, Form.CHANNEL_FREQUENCY, Role.ANY);

        if(frequency instanceof FrequencyConfigurationIdentifier fci)
        {
            //Convert Hertz to MegaHertz for the call value.
            call.setFrequency(fci.getValue() / 1E6d);
        }

        updateFrom(call, audioSegment.getIdentifierCollection().getFromIdentifier(), audioSegment.getAliasList());
        updateTo(call, audioSegment.getIdentifierCollection().getToIdentifier(), audioSegment.getAliasList());
        updateDuration(call, audioSegment.getDuration());
        updateFlags(call, audioSegment);

        return call;
    }

    /**
     * Updates the duplicate, record, stream and other call flags/info
     * @param call to update
     * @param audioSegment containing flags/info
     * @return true if the call is updated.
     */
    private static boolean updateFlags(Call call, AudioSegment audioSegment)
    {
        boolean updated = false;

        if(call.isDuplicate() ^ audioSegment.isDuplicate())
        {
            call.setDuplicate(true);
            updated = true;
        }

        if(call.isRecord() ^ audioSegment.recordAudioProperty().get())
        {
            call.setRecord(true);
            updated = true;
        }

        if(call.isStream() ^ audioSegment.hasBroadcastChannels())
        {
            call.setStream(true);
            updated = true;
        }

        if(call.getMonitor() != audioSegment.monitorPriorityProperty().get())
        {
            call.setMonitor(audioSegment.monitorPriorityProperty().get());
            updated = true;
        }

        return updated;
    }

    /**
     * Updates the call duration field
     * @param call to update
     * @param durationMs value
     * @return true if the value was updated
     */
    private static boolean updateDuration(Call call,  long durationMs)
    {
        double durationSeconds = durationMs / 1000.0;

        if(call.getDuration() != durationSeconds)
        {
            call.setDuration(durationSeconds);
            return true;
        }

        return false;
    }

    /**
     * Updates the FROM identifier value.
     * @param call to update
     * @param fromIdentifier value
     * @param aliasList for alias lookups
     * @return true if the value was updated
     */
    private static boolean updateFrom(Call call, Identifier fromIdentifier, AliasList aliasList)
    {
        if(fromIdentifier == null)
        {
            return false;
        }

        if(call.getFromId() == null || call.getFromId().contentEquals(fromIdentifier.toString()))
        {
            call.setFromId(fromIdentifier.toString());

            if(aliasList != null)
            {
                List<Alias> aliases = aliasList.getAliases(fromIdentifier);

                if(!aliases.isEmpty())
                {
                    call.setFromAlias(Joiner.on(";").join(aliases));
                }
            }

            return true;
        }

        return false;
    }

    /**
     * Updates the TO identifier value.
     * @param call to update
     * @param toIdentifier value
     * @return true if the value was updated
     */
    private static boolean updateTo(Call call, Identifier toIdentifier, AliasList aliasList)
    {
        if(toIdentifier == null)
        {
            return false;
        }

        if(call.getToId() == null || call.getToId().contentEquals(toIdentifier.toString()))
        {
            call.setToId(toIdentifier.toString());

            if(aliasList != null)
            {
                List<Alias> aliases = aliasList.getAliases(toIdentifier);

                if(!aliases.isEmpty())
                {
                    call.setToAlias(Joiner.on(";").join(aliases));
                }
            }

            switch(toIdentifier.getForm())
            {
                case TALKGROUP -> call.setCallType("Talk Group");
                case RADIO ->
                {
                    LOGGER.info("Private Call detected - TO identifier: " + toIdentifier);
                    call.setCallType("Private");
                }
                case PATCH_GROUP -> call.setCallType("Patch Group");
                default -> call.setCallType(toIdentifier.getForm().toString());
            }

            return true;
        }

        return false;
    }
}
