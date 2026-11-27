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

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.event.IDecodeEvent;
import io.github.dsheirer.module.decode.p25.P25TrafficChannelManager;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25Channel;
import io.github.dsheirer.module.decode.p25.identifier.channel.P25ExplicitChannel;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25FullyQualifiedRadioIdentifier;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBand;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.PDUHeader;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.AMBTCHeader;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.AMBTCMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.AMBTCUnitToUnitVoiceServiceChannelGrant;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.block.UnconfirmedDataBlock;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.Opcode;
import io.github.dsheirer.module.decode.p25.reference.Direction;
import io.github.dsheirer.module.decode.p25.reference.Duplex;
import io.github.dsheirer.module.decode.p25.reference.PDUFormat;
import io.github.dsheirer.module.decode.p25.reference.ServiceAccessPoint;
import io.github.dsheirer.module.decode.p25.reference.SessionMode;
import io.github.dsheirer.module.decode.p25.reference.Vendor;
import io.github.dsheirer.module.decode.p25.reference.VoiceServiceOptions;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static io.github.dsheirer.module.decode.p25.phase1.P25P1MessageFramerPDUTest.encodeHalfRate;
import static io.github.dsheirer.module.decode.p25.phase1.P25P1MessageFramerPDUTest.frame;
import static io.github.dsheirer.module.decode.p25.phase1.P25P1MessageFramerPDUTest.interleave;
import static io.github.dsheirer.module.decode.p25.phase1.P25P1MessageFramerPDUTest.pduMessage;
import static io.github.dsheirer.module.decode.p25.phase1.P25P1MessageFramerPDUTest.setHeaderCRC;
import static io.github.dsheirer.module.decode.p25.phase1.P25P1MessageFramerPDUTest.setPacketCRC;
import static io.github.dsheirer.module.decode.p25.phase1.P25P1MessageFramerPDUTest.toDibits;
import static io.github.dsheirer.module.decode.p25.phase1.P25P1MessageFramerPDUTest.unconfirmedDataBlock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that the packet CRC-32 (TIA-102.BAAA-B 5.3.3) of an alternate multi-block trunking control (AMBTC) packet is
 * checked, so that an AMBTC with a damaged data block is invalid and its channel grant is not acted on.
 *
 * The header and the two data blocks are 1/2 rate trellis encoded, interleaved and framed with the message framer.  A
 * damaged data block is simulated by changing a data bit before trellis encoding, as when the Viterbi decoder settles
 * on a wrong path that it can't detect.
 */
public class P25P1PacketCRCTest
{
    private static final int NAC = 0x460;
    private static final int SERVICE_OPTIONS = 0xA4; //Emergency, full duplex, circuit, priority 4
    private static final int SOURCE_LOCAL_ADDRESS = 1234567;
    private static final int SOURCE_WACN = 0xBEE00;
    private static final int SOURCE_SYSTEM = 0x3A1;
    private static final int SOURCE_ID = 1000001;
    private static final int TARGET_LOCAL_ADDRESS = 43981;
    private static final int TARGET_WACN = 0x91F2E;
    private static final int TARGET_SYSTEM = 0x2B7;
    private static final int TARGET_ID = 8000001;
    private static final int DOWNLINK_BAND = 1;
    private static final int DOWNLINK_CHANNEL = 0x123;
    private static final int UPLINK_BAND = 2;
    private static final int UPLINK_CHANNEL = 0x456;
    private static final long BASE_FREQUENCY = 851_000_000L;
    private static final long CHANNEL_SPACING = 6_250L;

