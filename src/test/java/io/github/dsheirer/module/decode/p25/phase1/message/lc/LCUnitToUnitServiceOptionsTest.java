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
import io.github.dsheirer.identifier.radio.FullyQualifiedRadioIdentifier;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCSourceIDExtension;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCUnitToUnitAnswerRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCUnitToUnitVoiceChannelUserExtended;
import io.github.dsheirer.module.decode.p25.reference.VoiceServiceOptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Field positions of the unit-to-unit link control words whose service options aren't in octet 2.  TIA-102.AABF-D
 * 7.3.5 Unit to Unit Answer Request: octet 1 service options, octet 2 reserved with S in bit 0.  7.3.29 Unit to Unit
 * Voice Channel User - Extended: octet 1 reserved with S in bit 0, octet 2 service options.  Both: octets 3-5 target,
 * octets 6-8 source.
 */
public class LCUnitToUnitServiceOptionsTest
{
    //Emergency, encrypted, priority 5
    private static final int SERVICE_OPTIONS = 0xC5;
    private static final int TARGET = 0x123456;
    private static final int SOURCE = 0x789ABC;

    /**
     * Creates a standard (SF=1) link control word.
     */
    private static CorrectedBinaryMessage linkControl(int opcode, int octet1, int octet2, int target, int source)
    {
        CorrectedBinaryMessage message = new CorrectedBinaryMessage(72);
        message.load(0, 8, 0x40 | opcode);
        message.load(8, 8, octet1);
        message.load(16, 8, octet2);
        message.load(24, 24, target);
        message.load(48, 24, source);
        return message;
    }

    private static void assertServiceOptions(VoiceServiceOptions serviceOptions)
    {
        assertTrue(serviceOptions.isEmergency());
        assertTrue(serviceOptions.isEncrypted());
        assertEquals(5, serviceOptions.getPriority());
    }

    @Test
    public void unitToUnitAnswerRequest()
    {
        //S = 0
        LinkControlWord lcw = LinkControlWordFactory.create(linkControl(5, SERVICE_OPTIONS, 0x00, TARGET, SOURCE),
                0, false);
        LCUnitToUnitAnswerRequest request = assertInstanceOf(LCUnitToUnitAnswerRequest.class, lcw);

        assertServiceOptions(request.getServiceOptions());
        assertEquals(TARGET, request.getTargetAddress().getValue());
        assertEquals(SOURCE, request.getSourceAddress().getValue());
    }

    @Test
    public void unitToUnitVoiceChannelUserExtended()
    {
        //S = 1, always for this message
        LinkControlWord lcw = LinkControlWordFactory.create(linkControl(10, 0x01, SERVICE_OPTIONS, TARGET, SOURCE),
                0, false);
        LCUnitToUnitVoiceChannelUserExtended user = assertInstanceOf(LCUnitToUnitVoiceChannelUserExtended.class, lcw);

        assertServiceOptions(user.getServiceOptions());
        assertEquals(TARGET, user.getTargetAddress().getValue());

        //Source ID Extension (AABF-D 7.3.32): octet 1 reserved, network ID octets 2-4, system ID octets 4-5,
        //source ID octets 6-8
        CorrectedBinaryMessage extension = new CorrectedBinaryMessage(72);
        extension.load(0, 8, 0x40 | 9);
        extension.load(16, 20, 0xBEE00);
        extension.load(36, 12, 0x2A1);
        extension.load(48, 24, 0x00ABCD);
        user.setSourceIDExtension(assertInstanceOf(LCSourceIDExtension.class,
                LinkControlWordFactory.create(extension, 0, false)));

        FullyQualifiedRadioIdentifier source = user.getSourceAddress();
        assertEquals(SOURCE, source.getValue());
        assertEquals(0xBEE00, source.getWacn());
        assertEquals(0x2A1, source.getSystem());
        assertEquals(0x00ABCD, source.getRadio());
        assertFalse(user.getIdentifiers().isEmpty());
    }
}
