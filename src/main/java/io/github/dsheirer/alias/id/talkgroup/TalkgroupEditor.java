/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
package io.github.dsheirer.alias.id.talkgroup;

import io.github.dsheirer.alias.id.AliasID;
import io.github.dsheirer.gui.editor.DocumentListenerEditor;
import io.github.dsheirer.protocol.Protocol;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class TalkgroupEditor extends DocumentListenerEditor<AliasID>
{
    private final static Logger mLog = LoggerFactory.getLogger(TalkgroupEditor.class);

    private static final long serialVersionUID = 1L;

    private static final String HELP_TEXT =
        "<html><h3>Talkgroup Identifier</h3></html>";

    private JComboBox<Protocol> mComboProtocol;
    private JFormattedTextField mTalkgroupField;

    public TalkgroupEditor(AliasID aliasID)
    {
        initGUI();
        setItem(aliasID);
    }

    private void initGUI()
    {
        setLayout(new MigLayout("fill,wrap 2", "[right][left]", "[][]"));

        add(new JLabel("Protocol:"));

        mComboProtocol = new JComboBox<Protocol>();

        DefaultComboBoxModel<Protocol> model = new DefaultComboBoxModel<>();

        for(Protocol protocol : Protocol.TALKGROUP_PROTOCOLS)
        {
            model.addElement(protocol);
        }

        mComboProtocol.setModel(model);
        mComboProtocol.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                Protocol selected = mComboProtocol.getItemAt(mComboProtocol.getSelectedIndex());
                setModified(true);
//                ValidatingEditor<Channel> editor = DecoderFactory.getEditor(selected, mChannelMapModel);
//                setEditor(editor);
            }
        });

        add(mComboProtocol);

        add(new JLabel("Value:"));

        mTalkgroupField = new JFormattedTextField();
        mTalkgroupField.setColumns(10);
        mTalkgroupField.setValue(new Integer(0));
        mTalkgroupField.getDocument().addDocumentListener(this);
        mTalkgroupField.setToolTipText(HELP_TEXT);
        add(mTalkgroupField, "growx,push");

        JLabel help = new JLabel("Help ...");
        help.setForeground(Color.BLUE.brighter());
        help.setCursor(new Cursor(Cursor.HAND_CURSOR));
        help.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                JOptionPane.showMessageDialog(TalkgroupEditor.this,
                    HELP_TEXT, "Help", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        add(help, "align left");
    }

    public Talkgroup getTalkgroup()
    {
        if(getItem() instanceof Talkgroup)
        {
            return (Talkgroup)getItem();
        }

        return null;
    }

    @Override
    public void setItem(AliasID aliasID)
    {
        super.setItem(aliasID);

        Talkgroup talkgroup = getTalkgroup();

        if(talkgroup != null)
        {
            mComboProtocol.setSelectedItem(talkgroup.getProtocol());
            mTalkgroupField.setValue(new Integer(talkgroup.getValue()));
        }

        setModified(false);

        repaint();
    }

    @Override
    public void save()
    {
        Talkgroup talkgroup = getTalkgroup();

        if(talkgroup != null)
        {
            Protocol selected = mComboProtocol.getItemAt(mComboProtocol.getSelectedIndex());
            talkgroup.setProtocol(selected);

            try
            {
                int value = ((Number)mTalkgroupField.getValue()).intValue();
                talkgroup.setValue(value);
            }
            catch(Exception e)
            {
                mLog.error("Error parsing minimum talkgroup value from [" + mTalkgroupField.getValue() + "]");
            }
        }

        setModified(false);
    }
}