    /**
     * Data octets of the two data blocks.  Block 0: source WACN, system and ID, target address, downlink band and
     * channel.  Block 1: uplink band and channel, low 12 bits of the target WACN, target system and ID, and the
     * packet CRC in the last four octets.
     */
    private static byte[] dataOctets()
    {
        BinaryMessage data = new BinaryMessage(192);
        data.load(0, 20, SOURCE_WACN);
        data.load(20, 12, SOURCE_SYSTEM);
        data.load(32, 24, SOURCE_ID);
        data.load(56, 24, TARGET_LOCAL_ADDRESS);
        data.load(80, 4, DOWNLINK_BAND);
        data.load(84, 12, DOWNLINK_CHANNEL);
        data.load(96, 4, UPLINK_BAND);
        data.load(100, 12, UPLINK_CHANNEL);
        data.load(112, 12, TARGET_WACN & 0xFFF);
        data.load(124, 12, TARGET_SYSTEM);
        data.load(136, 24, TARGET_ID);

        byte[] octets = new byte[24];
        for(int x = 0; x < octets.length; x++)
        {
            octets[x] = (byte)data.getInt(x * 8, x * 8 + 7);
        }

        setPacketCRC(octets);
        return octets;
    }

    /**
     * AMBTC header for an outbound unit-to-unit voice channel grant with two data blocks to follow.
     */
    private static BinaryMessage header()
    {
        BinaryMessage header = new BinaryMessage(96);
        header.set(PDUHeader.PACKET_DIRECTION_INDICATOR);
        header.setInt(PDUFormat.ALTERNATE_MULTI_BLOCK_TRUNKING_CONTROL.getValue(), PDUHeader.PDU_FORMAT);
        header.set(8);
        header.set(9);
        header.setInt(ServiceAccessPoint.UNENCRYPTED_TRUNKING_CONTROL.getValue(), AMBTCHeader.SAP_ID);
        header.setInt(0, PDUHeader.VENDOR_ID);
        header.setInt(SOURCE_LOCAL_ADDRESS, PDUHeader.LOGICAL_LINK_ID);
        header.set(48);
        header.setInt(2, PDUHeader.BLOCKS_TO_FOLLOW);
        header.setInt(Opcode.OSP_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT.getCode(), AMBTCHeader.OPCODE);
        header.load(64, 8, SERVICE_OPTIONS);
        header.load(72, 8, TARGET_WACN >> 12);
        setHeaderCRC(header);
        return header;
    }

    /**
     * Frames the header and data octets as a PDU followed by a terminator and returns the AMBTC message.
     */
    private static AMBTCMessage frameAMBTC(BinaryMessage header, byte[] dataOctets)
    {
        List<BinaryMessage> blocks = new ArrayList<>();
        for(int x = 0; x < dataOctets.length / 12; x++)
        {
            byte[] octets = new byte[12];
            System.arraycopy(dataOctets, x * 12, octets, 0, 12);
            blocks.add(interleave(encodeHalfRate(unconfirmedDataBlock(octets))));
        }

        List<Dibit> dibits = new ArrayList<>();
        dibits.addAll(toDibits(frame(P25P1DataUnitID.PACKET_DATA_UNIT, pduMessage(interleave(encodeHalfRate(header)), blocks))));
        dibits.addAll(toDibits(frame(P25P1DataUnitID.TERMINATOR_DATA_UNIT, new BinaryMessage(28))));
        for(int x = 0; x < 10; x++)
        {
            dibits.add(Dibit.D00_PLUS_1);
        }

        List<AMBTCMessage> ambtcs = new ArrayList<>();
        for(IMessage message : frame(dibits))
        {
            if(message instanceof AMBTCMessage ambtc)
            {
                ambtcs.add(ambtc);
            }
        }

        assertEquals(1, ambtcs.size(), "AMBTC messages: " + ambtcs);
        return ambtcs.get(0);
    }

