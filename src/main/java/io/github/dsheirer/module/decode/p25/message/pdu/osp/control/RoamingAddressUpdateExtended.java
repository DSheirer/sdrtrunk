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
package io.github.dsheirer.module.decode.p25.message.pdu.osp.control;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.edac.CRCP25;
import io.github.dsheirer.identifier.IIdentifier;
import io.github.dsheirer.identifier.integer.node.APCO25System;
import io.github.dsheirer.identifier.integer.node.APCO25Wacn;
import io.github.dsheirer.identifier.integer.talkgroup.APCO25FromTalkgroup;
import io.github.dsheirer.identifier.integer.talkgroup.APCO25ToTalkgroup;
import io.github.dsheirer.module.decode.p25.P25MessageFramer;
import io.github.dsheirer.module.decode.p25.message.pdu.PDUMessage;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;

public class RoamingAddressUpdateExtended extends PDUMessage
{
    public static final int[] TARGET_ADDRESS = {88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111};
    public static final int LAST_MESSAGE_INDICATOR = 128;
    public static final int[] MESSAGE_SEQUENCE_NUMBER = {132, 133, 134, 135};
    public static final int[] WACN_A = {136, 137, 138, 139, 140, 141, 142, 143, 160, 161, 162, 163, 164, 165, 166, 167, 168, 169, 170, 171};
    public static final int[] SYSTEM_ID_A = {172, 173, 174, 175, 176, 177, 178, 179, 180, 181, 182, 183};
    public static final int[] SOURCE_ADDRESS_FORMAT_1 = {184, 185, 186, 187, 188, 189, 190, 191, 192, 193, 194, 195, 196, 197, 198, 199, 200, 201, 202, 203, 204, 205, 206, 207};
    public static final int[] WACN_B = {184, 185, 186, 187, 188, 189, 190, 191, 192, 193, 194, 195, 196, 197, 198, 199, 200, 201, 202, 203};
    public static final int[] SYSTEM_ID_B = {204, 205, 206, 207, 208, 209, 210, 211, 212, 213, 214, 215};
    public static final int[] WACN_C = {216, 217, 218, 219, 220, 221, 222, 223, 224, 225, 226, 227, 228, 229, 230, 231, 232, 233, 234, 235};
    public static final int[] SYSTEM_ID_C = {236, 237, 238, 239, 240, 241, 242, 243, 244, 245, 246, 247};
    public static final int[] WACN_D = {248, 249, 250, 251, 252, 253, 254, 255, 256, 257, 258, 259, 260, 261, 262, 263, 264, 265, 266, 267};
    public static final int[] SYSTEM_ID_D = {268, 269, 270, 271, 272, 273, 274, 275, 276, 277, 278, 279};
    public static final int[] SOURCE_ADDRESS_FORMAT_2 = {280, 281, 282, 283, 284, 285, 286, 287, 288, 289, 290, 291, 292, 293, 294, 295, 296, 297, 298, 299, 300, 301, 302, 303};
    public static final int[] WACN_E = {280, 281, 282, 283, 284, 285, 286, 287, 288, 289, 290, 291, 292, 293, 294, 295, 296, 297, 298, 299};
    public static final int[] SYSTEM_ID_E = {300, 301, 302, 303, 304, 305, 306, 307, 308, 309, 310, 311};
    public static final int[] WACN_F = {312, 313, 314, 315, 316, 317, 318, 319, 320, 321, 322, 323, 324, 325, 326, 327, 328, 329, 330, 331};
    public static final int[] SYSTEM_ID_F = {332, 333, 334, 335, 336, 337, 338, 339, 340, 341, 342, 343};
    public static final int[] WACN_G = {344, 345, 346, 347, 348, 349, 350, 351, 352, 353, 354, 355, 356, 357, 358, 359, 360, 361, 362, 363};
    public static final int[] SYSTEM_ID_G = {364, 365, 366, 367, 368, 369, 370, 371, 372, 373, 374, 375};
    public static final int[] SOURCE_ADDRESS_FORMAT_3 = {376, 377, 378, 379, 380, 381, 382, 383, 384, 385, 386, 387, 388, 389, 390, 391, 392, 393, 394, 395, 396, 397, 398, 399};
    public static final int[] MULTIPLE_BLOCK_CRC_FORMAT_1 = {224, 225, 226, 227, 228, 229, 230, 231, 232, 233, 234, 235, 236, 237, 238, 239, 240, 241, 242, 243, 244, 245, 246, 247, 248, 249, 250, 251, 252, 253, 254, 255};
    public static final int[] MULTIPLE_BLOCK_CRC_FORMAT_2 = {320, 321, 322, 323, 324, 325, 326, 327, 328, 329, 330, 331, 332, 333, 334, 335, 336, 337, 338, 339, 340, 341, 342, 343, 344, 345, 346, 347, 348, 349, 350, 351};
    public static final int[] MULTIPLE_BLOCK_CRC_FORMAT_3 = {416, 417, 418, 419, 420, 421, 422, 423, 424, 425, 426, 427, 428, 429, 430, 431, 432, 433, 434, 435, 436, 437, 438, 439, 440, 441, 442, 443, 444, 445, 446, 447};

