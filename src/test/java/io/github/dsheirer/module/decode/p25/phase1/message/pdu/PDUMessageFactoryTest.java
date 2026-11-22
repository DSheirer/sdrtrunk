/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

package io.github.dsheirer.module.decode.p25.phase1.message.pdu;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.ip.IPacket;
import io.github.dsheirer.module.decode.ip.ipv4.IPV4Packet;
import io.github.dsheirer.module.decode.ip.mototrbo.ars.ARSPacket;
import io.github.dsheirer.module.decode.ip.mototrbo.ars.RegistrationAcknowledgement;
import io.github.dsheirer.module.decode.ip.udp.UDPPacket;
import io.github.dsheirer.module.decode.p25.phase1.message.P25P1Message;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.block.DataBlock;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.packet.PacketHeader;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.packet.PacketMessage;
import io.github.dsheirer.module.decode.p25.reference.PDUFormat;
import io.github.dsheirer.module.decode.p25.reference.PDUType;
import io.github.dsheirer.module.decode.p25.reference.ServiceAccessPoint;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for PDU header and message creation, in particular that an unconfirmed data header (format 0x15) that
 * carries a packet data SAP is processed as an IP packet and not as unconfirmed multi-block trunking control.
 */
public class PDUMessageFactoryTest
{
    private static final int NAC = 0x460;

    /**
     * Outbound unconfirmed data header: format 0x15, SAP 4 (PACKET DATA), MFID 0, LLID 4006527, 4 blocks to
     * follow, 10 pad octets, data header offset 2.  The header CRC octets are zero; the header factory derives
     * the CRC result from the corrected bit count and does not evaluate them.
     */
    private static final String UNCONFIRMED_SAP_4_HEADER = "3504003D227F840A00020000";
    private static final String UNCONFIRMED_SAP_0_HEADER = "3500003D227F840A00020000";
    private static final String UNCONFIRMED_SAP_6_HEADER = "3506003D227F840A00020000";
    private static final String UNCONFIRMED_SAP_61_HEADER = "353D003D227F840A00020000";
    private static final String UNCONFIRMED_SAP_63_HEADER = "353F003D227F840A00020000";
    /** Same as UNCONFIRMED_SAP_4_HEADER with LLID 4001080 and 5 pad octets */
    private static final String UNCONFIRMED_SAP_4_HEADER_PAD_5 = "3504003D0D38840500020000";
    /** Outbound confirmed data header: format 0x16, SAP 0, LLID 4006527, 4 blocks, 10 pad octets, offset 2 */
    private static final String CONFIRMED_SAP_0_HEADER = "7600003D227F840A00020000";

    /**
     * Unconfirmed (1/2 rate, 12 octet) data blocks captured from a Motorola P25 Phase 1 control channel, as logged
     * by the PDUSequence MSG: field.  SNDCP header 0x4100 (RF UNCONFIRMED DATA, NSAPI 1), IPv4/UDP from
     * 10.51.4.116:49516 to 10.71.213.29:4005, ARS registration acknowledgement (refresh 8 x 30 minutes),
     * 10 pad octets (0xAA) and the 32-bit packet CRC.
     */
    private static final String[] ARS_ACK_BLOCKS = {"410045000020280200008011", "24C00A3304740A47D51DC16C",
        "0FA5000C81AE0002BF08AAAA", "AAAAAAAAAAAAAAAAC0F19102"};

    /**
     * As above, to 10.71.212.139 with a 5 octet trailer (0x04 + 32-bit time) after the ARS extension octet and
     * 5 pad octets.
     */
    private static final String[] ARS_ACK_WITH_TRAILER_BLOCKS = {"4100450000255F7E00008011", "EDD00A3304740A47D48BC16C",
        "0FA5001113E60007BF08046A", "D3E096AAAAAAAAAA8675BA00"};

    @Test
    public void unconfirmedDataHeaderWithPacketDataSapIsPacketHeader()
    {
        for(String hex : new String[]{UNCONFIRMED_SAP_0_HEADER, UNCONFIRMED_SAP_4_HEADER, UNCONFIRMED_SAP_6_HEADER})
        {
            PDUHeader header = PDUHeaderFactory.getPDUHeader(CorrectedBinaryMessage.loadHex(hex));
            assertEquals(PDUFormat.UNCONFIRMED_MULTI_BLOCK_TRUNKING_CONTROL, header.getFormat(), hex);
            assertInstanceOf(PacketHeader.class, header, hex);
            assertFalse(header.isConfirmationRequired(), hex);
            assertTrue(header.isOutbound(), hex);
        }

        PacketHeader header = (PacketHeader)PDUHeaderFactory.getPDUHeader(CorrectedBinaryMessage.loadHex(UNCONFIRMED_SAP_4_HEADER));
        assertEquals(ServiceAccessPoint.PACKET_DATA, header.getServiceAccessPoint());
        assertEquals(4, header.getBlocksToFollowCount());
        assertEquals(10, header.getPadOctetCount());
        assertEquals(2, header.getDataHeaderOffset());
    }

    @Test
    public void unconfirmedDataHeaderWithTrunkingControlSapIsNotPacketHeader()
    {
        for(String hex : new String[]{UNCONFIRMED_SAP_61_HEADER, UNCONFIRMED_SAP_63_HEADER})
        {
            PDUHeader header = PDUHeaderFactory.getPDUHeader(CorrectedBinaryMessage.loadHex(hex));
            assertEquals(PDUFormat.UNCONFIRMED_MULTI_BLOCK_TRUNKING_CONTROL, header.getFormat(), hex);
            assertFalse(header instanceof PacketHeader, hex);

            P25P1Message message = PDUMessageFactory.create(new PDUSequence(header, 0, NAC), NAC, 0);
            assertInstanceOf(PDUSequenceMessage.class, message, hex);
        }
    }

