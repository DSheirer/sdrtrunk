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
package io.github.dsheirer.module.log;

import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.module.Module;
import io.github.dsheirer.module.log.config.EventLogConfiguration;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.config.SourceConfigTuner;
import io.github.dsheirer.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class EventLogManager
{
    private final static Logger mLog = LoggerFactory.getLogger(EventLogManager.class);

    private UserPreferences mUserPreferences;
    private AliasModel mAliasModel;

    public EventLogManager(AliasModel aliasModel, UserPreferences userPreferences)
    {
        mAliasModel = aliasModel;
        mUserPreferences = userPreferences;
    }

    public List<Module> getLoggers(Channel channel)
    {
        EventLogConfiguration config = channel.getEventLogConfiguration();
        String prefix = StringUtils.replaceIllegalCharacters(channel.getName());
        long frequency = 0;

        if(channel.getSourceConfiguration() instanceof SourceConfigTuner)
        {
            frequency = ((SourceConfigTuner)channel.getSourceConfiguration()).getFrequency();
        }

        List<Module> loggers = new ArrayList<Module>();

        for(EventLogType type : config.getLoggers())
        {
            switch(type)
            {
                case CALL_EVENT:
                case DECODED_MESSAGE:
                    if(channel.getChannelType() == Channel.ChannelType.STANDARD)
                    {
                        loggers.add(getLogger(type, prefix, frequency));
                    }
                    break;
                case TRAFFIC_CALL_EVENT:
                case TRAFFIC_DECODED_MESSAGE:
                    if(channel.getChannelType() == Channel.ChannelType.TRAFFIC)
                    {
                        loggers.add(getLogger(type, prefix, frequency));
                    }
                    break;
            }
        }

        return loggers;
    }

    public EventLogger getLogger(EventLogType eventLogType, String prefix, long frequency)
    {
        StringBuilder sb = new StringBuilder();

        sb.append(prefix);
        sb.append(eventLogType.getFileSuffix());
        sb.append(".log");

        Path eventLogDirectory = mUserPreferences.getDirectoryPreference().getDirectoryEventLog();

        switch(eventLogType)
        {
            case CALL_EVENT:
                return new DecodeEventLogger(mAliasModel, eventLogDirectory, sb.toString(), frequency);
            case DECODED_MESSAGE:
                return new MessageEventLogger(eventLogDirectory, sb.toString(), MessageEventLogger.Type.DECODED, frequency);
            case TRAFFIC_CALL_EVENT:
                return new DecodeEventLogger(mAliasModel, eventLogDirectory, sb.toString(), frequency);
            case TRAFFIC_DECODED_MESSAGE:
                return new MessageEventLogger(eventLogDirectory, sb.toString(), MessageEventLogger.Type.DECODED, frequency);
            default:
                return null;
        }
    }
}
