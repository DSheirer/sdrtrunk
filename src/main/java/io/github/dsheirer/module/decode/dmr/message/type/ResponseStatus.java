/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.module.decode.dmr.message.type;

/**
 * Response header status parser class.
 *
 * ETSI TS 102 361-1 V2.5.1 Table 8.3: Response Packet Class, Type, and Status definitions
 */
public class ResponseStatus
{
    private int mValue;


    /**
     * Constructs an instance
     * @param value in the range of 0 - 255
     */
    public ResponseStatus(int value)
    {
        mValue = value;
    }

    /**
     * Status class indicating success, fail, or selective retry.
     * @return status.
     */
    public Status getStatus()
    {
        return Status.fromValue(mValue);
    }

    /**
     * Type of status message, specific to each status class.
     * @return
     */
    public int getType()
    {
        return (mValue & 0x38) >> 3;
    }

    /**
     * Value of the Status field.
     * @return value
     */
    public int getValue()
    {
        return (mValue & 0x07);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        switch(getStatus())
        {
            case ACK:
                sb.append("SUCCESS - ALL BLOCKS THRU ").append(getValue()).append(" RECEIVED");
                break;
            case NACK:
                switch(getType())
                {
                    case 0:
                        sb.append("FAIL - ILLEGAL FORMAT - LAST VALID FSN:").append(getValue());
                        break;
                    case 1:
                        sb.append("FAIL - PACKET CRC FAIL - LAST VALID FSN:").append(getValue());
                        break;
                    case 2:
                        sb.append("FAIL - RECIPIENT MEMORY FULL - LAST VALID FSN:").append(getValue());
                        break;
                    case 3:
                        sb.append("FAIL - OUT OF SEQUENCE FSN:").append(getValue());
                        break;
                    case 4:
                        sb.append("FAIL - UNDELIVERABLE - LAST VALID FSN:").append(getValue());
                        break;
                    case 5:
                        sb.append("FAIL - PACKET OUT OF SEQUENCE - LAST VALID FSN:").append(getValue());
                        break;
                    case 6:
                        sb.append("FAIL - INVALID USER ON THIS SYSTEM");
                        break;
                    default:
                        sb.append("FAIL - UNKNOWN TYPE [").append(getType()).append("] VALUE [").append(getValue()).append("]");
                        break;
                }
                break;
            case SACK:
                sb.append("SELECTIVE RETRY - LAST VALID FSN:").append(getValue());
                break;
            case RESERVED:
                sb.append("UNKNOWN/RESERVED WITH TYPE [").append(getType()).append("] VALUE [").append(getValue()).append("]");
                break;
        }

        return sb.toString();
    }

    /**
     * Status for the response, from the 2 MSB of the value.
     */
    private enum Status
    {
        ACK,   //Success
        NACK,  //Fail
        SACK,  //Selective Retry
        RESERVED;

        /**
         * Lookup the status from the value.
         * @param value of the status
         * @return lookup or RESERVED
         */
        public static Status fromValue(int value)
        {
            int masked = (value & 0xC0) >> 6;

            if(0 <= masked && masked <= 2)
            {
                return Status.values()[masked];
            }

            return RESERVED;
        }
    }
}
