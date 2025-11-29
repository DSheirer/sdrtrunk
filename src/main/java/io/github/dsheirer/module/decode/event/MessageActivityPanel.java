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

import io.github.dsheirer.filter.FilterSet;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.MessageHistory;
import io.github.dsheirer.module.ProcessingChain;
import io.github.dsheirer.module.decode.DecoderFactory;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.swing.JTableColumnWidthMonitor;
import io.github.dsheirer.sample.Listener;
import java.util.ArrayList;
import java.util.List;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

/**
 * Panel to display decoded messages/activity.
 */
public class MessageActivityPanel extends JPanel implements Listener<ProcessingChain>
{
    private static final long serialVersionUID = 1L;
    private static final Logger mLog = LoggerFactory.getLogger(MessageActivityPanel.class);
    private final String TABLE_PREFERENCE_KEY = "message.activity.panel";
    private MessageActivityModel mMessageModel = new MessageActivityModel();
    private MessageHistory mCurrentMessageHistory;
    private JTable mTable = new JTable(mMessageModel);
    private TableRowSorter<TableModel> mTableRowSorter;
    private JTableColumnWidthMonitor mTableColumnWidthMonitor;
    private UserPreferences mUserPreferences;
    private FilterSet<IMessage> mMessageFilterSet;
    private HistoryManagementPanel<IMessage> mHistoryManagementPanel;

    /**
     * Constructs an instance
     * @param userPreferences
     */
    public MessageActivityPanel(UserPreferences userPreferences)
    {
        mUserPreferences = userPreferences;
        mTableRowSorter = new TableRowSorter<>(mMessageModel);
        mTableRowSorter.setRowFilter(new MessageRowFilter());
        mTable.setRowSorter(mTableRowSorter);
        mTableColumnWidthMonitor = new JTableColumnWidthMonitor(mUserPreferences, mTable, TABLE_PREFERENCE_KEY);
        setLayout(new MigLayout("insets 0 0 0 0", "[][grow,fill]", "[]0[grow,fill]"));
        mHistoryManagementPanel = new HistoryManagementPanel<>(mMessageModel, "Message Filter Editor", mUserPreferences);
        add(mHistoryManagementPanel, "span,growx");
        add(new JScrollPane(mTable), "span,grow");
    }

    /**
     * Updates the message activity model with message history from the specified processing chain
     */
    @Override
    public void receive(ProcessingChain processingChain)
    {
        if(mCurrentMessageHistory != null)
        {
            mCurrentMessageHistory.removeListener(mMessageModel);
        }

        //Unregister from changes made to the filter set
        if(mMessageFilterSet != null)
        {
            mMessageFilterSet.register(null);
        }

        if(processingChain != null)
        {
            mCurrentMessageHistory = processingChain.getMessageHistory();
            mMessageFilterSet = DecoderFactory.getMessageFilters(processingChain.getModules());
            //Register filter change listener to refresh the table any time the event filters are changed.
            mMessageFilterSet.register(() -> mMessageModel.fireTableDataChanged());
            if(mHistoryManagementPanel != null)
            {
                mHistoryManagementPanel.updateFilterSet(mMessageFilterSet);
            }

            List<MessageItem> currentHistory = new ArrayList<>();
            for(IMessage message: mCurrentMessageHistory.getItems())
            {
                currentHistory.add(new MessageItem(message));
            }

            mMessageModel.clearAndSet(currentHistory);
            mCurrentMessageHistory.addListener(mMessageModel);
            mHistoryManagementPanel.setEnabled(true);
        }
        else
        {
            mCurrentMessageHistory = null;
            mMessageFilterSet = null;
            mMessageModel.clear();
            mHistoryManagementPanel.setEnabled(false);
        }
    }

    /**
     * Row visibility filter for messages
     */
    public class MessageRowFilter extends RowFilter<TableModel, Integer>
    {
        @Override
        public boolean include(Entry<? extends TableModel, ? extends Integer> entry)
        {
            if(entry.getModel() instanceof MessageActivityModel model)
            {
                MessageItem item = model.getItem(entry.getIdentifier());

                if(mMessageFilterSet != null && item != null && item.getMessage() != null)
                {
                    IMessage message = item.getMessage();
                    return mMessageFilterSet.canProcess(message) && mMessageFilterSet.passes(message);
                }
            }

            return false;
        }
    }
}
