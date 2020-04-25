/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014,2015 Dennis Sheirer
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package io.github.dsheirer.module.decode.lj1200;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.CRC;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.message.Message;
import io.github.dsheirer.protocol.Protocol;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;

public class LJ1200TransponderMessage extends Message
{
    private final static Logger mLog = LoggerFactory.getLogger(LJ1200TransponderMessage.class);

    public static int[] SYNC = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};

    public static int[] MESSAGE_CRC = {64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79};

    private static SimpleDateFormat mSDF = new SimpleDateFormat("yyyyMMdd HHmmss");

    private CorrectedBinaryMessage mMessage;
    private CRC mCRC;

    public LJ1200TransponderMessage(CorrectedBinaryMessage message)
    {
        mMessage = message;

        checkCRC();

        switch(mCRC)
        {
            case CORRECTED:
                mLog.debug("CORR:" + message.toString());
                break;
            case FAILED_CRC:
                mLog.debug("FAIL:" + message.toString());
                break;
            case PASSED:
                mLog.debug("PASS:" + message.toString());
                break;
            case UNKNOWN:
                mLog.debug("UNKNOWN:" + message.toString());
                break;
            default:
                break;
        }
    }

    private void checkCRC()
    {
        mCRC = CRC.UNKNOWN;
    }

    public boolean isValid()
    {
//    	return mCRC == CRC.PASSED || mCRC == CRC.CORRECTED;

        /* Override validity check until proper CRC is implemented */
        return true;
    }

    public String getCRC()
    {
        return mMessage.getHex(MESSAGE_CRC, 4);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("TRANSPONDER TEST");

        return sb.toString();
    }

    /**
     * Pads spaces onto the end of the value to make it 'places' long
     */
    public String pad(String value, int places, String padCharacter)
    {
        return StringUtils.rightPad(value, places, padCharacter);
    }

    /**
     * Pads an integer value with additional zeroes to make it decimalPlaces long
     */
    public String format(int number, int decimalPlaces)
    {
        return StringUtils.leftPad(Integer.valueOf(number).toString(), decimalPlaces, '0');
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.LOJACK;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.EMPTY_LIST;
    }
}
