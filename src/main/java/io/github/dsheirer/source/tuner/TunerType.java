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
package io.github.dsheirer.source.tuner;

import java.util.EnumSet;

/**
 * List of known and optionally supported tuner types
 */
public enum TunerType
{
    AIRSPY_R820T("R820T"),
    AIRSPY_HF_PLUS("HF+"),
    ELONICS_E4000("E4000"),
    FCI_FC2580("FC2580"),
    FITIPOWER_FC0012("FC0012"),
    FITIPOWER_FC0013("FC0013"),
    FUNCUBE_DONGLE_PRO("Funcube Dongle Pro"),
    FUNCUBE_DONGLE_PRO_PLUS("Funcube Dongle Pro Plus"),
    HACKRF_ONE("ONE"),
    HACKRF_JAWBREAKER("Jawbreaker"),
    HACKRF_RAD1O("RAD1O"),
    RAFAELMICRO_R820T("R820T"),
    RAFAELMICRO_R828D("R828D"),
    RSP_1("RSP1"),
    RSP_1A("RSP1A"),
    RSP_2("RSP2"),
    RSP_DUO_1("RSPduo Tuner 1"),
    RSP_DUO_2("RSPduo Tuner 2"),
    RSP_DX("RSPdx"),

    TEST("Test"),
    RECORDING("Recording"),
    UNKNOWN("Unknown");

    private String mLabel;

    /**
     * Constructs an instance
     *
     * @param label pretty
     */
    TunerType(String label)
    {
        mLabel = label;
    }

    public String getLabel()
    {
        return mLabel;
    }



    /**
     * Supported USB tuner types
     */
    public static EnumSet<TunerType> SUPPORTED_USB_TUNERS = EnumSet.of(AIRSPY_R820T, ELONICS_E4000, FITIPOWER_FC0012,
            HACKRF_JAWBREAKER, HACKRF_RAD1O, HACKRF_ONE, RAFAELMICRO_R820T);

    /**
     * Supported sound card tuner types
     */
    public static EnumSet<TunerType> SUPPORTED_SOUND_CARD_TUNERS = EnumSet.of(FUNCUBE_DONGLE_PRO, FUNCUBE_DONGLE_PRO_PLUS);

    /**
     * Indicates if this tuner is supported as a USB tuner
     */
    public boolean isSupportedUsbTuner()
    {
        return SUPPORTED_USB_TUNERS.contains(this);
    }

    /**
     * Indicates if this tuner type is supported as a sound card tuner
     */
    public boolean isSupportedSoundCardTuner()
    {
        return SUPPORTED_SOUND_CARD_TUNERS.contains(this);
    }

    @Override
    public String toString()
    {
        return getLabel();
    }
}
