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
package io.github.dsheirer.module.decode.event;

import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.sample.Listener;
import java.awt.EventQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decode event table model
 */
public class DecodeEventModel extends ClearableHistoryModel<IDecodeEvent> implements Listener<IDecodeEvent>
{
    private static final long serialVersionUID = 1L;
    private final static Logger mLog = LoggerFactory.getLogger(DecodeEventModel.class);
    public static final int COLUMN_TIME = 0;
    public static final int COLUMN_DURATION = 1;
    public static final int COLUMN_EVENT = 2;
    public static final int COLUMN_FROM_ID = 3;
    public static final int COLUMN_FROM_ALIAS = 4;
    public static final int COLUMN_TO_ID = 5;
    public static final int COLUMN_TO_ALIAS = 6;
    public static final int COLUMN_CHANNEL = 7;
    public static final int COLUMN_FREQUENCY = 8;
    public static final int COLUMN_DETAILS = 9;
    protected String[] mHeaders = new String[]{"Time", "Duration", "Event", "From", "Alias", "To", "Alias", "Channel", "Frequency", "Details"};

    public DecodeEventModel()
    {
        MyEventBus.getGlobalEventBus().register(this);
    }

    /**
     * Receives preference update notifications via the event bus
     * @param preferenceType that was updated
     */
    @Subscribe
    public void preferenceUpdated(PreferenceType preferenceType)
    {
        if(preferenceType == PreferenceType.DECODE_EVENT || preferenceType == PreferenceType.TALKGROUP_FORMAT)
        {
            fireTableDataChanged();
        }
    }

    /**
     * Adds, updates or deletes the event from the model.  Producers can send
     * the same call event multiple times to indicate that information in the
     * event is updated.  Producers can also mark the event as invalid and the
     * event will be removed from the model.
     */
    public void receive(final IDecodeEvent event)
    {
        EventQueue.invokeLater(() -> add(event));
    }

    @Override
    public int getColumnCount()
    {
        return mHeaders.length;
    }

    public String getColumnName(int column)
    {
        return mHeaders[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
        IDecodeEvent event = getItem(rowIndex);

        if(event != null)
        {
            switch(columnIndex)
            {
                case COLUMN_TIME:
                    return event.getTimeStart();
                case COLUMN_DURATION:
                    return event.getDuration();
                case COLUMN_EVENT:
                    return event.getEventType().getLabel();
                case COLUMN_FROM_ID:
                    return event.getIdentifierCollection();
                case COLUMN_FROM_ALIAS:
                    return event.getIdentifierCollection();
                case COLUMN_TO_ID:
                    return event.getIdentifierCollection();
                case COLUMN_TO_ALIAS:
                    return event.getIdentifierCollection();
                case COLUMN_CHANNEL:
                    IChannelDescriptor channelDescriptor = event.getChannelDescriptor();

                    if(channelDescriptor != null)
                    {
                        if(event.hasTimeslot() && !event.toString().contains("TS"))
                        {
                            return channelDescriptor + " TS" + event.getTimeslot();
                        }
                        else
                        {
                            return channelDescriptor.toString();
                        }
                    }
                    else
                    {
                        if(event.hasTimeslot() && !event.toString().contains("TS"))
                        {
                            return "TS" + event.getTimeslot();
                        }
                        else
                        {
                            return null;
                        }
                    }
                case COLUMN_FREQUENCY:
                    return event.getChannelDescriptor();
                case COLUMN_DETAILS:
                    return event.getDetails();
            }
        }

        return null;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex)
    {
        switch(columnIndex)
        {
            case COLUMN_DURATION:
            case COLUMN_TIME:
                return Long.class;
            case COLUMN_DETAILS:
            case COLUMN_EVENT:
                return String.class;
            case COLUMN_FREQUENCY:
            case COLUMN_FROM_ALIAS:
            case COLUMN_FROM_ID:
            case COLUMN_TO_ALIAS:
            case COLUMN_TO_ID:
                return IdentifierCollection.class;
            case COLUMN_CHANNEL:
                return String.class;
        }

        return super.getColumnClass(columnIndex);
    }
}
