/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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
import io.github.dsheirer.module.ProcessingChain;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.source.SourceEvent;
import java.text.DecimalFormat;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.javafx.IconNode;

/**
 * Signal power meter with squelch threshold controls for the AM decoder.
 */
public class SignalPowerView extends HBox
{
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.0");
    private static final String NOT_AVAILABLE = "Not Available";
    private final PowerMeter mPowerMeter = new PowerMeter();
    private final PeakMonitor mPeakMonitor = new PeakMonitor(PowerMeter.DEFAULT_MINIMUM_POWER);
    private final Label mPowerLabel = new Label("0");
    private final Label mPeakLabel = new Label("0");
    private final Label mSquelchLabel = new Label("Squelch:");
    private final Label mSquelchValueLabel = new Label(NOT_AVAILABLE);
    private final Button mSquelchUpButton = new Button();
    private final Button mSquelchDownButton = new Button();
    private final CheckBox mSquelchAutoTrackCheckBox = new CheckBox("Auto Track");
    private final AtomicBoolean mPowerUpdatePending = new AtomicBoolean();
    private volatile double mPower;
    private volatile double mPeak;
    private volatile double mSquelchThreshold;
    private final PlaylistManager mPlaylistManager;
    private volatile ProcessingChain mProcessingChain;

    /**
     * Constructs an instance.  Construct on the JavaFX application thread.
     * @param playlistManager for saving squelch changes to the channel configuration
     */
    public SignalPowerView(PlaylistManager playlistManager)
    {
        super(10);
        mPlaylistManager = playlistManager;
        setPadding(new Insets(5));

        Label meterTitle = new Label("Power (dB)");
        mPowerMeter.setPeakVisible(true);
        mPowerMeter.setSquelchThresholdVisible(true);
        //Follow the themed label text color and font so that the meter is readable in light and dark themes
        mPowerMeter.foregroundProperty().bind(meterTitle.textFillProperty());
        mPowerMeter.fontProperty().bind(meterTitle.fontProperty());
        VBox.setVgrow(mPowerMeter, Priority.ALWAYS);
        VBox meterBox = new VBox(3, meterTitle, mPowerMeter);
        meterBox.setMinWidth(90);

        mPeakLabel.setTooltip(new Tooltip("Current peak power level in decibels."));
        mPowerLabel.setTooltip(new Tooltip("Current Power level in decibels"));
        mSquelchValueLabel.setTooltip(new Tooltip("Squelch threshold value in decibels"));

        mSquelchUpButton.setGraphic(icon(FontAwesome.ANGLE_UP));
        mSquelchUpButton.setTooltip(new Tooltip("Increases the squelch threshold value"));
        mSquelchUpButton.setOnAction(e -> broadcast(SourceEvent.requestSquelchThreshold(null, mSquelchThreshold + 1)));

        mSquelchDownButton.setGraphic(icon(FontAwesome.ANGLE_DOWN));
        mSquelchDownButton.setTooltip(new Tooltip("Decreases the squelch threshold value."));
        mSquelchDownButton.setOnAction(e -> broadcast(SourceEvent.requestSquelchThreshold(null, mSquelchThreshold - 1)));

        mSquelchAutoTrackCheckBox.setTooltip(new Tooltip("Enable or disable monitoring of the noise floor to " +
            "auto-adjust the squelch threshold value maintaining a consistent level/buffer above the noise floor"));
        mSquelchAutoTrackCheckBox.setOnAction(e -> {
            broadcast(SourceEvent.requestSquelchAutoTrack(mSquelchAutoTrackCheckBox.isSelected()));
            setConfigSquelchAutoTrack(mSquelchAutoTrackCheckBox.isSelected());
        });

        GridPane values = new GridPane();
        values.setHgap(5);
        values.setVgap(5);
        values.add(new Label("Peak:"), 0, 0);
        values.add(mPeakLabel, 1, 0);
        values.add(new Label("Power:"), 0, 1);
        values.add(mPowerLabel, 1, 1);
        values.add(mSquelchLabel, 0, 2);
        values.add(mSquelchValueLabel, 1, 2);
        values.add(new HBox(3, mSquelchUpButton, mSquelchDownButton), 0, 3, 2, 1);
        values.add(mSquelchAutoTrackCheckBox, 0, 4, 2, 1);
        values.setAlignment(Pos.TOP_LEFT);

        getChildren().addAll(meterBox, values);
        reset();
    }

