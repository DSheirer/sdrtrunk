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
package icon;

import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class IconViewer extends JFrame implements ActionListener
{
    private static final long serialVersionUID = 1L;
    private static final Logger mLog = LoggerFactory.getLogger(IconViewer.class);
    private static final String ADD = "Add";
    private static final String DEFAULT = "Set Default";
    private static final String DELETE = "Delete";

    private JButton mAddButton;
    private JButton mDeleteButton;
    private JButton mDefaultButton;
    private JList<Icon> mIconList;
    private IconManager mIconManager;

    public IconViewer(IconManager model)
    {
        mIconManager = model;

        init();
    }

    private void init()
    {
        setTitle("Icon Viewer");
        setSize(400, 400);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new MigLayout("", "[grow,fill]", "[grow,fill]"));

        /**
         * Editor Panel
         */
        JPanel editorPanel = new JPanel();
        editorPanel.setLayout(new MigLayout("insets 1 1 1 1", "[grow,fill][grow,fill][grow,fill]", "[grow,fill][]"));

        mIconList = new JList<>(mIconManager.getModel());
        IconCellRenderer renderer = new IconCellRenderer(mIconManager);
        renderer.setPreferredSize(new Dimension(200, 30));
        mIconList.setCellRenderer(renderer);
        mIconList.addListSelectionListener(new ListSelectionListener()
        {
            @Override
            public void valueChanged(ListSelectionEvent e)
            {
                checkButtons();
            }
        });
        editorPanel.add(new JScrollPane(mIconList), "span");

        mAddButton = new JButton(ADD);
        mAddButton.addActionListener(this);
        editorPanel.add(mAddButton);

        mDeleteButton = new JButton(DELETE);
        mDeleteButton.addActionListener(this);
        mDeleteButton.setEnabled(false);
        editorPanel.add(mDeleteButton);

        mDefaultButton = new JButton(DEFAULT);
        mDefaultButton.addActionListener(this);
        mDefaultButton.setEnabled(false);
        editorPanel.add(mDefaultButton, "wrap");

        add(editorPanel);
    }

    private void checkButtons()
    {
        Icon selected = mIconList.getSelectedValue();

        mDefaultButton.setEnabled(selected != null && selected != mIconManager.getModel().getDefaultIcon());
        mDeleteButton.setEnabled(selected != null && selected != mIconManager.getModel().getDefaultIcon());
    }

    public void actionPerformed(ActionEvent e)
    {
        Icon selected = mIconList.getSelectedValue();

        switch(e.getActionCommand())
        {
            case ADD:
                IconSelector iconSelector = new IconSelector(this, mIconManager.getModel());
                iconSelector.setVisible(true);
                break;
            case DELETE:
                if(selected != null)
                {
                    int choice = JOptionPane.showConfirmDialog(IconViewer.this,
                        "Are you sure you want to delete this icon?", "Delete Icon?", JOptionPane.YES_NO_OPTION);

                    if(choice == JOptionPane.YES_OPTION)
                    {
                        mIconManager.getModel().removeElement(selected);
                    }
                }
                break;
            case DEFAULT:
                if(selected != null)
                {
                    mIconManager.getModel().setDefaultIcon(selected);
                }
                break;
        }

        checkButtons();
    }
}
