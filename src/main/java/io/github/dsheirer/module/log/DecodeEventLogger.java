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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.QuoteMode;

public class DecodeEventLogger extends EventLogger implements IDecodeEventListener, Listener<IDecodeEvent>
{
    private SimpleDateFormat mTimestampFormat = TimestampFormat.TIMESTAMP_COLONS.getFormatter();
    private DecimalFormat mFrequencyFormat = new DecimalFormat("0.000000");
    private AliasList mAliasList;
    private AliasModel mAliasModel;

    /**
     * The CSV format that SDR Trunk will use to write decode event logs
     * <p>
     * It uses the standard, default CSV format (RFC 4180 and permitting empty lines) but *always* quoting cells since
     * that's what SDR Trunk has done previously when hand-crafting CSV rows.
     */
    private final CSVFormat mCsvFormat = CSVFormat.Builder.create(CSVFormat.DEFAULT)
            .setQuoteMode(QuoteMode.ALL)
            .build();

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
        return "TIMESTAMP,DURATION_MS,PROTOCOL,EVENT,FROM,TO,CHANNEL_NUMBER,FREQUENCY,TIMESLOT,DETAILS,EVENT_ID";
    }

    private String toCSV(IDecodeEvent event)
    {
        List<Object> cells = new ArrayList<>();

        cells.add(mTimestampFormat.format(new Date(event.getTimeStart())));
        cells.add(event.getDuration() > 0 ? event.getDuration() : "");
        cells.add(event.getProtocol());
        cells.add(event.getEventType());

        Identifier fromIdentifier = event.getIdentifierCollection().getFromIdentifier();
        if(fromIdentifier != null)
        {
            cells.add(fromIdentifier);
        }
        else
        {
            cells.add("");
        }

        Identifier toIdentifier = event.getIdentifierCollection().getToIdentifier();

        if(toIdentifier != null)
        {
            Identifier identifier = event.getIdentifierCollection()
                .getIdentifier(IdentifierClass.CONFIGURATION,Form.ALIAS_LIST,Role.ANY);
            mAliasList = mAliasModel.getAliasList((AliasListConfigurationIdentifier)identifier);

            if(mAliasList != null)
            {
                String mystring = (!mAliasList.getAliases(toIdentifier).isEmpty()) ?
                    mAliasList.getAliases(toIdentifier).toString() : "";
                cells.add(mystring + " (" + toIdentifier + ")");
            }
            else
            {
                cells.add("");
            }
        }
        else
        {
            cells.add("");
        }

        IChannelDescriptor descriptor = event.getChannelDescriptor();
        cells.add(descriptor != null ? descriptor : "");

        if(descriptor != null)
        {
            cells.add(mFrequencyFormat.format(descriptor.getDownlinkFrequency() / 1e6d));
        }
        else
        {
            Identifier frequency = event.getIdentifierCollection()
                    .getIdentifier(IdentifierClass.CONFIGURATION, Form.CHANNEL_FREQUENCY, Role.ANY);

            if(frequency instanceof FrequencyConfigurationIdentifier)
            {
                cells.add(mFrequencyFormat
                        .format(((FrequencyConfigurationIdentifier)frequency).getValue() / 1e6d));

            }
            else
            {
                cells.add("");
            }
        }

        if(event.hasTimeslot())
        {
            cells.add("TS:" + event.getTimeslot());
        }
        else
        {
            cells.add("");
        }

        String details = event.getDetails();
        cells.add(details != null ? details : "");

        cells.add(event.hashCode());

        return mCsvFormat.format(cells.toArray());
    }
}
