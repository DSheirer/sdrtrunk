/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

package io.github.dsheirer.audio.playbackfx;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;

/**
 * Complete audio playback view
 */
public class AudioPlaybackChannelViewComplete extends AudioPlaybackChannelView
{
    private AudioSpectrumView mAudioSpectrumView;
    private Label mFrequencyLabel;
    private Label mNameLabel;
    private Label mSiteLabel;

    /**
     * Constructs an instance
     * @param controller for audio playback that backs this view.
     */
    public AudioPlaybackChannelViewComplete(AudioPlaybackChannelController controller)
    {
        super(controller);
        //Bind the audio spectrum view to the controller's media player so that it can update as the playback changes
        getAudioSpectrumView().mediaPlayerProperty().bind(getController().mediaPlayerProperty());
        init();
    }

    /**
     * Resets the spectrum width during resizing.
     */
    public void resetWidth()
    {
        getAudioSpectrumView().resetWidth();
    }

    private void init()
    {
        setSpacing(3);
        setMaxWidth(Double.MAX_VALUE);
        setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, null, null)));
        setPadding(new Insets(0));
        GridPane gridPane = new GridPane();
        gridPane.setPadding(new Insets(0, 0, 2, 2));
        gridPane.setHgap(3);

        int row = 0;

        GridPane.setHalignment(getNameLabel(), HPos.RIGHT);
        GridPane.setHgrow(getNameLabel(), Priority.NEVER);
        gridPane.add(getNameLabel(), 0, row, 2, 1);

        HBox playbackStatusIdentifierBox = new HBox();
        playbackStatusIdentifierBox.setAlignment(Pos.CENTER);
        HBox.setHgrow(getMediaPlayerStatus(), Priority.ALWAYS);
        getMediaPlayerStatus().setMaxWidth(Double.MAX_VALUE);
        getMediaPlayerStatus().setAlignment(Pos.BASELINE_RIGHT);
        playbackStatusIdentifierBox.getChildren().addAll(getPlaybackMode(), getMediaPlayerStatus());

        GridPane.setHalignment(playbackStatusIdentifierBox, HPos.LEFT);
        GridPane.setHgrow(playbackStatusIdentifierBox, Priority.ALWAYS);
        gridPane.add(playbackStatusIdentifierBox, 2, row, 2, 1);

        GridPane.setHalignment(getAudioSpectrumView(), HPos.RIGHT);
        GridPane.setHgrow(getAudioSpectrumView(), Priority.ALWAYS);
        gridPane.add(getAudioSpectrumView(), 4, row, 1, 3);

        row++;
        GridPane.setHgrow(getLockLabel(), Priority.NEVER);
        gridPane.add(getLockLabel(), 0, row);

        Label toLabel = new Label("TO:");
        toLabel.setDisable(true);
        GridPane.setHalignment(toLabel, HPos.RIGHT);
        GridPane.setHgrow(toLabel, Priority.NEVER);
        gridPane.add(toLabel, 1, row);

        GridPane.setHalignment(getToLabel(), HPos.RIGHT);
        GridPane.setHgrow(getToLabel(), Priority.NEVER);
        gridPane.add(getToLabel(), 2, row);

        GridPane.setHalignment(getToAliasLabel(), HPos.LEFT);
        GridPane.setHgrow(getToAliasLabel(), Priority.ALWAYS);
        gridPane.add(getToAliasLabel(), 3, row);

        row++;

        GridPane.setHgrow(getMuteLabel(), Priority.NEVER);
        gridPane.add(getMuteLabel(), 0, row);

        Label fromLabel = new Label("FM:");
        fromLabel.setDisable(true);
        GridPane.setHalignment(fromLabel, HPos.RIGHT);
        GridPane.setHgrow(fromLabel, Priority.NEVER);
        gridPane.add(fromLabel, 1, row);

        GridPane.setHalignment(getFromLabel(), HPos.RIGHT);
        GridPane.setHgrow(getFromLabel(), Priority.NEVER);
        gridPane.add(getFromLabel(), 2, row);

        GridPane.setHalignment(getFromAliasLabel(), HPos.LEFT);
        GridPane.setHgrow(getFromAliasLabel(), Priority.ALWAYS);
        gridPane.add(getFromAliasLabel(), 3, row);

        row++;

        Label channelLabel = new Label("CHAN:");
        channelLabel.setDisable(true);
        GridPane.setHalignment(channelLabel, HPos.RIGHT);
        GridPane.setHgrow(channelLabel, Priority.NEVER);
        gridPane.add(channelLabel, 0, row, 2, 1);

        GridPane.setHalignment(getFrequencyLabel(), HPos.LEFT);
        GridPane.setHgrow(getFrequencyLabel(), Priority.ALWAYS);
        gridPane.add(getFrequencyLabel(), 2, row);

        HBox systemSiteBox = new HBox();
        systemSiteBox.setSpacing(3);
        Label dashLabel = new Label("-");
        dashLabel.textProperty().bind(getSiteLabel().textProperty().map(s -> getSiteLabel()
                .textProperty().get() != null ? " - " : null));
        systemSiteBox.getChildren().addAll(getSystemLabel(), dashLabel, getSiteLabel());
        GridPane.setHalignment(systemSiteBox, HPos.LEFT);
        GridPane.setHgrow(systemSiteBox, Priority.ALWAYS);
        gridPane.add(systemSiteBox, 3, row, 2, 1);

        ColumnConstraints cc0 = new ColumnConstraints();
        cc0.setPercentWidth(4);
        ColumnConstraints cc1 = new ColumnConstraints();
        cc1.setPercentWidth(4);
        ColumnConstraints cc2 = new ColumnConstraints();
        cc2.setPercentWidth(14);
        ColumnConstraints cc3 = new ColumnConstraints();
        cc3.setPercentWidth(28);
        ColumnConstraints cc4 = new ColumnConstraints();
        cc4.setPercentWidth(50);
        gridPane.getColumnConstraints().addAll(cc0, cc1, cc2, cc3, cc4);

        getChildren().add(gridPane);
    }

    private AudioSpectrumView getAudioSpectrumView()
    {
        if(mAudioSpectrumView == null)
        {
            mAudioSpectrumView = new AudioSpectrumView();
        }

        return mAudioSpectrumView;
    }

    private Label getFrequencyLabel()
    {
        if(mFrequencyLabel == null)
        {
            mFrequencyLabel = new Label();
            mFrequencyLabel.textProperty().bind(getController().frequencyProperty());
        }

        return mFrequencyLabel;
    }

    private Label getSiteLabel()
    {
        if(mSiteLabel == null)
        {
            mSiteLabel = new Label();
            mSiteLabel.textProperty().bind(getController().siteProperty());
        }

        return mSiteLabel;
    }

    private Label getNameLabel()
    {
        if(mNameLabel == null)
        {
            mNameLabel = new Label();
            mNameLabel.setDisable(true);
            mNameLabel.textProperty().bind(getController().nameProperty().concat(":"));
        }

        return mNameLabel;
    }
}
