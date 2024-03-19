/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

package io.github.dsheirer.module.decode.p25.phase1.message.lc;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.FragmentedIntField;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.message.AbstractMessage;
import io.github.dsheirer.module.decode.p25.reference.Vendor;
import java.util.List;

/**
 * APCO 25 Link Control Word.  This message word is contained in Logical Link Data Unit 1 and Terminator with
 * Link Control messages.
 */
public abstract class LinkControlWord extends AbstractMessage
{
    public static final int OCTET_0_BIT_0 = 0;
    public static final int OCTET_1_BIT_8 = 8;
    public static final int OCTET_2_BIT_16 = 16;
    public static final int OCTET_3_BIT_24 = 24;
    public static final int OCTET_4_BIT_32 = 32;
    public static final int OCTET_5_BIT_40 = 40;
    public static final int OCTET_6_BIT_48 = 48;
    public static final int OCTET_7_BIT_56 = 56;
    public static final int OCTET_8_BIT_64 = 64;
    public static final int OCTET_9_BIT_72 = 72;
    public static final int OCTET_10_BIT_80 = 80;
    public static final int OCTET_11_BIT_88 = 88;
    public static final int OCTET_12_BIT_96 = 96;

    private static final int ENCRYPTION_FLAG = 0;
    private static final int STANDARD_VENDOR_ID_FLAG = 1;
    private static final FragmentedIntField OPCODE = FragmentedIntField.of(2, 3, 4, 5, 6, 7);
    private static final IntField VENDOR = IntField.length8(OCTET_1_BIT_8);
    private LinkControlOpcode mLinkControlOpcode;
    private boolean mValid;

    /**
     * Constructs a Link Control Word from the binary message sequence.
     *
     * @param message
     */
    public LinkControlWord(CorrectedBinaryMessage message)
    {
        super(message);
    }

    /**
     * Indicates if the message passes CRC checks.
     */
    public boolean isValid()
    {
        return mValid;
    }

    /**
     * Sets the valid flag for this message.
     */
    public void setValid(boolean valid)
    {
        mValid = valid;
    }

    /**
     * Indicates if this is an encrypted LCW
     */
    public boolean isEncrypted()
    {
        return getMessage().get(ENCRYPTION_FLAG);
    }

    /**
     * Indicates if this is a standard vendor format LCW
     */
    public boolean isStandardVendorFormat()
    {
        return isStandardVendorFormat(getMessage());
    }

    /**
     * Indicates if the link control word message has standard vendor format.
     */
    public static boolean isStandardVendorFormat(BinaryMessage binaryMessage)
    {
        return binaryMessage.get(STANDARD_VENDOR_ID_FLAG);
    }

    /**
     * Vendor format for this link control word.
     */
    public Vendor getVendor()
    {
        return getVendor(getMessage());
    }

    /**
     * Lookup the Vendor format for the specified LCW
     */
    public static Vendor getVendor(CorrectedBinaryMessage binaryMessage)
    {
        if(isStandardVendorFormat(binaryMessage))
        {
            return Vendor.STANDARD;
        }
        else
        {
            return Vendor.fromValue(binaryMessage.getInt(VENDOR));
        }
    }

    /**
     * Opcode for this LCW
     */
    public LinkControlOpcode getOpcode()
    {
        if(mLinkControlOpcode == null)
        {
            mLinkControlOpcode = getOpcode(getMessage());
        }

        return mLinkControlOpcode;
    }

    /**
     * Opcode number for this LCW
     */
    public int getOpcodeNumber()
    {
        return getInt(OPCODE);
    }

    /**
     * Identifies the link control word opcode from the binary message.
     */
    public static LinkControlOpcode getOpcode(CorrectedBinaryMessage binaryMessage)
    {
        return LinkControlOpcode.fromValue(binaryMessage.getInt(OPCODE), getVendor(binaryMessage));
    }

    /**
     * List of identifiers provided by the message
     */
    public abstract List<Identifier> getIdentifiers();

    /**
     * Creates a string with the basic Link Control Word information
     */
    public String getMessageStub()
    {
        StringBuilder sb = new StringBuilder();

        if(!isValid())
        {
            sb.append("***LINK CONTROL CRC FAIL*** ");
        }

        sb.append(getOpcode().getLabel());

        if(isEncrypted())
        {
            sb.append(" ENCRYPTED");
        }
        else
        {
            if(!isStandardVendorFormat())
            {
                Vendor vendor = getVendor();

                if(vendor != Vendor.STANDARD)
                {
                    sb.append(" ").append(vendor.getLabel());
                }
            }
        }

        return sb.toString();
    }
}
