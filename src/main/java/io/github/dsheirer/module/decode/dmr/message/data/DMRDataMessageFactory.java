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

package io.github.dsheirer.module.decode.dmr.message.data;

import io.github.dsheirer.bits.BitSetFullException;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.CRCDMR;
import io.github.dsheirer.edac.trellis.ViterbiDecoder_3_4_DMR;
import io.github.dsheirer.module.decode.dmr.DMRCrcMaskManager;
import io.github.dsheirer.module.decode.dmr.bptc.BPTC_196_96;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.data.block.DataBlock1Rate;
import io.github.dsheirer.module.decode.dmr.message.data.block.DataBlock1_2Rate;
import io.github.dsheirer.module.decode.dmr.message.data.block.DataBlock3_4Rate;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.CSBKMessageFactory;
import io.github.dsheirer.module.decode.dmr.message.data.header.ConfirmedDataHeader;
import io.github.dsheirer.module.decode.dmr.message.data.header.DataHeader;
import io.github.dsheirer.module.decode.dmr.message.data.header.DefinedShortDataHeader;
import io.github.dsheirer.module.decode.dmr.message.data.header.MBCHeader;
import io.github.dsheirer.module.decode.dmr.message.data.header.PiHeader;
import io.github.dsheirer.module.decode.dmr.message.data.header.ProprietaryDataHeader;
import io.github.dsheirer.module.decode.dmr.message.data.header.RawShortDataHeader;
import io.github.dsheirer.module.decode.dmr.message.data.header.ResponseDataHeader;
import io.github.dsheirer.module.decode.dmr.message.data.header.ShortDataHeader;
import io.github.dsheirer.module.decode.dmr.message.data.header.StatusDataHeader;
import io.github.dsheirer.module.decode.dmr.message.data.header.UDTHeader;
import io.github.dsheirer.module.decode.dmr.message.data.header.UnconfirmedDataHeader;
import io.github.dsheirer.module.decode.dmr.message.data.header.VoiceHeader;
import io.github.dsheirer.module.decode.dmr.message.data.header.hytera.HyteraDataEncryptionHeader;
import io.github.dsheirer.module.decode.dmr.message.data.header.motorola.MNISProprietaryDataHeader;
import io.github.dsheirer.module.decode.dmr.message.data.header.motorola.MotorolaDataEncryptionHeader;
import io.github.dsheirer.module.decode.dmr.message.data.lc.LCMessage;
import io.github.dsheirer.module.decode.dmr.message.data.lc.LCMessageFactory;
import io.github.dsheirer.module.decode.dmr.message.data.mbc.MBCContinuationBlock;
import io.github.dsheirer.module.decode.dmr.message.data.terminator.Terminator;
import io.github.dsheirer.module.decode.dmr.message.data.usb.USBData;
import io.github.dsheirer.module.decode.dmr.message.type.DataPacketFormat;
import io.github.dsheirer.module.decode.dmr.message.type.ServiceAccessPoint;
import io.github.dsheirer.module.decode.dmr.message.type.Vendor;
import io.github.dsheirer.module.decode.dmr.sync.DMRSyncPattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating data messages that contain a 196-bit BPTC protected message.
 */
public class DMRDataMessageFactory
{
    private final static Logger mLog = LoggerFactory.getLogger(DMRDataMessageFactory.class);
    private static final ViterbiDecoder_3_4_DMR VITERBI_DECODER = new ViterbiDecoder_3_4_DMR();
    private final LCMessageFactory mLinkControlMessageFactory;

    /**
     * Constructs an instance
     * @param maskManager for managing CSBK and link control error detection.
     */
    public DMRDataMessageFactory(DMRCrcMaskManager maskManager)
    {
        mLinkControlMessageFactory = new LCMessageFactory(maskManager);
    }

