/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
package module.decode.mpt1327;

import controller.channel.Channel;
import controller.channel.map.ChannelMap;
import controller.channel.map.ChannelMapManagerFrame;
import controller.channel.map.ChannelMapModel;
import gui.editor.Editor;
import gui.editor.EditorValidationException;
import gui.editor.ValidatingEditor;
import module.decode.config.DecodeConfiguration;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class MPT1327DecoderEditor extends ValidatingEditor<Channel>
{
    private static final long serialVersionUID = 1L;

    private JCheckBox mAFC;
    private JSlider mAFCMaximumCorrection;
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

        mAFC = new JCheckBox("AFC: 3000 Hz");
        mAFC.setEnabled(false);
        mAFC.setToolTipText("AFC automatically adjusts the center frequency of the channel to "
            + "correct/compensate for inaccuracies and frequency drift in the tuner");
        mAFC.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent arg0)
            {
                setModified(true);

                if(mAFC.isSelected() && !mAFCMaximumCorrection.isEnabled())
                {
                    mAFCMaximumCorrection.setEnabled(true);
                }
                else if(!mAFC.isSelected() && mAFCMaximumCorrection.isEnabled())
                {
                    mAFCMaximumCorrection.setEnabled(false);
                }
            }
        });

        add(mAFC);

        mAFCMaximumCorrection = new JSlider(0, 7000, 3000);
        mAFCMaximumCorrection.setEnabled(false);
        mAFCMaximumCorrection.setToolTipText("Maximum AFC frequency correction (0 - 15kHz)");
        mAFCMaximumCorrection.setMajorTickSpacing(2000);
        mAFCMaximumCorrection.setMinorTickSpacing(1000);
        mAFCMaximumCorrection.setPaintTicks(true);

        mAFCMaximumCorrection.addChangeListener(new ChangeListener()
        {
            @Override
            public void stateChanged(ChangeEvent e)
            {
                mAFC.setText("AFC: " + mAFCMaximumCorrection.getValue() + " Hz");
                setModified(true);
            }
        });
        add(mAFCMaximumCorrection);

        JButton channelMaps = new JButton("Channel Maps");
        channelMaps.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                final ChannelMapManagerFrame manager =
                    new ChannelMapManagerFrame(mChannelMapModel);

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
        add(channelMaps, "span 2");

        List<ChannelMap> maps = mChannelMapModel.getChannelMaps();
        ChannelMap[] mapArray = maps.toArray(new ChannelMap[maps.size()]);

        mComboChannelMaps = new JComboBox<ChannelMap>();
        mComboChannelMaps.setModel(new DefaultComboBoxModel<ChannelMap>(mapArray));
        mComboChannelMaps.setEnabled(false);
        add(new JLabel("Channel Map:"));
        add(mComboChannelMaps, "span 3,grow");

        mTrafficChannelPoolSize = new JSlider(JSlider.HORIZONTAL, DecodeConfiguration.TRAFFIC_CHANNEL_LIMIT_MINIMUM,
            DecodeConfiguration.TRAFFIC_CHANNEL_LIMIT_MAXIMUM, DecodeConfiguration.TRAFFIC_CHANNEL_LIMIT_DEFAULT);
        mTrafficChannelPoolSize.setEnabled(false);
        mTrafficChannelPoolSize.setMajorTickSpacing(10);
        mTrafficChannelPoolSize.setMinorTickSpacing(5);
        mTrafficChannelPoolSize.setPaintTicks(true);
        mTrafficChannelPoolSizeLabel = new JLabel("Traffic Pool: " +
            mTrafficChannelPoolSize.getValue() + " ");

        mTrafficChannelPoolSize.addChangeListener(new ChangeListener()
        {
            @Override
            public void stateChanged(ChangeEvent e)
            {
                mTrafficChannelPoolSizeLabel.setText("Traffic Pool: " +
                    mTrafficChannelPoolSize.getValue());
                setModified(true);
            }
        });

        add(mTrafficChannelPoolSizeLabel);
        add(mTrafficChannelPoolSize);

        mCallTimeout = new JSlider(JSlider.HORIZONTAL, DecodeConfiguration.CALL_TIMEOUT_MINIMUM,
            DecodeConfiguration.CALL_TIMEOUT_MAXIMUM, DecodeConfiguration.DEFAULT_CALL_TIMEOUT_SECONDS);
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

    @Override
    public void save()
    {
        if(hasItem() && isModified())
        {
            DecodeConfigMPT1327 mpt = new DecodeConfigMPT1327();
            mpt.setAFC(mAFC.isSelected());
            mpt.setAFCMaximumCorrection(mAFCMaximumCorrection.getValue());
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
        if(mAFC.isEnabled() != enabled)
        {
            mAFC.setEnabled(enabled);
        }

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
                mAFC.setSelected(mpt.isAFCEnabled());
                mAFCMaximumCorrection.setValue(mpt.getAFCMaximumCorrection());
                mAFCMaximumCorrection.setEnabled(mpt.isAFCEnabled());

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
                mAFC.setSelected(false);
                mAFCMaximumCorrection.setValue(DecodeConfiguration.DEFAULT_AFC_MAX_CORRECTION);
                mAFCMaximumCorrection.setEnabled(false);
                mTrafficChannelPoolSize.setValue(DecodeConfiguration.TRAFFIC_CHANNEL_LIMIT_DEFAULT);
                mCallTimeout.setValue(DecodeConfiguration.DEFAULT_CALL_TIMEOUT_SECONDS);

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