    private enum Format
    {
        FORMAT_1, FORMAT_2, FORMAT_3
    }

    private Format mFormat;
    private IIdentifier mTargetAddress;
    private IIdentifier mSourceAddress;
    private IIdentifier mWACNA;
    private IIdentifier mWACNB;
    private IIdentifier mWACNC;
    private IIdentifier mWACND;
    private IIdentifier mWACNE;
    private IIdentifier mWACNF;
    private IIdentifier mWACNG;
    private IIdentifier mSystemA;
    private IIdentifier mSystemB;
    private IIdentifier mSystemC;
    private IIdentifier mSystemD;
    private IIdentifier mSystemE;
    private IIdentifier mSystemF;
    private IIdentifier mSystemG;

    public RoamingAddressUpdateExtended(BinaryMessage message, DataUnitID duid, AliasList aliasList)
    {
        super(message, duid, aliasList);

        if(mMessage.size() == P25MessageFramer.PDU2_BEGIN)
        {
            mFormat = Format.FORMAT_1;

            /* Header block is already error detected/corrected - perform error
             * detection correction on the intermediate and final data blocks */
            mMessage = CRCP25.correctPDU1(mMessage);
            mCRC[1] = mMessage.getCRC();
        }
        else if(mMessage.size() == P25MessageFramer.PDU3_BEGIN)
        {
            mFormat = Format.FORMAT_2;

            /* Header block is already error detected/corrected - perform error
             * detection correction on the intermediate and final data blocks */
            mMessage = CRCP25.correctPDU2(mMessage);
            mCRC[1] = mMessage.getCRC();
        }
        else if(mMessage.size() == P25MessageFramer.PDU3_DECODED_END)
        {
            mFormat = Format.FORMAT_3;

            /* Header block is already error detected/corrected - perform error
             * detection correction on the intermediate and final data blocks */
            mMessage = CRCP25.correctPDU3(mMessage);
            mCRC[1] = mMessage.getCRC();
        }
        else
        {
            mFormat = Format.FORMAT_1;

            /* Header block is already error detected/corrected - perform error
             * detection correction on the intermediate and final data blocks */
            mMessage = CRCP25.correctPDU1(mMessage);
            mCRC[1] = mMessage.getCRC();
        }
    }

    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());

        sb.append(" ROAMING ADDRESS STACK");

        if(isLastBlock())
        {
            sb.append(" LAST BLOCK");
        }

        sb.append("MSG SEQ:");
        sb.append(getMessageSequenceNumber());

        sb.append(" FM:");
        sb.append(getSourceAddress());
        sb.append(" TO:");
        sb.append(getTargetAddress());

        sb.append(" WACN:SYS A:").append(getWACNA()).append(":").append(getSystemA());

        if(isFormat2())
        {
            sb.append(" B:").append(getWACNB()).append(":").append(getSystemB());
            sb.append(" C:").append(getWACNC()).append(":").append(getSystemC());
            sb.append(" D:").append(getWACND()).append(":").append(getSystemD());
        }

        if(isFormat3())
        {
            sb.append(" E:").append(getWACNE()).append(":").append(getSystemE());
            sb.append(" F:").append(getWACNF()).append(":").append(getSystemF());
            sb.append(" G:").append(getWACNG()).append(":").append(getSystemG());
        }

        return sb.toString();
    }

    public boolean isLastBlock()
    {
        return mMessage.get(LAST_MESSAGE_INDICATOR);
    }

    public int getMessageSequenceNumber()
    {
        return mMessage.getInt(MESSAGE_SEQUENCE_NUMBER);
    }

    public IIdentifier getTargetAddress()
    {
        if(mTargetAddress == null)
        {
            mTargetAddress = APCO25ToTalkgroup.createIndividual(mMessage.getInt(TARGET_ADDRESS));
        }

        return mTargetAddress;
    }

    public IIdentifier getWACNA()
    {
        if(mWACNA == null)
        {
            mWACNA = APCO25Wacn.create(mMessage.getInt(WACN_A));
        }

        return mWACNA;
    }

    public IIdentifier getWACNB()
    {
        if((isFormat2() || isFormat3()) && mWACNB == null)
        {
            mWACNB = APCO25Wacn.create(mMessage.getInt(WACN_B));
        }

        return mWACNB;
    }

    public IIdentifier getWACNC()
    {
        if((isFormat2() || isFormat3()) && mWACNC == null)
        {
            mWACNC = APCO25Wacn.create(mMessage.getInt(WACN_C));
        }

        return mWACNC;
    }

    public IIdentifier getWACND()
    {
        if((isFormat2() || isFormat3()) && mWACND == null)
        {
            mWACND = APCO25Wacn.create(mMessage.getInt(WACN_D));
        }

        return mWACND;
    }

    public IIdentifier getWACNE()
    {
        if(isFormat3() && mWACNE == null)
        {
            mWACNE = APCO25Wacn.create(mMessage.getInt(WACN_E));
        }

        return mWACNE;
    }

    public IIdentifier getWACNF()
    {
        if(isFormat3() && mWACNF == null)
        {
            mWACNF = APCO25Wacn.create(mMessage.getInt(WACN_F));
        }

        return mWACNF;
    }

    public IIdentifier getWACNG()
    {
        if(isFormat3() && mWACNG == null)
        {
            mWACNG = APCO25Wacn.create(mMessage.getInt(WACN_G));
        }

        return mWACNG;
    }

    public IIdentifier getSystemA()
    {
        if(mSystemA == null)
        {
            mSystemA = APCO25System.create(mMessage.getInt(SYSTEM_ID_A));
        }

        return mSystemA;
    }

    public IIdentifier getSystemB()
    {
        if((isFormat2() || isFormat3()) && mSystemB == null)
        {
            mSystemB = APCO25System.create(mMessage.getInt(SYSTEM_ID_B));
        }

        return mSystemB;
    }

    public IIdentifier getSystemC()
    {
        if((isFormat2() || isFormat3()) && mSystemC == null)
        {
            mSystemC = APCO25System.create(mMessage.getInt(SYSTEM_ID_C));
        }

        return mSystemC;
    }

    public IIdentifier getSystemD()
    {
        if((isFormat2() || isFormat3()) && mSystemD == null)
        {
            mSystemD = APCO25System.create(mMessage.getInt(SYSTEM_ID_D));
        }

        return mSystemD;
    }

    public IIdentifier getSystemE()
    {
        if(isFormat3() && mSystemE == null)
        {
            mSystemE = APCO25System.create(mMessage.getInt(SYSTEM_ID_E));
        }

        return mSystemE;
    }

    public IIdentifier getSystemF()
    {
        if(isFormat3() && mSystemF == null)
        {
            mSystemF = APCO25System.create(mMessage.getInt(SYSTEM_ID_F));
        }

        return mSystemF;
    }

    public IIdentifier getSystemG()
    {
        if(isFormat3() && mSystemG == null)
        {
            mSystemG = APCO25System.create(mMessage.getInt(SYSTEM_ID_G));
        }

        return mSystemG;
    }

    public IIdentifier getSourceAddress()
    {
        if(mSourceAddress == null)
        {
            switch(mFormat)
            {
                case FORMAT_1:
                    mSourceAddress = APCO25FromTalkgroup.createIndividual(mMessage.getInt(SOURCE_ADDRESS_FORMAT_1));
                    break;
                case FORMAT_2:
                    mSourceAddress = APCO25FromTalkgroup.createIndividual(mMessage.getInt(SOURCE_ADDRESS_FORMAT_2));
                    break;
                case FORMAT_3:
                    mSourceAddress = APCO25FromTalkgroup.createIndividual(mMessage.getInt(SOURCE_ADDRESS_FORMAT_3));
                    break;
            }
        }

        return mSourceAddress;
    }

    public boolean isFormat2()
    {
        return mFormat != Format.FORMAT_3;
    }

    public boolean isFormat3()
    {
        return mFormat == Format.FORMAT_3;
    }
}
