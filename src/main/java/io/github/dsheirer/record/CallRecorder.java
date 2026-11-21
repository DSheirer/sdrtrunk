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
package io.github.dsheirer.record;

import io.github.dsheirer.audio.AudioSegment;
import io.github.dsheirer.database.call.CallRecord;
import io.github.dsheirer.database.call.RecordingState;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.string.StringIdentifier;
import io.github.dsheirer.identifier.tone.Tone;
import io.github.dsheirer.identifier.tone.ToneIdentifier;
import io.github.dsheirer.identifier.tone.ToneSequence;
import io.github.dsheirer.settings.Settings;
import io.github.dsheirer.record.stream.IStreamingAudioWriter;
import io.github.dsheirer.record.stream.StreamingMP3Writer;
import io.github.dsheirer.record.stream.StreamingWaveWriter;
import io.github.dsheirer.record.wave.AudioMetadataUtils;
import io.github.dsheirer.util.StringUtils;
import io.github.dsheirer.util.TimeStamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Records call audio to MP3 or WAVE files while the call is in progress (streaming).
 *
 * Every call is recorded when the record all calls preference is enabled (default), otherwise only calls with an
 * alias that has the record option.  The recording file exists from the start of the call, and its path is stored
 * with the call.  The file name is fixed when the recording starts, so it contains the identifiers known at that time.
 * The recording starts when the call has a TO identifier, or after one second of audio without one.  The final
 * metadata (ID3/LIST tags) is written when the call completes.
 *
 * The call manager creates one {@link Recording} per call and drives it from its call processing thread.  This class
 * isn't thread safe: all methods must be invoked on that thread.
 */
public class CallRecorder
{
    private final static Logger mLog = LoggerFactory.getLogger(CallRecorder.class);
    private static final long MAX_WAIT_FOR_TO_IDENTIFIER_MS = 1000;
    private final Settings mSettings;
    private int mUnknownAudioRecordingIndex = 1;
    private int mDuplicateAudioRecordingSuffix = 1;
    private String mPreviousRecordingPath = null;

    /**
     * Constructs an instance
     * @param settings for the recording format, record all calls and the recordings directory
     */
    public CallRecorder(Settings settings)
    {
        mSettings = settings;
    }

    /**
     * Creates the recording for a call.  The recording starts when the call is recordable.
     */
    public Recording createRecording(AudioSegment audioSegment)
    {
        return new Recording(audioSegment);
    }

    /**
     * Base path to recordings folder
     */
    public Path getRecordingBasePath()
    {
        return mSettings.getDirectorySettings().getDirectoryRecording();
    }

    /**
     * Folder for a new recording: the recordings folder, or a subfolder of it from the folder layout preference.
     */
    Path getRecordingFolder(IdentifierCollection identifierCollection)
    {
        var preference = mSettings.getRecordSettings();
        return RecordingFolders.getFolder(getRecordingBasePath(), preference.getRecordingFolderLayout(),
            preference.isRecordingHourFolder(), RecordingFolders.CallFolders.from(identifierCollection),
            System.currentTimeMillis());
    }

    /**
     * Indicates if the call is recorded: all calls when record all calls is enabled, otherwise only calls with an
     * alias that has the record option.  Duplicate and Do Not Monitor calls are recorded and later deleted by the
     * call database retention rule.
     */
    public boolean isRecordable(AudioSegment audioSegment)
    {
        return mSettings.getRecordSettings().isRecordAllCalls() || audioSegment.recordAudioProperty().get();
    }

    /**
     * Creates a unique recording file path for the identifiers.
     */
    private Path getUniqueRecordingPath(IdentifierCollection identifierCollection, RecordFormat recordFormat)
    {
        Path path = getAudioRecordingPath(identifierCollection, recordFormat);

        try
        {
            Files.createDirectories(path.getParent());
        }
        catch(IOException ioe)
        {
            mLog.error("Unable to create the recording folder [" + path.getParent() + "] - " + ioe.getMessage());
        }

        if(!Files.exists(path))
        {
            return path;
        }

        String name = path.getFileName().toString();
        String base = name.substring(0, name.length() - recordFormat.getExtension().length());

        for(int version = 2; version < 1000; version++)
        {
            Path candidate = path.resolveSibling(base + "_V" + version + recordFormat.getExtension());

            if(!Files.exists(candidate))
            {
                return candidate;
            }
        }

        return path.resolveSibling(base + "_" + System.nanoTime() + recordFormat.getExtension());
    }

