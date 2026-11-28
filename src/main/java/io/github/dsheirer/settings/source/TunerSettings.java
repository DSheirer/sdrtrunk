/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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

package io.github.dsheirer.settings.source;

import io.github.dsheirer.settings.Setting;
import io.github.dsheirer.gui.settings.tuner.RspDuoSelectionMode;
import io.github.dsheirer.settings.AbstractSettings;
import io.github.dsheirer.settings.SettingsType;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tuner settings
 */
public class TunerSettings extends AbstractSettings
{
    public static final ChannelizerType DEFAULT_CHANNELIZER_TYPE = ChannelizerType.POLYPHASE;
    public static final RspDuoSelectionMode DEFAULT_RSP_DUO_TUNER_MODE = RspDuoSelectionMode.DUAL;
    private final static Logger mLog = LoggerFactory.getLogger(TunerSettings.class);

    private ChannelizerType mChannelizerType;
    private RspDuoSelectionMode mRspDuoSelectionMode;

    /**
     * Constructs a tuner setting with the update listener
     *
     * @param updateListener
     */
    public TunerSettings(Listener<SettingsType> updateListener)
    {
        super(updateListener);
    }

    @Override
    public SettingsType getSettingsType()
    {
        return SettingsType.TUNER;
    }


    /**
     * Channelizer type used by the tuners.  A stored value that isn't a channelizer type (for example one saved by a
     * version with other types) reads as the default.
     */
    public ChannelizerType getChannelizerType()
    {
        if(mChannelizerType == null)
        {
            String type = Setting.TUNER_CHANNELIZER_TYPE.get(DEFAULT_CHANNELIZER_TYPE.name());
            mChannelizerType = DEFAULT_CHANNELIZER_TYPE;

            for(ChannelizerType channelizerType : ChannelizerType.values())
            {
                if(channelizerType.name().equalsIgnoreCase(type))
                {
                    mChannelizerType = channelizerType;
                }
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
        Setting.TUNER_CHANNELIZER_TYPE.put(mChannelizerType.name());
        notifySettingsUpdated();
    }

    /**
     * RSPduo tuner select mode.
     * @return mode or a default value of DUAL
     */
    public RspDuoSelectionMode getRspDuoTunerMode()
    {
        if(mRspDuoSelectionMode == null)
        {
            String mode = Setting.TUNER_RSP_DUO_TUNER_MODE.get(DEFAULT_RSP_DUO_TUNER_MODE.name());
            mRspDuoSelectionMode = RspDuoSelectionMode.fromValue(mode);
        }

        return mRspDuoSelectionMode;
    }

    /**
     * Sets the RSPduo tuner select mode
     * @param mode to use
     */
    public void setRspDuoTunerMode(RspDuoSelectionMode mode)
    {
        mRspDuoSelectionMode = mode;
        Setting.TUNER_RSP_DUO_TUNER_MODE.put(mRspDuoSelectionMode.name());
        notifySettingsUpdated();
    }
}
