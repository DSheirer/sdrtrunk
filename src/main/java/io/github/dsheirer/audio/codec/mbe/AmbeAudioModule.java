/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */

package io.github.dsheirer.audio.codec.mbe;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.preference.UserPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AmbeAudioModule extends JmbeAudioModule
{
    private static final Logger mLog = LoggerFactory.getLogger(AmbeAudioModule.class);
    private static final String AMBE_CODEC = "AMBE 3600 x 2450";
    private static boolean sLibraryStatusLogged = false;

    public AmbeAudioModule(UserPreferences userPreferences, AliasList aliasList, int timeslot)
    {
        super(userPreferences, aliasList, timeslot);

        if(!sLibraryStatusLogged)
        {
            if(getAudioCodec() != null)
            {
                mLog.info("AMBE CODEC successfully loaded - P25-2/DMR/NXDN audio will be available");
            }
            else
            {
                mLog.warn("AMBE CODEC not loaded - P25-2/DMR/NXDN audio will NOT be available");
            }

            sLibraryStatusLogged = true;
        }
    }

    @Override
    protected String getCodecName()
    {
        return AMBE_CODEC;
    }
}
