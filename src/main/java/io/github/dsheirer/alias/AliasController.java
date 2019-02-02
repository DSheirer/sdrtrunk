/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2019 Dennis Sheirer
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

package io.github.dsheirer.alias;

import com.jidesoft.swing.JideSplitPane;
import io.github.dsheirer.audio.broadcast.BroadcastModel;
import io.github.dsheirer.gui.editor.Editor;
import io.github.dsheirer.icon.IconManager;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.swing.JTableColumnWidthMonitor;
import net.coderazzi.filters.gui.AutoChoices;
import net.coderazzi.filters.gui.TableFilterHeader;
import net.miginfocom.swing.MigLayout;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class AliasController extends JPanel implements ActionListener, ListSelectionListener
{
    private static final long serialVersionUID = 1L;

    private static final String TABLE_PREFERENCE_KEY = "alias.controller";
    private AliasModel mAliasModel;
    private JTable mAliasTable;
    private TableFilterHeader mTableFilterHeader;
    private AliasEditor mAliasEditor;
    private MultipleAliasEditor mMultipleAliasEditor;
    private JideSplitPane mSplitPane;
    private UserPreferences mUserPreferences;

    private static final String NEW_ALIAS = "New";
    private static final String COPY_ALIAS = "Copy";
    private static final String DELETE_ALIAS = "Delete";

    private JButton mNewButton = new JButton(NEW_ALIAS);
    private JButton mCopyButton = new JButton(COPY_ALIAS);
    private JButton mDeleteButton = new JButton(DELETE_ALIAS);

    private IconCellRenderer mIconCellRenderer;
    private JTableColumnWidthMonitor mColumnWidthMonitor;

    public AliasController(AliasModel aliasModel, BroadcastModel broadcastModel, IconManager iconManager,
                           UserPreferences userPreferences)
    {
        mAliasModel = aliasModel;
        mAliasEditor = new AliasEditor(mAliasModel, broadcastModel, iconManager);
        mMultipleAliasEditor = new MultipleAliasEditor(mAliasModel, broadcastModel, iconManager);
        mIconCellRenderer = new IconCellRenderer(iconManager);
        mUserPreferences = userPreferences;

        init();

        mAliasModel.addListener(mAliasEditor);
    }

    private void init()
    {
        setLayout(new MigLayout("insets 0 0 0 0", "[grow,fill]", "[grow,fill]"));

        //System Configuration View and Editor
        mAliasTable = new JTable(mAliasModel);
        mAliasTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        mAliasTable.getSelectionModel().addListSelectionListener(this);
        mAliasTable.setAutoCreateRowSorter(true);

        mAliasTable.getColumnModel().getColumn(AliasModel.COLUMN_COLOR).setCellRenderer(new ColorCellRenderer());

        mAliasTable.getColumnModel().getColumn(AliasModel.COLUMN_ICON).setCellRenderer(mIconCellRenderer);

        mColumnWidthMonitor = new JTableColumnWidthMonitor(mUserPreferences, mAliasTable, TABLE_PREFERENCE_KEY);

        mTableFilterHeader = new TableFilterHeader(mAliasTable, AutoChoices.ENABLED);
        mTableFilterHeader.setFilterOnUpdates(true);

        JScrollPane tableScroller = new JScrollPane(mAliasTable);

        JPanel buttonsPanel = new JPanel();

        buttonsPanel.setLayout(new MigLayout("insets 0 0 0 0",
            "[grow,fill][grow,fill][grow,fill]", "[]"));

        mNewButton.addActionListener(this);
        mNewButton.setToolTipText("Adds a new alias");
        buttonsPanel.add(mNewButton);

        mCopyButton.addActionListener(this);
        mCopyButton.setEnabled(false);
        mCopyButton.setToolTipText("Creates a copy of the currently selected alias and adds it");
        buttonsPanel.add(mCopyButton);

        mDeleteButton.addActionListener(this);
        mDeleteButton.setEnabled(false);
        mDeleteButton.setToolTipText("Deletes the currently selected alias");
        buttonsPanel.add(mDeleteButton);

        JPanel listAndButtonsPanel = new JPanel();

        listAndButtonsPanel.setLayout(new MigLayout("insets 0 0 0 0", "[grow,fill]",
            "[grow,fill][]"));

        listAndButtonsPanel.add(tableScroller, "wrap");
        listAndButtonsPanel.add(buttonsPanel);

        mSplitPane = new JideSplitPane(JideSplitPane.HORIZONTAL_SPLIT);
        mSplitPane.setDividerSize(5);
        mSplitPane.setShowGripper(true);
        mSplitPane.add(listAndButtonsPanel);
        mSplitPane.add(mAliasEditor);

        add(mSplitPane);
    }

    /**
     * Sets the editor argument as the currently visible alias editor
     */
    private void setEditor(Editor<?> editor)
    {
        if(mSplitPane.getComponentCount() == 3)
        {
            Component component = mSplitPane.getComponent(2);

            if(component instanceof Editor<?> && component != editor)
            {
                //Get current divider location so that we can reapply it after
                //changing out the editors, so that the interface doesn't move
                int location = mSplitPane.getDividerLocation(0);

                ((Editor<?>) component).setItem(null);

                mSplitPane.remove(component);
                editor.setPreferredSize(new Dimension(100, 100));
                mSplitPane.add(editor);

                mSplitPane.validate();
                mSplitPane.setDividerLocation(0, location);
                mSplitPane.repaint();
            }
        }
    }

    private Alias getAlias(int selectedRow)
    {
        if(selectedRow >= 0)
        {
            int index = mAliasTable.convertRowIndexToModel(selectedRow);

            return mAliasModel.getAliasAtIndex(index);
        }

        return null;
    }

    @Override
    public void valueChanged(ListSelectionEvent event)
    {
        //Limits event firing to only when selection is complete
        if(!event.getValueIsAdjusting())
        {
            int[] selectedRows = mAliasTable.getSelectedRows();

            if(selectedRows.length == 0)
            {
                mAliasEditor.setItem(null);
                mCopyButton.setEnabled(false);
                mDeleteButton.setEnabled(false);
            }
            else if(selectedRows.length == 1)
            {
                setEditor(mAliasEditor);

                Alias selectedAlias = getAlias(selectedRows[0]);

                mAliasEditor.setItem(selectedAlias);

                if(selectedAlias != null)
                {
                    mCopyButton.setEnabled(true);
                    mDeleteButton.setEnabled(true);
                }
                else
                {
                    mCopyButton.setEnabled(false);
                    mDeleteButton.setEnabled(false);
                }
            }
            else
            {
                setEditor(mMultipleAliasEditor);

                mCopyButton.setEnabled(true);
                mDeleteButton.setEnabled(true);

                List<Alias> selectedAliases = new ArrayList<Alias>();

                for(int selectedRow : selectedRows)
                {
                    Alias alias = getAlias(selectedRow);

                    selectedAliases.add(alias);
                }

                mMultipleAliasEditor.setItem(selectedAliases);
            }
        }
    }

    /**
     * Adds the alias to the alias table/model and scrolls the view it
     */
    private void addAlias(Alias alias)
    {
        //Remove any column headers so that the newly added alias shows correctly.
        mTableFilterHeader.resetFilter();

        int index = mAliasModel.addAlias(alias);

        if(index >= 0)
        {
            int translatedIndex = mAliasTable.convertRowIndexToView(index);
            mAliasTable.setRowSelectionInterval(translatedIndex, translatedIndex);
            mAliasTable.scrollRectToVisible(
                new Rectangle(mAliasTable.getCellRect(translatedIndex, 0, true)));
        }
    }

    private List<Alias> getSelectedAliases()
    {
        List<Alias> selected = new ArrayList<>();

        int[] rows = mAliasTable.getSelectedRows();

        for(int row : rows)
        {
            if(row >= 0)
            {
                Alias alias = mAliasModel.getAliasAtIndex(mAliasTable.convertRowIndexToModel(row));

                if(alias != null)
                {
                    selected.add(alias);
                }
            }
        }

        return selected;
    }

    /**
     * Responds to New, Copy and Delete Channel button invocations
     */
    @Override
    public void actionPerformed(ActionEvent event)
    {
        switch(event.getActionCommand())
        {
            case NEW_ALIAS:
                addAlias(new Alias("New Alias"));
                break;
            case COPY_ALIAS:
                for(Alias alias : getSelectedAliases())
                {
                    addAlias(AliasFactory.copyOf(alias));
                }
                break;
            case DELETE_ALIAS:
                List<Alias> toDelete = getSelectedAliases();

                if(toDelete != null && !toDelete.isEmpty())
                {
                    String title = toDelete.size() == 1 ? "Delete Alias?" : "Delete Aliases?";
                    String prompt = toDelete.size() == 1 ? "Do you want to delete this alias?" :
                        "Do you want to delete these " + toDelete.size() + " aliases?";

                    int choice = JOptionPane.showConfirmDialog(AliasController.this,
                        prompt, title, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

                    if(choice == JOptionPane.YES_OPTION)
                    {
                        for(Alias alias : toDelete)
                        {
                            mAliasModel.removeAlias(alias);
                        }
                    }
                }
                break;
            default:
                break;
        }
    }

    /**
     * Colorizes the cell based on the cell's integer value
     */
    public class ColorCellRenderer extends DefaultTableCellRenderer
    {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable table,
                                                       Object value, boolean isSelected, boolean hasFocus, int row,
                                                       int column)
        {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table,
                value, isSelected, hasFocus, row, column);

            label.setBackground(new Color((int) value));

            label.setText("");

            return label;
        }
    }

    /**
     * Displays cell's icon and name
     */
    public class IconCellRenderer extends DefaultTableCellRenderer
    {
        private static final long serialVersionUID = 1L;

        private IconManager mIconManager;

        public IconCellRenderer(IconManager iconManager)
        {
            mIconManager = iconManager;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table,
                                                       Object value, boolean isSelected, boolean hasFocus, int row,
                                                       int column)
        {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table,
                value, isSelected, hasFocus, row, column);

            ImageIcon icon = mIconManager.getIcon(label.getText(), 12);

            if(icon != null)
            {
                label.setIcon(icon);
            }

            return label;
        }
    }
}
