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

package io.github.dsheirer.module.decode.nxdn.layer1;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.FragmentedIntField;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.module.decode.nxdn.NXDNMessage;
import io.github.dsheirer.module.decode.nxdn.layer2.Direction;
import io.github.dsheirer.module.decode.nxdn.layer2.Interleaver;
import io.github.dsheirer.module.decode.nxdn.layer2.LICH;
import io.github.dsheirer.module.decode.nxdn.layer2.RFChannel;
import io.github.dsheirer.module.decode.nxdn.layer2.SACCHFragment;
import io.github.dsheirer.module.decode.nxdn.layer2.Scrambler;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNLayer3Message;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNMessageFactory;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNMessageType;
import io.github.dsheirer.module.decode.nxdn.layer3.call.Audio;
import io.github.dsheirer.module.decode.nxdn.layer3.coding.Convolution;
import io.github.dsheirer.module.decode.nxdn.layer3.coding.NXDNCRC;
import io.github.dsheirer.module.decode.nxdn.layer3.coding.PunctureProvider;
import io.github.dsheirer.module.decode.nxdn.layer3.coding.PunctureProviderCACAndFACCH2_UDCH;
import io.github.dsheirer.module.decode.nxdn.layer3.coding.PunctureProviderFACCH1_UDCH2_FACCH3;
import io.github.dsheirer.module.decode.nxdn.layer3.coding.PunctureProviderLongCAC;
import io.github.dsheirer.module.decode.nxdn.layer3.coding.PunctureProviderNone;
import io.github.dsheirer.module.decode.nxdn.layer3.coding.PunctureProviderSACCHAndSCCH;
import io.github.dsheirer.module.decode.nxdn.layer3.type.AudioCodec;
import java.util.ArrayList;
import java.util.List;

/**
 * NXDN layer 1 frame.
 */
public class Frame
{
    private static final BinaryMessage SCRAMBLE_SEQUENCE_LICH = Scrambler.generate(16);
    private static final BinaryMessage SCRAMBLE_SEQUENCE_FULL = Scrambler.generate(364);
    private static final BinaryMessage SCRAMBLE_SEQUENCE_CONTROL_INBOUND = Scrambler.generate(268);
    private static final BinaryMessage SCRAMBLE_SEQUENCE_CONTROL_OUTBOUND = Scrambler.generate(340);
    private static final Interleaver INTERLEAVER_16_9 = new Interleaver(16, 9);
    private static final Interleaver INTERLEAVER_12_29 = new Interleaver(12, 29);
    private static final Interleaver INTERLEAVER_12_25 = new Interleaver(12, 25);
    private static final Interleaver INTERLEAVER_12_21 = new Interleaver(12, 21);
    private static final Interleaver INTERLEAVER_12_5 = new Interleaver(12, 5);
    private static final PunctureProvider PUNCTURE_PROVIDER_CAC_FACCH2_UDCH = new PunctureProviderCACAndFACCH2_UDCH();
    private static final PunctureProvider PUNCTURE_PROVIDER_FACCH1_FACCH3_UDCH2 = new PunctureProviderFACCH1_UDCH2_FACCH3();
    private static final PunctureProvider PUNCTURE_PROVIDER_LONG_CAC = new PunctureProviderLongCAC();
    private static final PunctureProvider PUNCTURE_PROVIDER_NONE = new PunctureProviderNone();
    private static final PunctureProvider PUNCTURE_PROVIDER_SACCH_SCCH = new PunctureProviderSACCHAndSCCH();
    private static final IntField STRUCTURE = IntField.length2(0);
    private static final IntField RADIO_ACCESS_NUMBER = IntField.length6(2);
    private static final FragmentedIntField LICH_FIELD = FragmentedIntField.of(0, 2, 4, 6, 8, 10, 12);
    private static final int RAN_TYPE_D = 0;

    static
    {
        //Clear the LICH bits in the complete scramble sequences so that we don't double-scramble the bits.
        SCRAMBLE_SEQUENCE_FULL.xor(SCRAMBLE_SEQUENCE_LICH);
        SCRAMBLE_SEQUENCE_CONTROL_INBOUND.xor(SCRAMBLE_SEQUENCE_LICH);
        SCRAMBLE_SEQUENCE_CONTROL_OUTBOUND.xor(SCRAMBLE_SEQUENCE_LICH);
    }

    private final LICH mLICH;
    private List<NXDNMessage> mMessages = new ArrayList<>();

