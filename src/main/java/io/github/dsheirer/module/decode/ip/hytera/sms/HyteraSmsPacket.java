/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.module.decode.ip.hytera.sms;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.module.decode.dmr.identifier.DmrTier3Radio;
import io.github.dsheirer.module.decode.ip.IPacket;
import io.github.dsheirer.module.decode.ip.hytera.sds.DestinationId;
import io.github.dsheirer.module.decode.ip.hytera.sds.Encoding;
import io.github.dsheirer.module.decode.ip.hytera.sds.HyteraToken;
import io.github.dsheirer.module.decode.ip.hytera.sds.HyteraTokenHeader;
import io.github.dsheirer.module.decode.ip.hytera.sds.HyteraTokenType;
import io.github.dsheirer.module.decode.ip.hytera.sds.MessageId;
import io.github.dsheirer.module.decode.ip.hytera.sds.Payload;
import io.github.dsheirer.module.decode.ip.hytera.sds.SourceId;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * Hytera SDS Long Data Message with SMS encoded payload
 */
public class HyteraSmsPacket implements IPacket
{
    private HyteraTokenHeader mHeader;
    private String mSMS;
    private RadioIdentifier mSourceRadio;
    private RadioIdentifier mDestinationRadio;
    private List<Identifier> mIdentifiers;
    private MessageId mMessageId;

    /**
     * Constructs a parser for a header contained within a binary message starting at the offset.
     *
     * @param header to the packet within the message
     */
    public HyteraSmsPacket(HyteraTokenHeader header)
    {
        mHeader = header;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("HYTERA LONG SMS");
        if(hasSMS())
        {
            sb.append(":").append(getSMS());
        }
        if(hasMessageId())
        {
            sb.append(" ID:").append(getMessageId().getId());
        }
        if(hasSource())
        {
            sb.append(" SOURCE:").append(getSource());
        }
        if(hasDestination())
        {
            sb.append("DESTINATION:").append(getDestination());
        }

        return sb.toString();
    }

    @Override
    public HyteraTokenHeader getHeader()
    {
        return mHeader;
    }

    /**
     * Message ID for the SMS message
     */
    public MessageId getMessageId()
    {
        if(mMessageId == null)
        {
            HyteraToken idToken = getHeader().getTokenByType(HyteraTokenType.ID_MESSAGE);

            if(idToken instanceof MessageId messageId)
            {
                mMessageId = messageId;
            }
        }

        return mMessageId;
    }

    /**
     * Indicates if the SMS has a message ID
     * @return message ID
     */
    public boolean hasMessageId()
    {
        return getMessageId() != null;
    }

    /**
     * Source radio identifier for the SMS message
     */
    public RadioIdentifier getSource()
    {
        if(mSourceRadio == null)
        {
            HyteraToken sourceToken = getHeader().getTokenByType(HyteraTokenType.ID_SOURCE);

            if(sourceToken instanceof SourceId sourceId)
            {
                mSourceRadio = DmrTier3Radio.createFrom(sourceId.getId());
            }
        }

        return mSourceRadio;
    }

    /**
     * Indicates if the SMS message has a source identifier
     */
    public boolean hasSource()
    {
        return getSource() != null;
    }

    /**
     * Destination radio identifier for the SMS message
     */
    public RadioIdentifier getDestination()
    {
        if(mDestinationRadio == null)
        {
            HyteraToken destinationToken = getHeader().getTokenByType(HyteraTokenType.ID_DESTINATION);

            if(destinationToken instanceof DestinationId destinationId)
            {
                mDestinationRadio = DmrTier3Radio.createTo(destinationId.getId());
            }
        }

        return mDestinationRadio;
    }

    /**
     * Indicates if the SMS message has a destination identifier
     */
    public boolean hasDestination()
    {
        return getDestination() != null;
    }

    /**
     * Indicates if this payload has an SMS message
     * @return true if SMS
     */
    public boolean hasSMS()
    {
        return getSMS() != null;
    }

    /**
     * Parsed SMS message
     * @return
     */
    public String getSMS()
    {
        if(mSMS == null)
        {
            HyteraToken encodingToken = getHeader().getTokenByType(HyteraTokenType.ENCODING);

            if(encodingToken instanceof Encoding encoding)
            {
                HyteraToken payloadToken = getHeader().getTokenByType(HyteraTokenType.PAYLOAD);

                if(payloadToken instanceof Payload payload)
                {
                    switch(encoding.getEncoding())
                    {
                        case ISO_7:
                            mSMS = parseISO7Payload(payload.getPayload());
                            break;
                        case ISO_8:
                            mSMS = parseISO8Payload(payload.getPayload());
                            break;
                        case UNICODE:
                            mSMS = parseUnicodePayload(payload.getPayload());
                            break;
                        case GBK:
                            mSMS = parseGB2312Payload(payload.getPayload());
                            break;
                    }
                }
            }
        }

        return mSMS;
    }

    /**
     * Parses a GB2312 Simplified Chinese Characters 16-bit payload from the message.
     * @return parsed message
     */
    private String parseGB2312Payload(BinaryMessage message)
    {
        int length = message.size();

        if(length > 16)
        {
            try
            {
                return message.parseGB2312(0, (length / 16));
            }
            catch(UnsupportedEncodingException uee)
            {
                return "(GB2312 unsupported decoding)";
            }
        }
        else
        {
            return "(insufficient data)";
        }
    }

    /**
     * Parses a unicode 16-bit payload from the message.
     * @return parsed message
     */
    private String parseUnicodePayload(BinaryMessage message)
    {
        int length = message.size();

        if(length > 16)
        {
            return message.parseUnicode(0, (length / 16));
        }
        else
        {
            return "(insufficient data)";
        }
    }

    /**
     * Parses an ISO-7 payload from the message.
     * @return parsed message
     */
    private String parseISO7Payload(BinaryMessage message)
    {
        int length = message.size();

        if(length > 7)
        {
            return message.parseISO7(0, (length / 7));
        }
        else
        {
            return "(insufficient data)";
        }
    }

    /**
     * Parses an ISO-8 payload from the message.
     * @return parsed message
     */
    private String parseISO8Payload(BinaryMessage message)
    {
        int length = message.size();

        if(length > 8)
        {
            return message.parseISO8(0, (length / 8));
        }
        else
        {
            return "(insufficient data)";
        }
    }


    @Override
    public IPacket getPayload() {return null;}
    @Override
    public boolean hasPayload() {return false;}

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            if(hasSource())
            {
                mIdentifiers.add(getSource());
            }
            if(hasDestination())
            {
                mIdentifiers.add(getDestination());
            }
        }

        return mIdentifiers;
    }
}
