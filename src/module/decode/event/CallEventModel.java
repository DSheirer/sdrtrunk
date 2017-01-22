/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014,2015 Dennis Sheirer
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package module.decode.event;

import alias.Alias;
import module.decode.event.CallEvent.CallEventType;
import sample.Listener;

import javax.swing.table.AbstractTableModel;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class CallEventModel extends AbstractTableModel implements Listener<CallEvent>
{
    private static final long serialVersionUID = 1L;

    private static DecimalFormat mFrequencyFormatter = new DecimalFormat("0.000000");

    public static final int TIME = 0;
    public static final int EVENT = 1;
    public static final int FROM_ID = 2;
    public static final int FROM_ALIAS = 3;
    public static final int TO_ID = 4;
    public static final int TO_ALIAS = 5;
    public static final int CHANNEL = 6;
    public static final int FREQUENCY = 7;
    public static final int DETAILS = 8;

    protected int mMaxMessages = 500;

    protected List<CallEvent> mEvents = new ArrayList<CallEvent>();

    protected String[] mHeaders = new String[]{"Time",
        "Event",
        "From",
        "Alias",
        "To",
        "Alias",
        "Channel",
        "Frequency",
        "Details"};

    private SimpleDateFormat mSDFTime = new SimpleDateFormat("HH:mm:ss");

    public CallEventModel()
    {
    }

    public void dispose()
    {
        mEvents.clear();
    }

    public void reset()
    {
        mEvents.clear();

        fireTableDataChanged();
    }

    public int getMaxMessageCount()
    {
        return mMaxMessages;
    }

    public void setMaxMessageCount(int count)
    {
        mMaxMessages = count;
    }

    /**
     * Adds, updates or deletes the event from the model.  Producers can send
     * the same call event multiple times to indicate that information in the
     * event is updated.  Producers can also mark the event as invalid and the
     * event will be removed from the model.
     */
    public void receive(final CallEvent event)
    {
        if(event.isValid())
        {
            if(!mEvents.contains(event))
            {
                mEvents.add(0, event);

                fireTableRowsInserted(0, 0);

                prune();
            }
            else
            {
                int row = mEvents.indexOf(event);

                fireTableRowsUpdated(row, row);
            }
        }
        else
        {
            if(mEvents.contains(event))
            {
                int row = mEvents.indexOf(event);

                mEvents.remove(event);

                fireTableRowsDeleted(row, row);
            }
        }
    }

    private void prune()
    {
        while(mEvents.size() > mMaxMessages)
        {
            int index = mEvents.size() - 1;
            mEvents.remove(index);

            fireTableRowsDeleted(index, index);
        }
    }

    @Override
    public int getRowCount()
    {
        return mEvents.size();
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
        synchronized(mEvents)
        {
            switch(columnIndex)
            {
                case TIME:
                    StringBuilder sb = new StringBuilder();

                    sb.append(mSDFTime.format(
                        mEvents.get(rowIndex).getEventStartTime()));

                    if(mEvents.get(rowIndex).getEventEndTime() != 0)
                    {
                        sb.append(" - ");
                        sb.append(mSDFTime.format(
                            mEvents.get(rowIndex).getEventEndTime()));
                    }
                    else if(mEvents.get(rowIndex)
                        .getCallEventType() == CallEventType.CALL)
                    {
                        sb.append(" - In Progress");
                    }

                    return sb.toString();
                case EVENT:
                    return mEvents.get(rowIndex).getCallEventType();
                case FROM_ID:
                    return mEvents.get(rowIndex).getFromID();
                case FROM_ALIAS:
                    return mEvents.get(rowIndex).getFromIDAlias();
                case TO_ID:
                    return mEvents.get(rowIndex).getToID();
                case TO_ALIAS:
                    return mEvents.get(rowIndex).getToIDAlias();
                case CHANNEL:
                    return mEvents.get(rowIndex).getChannel();
                case FREQUENCY:
                    long frequency = mEvents.get(rowIndex).getFrequency();

                    if(frequency != 0)
                    {
                        return mFrequencyFormatter.format((double) frequency / 1E6d);
                    }
                    else
                    {
                        return null;
                    }
                case DETAILS:
                    return mEvents.get(rowIndex).getDetails();
            }
        }

        return null;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex)
    {
        if(columnIndex == FROM_ALIAS || columnIndex == TO_ALIAS)
        {
            return Alias.class;
        }

        return super.getColumnClass(columnIndex);
    }
}
