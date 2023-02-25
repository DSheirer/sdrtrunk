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

package io.github.dsheirer.module.decode.ip.mototrbo.lrrp.token;

import io.github.dsheirer.bits.CorrectedBinaryMessage;

/**
 * LRRP Token Factory
 */
public class TokenFactory
{
    public static Token createToken(String id, CorrectedBinaryMessage message, int offset, int remainingCharacterCount)
    {
        TokenType tokenType = TokenType.fromValue(id);

        switch(tokenType)
        {
            case HEADING:
                return new Heading(message, offset);
            case IDENTITY:
                return new Identity(message, offset);
            case POINT_2D:
                return new Point2d(message, offset);
            case CIRCLE_2D:
                return new Circle2d(message, offset);
            case POINT_3D:
                return new Point3d(message, offset);
            case CIRCLE_3D:
                return new Circle3d(message, offset);
            case RESPONSE:
                return new Response(message, offset);
            case SPEED:
                return new Speed(message, offset);
            case SUCCESS:
                return new Success(message, offset);
            case TIMESTAMP:
                return new Timestamp(message, offset);
            case TRIGGER_DISTANCE:
                return new TriggerDistance(message, offset);
            case TRIGGER_GPIO:
                return new TriggerGpio(message, offset);
            case TRIGGER_ON_MOVE:
                return new TriggerOnMove(message, offset);
            case TRIGGER_PERIODIC:
                return new TriggerPeriodic(message, offset);
            case VERSION:
                return new Version(message, offset);
            case UNKNOWN_23:
                return new Unknown23(message, offset);
            case REQUEST_61:
                return new Request61(message, offset);
            case REQUEST_73:
                return new Request73(message, offset);
            case UNKNOWN:
            default:
                return new UnknownToken(message, offset, remainingCharacterCount);
        }
    }
}
