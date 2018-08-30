/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.module.decode.p25.message.tsbk;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.edac.CRC;
import io.github.dsheirer.module.decode.p25.message.P25Message;
import io.github.dsheirer.module.decode.p25.message.tsbk.motorola.MotorolaOpcode;
import io.github.dsheirer.module.decode.p25.message.tsbk.vendor.VendorOpcode;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;
import io.github.dsheirer.module.decode.p25.reference.Opcode;
import io.github.dsheirer.module.decode.p25.reference.Vendor;

public class TSBKMessage extends P25Message
{
    public static final int MESSAGE_START = 64;
    public static final int LAST_BLOCK_FLAG = 64;
    public static final int ENCRYPTED_FLAG = 65;
    public static final int[] OPCODE = {66, 67, 68, 69, 70, 71};
    public static final int[] VENDOR_ID = {72, 73, 74, 75, 76, 77, 78, 79};
    public static final int[] SERVICE_OPTIONS = {64, 65, 66, 67, 68, 69, 70, 71};

    public static final int[] BLOCK1 = {64, 65, 66, 67, 68, 69, 70, 71};
    public static final int[] BLOCK2 = {72, 73, 74, 75, 76, 77, 78, 79};
    public static final int[] BLOCK3 = {80, 81, 82, 83, 84, 85, 86, 87};
    public static final int[] BLOCK4 = {88, 89, 90, 91, 92, 93, 94, 95};
    public static final int[] BLOCK5 = {96, 97, 98, 99, 100, 101, 102, 103};
    public static final int[] BLOCK6 = {104, 105, 106, 107, 108, 109, 110, 111};
    public static final int[] BLOCK7 = {112, 113, 114, 115, 116, 117, 118, 119};
    public static final int[] BLOCK8 = {120, 121, 122, 123, 124, 125, 126, 127};
    public static final int[] BLOCK9 = {128, 129, 130, 131, 132, 133, 134, 135};
    public static final int[] BLOCK10 = {136, 137, 138, 139, 140, 141, 142, 143};
    public static final int[] BLOCK11 = {144, 145, 146, 147, 148, 149, 150, 151};
    public static final int[] BLOCK12 = {152, 153, 154, 155, 156, 157, 158, 159};
    public static final int CRC_START = 144;

    public TSBKMessage(BinaryMessage message, DataUnitID duid, AliasList aliasList)
    {
        super(message, duid, aliasList);

        /* Since CRC is checked before construction, mark it as passed */
        mCRC = new CRC[1];
        mCRC[0] = CRC.PASSED;
    }

    public boolean isLastBlock()
    {
        return mMessage.get(LAST_BLOCK_FLAG);
    }

    public boolean isEncrypted()
    {
        return mMessage.get(ENCRYPTED_FLAG);
    }

    public Vendor getVendor()
    {
        return Vendor.fromValue(mMessage.getInt(VENDOR_ID));
    }

    public Opcode getOpcode()
    {
        return Opcode.fromValue(mMessage.getInt(OPCODE));
    }

    public MotorolaOpcode getMotorolaOpcode()
    {
        return MotorolaOpcode.fromValue(mMessage.getInt(OPCODE));
    }

    public VendorOpcode getVendorOpcode()
    {
        return VendorOpcode.fromValue(mMessage.getInt(OPCODE));
    }

    @Override
    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());

        sb.append(" - " + getMessageHex());

        return sb.toString();
    }

    protected String getMessageStub()
    {
        StringBuilder sb = new StringBuilder();

        Vendor vendor = getVendor();

        sb.append("NAC:");
        sb.append(getNAC());
        sb.append(" ");
        sb.append(getDUID().getLabel());

        if(vendor == Vendor.STANDARD)
        {
            if(isEncrypted())
            {
                sb.append(" ENCRYPTED");
            }
            else
            {
                sb.append(" ");
                sb.append(getOpcode().getLabel());
            }
        }
        else
        {
            sb.append(" ");
            sb.append(vendor.getLabel());

            if(isEncrypted())
            {
                sb.append(" ENCRYPTED");
            }
            else
            {
                sb.append(" OPCD:");
                sb.append(mMessage.getHex(OPCODE, 2));
            }
        }

        return sb.toString();
    }

    public String getMessageHex()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(mMessage.getHex(BLOCK1, 2) + " ");
        sb.append(mMessage.getHex(BLOCK2, 2) + " ");
        sb.append(mMessage.getHex(BLOCK3, 2) + " ");
        sb.append(mMessage.getHex(BLOCK4, 2) + " ");
        sb.append(mMessage.getHex(BLOCK5, 2) + " ");
        sb.append(mMessage.getHex(BLOCK6, 2) + " ");
        sb.append(mMessage.getHex(BLOCK7, 2) + " ");
        sb.append(mMessage.getHex(BLOCK8, 2) + " ");
        sb.append(mMessage.getHex(BLOCK9, 2) + " ");
        sb.append(mMessage.getHex(BLOCK10, 2) + " ");
        sb.append(mMessage.getHex(BLOCK11, 2) + " ");
        sb.append(mMessage.getHex(BLOCK12, 2) + " ");

        return sb.toString();
    }
}
