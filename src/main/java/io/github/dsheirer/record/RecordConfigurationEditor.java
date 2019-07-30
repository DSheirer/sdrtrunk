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
package io.github.dsheirer.record;

import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.gui.editor.Editor;
import io.github.dsheirer.record.config.RecordConfiguration;
import net.miginfocom.swing.MigLayout;

import javax.swing.JCheckBox;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class RecordConfigurationEditor extends Editor<Channel>
{
    private static final long serialVersionUID = 1L;

    private JCheckBox mBasebandRecorder;
    private JCheckBox mTrafficBasebandRecorder;
    private JCheckBox mBitstreamRecorder;
    private JCheckBox mTrafficBitstreamRecorder;
    private JCheckBox mMBERecorder;
    private JCheckBox mTrafficMBERecorder;

    public RecordConfigurationEditor()
    {
        init();
    }

    private void init()
    {
        setLayout(new MigLayout("fill,wrap 2", "", "[][][][grow]"));

        mBasebandRecorder = new JCheckBox(RecorderType.BASEBAND.getDisplayString());
        mBasebandRecorder.setEnabled(false);
        mBasebandRecorder.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                setModified(true);
            }
        });
        add(mBasebandRecorder);

        mTrafficBasebandRecorder = new JCheckBox(RecorderType.TRAFFIC_BASEBAND.getDisplayString());
        mTrafficBasebandRecorder.setEnabled(false);
        mTrafficBasebandRecorder.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                setModified(true);
            }
        });
        add(mTrafficBasebandRecorder);

        mBitstreamRecorder = new JCheckBox(RecorderType.DEMODULATED_BIT_STREAM.getDisplayString());
        mBitstreamRecorder.setEnabled(false);
        mBitstreamRecorder.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                setModified(true);
            }
        });
        add(mBitstreamRecorder);

        mTrafficBitstreamRecorder = new JCheckBox(RecorderType.TRAFFIC_DEMODULATED_BIT_STREAM.getDisplayString());
        mTrafficBitstreamRecorder.setEnabled(false);
        mTrafficBitstreamRecorder.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                setModified(true);
            }
        });
        add(mTrafficBitstreamRecorder);

        mMBERecorder = new JCheckBox(RecorderType.MBE_CALL_SEQUENCE.getDisplayString());
        mMBERecorder.setEnabled(false);
        mMBERecorder.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                setModified(true);
            }
        });
        add(mMBERecorder);

        mTrafficMBERecorder = new JCheckBox(RecorderType.TRAFFIC_MBE_CALL_SEQUENCE.getDisplayString());
        mTrafficMBERecorder.setEnabled(false);
        mTrafficMBERecorder.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                setModified(true);
            }
        });
        add(mTrafficMBERecorder);
    }

    private void setControlsEnabled(boolean enabled)
    {
        if(mBasebandRecorder.isEnabled() != enabled)
        {
            mBasebandRecorder.setEnabled(enabled);
        }

        if(mTrafficBasebandRecorder.isEnabled() != enabled)
        {
            mTrafficBasebandRecorder.setEnabled(enabled);
        }

        if(mBitstreamRecorder.isEnabled() != enabled)
        {
            mBitstreamRecorder.setEnabled(enabled);
        }

        if(mTrafficBitstreamRecorder.isEnabled() != enabled)
        {
            mTrafficBitstreamRecorder.setEnabled(enabled);
        }

        if(mMBERecorder.isEnabled() != enabled)
        {
            mMBERecorder.setEnabled(enabled);
        }

        if(mTrafficMBERecorder.isEnabled() != enabled)
        {
            mTrafficMBERecorder.setEnabled(enabled);
        }
    }

    @Override
    public void save()
    {
        if(hasItem() && isModified())
        {
            RecordConfiguration config = getItem().getRecordConfiguration();

            config.clearRecorders();

            if(mBasebandRecorder.isSelected())
            {
                config.addRecorder(RecorderType.BASEBAND);
            }

            if(mTrafficBasebandRecorder.isSelected())
            {
                config.addRecorder(RecorderType.TRAFFIC_BASEBAND);
            }

            boolean providesBitstream = getItem().getDecodeConfiguration().getDecoderType().providesBitstream();

            if(providesBitstream && mBitstreamRecorder.isSelected())
            {
                config.addRecorder(RecorderType.DEMODULATED_BIT_STREAM);
            }

            if(providesBitstream && mTrafficBitstreamRecorder.isSelected())
            {
                config.addRecorder(RecorderType.TRAFFIC_DEMODULATED_BIT_STREAM);
            }

            boolean providesMBE = getItem().getDecodeConfiguration().getDecoderType().providesMBEAudioFrames();

            if(providesMBE && mMBERecorder.isSelected())
            {
                config.addRecorder(RecorderType.MBE_CALL_SEQUENCE);
            }

            if(providesMBE && mTrafficMBERecorder.isSelected())
            {
                config.addRecorder(RecorderType.TRAFFIC_MBE_CALL_SEQUENCE);
            }
        }

        setModified(false);
    }

    @Override
    public void setItem(Channel channel)
    {
        super.setItem(channel);

        if(hasItem())
        {
            setControlsEnabled(true);

            List<RecorderType> recorders = getItem().getRecordConfiguration().getRecorders();
            mBasebandRecorder.setSelected(recorders.contains(RecorderType.BASEBAND));
            mTrafficBasebandRecorder.setSelected(recorders.contains(RecorderType.TRAFFIC_BASEBAND));

            boolean providesBitstream = channel.getDecodeConfiguration().getDecoderType().providesBitstream();

            if(providesBitstream)
            {
                mBitstreamRecorder.setSelected(recorders.contains(RecorderType.DEMODULATED_BIT_STREAM));
                mTrafficBitstreamRecorder.setSelected(recorders.contains(RecorderType.TRAFFIC_DEMODULATED_BIT_STREAM));
            }
            else
            {
                mBitstreamRecorder.setSelected(false);
                mBitstreamRecorder.setEnabled(false);
                mTrafficBitstreamRecorder.setSelected(false);
                mTrafficBitstreamRecorder.setEnabled(false);
            }

            boolean providesMBEFrames = channel.getDecodeConfiguration().getDecoderType().providesMBEAudioFrames();

            if(providesMBEFrames)
            {
                mMBERecorder.setSelected(recorders.contains(RecorderType.MBE_CALL_SEQUENCE));
                mTrafficMBERecorder.setSelected(recorders.contains(RecorderType.TRAFFIC_MBE_CALL_SEQUENCE));
            }
            else
            {
                mMBERecorder.setSelected(false);
                mMBERecorder.setEnabled(false);
                mTrafficMBERecorder.setSelected(false);
                mTrafficMBERecorder.setEnabled(false);
            }
        }
        else
        {
            setControlsEnabled(false);
            mBasebandRecorder.setSelected(false);
            mBitstreamRecorder.setSelected(false);
            mMBERecorder.setSelected(false);
            mTrafficMBERecorder.setSelected(false);
            mTrafficBitstreamRecorder.setSelected(false);
            mTrafficBasebandRecorder.setSelected(false);
        }
    }
}
