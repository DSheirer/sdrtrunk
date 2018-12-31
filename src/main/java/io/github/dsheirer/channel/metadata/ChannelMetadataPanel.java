/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */
package io.github.dsheirer.channel.metadata;

import com.google.common.base.Joiner;
import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.channel.state.State;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.ChannelModel;
import io.github.dsheirer.controller.channel.ChannelProcessingManager;
import io.github.dsheirer.controller.channel.ChannelUtils;
import io.github.dsheirer.icon.IconManager;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.configuration.FrequencyConfigurationIdentifier;
import io.github.dsheirer.identifier.decoder.DecoderStateIdentifier;
import io.github.dsheirer.module.ProcessingChain;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.identifier.TalkgroupFormatPreference;
import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;
import net.miginfocom.swing.MigLayout;

import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChannelMetadataPanel extends JPanel implements ListSelectionListener
{
    private ChannelModel mChannelModel;
    private ChannelProcessingManager mChannelProcessingManager;
    private IconManager mIconManager;
    private UserPreferences mUserPreferences;
    private JTable mTable;
    private Broadcaster<ProcessingChain> mSelectedProcessingChainBroadcaster = new Broadcaster<>();
    private Map<State,Color> mBackgroundColors = new HashMap<>();
    private Map<State,Color> mForegroundColors = new HashMap<>();

    /**
     * Table view for currently decoding channel metadata
     */
    public ChannelMetadataPanel(ChannelModel channelModel, ChannelProcessingManager channelProcessingManager,
                                IconManager iconManager, UserPreferences userPreferences)
    {
        mChannelModel = channelModel;
        mChannelProcessingManager = channelProcessingManager;
        mIconManager = iconManager;
        mUserPreferences = userPreferences;
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
        mTable.addMouseListener(new MouseSupport());

        mTable.getColumnModel().getColumn(ChannelMetadataModel.COLUMN_DECODER_STATE)
            .setCellRenderer(new ColoredStateCellRenderer());
        mTable.getColumnModel().getColumn(ChannelMetadataModel.COLUMN_USER_FROM)
            .setCellRenderer(new FromCellRenderer(mUserPreferences.getTalkgroupFormatPreference()));
        mTable.getColumnModel().getColumn(ChannelMetadataModel.COLUMN_USER_TO)
            .setCellRenderer(new ToCellRenderer(mUserPreferences.getTalkgroupFormatPreference()));
        mTable.getColumnModel().getColumn(ChannelMetadataModel.COLUMN_USER_FROM_ALIAS)
            .setCellRenderer(new AliasCellRenderer());
        mTable.getColumnModel().getColumn(ChannelMetadataModel.COLUMN_USER_TO_ALIAS)
            .setCellRenderer(new AliasCellRenderer());
        mTable.getColumnModel().getColumn(ChannelMetadataModel.COLUMN_CONFIGURATION_FREQUENCY)
            .setCellRenderer(new FrequencyCellRenderer());

        JScrollPane scrollPane = new JScrollPane(mTable, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        add(scrollPane);

        setColors();
    }

    /**
     * Setup the background and foreground color palette for the various channel states.
     */
    private void setColors()
    {
        mBackgroundColors.put(State.ACTIVE, Color.CYAN);
        mForegroundColors.put(State.ACTIVE, Color.YELLOW);
        mBackgroundColors.put(State.CALL, Color.BLUE);
        mForegroundColors.put(State.CALL, Color.YELLOW);
        mBackgroundColors.put(State.CONTROL, Color.ORANGE);
        mForegroundColors.put(State.CONTROL, Color.BLUE);
        mBackgroundColors.put(State.DATA, Color.GREEN);
        mForegroundColors.put(State.DATA, Color.BLUE);
        mBackgroundColors.put(State.ENCRYPTED, Color.MAGENTA);
        mForegroundColors.put(State.ENCRYPTED, Color.WHITE);
        mBackgroundColors.put(State.FADE, Color.LIGHT_GRAY);
        mForegroundColors.put(State.FADE, Color.DARK_GRAY);
        mBackgroundColors.put(State.IDLE, Color.WHITE);
        mForegroundColors.put(State.IDLE, Color.DARK_GRAY);
        mBackgroundColors.put(State.RESET, Color.PINK);
        mForegroundColors.put(State.RESET, Color.YELLOW);
        mBackgroundColors.put(State.TEARDOWN, Color.DARK_GRAY);
        mForegroundColors.put(State.TEARDOWN, Color.WHITE);
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

                ChannelMetadata selectedMetadata = mChannelProcessingManager.getChannelMetadataModel()
                    .getChannelMetadata(selectedModelRow);

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

    /**
     * Cell renderer for frequency values
     */
    public class FrequencyCellRenderer extends DefaultTableCellRenderer
    {
        private final DecimalFormat FREQUENCY_FORMATTER = new DecimalFormat( "#.00000" );

        public FrequencyCellRenderer()
        {
            setHorizontalAlignment(JLabel.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
        {
            JLabel label = (JLabel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if(value instanceof FrequencyConfigurationIdentifier)
            {
                long frequency = ((FrequencyConfigurationIdentifier)value).getValue();
                label.setText(FREQUENCY_FORMATTER.format(frequency / 1e6d));
            }
            else
            {
                label.setText(null);
            }

            return label;
        }
    }

    /**
     * Alias cell renderer
     */
    public class AliasCellRenderer extends DefaultTableCellRenderer
    {
        public AliasCellRenderer()
        {
            setHorizontalAlignment(JLabel.LEFT);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
        {
            JLabel label = (JLabel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if(value instanceof List<?>)
            {
                List<Alias> aliases = (List<Alias>)value;

                if(!aliases.isEmpty())
                {
                    label.setText(Joiner.on(", ").skipNulls().join(aliases));
                    label.setIcon(mIconManager.getIcon(aliases.get(0).getIconName(), IconManager.DEFAULT_ICON_SIZE));
                    label.setForeground(aliases.get(0).getDisplayColor());
                }
                else
                {
                    label.setText(null);
                    label.setIcon(null);
                    label.setForeground(table.getForeground());
                }
            }
            else
            {
                label.setText(null);
                label.setIcon(null);
                label.setForeground(table.getForeground());
            }

            return label;
        }
    }

    /**
     * Abstract cell renderer for identifiers
     */
    public abstract class IdentifierCellRenderer extends DefaultTableCellRenderer
    {
        private final static String EMPTY_VALUE = "-----";
        private TalkgroupFormatPreference mTalkgroupFormatPreference;

        public IdentifierCellRenderer(TalkgroupFormatPreference talkgroupFormatPreference)
        {
            mTalkgroupFormatPreference = talkgroupFormatPreference;
            setHorizontalAlignment(JLabel.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
        {
            JLabel label = (JLabel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if(value instanceof ChannelMetadata)
            {
                ChannelMetadata channelMetadata = (ChannelMetadata)value;
                Identifier identifier = getIdentifier(channelMetadata);
                String text = mTalkgroupFormatPreference.format(identifier);
                if(text == null || text.isEmpty())
                {
                    text = EMPTY_VALUE;
                }

                label.setText(text);
            }
            else
            {
                label.setText(EMPTY_VALUE);
            }

            return label;
        }

        public abstract Identifier getIdentifier(ChannelMetadata channelMetadata);
    }

    /**
     * Cell renderer for the FROM identifier
     */
    public class FromCellRenderer extends IdentifierCellRenderer
    {
        public FromCellRenderer(TalkgroupFormatPreference talkgroupFormatPreference)
        {
            super(talkgroupFormatPreference);
        }

        @Override
        public Identifier getIdentifier(ChannelMetadata channelMetadata)
        {
            return channelMetadata.getFromIdentifier();
        }
    }

    /**
     * Cell renderer for the TO identifier
     */
    public class ToCellRenderer extends IdentifierCellRenderer
    {
        public ToCellRenderer(TalkgroupFormatPreference talkgroupFormatPreference)
        {
            super(talkgroupFormatPreference);
        }

        @Override
        public Identifier getIdentifier(ChannelMetadata channelMetadata)
        {
            return channelMetadata.getToIdentifier();
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

            if(value instanceof DecoderStateIdentifier)
            {
                State state = ((DecoderStateIdentifier)value).getValue();
                label.setText(state.getDisplayValue());

                if(mBackgroundColors.containsKey(state))
                {
                    background = mBackgroundColors.get(state);
                }

                if(mForegroundColors.containsKey(state))
                {
                    foreground = mForegroundColors.get(state);
                }
            }
            else
            {
                setText("----");
            }

            setBackground(background);
            setForeground(foreground);

            return label;
        }
    }

    public class MouseSupport extends MouseAdapter
    {
        @Override
        public void mouseClicked(MouseEvent e)
        {
            if(e.getButton() == MouseEvent.BUTTON3) //Right click for context
            {
                JPopupMenu popupMenu = new JPopupMenu();

                boolean populated = false;

                int viewRowIndex = mTable.rowAtPoint(e.getPoint());

                if(viewRowIndex >= 0)
                {
                    int modelRowIndex = mTable.convertRowIndexToModel(viewRowIndex);

                    if(modelRowIndex >= 0)
                    {
                        ChannelMetadata metadata = mChannelProcessingManager.getChannelMetadataModel().getChannelMetadata(modelRowIndex);

                        if(metadata != null)
                        {
                            Channel channel = mChannelProcessingManager.getChannelMetadataModel()
                                .getChannelFromMetadata(metadata);

                            if(channel != null)
                            {
                                popupMenu.add(ChannelUtils.getContextMenu(mChannelModel, mChannelProcessingManager, channel, mTable));
                                populated = true;
                            }
                        }

                    }
                }

                if(!populated)
                {
                    popupMenu.add(new JMenuItem("No Actions Available"));
                }

                popupMenu.show(mTable, e.getX(), e.getY());
            }
        }
    }
}
