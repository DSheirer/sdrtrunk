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
package io.github.dsheirer.module.decode.ip.mototrbo.lrrp;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.ip.mototrbo.lrrp.token.Identity;
import io.github.dsheirer.module.decode.ip.mototrbo.lrrp.token.RequestParameter;
import io.github.dsheirer.module.decode.ip.mototrbo.lrrp.token.RequestedTokens;
import io.github.dsheirer.module.decode.ip.mototrbo.lrrp.token.Token;
import io.github.dsheirer.module.decode.ip.mototrbo.lrrp.token.TokenType;
import io.github.dsheirer.module.decode.ip.mototrbo.lrrp.token.TriggerCondition;
import io.github.dsheirer.module.decode.ip.mototrbo.lrrp.token.TriggerInterval;
import io.github.dsheirer.module.decode.ip.mototrbo.lrrp.token.TriggerPeriodic;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * JUnit tests for LRRP request packet token parsing.
 *
 * The sample packets are Motorola triggered location start requests captured from a P25 system (UDP port 4001).
 * Both carry the same requested fields (time, horizontal speed, horizontal direction) and the same two parameters of
 * unknown meaning (0x44 = 100, 0x42 = 3600); one is a periodic trigger every 600 seconds and the other is an
 * emergency trigger.
 */
public class LRRPPacketTest
{
    //Periodic profile: ID FFFFEE, periodic trigger with a 600 second interval (0x31 0x84 0x58)
    private static final String PERIODIC_REQUEST = "09112203FFFFEE524464429C10625734318458";
    //Condition profile: ID FFFF02, trigger condition emergency (0x4A 0x02)
    private static final String EMERGENCY_REQUEST = "09102203FFFF02524464429C106257344A02";
    //PERIODIC_REQUEST as received with the packet type byte corrupted to 0xA3 (unknown type)
    private static final String UNKNOWN_TYPE_REQUEST = "A3112203FFFFEE524464429C10625734318458";
    //EMERGENCY_REQUEST with the packet type byte corrupted to 0x07 (immediate location response)
    private static final String DAMAGED_TYPE_REQUEST = "07102203FFFF02524464429C106257344A02";

    private static final List<TokenType> REQUESTED_FIELDS = List.of(TokenType.TIME, TokenType.HORIZONTAL_SPEED,
            TokenType.HORIZONTAL_DIRECTION);

    private static LRRPPacket parse(String hex)
    {
        return new LRRPPacket(CorrectedBinaryMessage.loadHex(hex), 0);
    }

    private static <T extends Token> T getToken(LRRPPacket packet, Class<T> clazz, TokenType tokenType)
    {
        for(Token token: packet.getTokens())
        {
            if(token.getTokenType() == tokenType && clazz.isInstance(token))
            {
                return clazz.cast(token);
            }
        }

        fail("Packet does not contain token " + tokenType + ": " + packet);
        return null;
    }