    /**
     * Constructs a frame.  Note: channel and direction are used as fallback values when we can't exactly match the
     * LICH value for this frame due to decoding errors.
     * @param message with frame bits
     * @param timestamp for the frame.
     * @param channel as tracked across the most recent 3 frames or UNKNOWN (default).
     * @param direction as tracked across the most recent 3 frames or OUTBOUND (default).
     */
    public Frame(CorrectedBinaryMessage message, long timestamp, RFChannel channel, Direction direction)
    {
        message.xor(SCRAMBLE_SEQUENCE_LICH);
        mLICH = LICH.fromValue(message.getInt(LICH_FIELD), channel, direction);

        //Descramble the message according to the RF Channel Type and repeater direction.  Control channel uses the
        // full scramble sequence truncated based on repeater direction.  All others use a full scramble sequence.
        switch(mLICH.getRFChannel())
        {
            case RCCH:
                if(mLICH.isOutbound())
                {
                    message.xor(SCRAMBLE_SEQUENCE_CONTROL_OUTBOUND);
                    CorrectedBinaryMessage payload = decodeCAC(message);
                    int ran = payload.getInt(RADIO_ACCESS_NUMBER);
                    CorrectedBinaryMessage cac = payload.getSubMessage(8, 152);
                    cac.setCorrectedBitCount(payload.getCorrectedBitCount());
                    NXDNMessageType type = NXDNMessageType.getControl(cac, mLICH);
                    NXDNLayer3Message layer3 = NXDNMessageFactory.get(type, cac, timestamp, ran, mLICH);
                    layer3.setValid(NXDNCRC.checkCAC(payload));
                    mMessages.add(layer3);
                }
                else
                {
                    message.xor(SCRAMBLE_SEQUENCE_CONTROL_INBOUND);
                    CorrectedBinaryMessage payload = mLICH.isLongCAC() ? decodeLongCAC(message) : decodeShortCAC(message);
                    int ran = payload.getInt(RADIO_ACCESS_NUMBER);
                    CorrectedBinaryMessage cac = mLICH.isLongCAC() ? payload.getSubMessage(8, 136) : payload.getSubMessage(8, 104);
                    cac.setCorrectedBitCount(payload.getCorrectedBitCount());
                    NXDNMessageType type = NXDNMessageType.getControl(cac, mLICH);
                    NXDNLayer3Message layer3 = NXDNMessageFactory.get(type, cac, timestamp, ran, mLICH);
                    layer3.setValid(mLICH.isLongCAC() ? NXDNCRC.checkLongCAC(payload) : NXDNCRC.checkShortCAC(payload));
                    mMessages.add(layer3);
                }
                break;
            default: //RDCH, RTCH and RTCH2
                message.xor(SCRAMBLE_SEQUENCE_FULL);

                if(mLICH.hasSACCH()) //SACCH + Voice/FACCH frames
                {
                    CorrectedBinaryMessage sacch = decodeSACCH(message);
                    SACCHFragment sacchFragment = new SACCHFragment(sacch, timestamp, mLICH);
                    sacchFragment.setValid(NXDNCRC.checkSACCH(sacch));
                    mMessages.add(sacchFragment);

                    if(mLICH.hasAudio())
                    {
                        List<byte[]> frames = new ArrayList<>();

                        if(mLICH.isVoiceFirst())
                        {
                            frames.add(message.getSubMessage(76, 148).toByteArray());
                            frames.add(message.getSubMessage(148, 220).toByteArray());
                        }

                        if(mLICH.isVoiceSecond())
                        {
                            frames.add(message.getSubMessage(220, 292).toByteArray());
                            frames.add(message.getSubMessage(292, 364).toByteArray());
                        }

                        mMessages.add(new Audio(AudioCodec.HALF_RATE, frames, timestamp, sacchFragment.getRAN(), sacchFragment.getLICH()));
                    }

                    if(mLICH.isFACCH1First())
                    {
                        CorrectedBinaryMessage payload = decodeFACCH1First(message);
                        NXDNMessageType type = NXDNMessageType.getTraffic(payload, mLICH);
                        NXDNLayer3Message layer3 = NXDNMessageFactory.get(type, payload, timestamp, sacchFragment.getRAN(), mLICH);
                        layer3.setValid(NXDNCRC.checkFACCH1(payload));
                        mMessages.add(layer3);
                    }

                    if(mLICH.isFACCH1Second())
                    {
                        CorrectedBinaryMessage payload = decodeFACCH1Second(message);
                        NXDNMessageType type = NXDNMessageType.getTraffic(payload, mLICH);
                        NXDNLayer3Message layer3 = NXDNMessageFactory.get(type, payload, timestamp, sacchFragment.getRAN(), mLICH);
                        layer3.setValid(NXDNCRC.checkFACCH1(payload));
                        mMessages.add(layer3);
                    }
                }
                else if(mLICH.hasSCCH()) //Type-D segments with SCCH
                {
                    CorrectedBinaryMessage scch = decodeSCCH(message);
                    NXDNMessageType typeSCCH = NXDNMessageType.getSCCH(scch, mLICH);
                    NXDNLayer3Message scchMessage = NXDNMessageFactory.get(typeSCCH, scch, timestamp, RAN_TYPE_D, mLICH);
                    scchMessage.setValid(NXDNCRC.checkSCCH(scch));
                    mMessages.add(scchMessage);

                    if(mLICH.isFACCH1First())
                    {
                        CorrectedBinaryMessage payload = decodeFACCH1First(message);
                        NXDNMessageType type = NXDNMessageType.getTypeD(payload, mLICH);
                        NXDNLayer3Message layer3 = NXDNMessageFactory.get(type, payload, timestamp, RAN_TYPE_D, mLICH);
                        layer3.setValid(NXDNCRC.checkFACCH1(payload));
                        mMessages.add(layer3);
                    }

                    if(mLICH.isFACCH1Second())
                    {
                        CorrectedBinaryMessage payload = decodeFACCH1Second(message);
                        NXDNMessageType type = NXDNMessageType.getTypeD(payload, mLICH);
                        NXDNLayer3Message layer3 = NXDNMessageFactory.get(type, payload, timestamp, RAN_TYPE_D, mLICH);
                        layer3.setValid(NXDNCRC.checkFACCH1(payload));
                        mMessages.add(layer3);
                    }

                    if(mLICH.isData()) //FACCH3 or UDCH2
                    {
                        CorrectedBinaryMessage payloadA = decodeUDCH2AndFACCH3A(message);
                        CorrectedBinaryMessage payloadB = decodeUDCH2AndFACCH3B(message);
                        boolean validA = NXDNCRC.checkFACCH3(payloadA);
                        boolean validB = NXDNCRC.checkFACCH3(payloadB);
                        CorrectedBinaryMessage combined = new CorrectedBinaryMessage(160);
                        NXDNMessageType type = NXDNMessageType.getTypeD(combined, mLICH);
                        NXDNLayer3Message layer3 = NXDNMessageFactory.get(type, combined, timestamp, RAN_TYPE_D, mLICH);
                        layer3.setValid(validA && validB);
                        mMessages.add(layer3);
                    }

                    if(mLICH.hasAudio())
                    {
                        List<byte[]> frames = new ArrayList<>();

                        if(mLICH.isVoiceFirst())
                        {
                            frames.add(message.getSubMessage(76, 148).toByteArray());
                            frames.add(message.getSubMessage(148, 220).toByteArray());
                        }

                        if(mLICH.isVoiceSecond())
                        {
                            frames.add(message.getSubMessage(220, 292).toByteArray());
                            frames.add(message.getSubMessage(292, 364).toByteArray());
                        }

                        mMessages.add(new Audio(AudioCodec.HALF_RATE, frames, timestamp, RAN_TYPE_D, mLICH));
                    }
                }
                else if(mLICH.getRFChannel() != RFChannel.RTCH2) //UDCH or FACCH2 frames on non-Type-D
                {
                    CorrectedBinaryMessage payload = decodeUDCHAndFACCH2(message);
                    int ran = payload.getInt(RADIO_ACCESS_NUMBER);
                    CorrectedBinaryMessage facch2 = payload.getSubMessage(8, 184);
                    facch2.setCorrectedBitCount(payload.getCorrectedBitCount());
                    NXDNMessageType type = NXDNMessageType.getTraffic(facch2, mLICH);
                    NXDNLayer3Message layer3 = NXDNMessageFactory.get(type, facch2, timestamp, ran, mLICH);
                    layer3.setValid(NXDNCRC.checkFACCH2(payload));
                    mMessages.add(layer3);
                }

                break;
        }
    }

