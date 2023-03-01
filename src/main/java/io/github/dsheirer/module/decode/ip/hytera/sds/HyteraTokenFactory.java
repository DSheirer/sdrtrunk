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

package io.github.dsheirer.module.decode.ip.hytera.sds;

import io.github.dsheirer.bits.CorrectedBinaryMessage;

/**
 * Constructs Hytera token field instances.
 */
public class HyteraTokenFactory
{
    /**
     * Creates a new parsed field instance
     * @param token that identifies the field type
     * @param message fragment that includes the token, run-length, and field content.
     * @return parsed field instance
     */
    public static HyteraToken getField(HyteraTokenType token, CorrectedBinaryMessage message)
    {
        return switch(token)
        {
            case ID_DESTINATION -> new DestinationId(message);
            case ENCODING -> new Encoding(message);
            case ID_MESSAGE -> new MessageId(message);
            case PAYLOAD -> new Payload(message);
            case MESSAGE_HEADER -> new MessageHeader(message);
            case ID_SOURCE -> new SourceId(message);
            default -> new Unknown(message);
        };
    }
}
