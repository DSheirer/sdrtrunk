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

package io.github.dsheirer.module.decode.p25.phase1.message.lc;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25Channel;
import io.github.dsheirer.module.decode.p25.identifier.channel.P25Channel;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCAdjacentSiteStatusBroadcast;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCAdjacentSiteStatusBroadcastExplicit;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Adjacent site status link control field positions.  TIA-102.AABF-D 7.3.15 (LCO 34): octet 2 bits 7-4 are the C, F,
 * V and A flags and octet 8 is the system service class.  7.3.25 (LCO 39, explicit): octet 8 bits 7-4 are the C, F, V
 * and A flags and bits 3-0 are reserved; there is no service class.
 */
public class LCAdjacentSiteStatusTest
{
    private static CorrectedBinaryMessage message(int... octets)
    {
        CorrectedBinaryMessage message = new CorrectedBinaryMessage(72);

        for(int x = 0; x < octets.length; x++)
        {
            message.load(x * 8, 8, octets[x]);
        }

        return message;
    }

    private static P25Channel channel(Object channel)
    {
        return assertInstanceOf(APCO25Channel.class, channel).getValue();
    }

    @Test
    public void explicitOctet8IsSiteFlags()
    {
        //LCO 39 SF=1, LRA 0x12, channel (T) band 1 number 0x234, RFSS 0x05, site 0x0D, channel (R) band 2
        //number 0x567, octet 8 C=1 F=0 V=1 A=0 with the reserved bits set
        LinkControlWord lcw = LinkControlWordFactory.create(message(0x40 | 39, 0x12, 0x12, 0x34, 0x05, 0x0D, 0x25,
            0x67, 0xAF), 0, false);
        LCAdjacentSiteStatusBroadcastExplicit adjacent = assertInstanceOf(LCAdjacentSiteStatusBroadcastExplicit.class,
            lcw);

        assertEquals(0x12, adjacent.getLocationRegistrationArea().getValue());
        assertEquals(0x05, adjacent.getRfss().getValue());
        assertEquals(0x0D, adjacent.getSite().getValue());
        P25Channel channel = channel(adjacent.getChannel());
        assertEquals(1, channel.getDownlinkBandIdentifier());
        assertEquals(0x234, channel.getDownlinkChannelNumber());
        assertEquals(2, channel.getUplinkBandIdentifier());
        assertEquals(0x567, channel.getUplinkChannelNumber());

        assertTrue(adjacent.isConventionalChannel());
        assertFalse(adjacent.isFailedConditionSite());
        assertTrue(adjacent.isValidSiteInformation());
        assertFalse(adjacent.isActiveNetworkConnectionToRfssControllerSite());
        assertEquals(List.of("CONVENTIONAL CHANNEL", "VALID INFORMATION"), adjacent.getSiteFlags());
    }

    @Test
    public void octet2FlagsAndOctet8ServiceClass()
    {
        //LCO 34 SF=1, LRA 0x12, C=0 F=1 V=0 A=1 and system 0x38D, RFSS 0x05, site 0x0D, channel band 1 number
        //0x234, service class 0x60 (voice and registration)
        LinkControlWord lcw = LinkControlWordFactory.create(message(0x40 | 34, 0x12, 0x53, 0x8D, 0x05, 0x0D, 0x12,
            0x34, 0x60), 0, false);
        LCAdjacentSiteStatusBroadcast adjacent = assertInstanceOf(LCAdjacentSiteStatusBroadcast.class, lcw);

        assertEquals(0x12, adjacent.getLocationRegistrationArea().getValue());
        assertEquals(0x38D, adjacent.getSystem().getValue());
        assertEquals(0x05, adjacent.getRfss().getValue());
        assertEquals(0x0D, adjacent.getSite().getValue());
        P25Channel channel = channel(adjacent.getChannel());
        assertEquals(1, channel.getDownlinkBandIdentifier());
        assertEquals(0x234, channel.getDownlinkChannelNumber());

        assertFalse(adjacent.isConventionalChannel());
        assertTrue(adjacent.isFailedConditionSite());
        assertFalse(adjacent.isValidSiteInformation());
        assertTrue(adjacent.isActiveNetworkConnectionToRfssControllerSite());
        assertEquals(List.of("FAILURE CONDITION", "ACTIVE RFSS CONNECTION"), adjacent.getSiteFlags());
        assertEquals(List.of("VOICE", "REGISTRATION"), adjacent.getSystemServiceClass().getServices());
    }
}
