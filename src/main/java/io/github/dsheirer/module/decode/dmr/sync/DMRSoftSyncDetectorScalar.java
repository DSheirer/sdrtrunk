/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

package io.github.dsheirer.module.decode.dmr.sync;

/**
 * Scalar implementation of DMR Soft Sync Detector.
 */
public class DMRSoftSyncDetectorScalar extends DMRSoftSyncDetector
{
    /**
     * Processes the demodulated soft symbol value and returns a correlation value against the preceding 24 soft
     * symbols that include this recent soft symbol.
     *
     * @return correlation score.
     */
    public float calculate()
    {
        float symbol;

        switch(mMode)
        {
            case AUTOMATIC:
                float allBaseData = 0;
                float allBaseVoice = 0;
                float allMobileData = 0;
                float allMobileVoice = 0;
                float allDirectData1 = 0;
                float allDirectData2 = 0;
                float allDirectVoice1 = 0;
                float allDirectVoice2 = 0;

                for(int x = 0; x < 24; x++)
                {
                    symbol = mSymbols[mSymbolPointer + x];

                    allBaseData += BASE_DATA[x] * symbol;
                    allBaseVoice += BASE_VOICE[x] * symbol;
                    allMobileData += MOBILE_DATA[x] * symbol;
                    allMobileVoice += MOBILE_VOICE[x] * symbol;
                    allDirectData1 += DIRECT_DATA_1[x] * symbol;
                    allDirectData2 += DIRECT_DATA_2[x] * symbol;
                    allDirectVoice1 += DIRECT_VOICE_1[x] * symbol;
                    allDirectVoice2 += DIRECT_VOICE_2[x] * symbol;
                }

                float allBestScore = allBaseData;
                mDetectedPattern = DMRSyncPattern.BASE_STATION_DATA;

                if(allBaseVoice > allBestScore)
                {
                    allBestScore = allBaseVoice;
                    mDetectedPattern = DMRSyncPattern.BASE_STATION_VOICE;
                }

                if(allMobileData > allBestScore)
                {
                    allBestScore = allMobileData;
                    mDetectedPattern = DMRSyncPattern.MOBILE_STATION_DATA;
                }

                if(allMobileVoice > allBestScore)
                {
                    allBestScore = allMobileVoice;
                    mDetectedPattern = DMRSyncPattern.MOBILE_STATION_VOICE;
                }

                if(allDirectData1 > allBestScore)
                {
                    allBestScore = allDirectData1;
                    mDetectedPattern = DMRSyncPattern.DIRECT_DATA_TIMESLOT_1;
                }

                if(allDirectData2 > allBestScore)
                {
                    allBestScore = allDirectData2;
                    mDetectedPattern = DMRSyncPattern.DIRECT_DATA_TIMESLOT_2;
                }

                if(allDirectVoice1 > allBestScore)
                {
                    allBestScore = allDirectVoice1;
                    mDetectedPattern = DMRSyncPattern.DIRECT_VOICE_TIMESLOT_1;
                }

                if(allDirectVoice2 > allBestScore)
                {
                    allBestScore = allDirectVoice2;
                    mDetectedPattern = DMRSyncPattern.DIRECT_VOICE_TIMESLOT_2;
                }

                return allBestScore;
            case BASE_ONLY:
                float baseData = 0;
                float baseVoice = 0;

                for(int x = 0; x < 24; x++)
                {
                    symbol = mSymbols[mSymbolPointer + x];
                    baseData += BASE_DATA[x] * symbol;
                    baseVoice += BASE_VOICE[x] * symbol;
                }

                if(baseData > baseVoice)
                {
                    mDetectedPattern = DMRSyncPattern.BASE_STATION_DATA;
                    return baseData;
                }
                else
                {
                    mDetectedPattern = DMRSyncPattern.BASE_STATION_VOICE;
                    return baseVoice;
                }
            case MOBILE_ONLY:
                float mobileData = 0;
                float mobileVoice = 0;

                for(int x = 0; x < 24; x++)
                {
                    symbol = mSymbols[mSymbolPointer + x];
                    mobileData += MOBILE_DATA[x] * symbol;
                    mobileVoice += MOBILE_VOICE[x] * symbol;
                }

                float mobileBestScore = mobileVoice;
                mDetectedPattern = DMRSyncPattern.MOBILE_STATION_VOICE;

                if(mobileData > mobileBestScore)
                {
                    mDetectedPattern = DMRSyncPattern.MOBILE_STATION_DATA;
                    mobileBestScore = mobileData;
                }

                return mobileBestScore;
            case DIRECT_ONLY:
                float directData1 = 0;
                float directData2 = 0;
                float directVoice1 = 0;
                float directVoice2 = 0;

                for(int x = 0; x < 24; x++)
                {
                    symbol = mSymbols[mSymbolPointer + x];
                    directData1 += DIRECT_DATA_1[x] * symbol;
                    directData2 += DIRECT_DATA_2[x] * symbol;
                    directVoice1 += DIRECT_VOICE_1[x] * symbol;
                    directVoice2 += DIRECT_VOICE_2[x] * symbol;
                }

                float directBestScore = directData1;
                mDetectedPattern = DMRSyncPattern.DIRECT_DATA_TIMESLOT_1;

                if(directData2 > directBestScore)
                {
                    directBestScore = directData2;
                    mDetectedPattern = DMRSyncPattern.DIRECT_DATA_TIMESLOT_2;
                }

                if(directVoice1 > directBestScore)
                {
                    directBestScore = directVoice1;
                    mDetectedPattern = DMRSyncPattern.DIRECT_VOICE_TIMESLOT_1;
                }

                if(directVoice2 > directBestScore)
                {
                    directBestScore = directVoice2;
                    mDetectedPattern = DMRSyncPattern.DIRECT_VOICE_TIMESLOT_2;
                }

                return directBestScore;
            default:
                throw new IllegalStateException("Unrecognized DMR Sync Detection Mode: " + mMode);
        }
    }
}
