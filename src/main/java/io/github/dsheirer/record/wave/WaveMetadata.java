/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2019 Dennis Sheirer
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
 * *****************************************************************************
 */

package io.github.dsheirer.record.wave;

import com.google.common.base.Joiner;
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
import io.github.dsheirer.identifier.patch.PatchGroup;
import io.github.dsheirer.identifier.patch.PatchGroupIdentifier;
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.properties.SystemProperties;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WaveMetadata
{
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyyMMddHHmmssSSS");
    private static final Charset ISO_8859_1 = Charset.forName("ISO-8859-1");
    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private static final byte NULL_TERMINATOR = (byte)0x00;
    private static final String LIST_CHUNK_IDENTIFIER = "LIST";
    private static final String INFO_TYPE_IDENTIFIER = "INFO";
    private static final String ID3_CHUNK_IDENTIFIER = "ID3 ";
    private static final String ID3V2_IDENTIFIER = "ID3";
    private static final byte ID3V2_MAJOR_VERSION = (byte)0x4;
    private static final byte ID3V2_MINOR_VERSION = (byte)0x0;
    private static final byte ID3V2_FLAGS = (byte)0x0;
    private static final byte ID3V2_ISO_8859_1_ENCODING = (byte)0x0;

    private Map<WaveMetadataType, String> mMetadataMap = new HashMap<>();

    /**
     * Audio Wave File - Metadata Chunk.  Supports creating a map of metadata key:value pairs and
     * converting the metadata to WAV audio recording INFO/LIST and ID3 v2.4.0 metadata chunks.
     */
    public WaveMetadata()
    {
    }

    public ByteBuffer getID3Chunk()
    {
        ByteBuffer subChunks = getID3Frames();
        int subChunksLength = subChunks.capacity();

        int overallLength = subChunksLength + 18;

        //Pad the overall length to make it an even 32-bit boundary
        int padding = 0;

        if(overallLength % 4 != 0)
        {
            padding = 4 - (overallLength % 4);
        }

        ByteBuffer chunk = ByteBuffer.allocate(overallLength + padding).order(ByteOrder.LITTLE_ENDIAN);

        int chunkLength = subChunksLength + 10 + padding;

        chunk.put(ID3_CHUNK_IDENTIFIER.getBytes());
        chunk.putInt(chunkLength);
        chunk.put(ID3V2_IDENTIFIER.getBytes());
        chunk.put(ID3V2_MAJOR_VERSION);
        chunk.put(ID3V2_MINOR_VERSION);
        chunk.put(ID3V2_FLAGS);
        chunk.put(getID3EncodedLength(subChunksLength));

        chunk.put(subChunks);

        chunk.position(0);

        return chunk;
    }

    /**
     * Creates an ID3 v2.4.0 compatible frame set from the metadata
     */
    public ByteBuffer getID3Frames()
    {
        int length = 0;
        List<ByteBuffer> buffers = new ArrayList<>();

        //List the primary metadata tags first
        for(Map.Entry<WaveMetadataType, String> entry: mMetadataMap.entrySet())
        {
            if(entry.getKey().isPrimaryTag())
            {
                ByteBuffer buffer = getID3Frame(entry.getKey(), entry.getValue());

                if(buffer != null)
                {
                    length += buffer.capacity();
                    buffers.add(buffer);
                }
            }
        }

        for(Map.Entry<WaveMetadataType, String> entry: mMetadataMap.entrySet())
        {
            if(!entry.getKey().isPrimaryTag())
            {
                ByteBuffer buffer = getID3Frame(entry.getKey(), entry.getValue());

                if(buffer != null)
                {
                    length += buffer.capacity();
                    buffers.add(buffer);
                }
            }
        }

        ByteBuffer concatenatedBuffer = ByteBuffer.allocate(length);

        for(ByteBuffer buffer: buffers)
        {
            concatenatedBuffer.put(buffer);
        }

        concatenatedBuffer.position(0);

        return concatenatedBuffer;
    }

    /**
     * Creates an ID3 v2.4.0 compatible metadata frame
     * @param type of metadata
     * @param value of the metadata
     * @return frame byte buffer or null if the value is empty or null
     */
    public ByteBuffer getID3Frame(WaveMetadataType type, String value)
    {
        if(value != null && !value.isEmpty())
        {
            ByteBuffer encodedValue = ISO_8859_1.encode(value);

            //Length is 4 bytes for tag ID, 4 bytes for length, value, and null terminator
            int chunkLength = encodedValue.capacity() + 11;

            ByteBuffer buffer = ByteBuffer.allocate(chunkLength).order(ByteOrder.LITTLE_ENDIAN);
            buffer.put(type.getID3Tag().getBytes());
            buffer.put(getID3EncodedLength(encodedValue.capacity() + 1));
            buffer.put(ID3V2_FLAGS);
            buffer.put(ID3V2_FLAGS);
            buffer.put(ID3V2_ISO_8859_1_ENCODING);
            buffer.put(encodedValue);

            buffer.position(0);

            return buffer;
        }

        return null;
    }

    /**
     * Creates a wave LIST chunk from the metadata tags
     */
    public ByteBuffer getLISTChunk()
    {
        ByteBuffer metadataBuffer = getLISTSubChunks();
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
    private ByteBuffer getLISTSubChunks()
    {
        int length = INFO_TYPE_IDENTIFIER.length();

        List<ByteBuffer> buffers = new ArrayList<>();

        //Add the primary tags first
        for(Map.Entry<WaveMetadataType, String> entry: mMetadataMap.entrySet())
        {
            if(entry.getKey().isPrimaryTag())
            {
                ByteBuffer buffer = getLISTSubChunk(entry.getKey(), entry.getValue());
                length += buffer.capacity();
                buffers.add(buffer);
            }
        }

        for(Map.Entry<WaveMetadataType, String> entry: mMetadataMap.entrySet())
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
    private ByteBuffer getLISTSubChunk(WaveMetadataType type, String value)
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

    /**
     * Adds the metadata key:value pair to the metadata map.  Note: since this is a map, any existing
     * key:value will be overwritten.
     * @param type identifying the metadata type
     * @param value to associate with the metadata type
     */
    public void add(WaveMetadataType type, String value)
    {
        if(value != null)
        {
            mMetadataMap.put(type, value);
        }
    }

    /**
     * Creates a WAVE recording metadata chunk from the audio metadata argument
     * @param identifierCollection to create the wave metadata from
     * @return wave metadata instance
     */
    public static WaveMetadata createFrom(IdentifierCollection identifierCollection, AliasList aliasList)
    {
        WaveMetadata waveMetadata = new WaveMetadata();

        waveMetadata.add(WaveMetadataType.SOFTWARE, SystemProperties.getInstance().getApplicationName());
        waveMetadata.add(WaveMetadataType.DATE_CREATED, SDF.format(new Date(System.currentTimeMillis())));

        if(identifierCollection != null)
        {
            Identifier system = identifierCollection.getIdentifier(IdentifierClass.CONFIGURATION, Form.SYSTEM, Role.ANY);
            if(system instanceof SystemConfigurationIdentifier)
            {
                waveMetadata.add(WaveMetadataType.ARTIST_NAME, ((SystemConfigurationIdentifier)system).getValue());
            }

            Identifier site = identifierCollection.getIdentifier(IdentifierClass.CONFIGURATION, Form.SITE, Role.ANY);
            if(site instanceof SiteConfigurationIdentifier)
            {
                waveMetadata.add(WaveMetadataType.ALBUM_TITLE, ((SiteConfigurationIdentifier)site).getValue());
            }

            Identifier channel = identifierCollection.getIdentifier(IdentifierClass.CONFIGURATION, Form.CHANNEL_NAME, Role.ANY);
            if(channel instanceof ChannelNameConfigurationIdentifier)
            {
                waveMetadata.add(WaveMetadataType.TRACK_TITLE, ((ChannelNameConfigurationIdentifier)channel).getValue());
            }

            Identifier decoder = identifierCollection.getIdentifier(IdentifierClass.CONFIGURATION, Form.DECODER_TYPE,
                Role.ANY);
            if(decoder instanceof DecoderTypeConfigurationIdentifier)
            {
                waveMetadata.add(WaveMetadataType.COMMENTS, ((DecoderTypeConfigurationIdentifier)decoder).getValue().getDisplayString());
            }

            Identifier channelName = identifierCollection.getIdentifier(IdentifierClass.DECODER, Form.CHANNEL_NAME, Role.ANY);

            if(channelName instanceof DecoderLogicalChannelNameIdentifier)
            {
                waveMetadata.add(WaveMetadataType.CHANNEL_ID, ((DecoderLogicalChannelNameIdentifier)channelName).getValue());
            }

            Identifier frequency = identifierCollection.getIdentifier(IdentifierClass.CONFIGURATION,
                Form.CHANNEL_FREQUENCY, Role.ANY);

            if(frequency instanceof FrequencyConfigurationIdentifier)
            {
                waveMetadata.add(WaveMetadataType.CHANNEL_FREQUENCY,
                    String.valueOf(((FrequencyConfigurationIdentifier)frequency).getValue()));
            }

            for(Identifier identifier: identifierCollection.getIdentifiers(Form.TALKGROUP))
            {
                if(identifier instanceof TalkgroupIdentifier && identifier.getRole() == Role.FROM)
                {
                    waveMetadata.add(WaveMetadataType.TALKGROUP_PRIMARY_FROM,
                        String.valueOf(((TalkgroupIdentifier)identifier).getValue()));

                    List<Alias> aliases = aliasList.getAliases(identifier);

                    if(!aliases.isEmpty())
                    {
                        waveMetadata.add(WaveMetadataType.TALKGROUP_PRIMARY_FROM_ALIAS, Joiner.on(", ").skipNulls().join(aliases));
                        waveMetadata.add(WaveMetadataType.TALKGROUP_PRIMARY_FROM_ICON, aliases.get(0).getIconName());
                    }
                }
                else if(identifier instanceof TalkgroupIdentifier && identifier.getRole() == Role.TO)
                {
                    List<Alias> aliases = aliasList.getAliases(identifier);

                    if(!aliases.isEmpty())
                    {
                        waveMetadata.add(WaveMetadataType.TALKGROUP_PRIMARY_TO_ALIAS, Joiner.on(", ").skipNulls().join(aliases));
                        waveMetadata.add(WaveMetadataType.TALKGROUP_PRIMARY_TO_ICON, aliases.get(0).getIconName());
                    }
                }
                else if(identifier instanceof PatchGroupIdentifier)
                {
                    PatchGroup patchGroup = ((PatchGroupIdentifier)identifier).getValue();

                    StringBuilder sb = new StringBuilder();
                    sb.append("P:").append(patchGroup.getPatchGroup()).append(patchGroup.getPatchedGroupIdentifiers());
                    waveMetadata.add(WaveMetadataType.TALKGROUP_PRIMARY_TO, sb.toString());

                    List<Alias> aliases = aliasList.getAliases(identifier);

                    if(!aliases.isEmpty())
                    {
                        waveMetadata.add(WaveMetadataType.TALKGROUP_PRIMARY_TO_ALIAS, Joiner.on(", ").skipNulls().join(aliases));
                        waveMetadata.add(WaveMetadataType.TALKGROUP_PRIMARY_TO_ICON, aliases.get(0).getIconName());
                    }
                }
            }
        }

        return waveMetadata;
    }

    /**
     * Converts the integer length to an ID3 compatible length field where each byte only uses the 7 least significant
     * bits for a maximum of 28 bits used out of the 32-bit representation.
     * @param length
     * @return
     */
    public static byte[] getID3EncodedLength(int length)
    {
        byte[] value = new byte[4];
        value[0] = (byte)((length >> 21) & 0x7F);
        value[1] = (byte)((length >> 14) & 0x7F);
        value[2] = (byte)((length >> 7) & 0x7F);
        value[3] = (byte)(length & 0x7F);

        return value;
    }
}
