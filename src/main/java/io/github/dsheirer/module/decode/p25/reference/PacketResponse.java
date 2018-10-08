package io.github.dsheirer.module.decode.p25.reference;

/**
 * Response Packet responses
 */
public enum PacketResponse
{
    ALL_BLOCKS_SUCCESSFULLY_RECEIVED("ALL BLOCKS RECEIVED", 0x08),
    ILLEGAL_FORMAT("ILLEGAL FORMAT", 0x40),
    PACKET_CRC_FAIL("PACKET CRC-32 FAILED", 0x48),
    MEMORY_FULL("MEMORY FULL", 0x50),
    FSN_OUT_OF_SEQUENCE("FSN OUT OF SEQUENCE", 0x58),
    UNDELIVERABLE("UNDELIVERABLE", 0x60),
    MSN_OUT_OF_SEQUENCE("MSG OUT OF SEQUENCE", 0x68),
    UNAUTHORIZED_USER("UNAUTHORIZED USER", 0x70),
    SELECTIVE_RETRY("SELECTIVE RETRY", 0x80),
    UNKNOWN("UNKNOWN", 0xFF);

    private String mLabel;
    private int mMask;

    PacketResponse(String label, int mask)
    {
        mLabel = label;
        mMask = mask;
    }

    public static PacketResponse fromValue(int value)
    {
        for(PacketResponse packetResponse : values())
        {
            if((value & packetResponse.getMask()) == packetResponse.getMask())
            {
                return packetResponse;
            }
        }

        return UNKNOWN;
    }

    private int getMask()
    {
        return mMask;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }
}