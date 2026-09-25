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

package io.github.dsheirer.module.decode.p25.phase1;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.FrequencyBandUpdate;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.GroupVoiceChannelGrant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Channel grants resolve their frequency from the band plan (IDEN_UPDATE) the processor holds for the grant's band
 * identifier.  receive() dispatches CRC-failed messages as well as valid ones, and a failed IDEN_UPDATE still parses,
 * with a corrupt base and spacing, so the band map must only ever be written by a valid message: otherwise every
 * grant on that identifier resolves to a phantom frequency until the next valid IDEN_UPDATE (#2298).
 */
public class P25P1MessageProcessorFrequencyBandTest
{
    private static final int NAC = 0x293;
    private static final int BAND = 0;
    private static final int CHANNEL = 451;
    private static final long BASE = 851_006_250;
    private static final long SPACING = 6_250;
    private static final long CORRUPT_BASE = 6_218_179_370L;
    private static final long CORRUPT_SPACING = 2_625;

    /**
     * An OSP IDEN_UPDATE (FDMA) for the band identifier, loaded at the field positions FrequencyBandUpdate reads:
     * identifier 16-19, bandwidth 20-28 (125 Hz units), transmit offset sign 29 and value 30-37 (250 kHz units),
     * spacing 38-47 (125 Hz units), base 48-79 (5 Hz units).
     */
    private static FrequencyBandUpdate idenUpdate(long base, long spacing, boolean valid)
    {
        CorrectedBinaryMessage message = new CorrectedBinaryMessage(96);
        message.load(16, 4, BAND);
        message.load(20, 9, (int)(12_500 / 125));
        message.load(29, 1, 0);
        message.load(30, 8, (int)(45_000_000 / 250_000));
        message.load(38, 10, (int)(spacing / 125));
        message.load(48, 32, (int)(base / 5));
        FrequencyBandUpdate update = new FrequencyBandUpdate(P25P1DataUnitID.TRUNKING_SIGNALING_BLOCK_1, message, NAC, 0);
        update.setValid(valid);
        return update;
    }

    /**
     * An OSP group voice channel grant: band 24-27, channel 28-39, group 40-55, source 56-79.
     */
    private static GroupVoiceChannelGrant grant()
    {
        CorrectedBinaryMessage message = new CorrectedBinaryMessage(96);
        message.load(24, 4, BAND);
        message.load(28, 12, CHANNEL);
        message.load(40, 16, 1234);
        message.load(56, 24, 5678);
        return new GroupVoiceChannelGrant(P25P1DataUnitID.TRUNKING_SIGNALING_BLOCK_1, message, NAC, 0);
    }

    /**
     * Feeds the messages through a fresh processor and returns the dispatched grant's downlink frequency.
     */
    private static long grantFrequency(IMessage... messages)
    {
        P25P1MessageProcessor processor = new P25P1MessageProcessor();
        List<IMessage> dispatched = new ArrayList<>();
        processor.setMessageListener(dispatched::add);

        for(IMessage message : messages)
        {
            processor.receive(message);
        }

        processor.receive(grant());

        for(IMessage message : dispatched)
        {
            if(message instanceof GroupVoiceChannelGrant grant)
            {
                return grant.getChannel().getDownlinkFrequency();
            }
        }

        throw new IllegalStateException("grant was not dispatched");
    }

    @Test
    public void validBandResolvesTheGrant()
    {
        assertEquals(BASE + CHANNEL * SPACING, grantFrequency(idenUpdate(BASE, SPACING, true)));
    }

    @Test
    public void failedBandDoesNotReplaceAValidOne()
    {
        //A CRC-failed IDEN_UPDATE as decoded on a live control channel: base 6.2 GHz, spacing 2.625 kHz.
        long frequency = grantFrequency(idenUpdate(BASE, SPACING, true), idenUpdate(CORRUPT_BASE, CORRUPT_SPACING, false));
        assertEquals(BASE + CHANNEL * SPACING, frequency);
    }

    @Test
    public void laterValidBandReplacesAnEarlierOne()
    {
        long other = 762_006_250;
        long frequency = grantFrequency(idenUpdate(BASE, SPACING, true), idenUpdate(other, SPACING, true));
        assertEquals(other + CHANNEL * SPACING, frequency);
    }

    @Test
    public void failedBandAloneLeavesTheGrantUnresolved()
    {
        assertEquals(0, grantFrequency(idenUpdate(CORRUPT_BASE, CORRUPT_SPACING, false)));
    }
}
