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
import io.github.dsheirer.module.decode.nxdn.layer3.coding.PunctureProviderCACAndFACCH2;
import io.github.dsheirer.module.decode.nxdn.layer3.coding.PunctureProviderFACCH1;
import io.github.dsheirer.module.decode.nxdn.layer3.coding.PunctureProviderLongCAC;
import io.github.dsheirer.module.decode.nxdn.layer3.coding.PunctureProviderNone;
import io.github.dsheirer.module.decode.nxdn.layer3.coding.PunctureProviderSACCH;
import io.github.dsheirer.module.decode.nxdn.layer3.type.AudioCodec;
import java.util.ArrayList;
import java.util.List;

/**
 * NXDN layer 1 frame.
 */
public class Frame
{
    private static final PunctureProvider PUNCTURE_PROVIDER_CAC_FACCH2 = new PunctureProviderCACAndFACCH2();
    private static final PunctureProvider PUNCTURE_PROVIDER_FACCH1 = new PunctureProviderFACCH1();
    private static final PunctureProvider PUNCTURE_PROVIDER_LONG_CAC = new PunctureProviderLongCAC();
    private static final PunctureProvider PUNCTURE_PROVIDER_NONE = new PunctureProviderNone();
    public static final PunctureProvider PUNCTURE_PROVIDER_SACCH = new PunctureProviderSACCH();
    private static final Interleaver INTERLEAVER_FACCH1 = new Interleaver(16, 9);
    private static final Interleaver INTERLEAVER_FACCH2 = new Interleaver(12, 29);
    private static final Interleaver INTERLEAVER_OUTBOUND_CAC = new Interleaver(12, 25);
    private static final Interleaver INTERLEAVER_INBOUND_CAC = new Interleaver(12, 21);
    private static final Interleaver INTERLEAVER_SACCH = new Interleaver(12, 5);
    private static final IntField STRUCTURE = IntField.length2(0);
    private static final IntField RADIO_ACCESS_NUMBER = IntField.length6(2);
    private static final FragmentedIntField LICH_FIELD = FragmentedIntField.of(0, 2, 4, 6, 8, 10, 12);
    private static final BinaryMessage SCRAMBLE_SEQUENCE_LICH = Scrambler.generate(16);
    private static final BinaryMessage SCRAMBLE_SEQUENCE_FULL = Scrambler.generate(364);
    private static final BinaryMessage SCRAMBLE_SEQUENCE_CONTROL_INBOUND = Scrambler.generate(268);
    private static final BinaryMessage SCRAMBLE_SEQUENCE_CONTROL_OUTBOUND = Scrambler.generate(340);

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

