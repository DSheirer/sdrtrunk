/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
package io.github.dsheirer.module.log;

import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.gui.editor.Editor;
import io.github.dsheirer.module.log.config.EventLogConfiguration;
import net.miginfocom.swing.MigLayout;

import javax.swing.JCheckBox;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class EventLogConfigurationEditor extends Editor<Channel>
{
    private static final long serialVersionUID = 1L;

    private JCheckBox mCallEventLogger;
    private JCheckBox mDecodedMessageLogger;
    private JCheckBox mTrafficCallEventLogger;
    private JCheckBox mTrafficDecodedMessageLogger;

    public EventLogConfigurationEditor()
    {
        init();
    }

    private void init()
    {
        setLayout(new MigLayout("fill,wrap 2", "", "[][][grow]"));

        mCallEventLogger = new JCheckBox("Call Events");
        mCallEventLogger.setEnabled(false);
        mCallEventLogger.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                setModified(true);
            }
        });
        add(mCallEventLogger);

        mDecodedMessageLogger = new JCheckBox("Decoded Messages");
        mDecodedMessageLogger.setEnabled(false);
        mDecodedMessageLogger.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                setModified(true);
            }
        });
        add(mDecodedMessageLogger);

        mTrafficCallEventLogger = new JCheckBox("Traffic Channel Call Events");
        mTrafficCallEventLogger.setEnabled(false);
        mTrafficCallEventLogger.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                setModified(true);
            }
        });
        add(mTrafficCallEventLogger);

        mTrafficDecodedMessageLogger = new JCheckBox("Traffic Channel Decoded Messages");
        mTrafficDecodedMessageLogger.setEnabled(false);
        mTrafficDecodedMessageLogger.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                setModified(true);
            }
        });
        add(mTrafficDecodedMessageLogger);
    }

    /**
     * Saves the current configuration as the stored configuration
     */
    @Override
    public void save()
    {
        if(hasItem())
        {
            EventLogConfiguration config = getItem().getEventLogConfiguration();

            config.clear();

            if(mCallEventLogger.isSelected())
            {
                config.addLogger(EventLogType.CALL_EVENT);
            }
            if(mDecodedMessageLogger.isSelected())
            {
                config.addLogger(EventLogType.DECODED_MESSAGE);
            }
            if(mTrafficCallEventLogger.isSelected())
            {
                config.addLogger(EventLogType.TRAFFIC_CALL_EVENT);
            }
            if(mTrafficDecodedMessageLogger.isSelected())
            {
                config.addLogger(EventLogType.TRAFFIC_DECODED_MESSAGE);
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

            List<EventLogType> loggers = getItem().getEventLogConfiguration().getLoggers();
            mDecodedMessageLogger.setSelected(loggers.contains(EventLogType.DECODED_MESSAGE));
            mCallEventLogger.setSelected(loggers.contains(EventLogType.CALL_EVENT));
            mTrafficCallEventLogger.setSelected(loggers.contains(EventLogType.TRAFFIC_CALL_EVENT));
            mTrafficDecodedMessageLogger.setSelected(loggers.contains(EventLogType.TRAFFIC_DECODED_MESSAGE));
        }
        else
        {
            setControlsEnabled(false);
        }
    }

    private void setControlsEnabled(boolean enabled)
    {
        if(mDecodedMessageLogger.isEnabled() != enabled)
        {
            mDecodedMessageLogger.setEnabled(enabled);
        }

        if(mCallEventLogger.isEnabled() != enabled)
        {
            mCallEventLogger.setEnabled(enabled);
        }

        if(mTrafficDecodedMessageLogger.isEnabled() != enabled)
        {
            mTrafficDecodedMessageLogger.setEnabled(enabled);
        }

        if(mTrafficCallEventLogger.isEnabled() != enabled)
        {
            mTrafficCallEventLogger.setEnabled(enabled);
        }
    }
}