    @Test
    public void confirmedPacketDataHeaderIsPacketHeader()
    {
        PDUHeader header = PDUHeaderFactory.getPDUHeader(CorrectedBinaryMessage.loadHex(CONFIRMED_SAP_0_HEADER));
        assertEquals(PDUFormat.PACKET_DATA, header.getFormat());
        assertInstanceOf(PacketHeader.class, header);
        assertTrue(header.isConfirmationRequired());
    }

    @Test
    public void unconfirmedPacketDataDecodesAsIpPacket()
    {
        PacketMessage packetMessage = createPacketMessage(UNCONFIRMED_SAP_4_HEADER, ARS_ACK_BLOCKS);

        assertEquals(PDUType.OUTBOUND_SNDCP_RF_UNCONFIRMED_DATA, packetMessage.getSNDCPPacketHeader().getPDUType());
        assertEquals(1, packetMessage.getSNDCPPacketHeader().getNSAPI());
        //2 octets SNDCP header + 32 octet IP packet + 10 pad octets + 4 CRC octets = 48 octets = 4 x 12 octet blocks
        assertEquals(32 * 8, packetMessage.getPacketMessage().size());

        RegistrationAcknowledgement acknowledgement = getRegistrationAcknowledgement(packetMessage);
        assertTrue(acknowledgement.isValid());
        assertTrue(acknowledgement.isSuccessful());
        assertEquals(8, acknowledgement.getPayloadValue());

        String text = packetMessage.toString();
        assertTrue(text.contains(" IPPKT "), text);
        assertTrue(text.contains("LLID:4006527"), text);
        assertTrue(text.contains("NSAPI:1"), text);
        assertTrue(text.contains("IP FROM:10.51.4.116 TO:10.71.213.29"), text);
        assertTrue(text.contains("UDP PORT FROM:49516 TO:4005"), text);
        assertTrue(text.contains("ARS REGISTRATION SUCCESS REFRESH IN:240mins"), text);
    }

    @Test
    public void unconfirmedPacketDataWithArsTrailerDecodesAsIpPacket()
    {
        PacketMessage packetMessage = createPacketMessage(UNCONFIRMED_SAP_4_HEADER_PAD_5, ARS_ACK_WITH_TRAILER_BLOCKS);

        //2 octets SNDCP header + 37 octet IP packet + 5 pad octets + 4 CRC octets = 48 octets
        assertEquals(37 * 8, packetMessage.getPacketMessage().size());

        RegistrationAcknowledgement acknowledgement = getRegistrationAcknowledgement(packetMessage);
        assertTrue(acknowledgement.isValid());
        assertTrue(acknowledgement.isSuccessful());
        assertEquals(8, acknowledgement.getPayloadValue());

        String text = packetMessage.toString();
        assertTrue(text.contains("LLID:4001080"), text);
        assertTrue(text.contains("IP FROM:10.51.4.116 TO:10.71.212.139"), text);
        assertTrue(text.contains("UDP PORT FROM:49516 TO:4005"), text);
        assertTrue(text.contains("ARS REGISTRATION SUCCESS REFRESH IN:240mins"), text);
    }

    /**
     * Creates a PDU sequence from the header and data blocks and processes it with the PDU message factory.
     */
    private static PacketMessage createPacketMessage(String headerHex, String[] blocksHex)
    {
        PDUHeader header = PDUHeaderFactory.getPDUHeader(CorrectedBinaryMessage.loadHex(headerHex));
        PDUSequence sequence = new PDUSequence(header, 0, NAC);

        for(String blockHex : blocksHex)
        {
            sequence.addDataBlock(dataBlock(blockHex));
        }

        assertTrue(sequence.isComplete());

        P25P1Message message = PDUMessageFactory.create(sequence, NAC, 0);
        assertInstanceOf(PacketMessage.class, message);
        return (PacketMessage)message;
    }

    private static RegistrationAcknowledgement getRegistrationAcknowledgement(PacketMessage packetMessage)
    {
        IPacket packet = packetMessage.getPacket();
        assertInstanceOf(IPV4Packet.class, packet);
        IPacket udp = ((IPV4Packet)packet).getPayload();
        assertInstanceOf(UDPPacket.class, udp);
        assertEquals(4005, ((UDPPacket)udp).getHeader().getDestinationPort().getValue());
        IPacket ars = ((UDPPacket)udp).getPayload();
        assertInstanceOf(ARSPacket.class, ars);
        assertInstanceOf(RegistrationAcknowledgement.class, ((ARSPacket)ars).getHeader());
        return (RegistrationAcknowledgement)((ARSPacket)ars).getHeader();
    }

    /**
     * Data block that returns an already decoded (1/2 rate, 96-bit) payload.
     */
    private static DataBlock dataBlock(String hex)
    {
        BinaryMessage message = BinaryMessage.loadHex(hex);

        return new DataBlock()
        {
            @Override
            public BinaryMessage getMessage()
            {
                return message;
            }

            @Override
            public int getBitErrorsCount()
            {
                return 0;
            }

            @Override
            public boolean isValid()
            {
                return true;
            }
        };
    }
}