    @Test
    public void ambtcWithCorrectPacketCRCIsValid()
    {
        AMBTCMessage ambtc = frameAMBTC(header(), dataOctets());
        String text = ambtc.toString();

        assertTrue(ambtc.isValid(), text);
        assertTrue(ambtc.getPDUSequence().isComplete(), text);
        assertTrue(ambtc.getPDUSequence().isPacketCRCValid(), text);
        assertFalse(text.contains("CRC-FAILED"), text);
        assertEquals(0, ambtc.getBitErrorsCount(), text);
        assertEquals(NAC, ((Number)ambtc.getNAC().getValue()).intValue());

        AMBTCHeader header = ambtc.getHeader();
        assertTrue(header.isValid());
        assertEquals(PDUFormat.ALTERNATE_MULTI_BLOCK_TRUNKING_CONTROL, header.getFormat());
        assertEquals(Direction.OUTBOUND, header.getDirection());
        assertFalse(header.isConfirmationRequired());
        assertEquals(ServiceAccessPoint.UNENCRYPTED_TRUNKING_CONTROL, header.getServiceAccessPoint());
        assertEquals(Vendor.STANDARD, header.getVendor());
        assertEquals(2, header.getBlocksToFollowCount());
        assertEquals(Opcode.OSP_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT, header.getOpcode());

        assertEquals(2, ambtc.getPDUSequence().getDataBlocks().size());
        assertInstanceOf(UnconfirmedDataBlock.class, ambtc.getDataBlock(0));
        assertInstanceOf(UnconfirmedDataBlock.class, ambtc.getDataBlock(1));

        AMBTCUnitToUnitVoiceServiceChannelGrant grant = assertInstanceOf(AMBTCUnitToUnitVoiceServiceChannelGrant.class, ambtc);

        VoiceServiceOptions serviceOptions = grant.getServiceOptions();
        assertTrue(serviceOptions.isEmergency());
        assertFalse(serviceOptions.isEncrypted());
        assertEquals(Duplex.FULL, serviceOptions.getDuplex());
        assertEquals(SessionMode.CIRCUIT, serviceOptions.getSessionMode());
        assertEquals(4, serviceOptions.getPriority());

        APCO25FullyQualifiedRadioIdentifier source = grant.getSourceAddress();
        assertEquals(Role.FROM, source.getRole());
        assertEquals(SOURCE_LOCAL_ADDRESS, source.getValue());
        assertEquals(SOURCE_WACN, source.getWacn());
        assertEquals(SOURCE_SYSTEM, source.getSystem());
        assertEquals(SOURCE_ID, source.getRadio());

        APCO25FullyQualifiedRadioIdentifier target = assertInstanceOf(APCO25FullyQualifiedRadioIdentifier.class,
            grant.getTargetAddress());
        assertEquals(Role.TO, target.getRole());
        assertEquals(TARGET_LOCAL_ADDRESS, target.getValue());
        assertEquals(TARGET_WACN, target.getWacn());
        assertEquals(TARGET_SYSTEM, target.getSystem());
        assertEquals(TARGET_ID, target.getRadio());

        P25ExplicitChannel channel = assertInstanceOf(P25ExplicitChannel.class, grant.getChannel().getValue());
        assertEquals(DOWNLINK_BAND, channel.getDownlinkBandIdentifier());
        assertEquals(DOWNLINK_CHANNEL, channel.getDownlinkChannelNumber());
        assertEquals(UPLINK_BAND, channel.getUplinkBandIdentifier());
        assertEquals(UPLINK_CHANNEL, channel.getUplinkChannelNumber());
    }

    @Test
    public void ambtcWithDamagedDataBlockIsInvalid()
    {
        byte[] octets = dataOctets();
        octets[11] ^= 0x01; //Block 0 downlink channel number LSB: the grant now points to another channel
        AMBTCMessage ambtc = frameAMBTC(header(), octets);
        String text = ambtc.toString();

        assertTrue(ambtc.getHeader().isValid(), text);
        assertTrue(ambtc.getPDUSequence().isComplete(), text);
        assertEquals(0, ambtc.getBitErrorsCount(), "The trellis decoder can't see this error: " + text);
        assertEquals(DOWNLINK_CHANNEL ^ 1, ((AMBTCUnitToUnitVoiceServiceChannelGrant)ambtc).getChannel().getValue()
            .getDownlinkChannelNumber(), text);

        assertFalse(ambtc.getPDUSequence().isPacketCRCValid(), text);
        assertFalse(ambtc.isValid(), text);
        assertTrue(text.contains("CRC-FAILED"), text);
    }

