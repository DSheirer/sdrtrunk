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
import org.apache.commons.text.StringEscapeUtils;

import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

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
        List<Object> cells = new ArrayList<>();

        cells.add(mTimestampFormat.format(new Date(event.getTimeStart())));
        cells.add(event.getDuration() > 0 ? event.getDuration() : null);
        cells.add(event.getProtocol());
        cells.add(event.getEventDescription());

        List<Identifier> fromIdentifiers = event.getIdentifierCollection().getIdentifiers(Role.FROM);
        if(fromIdentifiers != null && !fromIdentifiers.isEmpty())
        {
            cells.add(fromIdentifiers.get(0));
        }
        else
        {
            cells.add(null);
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
                cells.add(mystring + " (" + toIdentifiers.get(0) + ")");
            }
            else
            {
                cells.add(null);
            }
        }
        else
        {
            cells.add(null);
        }

        cells.add(event.getChannelDescriptor());

        Identifier frequency = event.getIdentifierCollection()
            .getIdentifier(IdentifierClass.CONFIGURATION, Form.CHANNEL_FREQUENCY, Role.ANY);

        if(frequency instanceof FrequencyConfigurationIdentifier)
        {
            cells.add(mFrequencyFormat
                    .format(((FrequencyConfigurationIdentifier)frequency).getValue() / 1e6d));

        }
        else
        {
            cells.add(null);
        }

        if(event.hasTimeslot())
        {
            cells.add("TS:" + event.getTimeslot());
        }
        else
        {
            cells.add(null);
        }

        String details = event.getDetails();
        cells.add(details != null ? details : "");

        return cells.stream()
                .map(cell -> StringEscapeUtils.escapeCsv(String.valueOf(cell == null ? "" : cell)))
                .collect(Collectors.joining(","));
    }
}