    /**
     * Opens a streaming writer for the recording format.
     */
    private IStreamingAudioWriter createWriter(Path path, RecordFormat recordFormat, AudioSegment audioSegment)
        throws IOException
    {
        return switch(recordFormat)
        {
            case MP3 -> new StreamingMP3Writer(path, mSettings.getMP3Settings().getAudioSampleRate(),
                mSettings.getMP3Settings().getMP3Setting(),
                mSettings.getMP3Settings().isNormalizeAudioBeforeEncode(),
                AudioMetadataUtils.getMetadataMap(audioSegment.getIdentifierCollection(), audioSegment.getAliasList()));
            case WAVE -> new StreamingWaveWriter(path);
        };
    }

    /**
     * Recording of one call.  It updates the recording fields of the call record (format, file path, file size and
     * recording state).
     */
    public class Recording
    {
        private final AudioSegment mAudioSegment;
        private IStreamingAudioWriter mWriter;
        private int mBuffersWritten;
        private boolean mRecordingAttempted;

        private Recording(AudioSegment audioSegment)
        {
            mAudioSegment = audioSegment;
        }

        /**
         * Starts the recording when the call becomes recordable, and writes the audio received since the last update.
         * @param call record to update with the recording details
         */
        public void update(CallRecord call)
        {
            if(mWriter != null)
            {
                writeAudio(call);
                return;
            }

            if(mRecordingAttempted || !isRecordable(mAudioSegment) || !mAudioSegment.hasAudio())
            {
                return;
            }

            //Give the decoder a moment to provide the TO identifier, since it is part of the file name
            if(mAudioSegment.getIdentifierCollection().getToIdentifier() == null &&
                mAudioSegment.getDuration() < MAX_WAIT_FOR_TO_IDENTIFIER_MS && !mAudioSegment.isComplete())
            {
                return;
            }

            mRecordingAttempted = true;
            RecordFormat recordFormat = mSettings.getRecordSettings().getAudioRecordFormat();
            Path path = getUniqueRecordingPath(mAudioSegment.getIdentifierCollection(), recordFormat);

            try
            {
                try
                {
                    mWriter = createWriter(path, recordFormat, mAudioSegment);
                }
                catch(NoSuchFileException nsfe)
                {
                    //The age-off removed the (empty) folder after it was created - create it again
                    Files.createDirectories(path.getParent());
                    mWriter = createWriter(path, recordFormat, mAudioSegment);
                }
                call.setRecordFormat(recordFormat.name());
                call.setFilePath(path.toAbsolutePath().toString());
                call.setRecordingState(RecordingState.RECORDING);
                writeAudio(call);
            }
            catch(IOException ioe)
            {
                mLog.error("Error creating audio recording [" + path + "] - " + ioe.getMessage());
                mWriter = null;
                call.setRecordingState(RecordingState.FAILED);
            }
        }

        /**
         * Writes the audio buffers that haven't been written yet.
         */
        private void writeAudio(CallRecord call)
        {
            int count = mAudioSegment.getAudioBufferCount();

            if(mWriter != null && mBuffersWritten < count)
            {
                List<float[]> buffers = new ArrayList<>(mAudioSegment.getAudioBuffers().subList(mBuffersWritten, count));

                try
                {
                    mWriter.write(buffers);
                    mBuffersWritten = count;
                }
                catch(IOException ioe)
                {
                    mLog.error("Error writing audio recording [" + mWriter.getPath() + "] - " + ioe.getMessage());
                    mWriter.abort();
                    mWriter = null;
                    call.setRecordingState(RecordingState.FAILED);
                }
            }
        }

