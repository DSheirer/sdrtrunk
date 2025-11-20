package io.github.dsheirer.module.decode.nxdn.message;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;

import java.util.Collections;
import java.util.List;

public class DataCallBlock extends NXDNMessage
{
    private static final IntField PACKET_FRAME_NUMBER = IntField.length4(OCTET_1);
    private static final IntField BLOCK_NUMBER = IntField.length4(OCTET_1 + 4);


    /**
     * Constructs an instance
     *
     * @param message   with binary data
     * @param timestamp for the message
     */
    public DataCallBlock(CorrectedBinaryMessage message, long timestamp)
    {
        super(message, timestamp);
    }

    @Override
    public NXDNMessageType getMessageType()
    {
        return NXDNMessageType.TC_DATA_CALL_BLOCK;
    }

    @Override
    public String toString()
    {
        return getMessageType() + " PACKET FRAME:" + getPacketFrameNumber() + " BLOCK_NUMBER:" + getBlockNumber();
    }

    /**
     * Packet number
     */
    public int getPacketFrameNumber()
    {
        return getMessage().getInt(PACKET_FRAME_NUMBER);
    }

    /**
     * Block number
     */
    public int getBlockNumber()
    {
        return getMessage().getInt(BLOCK_NUMBER);
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.emptyList();
    }
}
