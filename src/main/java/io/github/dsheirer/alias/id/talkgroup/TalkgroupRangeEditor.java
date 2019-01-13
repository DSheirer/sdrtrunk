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

public class TalkgroupRangeEditor extends DocumentListenerEditor<AliasID>
{
    private final static Logger mLog = LoggerFactory.getLogger(TalkgroupRangeEditor.class);
    private static final long serialVersionUID = 1L;

    private static final String HELP_TEXT =
        "<html><h3>Talkgroup Range Identifier</h3>Identifies a range of talkgroup values</html>";

    private JComboBox<Protocol> mComboProtocol;
    private JFormattedTextField mMinTalkgroupField;
    private JFormattedTextField mMaxTalkgroupField;

    public TalkgroupRangeEditor(AliasID aliasID)
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

        add(new JLabel("Min:"));

        mMinTalkgroupField = new JFormattedTextField();
        mMinTalkgroupField.setColumns(10);
        mMinTalkgroupField.setValue(0);
        mMinTalkgroupField.getDocument().addDocumentListener(this);
        mMinTalkgroupField.setToolTipText(HELP_TEXT);
        add(mMinTalkgroupField, "growx,push");

        add(new JLabel("Max:"));

        mMaxTalkgroupField = new JFormattedTextField();
        mMaxTalkgroupField.setColumns(10);
        mMaxTalkgroupField.setValue(0);
        mMaxTalkgroupField.getDocument().addDocumentListener(this);
        mMaxTalkgroupField.setToolTipText(HELP_TEXT);
        add(mMaxTalkgroupField, "growx,push");

        JLabel help = new JLabel("Help ...");
        help.setForeground(Color.BLUE.brighter());
        help.setCursor(new Cursor(Cursor.HAND_CURSOR));
        help.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                JOptionPane.showMessageDialog(TalkgroupRangeEditor.this,
                    HELP_TEXT, "Help", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        add(help, "align left");
    }

    public TalkgroupRange getTalkgroupRange()
    {
        if(getItem() instanceof TalkgroupRange)
        {
            return (TalkgroupRange)getItem();
        }

        return null;
    }

    @Override
    public void setItem(AliasID aliasID)
    {
        super.setItem(aliasID);

        TalkgroupRange talkgroupRange = getTalkgroupRange();

        if(talkgroupRange != null)
        {
            mComboProtocol.setSelectedItem(talkgroupRange.getProtocol());
            mMinTalkgroupField.setValue(talkgroupRange.getMinTalkgroup());
            mMaxTalkgroupField.setValue(talkgroupRange.getMaxTalkgroup());
        }

        setModified(false);

        repaint();
    }

    @Override
    public void save()
    {
        TalkgroupRange talkgroupRange = getTalkgroupRange();

        if(talkgroupRange != null)
        {
            Protocol selected = mComboProtocol.getItemAt(mComboProtocol.getSelectedIndex());
            talkgroupRange.setProtocol(selected);

            try
            {
                int minTalkgroup = ((Number)mMinTalkgroupField.getValue()).intValue();
                talkgroupRange.setMinTalkgroup(minTalkgroup);
            }
            catch(Exception e)
            {
                mLog.error("Error parsing minimum talkgroup value from [" + mMinTalkgroupField.getText() + "]");
            }

            try
            {
                int maxTalkgroup = ((Number)mMaxTalkgroupField.getValue()).intValue();
                talkgroupRange.setMaxTalkgroup(maxTalkgroup);
            }
            catch(Exception e)
            {
                mLog.error("Error parsing maximum talkgroup value from [" + mMaxTalkgroupField.getText() + "]");
            }
        }

        setModified(false);
    }
}
