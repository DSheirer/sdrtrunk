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

@Deprecated
public class RadioRangeEditor extends DocumentListenerEditor<AliasID>
{
    private final static Logger mLog = LoggerFactory.getLogger(RadioRangeEditor.class);
    private static final long serialVersionUID = 1L;

    private static final String VALID_CHARACTERS_FOR_ASTERISK_MASKS = "0123456789 ";
    private MaskFormatter mMaskFormatter = new MaskFormatter();
    private JComboBox<Protocol> mComboProtocol;
    private JFormattedTextField mMinRadioField;
    private JFormattedTextField mMaxRadioField;

    public RadioRangeEditor(AliasID aliasID)
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

        add(new JLabel("Min:"));

        mMinRadioField = new JFormattedTextField(mMaskFormatter);
        mMinRadioField.getDocument().addDocumentListener(this);
        add(mMinRadioField, "growx,push");

        add(new JLabel("Max:"));

        mMaxRadioField = new JFormattedTextField(mMaskFormatter);
        mMaxRadioField.getDocument().addDocumentListener(this);
        add(mMaxRadioField, "growx,push");

        JLabel help = new JLabel("Help ...");
        help.setForeground(Color.BLUE.brighter());
        help.setCursor(new Cursor(Cursor.HAND_CURSOR));
        help.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                RadioFormat radioFormat = RadioFormat.get(getCurrentProtocol());
                JOptionPane.showMessageDialog(RadioRangeEditor.this,
                    radioFormat.getValidRangeHelpText(), "Help", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        add(help, "align left");
    }

    private void updateEditor(Protocol protocol)
    {
        RadioFormat mask = RadioFormat.get(protocol);
        int currentMinValue = 0;
        int currentMaxValue = 0;
        RadioRange radioRange = getRadioRange();

        if(radioRange != null)
        {
            currentMinValue = radioRange.getMinRadio();
            currentMaxValue = radioRange.getMaxRadio();
        }

        try
        {
            mMinRadioField.setValue(null);
            mMaxRadioField.setValue(null);

            mMinRadioField.setToolTipText(mask.getValidRangeHelpText());
            mMaxRadioField.setToolTipText(mask.getValidRangeHelpText());
            mMaskFormatter.setMask(mask.getMask());
            if(mask.getMask().contains("*"))
            {
                mMaskFormatter.setValidCharacters(VALID_CHARACTERS_FOR_ASTERISK_MASKS);
            }

            mMinRadioField.setValue(RadioFormatter.format(protocol, currentMinValue));
            mMaxRadioField.setValue(RadioFormatter.format(protocol, currentMaxValue));
        }
        catch(ParseException pe)
        {
            mLog.error("Error applying radio editor mask to mask formatter [" + mask.getMask() + "]");
        }
    }

    private Protocol getCurrentProtocol()
    {
        return mComboProtocol.getItemAt(mComboProtocol.getSelectedIndex());
    }



    public RadioRange getRadioRange()
    {
        if(getItem() instanceof RadioRange)
        {
            return (RadioRange)getItem();
        }

        return null;
    }

    @Override
    public void setItem(AliasID aliasID)
    {
        super.setItem(aliasID);

        RadioRange radioRange = getRadioRange();

        updateEditor(radioRange.getProtocol());

        if(radioRange != null)
        {
            mComboProtocol.getModel().setSelectedItem(radioRange.getProtocol());
            String minFormatted = RadioFormatter.format(radioRange.getProtocol(), radioRange.getMinRadio());
            mMinRadioField.setValue(minFormatted);
            String maxFormatted = RadioFormatter.format(radioRange.getProtocol(), radioRange.getMaxRadio());
            mMaxRadioField.setValue(maxFormatted);
        }
        else
        {
            mComboProtocol.getModel().setSelectedItem(Protocol.UNKNOWN);
            mMinRadioField.setValue(RadioFormatter.format(getCurrentProtocol(), 0));
            mMaxRadioField.setValue(RadioFormatter.format(getCurrentProtocol(), 0));
        }

        setModified(false);

        repaint();
    }

    @Override
    public void save()
    {
        RadioRange radioRange = getRadioRange();

        if(radioRange != null)
        {
            Protocol protocol = mComboProtocol.getItemAt(mComboProtocol.getSelectedIndex());

            if(protocol == null)
            {
                protocol = Protocol.UNKNOWN;
            }

            radioRange.setProtocol(protocol);

            int minValue = -1;
            int maxValue = -1;

            try
            {
                minValue = RadioFormatter.parse(protocol, mMinRadioField.getText());
            }
            catch(ParseException pe)
            {
                //ignore ... value is still -1 and outside valid value range
            }

            try
            {
                maxValue = RadioFormatter.parse(protocol, mMaxRadioField.getText());
            }
            catch(ParseException pe)
            {
                //ignore ... value is still -1 and outside valid value range
            }

            RadioFormat mask = RadioFormat.get(protocol);

            //Check for valid value within range ... notify user but allow value to persist
            if(minValue < mask.getMinimumValidValue() || minValue > mask.getMaximumValidValue())
            {
                String message = "Invalid minimum value [" + mMinRadioField.getText() + "].  " + protocol.name() +
                    " valid range is [" + mask.getValidRangeDescription() + "]";

                JOptionPane.showMessageDialog(RadioRangeEditor.this, message,
                    "Invalid Radio Value", JOptionPane.ERROR_MESSAGE);
            }
            else
            {
                radioRange.setMinRadio(minValue);
            }

            //Check for valid value within range ... notify user but allow value to persist
            if(maxValue < mask.getMinimumValidValue() || maxValue > mask.getMaximumValidValue())
            {
                String message = "Invalid maximum value [" + mMaxRadioField.getText() + "].  " + protocol.name() +
                    " valid range is [" + mask.getValidRangeDescription() + "]";

                JOptionPane.showMessageDialog(RadioRangeEditor.this, message,
                    "Invalid Radio Value", JOptionPane.ERROR_MESSAGE);
            }
            else
            {
                radioRange.setMaxRadio(maxValue);
            }

            radioRange.setProtocol(protocol);
        }

        setModified(false);
    }
}
