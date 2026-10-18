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
import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.edac.CRCP25;
import io.github.dsheirer.edac.bch.BCH_63_16_23_P25_Test;
import io.github.dsheirer.edac.trellis.P25_1_2_Node;
import io.github.dsheirer.edac.trellis.P25_3_4_Node;
import io.github.dsheirer.message.DroppedSamplesMessage;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.SyncLossMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.P25P1Message;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.PDUHeader;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.PDUSequence;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.PDUSequenceMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.block.ConfirmedDataBlock;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.block.DataBlock;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.packet.PacketHeader;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.packet.PacketMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.umbtc.UMBTCMessage;
import io.github.dsheirer.module.decode.p25.phase1.sync.P25P1SyncDetector;
import io.github.dsheirer.module.decode.p25.reference.PDUFormat;
import io.github.dsheirer.sample.Listener;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for P25 Phase 1 message framing of Packet Data Units (PDU), in particular that a PDU sequence is assembled
 * with every data block that its header announces.  The framer previously stopped after five data blocks, so a
 * fourteen block confirmed IP packet was always reported as "*INCOMPLETE - RECEIVED 5/14 DATA BLOCKS" and a six
 * block unconfirmed packet as 5/6.
 *
 * The tests synthesize the transmitted dibit stream: frame sync, BCH encoded NID, trellis encoded and interleaved
 * header and data blocks, trailing null bits, and a status symbol after every 35 dibits.  Each PDU is followed by a
 * terminator data unit so that the framer's dibit accounting for the PDU is also verified: a wrong elapsed length
 * for the PDU would produce a sync loss or dropped samples message before the terminator.
 */
public class P25P1MessageFramerTest
{
    private static final int NAC = 0x460;
    private static final int SYNC_AND_NID_BITS = 112;
    private static final int STATUS_SYMBOL_INTERVAL_BITS = 70;
    private static final int BLOCK_BITS = 196;
    private static final Dibit STATUS_DIBIT = Dibit.D01_PLUS_3;
    private static final int SNDCP_OUTBOUND_RF_CONFIRMED_DATA = 5;
    private static final int SNDCP_OUTBOUND_RF_UNCONFIRMED_DATA = 4;
    private static final String FEP_ADDRESS = "10.51.4.23";
    private static final String RADIO_ADDRESS = "10.71.20.10";
    private static final int RADIO_LLID = 4000010;
    private static final int MDLC_PORT = 2002;

    /**
     * First 47 octets of a Motorola MDLC over IP message frame as transmitted on a control channel in a fourteen block
     * confirmed PDU, which was the content of the five data blocks that the framer captured.  Octets 47 onward carry
     * the message text.
     */
    private static final String MDLC_MESSAGE_PREFIX = "400640000900B97A1D400000160000000040020500098644B00001000100000001" +
        "D441000010050000000000000000";

