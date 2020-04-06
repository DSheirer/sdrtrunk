/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.source.tuner;

import com.jidesoft.swing.JideSplitPane;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.swing.JTableColumnWidthMonitor;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.tuner.TunerEvent.Event;
import io.github.dsheirer.source.tuner.recording.AddRecordingTunerDialog;
import io.github.dsheirer.source.tuner.recording.RecordingTuner;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class TunerViewPanel extends JPanel
{
    private static final long serialVersionUID = 1L;
    private final static Logger mLog = LoggerFactory.getLogger(TunerViewPanel.class);
    private static final String TABLE_PREFERENCE_KEY = "tuner.view.panel";

    private TunerModel mTunerModel;
    private JTable mTunerTable;
    private JTableColumnWidthMonitor mColumnWidthMonitor;
    private TableRowSorter<TunerModel> mRowSorter;
    private JideSplitPane mSplitPane;
    private TunerEditor mTunerEditor;
    private UserPreferences mUserPreferences;

    public TunerViewPanel(TunerModel tunerModel, UserPreferences userPreferences)
    {
        mTunerModel = tunerModel;
        mTunerEditor = new TunerEditor(mTunerModel.getTunerConfigurationModel(), userPreferences);
        mUserPreferences = userPreferences;

        init();
    }

    private void init()
    {
        setLayout(new MigLayout("insets 0 0 0 0", "[fill,grow]", "[fill,grow]"));

        mRowSorter = new TableRowSorter<>(mTunerModel);
        List<RowSorter.SortKey> sortKeys = new ArrayList<>();
        sortKeys.add(new RowSorter.SortKey(TunerModel.TUNER_TYPE, SortOrder.ASCENDING));
        sortKeys.add(new RowSorter.SortKey(TunerModel.TUNER_ID, SortOrder.ASCENDING));
        mRowSorter.setSortKeys(sortKeys);

        mTunerTable = new JTable(mTunerModel);
        mTunerTable.setRowSorter(mRowSorter);
        mTunerTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        mTunerTable.getSelectionModel().addListSelectionListener(new ListSelectionListener()
        {
            @Override
            public void valueChanged(ListSelectionEvent event)
            {
                if(!event.getValueIsAdjusting())
                {
                    int row = mTunerTable.getSelectedRow();

                    if(row >= 0)
                    {
                        int modelRow = mTunerTable.convertRowIndexToModel(row);
                        mTunerEditor.setItem(mTunerModel.getTuner(modelRow));
                    }
                }
            }
        });

        mTunerModel.addListener(new Listener<TunerEvent>()
        {
            @Override
            public void receive(TunerEvent tunerEvent)
            {
                switch(tunerEvent.getEvent())
                {
                    case LOCK_STATE_CHANGE:
                        if(tunerEvent.getTuner() != null)
                        {
                            int row = mTunerTable.getSelectedRow();

                            if(row >= 0)
                            {
                                int modelRow = mTunerTable.convertRowIndexToModel(row);
                                Tuner selectedTuner = mTunerModel.getTuner(modelRow);

                                if(selectedTuner == tunerEvent.getTuner())
                                {
                                    mTunerEditor.setTunerLockState(selectedTuner.getTunerController().isLocked());
                                }
                            }
                        }
                        break;
                }
            }
        });

        mTunerTable.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                if(e.getButton() == MouseEvent.BUTTON1) //Left-Click
                {
                    int column = mTunerTable.columnAtPoint(e.getPoint());

                    if(column == TunerModel.SPECTRAL_DISPLAY_NEW)
                    {
                        int tableRow = mTunerTable.rowAtPoint(e.getPoint());
                        int modelRow = mTunerTable.convertRowIndexToModel(tableRow);

                        Tuner tuner = mTunerModel.getTuner(modelRow);

                        if(tuner != null)
                        {
                            mTunerModel.broadcast(new TunerEvent(tuner,
                                Event.REQUEST_NEW_SPECTRAL_DISPLAY));
                        }
                    }
                }
                else if(e.getButton() == MouseEvent.BUTTON3) //Right-Click
                {
                    int tableRow = mTunerTable.rowAtPoint(e.getPoint());
                    int modelRow = mTunerTable.convertRowIndexToModel(tableRow);

                    final Tuner tuner = mTunerModel.getTuner(modelRow);

                    if(tuner instanceof RecordingTuner && tuner.getChannelSourceManager().getTunerChannelCount() == 0)
                    {
                        JPopupMenu popupMenu = new JPopupMenu();
                        JMenuItem removeTunerItem = new JMenuItem("Remove Recording Tuner");
                        removeTunerItem.addActionListener(new ActionListener()
                        {
                            @Override
                            public void actionPerformed(ActionEvent e)
                            {
                                mTunerModel.removeTuner(tuner);
                            }
                        });
                        popupMenu.add(removeTunerItem);
                        popupMenu.show(mTunerTable, e.getX(), e.getY());
                    }
                }
            }
        });

        TableCellRenderer linkCellRenderer = new LinkCellRenderer();
        mTunerTable.getColumnModel().getColumn(TunerModel.SPECTRAL_DISPLAY_NEW).setCellRenderer(linkCellRenderer);

        TableCellRenderer errorCellRenderer = new ErrorCellRenderer();
        mTunerTable.getColumnModel().getColumn(TunerModel.SAMPLE_RATE).setCellRenderer(errorCellRenderer);
        mTunerTable.getColumnModel().getColumn(TunerModel.FREQUENCY).setCellRenderer(errorCellRenderer);
        mTunerTable.getColumnModel().getColumn(TunerModel.CHANNEL_COUNT).setCellRenderer(errorCellRenderer);
        mTunerTable.getColumnModel().getColumn(TunerModel.FREQUENCY_ERROR).setCellRenderer(errorCellRenderer);
        mTunerTable.getColumnModel().getColumn(TunerModel.MEASURED_FREQUENCY_ERROR).setCellRenderer(errorCellRenderer);

        mColumnWidthMonitor = new JTableColumnWidthMonitor(mUserPreferences, mTunerTable, TABLE_PREFERENCE_KEY);
        JScrollPane tunerTableScroller = new JScrollPane(mTunerTable);
        tunerTableScroller.setPreferredSize(new Dimension(400, 20));

        JPanel tunerTablePanel = new JPanel();
        tunerTablePanel.setLayout(new MigLayout("insets 0 0 0 0", "[fill,grow][]", "[fill,grow][]"));
        tunerTablePanel.add(tunerTableScroller, "span");

        tunerTablePanel.add(new JLabel("")); //Empty spacer
        JButton addRecordingTunerButton = new JButton("Add Recording Tuner");
        addRecordingTunerButton.setEnabled(mTunerModel.canAddRecordingTuner());
        addRecordingTunerButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                AddRecordingTunerDialog dialog = new AddRecordingTunerDialog(mUserPreferences, mTunerModel);
                dialog.setLocationRelativeTo(TunerViewPanel.this);

                EventQueue.invokeLater(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        dialog.setVisible(true);
                    }
                });
            }
        });
        tunerTablePanel.add(addRecordingTunerButton);

        tunerTablePanel.add(addRecordingTunerButton);

        JScrollPane editorScroller = new JScrollPane(mTunerEditor);
        editorScroller.setPreferredSize(new Dimension(400, 80));

        mSplitPane = new JideSplitPane();
        mSplitPane.setOrientation(JideSplitPane.VERTICAL_SPLIT);
        mSplitPane.add(tunerTablePanel);
        mSplitPane.add(editorScroller);

        add(mSplitPane);
    }

    public class ErrorCellRenderer extends DefaultTableCellRenderer
    {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
        {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            Tuner tuner = mTunerModel.getTuner(mTunerTable.convertRowIndexToModel(row));

            if(isSelected)
            {
                component.setBackground(table.getSelectionBackground());
            }
            else if(tuner.hasError())
            {
                component.setBackground(Color.RED);
            }
            else
            {
                component.setBackground(table.getBackground());
            }

            return component;
        }
    }

    public class LinkCellRenderer extends DefaultTableCellRenderer
    {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable table,
                                                       Object value, boolean isSelected, boolean hasFocus, int row,
                                                       int column)
        {
            JLabel label = (JLabel)super.getTableCellRendererComponent(table,
                value, isSelected, hasFocus, row, column);

            label.setForeground(Color.BLUE.brighter());
            label.setToolTipText("Show this tuner in a new spectral display");

            return label;
        }
    }
}
