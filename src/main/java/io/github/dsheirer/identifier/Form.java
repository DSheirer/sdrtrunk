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
package io.github.dsheirer.identifier;

/**
 * Identifier form.  Indicates the type of identifier.
 */
public enum Form
{
    ALIAS_LIST,
    CHANNEL,
    CHANNEL_DESCRIPTOR,
    CHANNEL_NAME,
    CHANNEL_FREQUENCY,
    DECODER_TYPE,
    ENCRYPTION_KEY,
    ESN,
    IPV4_ADDRESS,
    LOCATION_REGISTRATION_AREA,
    LOJACK,
    NEIGHBOR_SITE,
    NETWORK_ACCESS_CODE,
    PATCH_GROUP,
    RF_SUBSYSTEM,
    SHORT_DATA_MESSAGE,
    SITE,
    STATE,
    SYSTEM,
    TALKGROUP,
    TELEPHONE_NUMBER,
    UNIT_STATUS,
    USER_STATUS,
    UNIQUE_ID,
    WACN,
    ANY;
}
