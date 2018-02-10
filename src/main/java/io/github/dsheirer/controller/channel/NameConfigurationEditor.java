/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014-2016 Dennis Sheirer
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package io.github.dsheirer.controller.channel;

import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.gui.editor.Editor;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.List;

public class NameConfigurationEditor extends Editor<Channel> implements DocumentListener
{
    private static final long serialVersionUID = 1L;

    private final static Logger mLog = LoggerFactory.getLogger(NameConfigurationEditor.class);

    private static ComboBoxModel<String> EMPTY_SYSTEM_MODEL = new DefaultComboBoxModel<>();
    private static ComboBoxModel<String> EMPTY_SITE_MODEL = new DefaultComboBoxModel<>();
    private static ComboBoxModel<String> EMPTY_ALIAS_LIST_MODEL = new DefaultComboBoxModel<>();

    private AliasModel mAliasModel;
    private ChannelModel mChannelModel;

    private JTextField mChannelName;
    private JComboBox<String> mSystemNameCombo;
    private JComboBox<String> mSiteNameCombo;
    private JComboBox<String> mAliasListCombo;
    private JCheckBox mAutoStartCheckBox;
    private JSpinner mAutoStartOrder;

    public NameConfigurationEditor(AliasModel aliasModel, ChannelModel model)
    {
        mAliasModel = aliasModel;
        mChannelModel = model;

        init();
    }

