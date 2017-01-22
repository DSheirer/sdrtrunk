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
package channel.metadata;

import alias.Alias;
import channel.state.State;
import controller.channel.Channel;
import controller.channel.ChannelProcessingManager;
import icon.IconManager;
import module.ProcessingChain;
import net.miginfocom.swing.MigLayout;
import properties.SystemProperties;
import sample.Broadcaster;
import sample.Listener;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class ChannelMetadataPanel extends JPanel implements ListSelectionListener
{
    private static final String PROPERTY_PREFIX_BACKGROUND = "channel.metadata.panel.state.color.";
    private static final String PROPERTY_PREFIX_FOREGROUND = "channel.metadata.panel.text.color.";
    private ChannelProcessingManager mChannelProcessingManager;
    private IconManager mIconManager;
    private JTable mTable;
    private Broadcaster<ProcessingChain> mSelectedProcessingChainBroadcaster = new Broadcaster<>();

    /**
     * Table view for currently decoding channel metadata
     */
    public ChannelMetadataPanel(ChannelProcessingManager channelProcessingManager, IconManager iconManager)
    {
        mIconManager = iconManager;
        mChannelProcessingManager = channelProcessingManager;

        init();
    }

    /**
     * Initializes the panel
     */
    private void init()
    {
        setLayout( new MigLayout( "insets 0 0 0 0", "[grow,fill]", "[grow,fill]") );

        mTable = new JTable(mChannelProcessingManager.getChannelMetadataModel());

        DefaultTableCellRenderer renderer = (DefaultTableCellRenderer)mTable.getDefaultRenderer(String.class);
        renderer.setHorizontalAlignment(SwingConstants.CENTER);

        mTable.getSelectionModel().addListSelectionListener(this);

        mTable.getColumnModel().getColumn(ChannelMetadataModel.COLUMN_STATE)
            .setCellRenderer(new ColoredStateCellRenderer());
        mTable.getColumnModel().getColumn(ChannelMetadataModel.COLUMN_PRIMARY_TO)
            .setCellRenderer(new AliasedValueCellRenderer(Attribute.PRIMARY_ADDRESS_TO));
        mTable.getColumnModel().getColumn(ChannelMetadataModel.COLUMN_PRIMARY_FROM)
            .setCellRenderer(new AliasedValueCellRenderer(Attribute.PRIMARY_ADDRESS_FROM));
        mTable.getColumnModel().getColumn(ChannelMetadataModel.COLUMN_SECONDARY_TO)
            .setCellRenderer(new AliasedValueCellRenderer(Attribute.SECONDARY_ADDRESS_TO));
        mTable.getColumnModel().getColumn(ChannelMetadataModel.COLUMN_SECONDARY_FROM)
            .setCellRenderer(new AliasedValueCellRenderer(Attribute.SECONDARY_ADDRESS_FROM));

        JScrollPane scrollPane = new JScrollPane(mTable, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        add(scrollPane);

//        new TableColumnWidthMonitor(mTable, PROPERTY_PREFIX, new int[] {15,15,15,15,25,25,25,25,15,40});
    }

    @Override
    public void valueChanged(ListSelectionEvent e)
    {
        if(!mTable.getSelectionModel().getValueIsAdjusting())
        {
            ProcessingChain processingChain = null;

            int selectedViewRow = mTable.getSelectedRow();

            if(selectedViewRow >= 0)
            {
                int selectedModelRow = mTable.convertRowIndexToModel(selectedViewRow);

                Metadata selectedMetadata = mChannelProcessingManager.getChannelMetadataModel()
                    .getMetadata(selectedModelRow);

                if(selectedMetadata != null)
                {
                    Channel selectedChannel = mChannelProcessingManager.getChannelMetadataModel()
                        .getChannelFromMetadata(selectedMetadata);

                    processingChain = mChannelProcessingManager.getProcessingChain(selectedChannel);
                }
            }

            mSelectedProcessingChainBroadcaster.broadcast(processingChain);
        }
    }

    /**
     * Adds the listener to receive the processing chain associated with the metadata selected in the
     * metadata table.
     */
    public void addProcessingChainSelectionListener(Listener<ProcessingChain> listener)
    {
        mSelectedProcessingChainBroadcaster.addListener(listener);
    }

    public class AliasedValueCellRenderer extends DefaultTableCellRenderer
    {
        private Attribute mAttribute;

        public AliasedValueCellRenderer(Attribute attribute)
        {
            assert(attribute != null);
            mAttribute = attribute;
            setHorizontalAlignment(JLabel.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                       int row, int column)
        {
            JLabel label = (JLabel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            Metadata metadata = (Metadata)value;
            String value1 = metadata.getValue(mAttribute);
            Alias alias = metadata.getAlias(mAttribute);

            label.setText(alias != null ? alias.getName() : value1);
            label.setIcon(alias != null ? mIconManager.getIcon(alias.getIconName(), IconManager.DEFAULT_ICON_SIZE) : null);
            label.setForeground(alias != null ? alias.getDisplayColor() : table.getForeground());

            return label;
        }
    }

    public class ColoredStateCellRenderer extends DefaultTableCellRenderer
    {
        public ColoredStateCellRenderer()
        {
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                       int row, int column)
        {
            JLabel label = (JLabel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            Color background = table.getBackground();
            Color foreground = table.getForeground();

            if(value instanceof State)
            {
                State state = (State)value;
                label.setText(state.getDisplayValue());

                switch(state)
                {
                    case CALL:
                        background = SystemProperties.getInstance().get(PROPERTY_PREFIX_BACKGROUND + "call", Color.BLUE);
                        foreground = SystemProperties.getInstance().get(PROPERTY_PREFIX_FOREGROUND + "call", Color.YELLOW);
                        break;
                    case CONTROL:
                        background = SystemProperties.getInstance().get(PROPERTY_PREFIX_BACKGROUND + "control", Color.ORANGE);
                        foreground = SystemProperties.getInstance().get(PROPERTY_PREFIX_FOREGROUND + "control", Color.BLUE);
                        break;
                    case DATA:
                        background = SystemProperties.getInstance().get(PROPERTY_PREFIX_BACKGROUND + "data", Color.GREEN);
                        foreground = SystemProperties.getInstance().get(PROPERTY_PREFIX_FOREGROUND + "data", Color.BLUE);
                        break;
                    case ENCRYPTED:
                        background = SystemProperties.getInstance().get(PROPERTY_PREFIX_BACKGROUND + "encrypted", Color.MAGENTA);
                        foreground = SystemProperties.getInstance().get(PROPERTY_PREFIX_FOREGROUND + "encrypted", Color.WHITE);
                        break;
                    case FADE:
                        background = SystemProperties.getInstance().get(PROPERTY_PREFIX_BACKGROUND + "fade", Color.LIGHT_GRAY.BLUE);
                        foreground = SystemProperties.getInstance().get(PROPERTY_PREFIX_FOREGROUND + "fade", Color.YELLOW);
                        break;
                    case IDLE:
                        background = SystemProperties.getInstance().get(PROPERTY_PREFIX_BACKGROUND + "idle", Color.WHITE);
                        foreground = SystemProperties.getInstance().get(PROPERTY_PREFIX_FOREGROUND + "idle", Color.BLUE);
                        break;
                    case RESET:
                        background = SystemProperties.getInstance().get(PROPERTY_PREFIX_BACKGROUND + "reset", Color.PINK);
                        foreground = SystemProperties.getInstance().get(PROPERTY_PREFIX_FOREGROUND + "reset", Color.YELLOW);
                        break;
                    case TEARDOWN:
                        background = SystemProperties.getInstance().get(PROPERTY_PREFIX_BACKGROUND + "teardown", Color.DARK_GRAY);
                        foreground = SystemProperties.getInstance().get(PROPERTY_PREFIX_FOREGROUND + "teardown", Color.WHITE);
                        break;
                }
            }
            else
            {
                setText("");
            }

            setBackground(background);
            setForeground(foreground);

            return label;
        }
    }
}
