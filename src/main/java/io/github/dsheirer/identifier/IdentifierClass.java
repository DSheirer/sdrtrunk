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
package io.github.dsheirer.identifier;

/**
 * Identifier form.  Indicates the type of identifier.
 */
public enum IdentifierClass
{
    //Decoder configuration detail
    CONFIGURATION,

    //Decoder state or information
    DECODER,

    //Communication network identifier
    NETWORK,

    //User network address
    USER_NETWORK_ADDRESS,

    //User network port
    USER_NETWORK_PORT,

    //Communication user identifier
    USER;
}
