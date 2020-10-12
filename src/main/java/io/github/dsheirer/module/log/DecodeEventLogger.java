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
package io.github.dsheirer.module.log;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.configuration.AliasListConfigurationIdentifier;
import io.github.dsheirer.identifier.configuration.FrequencyConfigurationIdentifier;
import io.github.dsheirer.module.decode.event.IDecodeEvent;
import io.github.dsheirer.module.decode.event.IDecodeEventListener;
import io.github.dsheirer.preference.TimestampFormat;
import io.github.dsheirer.sample.Listener;

import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class DecodeEventLogger extends EventLogger implements IDecodeEventListener, Listener<IDecodeEvent>
{
    private SimpleDateFormat mTimestampFormat = TimestampFormat.TIMESTAMP_COLONS.getFormatter();
    private DecimalFormat mFrequencyFormat = new DecimalFormat("0.000000");
    private AliasList mAliasList;
    private AliasModel mAliasModel;

    public DecodeEventLogger(AliasModel aliasModel, Path logDirectory, String fileNameSuffix, long frequency)
    {
        super(logDirectory, fileNameSuffix, frequency);
        mAliasModel = aliasModel;
    }

    @Override
    public void receive(IDecodeEvent decodeEvent)
    {
                write(toCSV(decodeEvent));
    }

    @Override
    public String getHeader()
    {
        return getCSVHeader();
    }

    @Override
    public Listener<IDecodeEvent> getDecodeEventListener()
    {
        return this;
    }

    @Override
    public void reset()
    {
    }

    public static String getCSVHeader()
    {
        return "TIMESTAMP,DURATION_MS,PROTOCOL,EVENT,FROM,TO,CHANNEL_NUMBER,FREQUENCY,TIMESLOT,DETAILS";
    }

    private String toCSV(IDecodeEvent event)
    {

        StringBuilder sb = new StringBuilder();

        sb.append("\"").append(mTimestampFormat.format(new Date(event.getTimeStart()))).append("\"");
        sb.append(",\"").append(event.getDuration() > 0 ? event.getDuration() : "").append("\"");
        sb.append(",\"").append(event.getProtocol()).append("\"");

        String description = event.getEventDescription();

        sb.append(",\"").append(description != null ? description : "").append("\"");

        List<Identifier> fromIdentifiers = event.getIdentifierCollection().getIdentifiers(Role.FROM);
        if(fromIdentifiers != null && !fromIdentifiers.isEmpty())
        {
            sb.append(",\"").append(fromIdentifiers.get(0)).append("\"");
        }
        else
        {
            sb.append(",\"\"");
        }

        List<Identifier> toIdentifiers = event.getIdentifierCollection().getIdentifiers(Role.TO);

        if(toIdentifiers != null && !toIdentifiers.isEmpty())
        {
            Identifier identifier = event.getIdentifierCollection()
                .getIdentifier(IdentifierClass.CONFIGURATION,Form.ALIAS_LIST,Role.ANY);
            mAliasList = mAliasModel.getAliasList((AliasListConfigurationIdentifier)identifier);

            if(mAliasList != null)
            {
                String mystring = (!mAliasList.getAliases(toIdentifiers.get(0)).isEmpty()) ?
                    mAliasList.getAliases(toIdentifiers.get(0)).get(0).toString() : "";
                sb.append(",\"").append(mystring).append(" (").append(toIdentifiers.get(0)).append(")\"");
            }
            else
            {
                sb.append(",\"\"");
            }
        }
        else
        {
            sb.append(",\"\"");
        }

        IChannelDescriptor descriptor = event.getChannelDescriptor();

        sb.append(",\"").append(descriptor != null ? descriptor : "").append("\"");

        Identifier frequency = event.getIdentifierCollection()
            .getIdentifier(IdentifierClass.CONFIGURATION, Form.CHANNEL_FREQUENCY, Role.ANY);

        if(frequency instanceof FrequencyConfigurationIdentifier)
        {
            sb.append(",\"").append(mFrequencyFormat
                .format(((FrequencyConfigurationIdentifier)frequency).getValue() / 1e6d)).append("\"");
        }
        else
        {
            sb.append(",\"\"");
        }

        if(event.hasTimeslot())
        {
            sb.append(",\"TS:").append(event.getTimeslot());
        }
        else
        {
            sb.append(",\"\"");
        }

        String details = event.getDetails();

        sb.append(",\"").append(details != null ? details : "").append("\"");

        return sb.toString();
    }
}
