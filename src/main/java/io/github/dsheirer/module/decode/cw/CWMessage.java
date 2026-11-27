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

package io.github.dsheirer.module.decode.cw;

import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.message.Message;
import io.github.dsheirer.protocol.Protocol;
import java.util.Collections;
import java.util.List;

/**
 * Morse code (CW) station ID message.
 */
public class CWMessage extends Message
{
    private final String mText;
    private final int mToneFrequency;
    private final int mWordsPerMinute;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs an instance
     * @param timestamp of the start of the ID
     * @param text decoded
     * @param toneFrequency of the CW tone in Hertz
     * @param wordsPerMinute keying speed
     */
    public CWMessage(long timestamp, String text, int toneFrequency, int wordsPerMinute)
    {
        super(timestamp);
        mText = text;
        mToneFrequency = toneFrequency;
        mWordsPerMinute = wordsPerMinute;
    }

    /**
     * Decoded text
     */
    public String getText()
    {
        return mText;
    }

    /**
     * Station call sign: the word in the text that contains both letters and digits (the format of a call sign), or
     * the full text when no word has that format.
     */
    public String getCallSign()
    {
        String callSign = null;

        for(String word: mText.split(" "))
        {
            if(word.chars().anyMatch(Character::isLetter) && word.chars().anyMatch(Character::isDigit) &&
                (callSign == null || word.length() > callSign.length()))
            {
                callSign = word;
            }
        }

        return callSign != null ? callSign : mText;
    }

    /**
     * Frequency of the CW tone in Hertz
     */
    public int getToneFrequency()
    {
        return mToneFrequency;
    }

    /**
     * Keying speed in words per minute
     */
    public int getWordsPerMinute()
    {
        return mWordsPerMinute;
    }

    @Override
    public boolean isValid()
    {
        return true;
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.CW;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = Collections.singletonList(new CWStationIdentifier(getCallSign()));
        }

        return mIdentifiers;
    }

    @Override
    public String toString()
    {
        return "CW ID [" + mText + "] TONE:" + mToneFrequency + "HZ SPEED:" + mWordsPerMinute + "WPM";
    }
}
