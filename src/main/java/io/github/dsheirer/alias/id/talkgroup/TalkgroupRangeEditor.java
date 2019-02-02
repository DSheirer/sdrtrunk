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
import javax.swing.text.MaskFormatter;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.ParseException;

public class TalkgroupRangeEditor extends DocumentListenerEditor<AliasID>
{
    private final static Logger mLog = LoggerFactory.getLogger(TalkgroupRangeEditor.class);
    private static final long serialVersionUID = 1L;

    private static final String VALID_CHARACTERS_FOR_ASTERISK_MASKS = "0123456789 ";
    private MaskFormatter mMaskFormatter = new MaskFormatter();
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
                Protocol protocol = (Protocol)mComboProtocol.getSelectedItem();
                updateEditor(protocol);
                setModified(true);
            }
        });

        add(mComboProtocol);

        add(new JLabel("Min:"));

        mMinTalkgroupField = new JFormattedTextField(mMaskFormatter);
        mMinTalkgroupField.getDocument().addDocumentListener(this);
        add(mMinTalkgroupField, "growx,push");

        add(new JLabel("Max:"));

        mMaxTalkgroupField = new JFormattedTextField(mMaskFormatter);
        mMaxTalkgroupField.getDocument().addDocumentListener(this);
        add(mMaxTalkgroupField, "growx,push");

        JLabel help = new JLabel("Help ...");
        help.setForeground(Color.BLUE.brighter());
        help.setCursor(new Cursor(Cursor.HAND_CURSOR));
        help.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                TalkgroupFormat talkgroupFormat = TalkgroupFormat.get(getCurrentProtocol());
                JOptionPane.showMessageDialog(TalkgroupRangeEditor.this,
                    talkgroupFormat.getValidRangeHelpText(), "Help", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        add(help, "align left");
    }

    private void updateEditor(Protocol protocol)
    {
        TalkgroupFormat mask = TalkgroupFormat.get(protocol);
        int currentMinValue = 0;
        int currentMaxValue = 0;
        TalkgroupRange talkgroupRange = getTalkgroupRange();

        if(talkgroupRange != null)
        {
            currentMinValue = talkgroupRange.getMinTalkgroup();
            currentMaxValue = talkgroupRange.getMaxTalkgroup();
        }

        try
        {
            mMinTalkgroupField.setValue(null);
            mMaxTalkgroupField.setValue(null);

            mMinTalkgroupField.setToolTipText(mask.getValidRangeHelpText());
            mMaxTalkgroupField.setToolTipText(mask.getValidRangeHelpText());
            mMaskFormatter.setMask(mask.getMask());
            if(mask.getMask().contains("*"))
            {
                mMaskFormatter.setValidCharacters(VALID_CHARACTERS_FOR_ASTERISK_MASKS);
            }

            mMinTalkgroupField.setValue(TalkgroupFormatter.format(protocol, currentMinValue));
            mMaxTalkgroupField.setValue(TalkgroupFormatter.format(protocol, currentMaxValue));
        }
        catch(ParseException pe)
        {
            mLog.error("Error applying talkgroup editor mask to mask formatter [" + mask.getMask() + "]");
        }
    }

    private Protocol getCurrentProtocol()
    {
        return mComboProtocol.getItemAt(mComboProtocol.getSelectedIndex());
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

        updateEditor(talkgroupRange.getProtocol());

        if(talkgroupRange != null)
        {
            mComboProtocol.getModel().setSelectedItem(talkgroupRange.getProtocol());
            String minFormatted = TalkgroupFormatter.format(talkgroupRange.getProtocol(), talkgroupRange.getMinTalkgroup());
            mMinTalkgroupField.setValue(minFormatted);
            String maxFormatted = TalkgroupFormatter.format(talkgroupRange.getProtocol(), talkgroupRange.getMaxTalkgroup());
            mMaxTalkgroupField.setValue(maxFormatted);
        }
        else
        {
            mComboProtocol.getModel().setSelectedItem(Protocol.UNKNOWN);
            mMinTalkgroupField.setValue(TalkgroupFormatter.format(getCurrentProtocol(), 0));
            mMaxTalkgroupField.setValue(TalkgroupFormatter.format(getCurrentProtocol(), 0));
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
            Protocol protocol = mComboProtocol.getItemAt(mComboProtocol.getSelectedIndex());

            if(protocol == null)
            {
                protocol = Protocol.UNKNOWN;
            }

            talkgroupRange.setProtocol(protocol);

            int minValue = -1;
            int maxValue = -1;

            try
            {
                minValue = TalkgroupFormatter.parse(protocol, mMinTalkgroupField.getText());
            }
            catch(ParseException pe)
            {
                //ignore ... value is still -1 and outside valid value range
            }

            try
            {
                maxValue = TalkgroupFormatter.parse(protocol, mMaxTalkgroupField.getText());
            }
            catch(ParseException pe)
            {
                //ignore ... value is still -1 and outside valid value range
            }

            TalkgroupFormat mask = TalkgroupFormat.get(protocol);

            //Check for valid value within range ... notify user but allow value to persist
            if(minValue < mask.getMinimumValidValue() || minValue > mask.getMaximumValidValue())
            {
                String message = "Invalid minimum value [" + mMinTalkgroupField.getText() + "].  " + protocol.name() +
                    " valid range is [" + mask.getValidRangeDescription() + "]";

                JOptionPane.showMessageDialog(TalkgroupRangeEditor.this, message,
                    "Invalid Talkgroup Value", JOptionPane.ERROR_MESSAGE);
            }
            else
            {
                talkgroupRange.setMinTalkgroup(minValue);
            }

            //Check for valid value within range ... notify user but allow value to persist
            if(maxValue < mask.getMinimumValidValue() || maxValue > mask.getMaximumValidValue())
            {
                String message = "Invalid maximum value [" + mMaxTalkgroupField.getText() + "].  " + protocol.name() +
                    " valid range is [" + mask.getValidRangeDescription() + "]";

                JOptionPane.showMessageDialog(TalkgroupRangeEditor.this, message,
                    "Invalid Talkgroup Value", JOptionPane.ERROR_MESSAGE);
            }
            else
            {
                talkgroupRange.setMaxTalkgroup(maxValue);
            }

            talkgroupRange.setProtocol(protocol);
        }

        setModified(false);
    }
}
