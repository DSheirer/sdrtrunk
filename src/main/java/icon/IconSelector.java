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

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class IconSelector extends JDialog implements DocumentListener
{
    private IconTableModel mIconTableModel;
    private JTextField mNameText;
    private JTextField mFileLabel;
    private JButton mFileChooserButton;
    private JButton mOKButton;
    private JButton mCancelButton;

    /**
     * Window for selecting an Icon to add to the Icon Model
     *
     * @param owner for this modal dialog
     * @param iconTableModel to receive the added/selected icon
     */
    public IconSelector(Frame owner, IconTableModel iconTableModel)
    {
        super(owner, "Add Icon", true);

        mIconTableModel = iconTableModel;

        init();

        setLocationRelativeTo(owner);
    }

    private void init()
    {
        setSize(250, 170);

        JPanel contentPanel = new JPanel();
        contentPanel.setPreferredSize(new Dimension(250, 170));
        contentPanel.setLayout(new MigLayout("insets 1 1 1 1", "[align right][align left,grow,fill]", "[][][][grow,fill][]"));

        contentPanel.add(new JLabel("Name"));

        mNameText = new JTextField();
        mNameText.getDocument().addDocumentListener(this);
        contentPanel.add(mNameText, "wrap");

        contentPanel.add(new JLabel("File"));

        mFileLabel = new JTextField();
        mFileLabel.setEnabled(false);
        contentPanel.add(mFileLabel, "wrap");

        mFileChooserButton = new JButton("Select File ...");
        mFileChooserButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                JFileChooser fileChooser = new JFileChooser();

//                fileChooser.setSelectedFile(null);

                //Add a custom file filter and disable the default
                fileChooser.addChoosableFileFilter(new ImageFilter());
                fileChooser.setAcceptAllFileFilterUsed(false);

                int returnVal = fileChooser.showDialog(IconSelector.this, "Select");

                if(returnVal == JFileChooser.APPROVE_OPTION)
                {
                    File file = fileChooser.getSelectedFile();

                    if(file != null)
                    {
                        mFileLabel.setText(file.getAbsolutePath());
                        checkButtons();
                    }
                }
            }
        });
        contentPanel.add(mFileChooserButton, "span,align center");

        contentPanel.add(new JLabel(), "wrap");

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new MigLayout("", "[grow,fill][grow,fill]", "[]"));

        mOKButton = new JButton("OK");
        mOKButton.setEnabled(false);
        mOKButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                String name = mNameText.getText();
                String path = mFileLabel.getText();

                Icon icon = new Icon(name, path);
                mIconTableModel.add(icon);

                IconSelector.this.dispose();
            }
        });

        mCancelButton = new JButton("Cancel");
        mCancelButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                IconSelector.this.dispose();
            }
        });

        buttonPanel.add(mOKButton);
        buttonPanel.add(mCancelButton);

        contentPanel.add(buttonPanel, "span,grow");

        add(contentPanel);
    }

    private void checkButtons()
    {
        String name = mNameText.getText();
        String path = mFileLabel.getText();

        mOKButton.setEnabled(name != null && path != null && !path.isEmpty() && !mIconTableModel.hasIcon(name));
    }

    @Override
    public void insertUpdate(DocumentEvent e)
    {
        checkButtons();
    }

    @Override
    public void removeUpdate(DocumentEvent e)
    {
        checkButtons();
    }

    @Override
    public void changedUpdate(DocumentEvent e)
    {
        checkButtons();
    }
}

