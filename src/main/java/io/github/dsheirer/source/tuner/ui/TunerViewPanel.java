/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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

package io.github.dsheirer.source.tuner.ui;

import com.jidesoft.swing.JideSplitPane;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.swing.JTableColumnWidthMonitor;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.manager.TunerStatus;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel containing a discovered tuners table and a tuner editor for a selected tuner.
 */
public class TunerViewPanel extends JPanel
{
    private static final long serialVersionUID = 1L;
    private final static Logger mLog = LoggerFactory.getLogger(TunerViewPanel.class);
    private static final String TABLE_PREFERENCE_KEY = "tuner.view.panel";

    private DiscoveredTunerModel mDiscoveredTunerModel;
    private JTable mTunerTable;
    private JTableColumnWidthMonitor mColumnWidthMonitor;
    private TableRowSorter<DiscoveredTunerModel> mRowSorter;
    private JideSplitPane mSplitPane;
    private DiscoveredTunerEditor mDiscoveredTunerEditor;
    private UserPreferences mUserPreferences;

    /**
     * Constructs an instance
     * @param tunerManager for tuners
     * @param userPreferences for making recordings in the tuner editor
     */
    public TunerViewPanel(TunerManager tunerManager, UserPreferences userPreferences)
    {
        mDiscoveredTunerModel = tunerManager.getDiscoveredTunerModel();
        mDiscoveredTunerEditor = new DiscoveredTunerEditor(userPreferences, tunerManager);
        mUserPreferences = userPreferences;
        init();
    }

    private void init()
    {
        setLayout(new MigLayout("insets 0 0 0 0", "[fill,grow]", "[fill,grow]"));

        mRowSorter = new TableRowSorter<>(mDiscoveredTunerModel);
        List<RowSorter.SortKey> sortKeys = new ArrayList<>();
        sortKeys.add(new RowSorter.SortKey(DiscoveredTunerModel.COLUMN_TUNER_TYPE, SortOrder.ASCENDING));
        mRowSorter.setSortKeys(sortKeys);

        mTunerTable = new JTable(mDiscoveredTunerModel);
        mTunerTable.setRowSorter(mRowSorter);
        mTunerTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        mTunerTable.getSelectionModel().addListSelectionListener(event ->
        {
            if(!event.getValueIsAdjusting())
            {
                int row = mTunerTable.getSelectedRow();

                if(row >= 0)
                {
                    int modelRow = mTunerTable.convertRowIndexToModel(row);
                    mDiscoveredTunerEditor.setItem(mDiscoveredTunerModel.getDiscoveredTuner(modelRow));
                }
            }
        });

        //Monitor for tuner removal events so we can update the editor when our selected tuner is removed
        mDiscoveredTunerModel.addTableModelListener(e ->
        {
            //Detect when status is for the currently selected tuner
            if(e.getType() == TableModelEvent.DELETE &&
                mDiscoveredTunerEditor.hasItem() &&
                !mDiscoveredTunerModel.hasTuner(mDiscoveredTunerEditor.getItem()))
            {
                mDiscoveredTunerEditor.setItem(null);
            }
        });

        mDiscoveredTunerModel.addListener(tunerEvent ->
        {
            switch(tunerEvent.getEvent())
            {
                case UPDATE_LOCK_STATE:
                    if(tunerEvent.getTuner() != null)
                    {
                        int row = mTunerTable.getSelectedRow();

                        if(row >= 0)
                        {
                            int modelRow = mTunerTable.convertRowIndexToModel(row);
                            DiscoveredTuner selectedTuner = mDiscoveredTunerModel.getDiscoveredTuner(modelRow);

                            if(selectedTuner != null && selectedTuner.hasTuner() && tunerEvent.getTuner() == selectedTuner.getTuner())
                            {
                                mDiscoveredTunerEditor.setTunerLockState(selectedTuner.getTuner().getTunerController().isLocked());
                            }
                        }
                    }
                    break;
            }
        });


        TableCellRenderer errorCellRenderer = new TunerStatusCellRenderer();
        mTunerTable.getColumnModel().getColumn(DiscoveredTunerModel.COLUMN_TUNER_STATUS).setCellRenderer(errorCellRenderer);

        mColumnWidthMonitor = new JTableColumnWidthMonitor(mUserPreferences, mTunerTable, TABLE_PREFERENCE_KEY);
        JScrollPane tunerTableScroller = new JScrollPane(mTunerTable);

        JPanel tunerTablePanel = new JPanel();
        tunerTablePanel.setLayout(new MigLayout("insets 0 0 0 0", "[fill,grow][]", "[fill,grow][]"));
        tunerTablePanel.add(tunerTableScroller, "span");

        tunerTablePanel.add(new JLabel("")); //Empty spacer

//        JButton addRecordingTunerButton = new JButton("Add Recording Tuner");
//        addRecordingTunerButton.addActionListener(e ->
//        {
//            AddRecordingTunerDialog dialog = new AddRecordingTunerDialog(mUserPreferences, mDiscoveredTunerModel);
//            dialog.setLocationRelativeTo(TunerViewPanel.this);
//            EventQueue.invokeLater(() -> dialog.setVisible(true));
//        });
//
//        tunerTablePanel.add(addRecordingTunerButton);

        tunerTablePanel.setPreferredSize(new Dimension(200,200));
        JScrollPane editorScroller = new JScrollPane(mDiscoveredTunerEditor);
        editorScroller.setPreferredSize(new Dimension(200, 200));

        mSplitPane = new JideSplitPane();
        mSplitPane.setOrientation(JideSplitPane.HORIZONTAL_SPLIT);
        mSplitPane.add(tunerTablePanel);
        mSplitPane.add(editorScroller);

        add(mSplitPane);
    }

    /**
     * Custom cell renderer for the TunerStatus enumeration column
     */
    public class TunerStatusCellRenderer extends DefaultTableCellRenderer
    {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                       int row, int column)
        {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if(value instanceof TunerStatus tunerStatus)
            {
                if(tunerStatus == TunerStatus.ERROR)
                {
                    component.setForeground(Color.RED);
                }
                else if(tunerStatus == TunerStatus.DISABLED)
                {
                    component.setForeground(Color.DARK_GRAY);
                }
                else
                {
                    component.setForeground(table.getForeground());
                }
            }
            else
            {
                component.setForeground(table.getForeground());
            }

            return component;
        }
    }
}
