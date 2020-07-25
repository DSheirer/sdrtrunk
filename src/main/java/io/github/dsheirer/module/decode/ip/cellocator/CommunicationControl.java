/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.module.decode.ip.cellocator;

/**
 * Communication Control Field parser
 */
public class CommunicationControl
{
    public enum Initiative {ACTIVE, RESPONSE};
    public enum SpeedType {MOMENTARY, MAX};

    private static final int MASK_CAN_ORIGINATED_ODOMETER = 0x8000;
    private static final int MASK_CAN_ORIGINATED_SPEED = 0x4000;
    private static final int MASK_MULTI_PURPOSE_FIELD_CONTENT = 0x3000;
    private static final int MASK_CR200_CR300_MESSAGE_SOURCE = 0x800;
    private static final int MASK_GARMIN_CONNECTED = 0x400;
    private static final int MASK_GARMIN_ENABLED = 0x200;
    private static final int MASK_CR200_CR300_MESSAGE_INITIATIVE = 0x100;
    private static final int MASK_CR200_CR300_GSM_HIBERNATION_INDICATOR = 0x80;
    private static final int MASK_MOMENTARY_OR_MAX_SPEED_VALUE = 0x40;
    private static final int MASK_BUSINESS_OR_PRIVATE_MODE = 0x20;
    private static final int MASK_FIRMWARE_SUB_VERSION = 0x1F;

    private int mValue;

    /**
     * Constructs an instance
     *
     * @param value of the 16-bit field
     */
    public CommunicationControl(int value)
    {
        mValue = value;
    }

    /**
     * Indicates if the odometer value originated from the vehicle CAN bus
     */
    public boolean isCANOriginatedOdometer()
    {
        return isSet(MASK_CAN_ORIGINATED_ODOMETER);
    }

    /**
     * Indicates if the speed value originated from the vehicle CAN bus
     */
    public boolean isCANOriginatedSpeed()
    {
        return isSet(MASK_CAN_ORIGINATED_SPEED);
    }

    /**
     * Indicates if the Garmin GPS is connected to the unit
     */
    public boolean isGarminConnected()
    {
        return isSet(MASK_GARMIN_CONNECTED);
    }

    /**
     * Indicates if the Garmin GPS is enabled
     */
    public boolean isGarminEnabled()
    {
        return isSet(MASK_GARMIN_ENABLED);
    }

    /**
     * Indicates if the message was initiated by the unit or if the message is a response to a request.
     */
    public Initiative getMessageSource()
    {
        return isSet(MASK_CR200_CR300_MESSAGE_INITIATIVE) ? Initiative.RESPONSE : Initiative.ACTIVE;
    }

    /**
     * Indicates the context of the speed value contained in the message, momentary or max recorded from last event.
     */
    public SpeedType getSpeedType()
    {
        return isSet(MASK_MOMENTARY_OR_MAX_SPEED_VALUE) ? SpeedType.MAX : SpeedType.MOMENTARY;
    }

    /**
     * Indicates if the masked value is set for this field
     * @param mask to check
     * @return true if all set bits of the mask value are also set in the field bits
     */
    private boolean isSet(int mask)
    {
        return (mValue & mask) == mask;
    }
}
