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
import icon.IconManager;
import icon.IconTableModel;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.text.DecimalFormat;

public class ChannelMetadataPanel extends JPanel
{
    private ChannelMetadataModel mChannelMetadataModel;
    private IconManager mIconManager;
    private JTable mTable;

    public ChannelMetadataPanel(ChannelMetadataModel channelMetadataModel, IconManager iconManager)
    {
        mChannelMetadataModel = channelMetadataModel;
        mIconManager = iconManager;

        init();
    }

    /**
     * Initializes the panel
     */
    private void init()
    {
        setLayout( new MigLayout( "insets 0 0 0 0", "[grow,fill]", "[grow,fill]") );

        mTable = new JTable(mChannelMetadataModel);
        mTable.setRowHeight(35);

        mTable.getColumnModel().getColumn(ChannelMetadataModel.COLUMN_STATE).setCellRenderer(new StateCellRenderer());

        mTable.getColumnModel().getColumn(ChannelMetadataModel.COLUMN_FREQUENCY)
            .setCellRenderer(new FrequencyCellRenderer());

        mTable.getColumnModel().getColumn(ChannelMetadataModel.COLUMN_PRIMARY)
            .setCellRenderer(new AliasedValueCellRenderer(Attribute.PRIMARY_ADDRESS_TO, Attribute.PRIMARY_ADDRESS_FROM));

        mTable.getColumnModel().getColumn(ChannelMetadataModel.COLUMN_SECONDARY)
            .setCellRenderer(new AliasedValueCellRenderer(Attribute.SECONDARY_ADDRESS_TO, Attribute.SECONDARY_ADDRESS_FROM));

        mTable.getColumnModel().getColumn(ChannelMetadataModel.COLUMN_MESSAGE)
            .setCellRenderer(new AliasedValueCellRenderer(Attribute.MESSAGE, Attribute.MESSAGE_TYPE));

        mTable.getColumnModel().getColumn(ChannelMetadataModel.COLUMN_NETWORK)
            .setCellRenderer(new AliasedValueCellRenderer(Attribute.NETWORK_ID_1, Attribute.NETWORK_ID_2));

        mTable.getColumnModel().getColumn(ChannelMetadataModel.COLUMN_CONFIGURATION)
            .setCellRenderer(new AliasedValueCellRenderer(Attribute.CHANNEL_CONFIGURATION_LABEL_1,
                Attribute.CHANNEL_CONFIGURATION_LABEL_2));

        JScrollPane scrollPane = new JScrollPane(mTable, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        add(scrollPane);

    }

    /**
     * Adds a selection listener to the contained metadata JTable
     */
    public void addListSelectionListener(ListSelectionListener listener)
    {
        mTable.getSelectionModel().addListSelectionListener(listener);
    }

    /**
     * Removes the selection listener from the contained metadata JTable
     */
    public void removeListSelectionListener(ListSelectionListener listener)
    {
        mTable.getSelectionModel().removeListSelectionListener(listener);
    }

    public class AliasedValueCellRenderer extends JPanel implements TableCellRenderer
    {
        private JLabel mLabel1;
        private JLabel mAlias1;
        private JLabel mLabel2;
        private JLabel mAlias2;

        private Attribute mAttribute1;
        private Attribute mAttribute2;

        public AliasedValueCellRenderer(Attribute attribute1, Attribute attribute2)
        {
            mAttribute1 = attribute1;
            mAttribute2 = attribute2;

            setLayout( new MigLayout( "insets 0 0 0 0", "[12][grow,fill]", "[grow,fill]0[grow,fill]") );

            mAlias1 = new JLabel();
            add(mAlias1);
            mLabel1 = new JLabel("---");
            mLabel1.setFont(mTable.getFont());
            add(mLabel1,"wrap");
            mAlias2 = new JLabel();
            add(mAlias2);
            mLabel2 = new JLabel("---");
            mLabel2.setFont(mTable.getFont());
            add(mLabel2,"wrap");
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                       int row, int column)
        {
            MutableMetadata metadata = (MutableMetadata)value;

            if(mAttribute1 != null)
            {
                String value1 = metadata.getValue(mAttribute1);
                mLabel1.setText(value1);

                Alias alias1 = metadata.getAlias(mAttribute1);

                if(alias1 != null)
                {
                    mAlias1.setIcon(mIconManager.getIcon(alias1.getIconName(), IconManager.DEFAULT_ICON_SIZE));
                    mLabel1.setForeground(alias1.getDisplayColor());
                }
                else
                {
                    mAlias1.setIcon(null);
                    mLabel1.setForeground(mTable.getForeground());
                }
            }

            if(mAttribute2 != null)
            {
                String value2 = metadata.getValue(mAttribute2);
                mLabel2.setText(value2);

                Alias alias2 = metadata.getAlias(mAttribute2);

                if(alias2 != null)
                {
                    mAlias2.setIcon(mIconManager.getIcon(alias2.getIconName(), IconManager.DEFAULT_ICON_SIZE));
                    mLabel2.setForeground(alias2.getDisplayColor());
                }
                else
                {
                    mAlias2.setIcon(null);
                    mLabel2.setForeground(mTable.getForeground());
                }
            }

            setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());

            return this;
        }
    }

    public class FrequencyCellRenderer extends JPanel implements TableCellRenderer
    {
        private final DecimalFormat FREQUENCY_FORMATTER = new DecimalFormat( "#.000000" );

        private JLabel mLabel1;
        private JLabel mLabel2;

        public FrequencyCellRenderer()
        {
            setLayout( new MigLayout( "insets 0 0 0 0", "[grow,fill,align center]", "[grow,fill]0[grow,fill]") );

            mLabel1 = new JLabel();
            mLabel1.setFont(mTable.getFont());
            add(mLabel1,"wrap");
            mLabel2 = new JLabel();
            mLabel2.setFont(mTable.getFont());
            add(mLabel2);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                       int row, int column)
        {
            MutableMetadata metadata = (MutableMetadata)value;

            if(metadata.hasChannelFrequency())
            {
                mLabel1.setText(FREQUENCY_FORMATTER.format((double)metadata.getChannelFrequency() / 1E6d));
            }
            else
            {
                mLabel1.setText(null);
            }

            mLabel2.setText(metadata.getChannelFrequencyLabel());

            setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());

            return this;
        }
    }

    public class StateCellRenderer extends JPanel implements TableCellRenderer
    {
        private JLabel mLabel1;

        public StateCellRenderer()
        {
            setLayout( new MigLayout( "insets 0 0 0 0", "[grow,fill,align center]", "[grow,fill,align center]") );

            mLabel1 = new JLabel();
            mLabel1.setFont(mTable.getFont());
            add(mLabel1);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                       int row, int column)
        {
            MutableMetadata metadata = (MutableMetadata)value;

            mLabel1.setText(metadata.getState().getDisplayValue());

            setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());

            return this;
        }
    }

}