        AudioCodec codec = AudioCodec.HALF_RATE; //TODO: figure out how to properly determine which codec?

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
                    NXDNMessageType type = NXDNMessageType.getControlOutbound(cac);
                    NXDNLayer3Message layer3 = NXDNMessageFactory.get(type, cac, timestamp, ran, mLICH);
                    layer3.setValid(NXDNCRC.checkCAC(payload));
                    mMessages.add(layer3);
                }
                else
                {
                    message.xor(SCRAMBLE_SEQUENCE_CONTROL_INBOUND);
                    CorrectedBinaryMessage payload = mLICH.isLongCAC() ? decodeLongCAC(message) :
                            decodeShortCAC(message);
//TODO: do I need this?    Structure structure = Structure.fromControlValue(payload.getInt(CAC_STRUCTURE));
                    int ran = payload.getInt(RADIO_ACCESS_NUMBER);
                    CorrectedBinaryMessage cac = mLICH.isLongCAC() ? payload.getSubMessage(8, 136) : payload.getSubMessage(8, 104);
                    NXDNMessageType type = NXDNMessageType.getControlInbound(cac);
                    NXDNLayer3Message layer3 = NXDNMessageFactory.get(type, cac, timestamp, ran, mLICH);
                    layer3.setValid(mLICH.isLongCAC() ? NXDNCRC.checkLongCAC(payload) : NXDNCRC.checkShortCAC(payload));
                    mMessages.add(layer3);
                }
                break;
            default: //RTCH and RDCH
                message.xor(SCRAMBLE_SEQUENCE_FULL);

                if(mLICH.hasSACCH()) //SACCH + Voice/FACCH frames
                {
                    CorrectedBinaryMessage sacch = decodeSACCH(message);
                    //TODO: check crc
                    SACCHFragment sacchFragment = new SACCHFragment(sacch, timestamp, mLICH);
                    sacchFragment.setValid(NXDNCRC.checkSACCH(sacch));
                    mMessages.add(sacchFragment);

                    if(mLICH.hasAudio())
                    {
                        List<byte[]> frames = new ArrayList<>();

                        if(mLICH.isVoiceFirst())
                        {
                            if(codec == AudioCodec.HALF_RATE)
                            {
                                frames.add(message.getSubMessage(76, 148).toByteArray());
                                frames.add(message.getSubMessage(148, 220).toByteArray());
                            }
                            else
                            {
                                frames.add(message.getSubMessage(76, 220).toByteArray());
                            }
                        }

                        if(mLICH.isVoiceSecond())
                        {
                            if(codec == AudioCodec.HALF_RATE)
                            {
                                frames.add(message.getSubMessage(220, 292).toByteArray());
                                frames.add(message.getSubMessage(292, 364).toByteArray());
                            }
                            else
                            {
                                frames.add(message.getSubMessage(220, 364).toByteArray());
                            }
                        }

                        mMessages.add(new Audio(codec, frames, timestamp, sacchFragment.getRAN(), sacchFragment.getLICH()));
                    }

                    if(mLICH.isFACCH1First())
                    {
                        CorrectedBinaryMessage payload = decodeFACCH1First(message);
                        NXDNMessageType type = mLICH.isOutbound() ? NXDNMessageType.getTrafficOutbound(payload) :
                                NXDNMessageType.getTrafficInbound(payload);
                        NXDNLayer3Message layer3 = NXDNMessageFactory.get(type, payload, timestamp, sacchFragment.getRAN(), mLICH);
                        layer3.setValid(NXDNCRC.checkFACCH1(payload));
                        mMessages.add(layer3);
                    }

                    if(mLICH.isFACCH1Second())
                    {
                        CorrectedBinaryMessage payload = decodeFACCH1Second(message);
                        NXDNMessageType type = mLICH.isOutbound() ? NXDNMessageType.getTrafficOutbound(payload) :
                                NXDNMessageType.getTrafficInbound(payload);
                        NXDNLayer3Message layer3 = NXDNMessageFactory.get(type, payload, timestamp, sacchFragment.getRAN(), mLICH);
                        layer3.setValid(NXDNCRC.checkFACCH1(payload));
                        mMessages.add(layer3);
                    }
                }
                else //UDCH or FACCH2 frames
                {
                    CorrectedBinaryMessage payload = decodeUDCHAndFACCH2(message);
                    int ran = payload.getInt(RADIO_ACCESS_NUMBER);
                    CorrectedBinaryMessage facch2 = payload.getSubMessage(8, 184);
                    NXDNMessageType type = mLICH.isOutbound() ? NXDNMessageType.getTrafficOutbound(facch2) :
                            NXDNMessageType.getTrafficInbound(facch2);
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
        return decode(message, 76, INTERLEAVER_FACCH1, PUNCTURE_PROVIDER_FACCH1);
    }

    /**
     * Decodes the second FACCH1 frame
     * @param message to decode
     * @return decoded message
     */
    public static CorrectedBinaryMessage decodeFACCH1Second(CorrectedBinaryMessage message)
    {
        return decode(message, 220, INTERLEAVER_FACCH1, PUNCTURE_PROVIDER_FACCH1);
    }

    /**
     * Decodes the FACCH2 frame
     * @param message to decode
     * @return decoded message
     */
    public static CorrectedBinaryMessage decodeUDCHAndFACCH2(CorrectedBinaryMessage message)
    {
        return decode(message, 16, INTERLEAVER_FACCH2, PUNCTURE_PROVIDER_CAC_FACCH2);
    }

    /**
     * Decodes the SACCH frame
     * @param message to decode
     * @return decoded message
     */
    public static CorrectedBinaryMessage decodeSACCH(CorrectedBinaryMessage message)
    {
        return decode(message, 16, INTERLEAVER_SACCH, PUNCTURE_PROVIDER_SACCH);
    }

    /**
     * Decodes the outbound control frame
     * @param message to decode
     * @return decoded message
     */
    public static CorrectedBinaryMessage decodeCAC(CorrectedBinaryMessage message)
    {
        return decode(message, 16, INTERLEAVER_OUTBOUND_CAC, PUNCTURE_PROVIDER_CAC_FACCH2);
    }

    /**
     * Decodes the inbound long control frame
     * @param message to decode
     * @return decoded message
     */
    public static CorrectedBinaryMessage decodeLongCAC(CorrectedBinaryMessage message)
    {
        return decode(message, 16, INTERLEAVER_INBOUND_CAC, PUNCTURE_PROVIDER_LONG_CAC);
    }

    /**
     * Decodes the inbound short control frame
     * @param message to decode
     * @return decoded message
     */
    public static CorrectedBinaryMessage decodeShortCAC(CorrectedBinaryMessage message)
    {
        return decode(message, 16, INTERLEAVER_INBOUND_CAC, PUNCTURE_PROVIDER_NONE);
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
