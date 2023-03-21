/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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
 * ****************************************************************************
 */
package io.github.dsheirer.controller.channel;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.util.ThreadPool;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

public class ChannelAutoStartFrame extends JFrame
{
    private final static Logger mLog = LoggerFactory.getLogger(ChannelAutoStartFrame.class);

    private Listener<ChannelEvent> mChannelEventListener;
    private List<Channel> mChannels;

    private JLabel mCountdownLabel;
    private JButton mStartButton;
    private JButton mCancelButton;
    private JTable mChannelTable;
    private AtomicBoolean mChannelsStarted = new AtomicBoolean();
    private int mAutoStartTimeoutSeconds;
    private ScheduledFuture<?> mTimerFuture;

    /**
     * Creates and displays a channel auto-start gui for presenting the user with a list of channels that
     * will be automatically started once the countdown timer reaches zero, or the user chooses to start
     * now or cancel.
     *
     * @param listener to receive channel start/enable request(s)
     * @param channels to auto-start
     */
    public ChannelAutoStartFrame(Listener<ChannelEvent> listener, List<Channel> channels, UserPreferences userPreferences)
    {
        mChannelEventListener = listener;
        mChannels = channels;
        mAutoStartTimeoutSeconds = userPreferences.getApplicationPreference().getChannelAutoStartTimeout();

        init();

        EventQueue.invokeLater(() -> {
            setVisible(true);
            toFront();
            startTimer();
        });
    }

    private void init()
    {
        setTitle("Auto-Start Channels");
        setSize(new Dimension(400, 300));
        setLocationRelativeTo(null);
        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent event)
            {
                mLog.info("Channel auto-start canceled by user - window closed");
                stopTimer();
                super.windowClosing(event);
            }
        });

        JPanel panel = new JPanel();
        panel.setLayout(new MigLayout("", "[grow,fill][grow,fill]",
            "[][][grow,fill][]"));
        panel.add(new JLabel("The following channels will be automatically"), "span");

        mCountdownLabel = new JLabel(getCountdownText(mAutoStartTimeoutSeconds));
        panel.add(mCountdownLabel, "span");

        panel.add(new JScrollPane(getChannelTable()), "span");

        mStartButton = new JButton("Start Now");
        mStartButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                mLog.info("Starting [" + mChannels.size() + "] channels now - user invoked");
                startChannels();
                stopTimer();
            }
        });
        panel.add(mStartButton);

        mCancelButton = new JButton("Cancel");
        mCancelButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                mLog.info("Channel auto-start canceled by user");
                stopTimer();
            }
        });
        panel.add(mCancelButton);
        setContentPane(panel);
    }

    private String getCountdownText(int value)
    {
        return "started in: " + value + " seconds.";
    }

    /**
     * Updates the countdown text label on the swing event thread
     *
     * @param value for the countdown
     */
    private void updateCountdownText(final int value)
    {
        EventQueue.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                mCountdownLabel.setText(getCountdownText(value));
            }
        });
    }

    /**
     * Creates (once) a table of auto-start channels
     */
    private JTable getChannelTable()
    {
        if(mChannelTable == null)
        {
            AutoStartChannelModel model = new AutoStartChannelModel(mChannels);
            mChannelTable = new JTable(model);
        }

        return mChannelTable;
    }

    /**
     * Sends an enable (ie start) request for each of the auto start channels.
     *
     * This method is thread-safe and will only be executed once.
     *
     * Although the redundant channel-start request would be ignored, this once-only will prevent the chance
     * that both the timer and the user would attempt to start the channels at the same time.
     */
    private void startChannels()
    {
        if(mChannelsStarted.compareAndSet(false, true))
        {
            if(mChannelEventListener != null)
            {
                for(Channel channel : mChannels)
                {
                    mChannelEventListener.receive(new ChannelEvent(channel, ChannelEvent.Event.REQUEST_ENABLE));
                }
            }
        }
    }

    /**
     * Starts the countdown timer to fire once a second.
     */
    private void startTimer()
    {
        if(mTimerFuture == null)
        {
            mTimerFuture = ThreadPool.SCHEDULED.scheduleAtFixedRate(new CountdownTimer(), 0, 1, TimeUnit.SECONDS);
        }
    }

    /**
     * Stops the countdown timer and disposes this frame
     */
    private void stopTimer()
    {
        if(mTimerFuture != null)
        {
            mTimerFuture.cancel(true);
            mTimerFuture = null;
        }

        EventQueue.invokeLater(() -> dispose());
    }

    /**
     * Updates the countdown text and auto-starts the channels once the timer falls below zero.
     */
    public class CountdownTimer implements Runnable
    {
        private int mCount = mAutoStartTimeoutSeconds;

        @Override
        public void run()
        {
            if(mCount >= 0)
            {
                updateCountdownText(mCount);
                mCount--;
            }
            else
            {
                mLog.info("Starting [" + mChannels.size() + "] now - timer invoked");
                startChannels();
                stopTimer();
            }
        }
    }
}