    /**
     * Decodes the frame payload
     * @param message from the frame
     * @param offset to the start of the payload
     * @param interleaver for the payload type
     * @param punctureProvider for the payload type
     * @return decoded message
     */
    private static CorrectedBinaryMessage decode(CorrectedBinaryMessage message, int offset, Interleaver interleaver, PunctureProvider punctureProvider)
    {
        CorrectedBinaryMessage deinterleaved = interleaver.deinterleave(message, offset);
        return Convolution.decode(punctureProvider.depuncture(deinterleaved), punctureProvider);
    }

    /**
     * Decodes the first FACCH1 frame
     * @param message to decode
     * @return decoded message
     */
    public static CorrectedBinaryMessage decodeFACCH1First(CorrectedBinaryMessage message)
    {
        return decode(message, 76, INTERLEAVER_16_9, PUNCTURE_PROVIDER_FACCH1_FACCH3_UDCH2);
    }

    /**
     * Decodes the second FACCH1 frame
     * @param message to decode
     * @return decoded message
     */
    public static CorrectedBinaryMessage decodeFACCH1Second(CorrectedBinaryMessage message)
    {
        return decode(message, 220, INTERLEAVER_16_9, PUNCTURE_PROVIDER_FACCH1_FACCH3_UDCH2);
    }

