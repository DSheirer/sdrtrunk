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

import alias.AliasModel;
import com.jidesoft.swing.JideSplitPane;
import gui.editor.Editor;
import gui.editor.EmptyEditor;
import icon.IconManager;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import settings.SettingsManager;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class BroadcastPanel extends JPanel implements ActionListener, ListSelectionListener
{
    private final static Logger mLog = LoggerFactory.getLogger( BroadcastPanel.class );

    private static final String NEW_BROADCAST_CONFIGURATION = "New";
    private static final String COPY_BROADCAST_CONFIGURATION = "Copy";
    private static final String DELETE_BROADCAST_CONFIGURATION = "Delete";

    private BroadcastModel mBroadcastModel;
    private AliasModel mAliasModel;
    private IconManager mIconManager;

    private BroadcastStatusPanel mBroadcastStatusPanel;
    private JideSplitPane mSplitPane;
    private Editor<BroadcastConfiguration> mEmptyEditor = new EmptyEditor<>("Configuration:");
    private Editor<BroadcastConfiguration> mEditor;
    private JButton mNewButton = new JButton(NEW_BROADCAST_CONFIGURATION);
    private JButton mCopyButton = new JButton(COPY_BROADCAST_CONFIGURATION);
    private JButton mDeleteButton = new JButton(DELETE_BROADCAST_CONFIGURATION);

    public BroadcastPanel(BroadcastModel broadcastModel, AliasModel aliasModel, IconManager iconManager)
    {
        mBroadcastModel = broadcastModel;
        mAliasModel = aliasModel;
        mIconManager = iconManager;
        mEditor = mEmptyEditor;

        init();
    }

    private void init()
    {
        setLayout(new MigLayout("insets 0 0 0 0 ", "[grow,fill]", "[grow,fill]"));

        mBroadcastStatusPanel = new BroadcastStatusPanel(mBroadcastModel);

        mBroadcastStatusPanel.getTable().setAutoCreateRowSorter(true);
        mBroadcastStatusPanel.getTable().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        mBroadcastStatusPanel.getTable().getSelectionModel().addListSelectionListener(BroadcastPanel.this);

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new MigLayout("insets 0 0 0 0", "[grow,fill][grow,fill][grow,fill]", "[]"));

        mNewButton = new JButton("New ...");
        mNewButton.setToolTipText("Create a new broadcast audio configuration");
        mNewButton.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                JPopupMenu menu = new JPopupMenu();

                menu.add(new AddBroadcastConfigurationItem(BroadcastServerType.BROADCASTIFY));
                menu.add(new AddBroadcastConfigurationItem(BroadcastServerType.ICECAST_TCP));
                menu.add(new AddBroadcastConfigurationItem(BroadcastServerType.ICECAST_HTTP));
                menu.add(new AddBroadcastConfigurationItem(BroadcastServerType.SHOUTCAST_V1));
                menu.add(new AddBroadcastConfigurationItem(BroadcastServerType.SHOUTCAST_V2));

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

        listAndButtonsPanel.add(mBroadcastStatusPanel, "wrap");
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
        switch(e.getActionCommand())
        {
            case COPY_BROADCAST_CONFIGURATION:
                BroadcastConfiguration copySelected = getSelectedBroadcastConfiguration();
                mBroadcastModel.cloneBroadcastConfiguration(copySelected);
                break;
            case DELETE_BROADCAST_CONFIGURATION:
                BroadcastConfiguration deleteSelected = getSelectedBroadcastConfiguration();

                if(deleteSelected != null)
                {
                    int choice = JOptionPane.showConfirmDialog(BroadcastPanel.this, "Do you want to delete broadcast\n" +
                            " configuration " + deleteSelected.getName() + "?", "Delete Broadcast Configuration?",
                            JOptionPane.YES_NO_OPTION);

                    if(choice == JOptionPane.YES_OPTION)
                    {
                        mBroadcastModel.removeBroadcastConfiguration(deleteSelected);
                    }
                }
                break;
        }

    }

    /**
     * Returns the selected configuration
     */
    private BroadcastConfiguration getSelectedBroadcastConfiguration()
    {
        int viewRow = mBroadcastStatusPanel.getTable().getSelectedRow();

        if(viewRow >= 0)
        {
            int modelRow = mBroadcastStatusPanel.getTable().convertRowIndexToModel(viewRow);

            if(modelRow >= 0)
            {
                return mBroadcastModel.getConfigurationAt(modelRow);
            }
        }

        return null;
    }


    @Override
    public void valueChanged(ListSelectionEvent e)
    {
        if (!e.getValueIsAdjusting())
        {
            BroadcastConfiguration selectedConfiguration = getSelectedBroadcastConfiguration();

            //Enable buttons accordingly
            mCopyButton.setEnabled(selectedConfiguration != null);
            mDeleteButton.setEnabled(selectedConfiguration != null);

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
                mEditor = BroadcastFactory.getEditor(selectedConfiguration, mBroadcastModel, mAliasModel,
                    mIconManager);
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
            super(type.toString(), mIconManager.getScaledIcon(new ImageIcon(type.getIconPath()), 14));

            mBroadcastServerType = type;

            setToolTipText("Add a new " + type.toString() + " audio stream configuration");

            addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    final BroadcastConfiguration configuration =
                            BroadcastFactory.getConfiguration(mBroadcastServerType, BroadcastFormat.MP3);

                    if(configuration != null)
                    {
                        mBroadcastModel.addBroadcastConfiguration(configuration);

                        int modelRow = mBroadcastModel.getRowForConfiguration(configuration);
                        int tableRow = mBroadcastStatusPanel.getTable().convertRowIndexToView(modelRow);
                        mBroadcastStatusPanel.getTable().changeSelection(tableRow, 0, false, false);
                    }
                }
            });
        }
    }

}
