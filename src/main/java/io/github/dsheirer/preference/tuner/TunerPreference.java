/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */

package io.github.dsheirer.preference.tuner;

import io.github.dsheirer.preference.Preference;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.prefs.Preferences;

/**
 * Tuner preferences
 */
public class TunerPreference extends Preference
{
    private final static Logger mLog = LoggerFactory.getLogger(TunerPreference.class);
    private Preferences mPreferences = Preferences.userNodeForPackage(TunerPreference.class);
    private static final String PREFERENCE_KEY_CHANNELIZER_TYPE = "channelizer.type";

    private ChannelizerType mChannelizerType;

    /**
     * Constructs a tuner preference with the update listener
     *
     * @param updateListener
     */
    public TunerPreference(Listener<PreferenceType> updateListener)
    {
        super(updateListener);
    }

    @Override
    public PreferenceType getPreferenceType()
    {
        return PreferenceType.TUNER;
    }


    /**
     * Channelizer type used by the tuners
     */
    public ChannelizerType getChannelizerType()
    {
        if(mChannelizerType == null)
        {
            String type = mPreferences.get(PREFERENCE_KEY_CHANNELIZER_TYPE, ChannelizerType.POLYPHASE.name());

            if(type != null)
            {
                if(type.equalsIgnoreCase(ChannelizerType.POLYPHASE.name()))
                {
                    mChannelizerType = ChannelizerType.POLYPHASE;
                }
                else if(type.equalsIgnoreCase(ChannelizerType.HETERODYNE.name()))
                {
                    mChannelizerType = ChannelizerType.HETERODYNE;
                }
            }

            if(type == null)
            {
                mChannelizerType = ChannelizerType.POLYPHASE;
            }
        }

        return mChannelizerType;
    }

    /**
     * Sets the channelizer type use by tuners
     */
    public void setChannelizerType(ChannelizerType type)
    {
        mChannelizerType = type;
        mPreferences.put(PREFERENCE_KEY_CHANNELIZER_TYPE, mChannelizerType.name());
        notifyPreferenceUpdated();
    }
}