    private static boolean hasToken(LRRPPacket packet, TokenType tokenType)
    {
        for(Token token: packet.getTokens())
        {
            if(token.getTokenType() == tokenType)
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Number of payload bytes accounted for by the parsed tokens: parameter tokens contribute their byte length and
     * each requested field flag contributes one byte.
     */
    private static int getConsumedBytes(LRRPPacket packet)
    {
        int consumed = 0;

        for(Token token: packet.getTokens())
        {
            if(token instanceof RequestedTokens requested)
            {
                consumed += requested.getRequestedTokens().size() + requested.getUnrecognizedRequestedTokens().size();
            }
            else
            {
                consumed += token.getByteLength();
            }
        }

        return consumed;
    }

    /**
     * Asserts the fields that both sample requests share.
     */
    private static void assertCommonRequestTokens(LRRPPacket packet, int expectedId)
    {
        Identity identity = getToken(packet, Identity.class, TokenType.IDENTITY);
        assertEquals(expectedId, identity.getID());
        assertEquals(5, identity.getByteLength());

        assertNotNull(getToken(packet, TriggerPeriodic.class, TokenType.TRIGGER_PERIODIC));

        RequestParameter parameter42 = getToken(packet, RequestParameter.class, TokenType.REQUEST_PARAMETER_42);
        assertEquals(3600, parameter42.getValue()); //0x42 0x9C 0x10
        assertEquals(3, parameter42.getByteLength());

        RequestParameter parameter44 = getToken(packet, RequestParameter.class, TokenType.REQUEST_PARAMETER_44);
        assertEquals(100, parameter44.getValue()); //0x44 0x64
        assertEquals(2, parameter44.getByteLength());

        RequestedTokens requested = getToken(packet, RequestedTokens.class, TokenType.REQUESTED_TOKENS);
        assertEquals(REQUESTED_FIELDS, requested.getRequestedTokens());
        assertTrue(requested.getUnrecognizedRequestedTokens().isEmpty(), "No stray tokens: " + packet);

        //Every payload byte is accounted for exactly once
        assertEquals(packet.getHeader().getPayloadLength(), getConsumedBytes(packet));
    }

    @Test
    public void periodicTriggeredLocationStartRequest()
    {
        LRRPPacket packet = parse(PERIODIC_REQUEST);

        assertEquals(LRRPPacketType.TRIGGERED_LOCATION_START_REQUEST, packet.getHeader().getLRRPPacketType());
        assertEquals(17, packet.getHeader().getPayloadLength());
        assertCommonRequestTokens(packet, 0xFFFFEE);

        TriggerInterval interval = getToken(packet, TriggerInterval.class, TokenType.TRIGGER_INTERVAL);
        assertEquals(600, interval.getSeconds()); //0x31 0x84 0x58
        assertEquals(3, interval.getByteLength());
        assertFalse(hasToken(packet, TokenType.TRIGGER_CONDITION));

        assertEquals("LRRP TRIGGERED LOCATION START REQUEST ID:FFFFEE PERIODIC INTERVAL:600 SECS PARAM-42:3600 " +
                "PARAM-44:100 REQUESTED TOKENS [TIME,HORIZONTAL SPEED,HORIZONTAL DIRECTION]", packet.toString());
    }

    @Test
    public void emergencyTriggeredLocationStartRequest()
    {
        LRRPPacket packet = parse(EMERGENCY_REQUEST);

        assertEquals(LRRPPacketType.TRIGGERED_LOCATION_START_REQUEST, packet.getHeader().getLRRPPacketType());
        assertEquals(16, packet.getHeader().getPayloadLength());
        assertCommonRequestTokens(packet, 0xFFFF02);

        TriggerCondition condition = getToken(packet, TriggerCondition.class, TokenType.TRIGGER_CONDITION);
        assertEquals(TriggerCondition.Condition.EMERGENCY, condition.getCondition()); //0x4A 0x02
        assertEquals(2, condition.getByteLength());
        assertFalse(hasToken(packet, TokenType.TRIGGER_INTERVAL));

        assertEquals("LRRP TRIGGERED LOCATION START REQUEST ID:FFFF02 PERIODIC TRIGGER CONDITION:EMERGENCY " +
                "PARAM-42:3600 PARAM-44:100 REQUESTED TOKENS [TIME,HORIZONTAL SPEED,HORIZONTAL DIRECTION]",
                packet.toString());
    }

    /**
     * A corrupted type byte that is not a known packet type takes the request parsing path, so the intact body still
     * decodes completely.
     */
    @Test
    public void unknownTypeRequestBodyStillDecodes()
    {
        LRRPPacket packet = parse(UNKNOWN_TYPE_REQUEST);

        assertEquals(LRRPPacketType.UNKNOWN, packet.getHeader().getLRRPPacketType());
        assertEquals(0xA3, packet.getHeader().getLrrpPacketTypeValue());
        assertCommonRequestTokens(packet, 0xFFFFEE);
        assertEquals(600, getToken(packet, TriggerInterval.class, TokenType.TRIGGER_INTERVAL).getSeconds());

        assertEquals("LRRP TYPE:UNKNOWN [163] ID:FFFFEE PERIODIC INTERVAL:600 SECS PARAM-42:3600 PARAM-44:100 " +
                "REQUESTED TOKENS [TIME,HORIZONTAL SPEED,HORIZONTAL DIRECTION]", packet.toString());
    }

    /**
     * A type byte corrupted to a valid response type (0x07) sends the body down the response parsing path, where
     * 0x34 is a timestamp rather than the periodic trigger flag.  The parse must still terminate within the payload
     * and the identity and variable length integer parameters must still decode.
     */
    @Test
    public void damagedTypeRequestParsesAsResponseWithoutError()
    {
        LRRPPacket packet = parse(DAMAGED_TYPE_REQUEST);

        assertEquals(LRRPPacketType.IMMEDIATE_LOCATION_RESPONSE, packet.getHeader().getLRRPPacketType());
        assertDoesNotThrow(packet::toString);

        assertEquals(0xFFFF02, getToken(packet, Identity.class, TokenType.IDENTITY).getID());
        assertEquals(3600, getToken(packet, RequestParameter.class, TokenType.REQUEST_PARAMETER_42).getValue());
        assertEquals(100, getToken(packet, RequestParameter.class, TokenType.REQUEST_PARAMETER_44).getValue());
    }
}
