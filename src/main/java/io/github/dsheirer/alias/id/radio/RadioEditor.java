/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */
package io.github.dsheirer.alias.id.radio;

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

public class RadioEditor extends DocumentListenerEditor<AliasID>
{
    private final static Logger mLog = LoggerFactory.getLogger(RadioEditor.class);
    private static final long serialVersionUID = 1L;

    private static final String VALID_CHARACTERS_FOR_ASTERISK_MASKS = "0123456789 ";
    private MaskFormatter mMaskFormatter = new MaskFormatter();
    private JComboBox<Protocol> mComboProtocol;
    private JFormattedTextField mRadioIdField;


    public RadioEditor(AliasID aliasID)
    {
        initGUI();
        setItem(aliasID);
    }

    private void initGUI()
    {
        setLayout(new MigLayout("fill,wrap 2", "[right][left]", "[][]"));

        add(new JLabel("Protocol:"));

        mComboProtocol = new JComboBox<>();

        DefaultComboBoxModel<Protocol> model = new DefaultComboBoxModel<>();

        for(Protocol protocol : Protocol.RADIO_ID_PROTOCOLS)
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

        add(new JLabel("Value:"));

        mRadioIdField = new JFormattedTextField(mMaskFormatter);
        mRadioIdField.getDocument().addDocumentListener(this);
        add(mRadioIdField, "growx,push");

        JLabel help = new JLabel("Help ...");
        help.setForeground(Color.BLUE.brighter());
        help.setCursor(new Cursor(Cursor.HAND_CURSOR));
        help.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                RadioFormat radioFormat = RadioFormat.get(getCurrentProtocol());
                JOptionPane.showMessageDialog(RadioEditor.this,
                    radioFormat.getValidRangeHelpText(), "Help", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        add(help, "align left");
    }

    private void updateEditor(Protocol protocol)
    {
        RadioFormat mask = RadioFormat.get(protocol);
        int currentValue = 0;
        Radio radio = getRadio();

        if(radio != null)
        {
            currentValue = radio.getValue();
        }

        try
        {
            mRadioIdField.setValue(null);
            mRadioIdField.setToolTipText(mask.getValidRangeHelpText());
            mMaskFormatter.setMask(mask.getMask());
            if(mask.getMask().contains("*"))
            {
                mMaskFormatter.setValidCharacters(VALID_CHARACTERS_FOR_ASTERISK_MASKS);
            }
            mRadioIdField.setValue(RadioFormatter.format(protocol, currentValue));
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

    /**
     * Current talkgroup alias id
     */
    public Radio getRadio()
    {
        if(getItem() instanceof Radio)
        {
            return (Radio)getItem();
        }

        return null;
    }

    @Override
    public void setItem(AliasID aliasID)
    {
        super.setItem(aliasID);

        Radio radio = getRadio();

        if(radio != null)
        {
            Protocol currentProtocol = getCurrentProtocol();
            if(currentProtocol == radio.getProtocol())
            {
                updateEditor(radio.getProtocol());
            }
            mComboProtocol.getModel().setSelectedItem(radio.getProtocol());

            String formatted = RadioFormatter.format(radio.getProtocol(), radio.getValue());
            mRadioIdField.setValue(formatted);
        }
        else
        {
            mComboProtocol.getModel().setSelectedItem(Protocol.UNKNOWN);
            mRadioIdField.setValue(RadioFormatter.format(getCurrentProtocol(), 0));
        }

        setModified(false);

        repaint();
    }

    @Override
    public void save()
    {
        Radio radio = getRadio();

        if(radio != null)
        {
            Protocol protocol = mComboProtocol.getItemAt(mComboProtocol.getSelectedIndex());

            if(protocol == null)
            {
                protocol = Protocol.UNKNOWN;
            }

            int value = -1;

            try
            {
                value = RadioFormatter.parse(protocol, mRadioIdField.getText());
            }
            catch(ParseException pe)
            {
                //ignore ... value is still -1 and outside valid value range
            }

            RadioFormat mask = RadioFormat.get(protocol);

            //Check for valid value within range ... notify user but allow value to persist
            if(value < mask.getMinimumValidValue() || value > mask.getMaximumValidValue())
            {
                String message = "Invalid value [" + mRadioIdField.getText() + "].  " + protocol.name() +
                    " valid range is [" + mask.getValidRangeDescription() + "]";

                JOptionPane.showMessageDialog(RadioEditor.this, message, "Invalid Radio ID Value",
                    JOptionPane.ERROR_MESSAGE);
            }
            else
            {
                radio.setValue(value);
            }

            radio.setProtocol(protocol);
        }

        setModified(false);
    }
}
