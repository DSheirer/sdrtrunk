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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses a sequence of key-down (tone on) and key-up (tone off) durations into Morse code text.
 *
 * Morse timing is relative to the dot (unit) length: a dot is 1 unit, a dash is 3 units, the gap between the elements
 * of a character is 1 unit, the gap between characters is 3 units and the gap between words is 7 units.  The unit
 * length is estimated from the gaps, since the gaps between elements within characters are the most common gaps.
 *
 * Since steady tones, voice and data signals can be mistaken for keyed tones, the result is only accepted when the
 * timing is consistent with Morse code: the key-down durations cluster at 1 and 3 units, the speed is plausible and
 * all of the characters are valid.
 */
public class MorseCodeParser
{
    public static final int MINIMUM_UNIT_MILLISECONDS = 15;  //80 WPM
    public static final int MAXIMUM_UNIT_MILLISECONDS = 150; //8 WPM
    private static final int MINIMUM_CHARACTERS = 3;
    private static final int MAXIMUM_CHARACTERS = 64;
    private static final double MINIMUM_CONSISTENT_ELEMENT_RATIO = 0.85;
    private static final Map<String,Character> CODE_TO_CHARACTER = new HashMap<>();
    private static final Map<Character,String> CHARACTER_TO_CODE = new HashMap<>();

    static
    {
        String[][] codes = {
            {"A", ".-"}, {"B", "-..."}, {"C", "-.-."}, {"D", "-.."}, {"E", "."}, {"F", "..-."}, {"G", "--."},
            {"H", "...."}, {"I", ".."}, {"J", ".---"}, {"K", "-.-"}, {"L", ".-.."}, {"M", "--"}, {"N", "-."},
            {"O", "---"}, {"P", ".--."}, {"Q", "--.-"}, {"R", ".-."}, {"S", "..."}, {"T", "-"}, {"U", "..-"},
            {"V", "...-"}, {"W", ".--"}, {"X", "-..-"}, {"Y", "-.--"}, {"Z", "--.."},
            {"0", "-----"}, {"1", ".----"}, {"2", "..---"}, {"3", "...--"}, {"4", "....-"}, {"5", "....."},
            {"6", "-...."}, {"7", "--..."}, {"8", "---.."}, {"9", "----."},
            {"/", "-..-."}, {"?", "..--.."}, {".", ".-.-.-"}, {",", "--..--"}, {"-", "-....-"}, {"=", "-...-"}
        };

        for(String[] code: codes)
        {
            CODE_TO_CHARACTER.put(code[1], code[0].charAt(0));
            CHARACTER_TO_CODE.put(code[0].charAt(0), code[1]);
        }
    }

    private MorseCodeParser()
    {
    }

    /**
     * Morse code (dots and dashes) for the character
     * @param character to look up (upper case)
     * @return code or null if the character is not supported
     */
    public static String getCode(char character)
    {
        return CHARACTER_TO_CODE.get(Character.toUpperCase(character));
    }

    /**
     * Parses the keying runs into text.
     * @param runs alternating key-down and key-up durations, starting and ending with key-down runs
     * @return result, or null if the keying is not consistent with Morse code
     */
    public static Result parse(List<KeyingRun> runs)
    {
        List<Integer> keyDown = new ArrayList<>();
        List<Integer> gaps = new ArrayList<>();

        for(KeyingRun run: runs)
        {
            (run.keyed() ? keyDown : gaps).add(run.milliseconds());
        }

        if(keyDown.size() < MINIMUM_CHARACTERS || gaps.size() < 2)
        {
            return null;
        }

        double unit = estimateUnit(gaps);

        if(unit < MINIMUM_UNIT_MILLISECONDS || unit > MAXIMUM_UNIT_MILLISECONDS)
        {
            return null;
        }

        //Key-down durations must cluster at 1 unit (dot) and 3 units (dash)
        int consistent = 0;

        for(int duration: keyDown)
        {
            double units = duration / unit;

            if((units >= 0.5 && units <= 1.7) || (units >= 2.2 && units <= 4.5))
            {
                consistent++;
            }
        }

        if(consistent < keyDown.size() * MINIMUM_CONSISTENT_ELEMENT_RATIO)
        {
            return null;
        }

        StringBuilder text = new StringBuilder();
        StringBuilder character = new StringBuilder();

        for(KeyingRun run: runs)
        {
            double units = run.milliseconds() / unit;

            if(run.keyed())
            {
                character.append(units < 2.0 ? '.' : '-');
            }
            else if(units >= 2.0)
            {
                if(!appendCharacter(text, character))
                {
                    return null;
                }

                if(units >= 5.0)
                {
                    text.append(' ');
                }
            }
        }

        if(!appendCharacter(text, character))
        {
            return null;
        }

        String decoded = text.toString().trim();
        int characters = decoded.replace(" ", "").length();

        if(characters < MINIMUM_CHARACTERS || characters > MAXIMUM_CHARACTERS)
        {
            return null;
        }

        return new Result(decoded, (int)Math.round(1200.0 / unit));
    }

    /**
     * Appends the decoded character and clears the character code.
     * @return false if the code is not a valid character
     */
    private static boolean appendCharacter(StringBuilder text, StringBuilder code)
    {
        if(code.isEmpty())
        {
            return true;
        }

        Character character = CODE_TO_CHARACTER.get(code.toString());
        code.setLength(0);

        if(character == null)
        {
            return false;
        }

        text.append(character);
        return true;
    }

    /**
     * Estimates the unit (dot) length as the median of the shorter half of the gaps.  The gaps between the elements
     * of a character (1 unit) are normally the majority of the gaps.
     */
    static double estimateUnit(List<Integer> gaps)
    {
        List<Integer> sorted = new ArrayList<>(gaps);
        Collections.sort(sorted);
        List<Integer> shorter = sorted.subList(0, Math.max(1, (sorted.size() + 1) / 2));
        return shorter.get(shorter.size() / 2);
    }

    /**
     * Key-down (tone on) or key-up (tone off) duration.
     * @param keyed true for tone on
     * @param milliseconds duration
     */
    public record KeyingRun(boolean keyed, int milliseconds)
    {
    }

    /**
     * Parsed Morse code
     * @param text decoded, with words separated by a single space
     * @param wordsPerMinute speed, from the unit length (PARIS standard: 1200 / unit milliseconds)
     */
    public record Result(String text, int wordsPerMinute)
    {
    }
}