    @Test
    public void pduLengthsMatchFixedDataUnitIds()
    {
        //Header only and header plus one block are the fixed data unit ID entries
        assertEquals(P25P1DataUnitID.PACKET_DATA_UNIT.getMessageLength(), P25P1DataUnitID.getPDUMessageLength(0));
        assertEquals(P25P1DataUnitID.PACKET_DATA_UNIT.getElapsedDibitLength(), P25P1DataUnitID.getPDUElapsedDibitLength(0));
        assertEquals(P25P1DataUnitID.PACKET_DATA_UNIT_BLOCK_1.getMessageLength(), P25P1DataUnitID.getPDUMessageLength(1));
        assertEquals(P25P1DataUnitID.PACKET_DATA_UNIT_BLOCK_1.getElapsedDibitLength(), P25P1DataUnitID.getPDUElapsedDibitLength(1));

        //Values that were previously hard coded for two to five data blocks: 196 bits per block plus null bits
        assertEquals(196 * 3, P25P1DataUnitID.getPDUMessageLength(2));
        assertEquals(360, P25P1DataUnitID.getPDUElapsedDibitLength(2));
        assertEquals(196 * 4 + 14, P25P1DataUnitID.getPDUMessageLength(3));
        assertEquals(468, P25P1DataUnitID.getPDUElapsedDibitLength(3));
        assertEquals(196 * 5 + 28, P25P1DataUnitID.getPDUMessageLength(4));
        assertEquals(576, P25P1DataUnitID.getPDUElapsedDibitLength(4));
        assertEquals(196 * 6 + 42, P25P1DataUnitID.getPDUMessageLength(5));
        assertEquals(684, P25P1DataUnitID.getPDUElapsedDibitLength(5));

        //Fourteen blocks: 112 + 15 * 196 = 3052 transmitted bits, padded to 44 status symbol intervals of 70 bits
        assertEquals(196 * 15 + 28, P25P1DataUnitID.getPDUMessageLength(14));
        assertEquals(44 * 36, P25P1DataUnitID.getPDUElapsedDibitLength(14));

        for(int blocks = 0; blocks <= P25P1DataUnitID.PDU_MAXIMUM_DATA_BLOCK_COUNT; blocks++)
        {
            int transmitted = SYNC_AND_NID_BITS + P25P1DataUnitID.getPDUMessageLength(blocks);
            assertEquals(0, transmitted % STATUS_SYMBOL_INTERVAL_BITS, "blocks: " + blocks);
            assertEquals(transmitted / STATUS_SYMBOL_INTERVAL_BITS * 36, P25P1DataUnitID.getPDUElapsedDibitLength(blocks),
                "blocks: " + blocks);
        }
    }

    @Test
    public void confirmedPduWithThreeDataBlocksIsAssembled()
    {
        assertPduAssembled(true, 3, udpPayload(14));
    }

    @Test
    public void confirmedPduWithFourteenDataBlocksIsAssembled()
    {
        //Motorola MDLC over IP message: 185 octet UDP payload = 215 octet SNDCP/IP/UDP packet, 5 pad octets, CRC
        byte[] payload = new byte[185];
        byte[] prefix = BinaryMessage.loadHex(MDLC_MESSAGE_PREFIX).getBytes();
        System.arraycopy(prefix, 0, payload, 0, prefix.length);
        byte[] text = "   STATION 10 - MDLC MESSAGE TEXT CARRIED IN DATA BLOCKS 6 THROUGH 14".getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(text, 0, payload, prefix.length, text.length);
        for(int x = prefix.length + text.length; x < payload.length; x++)
        {
            payload[x] = ' ';
        }

        PacketMessage packetMessage = assertInstanceOf(PacketMessage.class, assertPduAssembled(true, 14, payload));
        assertEquals(5, packetMessage.getHeader().getPadOctetCount());
    }

    @Test
    public void unconfirmedPduWithSixDataBlocksIsAssembled()
    {
        //Six 12-octet unconfirmed blocks = 72 octets: 2 SNDCP + 28 IP/UDP + 36 payload + 2 pad + 4 CRC
        assertPduAssembled(false, 6, udpPayload(36));
    }

    @Test
    public void confirmedPduWithMaximumDataBlocksIsAssembled()
    {
        //127 blocks x 16 octets = 2032 octets: 2 SNDCP + 28 IP/UDP + 1998 payload + 4 CRC
        assertPduAssembled(true, P25P1DataUnitID.PDU_MAXIMUM_DATA_BLOCK_COUNT, udpPayload(1998));
    }

