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

package io.github.dsheirer.module.decode.nxdn.layer3;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.nxdn.NXDNMessage;
import io.github.dsheirer.module.decode.nxdn.layer1.Frame;
import io.github.dsheirer.module.decode.nxdn.layer2.Direction;
import io.github.dsheirer.module.decode.nxdn.layer2.Interleaver;
import io.github.dsheirer.module.decode.nxdn.layer2.LICH;
import io.github.dsheirer.module.decode.nxdn.layer2.Option;
import io.github.dsheirer.module.decode.nxdn.layer2.RFChannel;
import io.github.dsheirer.module.decode.nxdn.layer3.broadcast.ServiceInfo;
import io.github.dsheirer.module.decode.nxdn.layer3.broadcast.SiteInformation;
import io.github.dsheirer.module.decode.nxdn.layer3.call.Audio;
import io.github.dsheirer.module.decode.nxdn.layer3.call.DataCallAcknowledge;
import io.github.dsheirer.module.decode.nxdn.layer3.call.DataCallBlock;
import io.github.dsheirer.module.decode.nxdn.layer3.call.DataCallHeader;
import io.github.dsheirer.module.decode.nxdn.layer3.call.HeaderDelay;
import io.github.dsheirer.module.decode.nxdn.layer3.call.RemoteControlRequest;
import io.github.dsheirer.module.decode.nxdn.layer3.call.RemoteControlRequestWithESN;
import io.github.dsheirer.module.decode.nxdn.layer3.call.RemoteControlResponse;
import io.github.dsheirer.module.decode.nxdn.layer3.call.RemoteControlResponseWithESN;
import io.github.dsheirer.module.decode.nxdn.layer3.call.ShortDataCallBlock;
import io.github.dsheirer.module.decode.nxdn.layer3.call.ShortDataCallRequestHeader;
import io.github.dsheirer.module.decode.nxdn.layer3.call.ShortDataCallResponse;
import io.github.dsheirer.module.decode.nxdn.layer3.call.ShortDataInitializationVector;
import io.github.dsheirer.module.decode.nxdn.layer3.call.StatusInquiryRequest;
import io.github.dsheirer.module.decode.nxdn.layer3.call.StatusInquiryResponse;
import io.github.dsheirer.module.decode.nxdn.layer3.call.StatusRequest;
import io.github.dsheirer.module.decode.nxdn.layer3.call.StatusResponse;
import io.github.dsheirer.module.decode.nxdn.layer3.call.TransmissionRelease;
import io.github.dsheirer.module.decode.nxdn.layer3.call.TransmissionReleaseExtension;
import io.github.dsheirer.module.decode.nxdn.layer3.call.VoiceCall;
import io.github.dsheirer.module.decode.nxdn.layer3.call.VoiceCallInitializationVector;
import io.github.dsheirer.module.decode.nxdn.layer3.coding.CACPunctureProvider;
import io.github.dsheirer.module.decode.nxdn.layer3.coding.Convolution;
import io.github.dsheirer.module.decode.nxdn.layer3.coding.PunctureProvider;
import io.github.dsheirer.module.decode.nxdn.layer3.type.AudioCodec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Factory for creating NXDN messages
 */
public class NXDNMessageFactory
{
    private static final PunctureProvider PUNCTURE_PROVIDER_CAC = new CACPunctureProvider();
    private static final Interleaver INTERLEAVER_CAC = new Interleaver(12, 25);

    /**
     * Decodes the messages from the frame.
     * @param frame containing a LICH and various data payloads.
     * @param audioCodec for the upcoming or ongoing call.
     * @return messages
     */
    public List<NXDNMessage> getMessages(Frame frame, AudioCodec audioCodec)
    {
        LICH lich = frame.getLICH();

        switch(lich.getFunctionalChannel())
        {
            case CAC:
//                return Collections.singletonList(decodeCAC(frame));
            case CAC_LONG:
                return Collections.singletonList(decodeLongCAC(frame));
            case CAC_SHORT:
                return Collections.singletonList(decodeShortCAC(frame));
            case SACCH_SUPER_FRAME, SACCH_SUPER_FRAME_IDLE:
                return decodeSuperFrame(frame, audioCodec);
            case SACCH_NON_SUPER_FRAME:
                break;
            case UDCH:
                return Collections.singletonList(decodeUDCH(frame));
            case UNKNOWN:
                //TODO: attempt to decode using all options for the RF channel and Direction
        }

        return null;
    }

