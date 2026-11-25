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

package io.github.dsheirer.gui.playlist.radioreference;

import io.github.dsheirer.module.decode.squelch.ctcss.CTCSSCode;
import io.github.dsheirer.module.decode.squelch.dcs.DCSCode;
import io.github.dsheirer.rrapi.type.Frequency;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tone or access code parsed from a Radio Reference frequency's tone field.
 *
 * Radio Reference writes the tone field in several forms: CTCSS as "118.8 PL" or "118.8", DCS as "023 DPL",
 * "D023N", "D023 N" or "D754I", P25 NAC as "NAC 293" or "$293", DMR color code as "CC1", "1 CC" or
 * "CC 3 TG 300 SL 1" and NXDN RAN as "RAN 1" or "13 RAN".
 * "CSQ" (carrier squelch) and blank fields have no tone.  Anything that isn't recognized, including fields that list
 * more than one tone, is treated as no tone rather than guessed.
 *
 * @param type of tone or access code
 * @param ctcssCode for a CTCSS tone, otherwise null
 * @param dcsCode for a DCS code, otherwise null
 * @param accessCode for a NAC, color code or RAN, otherwise 0
 */
public record RadioReferenceTone(Type type, CTCSSCode ctcssCode, DCSCode dcsCode, int accessCode)
{
    public enum Type {CTCSS, DCS, NAC, COLOR_CODE, RAN}

    /**
     * Maximum difference between a listed CTCSS frequency and a standard tone.
     */
    private static final float CTCSS_TOLERANCE_HZ = 0.15f;

    private static final Pattern CTCSS = Pattern.compile("^(\\d{2,3}\\.\\d{1,2})\\s*(?:PL|HZ|CTCSS|TPL)?$");
    private static final Pattern CTCSS_SUFFIXED = Pattern.compile("^(\\d{2,3})\\s*(?:PL|HZ|CTCSS|TPL)$");
    private static final Pattern DCS_SUFFIXED = Pattern.compile("^D?(\\d{3})\\s*(N|I|DPL|DCS|DPL\\s*N|DPL\\s*I|DCS\\s*N|DCS\\s*I)$");
    private static final Pattern DCS_PREFIXED = Pattern.compile("^(?:D|DPL\\s*|DCS\\s*)(\\d{3})\\s*(N|I)?$");
    private static final Pattern NAC = Pattern.compile("^(?:NAC\\s*(?:0X|\\$)?([0-9A-F]{1,3})|\\$([0-9A-F]{1,3})|([0-9A-F]{3})\\s*NAC)$");
    //DMR and NXDN entries can also list the talkgroup and slot after the code, e.g. "CC 3 TG 300 SL 1"
    //Code can be prefixed ("CC 1", "CC-1", "RAN:13") or suffixed ("1 CC", "13 RAN")
    private static final String ACCESS_CODE_TRAILER = "(?:\\s+(?:TG|SL|TS|SLOT)\\b.*)?$";
    private static final Pattern COLOR_CODE = Pattern.compile("^(?:(?:CC|COLOR\\s*CODE)\\s*[-:#]?\\s*(\\d{1,2})|(\\d{1,2})\\s*(?:CC|COLOR\\s*CODE))" + ACCESS_CODE_TRAILER);
    private static final Pattern RAN = Pattern.compile("^(?:RAN\\s*[-:#]?\\s*(\\d{1,2})|(\\d{1,2})\\s*RAN)" + ACCESS_CODE_TRAILER);