    /**
     * Frames a PDU carrying the UDP payload in the specified number of data blocks, followed by a terminator data
     * unit, and asserts that the framer dispatches the complete PDU sequence followed by the terminator with no sync
     * loss or dropped samples in between.
     *
     * The PDU is dispatched as a PacketMessage when this build decodes it as an IP packet (confirmed packet data
     * always; unconfirmed packet data only once PDUMessageFactory checks the SAP of format 0x15 headers), otherwise
     * as the raw sequence message of the trunking control path.  Either way every data block must be present.
     *
     * @return the dispatched PDU message
     */
    private static P25P1Message assertPduAssembled(boolean confirmed, int blockCount, byte[] udpPayload)
    {
        byte[] ipPacket = udpOverIpv4(FEP_ADDRESS, RADIO_ADDRESS, MDLC_PORT, udpPayload);
        int blockOctets = confirmed ? 16 : 12;
        byte[] pduPayload = new byte[blockOctets * blockCount];
        int sndcpType = confirmed ? SNDCP_OUTBOUND_RF_CONFIRMED_DATA : SNDCP_OUTBOUND_RF_UNCONFIRMED_DATA;
        pduPayload[0] = (byte)(sndcpType << 4); //SNDCP PDU type and NSAPI 0
        pduPayload[1] = 0; //No IP or UDP header compression
        System.arraycopy(ipPacket, 0, pduPayload, 2, ipPacket.length);
        int padOctets = pduPayload.length - 4 - 2 - ipPacket.length; //Last four octets are the packet CRC
        assertTrue(padOctets >= 0, "Packet does not fit in " + blockCount + " blocks");

        List<BinaryMessage> blocks = new ArrayList<>();
        for(int x = 0; x < blockCount; x++)
        {
            byte[] octets = new byte[blockOctets];
            System.arraycopy(pduPayload, x * blockOctets, octets, 0, blockOctets);
            blocks.add(confirmed ? interleave(encodeThreeQuarterRate(confirmedDataBlock(x, octets))) :
                interleave(encodeHalfRate(unconfirmedDataBlock(octets))));
        }

        BinaryMessage header = interleave(encodeHalfRate(packetHeader(confirmed, RADIO_LLID, blockCount, padOctets)));

        List<Dibit> dibits = new ArrayList<>();
        dibits.addAll(toDibits(frame(P25P1DataUnitID.PACKET_DATA_UNIT, pduMessage(header, blocks))));
        dibits.addAll(toDibits(frame(P25P1DataUnitID.TERMINATOR_DATA_UNIT, new BinaryMessage(28))));
        //Trailing dibits so that the terminator is dispatched (dispatch is delayed until the next non-status dibit)
        for(int x = 0; x < 10; x++)
        {
            dibits.add(Dibit.D00_PLUS_1);
        }

        List<IMessage> messages = frame(dibits);

        P25P1Message pduMessage = null;
        PDUSequence sequence = null;
        for(IMessage message : messages)
        {
            sequence = pduSequenceOf(message);

            if(sequence != null)
            {
                pduMessage = (P25P1Message)message;
                break;
            }
        }
        assertTrue(pduMessage != null, "No PDU message dispatched: " + messages);

        assertTrue(sequence.getHeader().isValid(), pduMessage.toString());
        assertEquals(confirmed, sequence.getHeader().isConfirmationRequired());
        assertEquals(blockCount, sequence.getHeader().getBlocksToFollowCount());
        assertEquals(blockCount, sequence.getDataBlocks().size(), pduMessage.toString());
        assertTrue(sequence.isComplete(), pduMessage.toString());

        for(int x = 0; x < blockCount; x++)
        {
            DataBlock dataBlock = sequence.getDataBlock(x);
            assertTrue(dataBlock.isValid(), "Data block " + x + " invalid");
            assertEquals(0, dataBlock.getBitErrorsCount(), "Data block " + x + " bit errors");

            if(confirmed)
            {
                assertInstanceOf(ConfirmedDataBlock.class, dataBlock);
                assertEquals(x, ((ConfirmedDataBlock)dataBlock).getSequenceNumber(), "Data block " + x + " sequence number");
            }
        }

        String text = pduMessage.toString();
        assertFalse(text.contains("INCOMPLETE"), text);

        if(pduMessage instanceof PacketMessage)
        {
            PacketMessage packetMessage = (PacketMessage)pduMessage;
            assertEquals(hex(pduPayload), packetMessage.getPayloadMessage().toHexString().toUpperCase());
            assertEquals(hex(ipPacket), packetMessage.getPacketMessage().toHexString().toUpperCase());
            assertTrue(text.contains("IPPKT LLID:" + RADIO_LLID + " NSAPI:0"), text);
            assertTrue(text.contains("IP FROM:" + FEP_ADDRESS + " TO:" + RADIO_ADDRESS + " UDP PORT FROM:" + MDLC_PORT +
                " TO:" + MDLC_PORT), text);
            assertTrue(text.contains("UNKNOWN PACKET:" + hex(udpPayload)), text);
        }
        else
        {
            //Not decoded as an IP packet by this build: the raw sequence must still report and carry every block
            assertFalse(confirmed, "Confirmed packet data was not decoded as a packet: " + text);
            assertTrue(text.contains("DATA BLOCKS:" + blockCount), text);
            assertTrue(text.toUpperCase().contains(hex(pduPayload)), text);
        }

        //The terminator that follows the PDU is framed without any sync loss or dropped samples, proving that the
        //framer accounted for the elapsed dibits of the complete PDU.
        List<IMessage> following = messages.subList(messages.indexOf(pduMessage) + 1, messages.size());
        boolean terminator = false;

        for(IMessage message : following)
        {
            assertFalse(message instanceof SyncLossMessage, "Sync loss after PDU: " + following);
            assertFalse(message instanceof DroppedSamplesMessage, "Dropped samples after PDU: " + following);

            if(message instanceof P25P1Message && ((P25P1Message)message).getDUID() == P25P1DataUnitID.TERMINATOR_DATA_UNIT)
            {
                terminator = true;
            }
        }

        assertTrue(terminator, "No terminator after PDU: " + following);

        return pduMessage;
    }