    @Test
    public void ambtcWithDamagedSecondDataBlockIsInvalid()
    {
        byte[] octets = dataOctets();
        octets[14] ^= 0x80; //Block 1 low 12 bits of the target WACN
        AMBTCMessage ambtc = frameAMBTC(header(), octets);

        assertFalse(ambtc.getPDUSequence().isPacketCRCValid(), ambtc.toString());
        assertFalse(ambtc.isValid(), ambtc.toString());
    }

    @Test
    public void ambtcWithDamagedPacketCRCIsInvalid()
    {
        byte[] octets = dataOctets();
        octets[23] ^= 0x01;
        AMBTCMessage ambtc = frameAMBTC(header(), octets);

        assertFalse(ambtc.isValid(), ambtc.toString());
    }

    @Test
    public void ambtcWithFailedHeaderCRCIsInvalid()
    {
        BinaryMessage header = header();
        header.flip(30); //Source address, after the header CRC was calculated
        header.flip(40); //Two errors: the CRC-CCITT detects them, but can't correct them
        AMBTCMessage ambtc = frameAMBTC(header, dataOctets());

        assertFalse(ambtc.getHeader().isValid(), ambtc.toString());
        assertFalse(ambtc.isValid(), ambtc.toString());
        assertTrue(ambtc.toString().contains("CRC-FAILED"), ambtc.toString());
    }

    @Test
    public void decoderStateIgnoresGrantWithDamagedDataBlock()
    {
        assertFalse(decodeEvents(frameAMBTC(header(), dataOctets())).isEmpty(),
            "Valid grant produced no decode event");

        byte[] octets = dataOctets();
        octets[11] ^= 0x01;
        assertTrue(decodeEvents(frameAMBTC(header(), octets)).isEmpty(), "Damaged grant produced a decode event");
    }

    /**
     * Processes the grant with a decoder state and traffic channel manager and returns the decode events.
     */
    private static List<IDecodeEvent> decodeEvents(AMBTCMessage ambtc)
    {
        //The message processor provides the frequency band for the channel from the band identifier messages
        APCO25Channel channel = ((AMBTCUnitToUnitVoiceServiceChannelGrant)ambtc).getChannel();
        channel.setFrequencyBand(band(DOWNLINK_BAND));
        assertNotNull(channel.getValue().getFrequencyBand());

        List<IDecodeEvent> events = new ArrayList<>();
        Channel control = new Channel("Test");
        control.setDecodeConfiguration(new DecodeConfigP25Phase1());
        P25TrafficChannelManager trafficChannelManager = new P25TrafficChannelManager(control);
        trafficChannelManager.addDecodeEventListener(events::add);
        P25P1DecoderState decoderState = new P25P1DecoderState(control, trafficChannelManager);
        decoderState.setDecoderStateListener(event -> {});
        decoderState.addDecodeEventListener(events::add);
        decoderState.start();
        decoderState.receive(ambtc);
        return events;
    }

    private static IFrequencyBand band(int identifier)
    {
        return new IFrequencyBand()
        {
            @Override public boolean isValid() {return true;}
            @Override public int getIdentifier() {return identifier;}
            @Override public long getChannelSpacing() {return CHANNEL_SPACING;}
            @Override public long getBaseFrequency() {return BASE_FREQUENCY;}
            @Override public int getBandwidth() {return 12500;}
            @Override public long getTransmitOffset() {return -45_000_000L;}
            @Override public long getDownlinkFrequency(int channelNumber) {return BASE_FREQUENCY + channelNumber * CHANNEL_SPACING;}
            @Override public long getUplinkFrequency(int channelNumber) {return getDownlinkFrequency(channelNumber) + getTransmitOffset();}
            @Override public boolean isTDMA() {return false;}
            @Override public int getTimeslotCount() {return 1;}
        };
    }
}