    private void init()
    {
        setLayout(new MigLayout("fill,wrap 4", "[right][grow,fill][right][grow,fill]", "[][][grow]"));

        mChannelName = new JTextField("");
        mChannelName.getDocument().addDocumentListener(this);
        mChannelName.setEnabled(false);

        add(new JLabel("Name:"));
        add(mChannelName);

        mSystemNameCombo = new JComboBox<>(EMPTY_SYSTEM_MODEL);
        mSystemNameCombo.setEnabled(false);
        mSystemNameCombo.setEditable(true);
        mSystemNameCombo.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                Object selected = mSystemNameCombo.getSelectedItem();

                if(selected != null)
                {
                    String system = (String)selected;

                    List<String> sites = mChannelModel.getSites(system);

                    if(!sites.contains(getItem().getSite()))
                    {
                        sites.add(getItem().getSite());
                    }

                    if(sites.isEmpty())
                    {
                        mSiteNameCombo.setModel(EMPTY_SITE_MODEL);
                    }
                    else
                    {
                        mSiteNameCombo.setModel(new DefaultComboBoxModel<String>(
                            sites.toArray(new String[sites.size()])));
                        ;
                    }

                    mSiteNameCombo.setSelectedItem(getItem().getSite());
                }

                setModified(true);
            }
        });

        add(new JLabel("System:"));
        add(mSystemNameCombo);

        mAliasListCombo = new JComboBox<String>(EMPTY_ALIAS_LIST_MODEL);
        mAliasListCombo.setEnabled(false);
        mAliasListCombo.setEditable(true);
        mAliasListCombo.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                setModified(true);
            }
        });

        add(new JLabel("Alias List:"));
        add(mAliasListCombo);

        mSiteNameCombo = new JComboBox<>(EMPTY_SITE_MODEL);
        mSiteNameCombo.setEnabled(false);
        mSiteNameCombo.setEditable(true);
        mSiteNameCombo.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                setModified(true);
            }
        });

        add(new JLabel("Site:"));
        add(mSiteNameCombo);

        JPanel autoStartPanel = new JPanel();

        mAutoStartCheckBox = new JCheckBox("Auto-Start");
        mAutoStartCheckBox.setToolTipText("Automatically start this channel each time the application runs.");
        mAutoStartCheckBox.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                mAutoStartOrder.setEnabled(mAutoStartCheckBox.isSelected());
                setModified(true);
            }
        });
        autoStartPanel.add(mAutoStartCheckBox);

        SpinnerModel model = new SpinnerNumberModel(0,0,500,1);
        mAutoStartOrder = new JSpinner(model);
        mAutoStartOrder.setEnabled(false);
        mAutoStartOrder.setPreferredSize(new Dimension(60, mAutoStartOrder.getPreferredSize().height));
        mAutoStartOrder.setToolTipText("Auto-start order: 1 (high) <> 500 (low). 0 = no ordering");
        mAutoStartOrder.addChangeListener(new ChangeListener()
        {
            @Override
            public void stateChanged(ChangeEvent e)
            {
                setModified(true);
            }
        });

        autoStartPanel.add(new JLabel("Order:"));
        autoStartPanel.add(mAutoStartOrder);
        add(new JLabel()); //Empty space holder
        add(autoStartPanel);
    }

    @Override
    public void save()
    {
        if(hasItem() && isModified())
        {
            getItem().setName(mChannelName.getText());

            Object system = mSystemNameCombo.getSelectedItem();
            getItem().setSystem((system == null ? null : (String)system));

            Object site = mSiteNameCombo.getSelectedItem();
            getItem().setSite((site == null ? null : (String)site));

            Object aliasList = mAliasListCombo.getSelectedItem();
            getItem().setAliasListName(aliasList == null ? null : (String)aliasList);

            getItem().setAutoStart(mAutoStartCheckBox.isSelected());

            int order = ((Number)mAutoStartOrder.getValue()).intValue();
            getItem().setAutoStartOrder((order > 0 ? order : null));
        }

        setModified(false);
    }

    private void setControlsEnabled(boolean enabled)
    {
        if(mChannelName.isEnabled() != enabled)
        {
            mChannelName.setEnabled(enabled);
        }

        if(mAliasListCombo.isEnabled() != enabled)
        {
            mAliasListCombo.setEnabled(enabled);
        }

        if(mSystemNameCombo.isEnabled() != enabled)
        {
            mSystemNameCombo.setEnabled(enabled);
        }

        if(mSiteNameCombo.isEnabled() != enabled)
        {
            mSiteNameCombo.setEnabled(enabled);
        }

        if(mAutoStartCheckBox.isEnabled() != enabled)
        {
            mAutoStartCheckBox.setEnabled(enabled);
        }

        if(mAutoStartOrder.isEnabled() != enabled)
        {
            mAutoStartOrder.setEnabled(enabled);
        }
    }

    @Override
    public void setItem(Channel channel)
    {
        super.setItem(channel);

        if(hasItem())
        {
            setControlsEnabled(true);

            mChannelName.setText(channel.getName());

            List<String> systems = mChannelModel.getSystems();

            if(systems.isEmpty())
            {
                mSystemNameCombo.setModel(EMPTY_SYSTEM_MODEL);
            }
            else
            {
                mSystemNameCombo.setModel(new DefaultComboBoxModel<String>(
                    systems.toArray(new String[systems.size()])));
                ;
            }

            mSystemNameCombo.setSelectedItem(channel.getSystem());

            List<String> sites = mChannelModel.getSites(channel.getSystem());

            if(sites.isEmpty())
            {
                mSiteNameCombo.setModel(EMPTY_SITE_MODEL);
            }
            else
            {
                mSiteNameCombo.setModel(new DefaultComboBoxModel<String>(
                    sites.toArray(new String[sites.size()])));
                ;
            }

            if(channel.getSite() != null)
            {
                mSiteNameCombo.setSelectedItem(channel.getSite());
            }

            List<String> lists = mAliasModel.getListNames();
            Collections.sort(lists);
            mAliasListCombo.setModel(new DefaultComboBoxModel<String>(
                lists.toArray(new String[lists.size()])));

            if(channel.getAliasListName() != null)
            {
                mAliasListCombo.setSelectedItem(channel.getAliasListName());
            }
        }
        else
        {
            setControlsEnabled(false);
            mChannelName.setText(null);
        }

        if(channel.isAutoStart())
        {
            mAutoStartCheckBox.setSelected(channel.isAutoStart());
            mAutoStartOrder.setEnabled(true);
        }
        else
        {
            mAutoStartCheckBox.setSelected(false);
            mAutoStartOrder.setEnabled(false);
        }

        mAutoStartOrder.setValue(channel.hasAutoStartOrder() ? channel.getAutoStartOrder() : 0);

        setModified(false);
    }

    @Override
    public void insertUpdate(DocumentEvent e)
    {
        setModified(true);
    }

    @Override
    public void changedUpdate(DocumentEvent e)
    {
        setModified(true);
    }

    @Override
    public void removeUpdate(DocumentEvent e)
    {
        setModified(true);
    }
}
