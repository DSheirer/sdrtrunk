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

    private JCheckBox mAudioRecorder;
    private JCheckBox mBasebandRecorder;
    private JCheckBox mTrafficBasebandRecorder;
    private JCheckBox mBitstreamRecorder;
    private JCheckBox mTrafficBitstreamRecorder;

    public RecordConfigurationEditor()
    {

        init();
    }

    private void init()
    {
        setLayout(new MigLayout("fill,wrap 2", "", "[][][][grow]"));

        mAudioRecorder = new JCheckBox("Audio");
        mAudioRecorder.setEnabled(false);
        mAudioRecorder.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                setModified(true);
            }
        });
        add(mAudioRecorder, "wrap");

        mBasebandRecorder = new JCheckBox("Baseband I/Q");
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

        mTrafficBasebandRecorder = new JCheckBox("Traffic Channel Baseband I/Q");
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

        mBitstreamRecorder = new JCheckBox("Demodulated Bitstream");
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

        mTrafficBitstreamRecorder = new JCheckBox("Traffic Channel Bitstream");
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
    }

    private void setControlsEnabled(boolean enabled)
    {
        if(mAudioRecorder.isEnabled() != enabled)
        {
            mAudioRecorder.setEnabled(enabled);
        }

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
    }

    @Override
    public void save()
    {
        if(hasItem() && isModified())
        {
            RecordConfiguration config = getItem().getRecordConfiguration();

            config.clearRecorders();

            if(mAudioRecorder.isSelected())
            {
                config.addRecorder(RecorderType.AUDIO);
            }

            if(mBasebandRecorder.isSelected())
            {
                config.addRecorder(RecorderType.BASEBAND);
            }

            if(mTrafficBasebandRecorder.isSelected())
            {
                config.addRecorder(RecorderType.TRAFFIC_BASEBAND);
            }

            if(mBitstreamRecorder.isSelected())
            {
                config.addRecorder(RecorderType.DEMODULATED_BIT_STREAM);
            }

            if(mTrafficBitstreamRecorder.isSelected())
            {
                config.addRecorder(RecorderType.TRAFFIC_DEMODULATED_BIT_STREAM);
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
            mAudioRecorder.setSelected(recorders.contains(RecorderType.AUDIO));
            mBasebandRecorder.setSelected(recorders.contains(RecorderType.BASEBAND));
            mTrafficBasebandRecorder.setSelected(recorders.contains(RecorderType.TRAFFIC_BASEBAND));
            mBitstreamRecorder.setSelected(recorders.contains(RecorderType.DEMODULATED_BIT_STREAM));
            mTrafficBitstreamRecorder.setSelected(recorders.contains(RecorderType.TRAFFIC_DEMODULATED_BIT_STREAM));
        }
        else
        {
            setControlsEnabled(false);
        }
    }
}
