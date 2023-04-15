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

package io.github.dsheirer.source.tuner.airspy.hf;

/**
 * Requests/Commands supported by the Airspy HF
 */
public enum Request
{
    INVALID(0),
    RECEIVER_MODE(1),
    SET_FREQUENCY(2),
    GET_SAMPLE_RATES(3),
    SET_SAMPLE_RATE(4),
    CONFIG_READ(5),
    CONFIG_WRITE(6),
    GET_SERIAL_NUMBER_BOARD_ID(7),
    SET_USER_OUTPUT(8),
    GET_VERSION_STRING(9),
    SET_HF_AGC(10),
    SET_HF_AGC_THRESHOLD(11),
    SET_HF_ATT(12),
    SET_HF_LNA(13),
    GET_SAMPLE_RATE_ARCHITECTURES(14),
    GET_FILTER_GAIN(15),
    GET_FREQUENCY_DELTA(16),
    SET_VCTCXO_CALIBRATION(17);

    private int mValue;

    /**
     * Constructs an instance
     * @param value of the entry
     */
    Request(int value)
    {
        mValue = value;
    }

    /**
     * Value associated with the entry
     * @return value cast to a byte.
     */
    public byte getRequest()
    {
        return (byte)mValue;
    }
}
