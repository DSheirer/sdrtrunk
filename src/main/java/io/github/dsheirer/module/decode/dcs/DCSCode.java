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

package io.github.dsheirer.module.decode.dcs;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * DCS codes as defined in ETSI TS 103 236 V1.1.1
 *
 * Code values are the bit reversed integer values of the 23-bit binary sequence representation.  The values are bit
 * reversed for normal (N) codes and as specified in the ETSI chart for the inverted (I) codes to align with the
 * transmitted format.
 */
public enum DCSCode
{
    //Inverted codes
    I023(7747603),
    I025(7043093),
    I026(6674454),
    I031(5371929),
    I032(6248474),
    I043(5990435),
    I047(1038375),
    I051(8169513),
    I054(7292972),
    I065(6101045),
    I071(6789177),
    I072(6895674),
    I073(3041339),
    I074(7632956),
    I114(3532876),
    I115(7518285),
    I116(8132686),
    I125(505941),
    I131(4012121),
    I132(3381338),
    I134(3070044),
    I143(3647587),
    I152(2017386),
    I155(4511853),
    I156(4880494),
    I162(7063666),
    I165(3266677),
    I172(391290),
    I174(1620092),
    I205(7248005),
    I223(6875283),
    I226(8063126),
    I243(4569251),
    I244(2074788),
    I245(5830821),
    I251(6453417),
    I261(1538225),
    I263(6195379),
    I265(4442293),
    I271(7948473),
    I306(850118),
    I311(3725513),
    I315(7104717),
    I331(2353369),
    I343(2717923),
    I346(3840230),
    I351(964841),
    I364(6838516),
    I365(3082485),
    I371(1411321),
    I411(7825673),
    I412(7981322),
    I413(4102411),
    I423(4954387),
    I431(7100697),
    I432(6486298),
    I445(8096037),
    I464(2615604),
    I465(6338869),
    I466(7215414),
    I503(3959107),
    I506(3115334),
    I516(4307278),
    I532(932186),
    I546(1698150),
    I565(817525),
    I606(6134150),
    I612(6756746),
    I624(1005972),
    I627(129431),
    I631(7506329),
    I632(8137114),
    I654(4995500),
    I662(2390450),
    I664(3750324),
    I703(2275779),
    I712(776650),
    I723(3770835),
    I731(1984985),
    I732(1108442),
    I734(895452),
    I743(1366499),
    I754(2161132),

    //Normal codes
    N023(6557239),
    N025(5508971),
    N026(3411411),
    N031(4984773),
    N032(2887037),
    N043(6425453),
    N047(7474680),
    N051(4852383),
    N054(1706363),
    N065(5639261),
    N071(5115123),
    N072(3018315),
    N073(7211834),
    N074(1969943),
    N114(1641430),
    N115(5836455),
    N116(3738655),
    N125(5574384),
    N131(5049950),
    N132(2952422),
    N134(1904058),
    N143(6490870),
    N152(2820540),
    N155(5967249),
    N156(3870505),
    N162(2558443),
    N165(5705158),
    N172(3084240),
    N174(2035340),
    N205(5278907),
    N223(6589323),
    N226(3442799),
    N243(6459089),
    N244(1215228),
    N245(5410701),
    N251(4886307),
    N261(4624244),
    N263(6719677),
    N265(5671393),
    N271(5146959),
    N306(3248024),
    N311(4820366),
    N315(5868315),
    N331(5082082),
    N343(6524746),
    N346(3378350),
    N351(4951736),
    N364(1543435),
    N365(5736570),
    N371(5212372),
    N411(4737911),
    N412(2640335),
    N413(6835390),
    N423(6573289),
    N431(5000475),
    N432(2903971),
    N445(5392623),
    N464(1461234),
    N465(5656195),
    N466(3558459),
    N503(6376222),
    N506(3229946),
    N516(3755713),
    N532(2969144),
    N546(3361740),
    N565(5721880),
    N606(3198173),
    N612(2673779),
    N624(1363320),
    N627(7655360),
    N631(5032103),
    N632(2935327),
    N654(1756697),
    N662(2543378),
    N664(1494606),
    N703(6409890),
    N712(2739688),
    N723(6670542),
    N731(5097788),
    N732(3001220),
    N734(1952472),
    N743(6540692),
    N754(1822594),
    UNKNOWN(0);

    private int mValue;

    DCSCode(int value)
    {
        mValue = value;
    }

    /**
     * Inverted DCS codes
     */
    public static final EnumSet<DCSCode> INVERTED_CODES = EnumSet.range(I023, I754);

    /**
     * Standard DCS codes
     */
    public static final EnumSet<DCSCode> STANDARD_CODES = EnumSet.range(N023,N754);

    /**
     * Lookup map for quickly finding a DCS code from the transmitted value.
     */
    private static final Map<Integer,DCSCode> CODE_MAP = new HashMap<>();

    static
    {
       for(DCSCode code: EnumSet.range(I023, N754))
        {
            CODE_MAP.put(code.getValue(), code);
        }
    }

    /**
     * Integer value of the code from transmitted bits.
     * @return value
     */
    public int getValue()
    {
        return mValue;
    }

    /**
     * Indicates if this is an inverted DCS code (true) or a normal DCS code (false).
     * @return true if inverted.
     */
    public boolean isInverted()
    {
        return INVERTED_CODES.contains(this);
    }

    @Override
    public String toString()
    {
        return "DCS-" + name().substring(1, 4) + (isInverted() ? " Inverted" : "");
    }

    /**
     * Indicates if the value maps to a known DCS code.
     * @param value to test
     * @return true if the value matches a DCS code.
     */
    public static boolean hasValue(int value)
    {
        return CODE_MAP.containsKey(value);
    }

    /**
     * Get the DCS code that matches the value, otherwise return UNKNOWN.
     * @param value to lookup
     * @return matching DCS code or UNKNOWN.
     */
    public static DCSCode fromValue(int value)
    {
        DCSCode code = CODE_MAP.get(value);

        if(code == null)
        {
            code = UNKNOWN;
        }

        return code;
    }
}
