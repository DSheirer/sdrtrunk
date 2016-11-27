/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2016 Dennis Sheirer
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
package audio.broadcast.broadcastify;

import audio.broadcast.BroadcastConfiguration;
import audio.broadcast.BroadcastEvent;
import audio.broadcast.BroadcastModel;
import audio.broadcast.BroadcastServerType;
import gui.editor.DocumentListenerEditor;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import settings.SettingsManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class BroadcastifyConfigurationEditor extends DocumentListenerEditor<BroadcastConfiguration>
{
    private final static Logger mLog = LoggerFactory.getLogger( BroadcastifyConfigurationEditor.class );

    private SettingsManager mSettingsManager;
    private BroadcastModel mBroadcastModel;
    private JTextField mFeedID;
    private JTextField mName;
    private JTextField mServer;
    private JTextField mPort;
    private JTextField mMountPoint;
    private JTextField mPassword;
    private JButton mSaveButton;
    private JButton mResetButton;
    private JCheckBox mEnabled;

    public BroadcastifyConfigurationEditor(BroadcastModel model, SettingsManager settingsManager)
    {
        mBroadcastModel = model;
        mSettingsManager = settingsManager;
        init();
    }

    private void init()
    {
        setLayout( new MigLayout( "fill,wrap 2", "[align right][grow,fill]", "[][][][][][][][][][grow,fill]" ) );
        setPreferredSize(new Dimension(150,400));


        JLabel channelLabel = new JLabel("Broadcastify Stream");

        ImageIcon broadcastifyIcon = mSettingsManager.getImageIcon(BroadcastServerType.BROADCASTIFY.getIconName(), 25);
        channelLabel.setIcon(broadcastifyIcon);

        add(channelLabel, "span, align center");

        add(new JLabel("Feed ID:"));
        mFeedID = new JTextField();
        mFeedID.getDocument().addDocumentListener(this);
        add(mFeedID);

        add(new JLabel("Name:"));
        mName = new JTextField();
        mName.getDocument().addDocumentListener(this);
        add(mName);

        add(new JLabel("Server:"));
        mServer = new JTextField();
        mServer.getDocument().addDocumentListener(this);
        add(mServer);

        add(new JLabel("Port:"));
        mPort = new JTextField();
        mPort.getDocument().addDocumentListener(this);
        add(mPort);

        add(new JLabel("Mount:"));
        mMountPoint = new JTextField();
        mMountPoint.getDocument().addDocumentListener(this);
        add(mMountPoint);

        add(new JLabel("Password:"));
        mPassword = new JTextField();
        mPassword.getDocument().addDocumentListener(this);
        add(mPassword);

        mEnabled = new JCheckBox("Enabled");
        mEnabled.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                setModified(true);
            }
        });
        add(mEnabled, "span");

        mSaveButton = new JButton("Save");
        mSaveButton.setEnabled(false);
        mSaveButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                save();
            }
        });
        add(mSaveButton);

        mResetButton = new JButton("Reset");
        mResetButton.setEnabled(false);
        mResetButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                setModified(false);
                setItem(getItem());
            }
        });
        add(mResetButton);
    }

    @Override
    public void setModified(boolean modified)
    {
        super.setModified(modified);

        mSaveButton.setEnabled(modified);
        mResetButton.setEnabled(modified);
    }

    @Override
    public void setItem(BroadcastConfiguration item)
    {
        super.setItem(item);

        if(hasItem())
        {
            BroadcastifyConfiguration config = (BroadcastifyConfiguration)getItem();

            mFeedID.setText(config.getFeedID() != 0 ? String.valueOf(config.getFeedID()) : null);
            mName.setText(config.getName());
            mServer.setText(config.getHost());
            mPort.setText(config.getPort() != 0 ? String.valueOf(config.getPort()) : null);
            mMountPoint.setText(config.getMountPoint());
            mPassword.setText(config.getPassword());
            mEnabled.setSelected(config.isEnabled());
        }
        else
        {
            mFeedID.setText(null);
            mName.setText(null);
            mServer.setText(null);
            mPort.setText(null);
            mMountPoint.setText(null);
            mPassword.setText(null);
            mEnabled.setSelected(false);
        }

        setModified(false);
    }

    @Override
    public void save()
    {
        BroadcastifyConfiguration config = (BroadcastifyConfiguration)getItem();

        if(validateConfiguration())
        {
            config.setName(mName.getText());
            config.setFeedID(getFeedID());
            config.setHost(mServer.getText());
            config.setPort(getPort());
            config.setMountPoint(mMountPoint.getText());
            config.setPassword(mPassword.getText());
            config.setEnabled(mEnabled.isSelected());

            setModified(false);

            mBroadcastModel.process(new BroadcastEvent(getItem(), BroadcastEvent.Event.CONFIGURATION_CHANGE));
        }
    }

    /**
     * Validates the user specified values in the controls prior to saving them to the configuration
     */
    private boolean validateConfiguration()
    {
        String name = mName.getText();

        //A unique stream name is required
        if(!mBroadcastModel.isUniqueName(name, getItem()))
        {
            String message = (name == null || name.isEmpty()) ?
                    "Please specify a stream name." : "Stream name " + name +
                    "is already in use.\nPlease choose another name.";

            JOptionPane.showMessageDialog(BroadcastifyConfigurationEditor.this,
                    message, "Invalid Stream Name", JOptionPane.ERROR_MESSAGE);

            mName.requestFocus();

            return false;
        }

        //Feed ID is optional, but required
        if(!validateIntegerTextField(mFeedID, "Invalid Feed ID", "Please specify a feed ID", 1, Integer.MAX_VALUE))
        {
            return true;
        }

        //Server address is optional, but required
        if(!validateTextField(mServer, "Invalid Server Address", "Please specify a server address."))
        {
            return true;
        }

        //Port is optional, but required
        if(!validateIntegerTextField(mPort, "Invalid Port Number", "Please specify a port number (1 <> 65535)", 1, 65535))
        {
            return true;
        }

        //Mount Point is optional, but required
        if(!validateTextField(mMountPoint, "Invalid Mount Point", "Please specify a mount point."))
        {
            return true;
        }

        //Password is optional, but required
        if(!validateTextField(mPassword, "Invalid Password", "Please specify a password."))
        {
            return true;
        }

        return true;
    }

    /**
     * Validates a text field control for a non-null, non-empty value
     * @param field to validate
     * @param title to use for error dialog
     * @param message to use for error dialog
     * @return true if field contains a non-null, non-empty value
     */
    private boolean validateTextField(JTextField field, String title, String message)
    {
        String text = field.getText();

        if(text == null || text.isEmpty())
        {
            JOptionPane.showMessageDialog(BroadcastifyConfigurationEditor.this, message, title,
                    JOptionPane.ERROR_MESSAGE);

            field.requestFocus();
            return false;
        }

        return true;
    }

    /**
     * Validates the text field control that contains an integer value for non-null, non-empty and within the
     * specified min/max valid range.
     * @param field to validate
     * @param title to use for error dialog
     * @param message to use for error dialog
     * @param minValid value
     * @param maxValid value
     * @return true if field contains a non-null, non-empty value within the valid min/max range
     */
    private boolean validateIntegerTextField(JTextField field, String title, String message, int minValid, int maxValid)
    {
        if(validateTextField(field, title, message))
        {
            String text = field.getText();

            try
            {
                int value = Integer.parseInt(text);

                if(minValid <= value && value <= maxValid)
                {
                    return true;
                }
            }
            catch(Exception e)
            {
                //Do nothing, we couldn't parse the number value
            }

            JOptionPane.showMessageDialog(BroadcastifyConfigurationEditor.this, message, title,
                    JOptionPane.ERROR_MESSAGE);

            field.requestFocus();
        }

        return false;
    }

    /**
     * Parses an Integer from the feed ID text control or returns 0 if the control doesn't contain an integer value.
     */
    private int getFeedID()
    {
        String feedID = mFeedID.getText();

        if(feedID != null && !feedID.isEmpty())
        {
            try
            {
                return Integer.parseInt(feedID);
            }
            catch(Exception e)
            {
                //Do nothing, we couldn't parse the number value
            }
        }

        return 0;
    }

    /**
     * Parses an Integer from the port text control or returns 0 if the control doesn't contain an integer value.
     */
    private int getPort()
    {
        String port = mPort.getText();

        if(port != null && !port.isEmpty())
        {
            try
            {
                return Integer.parseInt(port);
            }
            catch(Exception e)
            {
                //Do nothing, we couldn't parse the number value
            }
        }

        return 0;
    }
}