    /**
     * Runs the dibits through a message framer with hard sync detection and collects the dispatched messages.
     */
    private static List<IMessage> frame(List<Dibit> dibits)
    {
        List<IMessage> messages = new ArrayList<>();
        P25P1MessageFramer framer = new P25P1MessageFramer();
        framer.setListener(new Listener<IMessage>()
        {
            @Override
            public void receive(IMessage message)
            {
                messages.add(message);
            }
        });
        framer.start();

        for(Dibit dibit : dibits)
        {
            framer.processWithHardSyncDetect(dibit);
        }

        return messages;
    }

    /**
     * Transmitted bits of a frame: sync pattern, BCH encoded NID for the NAC and DUID, and the message bits.  The
     * message must include the trailing null bits that pad the frame to a multiple of the 70-bit status symbol interval.
     */
    private static BinaryMessage frame(P25P1DataUnitID duid, BinaryMessage message)
    {
        BinaryMessage frame = new BinaryMessage(SYNC_AND_NID_BITS + message.size());
        frame.load(0, 48, P25P1SyncDetector.SYNC_PATTERN);
        frame.load(48, BCH_63_16_23_P25_Test.create(NAC, duid.getValue()));
        frame.load(SYNC_AND_NID_BITS, message);
        assertEquals(0, frame.size() % STATUS_SYMBOL_INTERVAL_BITS, "Frame is not padded to the status symbol interval");
        return frame;
    }

    /**
     * Converts the frame bits to dibits with a status dibit inserted after every 35 dibits.
     */
    private static List<Dibit> toDibits(BinaryMessage frame)
    {
        List<Dibit> dibits = new ArrayList<>();
        int count = 0;

        for(int x = 0; x < frame.size(); x += 2)
        {
            dibits.add(Dibit.fromValue(frame.getInt(x, x + 1)));
            count++;

            if(count % (STATUS_SYMBOL_INTERVAL_BITS / 2) == 0)
            {
                dibits.add(STATUS_DIBIT);
            }
        }

        return dibits;
    }

    /**
     * PDU message bits: interleaved header, interleaved data blocks and trailing null bits that pad the transmitted
     * frame (sync + NID + message) to a multiple of the status symbol interval.
     */
    private static BinaryMessage pduMessage(BinaryMessage header, List<BinaryMessage> blocks)
    {
        int bits = BLOCK_BITS * (1 + blocks.size());
        int transmitted = SYNC_AND_NID_BITS + bits;
        int intervals = (transmitted + STATUS_SYMBOL_INTERVAL_BITS - 1) / STATUS_SYMBOL_INTERVAL_BITS;
        BinaryMessage message = new BinaryMessage(intervals * STATUS_SYMBOL_INTERVAL_BITS - SYNC_AND_NID_BITS);
        message.load(0, header);

        for(int x = 0; x < blocks.size(); x++)
        {
            message.load(BLOCK_BITS * (x + 1), blocks.get(x));
        }

        return message;
    }

