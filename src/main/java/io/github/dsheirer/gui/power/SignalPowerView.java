/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

package io.github.dsheirer.gui.power;

import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.dsp.squelch.ISquelchConfiguration;
import io.github.dsheirer.gui.control.DbPowerMeter;
import io.github.dsheirer.module.ProcessingChain;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.source.SourceEvent;
import java.awt.EventQueue;
import java.text.DecimalFormat;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;
import net.miginfocom.swing.MigLayout;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Swing view for displaying signal power measurements with integrated squelch control.
 */
public class SignalPowerView extends JPanel
{
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.0");
    private static final String NOT_AVAILABLE = "Not Available";
    private final DbPowerMeter mPowerMeter = new DbPowerMeter();
    private final PeakMonitor mPeakMonitor = new PeakMonitor(DbPowerMeter.DEFAULT_MINIMUM_POWER);
    private final JLabel mPowerLabel;
    private final JLabel mPeakLabel;
    private final JLabel mSquelchLabel;
    private final JLabel mSquelchValueLabel;
    private final JButton mSquelchUpButton;
    private final JButton mSquelchDownButton;
    private final JCheckBox mSquelchAutoTrackCheckBox;
    private double mSquelchThreshold;
    private final PlaylistManager mPlaylistManager;
    private ProcessingChain mProcessingChain;

    public SignalPowerView(PlaylistManager playlistManager)
    {
        mPlaylistManager = playlistManager;

        setLayout(new MigLayout("", "[][][][grow,fill]", "[grow,fill]"));
        mPowerMeter.setPeakVisible(true);
        mPowerMeter.setSquelchThresholdVisible(true);
        add(mPowerMeter);

        JPanel valuePanel = new JPanel();
        valuePanel.setLayout(new MigLayout("", "[right][left][][]", ""));

        mPeakLabel = new JLabel("0");
        mPeakLabel.setToolTipText("Current peak power level in decibels.");
        valuePanel.add(new JLabel("Peak:"));
        valuePanel.add(mPeakLabel, "wrap");

        mPowerLabel = new JLabel("0");
        mPowerLabel.setToolTipText("Current Power level in decibels");
        valuePanel.add(new JLabel("Power:"));
        valuePanel.add(mPowerLabel, "wrap");

        mSquelchLabel = new JLabel("Squelch:");
        mSquelchLabel.setEnabled(false);
        valuePanel.add(mSquelchLabel);
        mSquelchValueLabel = new JLabel(NOT_AVAILABLE);
        mSquelchValueLabel.setToolTipText("Squelch threshold value in decibels");
        mSquelchValueLabel.setEnabled(false);
        valuePanel.add(mSquelchValueLabel, "wrap");

        IconFontSwing.register(FontAwesome.getIconFont());
        Icon iconUp = IconFontSwing.buildIcon(FontAwesome.ANGLE_UP, 12);
        mSquelchUpButton = new JButton(iconUp);
        mSquelchUpButton.setToolTipText("Increases the squelch threshold value");
        mSquelchUpButton.setEnabled(false);
        mSquelchUpButton.addActionListener(e -> broadcast(SourceEvent.requestSquelchThreshold(null, mSquelchThreshold + 1)));
        valuePanel.add(mSquelchUpButton);

        Icon iconDown = IconFontSwing.buildIcon(FontAwesome.ANGLE_DOWN, 12);
        mSquelchDownButton = new JButton(iconDown);
        mSquelchDownButton.setToolTipText("Decreases the squelch threshold value.");
        mSquelchDownButton.setEnabled(false);
        mSquelchDownButton.addActionListener(e -> broadcast(SourceEvent.requestSquelchThreshold(null, mSquelchThreshold - 1)));
        valuePanel.add(mSquelchDownButton, "wrap");

        mSquelchAutoTrackCheckBox = new JCheckBox("Auto Track");
        mSquelchAutoTrackCheckBox.setToolTipText("Enable or disable monitoring of the noise floor to auto-adjust the " +
                "squelch threshold value maintaining a consistent level/buffer above the noise floor");
        mSquelchAutoTrackCheckBox.setEnabled(false);
        mSquelchAutoTrackCheckBox.addActionListener(e ->
        {
            broadcast(SourceEvent.requestSquelchAutoTrack(mSquelchAutoTrackCheckBox.isSelected()));
        });
        valuePanel.add(mSquelchAutoTrackCheckBox, "span,left");

        add(valuePanel);
    }

