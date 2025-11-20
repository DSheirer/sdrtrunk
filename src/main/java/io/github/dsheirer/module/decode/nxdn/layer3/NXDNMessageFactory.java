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
import io.github.dsheirer.module.decode.nxdn.layer2.LICH;
import io.github.dsheirer.module.decode.nxdn.layer2.Option;
import io.github.dsheirer.module.decode.nxdn.layer3.type.AudioCodec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Factory for creating NXDN messages
 */
public class NXDNMessageFactory
{
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
                return Collections.singletonList(decodeCAC(frame));
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
    private NXDNMessage decodeCAC(Frame frame)
    {
        CorrectedBinaryMessage cbm = deinterleave(frame.getMessage(), 16, 12, 25);
        CorrectedBinaryMessage expanded = new CorrectedBinaryMessage(350);

        int pointer = 0;

        //De-Puncture the deinterleaved message
        for(int x = 0; x < 300; x++)
        {
            if(cbm.get(x))
            {
                expanded.set(pointer);
            }

            pointer++;

            if((pointer % 14 == 3) || (pointer % 14 == 11))
            {
                pointer++;
            }
        }

        return null;
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
     * Creates an NXDN message parser for a message on the outbound traffic channel (TC)
     * @param type of message
     * @param message content
     * @param timestamp of the message
     * @return message parser
     */
    public static NXDNLayer3Message getTCOutbound(NXDNMessageType type, CorrectedBinaryMessage message, long timestamp)
    {
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
}
