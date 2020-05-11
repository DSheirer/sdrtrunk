/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2020 Zhenyu Mao
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
package io.github.dsheirer.module.decode.dmr.message.data;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.BitSetFullException;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.message.DMRMessage;
import io.github.dsheirer.module.decode.dmr.message.data.lc.ShortLCMessage;
import io.github.dsheirer.protocol.Protocol;

import java.util.ArrayList;
import java.util.List;

import static io.github.dsheirer.edac.BPTC_196_96.bptc_196_96_check_and_repair;
import static io.github.dsheirer.edac.BPTC_196_96.bptc_196_96_extractdata;
import static io.github.dsheirer.edac.BPTC_196_96.bptc_deinterleave;

public class DataMessage extends DMRMessage
{
    private SlotType mSlotType;
    protected CorrectedBinaryMessage dataMessage;
    /**
     * DMR Data Message.
     *
     * @param syncPattern either BASE_STATION_DATA or MOBILE_STATION_DATA
     * @param message containing 288-bit DMR message with preliminary bit corrections indicated.
     */
    public DataMessage(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, long timestamp, int timeslot)
    {
        super(syncPattern, message, timestamp, timeslot);
    }

    /**
     * Slot Type identifies the color code and data type for this data message
     */
    public SlotType getSlotType()
    {
        if(mSlotType == null)
        {
            mSlotType = new SlotType(getTransmittedMessage());
        }

        return mSlotType;
    }
    protected CorrectedBinaryMessage getMessageBody(CorrectedBinaryMessage _message)
    {
        CorrectedBinaryMessage bm1 = new CorrectedBinaryMessage(196);
        try {
            for(int i = 24; i < 122; i++) {
                bm1.add(_message.get(i));
            }
            for(int i = 190; i < 190 + 98; i++) {
                bm1.add(_message.get(i));
            }
        } catch (BitSetFullException ex) {

        }
        CorrectedBinaryMessage message = bptc_deinterleave(bm1);
        if(bptc_196_96_check_and_repair(message)) {
            message = bptc_196_96_extractdata(message);
        } else {
            return null;
        }
        return message;
    }
    @Override
    public String toString()
    {
        return null;
    }

    @Override
    public boolean isValid()
    {
        return true;
    }

    @Override
    public List<Identifier> getIdentifiers() {
        return new ArrayList<Identifier>();
    }
}
