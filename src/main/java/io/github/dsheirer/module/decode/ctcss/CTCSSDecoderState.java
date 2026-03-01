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
package io.github.dsheirer.module.decode.ctcss;

import io.github.dsheirer.channel.state.DecoderState;
import io.github.dsheirer.channel.state.DecoderStateEvent;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.DecoderType;
import java.util.HashMap;
import java.util.Map;

/**
 * Maintains the decoder state from the stream of decoded CTCSS tone messages.
 */
public class CTCSSDecoderState extends DecoderState
{
    private Map<CTCSSCode,Integer> mCtcssCodeCountsMap = new HashMap<>();
    private CTCSSCode mCurrentCode = CTCSSCode.UNKNOWN;

    /**
     * Constructs an instance
     */
    public CTCSSDecoderState()
    {
    }

    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.CTCSS;
    }

    @Override
    public void receive(IMessage message)
    {
        if(message instanceof CTCSSMessage ctcssMessage)
        {
            if(ctcssMessage.isToneLost())
            {
                // Tone was lost - clear the identifier
                getIdentifierCollection().remove(Form.TONE);
                mCurrentCode = CTCSSCode.UNKNOWN;
            }
            else
            {
                mCurrentCode = ctcssMessage.getCTCSSCode();
                if(!mCtcssCodeCountsMap.containsKey(mCurrentCode))
                {
                    mCtcssCodeCountsMap.put(mCurrentCode, 1);
                }
                else
                {
                    mCtcssCodeCountsMap.put(mCurrentCode, mCtcssCodeCountsMap.get(mCurrentCode) + 1);
                }
                getIdentifierCollection().update(ctcssMessage.getIdentifiers());
            }
        }
    }

    @Override
    public void receiveDecoderStateEvent(DecoderStateEvent event)
    {
        switch(event.getEvent())
        {
            case REQUEST_RESET:
                resetState();
                break;
            default:
                break;
        }
    }

    @Override
    public String getActivitySummary()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("=============================\n");
        sb.append("Decoder:\tContinuous Tone-Coded Squelch System (CTCSS)\n\n");
        if(mCtcssCodeCountsMap.isEmpty())
        {
            sb.append("   Detected Tones: (none)\n");
        }
        else
        {
            for(Map.Entry<CTCSSCode,Integer> entry: mCtcssCodeCountsMap.entrySet())
            {
                sb.append("   ").append(entry.getKey()).append(" - Count: ").append(entry.getValue());
                if(entry.getKey().equals(mCurrentCode))
                {
                    sb.append(" - Current\n");
                }
                else
                {
                    sb.append("\n");
                }
            }
        }
        return sb.toString();
    }

    @Override
    public void init()
    {
    }
}