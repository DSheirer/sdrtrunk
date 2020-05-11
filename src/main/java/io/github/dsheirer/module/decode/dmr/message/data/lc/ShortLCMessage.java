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
package io.github.dsheirer.module.decode.dmr.message.data.lc;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.BitSetFullException;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.BPTC_17_12_3;
import io.github.dsheirer.edac.CRCDMR;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.message.Message;
import io.github.dsheirer.protocol.Protocol;

import java.util.ArrayList;
import java.util.List;

public class ShortLCMessage extends Message {
    private BinaryMessage rawMessage;
    private int slco = -1;
    private boolean isValid = false;
    private int payLoad = -1;
    public ShortLCMessage() {
        rawMessage = new BinaryMessage(17 * 4);
    }
    public int getPayLoad() {
        return payLoad;
    }
    public int getSlco() {
        return slco;
    }

    public void appendMsg(BinaryMessage bm) throws BitSetFullException {
        for(int i = 0; i< 17; i++) {
            rawMessage.add(bm.get(i));
        }
    }
    public void finalizeMessage() {
        CorrectedBinaryMessage bm = new CorrectedBinaryMessage(68);
        int i, src;
        for (i = 0; i < 67; i++) {
            src = (i * 4) % 67;
            bm.set(i, rawMessage.get(src));
        }
        boolean check = BPTC_17_12_3.decode17123(bm, 0);
        check &= BPTC_17_12_3.decode17123(bm, 17);
        check &= BPTC_17_12_3.decode17123(bm, 34);

        for (i = 17; i < 29; i++) {
            bm.set(i-5, bm.get(i));
        }
        for (i = 34; i < 46; i++) {
            bm.set(i-10, bm.get(i));
        }
        if (CRCDMR.crc8(bm, 36) != 0) {
            check = false;
        }
        slco = bm.getInt(0,3); // 0 1 2 3 is SLCO
        payLoad = bm.getInt(4, 27);
        isValid = check;
    }
    @Override
    public String toString() {
        return "ShortLC: " + getPayLoad();
    }

    @Override
    public boolean isValid() {
        return isValid;
    }

    @Override
    public Protocol getProtocol() {
        return Protocol.DMR;
    }

    @Override
    public List<Identifier> getIdentifiers() {
        return new ArrayList<Identifier>();
    }
}
