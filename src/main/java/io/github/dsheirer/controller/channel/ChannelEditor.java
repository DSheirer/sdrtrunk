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

import com.jidesoft.swing.JideTabbedPane;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.controller.channel.map.ChannelMapModel;
import io.github.dsheirer.gui.editor.Editor;
import io.github.dsheirer.gui.editor.EditorValidationException;
import io.github.dsheirer.module.decode.AuxDecodeConfigurationEditor;
import io.github.dsheirer.module.decode.DecodeConfigurationEditor;
import io.github.dsheirer.module.log.EventLogConfigurationEditor;
import io.github.dsheirer.record.RecordConfigurationEditor;
import io.github.dsheirer.source.SourceConfigurationEditor;
import io.github.dsheirer.source.SourceManager;
import net.miginfocom.swing.MigLayout;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ChannelEditor extends Editor<Channel> implements ActionListener, ChannelEventListener
{
    private static final long serialVersionUID = 1L;
    private static final String ACTION_START = "Start";
    private static final String ACTION_STOP = "Stop";
    private static final String ACTION_SAVE = "Save";
    private static final String ACTION_RESET = "Reset";

    private NameConfigurationEditor mNameConfigurationEditor;
    private SourceConfigurationEditor mSourceConfigurationEditor;
    private DecodeConfigurationEditor mDecodeConfigurationEditor;
    private AuxDecodeConfigurationEditor mAuxDecodeConfigurationEditor;
    private EventLogConfigurationEditor mEventLogConfigurationEditor;
    private RecordConfigurationEditor mRecordConfigurationEditor;

    private JButton mProcessingStateButton = new JButton(ACTION_START);
    private JLabel mChannelName = new JLabel("Channel:");

    private ChannelModel mChannelModel;
    private ChannelMapModel mChannelMapModel;
    private SourceManager mSourceManager;

    private boolean mChannelStartRequested = false;

    public ChannelEditor(ChannelModel channelModel,
                         ChannelMapModel channelMapModel,
                         SourceManager sourceManager,
                         AliasModel aliasModel)
    {
        mChannelModel = channelModel;
        mChannelMapModel = channelMapModel;
        mSourceManager = sourceManager;
        mNameConfigurationEditor = new NameConfigurationEditor(aliasModel, mChannelModel);

        init();
    }

    private void init()
    {
        setLayout(new MigLayout("fill,wrap 3", "[grow,fill][grow,fill][grow,fill]", "[grow,fill][]"));

        JideTabbedPane tabs = new JideTabbedPane();
        tabs.setFont(this.getFont());
        tabs.setForeground(Color.BLACK);

        tabs.setTabTrailingComponent(mChannelName);

        mNameConfigurationEditor.setSaveRequestListener(this);
        tabs.addTab("Name/Alias", mNameConfigurationEditor);

        mSourceConfigurationEditor = new SourceConfigurationEditor(mSourceManager);
        mSourceConfigurationEditor.setSaveRequestListener(this);
        tabs.addTab("Source", mSourceConfigurationEditor);

        mDecodeConfigurationEditor = new DecodeConfigurationEditor(mChannelMapModel);
        mDecodeConfigurationEditor.setSaveRequestListener(this);
        tabs.addTab("Decoder", mDecodeConfigurationEditor);

        mAuxDecodeConfigurationEditor = new AuxDecodeConfigurationEditor();
        mAuxDecodeConfigurationEditor.setSaveRequestListener(this);
        tabs.addTab("Aux Decoders", mAuxDecodeConfigurationEditor);

        mEventLogConfigurationEditor = new EventLogConfigurationEditor();
        mEventLogConfigurationEditor.setSaveRequestListener(this);
        tabs.addTab("Logging", mEventLogConfigurationEditor);

        mRecordConfigurationEditor = new RecordConfigurationEditor();
        mRecordConfigurationEditor.setSaveRequestListener(this);
        tabs.addTab("Recording", mRecordConfigurationEditor);

        add(tabs, "span");

        mProcessingStateButton.addActionListener(this);
        mProcessingStateButton.setEnabled(false);
        mProcessingStateButton.setToolTipText("Start/Stop the currently selected channel");
        add(mProcessingStateButton);

        JButton btnSave = new JButton(ACTION_SAVE);
        btnSave.setToolTipText("Save changes to the currently selected channel");
        btnSave.addActionListener(ChannelEditor.this);
        add(btnSave);

        JButton btnReset = new JButton(ACTION_RESET);
        btnReset.setToolTipText("Reload the currently selected channel");
        btnReset.addActionListener(ChannelEditor.this);
        add(btnReset);
    }

    @Override
    public void channelChanged(ChannelEvent event)
    {
        if(hasItem() && getItem() == event.getChannel())
        {
            switch(event.getEvent())
            {
                case NOTIFICATION_CONFIGURATION_CHANGE:
                case NOTIFICATION_PROCESSING_START:
                case NOTIFICATION_PROCESSING_STOP:
                    setItem(getItem());
                    mChannelStartRequested = false;
                    break;
                case NOTIFICATION_DELETE:
                    setItem(null);
                    break;
                case NOTIFICATION_START_PROCESSING_REJECTED:
                    if(mChannelStartRequested)
                    {
                        JOptionPane.showMessageDialog(ChannelEditor.this, "Channel could not be "
                            + "started.  This is likely because there are no tuner channels "
                            + "available", "Couldn't start channel", JOptionPane.INFORMATION_MESSAGE);

                        mChannelStartRequested = false;
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        String command = e.getActionCommand();

        if(command.contentEquals(ACTION_START))
        {
            if(hasItem())
            {
                save();

                mChannelStartRequested = true;
                mChannelModel.broadcast(new ChannelEvent(getItem(), ChannelEvent.Event.REQUEST_ENABLE));
            }
        }
        else if(command.contentEquals(ACTION_STOP))
        {
            if(hasItem())
            {
                mChannelModel.broadcast(new ChannelEvent(getItem(), ChannelEvent.Event.REQUEST_DISABLE));
            }
        }
        else if(command.contentEquals(ACTION_SAVE))
        {
            save();
        }
        else if(command.contentEquals(ACTION_RESET))
        {
            setItem(getItem());
        }
    }

    /**
     * Sets the channel configuration in each of the channel editor components
     * Note: this method must be invoked on the swing event dispatch thread
     */
    public void setItem(final Channel channel)
    {
        super.setItem(channel);

        mNameConfigurationEditor.setItem(channel);
        mSourceConfigurationEditor.setItem(channel);
        mDecodeConfigurationEditor.setItem(channel);
        mAuxDecodeConfigurationEditor.setItem(channel);
        mEventLogConfigurationEditor.setItem(channel);
        mRecordConfigurationEditor.setItem(channel);

        if(channel != null)
        {
            mChannelName.setText("Channel: " + channel.getName());
            mProcessingStateButton.setText(channel.isProcessing() ? ACTION_STOP : ACTION_START);
            mProcessingStateButton.setEnabled(true);
            mProcessingStateButton.setBackground(channel.isProcessing() ? Color.GREEN : getBackground());
        }
        else
        {
            mChannelName.setText("Channel: ");
            mProcessingStateButton.setText(ACTION_START);
            mProcessingStateButton.setEnabled(false);
            mProcessingStateButton.setBackground(getBackground());
        }
    }

    public void save()
    {
        if(hasItem())
        {
            mNameConfigurationEditor.save();
            mSourceConfigurationEditor.save();
            mDecodeConfigurationEditor.save();
            mAuxDecodeConfigurationEditor.save();
            mEventLogConfigurationEditor.save();
            mRecordConfigurationEditor.save();

            try
            {
                mDecodeConfigurationEditor.validate(mSourceConfigurationEditor);
                mDecodeConfigurationEditor.validate(mAuxDecodeConfigurationEditor);
            }
            catch(EditorValidationException e)
            {
                e.getEditor().requestFocus();

                JOptionPane.showMessageDialog(e.getEditor(), e.getReason(),
                    "Configuration Error", JOptionPane.ERROR_MESSAGE);

                return;
            }


            mChannelModel.broadcast(new ChannelEvent(getItem(),
                ChannelEvent.Event.NOTIFICATION_CONFIGURATION_CHANGE));
        }

        setModified(false);
    }
}