    /**
     * Tone field text for a Radio Reference frequency.  Radio Reference returns DMR entries with an empty tone field
     * and the color code, talkgroup and slot in separate fields, so those are combined as "CC 9 TG * SL *".
     * @param frequency from Radio Reference
     * @return tone text or null when the frequency has no tone or color code
     */
    public static String getToneField(Frequency frequency)
    {
        if(frequency == null)
        {
            return null;
        }

        String tone = frequency.getTone();

        if(tone != null && !tone.isBlank())
        {
            return tone.trim();
        }

        String colorCode = frequency.getColorCode();

        if(colorCode == null || colorCode.isBlank())
        {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        colorCode = colorCode.trim();
        sb.append(colorCode.matches("\\d+") ? "CC " + colorCode : colorCode);

        if(frequency.getTalkgroup() != null && !frequency.getTalkgroup().isBlank())
        {
            sb.append(" TG ").append(frequency.getTalkgroup().trim());
        }

        if(frequency.getSlot() != null && !frequency.getSlot().isBlank())
        {
            sb.append(" SL ").append(frequency.getSlot().trim());
        }

        return sb.toString();
    }

    /**
     * Parses a Radio Reference tone field.
     * @param tone field value, may be null
     * @return parsed tone, or null when the field has no tone or isn't recognized
     */
    public static RadioReferenceTone parse(String tone)
    {
        if(tone == null)
        {
            return null;
        }

        String value = tone.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", " ");

        if(value.isEmpty() || value.equals("CSQ") || value.contains("/") || value.contains(","))
        {
            return null;
        }

        Matcher matcher = CTCSS.matcher(value);

        if(!matcher.matches())
        {
            matcher = CTCSS_SUFFIXED.matcher(value);
        }

        if(matcher.matches())
        {
            CTCSSCode code = ctcss(Float.parseFloat(matcher.group(1)));
            return code != null ? new RadioReferenceTone(Type.CTCSS, code, null, 0) : null;
        }

        matcher = DCS_SUFFIXED.matcher(value);

        if(!matcher.matches())
        {
            matcher = DCS_PREFIXED.matcher(value);
        }

        if(matcher.matches())
        {
            String polarity = matcher.group(2);
            boolean inverted = polarity != null && polarity.endsWith("I");
            DCSCode code = dcs(matcher.group(1), inverted);
            return code != null ? new RadioReferenceTone(Type.DCS, null, code, 0) : null;
        }

        matcher = NAC.matcher(value);

        if(matcher.matches())
        {
            String hex = matcher.group(1) != null ? matcher.group(1) :
                matcher.group(2) != null ? matcher.group(2) : matcher.group(3);
            return new RadioReferenceTone(Type.NAC, null, null, Integer.parseInt(hex, 16));
        }

        matcher = COLOR_CODE.matcher(value);

        if(matcher.matches())
        {
            int colorCode = Integer.parseInt(matcher.group(matcher.group(1) != null ? 1 : 2));
            return colorCode <= 15 ? new RadioReferenceTone(Type.COLOR_CODE, null, null, colorCode) : null;
        }

        matcher = RAN.matcher(value);

        if(matcher.matches())
        {
            int ran = Integer.parseInt(matcher.group(matcher.group(1) != null ? 1 : 2));
            return ran <= 63 ? new RadioReferenceTone(Type.RAN, null, null, ran) : null;
        }

        return null;
    }

    /**
     * Standard CTCSS tone closest to the frequency, when it's within tolerance.
     */
    private static CTCSSCode ctcss(float frequency)
    {
        for(CTCSSCode code : CTCSSCode.STANDARD_CODES)
        {
            if(Math.abs(code.getFrequency() - frequency) <= CTCSS_TOLERANCE_HZ)
            {
                return code;
            }
        }

        return null;
    }

    /**
     * DCS code for the three digit octal code and polarity, when it's a valid code.
     */
    private static DCSCode dcs(String digits, boolean inverted)
    {
        try
        {
            return DCSCode.valueOf((inverted ? "I" : "N") + digits);
        }
        catch(IllegalArgumentException e)
        {
            return null;
        }
    }

    @Override
    public String toString()
    {
        return switch(type)
        {
            case CTCSS -> ctcssCode.getFrequency() + " PL";
            case DCS -> dcsCode.name();
            case NAC -> String.format("NAC %03X", accessCode);
            case COLOR_CODE -> "CC " + accessCode;
            case RAN -> "RAN " + accessCode;
        };
    }
}
