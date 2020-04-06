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
package io.github.dsheirer.module.decode.mpt1327;

import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.map.ChannelMap;
import io.github.dsheirer.controller.channel.map.ChannelMapManagerFrame;
import io.github.dsheirer.controller.channel.map.ChannelMapModel;
import io.github.dsheirer.gui.editor.Editor;
import io.github.dsheirer.gui.editor.EditorValidationException;
import io.github.dsheirer.gui.editor.ValidatingEditor;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

public class MPT1327DecoderEditor extends ValidatingEditor<Channel>
{
    private final static Logger mLog = LoggerFactory.getLogger(MPT1327DecoderEditor.class);
    private static final long serialVersionUID = 1L;

    private JComboBox<ChannelMap> mComboChannelMaps;
    private JLabel mCallTimeoutLabel;
    private JSlider mCallTimeout;
    private JLabel mTrafficChannelPoolSizeLabel;
    private JSlider mTrafficChannelPoolSize;
    private ChannelMapModel mChannelMapModel;

    public MPT1327DecoderEditor(ChannelMapModel channelMapModel)
    {
        mChannelMapModel = channelMapModel;

        init();
    }

    private void init()
    {
        setLayout(new MigLayout("insets 0 0 0 0,wrap 4", "[right][grow,fill][right][grow,fill]", ""));

        mComboChannelMaps = new JComboBox<ChannelMap>();
        refreshChannelMaps();
        mComboChannelMaps.setEnabled(false);
        mComboChannelMaps.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                setModified(true);
            }
        });
        add(new JLabel("Channel Map:"));
        add(mComboChannelMaps);

        JButton channelMaps = new JButton("Channel Map Editor");
        channelMaps.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                final ChannelMapManagerFrame manager = new ChannelMapManagerFrame(mChannelMapModel);
                manager.addWindowListener(new WindowAdapter()
                {
                    @Override
                    public void windowClosed(WindowEvent e)
                    {
                        super.windowClosed(e);
                        mLog.debug("Refreshing channel maps combo box");
                        refreshChannelMaps();
                    }
                });

                EventQueue.invokeLater(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        manager.setVisible(true);
                    }
                });
            }
        });
        add(channelMaps, "span 2,grow");

        mTrafficChannelPoolSize = new JSlider(JSlider.HORIZONTAL, DecodeConfiguration.TRAFFIC_CHANNEL_LIMIT_MINIMUM,
            DecodeConfiguration.TRAFFIC_CHANNEL_LIMIT_MAXIMUM, DecodeConfiguration.TRAFFIC_CHANNEL_LIMIT_DEFAULT);
        mTrafficChannelPoolSize.setEnabled(false);
        mTrafficChannelPoolSize.setMajorTickSpacing(10);
        mTrafficChannelPoolSize.setMinorTickSpacing(5);
        mTrafficChannelPoolSize.setPaintTicks(true);
        mTrafficChannelPoolSizeLabel = new JLabel("Traffic Pool: " + mTrafficChannelPoolSize.getValue() + " ");

        mTrafficChannelPoolSize.addChangeListener(new ChangeListener()
        {
            @Override
            public void stateChanged(ChangeEvent e)
            {
                mTrafficChannelPoolSizeLabel.setText("Traffic Pool: " + mTrafficChannelPoolSize.getValue());
                setModified(true);
            }
        });

        add(mTrafficChannelPoolSizeLabel);
        add(mTrafficChannelPoolSize);

        mCallTimeout = new JSlider(JSlider.HORIZONTAL, DecodeConfiguration.CALL_TIMEOUT_MINIMUM,
            DecodeConfiguration.CALL_TIMEOUT_MAXIMUM, DecodeConfiguration.DEFAULT_CALL_TIMEOUT_DELAY_SECONDS);
        mCallTimeout.setEnabled(false);
        mCallTimeout.setMajorTickSpacing(100);
        mCallTimeout.setMinorTickSpacing(50);
        mCallTimeout.setPaintTicks(true);

        mCallTimeoutLabel = new JLabel("Timeout: " + mCallTimeout.getValue() + " ");

        mCallTimeout.addChangeListener(new ChangeListener()
        {
            @Override
            public void stateChanged(ChangeEvent e)
            {
                mCallTimeoutLabel.setText("Timeout: " + mCallTimeout.getValue());
                setModified(true);
            }
        });

        add(mCallTimeoutLabel);
        add(mCallTimeout);
    }

    private void refreshChannelMaps()
    {
        List<ChannelMap> maps = mChannelMapModel.getChannelMaps();

        ChannelMap[] mapArray = new ChannelMap[maps.size() + 1];

        int index = 1;

        for(ChannelMap channelMap: maps)
        {
            mapArray[index++] = channelMap;
        }

        mComboChannelMaps.setModel(new DefaultComboBoxModel<ChannelMap>(mapArray));
    }

    @Override
    public void save()
    {
        if(hasItem() && isModified())
        {
            DecodeConfigMPT1327 mpt = new DecodeConfigMPT1327();
            mpt.setTrafficChannelPoolSize(mTrafficChannelPoolSize.getValue());
            mpt.setCallTimeout(mCallTimeout.getValue());

            if(mComboChannelMaps.getSelectedItem() != null)
            {
                ChannelMap map = (ChannelMap)mComboChannelMaps.getSelectedItem();

                mpt.setChannelMapName(map.getName());
            }

            getItem().setDecodeConfiguration(mpt);
        }

        setModified(false);
    }

    @Override
    public void validate(Editor<Channel> editor) throws EditorValidationException
    {
        //No validation required
    }

    private void setControlsEnabled(boolean enabled)
    {
        if(mComboChannelMaps.isEnabled() != enabled)
        {
            mComboChannelMaps.setEnabled(enabled);
        }

        if(mTrafficChannelPoolSize.isEnabled() != enabled)
        {
            mTrafficChannelPoolSize.setEnabled(enabled);
        }

        if(mCallTimeout.isEnabled() != enabled)
        {
            mCallTimeout.setEnabled(enabled);
        }
    }

    @Override
    public void setItem(Channel item)
    {
        super.setItem(item);

        if(hasItem())
        {
            setControlsEnabled(true);

            DecodeConfiguration config = getItem().getDecodeConfiguration();

            if(config instanceof DecodeConfigMPT1327)
            {
                DecodeConfigMPT1327 mpt = (DecodeConfigMPT1327)config;
                ChannelMap map = mChannelMapModel.getChannelMap(mpt.getChannelMapName());

                if(map != null)
                {
                    mComboChannelMaps.setSelectedItem(map);
                }

                mTrafficChannelPoolSize.setValue(mpt.getTrafficChannelPoolSize());
                mCallTimeout.setValue(mpt.getCallTimeout());

                setModified(false);
            }
            else
            {
                mTrafficChannelPoolSize.setValue(DecodeConfiguration.TRAFFIC_CHANNEL_LIMIT_DEFAULT);
                mCallTimeout.setValue(DecodeConfiguration.DEFAULT_CALL_TIMEOUT_DELAY_SECONDS);

                setModified(true);
            }
        }
        else
        {
            setControlsEnabled(false);
            setModified(false);
        }
    }
}
