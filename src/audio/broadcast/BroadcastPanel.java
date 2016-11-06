/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2016 Dennis Sheirer
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

import alias.Alias;
import alias.AliasFactory;
import alias.id.AliasID;
import alias.id.AliasIDType;
import alias.id.AliasIdentifierEditor;
import com.jidesoft.swing.JideSplitPane;
import gui.editor.Editor;
import gui.editor.EmptyEditor;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class BroadcastPanel extends JPanel implements ActionListener, ListSelectionListener
{
    private static final String NEW_ALIAS = "New";
    private static final String COPY_ALIAS = "Copy";
    private static final String DELETE_ALIAS = "Delete";

    private BroadcastModel mBroadcastModel;
    private JTable mBroadcastConfigurationTable;
    private JideSplitPane mSplitPane;
    private Editor<BroadcastConfiguration> mEmptyEditor = new EmptyEditor<>("Configuration:");
    private Editor<BroadcastConfiguration> mEditor;
    private JButton mNewButton = new JButton(NEW_ALIAS);
    private JButton mCopyButton = new JButton(COPY_ALIAS);
    private JButton mDeleteButton = new JButton(DELETE_ALIAS);

    public BroadcastPanel(BroadcastModel broadcastModel)
    {
        mBroadcastModel = broadcastModel;
        mEditor = mEmptyEditor;

        init();
    }

    private void init()
    {
        setLayout(new MigLayout("insets 0 0 0 0 ", "[grow,fill]", "[grow,fill]"));

        mBroadcastConfigurationTable = new JTable(mBroadcastModel);
        mBroadcastConfigurationTable.setAutoCreateRowSorter(true);
        mBroadcastConfigurationTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        mBroadcastConfigurationTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        mBroadcastConfigurationTable.getSelectionModel().addListSelectionListener(BroadcastPanel.this);

        JScrollPane scroller = new JScrollPane(mBroadcastConfigurationTable);

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new MigLayout("insets 0 0 0 0", "[grow,fill][grow,fill][grow,fill]", "[]"));

        mNewButton = new JButton("New ...");
        mNewButton.setToolTipText("Create a new broadcast configuration");
        mNewButton.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                JPopupMenu menu = new JPopupMenu();

                menu.add(new AddBroadcastConfigurationItem(BroadcastServerType.BROADCASTIFY));

                menu.show(e.getComponent(), e.getX(), e.getY());
            }
        });

        buttonsPanel.add(mNewButton);

        mCopyButton.addActionListener(this);
        mCopyButton.setEnabled(false);
        mCopyButton.setToolTipText("Creates a copy of the currently selected streaming configuration and adds it");
        buttonsPanel.add(mCopyButton);

        mDeleteButton.addActionListener(this);
        mDeleteButton.setEnabled(false);
        mDeleteButton.setToolTipText("Deletes the currently selected streaming configuration");
        buttonsPanel.add(mDeleteButton);

        JPanel listAndButtonsPanel = new JPanel();

        listAndButtonsPanel.setLayout(new MigLayout("insets 0 0 0 0", "[grow,fill]", "[grow,fill][]"));

        listAndButtonsPanel.add(scroller, "wrap");
        listAndButtonsPanel.add(buttonsPanel);

        mSplitPane = new JideSplitPane(JideSplitPane.HORIZONTAL_SPLIT);
        mSplitPane.setDividerSize(5);
        mSplitPane.setShowGripper(true);
        mSplitPane.add(listAndButtonsPanel);

        mSplitPane.add(mEditor);

        add(mSplitPane);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {

    }

    @Override
    public void valueChanged(ListSelectionEvent e)
    {
        if (!e.getValueIsAdjusting())
        {
            int row = mBroadcastConfigurationTable.getSelectedRow();
            int modelRow = mBroadcastConfigurationTable.convertRowIndexToModel(row);

            BroadcastConfiguration selectedConfiguration = mBroadcastModel.getConfigurationAt(modelRow);

            if (mEditor.isModified())
            {
                int option = JOptionPane.showConfirmDialog(
                        BroadcastPanel.this,
                        "Configuration settings have changed.  Do you want to save these changes?",
                        "Save Changes?",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE);

                if (option == JOptionPane.YES_OPTION)
                {
                    mEditor.save();
                }
            }

            mSplitPane.removePane(mEditor);

            if (selectedConfiguration != null)
            {
                mEditor = BroadcastFactory.getEditor(selectedConfiguration, mBroadcastModel);
            }
            else
            {
                mEditor = mEmptyEditor;
            }

            mSplitPane.add(mEditor);

            revalidate();
        }
    }

    public class AddBroadcastConfigurationItem extends JMenuItem
    {
        private static final long serialVersionUID = 1L;

        private BroadcastServerType mBroadcastServerType;

        public AddBroadcastConfigurationItem(BroadcastServerType type)
        {
            super(type.toString());

            mBroadcastServerType = type;

            addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    final BroadcastConfiguration configuration =
                            BroadcastFactory.getConfiguration(mBroadcastServerType, BroadcastFormat.MP3);

                    mBroadcastModel.addBroadcastConfiguration(configuration);

                    int modelRow = mBroadcastModel.getRowForConfiguration(configuration);
                    int tableRow = mBroadcastConfigurationTable.convertRowIndexToView(modelRow);
                    mBroadcastConfigurationTable.changeSelection(tableRow, 0, false, false);}
            });
        }
    }

}
