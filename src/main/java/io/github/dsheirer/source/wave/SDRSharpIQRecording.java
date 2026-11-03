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
package io.github.dsheirer.source.wave;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Metadata for an SDRSharp raw I/Q baseband recording (.iq file extension).
 *
 * These recordings contain headerless, interleaved, signed 16-bit little-endian I/Q samples.  The recording metadata
 * is encoded in the file name using the format:
 *
 *   yyyyMMdd-HHmmss_[label_]TUNER_[frequency][Hz|kHz|MHz|GHz]_[rate][Sps|kSps|MSps]
 *
 * Example: 20261102-071525_toronto_AIRSPY_424461724Hz_10MSps.iq
 */
public class SDRSharpIQRecording
{
    public static final String FILE_EXTENSION = "iq";
    public static final int BYTES_PER_FRAME = 4; //16-bit I + 16-bit Q
    public static final int BITS_PER_SAMPLE = 16;
    public static final int CHANNELS = 2;

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final Pattern FILE_NAME_PATTERN = Pattern.compile(
        "^(\\d{8}-\\d{6})_(?:(.*)_)?([^_]+)_(\\d+(?:\\.\\d+)?)([kKMG]?)Hz_(\\d+(?:\\.\\d+)?)([kKMG]?)(?:S(?:ps|PS)|sps)?\\.iq$",
        Pattern.CASE_INSENSITIVE);

    private final File mFile;
    private final LocalDateTime mStartTime;
    private final String mLabel;
    private final String mTunerType;
    private final long mCenterFrequency;
    private final double mSampleRate;

    private SDRSharpIQRecording(File file, LocalDateTime startTime, String label, String tunerType,
                                long centerFrequency, double sampleRate)
    {
        mFile = file;
        mStartTime = startTime;
        mLabel = label;
        mTunerType = tunerType;
        mCenterFrequency = centerFrequency;
        mSampleRate = sampleRate;
    }

    /**
     * Indicates if the file has the SDRSharp .iq file extension.
     */
    public static boolean hasIQExtension(File file)
    {
        return file != null && file.getName().toLowerCase().endsWith("." + FILE_EXTENSION);
    }

    /**
     * Parses the recording metadata from the file name.
     * @param file to parse
     * @return parsed recording or null if the file name does not match the SDRSharp .iq file naming format.
     */
    public static SDRSharpIQRecording parse(File file)
    {
        if(file == null)
        {
            return null;
        }

        Matcher m = FILE_NAME_PATTERN.matcher(file.getName());

        if(!m.matches())
        {
            return null;
        }

        try
        {
            LocalDateTime startTime = LocalDateTime.parse(m.group(1), TIMESTAMP_FORMATTER);
            String label = m.group(2);
            String tunerType = m.group(3);
            long frequency = scale(m.group(4), m.group(5)).longValue();
            double sampleRate = scale(m.group(6), m.group(7)).doubleValue();

            if(frequency <= 0 || sampleRate <= 0)
            {
                return null;
            }

            return new SDRSharpIQRecording(file, startTime, label, tunerType, frequency, sampleRate);
        }
        catch(DateTimeParseException | NumberFormatException | ArithmeticException e)
        {
            return null;
        }
    }

    /**
     * Creates an SDRSharp compatible recording file name.
     * @param startTime of the recording
     * @param label optional (nullable) label (e.g. channel name)
     * @param tunerType tuner type (e.g. AIRSPY).  Underscores are replaced with hyphens since the underscore is the
     * file name field delimiter.
     * @param frequency center frequency in Hertz
     * @param sampleRate in samples per second
     * @return file name, including the .iq extension
     */
    public static String createFileName(LocalDateTime startTime, String label, String tunerType, long frequency,
                                        double sampleRate)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(TIMESTAMP_FORMATTER.format(startTime)).append("_");

        if(label != null && !label.isBlank())
        {
            sb.append(label.trim()).append("_");
        }

        String tuner = (tunerType == null || tunerType.isBlank()) ? "UNKNOWN" : tunerType.trim();
        sb.append(tuner.replace("_", "-")).append("_");
        sb.append(frequency).append("Hz_");
        sb.append(formatSampleRate(sampleRate)).append(".").append(FILE_EXTENSION);
        return sb.toString();
    }

    /**
     * Formats the sample rate using the largest metric unit prefix (e.g. 10000000 = 10MSps, 2500000 = 2.5MSps,
     * 25000 = 25kSps).  The rate is rounded to the nearest whole sample per second.
     */
    public static String formatSampleRate(double sampleRate)
    {
        BigDecimal rate = BigDecimal.valueOf(Math.round(sampleRate));
        String prefix = "";

        if(rate.compareTo(BigDecimal.valueOf(1_000_000)) >= 0)
        {
            rate = rate.movePointLeft(6);
            prefix = "M";
        }
        else if(rate.compareTo(BigDecimal.valueOf(1_000)) >= 0)
        {
            rate = rate.movePointLeft(3);
            prefix = "k";
        }

        return rate.stripTrailingZeros().toPlainString() + prefix + "Sps";
    }

    /**
     * Applies the metric unit prefix (k, M, G) to the value.
     */
    private static BigDecimal scale(String value, String prefix)
    {
        BigDecimal decimal = new BigDecimal(value);

        return switch(prefix)
        {
            case "k", "K" -> decimal.movePointRight(3);
            case "M" -> decimal.movePointRight(6);
            case "G", "g" -> decimal.movePointRight(9);
            //Lower-case 'm' (milli) makes no sense for frequency or sample rate - treat as mega.
            case "m" -> decimal.movePointRight(6);
            default -> decimal;
        };
    }

    public File getFile()
    {
        return mFile;
    }

    /**
     * Recording start time (local time of the recording computer).
     */
    public LocalDateTime getStartTime()
    {
        return mStartTime;
    }

    /**
     * Optional user label, or null.
     */
    public String getLabel()
    {
        return mLabel;
    }

    /**
     * Tuner type text from the file name (e.g. AIRSPY)
     */
    public String getTunerType()
    {
        return mTunerType;
    }

    /**
     * Center frequency in Hertz
     */
    public long getCenterFrequency()
    {
        return mCenterFrequency;
    }

    /**
     * Sample rate in samples per second
     */
    public double getSampleRate()
    {
        return mSampleRate;
    }

    @Override
    public String toString()
    {
        return "SDRSharp I/Q Recording - Tuner:" + mTunerType + " Start:" + mStartTime + " Frequency:" +
            mCenterFrequency + " Sample Rate:" + mSampleRate + (mLabel != null ? " Label:" + mLabel : "");
    }
}