    /**
     * Creates a data message class
     * @param pattern for the DMR burst
     * @param message DMR burst as transmitted
     * @param cach from the DMR burst
     * @param timestamp for the message
     * @param timeslot for the message
     * @return data message instance
     */
    public DataMessage create(DMRSyncPattern pattern, CorrectedBinaryMessage message, CACH cach, long timestamp,
                              int timeslot)
    {
        SlotType slotType = SlotType.getSlotType(message);

        if(slotType.isValid())
        {
            switch (slotType.getDataType())
            {
                case RATE_3_OF_4_DATA:
                    return new DataBlock3_4Rate(pattern, getTrellisPayload(message), cach, slotType, timestamp, timeslot);
                case RATE_1_DATA:
                    return new DataBlock1Rate(pattern, message, cach, slotType, timestamp, timeslot);
            }

            //Get the BPTC(196,96) protected payload.  If the corrected bit count is -1, the payload/message is invalid
            CorrectedBinaryMessage payload  = getPayload(message);

            switch(slotType.getDataType())
            {
                case SLOT_IDLE:
                    DataMessage idle = new IDLEMessage(pattern, payload, cach, slotType, timestamp, timeslot);
                    if(payload.getCorrectedBitCount() < 0)
                    {
                        idle.setValid(false);
                    }
                    return idle;
                case CSBK:
                    DataMessage csbk = CSBKMessageFactory.create(pattern, payload, cach, slotType, timestamp, timeslot);
                    if(payload.getCorrectedBitCount() < 0)
                    {
                        csbk.setValid(false);
                    }
                    return csbk;
                case USB_DATA:
                    DataMessage usb = new USBData(pattern, payload, cach, slotType, timestamp, timeslot);
                    if(payload.getCorrectedBitCount() < 0)
                    {
                        usb.setValid(false);
                    }
                    return usb;
                case MBC_ENC_HEADER:
                case MBC_HEADER:
                    DataMessage mbc = new MBCHeader(pattern, payload, cach, slotType, timestamp, timeslot);
                    if(payload.getCorrectedBitCount() < 0)
                    {
                        mbc.setValid(false);
                    }
                    return mbc;
                case CHANNEL_CONTROL_ENC_HEADER:
                case PI_HEADER:
                    LCMessage piLinkControl = mLinkControlMessageFactory.createFullEncryption(payload, timestamp,
                            timeslot);
                    DataMessage pi = new PiHeader(pattern, payload, cach, slotType, timestamp, timeslot, piLinkControl);
                    if(payload.getCorrectedBitCount() < 0)
                    {
                        pi.setValid(false);
                    }
                    return pi;
                case VOICE_HEADER:
                    LCMessage vhLinkControl = mLinkControlMessageFactory.createFull(payload, timestamp, timeslot, false);
                    DataMessage voice = new VoiceHeader(pattern, payload, cach, slotType, timestamp, timeslot,
                            vhLinkControl);
                    if(payload.getCorrectedBitCount() < 0)
                    {
                        voice.setValid(false);
                    }
                    return voice;
                case DATA_ENC_HEADER:
                case DATA_HEADER:
                    int crcBitCount = CRCDMR.correctCCITT80(payload, 0, 80, 0xCCCC);
                    boolean valid = crcBitCount < 2 && payload.getCorrectedBitCount() != -1;
                    DataPacketFormat dpf = DataHeader.getDataPacketFormat(payload);

                    switch(dpf)
                    {
                        case CONFIRMED_DATA_PACKET:
                            ConfirmedDataHeader cdh = new ConfirmedDataHeader(pattern, payload, cach, slotType,
                                    timestamp, timeslot);
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
                                        MotorolaDataEncryptionHeader mprdh = new MotorolaDataEncryptionHeader(pattern,
                                            payload, cach, slotType, timestamp, timeslot);
                                        mprdh.setValid(valid);
                                        return mprdh;
                                    }
                                case HYTERA_68:
                                    HyteraDataEncryptionHeader hsdh = new HyteraDataEncryptionHeader(pattern, payload,
                                            cach, slotType, timestamp, timeslot);
                                    hsdh.setValid(valid);
                                    return hsdh;
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
                    LCMessage tlcLinkControl = mLinkControlMessageFactory.createFull(payload, timestamp, timeslot, true);
                    DataMessage tlc = new Terminator(pattern, payload, cach, slotType, timestamp, timeslot, tlcLinkControl);
                    if(payload.getCorrectedBitCount() < 0)
                    {
                        tlc.setValid(false);
                    }
                    return tlc;
                case RATE_1_OF_2_DATA:
                    DataMessage data = new DataBlock1_2Rate(pattern, payload, cach, slotType, timestamp, timeslot);
                    if(payload.getCorrectedBitCount() < 0)
                    {
                        data.setValid(false);
                    }
                    return data;
                case MBC_BLOCK:
                    DataMessage mbcBlock = new MBCContinuationBlock(pattern, payload, cach, slotType, timestamp, timeslot);
                    if(payload.getCorrectedBitCount() < 0)
                    {
                        mbcBlock.setValid(false);
                    }
                    return mbcBlock;
                case RESERVED_15:
                case UNKNOWN:
                    DataMessage unk = new UnknownDataMessage(pattern, payload, cach, slotType, timestamp, timeslot);
                    if(payload.getCorrectedBitCount() < 0)
                    {
                        unk.setValid(false);
                    }
                    return unk;
            }
        }

        CorrectedBinaryMessage payload = getPayload(message);
        DataMessage unk = new UnknownDataMessage(pattern, payload, cach, slotType, timestamp, timeslot);
        if(payload.getCorrectedBitCount() < 0)
        {
            unk.setValid(false);
        }
        return unk;
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
     * @param message containing a payload
     */
    private static CorrectedBinaryMessage getPayload(CorrectedBinaryMessage message)
    {
        CorrectedBinaryMessage extracted = extract(message);
        return BPTC_196_96.extract(extracted);
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
            for(int i = 190; i < 288; i++)
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