    /**
     * Extract the SACCH from the descrambled message
     * @param message containing a SACCH field.
     * @return extracted SACCH.
     */
    private NXDNMessage getSACCH(CorrectedBinaryMessage message)
    {
        CorrectedBinaryMessage deinterleaved = deinterleave(message, 16, 12, 5);

        return null;
    }

    /**
     * Extract the first FACCH1 from the descrambled message
     * @param message containing a FACCH1 field in the first half of the frame.
     * @return extracted FACCH1.
     */
    private NXDNMessage getFACCH1First(CorrectedBinaryMessage message)
    {
        CorrectedBinaryMessage deinterleaved = deinterleave(message, 76, 16, 9);

        return null;
    }

    /**
     * Extract the second FACCH1 from the descrambled message
     * @param message containing a FACCH1 field in the second half of the frame.
     * @return extracted FACCH1.
     */
    private NXDNMessage getFACCH1Second(CorrectedBinaryMessage message)
    {
        CorrectedBinaryMessage deinterleaved = deinterleave(message, 220, 16, 9);

        return null;
    }

    /**
     * Extracts AMBE+ full or half rate voice data from the frame.
     * @param message containing voice data in the first, second or both halves of the frame.
     * @param lich indicating frame voice content.
     * @param codec indicating full or half rate
     * @param timestamp for the audio data
     * @return audio message
     */
    private Audio getAudio(CorrectedBinaryMessage message, LICH lich, AudioCodec codec, long timestamp)
    {
        List<byte[]> frames = new ArrayList<>();

        if(lich.hasVoiceFirst())
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

        if(lich.hasVoiceSecond())
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

        return new Audio(codec, frames, timestamp);
    }

    /**
     * Decodes the outbound control frame
     * @param frame to decode
     * @return decoded message
     */
    public static CorrectedBinaryMessage decodeCAC(CorrectedBinaryMessage message)
    {
        CorrectedBinaryMessage deinterleaved = INTERLEAVER_CAC.deinterleave(message, 16);
        CorrectedBinaryMessage depunctured = PUNCTURE_PROVIDER_CAC.depuncture(deinterleaved);
        CorrectedBinaryMessage decoded = Convolution.decode(depunctured, PUNCTURE_PROVIDER_CAC);
        return decoded;
    }

    /**
     * Decodes the inbound long control frame
     * @param frame to decode
     * @return decoded message
     */
    private NXDNMessage decodeLongCAC(Frame frame)
    {
        CorrectedBinaryMessage cbm = deinterleave(frame.getMessage(), 16, 12, 21);
        return null;
    }

    /**
     * Decodes the inbound short control frame
     * @param frame to decode
     * @return decoded message
     */
    private NXDNMessage decodeShortCAC(Frame frame)
    {
        CorrectedBinaryMessage cbm = deinterleave(frame.getMessage(), 16, 12, 21);
        return null;
    }

    /**
     * Decodes the SACCH
     * @param frame to decode
     * @param codec indicating full or half rate AMBE+ audio
     * @return decoded message
     */
    private List<NXDNMessage> decodeSuperFrame(Frame frame, AudioCodec codec)
    {
        List<NXDNMessage> messages = new ArrayList<>();
        messages.add(getSACCH(frame.getMessage()));

        LICH lich = frame.getLICH();

        if(lich.hasFACCH1First())
        {
            messages.add(getFACCH1First(frame.getMessage()));
        }

        if(lich.hasFACCH1Second())
        {
            messages.add(getFACCH1Second(frame.getMessage()));
        }

        if(lich.hasVoiceFirst() || lich.hasVoiceSecond())
        {
            messages.add(getAudio(frame.getMessage(), lich, codec, frame.getTimestamp()));
        }

        return messages;
    }

    /**
     * Decodes the UDCH
     * @param frame to decode
     * @return decoded message
     */
    private NXDNMessage decodeUDCH(Frame frame)
    {
        CorrectedBinaryMessage cbm = deinterleave(frame.getMessage(), 16, 12, 29);
        Option option = frame.getLICH().getOption();

        if(option == Option.UDCH)
        {

        }
        else if(option == Option.FACCH2)
        {

        }
        else
        {
            throw new IllegalArgumentException("Unrecognized option: " + option);
        }

        return null;
    }


