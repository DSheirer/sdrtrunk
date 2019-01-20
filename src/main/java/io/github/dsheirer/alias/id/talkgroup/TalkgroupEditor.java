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
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.text.MaskFormatter;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.ParseException;

public class TalkgroupEditor extends DocumentListenerEditor<AliasID>
{
    private final static Logger mLog = LoggerFactory.getLogger(TalkgroupEditor.class);

    private static final long serialVersionUID = 1L;
    private MaskFormatter mMaskFormatter = new MaskFormatter();
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

        mComboProtocol = new JComboBox<>();

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

        add(new JLabel("Value:"));

        mTalkgroupField = new JFormattedTextField(mMaskFormatter);
        mTalkgroupField.getDocument().addDocumentListener(this);
        add(mTalkgroupField, "growx,push");

        JLabel help = new JLabel("Help ...");
        help.setForeground(Color.BLUE.brighter());
        help.setCursor(new Cursor(Cursor.HAND_CURSOR));
        help.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                TalkgroupFormat talkgroupFormat = TalkgroupFormat.get(getCurrentProtocol());
                JOptionPane.showMessageDialog(TalkgroupEditor.this,
                    talkgroupFormat.getValidRangeHelpText(), "Help", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        add(help, "align left");
    }

    private void updateEditor(Protocol protocol)
    {
        TalkgroupFormat mask = TalkgroupFormat.get(protocol);
        int currentValue = 0;
        Talkgroup talkgroup = getTalkgroup();

        if(talkgroup != null)
        {
            currentValue = talkgroup.getValue();
        }

        try
        {
            mLog.debug("Setting value to null");
            mTalkgroupField.setValue(null);
            mLog.debug("Setting tooltip");
            mTalkgroupField.setToolTipText(mask.getValidRangeHelpText());
            mLog.debug("Setting mask");
            mMaskFormatter.setMask(mask.getMask());
            mLog.debug("Setting value");
            mTalkgroupField.setValue(TalkgroupFormatter.format(protocol, currentValue));
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
            Protocol currentProtocol = getCurrentProtocol();
            if(currentProtocol == talkgroup.getProtocol())
            {
                updateEditor(talkgroup.getProtocol());
            }
            mComboProtocol.getModel().setSelectedItem(talkgroup.getProtocol());

            String formatted = TalkgroupFormatter.format(talkgroup.getProtocol(), talkgroup.getValue());
            mTalkgroupField.setValue(formatted);
        }
        else
        {
            mComboProtocol.getModel().setSelectedItem(Protocol.UNKNOWN);
            mTalkgroupField.setValue(TalkgroupFormatter.format(getCurrentProtocol(), 0));
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
            Protocol protocol = mComboProtocol.getItemAt(mComboProtocol.getSelectedIndex());

            int value = -1;

            try
            {
                value = TalkgroupFormatter.parse(protocol, mTalkgroupField.getText());
            }
            catch(ParseException pe)
            {
                //ignore ... value is still -1 and outside valid value range
            }

            TalkgroupFormat mask = TalkgroupFormat.get(protocol);

            //Check for valid value within range ... notify user but allow value to persist
            if(value < mask.getMinimumValidValue() || value > mask.getMaximumValidValue())
            {
                String message = "Invalid value [" + mTalkgroupField.getText() + "].  " + protocol.name() +
                    " valid range is [" + mask.getValidRangeDescription() + "]";

                JOptionPane.showMessageDialog(TalkgroupEditor.this, message, "Invalid Talkgroup Value",
                    JOptionPane.ERROR_MESSAGE);
            }
            else
            {
                talkgroup.setValue(value);
            }

            talkgroup.setProtocol(protocol);
        }

        setModified(false);
    }

    public static void main(String[] args)
    {
        Talkgroup talkgroup = new Talkgroup();
        talkgroup.setProtocol(Protocol.APCO25);
        talkgroup.setValue(12345678 );

        JFrame frame = new JFrame("Talkgroup Editor");
        frame.setSize(new Dimension(500, 400));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel();
        TalkgroupEditor talkgroupEditor = new TalkgroupEditor(null);
        panel.add(talkgroupEditor);
        frame.setContentPane(panel);

        EventQueue.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                frame.setVisible(true);
                talkgroupEditor.setItem(talkgroup);
            }
        });
    }
}
