package io.github.dsheirer.module.decode.p25.message.pdu.header;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.CRCP25;
import io.github.dsheirer.module.decode.p25.message.pdu.ambtc.AMBTCHeader;
import io.github.dsheirer.module.decode.p25.reference.PDUFormat;

public class PDUHeaderFactory
{
    public static PDUHeader getPDUHeader(CorrectedBinaryMessage correctedBinaryMessage)
    {
        //CCITT-16 can detect and correct up to 1 bit error max - 2 bit errors indicates CRC-fail
        int errorCount = CRCP25.correctCCITT80(correctedBinaryMessage, 0, 80);
        boolean passesCRC = errorCount < 2;

        correctedBinaryMessage.incrementCorrectedBitCount(errorCount);

        PDUFormat format = PDUHeader.getFormat(correctedBinaryMessage);

        switch(format)
        {
            case ALTERNATE_MULTI_BLOCK_TRUNKING_CONTROL:
                return new AMBTCHeader(correctedBinaryMessage, passesCRC);
            case PACKET_DATA:
                return new PacketHeader(correctedBinaryMessage, passesCRC);
            case RESPONSE_PACKET_HEADER_FORMAT:
                return new ResponseHeader(correctedBinaryMessage, passesCRC);
            case UNCONFIRMED_MULTI_BLOCK_TRUNKING_CONTROL:
            default:
                return new PDUHeader(correctedBinaryMessage, passesCRC);
        }
    }
}
