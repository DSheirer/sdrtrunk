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

package io.github.dsheirer.controller.channel;

import io.github.dsheirer.gui.ApplicationIcons;
import io.github.dsheirer.gui.theme.ThemeManager;
import io.github.dsheirer.settings.Settings;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.util.ThreadPool;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lists the channels that are set to start automatically and starts them when the countdown ends.  The user can
 * start them now, or cancel by pressing Cancel or closing the window.
 */
public class ChannelAutoStartWindow
{
    private static final Logger mLog = LoggerFactory.getLogger(ChannelAutoStartWindow.class);

    private final Listener<ChannelEvent> mChannelEventListener;
    private final List<Channel> mChannels;
    private final int mTimeoutSeconds;
    private final AtomicBoolean mFinished = new AtomicBoolean();
    private final Stage mStage = new Stage();
    private final Label mCountdownLabel = new Label();
    private final ProgressBar mProgressBar = new ProgressBar(1.0);
    private final Timeline mTimeline;
    private int mRemainingSeconds;

    /**
     * Constructs an instance.  JavaFX thread only.
     * @param listener to receive the channel start requests
     * @param channels to start, in auto-start order
     * @param settings for the countdown length
     * @param owner window, or null
     */
    public ChannelAutoStartWindow(Listener<ChannelEvent> listener, List<Channel> channels,
                                  Settings settings, Window owner)
    {
        mChannelEventListener = listener;
        mChannels = channels;
        mTimeoutSeconds = Math.max(0, settings.getApplicationSettings().getChannelAutoStartTimeout());
        mRemainingSeconds = mTimeoutSeconds;

        Label title = new Label("Starting " + channels.size() + (channels.size() == 1 ? " channel" : " channels"));
        title.setStyle("-fx-font-size: 1.25em; -fx-font-weight: bold;");
        mProgressBar.setMaxWidth(Double.MAX_VALUE);

        TableView<Channel> table = new TableView<>(FXCollections.observableArrayList(channels));
        table.getColumns().addAll(List.of(
            column("Order", 60, channel -> String.valueOf(channel.getAutoStartOrder())),
            column("System", 140, Channel::getSystem),
            column("Site", 120, Channel::getSite),
            column("Name", 180, Channel::getName),
            column("Decoder", 100, channel -> channel.getDecodeConfiguration().getDecoderType()
                .getShortDisplayString())));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(table, Priority.ALWAYS);

        Button startButton = new Button("Start Now");
        startButton.setDefaultButton(true);
        startButton.setOnAction(event -> {
            mLog.info("Starting [" + mChannels.size() + "] channels now - user invoked");
            finish(true);
        });
        Button cancelButton = new Button("Cancel");
        cancelButton.setCancelButton(true);
        cancelButton.setOnAction(event -> {
            mLog.info("Channel auto-start canceled by user");
            finish(false);
        });
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox buttons = new HBox(8, spacer, cancelButton, startButton);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(8, title, mCountdownLabel, mProgressBar, table, buttons);
        root.setPadding(new Insets(12));

        Scene scene = new Scene(root, 640, 340);
        ThemeManager.getInstance().register(scene);
        mStage.setScene(scene);
        mStage.setTitle("Auto-Start Channels");
        ApplicationIcons.apply(mStage);

        if(owner != null)
        {
            mStage.initOwner(owner);
        }

        mStage.setOnCloseRequest(event -> {
            mLog.info("Channel auto-start canceled by user - window closed");
            finish(false);
        });

        mTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> tick()));
        mTimeline.setCycleCount(Timeline.INDEFINITE);
        updateCountdown();
    }

    /**
     * Shows the window and starts the countdown.  JavaFX thread only.
     */
    public void show()
    {
        mStage.show();
        mStage.toFront();

        if(mTimeoutSeconds == 0)
        {
            mLog.info("Starting [" + mChannels.size() + "] now - no auto-start delay");
            finish(true);
        }
        else
        {
            mTimeline.play();
        }
    }

    private void tick()
    {
        mRemainingSeconds--;

        if(mRemainingSeconds <= 0)
        {
            mLog.info("Starting [" + mChannels.size() + "] now - timer invoked");
            finish(true);
        }
        else
        {
            updateCountdown();
        }
    }

    private void updateCountdown()
    {
        mCountdownLabel.setText("These channels start automatically in " + mRemainingSeconds +
            (mRemainingSeconds == 1 ? " second." : " seconds."));
        mProgressBar.setProgress(mTimeoutSeconds > 0 ? (double)mRemainingSeconds / mTimeoutSeconds : 0.0);
    }

    /**
     * Stops the countdown, closes the window and optionally starts the channels.  Only the first call has an
     * effect, so that the timer and the user can't both start the channels.
     */
    private void finish(boolean start)
    {
        if(!mFinished.compareAndSet(false, true))
        {
            return;
        }

        mTimeline.stop();
        mStage.close();

        if(start && mChannelEventListener != null)
        {
            //Starting channels can block while tuners start, so start them off the JavaFX thread, in order
            ThreadPool.CACHED.submit(() -> {
                for(Channel channel: mChannels)
                {
                    mChannelEventListener.receive(new ChannelEvent(channel, ChannelEvent.Event.REQUEST_ENABLE));
                }
            });
        }
    }

    private static TableColumn<Channel,String> column(String title, double width, Function<Channel,String> value)
    {
        TableColumn<Channel,String> column = new TableColumn<>(title);
        column.setPrefWidth(width);
        column.setCellValueFactory(param -> new ReadOnlyStringWrapper(value.apply(param.getValue())));
        return column;
    }
}
