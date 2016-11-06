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
import audio.broadcast.BroadcastConfigurationEvent;
import audio.broadcast.BroadcastModel;
import gui.editor.DocumentListenerEditor;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class BroadcastifyConfigurationEditor extends DocumentListenerEditor<BroadcastConfiguration>
{
    private BroadcastModel mBroadcastModel;
    private JTextField mFeedID;
    private JTextField mName;
    private JTextField mServer;
    private JTextField mPort;
    private JTextField mMountPoint;
    private JTextField mPassword;
    private JButton mSaveButton;
    private JButton mResetButton;

    public BroadcastifyConfigurationEditor(BroadcastModel model)
    {
        mBroadcastModel = model;
        init();
    }

    private void init()
    {
        setLayout( new MigLayout( "fill,wrap 2", "[align right][grow,fill]", "[][][][][][][][][][][grow,fill]" ) );
        setPreferredSize(new Dimension(150,400));

        add(new JLabel("Broadcastify Channel"), "span, align center");

        JButton selectButton = new JButton("Select ...");
        add(selectButton, "wrap");

        add(new JSeparator(JSeparator.HORIZONTAL), "span,growx");

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
        }
        else
        {
            mFeedID.setText(null);
            mName.setText(null);
            mServer.setText(null);
            mPort.setText(null);
            mMountPoint.setText(null);
            mPassword.setText(null);
        }

        setModified(false);
    }

    @Override
    public void save()
    {
        BroadcastifyConfiguration config = (BroadcastifyConfiguration)getItem();

        String feedID = mFeedID.getText();

        if(feedID != null && !feedID.isEmpty())
        {
            try
            {
                int id = Integer.parseInt(feedID);
                config.setFeedID(id);
            }
            catch(Exception e)
            {
                //Do nothing, we couldn't parse the number value
            }
        }

        config.setName(mName.getText());
        config.setHost(mServer.getText());

        String port = mPort.getText();

        if(port != null && !port.isEmpty())
        {
            try
            {
                int number = Integer.parseInt(port);
                config.setPort(number);
            }
            catch(Exception e)
            {
                //Do nothing, we couldn't parse the number value
            }
        }

        config.setMountPoint(mMountPoint.getText());
        config.setPassword(mPassword.getText());

        setModified(false);

        mBroadcastModel.broadcast(new BroadcastConfigurationEvent(getItem(), BroadcastConfigurationEvent.Event.CHANGE));
    }
}
