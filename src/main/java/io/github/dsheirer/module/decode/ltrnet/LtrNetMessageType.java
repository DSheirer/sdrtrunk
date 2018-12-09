/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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

package io.github.dsheirer.module.decode.ltrnet;

public enum LtrNetMessageType
{
    ISW_CALL_END,
    ISW_CALL_START,
    ISW_REGISTRATION_REQUEST_ESN_HIGH,
    ISW_REGISTRATION_REQUEST_ESN_LOW,
    ISW_REQUEST_ACCESS,
    ISW_UNIQUE_ID,
    ISW_UNKNOWN,

    OSW_CALL_END,
    OSW_CALL_START,
    OSW_CHANNEL_MAP_HIGH,
    OSW_CHANNEL_MAP_LOW,
    OSW_SYSTEM_IDLE,
    OSW_NEIGHBOR_ID,
    OSW_RECEIVE_FREQUENCY_HIGH,
    OSW_RECEIVE_FREQUENCY_LOW,
    OSW_REGISTRATION_ACCEPT,
    OSW_SITE_ID,
    OSW_TRANSMIT_FREQUENCY_HIGH,
    OSW_TRANSMIT_FREQUENCY_LOW,
    OSW_UNKNOWN
}
