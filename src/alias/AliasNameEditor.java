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
package alias;

import alias.AliasEvent.Event;
import gui.editor.Editor;
import icon.Icon;
import icon.IconListCellRenderer;
import icon.IconManager;
import icon.IconTableModel;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class AliasNameEditor extends Editor<Alias>
{
    private static final long serialVersionUID = 1L;

    private static ComboBoxModel<String> EMPTY_LIST_MODEL = new DefaultComboBoxModel<>();
    private static ComboBoxModel<String> EMPTY_GROUP_MODEL = new DefaultComboBoxModel<>();

    private JComboBox<String> mListCombo = new JComboBox<>(EMPTY_LIST_MODEL);
    private JComboBox<String> mGroupCombo = new JComboBox<>(EMPTY_GROUP_MODEL);
    private JComboBox<Icon> mIconCombo;
    private JTextField mName;
    private JButton mButtonColor;
    private JButton mBtnIconManager;

    private AliasModel mAliasModel;
    private IconManager mIconManager;

    public AliasNameEditor(AliasModel aliasModel, IconManager iconManager)
    {
        mAliasModel = aliasModel;
        mIconManager = iconManager;

        mIconManager.getModel().addTableModelListener(new TableModelListener()
        {
            @Override
            public void tableChanged(TableModelEvent e)
            {
                refreshIcons();
            }
        });

        init();
    }

    private void init()
    {
        setLayout(new MigLayout("fill,wrap 2", "[right][grow,fill]", "[][][][][][][grow]"));

        add(new JLabel("Name:"));
        mName = new JTextField();
        mName.getDocument().addDocumentListener(new DocumentListener()
        {
            @Override
            public void removeUpdate(DocumentEvent e)
            {
                setModified(true);
            }

            @Override
            public void insertUpdate(DocumentEvent e)
            {
                setModified(true);
            }

            @Override
            public void changedUpdate(DocumentEvent e)
            {
            }
        });
        add(mName, "wrap");

        add(new JLabel("List:"));
        mListCombo.setEditable(true);
        mListCombo.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                if(mListCombo.getSelectedItem() != null)
                {
                    List<String> groups = mAliasModel
                        .getGroupNames((String) mListCombo.getSelectedItem());

                    if(groups.isEmpty())
                    {
                        mGroupCombo.setModel(EMPTY_GROUP_MODEL);
                    }
                    else
                    {
                        mGroupCombo.setModel(new DefaultComboBoxModel<String>(
                            groups.toArray(new String[groups.size()])));
                        ;
                    }
                }

                setModified(true);
            }
        });
        add(mListCombo, "wrap");

        add(new JLabel("Group:"));
        mGroupCombo.setEditable(true);
        mGroupCombo.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                setModified(true);
            }
        });
        add(mGroupCombo, "wrap");

        add(new JLabel("Color:"));

        mButtonColor = new JButton("Select ...");
        mButtonColor.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                Color newColor = JColorChooser.showDialog(
                    AliasNameEditor.this,
                    "Choose color for this alias",
                    (hasItem() ? getItem().getDisplayColor() : null));

                if(newColor != null)
                {
                    mButtonColor.setForeground(newColor);
                    mButtonColor.setBackground(newColor);

                    setModified(true);
                }
            }
        });
        add(mButtonColor, "wrap");

        add(new JLabel("Icon:"));

        mIconCombo = new JComboBox<Icon>(mIconManager.getIcons());

        IconListCellRenderer renderer = new IconListCellRenderer(mIconManager);
        renderer.setPreferredSize(new Dimension(200, 30));
        mIconCombo.setRenderer(renderer);
        mIconCombo.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                setModified(true);
            }
        });

        add(mIconCombo, "wrap");

        //Dummy place holder
        add(new JLabel());

        mBtnIconManager = new JButton("Icon Manager");
        mBtnIconManager.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent arg0)
            {
                mIconManager.showEditor(AliasNameEditor.this);
            }
        });

        add(mBtnIconManager, "span 2,wrap");

        setModified(false);
    }

    private void refreshIcons()
    {
        if(mIconCombo != null)
        {
            EventQueue.invokeLater(new Runnable()
            {
                @Override
                public void run()
                {
                    mIconCombo.setModel(new DefaultComboBoxModel<>(mIconManager.getIcons()));

                    if(hasItem())
                    {
                        String iconName = getItem().getIconName();
                        Icon aliasIcon = mIconManager.getModel().getIcon(iconName);

                        if(aliasIcon != null)
                        {
                            mIconCombo.setSelectedItem(aliasIcon);
                        }
                    }
                }
            });
        }
    }

    @Override
    public void setItem(Alias alias)
    {
        super.setItem(alias);

        if(hasItem())
        {
            mName.setText(alias.getName());

            mIconCombo.setModel(new DefaultComboBoxModel<>(mIconManager.getIcons()));

            List<String> listNames = mAliasModel.getListNames();

            if(listNames.isEmpty())
            {
                mListCombo.setModel(EMPTY_LIST_MODEL);
            }
            else
            {
                mListCombo.setModel(new DefaultComboBoxModel<String>(
                    listNames.toArray(new String[listNames.size()])));
                ;
            }

            mListCombo.setSelectedItem(alias.getList());

            List<String> groupNames = mAliasModel.getGroupNames(alias.getList());

            if(groupNames.isEmpty())
            {
                mGroupCombo.setModel(EMPTY_GROUP_MODEL);
            }
            else
            {
                mGroupCombo.setModel(new DefaultComboBoxModel<String>(
                    groupNames.toArray(new String[groupNames.size()])));
                ;
            }

            mGroupCombo.setSelectedItem(alias.getGroup());

            Color color = alias.getDisplayColor();

            mButtonColor.setBackground(color);
            mButtonColor.setForeground(color);

            String iconName = alias.getIconName();

            if(iconName == null)
            {
                iconName = IconTableModel.DEFAULT_ICON;
            }

            Icon savedIcon = mIconManager.getModel().getIcon(iconName);

            if(savedIcon != null)
            {
                mIconCombo.setSelectedItem(savedIcon);
            }
        }
        else
        {
            mListCombo.setModel(EMPTY_LIST_MODEL);
            mGroupCombo.setModel(EMPTY_GROUP_MODEL);
            mName.setText(null);

            mButtonColor.setBackground(getBackground());
            mButtonColor.setForeground(getForeground());
        }

        repaint();

        setModified(false);
    }

    @Override
    public void save()
    {
        if(hasItem() && isModified())
        {
            Alias alias = getItem();

            if(mListCombo.getSelectedItem() != null)
            {
                alias.setList((String) mListCombo.getSelectedItem());
            }

            if(mGroupCombo.getSelectedItem() != null)
            {
                alias.setGroup((String) mGroupCombo.getSelectedItem());
            }

            alias.setName(mName.getText());

            alias.setColor(mButtonColor.getBackground().getRGB());

            if(mIconCombo.getSelectedItem() != null)
            {
                alias.setIconName(((Icon) mIconCombo.getSelectedItem()).getName());
            }

            setModified(false);

            //Broadcast an alias change event to save the updates
            mAliasModel.broadcast(new AliasEvent(getItem(), Event.CHANGE));
        }
    }
}