    /**
     * Outbound packet data header (96 bits) with SAP 0 (unencrypted user data), data header offset 2 and CRC-CCITT.
     */
    private static BinaryMessage packetHeader(boolean confirmed, int llid, int blocksToFollow, int padOctets)
    {
        BinaryMessage header = new BinaryMessage(96);

        if(confirmed)
        {
            header.set(PDUHeader.CONFIRMATION_REQUIRED_INDICATOR);
        }

        header.set(PDUHeader.PACKET_DIRECTION_INDICATOR); //Outbound
        header.setInt(confirmed ? PDUFormat.PACKET_DATA.getValue() :
            PDUFormat.UNCONFIRMED_MULTI_BLOCK_TRUNKING_CONTROL.getValue(), PDUHeader.PDU_FORMAT);
        header.setInt(0, PacketHeader.SAP_ID);
        header.setInt(0, PDUHeader.VENDOR_ID);
        header.setInt(llid, PDUHeader.LOGICAL_LINK_ID);
        header.set(PacketHeader.FULL_MESSAGE_FLAG);
        header.setInt(blocksToFollow, PDUHeader.BLOCKS_TO_FOLLOW);
        header.setInt(padOctets, PacketHeader.PAD_OCTET_COUNT);
        header.setInt(2, PacketHeader.DATA_HEADER_OFFSET);

        int crc = 0xFFFF;

        for(int x = header.nextSetBit(0); x >= 0 && x < 80; x = header.nextSetBit(x + 1))
        {
            crc ^= CRCP25.CCITT_80_CHECKSUMS[x];
        }

        header.setInt(crc, PDUHeader.PDU_CRC);
        return header;
    }

    /**
     * Confirmed data block (144 bits): 7-bit sequence number, 9-bit CRC and 16 data octets.
     */
    private static BinaryMessage confirmedDataBlock(int sequenceNumber, byte[] octets)
    {
        BinaryMessage block = new BinaryMessage(144);
        block.setInt(sequenceNumber, ConfirmedDataBlock.SEQUENCE_NUMBER);

        for(int x = 0; x < 16; x++)
        {
            block.load(ConfirmedDataBlock.PAYLOAD_START + x * 8, 8, octets[x] & 0xFF);
        }

        int crc = 0;

        for(int x = block.nextSetBit(0); x >= 0 && x < 144; x = block.nextSetBit(x + 1))
        {
            if(x < 7)
            {
                crc ^= CRCP25.CRC9_CHECKSUMS[x];
            }
            else if(x > 15)
            {
                crc ^= CRCP25.CRC9_CHECKSUMS[x - 9];
            }
        }

        block.load(7, 9, crc);
        return block;
    }

    /**
     * Unconfirmed data block (96 bits): 12 data octets.
     */
    private static BinaryMessage unconfirmedDataBlock(byte[] octets)
    {
        BinaryMessage block = new BinaryMessage(96);

        for(int x = 0; x < 12; x++)
        {
            block.load(x * 8, 8, octets[x] & 0xFF);
        }

        return block;
    }

    /**
     * 1/2 rate trellis encodes 96 data bits (48 dibits plus a flushing dibit) into 49 four-bit symbols (196 bits).
     */
    private static BinaryMessage encodeHalfRate(BinaryMessage data)
    {
        assertEquals(96, data.size());
        BinaryMessage encoded = new BinaryMessage(BLOCK_BITS);
        int state = 0;

        for(int x = 0; x < 48; x++)
        {
            int input = data.getInt(x * 2, x * 2 + 1);
            encoded.load(x * 4, 4, P25_1_2_Node.TRANSITION_MATRIX[state][input]);
            state = input;
        }

        encoded.load(48 * 4, 4, P25_1_2_Node.TRANSITION_MATRIX[state][0]);
        return encoded;
    }