    /**
     * Decodes the FACCH2 frame
     * @param message to decode
     * @return decoded message
     */
    public static CorrectedBinaryMessage decodeUDCHAndFACCH2(CorrectedBinaryMessage message)
    {
        return decode(message, 16, INTERLEAVER_12_29, PUNCTURE_PROVIDER_CAC_FACCH2_UDCH);
    }

    /**
     * Decodes the first half of the FACCH3 and UDCH2 frame on Type-D systems
     * @param message to decode
     * @return decoded message
     */
    public static CorrectedBinaryMessage decodeUDCH2AndFACCH3A(CorrectedBinaryMessage message)
    {
        return decode(message, 76, INTERLEAVER_16_9, PUNCTURE_PROVIDER_FACCH1_FACCH3_UDCH2);
    }

    /**
     * Decodes the second half of the FACCH3 and UDCH2 frame on Type-D systems
     * @param message to decode
     * @return decoded message
     */
    public static CorrectedBinaryMessage decodeUDCH2AndFACCH3B(CorrectedBinaryMessage message)
    {
        return decode(message, 220, INTERLEAVER_16_9, PUNCTURE_PROVIDER_FACCH1_FACCH3_UDCH2);
    }

    /**
     * Decodes the SACCH frame
     * @param message to decode
     * @return decoded message
     */
    public static CorrectedBinaryMessage decodeSACCH(CorrectedBinaryMessage message)
    {
        return decode(message, 16, INTERLEAVER_12_5, PUNCTURE_PROVIDER_SACCH_SCCH);
    }

    /**
     * Decodes the SCCH frame in a Type-D system
     * @param message to decode
     * @return decoded message
     */
    public static CorrectedBinaryMessage decodeSCCH(CorrectedBinaryMessage message)
    {
        return decode(message, 16, INTERLEAVER_12_5, PUNCTURE_PROVIDER_SACCH_SCCH);
    }

    /**
     * Decodes the outbound control frame
     * @param message to decode
     * @return decoded message
     */
    public static CorrectedBinaryMessage decodeCAC(CorrectedBinaryMessage message)
    {
        return decode(message, 16, INTERLEAVER_12_25, PUNCTURE_PROVIDER_CAC_FACCH2_UDCH);
    }

    /**
     * Decodes the inbound long control frame
     * @param message to decode
     * @return decoded message
     */
    public static CorrectedBinaryMessage decodeLongCAC(CorrectedBinaryMessage message)
    {
        return decode(message, 16, INTERLEAVER_12_21, PUNCTURE_PROVIDER_LONG_CAC);
    }

    /**
     * Decodes the inbound short control frame
     * @param message to decode
     * @return decoded message
     */
    public static CorrectedBinaryMessage decodeShortCAC(CorrectedBinaryMessage message)
    {
        return decode(message, 16, INTERLEAVER_12_21, PUNCTURE_PROVIDER_NONE);
    }

    /**
     * Message(s( from this frame.
     */
    public List<NXDNMessage> getMessages()
    {
        return mMessages;
    }

    /**
     * Link information channel (LICH)
     * @return LICH
     */
    public LICH getLICH()
    {
        return mLICH;
    }
}
