package io.github.dsheirer.module.decode.p25.message.pdu;

import io.github.dsheirer.module.decode.p25.message.P25Message;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;

public class PacketSequenceMessage extends P25Message
{
    private PacketSequence mPacketSequence;

    public PacketSequenceMessage(PacketSequence packetSequence, int nac, long timestamp)
    {
        super(null, nac, timestamp);
        mPacketSequence = packetSequence;
    }

    public PacketSequence getPacketSequence()
    {
        return mPacketSequence;
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getPacketSequence().toString());
        return sb.toString();
    }

    @Override
    public DataUnitID getDUID()
    {
        return DataUnitID.PACKET_DATA_UNIT;
    }
}