    /**
     * Deinterleave the transmitted message. The interleaved bit count is width * depth.
     * @param original transmitted/interleaved message
     * @param width of the dividing process (12 or 16) (step 7 in the ICD).
     * @param depth of the interleaving
     * @return deinterleaved message.
     */


    //TODO: get rid of this


    public static CorrectedBinaryMessage deinterleave(CorrectedBinaryMessage original, int offset, int width, int depth)
    {
        CorrectedBinaryMessage deinterleaved = new CorrectedBinaryMessage(width * depth);

        int pointer = 0;

        for(int column = 0; column < width; column++)
        {
            for(int row = 0; row < depth; row++)
            {
                if(original.get(offset + (row * width) + column))
                {
                    deinterleaved.set(pointer);
                }

                pointer++;
            }
        }

        return deinterleaved;
    }

    /**
     * Creates an NXDN message parser for a message on the outbound control channel (CC)
     * @param message content
     * @param timestamp of the message
     * @return message parser
     */
    public static NXDNLayer3Message getLayer3ControlOutbound(CorrectedBinaryMessage message, long timestamp)
    {
        int value = NXDNLayer3Message.getMessageTypeValue(message);
        NXDNMessageType type = NXDNMessageType.getCCOutbound(value);

        return switch (type)
        {
            case BROADCAST_SITE_INFORMATION -> new SiteInformation(message, timestamp);
            case BROADCAST_SERVICE_INFORMATION -> new ServiceInfo(message, timestamp);
            default -> new UnknownMessage(message, timestamp);
        };
    }


    /**
     * Creates an NXDN message parser for a message on the outbound traffic channel (TC)
     * @param message content
     * @param timestamp of the message
     * @return message parser
     */
    public static NXDNLayer3Message getTCOutbound(CorrectedBinaryMessage message, long timestamp)
    {
        int value = NXDNLayer3Message.getMessageTypeValue(message);
        NXDNMessageType type = NXDNMessageType.getTCOutbound(value);

        return switch (type)
        {
            case TC_DATA_CALL_ACKNOWLEDGE -> new DataCallAcknowledge(message, timestamp);
            case TC_DATA_CALL_BLOCK -> new DataCallBlock(message, timestamp);
            case TC_DATA_CALL_HEADER -> new DataCallHeader(message, timestamp);
            case TC_HEADER_DELAY -> new HeaderDelay(message, timestamp);
            case TC_REMOTE_CONTROL_REQUEST -> new RemoteControlRequest(message, timestamp);
            case TC_REMOTE_CONTROL_REQUEST_WITH_ESN -> new RemoteControlRequestWithESN(message, timestamp);
            case TC_REMOTE_CONTROL_RESPONSE -> new RemoteControlResponse(message, timestamp);
            case TC_REMOTE_CONTROL_RESPONSE_WITH_ESN -> new RemoteControlResponseWithESN(message, timestamp);
            case TC_SHORT_DATA_CALL_BLOCK ->  new ShortDataCallBlock(message, timestamp);
            case TC_SHORT_DATA_CALL_INITIALIZATION_VECTOR -> new ShortDataInitializationVector(message, timestamp);
            case TC_SHORT_DATA_CALL_REQUEST_HEADER ->  new ShortDataCallRequestHeader(message, timestamp);
            case TC_SHORT_DATA_CALL_RESPONSE -> new ShortDataCallResponse(message, timestamp);
            case TC_STATUS_INQUIRY_REQUEST -> new StatusInquiryRequest(message, timestamp);
            case TC_STATUS_INQUIRY_RESPONSE -> new StatusInquiryResponse(message, timestamp);
            case TC_STATUS_REQUEST -> new StatusRequest(message, timestamp);
            case TC_STATUS_RESPONSE -> new StatusResponse(message, timestamp);
            case TC_TRANSMISSION_RELEASE -> new TransmissionRelease(message, timestamp);
            case TC_TRANSMISSION_RELEASE_EXTENSION -> new TransmissionReleaseExtension(message, timestamp);
            case TC_VOICE_CALL -> new VoiceCall(message, timestamp);
            case TC_VOICE_CALL_INITIALIZATION_VECTOR -> new VoiceCallInitializationVector(message, timestamp);
            default -> new UnknownMessage(message, timestamp);
        };
    }

