package io.github.dsheirer.module.decode.p25.message.tsbk;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;
import io.github.dsheirer.module.decode.p25.reference.Direction;

/**
 * P25 Inbound (ISP) TSBK Message
 */
public abstract class ISPMessage extends TSBKMessage
{
    /**
     * Constructs an inbound (ISP) TSBK from the binary message sequence.
     *
     * @param dataUnitID TSBK1/2/3
     * @param message binary sequence
     * @param nac decoded from the NID
     * @param timestamp for the message
     */
    public ISPMessage(DataUnitID dataUnitID, CorrectedBinaryMessage message, int nac, long timestamp)
    {
        super(dataUnitID, message, nac, timestamp);
    }

    @Override
    public Direction getDirection()
    {
        return Direction.INBOUND;
    }
}
