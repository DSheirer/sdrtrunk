package io.github.dsheirer.module.decode.p25.message.pdu;

import io.github.dsheirer.module.decode.p25.message.P25Message;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;

public class PacketSequenceMessage extends P25Message
{
    private PacketSequence mPacketSequence;

    public PacketSequenceMessage(PacketSequence packetSequence)
    {
        super(null, DataUnitID.PACKET_HEADER_DATA_UNIT, null);
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
}
