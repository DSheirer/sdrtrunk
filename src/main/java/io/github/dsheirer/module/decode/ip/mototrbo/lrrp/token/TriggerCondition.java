/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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
package io.github.dsheirer.module.decode.ip.mototrbo.lrrp.token;

import io.github.dsheirer.bits.CorrectedBinaryMessage;

/**
 * LRRP Trigger Condition Token
 *
 * Start Token: 0x4A
 * Total Length: 2 bytes
 *
 * Event that causes the radio to send a triggered location report.  Condition values are from node-dmr-lib
 * (src/DMRConst.js) and match captured requests: 0x4A 0x02 emergency, 0x4A 0x42 pin status change (telemetry),
 * 0x4A 0x50 PTT and 0x4A 0x51 PTT emergency.
 */
public class TriggerCondition extends Token
{
    private static final int[] CONDITION = new int[]{8, 9, 10, 11, 12, 13, 14, 15};

    /**
     * Trigger condition values
     */
    public enum Condition
    {
        EMERGENCY(0x02, "EMERGENCY"),
        PIN_STATUS_CHANGE(0x42, "PIN STATUS CHANGE"),
        PTT(0x50, "PTT"),
        PTT_EMERGENCY(0x51, "PTT EMERGENCY"),
        UNKNOWN(-1, "UNKNOWN");

        private int mValue;
        private String mLabel;

        Condition(int value, String label)
        {
            mValue = value;
            mLabel = label;
        }

        /**
         * Numeric value of the condition
         */
        public int getValue()
        {
            return mValue;
        }

        /**
         * Lookup a condition from a value
         * @param value to lookup
         * @return matching condition or UNKNOWN
         */
        public static Condition fromValue(int value)
        {
            for(Condition condition: Condition.values())
            {
                if(condition.getValue() == value)
                {
                    return condition;
                }
            }

            return UNKNOWN;
        }

        @Override
        public String toString()
        {
            return mLabel;
        }
    }

    /**
     * Constructs an instance of a trigger condition token.
     *
     * @param message containing the token
     * @param offset to the start of the token
     */
    public TriggerCondition(CorrectedBinaryMessage message, int offset)
    {
        super(message, offset);
    }

    @Override
    public TokenType getTokenType()
    {
        return TokenType.TRIGGER_CONDITION;
    }

    /**
     * Raw condition value
     */
    public int getConditionValue()
    {
        return getMessage().getInt(CONDITION, getOffset());
    }

    /**
     * Trigger condition
     */
    public Condition getCondition()
    {
        return Condition.fromValue(getConditionValue());
    }

    @Override
    public String toString()
    {
        Condition condition = getCondition();

        if(condition == Condition.UNKNOWN)
        {
            return "TRIGGER CONDITION:UNKNOWN[0x" + String.format("%02X", getConditionValue()) + "]";
        }

        return "TRIGGER CONDITION:" + condition;
    }
}