        /**
         * Writes the remaining audio and the final metadata, and closes the recording.  Invoke when the call
         * completes.
         * @param call record to update with the final recording details
         */
        public void complete(CallRecord call)
        {
            update(call);

            if(mWriter != null)
            {
                writeAudio(call);

                if(mWriter != null)
                {
                    Path path = mWriter.getPath();

                    try
                    {
                        mWriter.close(AudioMetadataUtils.getMetadataMap(mAudioSegment.getIdentifierCollection(),
                            mAudioSegment.getAliasList()));
                        call.setFileSize(Files.size(path));
                        call.setRecordingState(RecordingState.RECORDED);
                    }
                    catch(IOException ioe)
                    {
                        mLog.error("Error completing audio recording [" + path + "] - " + ioe.getMessage());
                        call.setRecordingState(Files.exists(path) ? RecordingState.RECORDED : RecordingState.FAILED);
                    }

                    mWriter = null;
                }
            }
        }
    }

    /**
     * Provides a formatted audio recording filename to use as the final audio filename.
     */
    /**
     * Appends a file name part, with an underscore separator after the previous part.
     */
    private static void appendPart(StringBuilder sb, String part)
    {
        if(part != null && !part.isBlank())
        {
            if(!sb.isEmpty())
            {
                sb.append("_");
            }

            sb.append(part.trim());
        }
    }

    private static String getConfiguration(IdentifierCollection identifierCollection, Form form)
    {
        Identifier identifier = identifierCollection.getIdentifier(IdentifierClass.CONFIGURATION, form, Role.ANY);
        return identifier instanceof StringIdentifier stringIdentifier ? stringIdentifier.getValue() : null;
    }

    private Path getAudioRecordingPath(IdentifierCollection identifierCollection, RecordFormat recordFormat)
    {
        StringBuilder sb = new StringBuilder();

        if(identifierCollection != null)
        {
            //Leave out the system, site and channel names when a recording subfolder already shows them
            RecordingFolderLayout layout = mSettings.getRecordSettings().getRecordingFolderLayout();

            if(!layout.hasSystemFolder())
            {
                appendPart(sb, getConfiguration(identifierCollection, Form.SYSTEM));
            }

            if(!layout.hasSiteFolder())
            {
                appendPart(sb, getConfiguration(identifierCollection, Form.SITE));
            }

            if(!layout.hasChannelFolder())
            {
                appendPart(sb, getConfiguration(identifierCollection, Form.CHANNEL));
            }

            Identifier to = identifierCollection.getIdentifier(IdentifierClass.USER, Form.TALKGROUP, Role.TO);

            if(to != null)
            {
                appendPart(sb, "TO_" + clean(to.toString()));
            }
            else
            {
                List<Identifier> toIdentifiers = identifierCollection.getIdentifiers(Role.TO);

                if(!toIdentifiers.isEmpty())
                {
                    appendPart(sb, "TO_" + clean(toIdentifiers.get(0).toString()));
                }
            }

            Identifier from = identifierCollection.getIdentifier(IdentifierClass.USER, Form.RADIO, Role.FROM);

            if(from != null)
            {
                appendPart(sb, "FROM_" + clean(from.toString()));
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
                            appendPart(sb, "FROM_" + clean(identifier.toString()));
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
                            appendPart(sb, "TONES");

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
        sbFinal.append(TimeStamp.getTimeStamp("_")).append("_");

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

        return getRecordingFolder(identifierCollection).resolve(sbFinal.toString());
    }


    /**
     * Audio segment completion monitor.  Listens for the audio segment's complete flag to be set and then
     * queues the audio segment for recording.
     */

    public static String clean(String value)
    {
        if(value != null)
        {
            return value.replace(":", "")
                    .replace(".", "_")
                    .replace("(", "_")
                    .replace(")", "")
                    .replace("ROAM ", "")
                    .replace("ISSI ", "");
        }

        return null;
    }
}
