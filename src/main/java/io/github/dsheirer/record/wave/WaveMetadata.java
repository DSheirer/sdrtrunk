package io.github.dsheirer.record.wave;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.identifier.IdentifierCollection;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

    public static WaveMetadata createFrom(IdentifierCollection identifierCollection, AliasList aliasList)
    {

        return null;
    }

//    /**
//     * Creates a WAVE recording metadata chunk from the audio metadata argument
//     * @param audioMetadata to create the wave metadata from
//     * @return wave metadata instance
//     */
//    public static WaveMetadata createFrom(Metadata audioMetadata)
//    {
//        WaveMetadata waveMetadata = new WaveMetadata();
//
//        waveMetadata.add(WaveMetadataType.SOFTWARE, SystemProperties.getInstance().getApplicationName());
//        waveMetadata.add(WaveMetadataType.DATE_CREATED, SDF.format(new Date(audioMetadata.getTimestamp())));
//        waveMetadata.add(WaveMetadataType.ARTIST_NAME, audioMetadata.getChannelConfigurationSystem());
//        waveMetadata.add(WaveMetadataType.ALBUM_TITLE, audioMetadata.getChannelConfigurationSite());
//        waveMetadata.add(WaveMetadataType.TRACK_TITLE, audioMetadata.getChannelConfigurationName());
//        waveMetadata.add(WaveMetadataType.COMMENTS, audioMetadata.getPrimaryDecoderType().getDisplayString());
//        waveMetadata.add(WaveMetadataType.CHANNEL_ID, audioMetadata.getChannelFrequencyLabel());
//        waveMetadata.add(WaveMetadataType.CHANNEL_FREQUENCY, String.valueOf(audioMetadata.getChannelFrequency()));
//
//        AliasedIdentifier networkID1 = audioMetadata.getNetworkID1();
//
//        if(networkID1 != null)
//        {
//            waveMetadata.add(WaveMetadataType.NETWORK_ID_1, networkID1.getIdentifier());
//        }
//
//        AliasedIdentifier networkID2 = audioMetadata.getNetworkID2();
//
//        if(networkID2 != null)
//        {
//            waveMetadata.add(WaveMetadataType.NETWORK_ID_2, networkID2.getIdentifier());
//        }
//
//        AliasedIdentifier primaryFrom = audioMetadata.getPrimaryAddressFrom();
//
//        if(primaryFrom != null)
//        {
//            waveMetadata.add(WaveMetadataType.TALKGROUP_PRIMARY_FROM, primaryFrom.getIdentifier());
//
//            Alias alias = primaryFrom.getAlias();
//
//            if(alias != null)
//            {
//                waveMetadata.add(WaveMetadataType.TALKGROUP_PRIMARY_FROM_ALIAS, alias.getName());
//                waveMetadata.add(WaveMetadataType.TALKGROUP_PRIMARY_FROM_ICON, alias.getIconName());
//            }
//        }
//
//        AliasedIdentifier primaryTo = audioMetadata.getPrimaryAddressTo();
//
//        if(primaryTo != null)
//        {
//            waveMetadata.add(WaveMetadataType.TALKGROUP_PRIMARY_TO, primaryTo.getIdentifier());
//
//            Alias alias = primaryTo.getAlias();
//
//            if(alias != null)
//            {
//                waveMetadata.add(WaveMetadataType.TALKGROUP_PRIMARY_TO_ALIAS, alias.getName());
//                waveMetadata.add(WaveMetadataType.TALKGROUP_PRIMARY_TO_ICON, alias.getIconName());
//
//                if(alias instanceof PatchGroupAlias)
//                {
//                    PatchGroupAlias patchGroupAlias = (PatchGroupAlias)alias;
//
//                    List<Alias> patchedAliases = patchGroupAlias.getPatchedAliases();
//
//                    if(patchedAliases.size() >= 1)
//                    {
//                        Alias patch = patchedAliases.get(0);
//                        waveMetadata.add(WaveMetadataType.TALKGROUP_PRIMARY_PATCHED_1, patch.getName());
//                    }
//                    if(patchedAliases.size() >= 2)
//                    {
//                        Alias patch = patchedAliases.get(1);
//                        waveMetadata.add(WaveMetadataType.TALKGROUP_PRIMARY_PATCHED_2, patch.getName());
//                    }
//                    if(patchedAliases.size() >= 3)
//                    {
//                        Alias patch = patchedAliases.get(2);
//                        waveMetadata.add(WaveMetadataType.TALKGROUP_PRIMARY_PATCHED_3, patch.getName());
//                    }
//                    if(patchedAliases.size() >= 4)
//                    {
//                        Alias patch = patchedAliases.get(3);
//                        waveMetadata.add(WaveMetadataType.TALKGROUP_PRIMARY_PATCHED_4, patch.getName());
//                    }
//                    if(patchedAliases.size() >= 5)
//                    {
//                        Alias patch = patchedAliases.get(4);
//                        waveMetadata.add(WaveMetadataType.TALKGROUP_PRIMARY_PATCHED_5, patch.getName());
//                    }
//                }
//            }
//        }
//
//        AliasedIdentifier secondaryFrom = audioMetadata.getSecondaryAddressFrom();
//
//        if(secondaryFrom != null)
//        {
//            waveMetadata.add(WaveMetadataType.TALKGROUP_SECONDARY_FROM, secondaryFrom.getIdentifier());
//
//            Alias alias = secondaryFrom.getAlias();
//
//            if(alias != null)
//            {
//                waveMetadata.add(WaveMetadataType.TALKGROUP_SECONDARY_FROM_ALIAS, alias.getName());
//                waveMetadata.add(WaveMetadataType.TALKGROUP_SECONDARY_FROM_ICON, alias.getIconName());
//            }
//        }
//
//        AliasedIdentifier secondaryTo = audioMetadata.getSecondaryAddressTo();
//
//        if(secondaryTo != null)
//        {
//            waveMetadata.add(WaveMetadataType.TALKGROUP_SECONDARY_TO, secondaryTo.getIdentifier());
//
//            Alias alias = secondaryTo.getAlias();
//
//            if(alias != null)
//            {
//                waveMetadata.add(WaveMetadataType.TALKGROUP_SECONDARY_TO_ALIAS, alias.getName());
//                waveMetadata.add(WaveMetadataType.TALKGROUP_SECONDARY_TO_ICON, alias.getIconName());
//            }
//        }
//
//        return waveMetadata;
//    }

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