    private IconNode icon(FontAwesome fontAwesome)
    {
        IconNode iconNode = new IconNode(fontAwesome);
        iconNode.fillProperty().bind(mSquelchLabel.textFillProperty());
        return iconNode;
    }

    /**
     * Updates the channel's decode configuration with a new squelch threshold value.
     *
     * Note: this is only used for the user threshold notification that follows a user change.  The threshold
     * notifications from the decoder can be values that are automatically tracked from current channel conditions, and
     * saving those would replace the user's setting.
     */
    private void setConfigSquelchThreshold(int threshold)
    {
        ProcessingChain processingChain = mProcessingChain;

        if(processingChain != null)
        {
            Channel channel = mPlaylistManager.getChannelProcessingManager().getChannel(processingChain);

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
        ProcessingChain processingChain = mProcessingChain;

        if(processingChain != null)
        {
            processingChain.broadcast(sourceEvent);
        }
    }

    /**
     * Resets controls when changing processing chain source.  Call on the JavaFX application thread.
     */
    private void reset()
    {
        mPeakMonitor.reset();
        mPowerMeter.reset();
        mPeakLabel.setText("0");
        mPowerLabel.setText("0");
        mSquelchLabel.setDisable(true);
        mSquelchValueLabel.setText(NOT_AVAILABLE);
        mSquelchValueLabel.setDisable(true);
        mSquelchUpButton.setDisable(true);
        mSquelchDownButton.setDisable(true);
        mSquelchAutoTrackCheckBox.setDisable(true);
        mSquelchAutoTrackCheckBox.setSelected(false);
    }

    /**
     * Processes power and squelch source events from the processing chain.  Can be called from any thread.
     */
    public void receive(SourceEvent sourceEvent)
    {
        switch(sourceEvent.getEvent())
        {
            case NOTIFICATION_CHANNEL_POWER ->
            {
                mPower = sourceEvent.getValue().doubleValue();
                mPeak = mPeakMonitor.process(mPower);

                //Power notifications are frequent, so combine the updates that arrive while one is pending
                if(mPowerUpdatePending.compareAndSet(false, true))
                {
                    Platform.runLater(() -> {
                        mPowerUpdatePending.set(false);
                        double power = mPower;
                        double peak = mPeak;
                        mPowerMeter.setPower(power);
                        mPowerLabel.setText(DECIMAL_FORMAT.format(power));
                        mPowerMeter.setPeak(peak);
                        mPeakLabel.setText(DECIMAL_FORMAT.format(peak));
                    });
                }
            }
            case NOTIFICATION_SQUELCH_THRESHOLD ->
            {
                final double threshold = sourceEvent.getValue().doubleValue();
                mSquelchThreshold = threshold;

                Platform.runLater(() -> {
                    mPowerMeter.setSquelchThreshold(threshold);
                    mSquelchLabel.setDisable(false);
                    mSquelchValueLabel.setDisable(false);
                    mSquelchValueLabel.setText(DECIMAL_FORMAT.format(threshold));
                    mSquelchDownButton.setDisable(false);
                    mSquelchUpButton.setDisable(false);
                });
            }
            case NOTIFICATION_SQUELCH_USER_THRESHOLD ->
                    setConfigSquelchThreshold((int)Math.round(sourceEvent.getValue().doubleValue()));
            case NOTIFICATION_SQUELCH_AUTO_TRACK ->
            {
                boolean autoTrack = sourceEvent.getValue().intValue() == 1;
                Platform.runLater(() -> {
                    mSquelchAutoTrackCheckBox.setSelected(autoTrack);
                    mSquelchAutoTrackCheckBox.setDisable(false);
                });
            }
        }
    }

    /**
     * Sets the processing chain for this view.  Call on the JavaFX application thread.
     */
    public void setProcessingChain(ProcessingChain processingChain)
    {
        mProcessingChain = processingChain;
        reset();

        //Request the current squelch settings, which enables the squelch controls when the decoder responds.
        broadcast(SourceEvent.requestCurrentSquelchThreshold());
        broadcast(SourceEvent.requestCurrentSquelchAutoTrack());
    }
}
