/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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
import io.github.dsheirer.module.Module;
import io.github.dsheirer.module.decode.event.IDecodeEvent;
import io.github.dsheirer.module.decode.event.IDecodeEventListener;
import io.github.dsheirer.preference.TimestampFormat;
import io.github.dsheirer.sample.Listener;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.QuoteMode;

/**
 * Module that receives decode events for a system (shared across channels) and writes them to a
 * RollingSystemEventLogger in the same CSV format as DecodeEventLogger.
 */
public class SystemEventLogModule extends Module implements IDecodeEventListener, Listener<IDecodeEvent>
{
    private final RollingSystemEventLogger mSystemLogger;
    private final AliasModel mAliasModel;
    private final SimpleDateFormat mTimestampFormat = TimestampFormat.TIMESTAMP_COLONS.getFormatter();
    private final DecimalFormat mFrequencyFormat = new DecimalFormat("0.000000");
    private final CSVFormat mCsvFormat = CSVFormat.Builder.create(CSVFormat.DEFAULT)
            .setQuoteMode(QuoteMode.ALL)
            .build();

    /**
     * Constructs an instance.
     * @param logger shared rolling logger for this system
     * @param aliasModel for alias lookups
     */
    public SystemEventLogModule(RollingSystemEventLogger logger, AliasModel aliasModel)
    {
        mSystemLogger = logger;
        mAliasModel = aliasModel;
    }

    @Override
    public void start()
    {
        mSystemLogger.addUser();
    }

    @Override
    public void stop()
    {
        mSystemLogger.removeUser();
    }

    @Override
    public void reset()
    {
    }

    @Override
    public Listener<IDecodeEvent> getDecodeEventListener()
    {
        return this;
    }

    @Override
    public void receive(IDecodeEvent event)
    {
        if(event != null)
        {
            mSystemLogger.write(toCSV(event));
        }
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
            Identifier aliasListId = event.getIdentifierCollection()
                    .getIdentifier(IdentifierClass.CONFIGURATION, Form.ALIAS_LIST, Role.ANY);
            if(aliasListId instanceof AliasListConfigurationIdentifier aliasListConfigId)
            {
                AliasList aliasList = mAliasModel.getAliasList(aliasListConfigId);
                if(aliasList != null && !aliasList.getAliases(toIdentifier).isEmpty())
                {
                    cells.add(aliasList.getAliases(toIdentifier).toString() + " (" + toIdentifier + ")");
                }
                else
                {
                    cells.add(toIdentifier);
                }
            }
            else
            {
                cells.add(toIdentifier);
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
            if(frequency instanceof FrequencyConfigurationIdentifier freqId)
            {
                cells.add(mFrequencyFormat.format(freqId.getValue() / 1e6d));
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
