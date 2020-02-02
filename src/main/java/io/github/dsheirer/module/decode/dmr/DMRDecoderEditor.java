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
package io.github.dsheirer.module.decode.dmr;

import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.gui.editor.Editor;
import io.github.dsheirer.gui.editor.EditorValidationException;
import io.github.dsheirer.gui.editor.ValidatingEditor;
import io.github.dsheirer.module.decode.AuxDecodeConfigurationEditor;
import io.github.dsheirer.module.decode.config.AuxDecodeConfiguration;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.module.decode.dmr.DecodeConfigDMR;
import io.github.dsheirer.module.decode.dmr.DMRDecoder;
import io.github.dsheirer.source.SourceConfigurationEditor;
import io.github.dsheirer.source.config.SourceConfigTuner;
import io.github.dsheirer.source.config.SourceConfigTunerMultipleFrequency;
import io.github.dsheirer.source.config.SourceConfiguration;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class DMRDecoderEditor extends ValidatingEditor<Channel>
{
    private final static Logger mLog = LoggerFactory.getLogger(DMRDecoderEditor.class);

    private static final long serialVersionUID = 1L;

    private JComboBox<DMRDecoder.Modulation> mComboModulation;
    private JCheckBox mIgnoreDataCalls;
    private JLabel mTrafficChannelPoolSizeLabel;
    private JSlider mTrafficChannelPoolSize;

    public DMRDecoderEditor()
    {
        init();
    }

    private void init()
    {
        setLayout(new MigLayout("insets 0 0 0 0,wrap 3", "[right][grow,fill][]", ""));

        mComboModulation = new JComboBox<DMRDecoder.Modulation>();
        mComboModulation.setModel(new DefaultComboBoxModel<DMRDecoder.Modulation>(
            DMRDecoder.Modulation.values()));
        mComboModulation.setEnabled(false);
        mComboModulation.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                setModified(true);
            }
        });

        add(new JLabel("Modulation:"));
        add(mComboModulation);

        mIgnoreDataCalls = new JCheckBox("Ignore Data Calls");
        mIgnoreDataCalls.setEnabled(false);
        mIgnoreDataCalls.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                setModified(true);
            }
        });

        add(mIgnoreDataCalls);

        mTrafficChannelPoolSize = new JSlider(JSlider.HORIZONTAL, DecodeConfiguration.TRAFFIC_CHANNEL_LIMIT_MINIMUM,
            DecodeConfiguration.TRAFFIC_CHANNEL_LIMIT_MAXIMUM, DecodeConfiguration.TRAFFIC_CHANNEL_LIMIT_DEFAULT);
        mTrafficChannelPoolSize.setEnabled(false);
        mTrafficChannelPoolSize.setMajorTickSpacing(10);
        mTrafficChannelPoolSize.setMinorTickSpacing(5);
        mTrafficChannelPoolSize.setPaintTicks(true);
        mTrafficChannelPoolSize.addChangeListener(new ChangeListener()
        {
            @Override
            public void stateChanged(ChangeEvent e)
            {
                mTrafficChannelPoolSizeLabel.setText("Traffic Pool: " + mTrafficChannelPoolSize.getValue());
                setModified(true);
            }
        });

        mTrafficChannelPoolSizeLabel = new JLabel("Traffic Pool: " + mTrafficChannelPoolSize.getValue() + " ");

        add(mTrafficChannelPoolSizeLabel);
        add(mTrafficChannelPoolSize);
    }

    @Override
    public void save()
    {
        if(hasItem() && isModified())
        {
            DecodeConfigDMR dmr = new DecodeConfigDMR();

            dmr.setModulation((DMRDecoder.Modulation)mComboModulation.getSelectedItem());
            dmr.setIgnoreDataCalls(mIgnoreDataCalls.isSelected());
            dmr.setTrafficChannelPoolSize(mTrafficChannelPoolSize.getValue());

            getItem().setDecodeConfiguration(dmr);
        }

        setModified(false);
    }

    @Override
    public void validate(Editor<Channel> editor) throws EditorValidationException
    {
        /**
         * Enforce CQPSK modulation uses a Tuner Source for I/Q sample data
         */
        if(editor instanceof SourceConfigurationEditor)
        {
            SourceConfiguration config = ((SourceConfigurationEditor)editor).getSourceConfiguration();

            if(!(config instanceof SourceConfigTuner || config instanceof SourceConfigTunerMultipleFrequency))
            {
                throw new EditorValidationException(editor,
                    "<html><body width='175'><h3>LSM Simulcast</h3>"
                        + "<p>P25 LSM Simulcast decoder can only be used with "
                        + "a tuner source.  Please change the Source to use a tuner"
                        + " or change the P25 Decoder to C4FM modulation</p>");
            }
        }
        else if(editor instanceof AuxDecodeConfigurationEditor)
        {
            AuxDecodeConfiguration config = ((AuxDecodeConfigurationEditor)editor).getConfiguration();

            if(config != null && !config.getAuxDecoders().isEmpty())
            {
                throw new EditorValidationException(editor,
                    "<html><body width='175'><h3>Auxilary Decoders</h3>"
                        + "<p>The auxiliary decoders work with analog audio and are not "
                        + "compatible with the P25 Decoder.  Please uncheck any auxiliary "
                        + "decoders that you have selected for this channel</p>");
            }
        }
    }

    private void setControlsEnabled(boolean enabled)
    {
        if(mComboModulation.isEnabled() != enabled)
        {
            mComboModulation.setEnabled(enabled);
        }

        if(mTrafficChannelPoolSize.isEnabled() != enabled)
        {
            mTrafficChannelPoolSize.setEnabled(enabled);
        }

        if(mIgnoreDataCalls.isEnabled() != enabled)
        {
            mIgnoreDataCalls.setEnabled(enabled);
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

            if(config instanceof DecodeConfigDMR)
            {
                DecodeConfigDMR dmr = (DecodeConfigDMR)config;

                mComboModulation.setSelectedItem(dmr.getModulation());
                mIgnoreDataCalls.setSelected(dmr.getIgnoreDataCalls());
                mTrafficChannelPoolSize.setValue(dmr.getTrafficChannelPoolSize());
                setModified(false);
            }
            else
            {
                mComboModulation.setSelectedItem(DMRDecoder.Modulation.C4FM);
                mIgnoreDataCalls.setSelected(false);
                mTrafficChannelPoolSize.setValue(DecodeConfiguration.TRAFFIC_CHANNEL_LIMIT_DEFAULT);
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
