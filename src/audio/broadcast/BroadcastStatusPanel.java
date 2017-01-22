/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
 *
 ******************************************************************************/
package audio.broadcast;

import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class BroadcastStatusPanel extends JPanel
{
    private JTable mTable;
    private JScrollPane mScrollPane;
    private BroadcastModel mBroadcastModel;

    public BroadcastStatusPanel(BroadcastModel broadcastModel)
    {
        mBroadcastModel = broadcastModel;

        init();
    }

    public JTable getTable()
    {
        return mTable;
    }

    private void init()
    {
        setLayout(new MigLayout("insets 0 0 0 0 ", "[grow,fill]", "[grow,fill]"));

        mTable = new JTable(mBroadcastModel);

        DefaultTableCellRenderer renderer = (DefaultTableCellRenderer)mTable.getDefaultRenderer(String.class);
        renderer.setHorizontalAlignment(SwingConstants.CENTER);

        mTable.getColumnModel().getColumn(BroadcastModel.COLUMN_BROADCASTER_STATUS).setCellRenderer(new StatusCellRenderer());

        mScrollPane = new JScrollPane(mTable);

        add(mScrollPane);
    }

    /**
     * Custom cell renderer for the broadcast state column.
     */
    public class StatusCellRenderer extends DefaultTableCellRenderer
    {
        public StatusCellRenderer()
        {
            setOpaque(true);
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column)
        {
            JLabel component = (JLabel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if(isSelected)
            {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            }
            else
            {
                if(value instanceof BroadcastState)
                {
                    BroadcastState state = (BroadcastState)value;

                    if(state == BroadcastState.CONNECTED)
                    {
                        setBackground(Color.GREEN);
                        setForeground(table.getForeground());
                    }
                    else if(state == BroadcastState.DISABLED)
                    {
                        setBackground(table.getBackground());
                        setForeground(Color.LIGHT_GRAY);
                    }
                    else if(state == BroadcastState.INVALID_SETTINGS)
                    {
                        setBackground(Color.YELLOW);
                        setForeground(table.getForeground());
                    }
                    else if(state.isErrorState())
                    {
                        setBackground(Color.RED);
                        setForeground(table.getForeground());
                    }
                    else
                    {
                        setBackground(table.getBackground());
                        setForeground(table.getForeground());
                    }
                }
                else
                {
                    setForeground(table.getForeground());
                    setBackground(table.getBackground());
                }
            }

            return this;
        }
    }
}
