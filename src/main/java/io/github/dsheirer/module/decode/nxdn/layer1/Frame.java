/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

package io.github.dsheirer.module.decode.nxdn.layer1;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.FragmentedIntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.nxdn.NXDNMessage;
import io.github.dsheirer.module.decode.nxdn.layer2.Direction;
import io.github.dsheirer.module.decode.nxdn.layer2.LICH;
import io.github.dsheirer.module.decode.nxdn.layer2.RFChannel;
import io.github.dsheirer.module.decode.nxdn.layer2.Scrambler;
import java.util.Collections;
import java.util.List;

/**
 * NXDN layer 1 frame.
 */
public class Frame extends NXDNMessage
{
    private static final FragmentedIntField LICH_FIELD = FragmentedIntField.of(0, 2, 4, 6, 8, 10, 12);
    private static final BinaryMessage SCRAMBLE_SEQUENCE_FULL = Scrambler.generate(348);
    private static final BinaryMessage SCRAMBLE_SEQUENCE_CONTROL_INBOUND = Scrambler.generate(268);
    private static final BinaryMessage SCRAMBLE_SEQUENCE_CONTROL_OUTBOUND = Scrambler.generate(340);
    private final LICH mLICH;

    /**
     * Constructs a frame.  Note: channel and direction are used as fallback values when we can't exactly match the
     * LICH value for this frame due to decoding errors.
     * @param message with frame bits
     * @param timestamp for the frame.
     * @param channel as tracked across the most recent 3 frames or UNKNOWN (default).
     * @param direction as tracked across the most recent 3 frames or OUTBOUND (default).
     */
    public Frame(CorrectedBinaryMessage message, long timestamp, RFChannel channel, Direction direction)
    {
        super(message, timestamp);
        mLICH = LICH.fromValue(getMessage().getInt(LICH_FIELD), channel, direction);

        //Descramble the message according to the RF Channel Type and repeater direction.  Control channel uses the
        // full scramble sequence truncated based on repeater direction.  All others use a full scramble sequence.
        if(mLICH.getRFChannel() == RFChannel.RCCH)
        {
            message.xor(mLICH.getDirection() == Direction.OUTBOUND ? SCRAMBLE_SEQUENCE_CONTROL_OUTBOUND :
                    SCRAMBLE_SEQUENCE_CONTROL_INBOUND);
        }
        else
        {
            message.xor(SCRAMBLE_SEQUENCE_FULL);
        }
    }

    /**
     * Link information channel (LICH)
     * @return LICH
     */
    public LICH getLICH()
    {
        return mLICH;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.emptyList();
    }
}
