/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.module.decode.dmr.message.data;

import io.github.dsheirer.bits.BitSetFullException;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.BPTC_196_96;
import io.github.dsheirer.edac.CRCDMR;
import io.github.dsheirer.edac.trellis.ViterbiDecoder_3_4_DMR;
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.data.block.DataBlock1Rate;
import io.github.dsheirer.module.decode.dmr.message.data.block.DataBlock1_2Rate;
import io.github.dsheirer.module.decode.dmr.message.data.block.DataBlock3_4Rate;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.CSBKMessageFactory;
import io.github.dsheirer.module.decode.dmr.message.data.header.ConfirmedDataHeader;
import io.github.dsheirer.module.decode.dmr.message.data.header.DataHeader;
import io.github.dsheirer.module.decode.dmr.message.data.header.DefinedShortDataHeader;
import io.github.dsheirer.module.decode.dmr.message.data.header.HeaderMessage;
import io.github.dsheirer.module.decode.dmr.message.data.header.MBCHeader;
import io.github.dsheirer.module.decode.dmr.message.data.header.ProprietaryDataHeader;
import io.github.dsheirer.module.decode.dmr.message.data.header.RawShortDataHeader;
import io.github.dsheirer.module.decode.dmr.message.data.header.ResponseDataHeader;
import io.github.dsheirer.module.decode.dmr.message.data.header.ShortDataHeader;
import io.github.dsheirer.module.decode.dmr.message.data.header.StatusDataHeader;
import io.github.dsheirer.module.decode.dmr.message.data.header.UDTHeader;
import io.github.dsheirer.module.decode.dmr.message.data.header.UnconfirmedDataHeader;
import io.github.dsheirer.module.decode.dmr.message.data.header.VoiceHeader;
import io.github.dsheirer.module.decode.dmr.message.data.header.hytera.HyteraProprietaryDataHeader;
import io.github.dsheirer.module.decode.dmr.message.data.header.motorola.MNISProprietaryDataHeader;
import io.github.dsheirer.module.decode.dmr.message.data.header.motorola.MotorolaProprietaryDataHeader;
import io.github.dsheirer.module.decode.dmr.message.data.mbc.MBCContinuationBlock;
import io.github.dsheirer.module.decode.dmr.message.data.terminator.Terminator;
import io.github.dsheirer.module.decode.dmr.message.data.usb.USBData;
import io.github.dsheirer.module.decode.dmr.message.type.DataPacketFormat;
import io.github.dsheirer.module.decode.dmr.message.type.ServiceAccessPoint;
import io.github.dsheirer.module.decode.dmr.message.type.Vendor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating data messages that contain a 196-bit BPTC protected message.
 */
public class DMRDataMessageFactory
{
    private final static Logger mLog = LoggerFactory.getLogger(DMRDataMessageFactory.class);
    private static final ViterbiDecoder_3_4_DMR VITERBI_DECODER = new ViterbiDecoder_3_4_DMR();

