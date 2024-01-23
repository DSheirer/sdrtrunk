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
package io.github.dsheirer.audio.broadcast;

import io.github.dsheirer.icon.Icon;
import io.github.dsheirer.icon.IconModel;
import io.github.dsheirer.preference.swing.JTableColumnWidthMonitor;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import net.miginfocom.swing.MigLayout;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Table of broadcast streams and statuses.
 */
public class BroadcastStatusPanel extends JPanel
{
    private JTable mTable;
    private JTableColumnWidthMonitor mColumnWidthMonitor;
    private JScrollPane mScrollPane;
    @Resource
    private BroadcastModel mBroadcastModel;
    private String mPreferenceKey = "application.broadcast.status.panel";

    /**
     * Constructs an instance
     */
    public BroadcastStatusPanel()
    {
    }

    public JTable getTable()
    {
        return mTable;
    }

    @PostConstruct
    private void postConstruct()
    {
        setPreferredSize(new Dimension(880, 70));
        setLayout(new MigLayout("insets 0 0 0 0 ", "[grow,fill]", "[grow,fill]"));
        mTable = new JTable(mBroadcastModel);
        mTable.setEnabled(false);
        DefaultTableCellRenderer renderer = (DefaultTableCellRenderer)mTable.getDefaultRenderer(String.class);
        renderer.setHorizontalAlignment(SwingConstants.CENTER);
        mTable.getColumnModel().getColumn(BroadcastModel.COLUMN_BROADCASTER_STATUS).setCellRenderer(new StatusCellRenderer());
        mTable.getColumnModel().getColumn(BroadcastModel.COLUMN_BROADCAST_SERVER_TYPE).setCellRenderer(new ServerTypeRenderer());
        mColumnWidthMonitor = new JTableColumnWidthMonitor(mTable, mPreferenceKey);
        mScrollPane = new JScrollPane(mTable);
        add(mScrollPane);
    }

    public class ServerTypeRenderer extends DefaultTableCellRenderer
    {
        public ServerTypeRenderer()
        {
            setOpaque(true);
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
        {
            JLabel component = (JLabel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if(value instanceof BroadcastServerType broadcastServerType)
            {
                component.setText(broadcastServerType.toString());
                Icon icon = new Icon("empty", broadcastServerType.getIconPath());
                ImageIcon imageIcon = icon.getIcon();
                ImageIcon scaledIcon = IconModel.getScaledIcon(imageIcon, 13);
                component.setIcon(scaledIcon);
            }
            else
            {
                component.setText(null);
                component.setIcon(null);
            }

            return component;
        }
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
                    else if(state == BroadcastState.INVALID_SETTINGS ||
                            state == BroadcastState.NETWORK_UNAVAILABLE)
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
