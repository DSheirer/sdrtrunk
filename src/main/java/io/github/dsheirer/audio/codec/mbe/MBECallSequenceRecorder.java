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

package io.github.dsheirer.audio.codec.mbe;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.IMessageListener;
import io.github.dsheirer.module.Module;
import io.github.dsheirer.preference.TimestampFormat;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

/**
 * Records MBE audio frame call sequences and metadata to a JSON format recording file
 */
public abstract class MBECallSequenceRecorder extends Module implements IMessageListener, Listener<IMessage>
{
    private final static Logger mLog = LoggerFactory.getLogger(MBECallSequenceRecorder.class);

    protected static final String CALL_TYPE_GROUP = "GROUP";
    protected static final String CALL_TYPE_INDIVIDUAL = "INDIVIDUAL";
    protected static final String CALL_TYPE_TELEPHONE_INTERCONNECT = "TELEPHONE INTERCONNECT";
    protected UserPreferences mUserPreferences;
    protected long mChannelFrequency;
    protected String mSystem;
    protected String mSite;
    private int mCallNumber = 1;

    /**
     * Constructs an instance
     * @param userPreferences to obtain recording directory
     * @param channelFrequency for the channel to record
     */
    public MBECallSequenceRecorder(UserPreferences userPreferences, long channelFrequency, String system, String site)
    {
        mUserPreferences = userPreferences;
        mChannelFrequency = channelFrequency;
        mSystem = system;
        mSite = site;
    }

    @Override
    public void reset()
    {
    }

    @Override
    public void start()
    {
    }

    /**
     * Writes an MBE call sequence recording to the recording directory
     * @param sequence to write
     */
    protected void writeCallSequence(MBECallSequence sequence)
    {
        writeCallSequence(sequence, null);
    }

    /**
     * Writes an MBE call sequence recording to the recording directory.
     *
     * @param optionalChannelTag to include in the filename
     * @param sequence containing voice frames
     */
    protected void writeCallSequence(MBECallSequence sequence, String optionalChannelTag)
    {
        if(sequence != null && sequence.hasAudio())
        {
            sequence.setSystem(mSystem);
            sequence.setSite(mSite);

            StringBuilder sb = new StringBuilder();
            sb.append(TimestampFormat.TIMESTAMP_COMPACT.getFormatter().format(new Date(System.currentTimeMillis())));
            sb.append("_").append(mChannelFrequency);
            sb.append("_").append(mCallNumber++);

            if(mCallNumber < 1)
            {
                mCallNumber = 1;
            }

            if(optionalChannelTag != null)
            {
                sb.append("_").append(optionalChannelTag);
            }

            if(sequence.getToIdentifier() != null)
            {
                sb.append("_").append(sequence.getToIdentifier().replace(":", ""));
            }
            if(sequence.getFromIdentifier() != null)
            {
                sb.append("_").append(sequence.getFromIdentifier().replace(":", ""));
            }

            if(sequence.isEncrypted())
            {
                sb.append("_encrypted");
            }

            sb.append(".mbe");

            Path recordingDirectory = mUserPreferences.getDirectoryPreference().getDirectoryRecording();
            Path filePath = recordingDirectory.resolve(sb.toString());

            try
            {
                OutputStream outputStream = Files.newOutputStream(filePath);
                ObjectMapper mapper = new ObjectMapper();
                mapper.writerWithDefaultPrettyPrinter().writeValue(outputStream, sequence);
                outputStream.close();
            }
            catch(IOException ioe)
            {
                mLog.error("Couldn't write MBE call sequence to path [" + filePath.toString() + "]", ioe);
            }
        }
    }

    /**
     * Implementation of IMessageListener interface
     */
    @Override
    public Listener<IMessage> getMessageListener()
    {
        return this;
    }
}
