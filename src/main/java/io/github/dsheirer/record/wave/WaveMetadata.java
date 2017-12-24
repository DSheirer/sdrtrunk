package io.github.dsheirer.record.wave;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.PatchGroupAlias;
import io.github.dsheirer.channel.metadata.AliasedIdentifier;
import io.github.dsheirer.channel.metadata.Metadata;
import io.github.dsheirer.properties.SystemProperties;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WaveMetadata
{
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyyMMddHHmmssSSS");
    private static final byte NULL_TERMINATOR = (byte)0x00;
    private static final String LIST = "LIST";
    private static final String INFO = "INFO";
    private static final String COMMENT = "Recording created by sdrtrunk";

    private Map<WaveMetadataType, String> mMetadataMap = new HashMap<>();

    /**
     * Audio Wave File - Metadata Chunk.  Supports creating a map of metadata key:value pairs and
     * converting the metadata to a .wav audio recording INFO chunk.
     */
    public WaveMetadata()
    {
    }

    /**
     * Creates a wave LIST chunk from the metadata tags
     */
    public ByteBuffer getLISTChunk()
    {
        ByteBuffer metadataBuffer = getAllTags();
        int tagsLength = metadataBuffer.capacity();

        int overallLength = tagsLength + 8;

        //Pad the overall length to make it an event 32-bit boundary
        int padding = 0;

        if(overallLength % 4 != 0)
        {
            padding = 4 - (overallLength % 4);
        }

        ByteBuffer chunk = ByteBuffer.allocate(overallLength + padding).order(ByteOrder.LITTLE_ENDIAN);

        chunk.put(LIST.getBytes());
        chunk.putInt(tagsLength + padding);
        chunk.put(metadataBuffer);

        chunk.position(0);

        return chunk;
    }

    /**
     * Formats all metadata key:value pairs in the metadata map into a wave INFO compatible format
     */
    private ByteBuffer getAllTags()
    {
        int length = INFO.length();

        List<ByteBuffer> buffers = new ArrayList<>();

        //Do the non-custom metadata types first
        for(Map.Entry<WaveMetadataType, String> entry: mMetadataMap.entrySet())
        {
            if(!entry.getKey().isCustomType())
            {
                ByteBuffer buffer = getListMetadataChunk(entry.getKey(), entry.getValue());
                length += buffer.capacity();
                buffers.add(buffer);
            }
        }

        //Do the custom metadata types second
        for(Map.Entry<WaveMetadataType, String> entry: mMetadataMap.entrySet())
        {
            if(entry.getKey().isCustomType())
            {
                ByteBuffer buffer = getListMetadataChunk(entry.getKey(), entry.getValue());
                length += buffer.capacity();
                buffers.add(buffer);
            }
        }

        ByteBuffer joinedBuffer = ByteBuffer.allocate(length);

        joinedBuffer.put(INFO.getBytes());

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
    private ByteBuffer getListMetadataChunk(WaveMetadataType type, String value)
    {
        //Length is 4 bytes for tag ID, 4 bytes for length, value, and null terminator
        int chunkLength = value.length() + 9;

//        //Ensure length is an even multiple of 4 bytes/32-bit word
//        if(chunkLength % 4 != 0)
//        {
//            chunkLength += (4 - (chunkLength % 4));
//        }
//
        ByteBuffer buffer = ByteBuffer.allocate(chunkLength).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(type.getTag().getBytes());
        buffer.putInt(value.length() + 1);
        buffer.put(value.getBytes());
        buffer.put(NULL_TERMINATOR);

        buffer.position(0);

        return buffer;
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
     * @param audioMetadata to create the wave metadata from
     * @return wave metadata instance
     */
    public static WaveMetadata createFrom(Metadata audioMetadata)
    {
        WaveMetadata waveMetadata = new WaveMetadata();

        waveMetadata.add(WaveMetadataType.GENRE, "TEST GENRE");
        waveMetadata.add(WaveMetadataType.PRODUCT, SystemProperties.getInstance().getApplicationName());
        waveMetadata.add(WaveMetadataType.COMMENTS, COMMENT);
        waveMetadata.add(WaveMetadataType.DATE_CREATED, SDF.format(new Date(audioMetadata.getTimestamp())));
        waveMetadata.add(WaveMetadataType.ARTIST_NAME, audioMetadata.getChannelConfigurationSystem());
        waveMetadata.add(WaveMetadataType.ALBUM_TITLE, audioMetadata.getChannelConfigurationSite());
        waveMetadata.add(WaveMetadataType.TRACK_TITLE, audioMetadata.getChannelConfigurationName());
        waveMetadata.add(WaveMetadataType.SOURCE_FORM, audioMetadata.getPrimaryDecoderType().getDisplayString());
//        waveMetadata.add(WaveMetadataType.ALIAS_LIST_NAME, "not implemented yet");
        waveMetadata.add(WaveMetadataType.CHANNEL_ID, audioMetadata.getChannelFrequencyLabel());
        waveMetadata.add(WaveMetadataType.CHANNEL_FREQUENCY, String.valueOf(audioMetadata.getChannelFrequency()));

        AliasedIdentifier networkID1 = audioMetadata.getNetworkID1();

        if(networkID1 != null)
        {
            waveMetadata.add(WaveMetadataType.NETWORK_ID_1, networkID1.getIdentifier());
        }

        AliasedIdentifier networkID2 = audioMetadata.getNetworkID2();

        if(networkID2 != null)
        {
            waveMetadata.add(WaveMetadataType.NETWORK_ID_2, networkID2.getIdentifier());
        }

        AliasedIdentifier primaryFrom = audioMetadata.getPrimaryAddressFrom();

        if(primaryFrom != null)
        {
            waveMetadata.add(WaveMetadataType.TALKGROUP_PRIMARY_FROM, primaryFrom.getIdentifier());

            Alias alias = primaryFrom.getAlias();

            if(alias != null)
            {
                waveMetadata.add(WaveMetadataType.TALKGROUP_PRIMARY_FROM_ALIAS, alias.getName());
                waveMetadata.add(WaveMetadataType.TALKGROUP_PRIMARY_FROM_ICON, alias.getIconName());
            }
        }

        AliasedIdentifier primaryTo = audioMetadata.getPrimaryAddressTo();

        if(primaryTo != null)
        {
            waveMetadata.add(WaveMetadataType.TALKGROUP_PRIMARY_TO, primaryTo.getIdentifier());

            Alias alias = primaryTo.getAlias();

            if(alias != null)
            {
                waveMetadata.add(WaveMetadataType.TALKGROUP_PRIMARY_TO_ALIAS, alias.getName());
                waveMetadata.add(WaveMetadataType.TALKGROUP_PRIMARY_TO_ICON, alias.getIconName());

                if(alias instanceof PatchGroupAlias)
                {
                    PatchGroupAlias patchGroupAlias = (PatchGroupAlias)alias;

                    List<Alias> patchedAliases = patchGroupAlias.getPatchedAliases();

                    if(patchedAliases.size() >= 1)
                    {
                        Alias patch = patchedAliases.get(0);
                        waveMetadata.add(WaveMetadataType.TALKGROUP_PRIMARY_PATCHED_1, patch.getName());
                    }
                    if(patchedAliases.size() >= 2)
                    {
                        Alias patch = patchedAliases.get(1);
                        waveMetadata.add(WaveMetadataType.TALKGROUP_PRIMARY_PATCHED_2, patch.getName());
                    }
                    if(patchedAliases.size() >= 3)
                    {
                        Alias patch = patchedAliases.get(2);
                        waveMetadata.add(WaveMetadataType.TALKGROUP_PRIMARY_PATCHED_3, patch.getName());
                    }
                    if(patchedAliases.size() >= 4)
                    {
                        Alias patch = patchedAliases.get(3);
                        waveMetadata.add(WaveMetadataType.TALKGROUP_PRIMARY_PATCHED_4, patch.getName());
                    }
                    if(patchedAliases.size() >= 5)
                    {
                        Alias patch = patchedAliases.get(4);
                        waveMetadata.add(WaveMetadataType.TALKGROUP_PRIMARY_PATCHED_5, patch.getName());
                    }
                }
            }
        }

        AliasedIdentifier secondaryFrom = audioMetadata.getSecondaryAddressFrom();

        if(secondaryFrom != null)
        {
            waveMetadata.add(WaveMetadataType.TALKGROUP_SECONDARY_FROM, secondaryFrom.getIdentifier());

            Alias alias = secondaryFrom.getAlias();

            if(alias != null)
            {
                waveMetadata.add(WaveMetadataType.TALKGROUP_SECONDARY_FROM_ALIAS, alias.getName());
                waveMetadata.add(WaveMetadataType.TALKGROUP_SECONDARY_FROM_ICON, alias.getIconName());
            }
        }

        AliasedIdentifier secondaryTo = audioMetadata.getSecondaryAddressTo();

        if(secondaryTo != null)
        {
            waveMetadata.add(WaveMetadataType.TALKGROUP_SECONDARY_TO, secondaryTo.getIdentifier());

            Alias alias = secondaryTo.getAlias();

            if(alias != null)
            {
                waveMetadata.add(WaveMetadataType.TALKGROUP_SECONDARY_TO_ALIAS, alias.getName());
                waveMetadata.add(WaveMetadataType.TALKGROUP_SECONDARY_TO_ICON, alias.getIconName());
            }
        }

        return waveMetadata;
    }
}