    /**
     * Creates a data message class
     * @param pattern for the DMR burst
     * @param message DMR burst as transmitted
     * @param cach from the DMR burst
     * @param timestamp for the message
     * @param timeslot for the message
     * @return data message instance
     */
    public static DataMessage create(DMRSyncPattern pattern, CorrectedBinaryMessage message, CACH cach, long timestamp,
                                     int timeslot)
    {
        SlotType slotType = SlotType.getSlotType(message);

        if(slotType.isValid())
        {
            switch(slotType.getDataType())
            {
                case SLOT_IDLE:
                    return new IDLEMessage(pattern, getPayload(message), cach, slotType, timestamp, timeslot);
                case CSBK:
                    return CSBKMessageFactory.create(pattern, getPayload(message), cach, slotType, timestamp, timeslot);
                case USB_DATA:
                    CorrectedBinaryMessage usbdPayload = getPayload(message);
                    switch(USBData.getServiceType(usbdPayload))
                    {
                        case LIP_SHORT_LOCATION_REQUEST:
                        default:
                            return new USBData(pattern, getPayload(message), cach, slotType, timestamp, timeslot);
                    }
                case MBC_ENC_HEADER:
                case MBC_HEADER:
                    return new MBCHeader(pattern, getPayload(message), cach, slotType, timestamp, timeslot);
                case CHANNEL_CONTROL_ENC_HEADER:
                case PI_HEADER:
                    return new HeaderMessage(pattern, getPayload(message), cach, slotType, timestamp, timeslot);
                case VOICE_HEADER:
                    return new VoiceHeader(pattern, getPayload(message), cach, slotType, timestamp, timeslot);
                case DATA_ENC_HEADER:
                case DATA_HEADER:
                    CorrectedBinaryMessage payload = getPayload(message);
                    int crcBitCount = CRCDMR.correctCCITT80(payload, 0, 80, 0xCCCC);
                    boolean valid = crcBitCount < 2;
                    DataPacketFormat dpf = DataHeader.getDataPacketFormat(payload);

                    switch(dpf)
                    {
                        case CONFIRMED_DATA_PACKET:
                            ConfirmedDataHeader cdh = new ConfirmedDataHeader(pattern, payload, cach, slotType, timestamp, timeslot);
                            cdh.setValid(valid);
                            return cdh;
                        case PROPRIETARY_DATA_PACKET:
                            Vendor vendor = ProprietaryDataHeader.getVendor(payload);

                            switch(vendor)
                            {
                                case MOTOROLA_CAPACITY_PLUS:
                                case MOTOROLA_CONNECT_PLUS:
                                    ServiceAccessPoint sap = ProprietaryDataHeader.getServiceAccessPoint(payload);

                                    if(sap == ServiceAccessPoint.SAP_1)
                                    {
                                        MNISProprietaryDataHeader mpdh = new MNISProprietaryDataHeader(pattern, payload,
                                            cach, slotType, timestamp, timeslot);
                                        mpdh.setValid(valid);
                                        return mpdh;
                                    }
                                    else
                                    {
                                        MotorolaProprietaryDataHeader mprdh = new MotorolaProprietaryDataHeader(pattern,
                                            payload, cach, slotType, timestamp, timeslot);
                                        mprdh.setValid(valid);
                                        return mprdh;
                                    }
                                case HYTERA_68:
                                    HyteraProprietaryDataHeader hpdh = new HyteraProprietaryDataHeader(pattern, payload,
                                        cach, slotType, timestamp, timeslot);
                                    hpdh.setValid(valid);
                                    return hpdh;
                                default:
                                    ProprietaryDataHeader pdh = new ProprietaryDataHeader(pattern, payload, cach,
                                        slotType, timestamp, timeslot);
                                    pdh.setValid(valid);
                                    return pdh;
                            }
                        case RAW_OR_STATUS_SHORT_DATA:
                            int appendedBlockCount = ShortDataHeader.getAppendedBlocks(payload);

                            if(appendedBlockCount == 0)
                            {
                                StatusDataHeader sdh = new StatusDataHeader(pattern, payload, cach, slotType, timestamp,
                                    timeslot);
                                sdh.setValid(valid);
                                return sdh;
                            }
                            else
                            {
                                RawShortDataHeader rsdh = new RawShortDataHeader(pattern, payload, cach, slotType,
                                    timestamp, timeslot);
                                rsdh.setValid(valid);
                                return rsdh;
                            }
                        case RESPONSE_PACKET:
                            ResponseDataHeader rdh = new ResponseDataHeader(pattern, payload, cach, slotType, timestamp,
                                timeslot);
                            rdh.setValid(valid);
                            return rdh;
                        case DEFINED_SHORT_DATA:
                            DefinedShortDataHeader dsdh = new DefinedShortDataHeader(pattern, payload, cach,slotType,
                                timestamp, timeslot);
                            dsdh.setValid(valid);
                            return dsdh;
                        case UNCONFIRMED_DATA_PACKET:
                            UnconfirmedDataHeader udh = new UnconfirmedDataHeader(pattern, payload, cach, slotType,
                                timestamp, timeslot);
                            udh.setValid(valid);
                            return udh;
                        case UNIFIED_DATA_TRANSPORT:
                            //TODO: this will eventually need its own factory
                            UDTHeader uh = new UDTHeader(pattern, payload, cach, slotType, timestamp, timeslot);
                            uh.setValid(valid);
                            return uh;
                        default:
                            DataHeader dh = new DataHeader(pattern, payload, cach, slotType, timestamp, timeslot);
                            dh.setValid(valid);
                            return dh;
                    }
                case TLC:
                    return new Terminator(pattern, getPayload(message), cach, slotType, timestamp, timeslot);
                case RATE_1_OF_2_DATA:
                    return new DataBlock1_2Rate(pattern, getPayload(message), cach, slotType, timestamp, timeslot);
                case RATE_3_OF_4_DATA:
                    return new DataBlock3_4Rate(pattern, getTrellisPayload(message), cach, slotType, timestamp, timeslot);
                case RATE_1_DATA:
                    return new DataBlock1Rate(pattern, message, cach, slotType, timestamp, timeslot);
                case MBC_BLOCK:
                    return new MBCContinuationBlock(pattern, getPayload(message), cach, slotType, timestamp, timeslot);
                case RESERVED_15:
                case UNKNOWN:
                    return new UnknownDataMessage(pattern, getPayload(message), cach, slotType, timestamp, timeslot);
            }
        }

        return new UnknownDataMessage(pattern, getPayload(message), cach, slotType, timestamp, timeslot);
    }

    /**
     * Decodes a 3/4 rate trellis coded message
     * @param message containing trellis coded message
     * @return decoded message
     */
    private static CorrectedBinaryMessage getTrellisPayload(CorrectedBinaryMessage message)
    {
        CorrectedBinaryMessage descrambled = extract(message);
        return VITERBI_DECODER.decode(descrambled);
    }

    /**
     * De-scramble, decode and error check a BPTC protected raw message and return a 96-bit error corrected
     * payload or null.
     * @param message
     * @return
     */
    private static CorrectedBinaryMessage getPayload(CorrectedBinaryMessage message)
    {
        CorrectedBinaryMessage descrambled = extract(message);
        return BPTC_196_96.extract(descrambled);
    }

    /**
     * Extracts the 196-bit message payload from the full DMR burst
     * @param message as transmitted
     * @return extracted message
     */
    private static CorrectedBinaryMessage extract(CorrectedBinaryMessage message)
    {
        CorrectedBinaryMessage extracted = new CorrectedBinaryMessage(196);

        try
        {
            for(int i = 24; i < 122; i++)
            {
                extracted.add(message.get(i));
            }
            for(int i = 190; i < 190 + 98; i++)
            {
                extracted.add(message.get(i));
            }
        }
        catch(BitSetFullException ex)
        {
            mLog.error("Error extracting DMR burst payload bits");
        }

        return extracted;
    }
}