    /**
     * Creates an NXDN message parser for a message on the ingbound traffic channel (TC)
     * @param type of message
     * @param message content
     * @param timestamp of the message
     * @return message parser
     */
    public static NXDNLayer3Message getTCInbound(NXDNMessageType type, CorrectedBinaryMessage message, long timestamp)
    {
        return switch (type)
        {
            case TC_DATA_CALL_ACKNOWLEDGE -> new DataCallAcknowledge(message, timestamp);
            case TC_DATA_CALL_BLOCK -> new DataCallBlock(message, timestamp);
            case TC_DATA_CALL_HEADER -> new DataCallHeader(message, timestamp);
            case TC_HEADER_DELAY -> new HeaderDelay(message, timestamp);
            case TC_REMOTE_CONTROL_REQUEST -> new RemoteControlRequest(message, timestamp);
            case TC_REMOTE_CONTROL_RESPONSE -> new RemoteControlResponse(message, timestamp);
            case TC_REMOTE_CONTROL_RESPONSE_WITH_ESN -> new RemoteControlResponseWithESN(message, timestamp);
            case TC_SHORT_DATA_CALL_BLOCK ->  new ShortDataCallBlock(message, timestamp);
            case TC_SHORT_DATA_CALL_INITIALIZATION_VECTOR -> new ShortDataInitializationVector(message, timestamp);
            case TC_SHORT_DATA_CALL_REQUEST_HEADER ->  new ShortDataCallRequestHeader(message, timestamp);
            case TC_SHORT_DATA_CALL_RESPONSE -> new ShortDataCallResponse(message, timestamp);
            case TC_STATUS_INQUIRY_REQUEST -> new StatusInquiryRequest(message, timestamp);
            case TC_STATUS_INQUIRY_RESPONSE -> new StatusInquiryResponse(message, timestamp);
            case TC_STATUS_REQUEST -> new StatusRequest(message, timestamp);
            case TC_STATUS_RESPONSE -> new StatusResponse(message, timestamp);
            case TC_TRANSMISSION_RELEASE -> new TransmissionRelease(message, timestamp);
            case TC_TRANSMISSION_RELEASE_EXTENSION -> new TransmissionReleaseExtension(message, timestamp);
            case TC_VOICE_CALL -> new VoiceCall(message, timestamp);
            case TC_VOICE_CALL_INITIALIZATION_VECTOR -> new VoiceCallInitializationVector(message, timestamp);
            default -> new UnknownMessage(message, timestamp);
        };
    }

    static void main()
    {
        //Goal: get all collected frames to the depunctured state:
        String[] frames =
        {
                //Scrambled form
                "5D774228A08EDA887B0420A24888B43BAA0706022636E08BC2C2878300A11F42026202AA76A6280758A085775FD",
                "5D776188A11E2A88332820E22088C41AAA3E00822EB2A087C68283A000A10DA202E2E2AA3622282758A085775FD",
                "5D77E2E9B05FEA00FB50A822188CBE2A8A2A1083223FA0028202C72242A39EA003A3D32A56022C7758A085775FD",

                //Unscrambled form
//                "555D4AA00084F00053AC008048001631002F26800E9C40094A4005030001376000EA2000D4A4000DD00005775FD",
//                "555D6900011400001B8000C02000661000162000061800054E00012000012580006AC0009420002DD00005775FD",
//                "555DEA611055C088D3F8880018041C20200230010A9500800A8045A24203B682012BF180F400047DD00005775FD"
        };

//        for(int x = 0; x < 1; x++)
        for(int x = 0; x < frames.length; x++)
        {
            System.out.println(x + " FRAME: " + frames[x]);
            CorrectedBinaryMessage frameBits = CorrectedBinaryMessage.loadHex(frames[x]);
            System.out.println(x + " LOADD: " + frameBits.toHexString());
            Frame frame = new Frame(frameBits, System.currentTimeMillis(), RFChannel.RCCH, Direction.OUTBOUND);
            CorrectedBinaryMessage decoded = decodeCAC(frame);
            System.out.println("      DECODED: " + decoded);
            CorrectedBinaryMessage encoded = Convolution.encode(decoded);
            System.out.println("   RE-ENCODED: " + encoded);
        }
    }
}
