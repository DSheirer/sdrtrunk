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

package io.github.dsheirer.record.wave;

import com.google.common.base.Joiner;
import com.mpatric.mp3agic.ID3v24Tag;
import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.configuration.ChannelNameConfigurationIdentifier;
import io.github.dsheirer.identifier.configuration.DecoderTypeConfigurationIdentifier;
import io.github.dsheirer.identifier.configuration.FrequencyConfigurationIdentifier;
import io.github.dsheirer.identifier.configuration.SiteConfigurationIdentifier;
import io.github.dsheirer.identifier.configuration.SystemConfigurationIdentifier;
import io.github.dsheirer.identifier.decoder.DecoderLogicalChannelNameIdentifier;
import io.github.dsheirer.properties.SystemProperties;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AudioMetadataUtils
{
    private final static Logger mLog = LoggerFactory.getLogger(AudioMetadataUtils.class);

    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private static final SimpleDateFormat YEAR_SDF = new SimpleDateFormat("yyyy");
    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private static final byte NULL_TERMINATOR = (byte)0x00;
    private static final String LIST_CHUNK_IDENTIFIER = "LIST";
    private static final String ID3_CHUNK_IDENTIFIER = "id3 ";
    private static final String INFO_TYPE_IDENTIFIER = "INFO";
    private static final String GENRE_SCANNER_AUDIO = "Scanner Audio";
    private static final String COMMENT_SEPARATOR = ";";

    /**
     * Audio Metadata Utilities.  Supports creating a map of metadata key:value pairs and converting the metadata to
     * WAV (RIFF LIST) and MP3 (ID3) metadata formats.
     */
    private AudioMetadataUtils()
    {
        //No constructor - all static utils
    }

    /**
     * Creates a metadata map from the audio metadata argument
     * @param identifierCollection to create the metadata from
     * @return map of metadata tags to values
     */
    public static Map<AudioMetadata, String> getMetadataMap(IdentifierCollection identifierCollection, AliasList aliasList)
    {
        Map<AudioMetadata, String> audioMetadata = new EnumMap<>(AudioMetadata.class);
        StringBuilder comments = new StringBuilder();
        audioMetadata.put(AudioMetadata.COMPOSER, SystemProperties.getInstance().getApplicationName());
        String dateCreated = SDF.format(new Date(System.currentTimeMillis()));
        audioMetadata.put(AudioMetadata.DATE_CREATED, dateCreated);
        comments.append("Date:").append(dateCreated).append(COMMENT_SEPARATOR);
        audioMetadata.put(AudioMetadata.YEAR, YEAR_SDF.format(new Date(System.currentTimeMillis())));
        audioMetadata.put(AudioMetadata.GENRE, GENRE_SCANNER_AUDIO);

        if(identifierCollection != null)
        {
            StringBuilder sb;

            Identifier to = identifierCollection.getToIdentifier();
            if(to != null)
            {
                sb = new StringBuilder();
                sb.append(to.toString().replace("ISSI ", ""));

                List<Alias> toAliases = aliasList.getAliases(to);

                if(!toAliases.isEmpty())
                {
                    sb.append("\"").append(Joiner.on("\",\"").join(toAliases)).append("\"");
                }

                audioMetadata.put(AudioMetadata.TRACK_TITLE, sb.toString());
            }

            Identifier from = identifierCollection.getFromIdentifier();
            if(from != null)
            {
                sb = new StringBuilder();
                sb.append(from.toString().replace("ISSI ", "").replace("ROAM ", ""));

                List<Alias> fromAliases = aliasList.getAliases(from);

                for(Alias alias: fromAliases)
                {
                    sb.append(" ").append(alias.toString());
                }

                audioMetadata.put(AudioMetadata.ARTIST_NAME, sb.toString());
            }
            
            sb = null;

            Identifier system = identifierCollection.getIdentifier(IdentifierClass.CONFIGURATION, Form.SYSTEM, Role.ANY);
            if(system instanceof SystemConfigurationIdentifier)
            {
                String value = ((SystemConfigurationIdentifier)system).getValue();
                audioMetadata.put(AudioMetadata.GROUPING, value);
                comments.append("System:").append(value).append(COMMENT_SEPARATOR);
            }

            Identifier site = identifierCollection.getIdentifier(IdentifierClass.CONFIGURATION, Form.SITE, Role.ANY);
            if(site instanceof SiteConfigurationIdentifier)
            {
                comments.append("Site:").append(((SiteConfigurationIdentifier)site).getValue()).append(COMMENT_SEPARATOR);
            }

            Identifier channel = identifierCollection.getIdentifier(IdentifierClass.CONFIGURATION, Form.CHANNEL, Role.ANY);
            if(channel instanceof ChannelNameConfigurationIdentifier)
            {
                String value = ((ChannelNameConfigurationIdentifier)channel).getValue();
                audioMetadata.put(AudioMetadata.ALBUM_TITLE, value);
                comments.append("Name:").append(value).append(COMMENT_SEPARATOR);
            }

            Identifier decoder = identifierCollection.getIdentifier(IdentifierClass.CONFIGURATION, Form.DECODER_TYPE,
                Role.ANY);
            if(decoder instanceof DecoderTypeConfigurationIdentifier)
            {
                comments.append("Decoder:")
                    .append(((DecoderTypeConfigurationIdentifier)decoder).getValue().getDisplayString())
                    .append(COMMENT_SEPARATOR);
            }

            Identifier channelName = identifierCollection.getIdentifier(IdentifierClass.DECODER, Form.CHANNEL_NAME, Role.BROADCAST);

            if(channelName instanceof DecoderLogicalChannelNameIdentifier)
            {
                comments.append("Channel:").append(((DecoderLogicalChannelNameIdentifier)channelName).getValue())
                    .append(COMMENT_SEPARATOR);
            }

            Identifier frequency = identifierCollection.getIdentifier(IdentifierClass.CONFIGURATION,
                Form.CHANNEL_FREQUENCY, Role.ANY);

            if(frequency instanceof FrequencyConfigurationIdentifier)
            {
                comments.append("Frequency:").append(((FrequencyConfigurationIdentifier)frequency).getValue()).append(COMMENT_SEPARATOR);
            }

        }

        audioMetadata.put(AudioMetadata.COMMENTS, comments.toString());

        return audioMetadata;
    }


    /**
     * Creates an ID3 V2.4 metadata chunk suitable for embedding in an .mp3 audio file
     * @param metadataMap of tags and values
     * @return byte buffer of metadata chunk
     */
    public static byte[] getMP3ID3(Map<AudioMetadata,String> metadataMap)
    {
        ID3v24Tag tag = new ID3v24Tag();

        for(Map.Entry<AudioMetadata, String> entry : metadataMap.entrySet())
        {
            switch(entry.getKey())
            {
                case ALBUM_TITLE:
                    tag.setAlbum(entry.getValue());
                    break;
                case ARTIST_NAME:
                    tag.setArtist(entry.getValue());
                    break;
                case COMMENTS:
                    tag.setComment(entry.getValue());
                    break;
                case COMPOSER:
                    tag.setComposer(entry.getValue());
                    break;
                case DATE_CREATED:
                    tag.setDate(entry.getValue());
                    break;
                case GENRE:
                    tag.setGenreDescription(entry.getValue());
                    break;
                case GROUPING:
                    tag.setGrouping(entry.getValue());
                    break;
                case TRACK_TITLE:
                    tag.setTitle(entry.getValue());
                    break;
                case YEAR:
                    tag.setYear(entry.getValue());
                    break;
            }
        }

        try
        {
            return tag.toBytes();
        }
        catch(Exception e)
        {
            mLog.error("Error creating MP3 ID3 tag bytes", e);
        }

        return new byte[0];
    }

    /**
     * Wraps the contents argument in a wave metadata id3 chunk tag
     * @param contents of the ID3 metadata
     * @return id3 header/length wrapped ID3 block
     */
    public static ByteBuffer getID3Chunk(byte[] contents)
    {
        int length = contents.length;

        //Pad the block out to a multiple of 4
        if(length % 4 != 0)
        {
            length += (4 - (length % 4));
        }

        ByteBuffer chunk = ByteBuffer.allocate(length + 8).order(ByteOrder.LITTLE_ENDIAN);
        chunk.put(ID3_CHUNK_IDENTIFIER.getBytes());
        chunk.putInt(length);
        chunk.put(contents);

        return chunk;
    }

    /**
     * Creates a wave LIST chunk from the metadata tags
     */
    public static ByteBuffer getLISTChunk(Map<AudioMetadata,String> metadataMap)
    {
        ByteBuffer metadataBuffer = getLISTSubChunks(metadataMap);
        int tagsLength = metadataBuffer.capacity();

        int overallLength = tagsLength + 8;

        //Pad the overall length to make it an event 32-bit boundary
        int padding = 0;

        if(overallLength % 4 != 0)
        {
            padding = 4 - (overallLength % 4);
        }

        ByteBuffer chunk = ByteBuffer.allocate(overallLength + padding).order(ByteOrder.LITTLE_ENDIAN);

        chunk.put(LIST_CHUNK_IDENTIFIER.getBytes());
        chunk.putInt(tagsLength + padding);
        chunk.put(metadataBuffer);

        chunk.position(0);

        return chunk;
    }

    /**
     * Formats all metadata key:value pairs in the metadata map into a wave INFO compatible format
     */
    private static ByteBuffer getLISTSubChunks(Map<AudioMetadata,String> metadataMap)
    {
        int length = INFO_TYPE_IDENTIFIER.length();

        List<ByteBuffer> buffers = new ArrayList<>();

        //Add the primary tags first
        for(Map.Entry<AudioMetadata, String> entry: metadataMap.entrySet())
        {
            if(entry.getKey().isPrimaryTag())
            {
                ByteBuffer buffer = getLISTSubChunk(entry.getKey(), entry.getValue());
                length += buffer.capacity();
                buffers.add(buffer);
            }
        }

        for(Map.Entry<AudioMetadata, String> entry: metadataMap.entrySet())
        {
            if(!entry.getKey().isPrimaryTag())
            {
                ByteBuffer buffer = getLISTSubChunk(entry.getKey(), entry.getValue());
                length += buffer.capacity();
                buffers.add(buffer);
            }
        }

        ByteBuffer joinedBuffer = ByteBuffer.allocate(length);

        joinedBuffer.put(INFO_TYPE_IDENTIFIER.getBytes());

        for(ByteBuffer buffer: buffers)
        {
            joinedBuffer.put(buffer);
        }

        joinedBuffer.position(0);

        return joinedBuffer;
    }

    /**
     * Converts the metadata type and value into a Wave LIST compatible byte array.
     *
     * @param type of metadata
     * @param value for the metadata
     * @return metadata and value formatted for wave chunk
     */
    private static ByteBuffer getLISTSubChunk(AudioMetadata type, String value)
    {
        if(value != null && !value.isEmpty())
        {
            //Length is 4 bytes for tag ID, 4 bytes for length, value, and null terminator
            ByteBuffer encodedValue = UTF_8.encode(value);
            int chunkLength = encodedValue.capacity() + 9;

            ByteBuffer buffer = ByteBuffer.allocate(chunkLength).order(ByteOrder.LITTLE_ENDIAN);
            buffer.put(type.getLISTTag().getBytes());
            buffer.putInt(encodedValue.capacity() + 1);
            buffer.put(encodedValue);
            buffer.put(NULL_TERMINATOR);

            buffer.position(0);

            return buffer;
        }

        return null;
    }
}