    /**
     * Updates the channel's decode configuration with a new squelch threshold value
     */
    private void setConfigSquelchThreshold(int threshold)
    {
        if(mProcessingChain != null)
        {
            Channel channel = mPlaylistManager.getChannelProcessingManager().getChannel(mProcessingChain);

            if(channel != null && channel.getDecodeConfiguration() instanceof ISquelchConfiguration configuration)
            {
                configuration.setSquelchThreshold(threshold);
                mPlaylistManager.schedulePlaylistSave();
            }
        }
    }

    /**
     * Updates the channel configuration squelch auto-track feature setting.
     * @param autoTrack true to enable.
     */
    private void setConfigSquelchAutoTrack(boolean autoTrack)
    {
        Channel channel = mPlaylistManager.getChannelProcessingManager().getChannel(mProcessingChain);

        if(channel != null && channel.getDecodeConfiguration() instanceof ISquelchConfiguration configuration)
        {
            configuration.setSquelchAutoTrack(autoTrack);
            mPlaylistManager.schedulePlaylistSave();
        }
    }

    private void broadcast(SourceEvent sourceEvent)
    {
        if(mProcessingChain != null)
        {
            mProcessingChain.broadcast(sourceEvent);
        }
    }

    /**
     * Resets controls when changing processing chain source.  Note: this must be called on the Swing
     * dispatch thread because it directly invokes swing components.
     */
    private void reset()
    {
        mPeakMonitor.reset();
        mPowerMeter.reset();
        mPeakLabel.setText("0");
        mPowerLabel.setText("0");
        mSquelchLabel.setEnabled(false);
        mSquelchValueLabel.setText("Not Available");
        mSquelchValueLabel.setEnabled(false);
        mSquelchUpButton.setEnabled(false);
        mSquelchDownButton.setEnabled(false);
        mSquelchAutoTrackCheckBox.setEnabled(false);
        mSquelchAutoTrackCheckBox.setSelected(false);
    }

    public void receive(SourceEvent sourceEvent)
    {
        switch(sourceEvent.getEvent())
        {
            case NOTIFICATION_CHANNEL_POWER ->
            {
                final double power = sourceEvent.getValue().doubleValue();
                final double peak = mPeakMonitor.process(power);

                EventQueue.invokeLater(() -> {
                    mPowerMeter.setPower(power);
                    mPowerLabel.setText(DECIMAL_FORMAT.format(power));

                    mPowerMeter.setPeak(peak);
                    mPeakLabel.setText(DECIMAL_FORMAT.format(peak));
                });
            }
            case NOTIFICATION_SQUELCH_THRESHOLD ->
            {
                final double threshold = sourceEvent.getValue().doubleValue();
                mSquelchThreshold = threshold;
                setConfigSquelchThreshold((int)threshold);

                EventQueue.invokeLater(() -> {
                    mPowerMeter.setSquelchThreshold(threshold);
                    mSquelchLabel.setEnabled(true);
                    mSquelchValueLabel.setEnabled(true);
                    mSquelchValueLabel.setText(DECIMAL_FORMAT.format(threshold));
                    mSquelchDownButton.setEnabled(true);
                    mSquelchUpButton.setEnabled(true);
                });
            }
            case NOTIFICATION_SQUELCH_AUTO_TRACK ->
            {
                boolean autoTrack = sourceEvent.getValue().intValue() == 1;
                setConfigSquelchAutoTrack(autoTrack);
                EventQueue.invokeLater(() -> {
                    mSquelchAutoTrackCheckBox.setSelected(autoTrack);
                    mSquelchAutoTrackCheckBox.setEnabled(true);
                });
            }
        }
    }

    /**
     * Sets the processing chain for this view
     */
    public void setProcessingChain(ProcessingChain processingChain)
    {
        mProcessingChain = processingChain;
        reset();
    }
}