    /**
     * 3/4 rate trellis encodes 144 data bits (48 tribits plus a flushing tribit) into 49 four-bit symbols (196 bits).
     */
    private static BinaryMessage encodeThreeQuarterRate(BinaryMessage data)
    {
        assertEquals(144, data.size());
        BinaryMessage encoded = new BinaryMessage(BLOCK_BITS);
        int state = 0;

        for(int x = 0; x < 48; x++)
        {
            int input = data.getInt(x * 3, x * 3 + 2);
            encoded.load(x * 4, 4, P25_3_4_Node.TRANSITION_MATRIX[state][input]);
            state = input;
        }

        encoded.load(48 * 4, 4, P25_3_4_Node.TRANSITION_MATRIX[state][0]);
        return encoded;
    }

    /**
     * Interleaves a 196-bit encoded block, the inverse of P25P1Interleave.deinterleaveChunk().
     */
    private static BinaryMessage interleave(BinaryMessage encoded)
    {
        BinaryMessage interleaved = new BinaryMessage(BLOCK_BITS);

        for(int x = 0; x < BLOCK_BITS; x++)
        {
            if(encoded.get(P25P1Interleave.DATA_DEINTERLEAVE[x]))
            {
                interleaved.set(x);
            }
        }

        return interleaved;
    }

    /**
     * UDP payload of the specified length with recognizable content.
     */
    private static byte[] udpPayload(int length)
    {
        byte[] payload = new byte[length];

        for(int x = 0; x < length; x++)
        {
            payload[x] = (byte)('A' + (x % 26));
        }

        return payload;
    }

    /**
     * IPv4 packet with a UDP datagram from and to the port, from the source to the destination address.
     */
    private static byte[] udpOverIpv4(String source, String destination, int port, byte[] payload)
    {
        int udpLength = 8 + payload.length;
        int totalLength = 20 + udpLength;
        byte[] packet = new byte[totalLength];
        packet[0] = 0x45; //Version 4, header length 5 words
        packet[2] = (byte)(totalLength >> 8);
        packet[3] = (byte)totalLength;
        packet[8] = 64; //TTL
        packet[9] = 17; //UDP
        System.arraycopy(address(source), 0, packet, 12, 4);
        System.arraycopy(address(destination), 0, packet, 16, 4);

        int sum = 0;

        for(int x = 0; x < 20; x += 2)
        {
            sum += ((packet[x] & 0xFF) << 8) | (packet[x + 1] & 0xFF);
        }

        while((sum >> 16) != 0)
        {
            sum = (sum & 0xFFFF) + (sum >> 16);
        }

        int checksum = ~sum & 0xFFFF;
        packet[10] = (byte)(checksum >> 8);
        packet[11] = (byte)checksum;

        packet[20] = (byte)(port >> 8);
        packet[21] = (byte)port;
        packet[22] = (byte)(port >> 8);
        packet[23] = (byte)port;
        packet[24] = (byte)(udpLength >> 8);
        packet[25] = (byte)udpLength;
        System.arraycopy(payload, 0, packet, 28, payload.length);
        return packet;
    }

    private static byte[] address(String dotted)
    {
        String[] parts = dotted.split("\\.");
        byte[] address = new byte[4];

        for(int x = 0; x < 4; x++)
        {
            address[x] = (byte)Integer.parseInt(parts[x]);
        }

        return address;
    }

    /**
     * PDU sequence carried by a dispatched message, or null when the message is not a PDU.
     */
    private static PDUSequence pduSequenceOf(IMessage message)
    {
        if(message instanceof PacketMessage)
        {
            return ((PacketMessage)message).getPDUSequence();
        }
        else if(message instanceof PDUSequenceMessage)
        {
            return ((PDUSequenceMessage)message).getPDUSequence();
        }
        else if(message instanceof UMBTCMessage)
        {
            return ((UMBTCMessage)message).getPDUSequence();
        }

        return null;
    }

    private static String hex(byte[] octets)
    {
        StringBuilder sb = new StringBuilder();

        for(byte octet : octets)
        {
            sb.append(String.format("%02X", octet & 0xFF));
        }

        return sb.toString();
    }
}
