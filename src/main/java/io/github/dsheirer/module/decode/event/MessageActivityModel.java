/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

import io.github.dsheirer.filter.AllPassFilter;
import io.github.dsheirer.filter.FilterSet;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.StuffBitsMessage;
import io.github.dsheirer.sample.Listener;
import java.awt.EventQueue;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

public class MessageActivityModel extends AbstractTableModel implements Listener<IMessage>
{
    private static final long serialVersionUID = 1L;
    private static final int TIME = 0;
    private static final int PROTOCOL = 1;
    private static final int TIMESLOT = 2;
    private static final int MESSAGE = 3;

    protected int mMaxMessages = 200;
    protected LinkedList<MessageItem> mMessageItems = new LinkedList<>();
    protected int[] mColumnWidths = {20, 20, 500};
    protected String[] mHeaders = new String[]{"Time", "Protocol", "Timeslot", "Message"};

    private SimpleDateFormat mSDFTime = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");

    private FilterSet<IMessage> mMessageFilterSet = new FilterSet<>(new AllPassFilter<>());

    public MessageActivityModel()
    {
    }

    /**
     * Applies the filter set
     */
    public void setFilters(FilterSet filterSet)
    {
        mMessageFilterSet = filterSet;
    }

    public void clearFilters()
    {
        mMessageFilterSet = new FilterSet<>(new AllPassFilter<>());
    }

    /**
     * Clears all messages from history
     */
    public void clear()
    {
        EventQueue.invokeLater(() -> {
            mMessageItems.clear();
            fireTableDataChanged();
        });
    }

    /**
     * Clears the current messages and loads the messages argument
     */
    public void clearAndSet(List<IMessage> messages)
    {
        EventQueue.invokeLater(() -> {
            mMessageItems.clear();
            fireTableDataChanged();
            for(IMessage message: messages)
            {
                receive(message);
            }
        });
    }

    public FilterSet<IMessage> getMessageFilterSet()
    {
        return mMessageFilterSet;
    }

    public void dispose()
    {
        mMessageItems.clear();
    }

    public int[] getColumnWidths()
    {
        return mColumnWidths;
    }

    public void setColumnWidths(int[] widths)
    {
        if(widths.length != 3)
        {
            throw new IllegalArgumentException("MessageActivityModel - column widths array should have 3 elements");
        }
        else
        {
            mColumnWidths = widths;
        }
    }

    public int getMaxMessageCount()
    {
        return mMaxMessages;
    }

    public void setMaxMessageCount(int count)
    {
        mMaxMessages = count;
    }

    public void receive(final IMessage message)
    {
        //Don't process tail bits or stuff bits message fragments
        if(message instanceof StuffBitsMessage)
        {
            return;
        }

        if(mMessageFilterSet.passes(message))
        {
            final MessageItem messageItem = new MessageItem(message);

            EventQueue.invokeLater(new Runnable()
            {
                @Override
                public void run()
                {
                    mMessageItems.addFirst(messageItem);

                    MessageActivityModel.this.fireTableRowsInserted(0, 0);

                    prune();
                }
            });
        }
    }

    private void prune()
    {
        while(mMessageItems.size() > mMaxMessages)
        {
            MessageItem removed = mMessageItems.removeLast();
            removed.dispose();
            super.fireTableRowsDeleted(mMessageItems.size() - 1, mMessageItems.size() - 1);
        }
    }

    @Override
    public int getRowCount()
    {
        return mMessageItems.size();
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
        if(0 <= rowIndex && rowIndex < mMessageItems.size())
        {
            MessageItem messageItem = mMessageItems.get(rowIndex);

            switch(columnIndex)
            {
                case TIME:
                    return messageItem.getTimestamp(mSDFTime);
                case PROTOCOL:
                    return messageItem.getProtocol();
                case TIMESLOT:
                    return messageItem.getTimeslot();
                case MESSAGE:
                    return messageItem.getText();
                default:
                    break;
            }
        }

        return null;
    }
}
