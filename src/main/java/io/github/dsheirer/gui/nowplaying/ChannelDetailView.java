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
package io.github.dsheirer.gui.nowplaying;

import io.github.dsheirer.channel.state.DecoderState;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.ChannelProcessingManager;
import io.github.dsheirer.module.ProcessingChain;
import io.github.dsheirer.sample.Listener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Activity summary details for the selected channel.
 */
public class ChannelDetailView extends VBox implements Listener<ProcessingChain>
{
    private static final String EMPTY_DETAILS = "Please select a channel to view details";
    private final ChannelProcessingManager mChannelProcessingManager;
    private final Label mSystemLabel = new Label();
    private final Label mSiteLabel = new Label();
    private final Label mNameLabel = new Label();
    private final TextArea mDetailTextArea = new TextArea(EMPTY_DETAILS);
    private ProcessingChain mProcessingChain;

    /**
     * Constructs an instance.  Construct on the JavaFX application thread.
     * @param channelProcessingManager for the channel of the processing chain
     */
    public ChannelDetailView(ChannelProcessingManager channelProcessingManager)
    {
        mChannelProcessingManager = channelProcessingManager;

        Button refreshButton = new Button("Refresh");
        refreshButton.setTooltip(new Tooltip("Update the channel details"));
        refreshButton.setOnAction(event -> refresh());

        HBox labels = new HBox(5, new Label("System:"), grow(mSystemLabel), new Label("Site:"), grow(mSiteLabel),
            new Label("Channel Name:"), grow(mNameLabel), refreshButton);
        labels.setAlignment(Pos.CENTER_LEFT);
        labels.setPadding(new Insets(3, 2, 3, 2));

        mDetailTextArea.setEditable(false);
        VBox.setVgrow(mDetailTextArea, Priority.ALWAYS);
        getChildren().addAll(labels, mDetailTextArea);
    }

    /**
     * Updates the details for the currently selected channel.  Call on the JavaFX application thread.
     */
    public void refresh()
    {
        receive(mProcessingChain);
    }

    private static Label grow(Label label)
    {
        label.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(label, Priority.ALWAYS);
        return label;
    }

    /**
     * Shows the details for the processing chain.  Call on the JavaFX application thread.
     * @param processingChain to show, or null to clear the view
     */
    @Override
    public void receive(ProcessingChain processingChain)
    {
        mProcessingChain = processingChain;

        Channel channel = mChannelProcessingManager.getChannel(processingChain);
        mSystemLabel.setText(channel != null ? channel.getSystem() : null);
        mSiteLabel.setText(channel != null ? channel.getSite() : null);
        mNameLabel.setText(channel != null ?
            (channel.getChannelType() == Channel.ChannelType.TRAFFIC ? "Traffic Channel" : channel.getName()) : null);

        if(processingChain != null)
        {
            StringBuilder sb = new StringBuilder();

            for(DecoderState decoderState: processingChain.getDecoderStates())
            {
                sb.append(decoderState.getActivitySummary());
            }

            //Keep the scroll position, so that a refresh doesn't move the user back to the top
            double scrollTop = mDetailTextArea.getScrollTop();
            mDetailTextArea.setText(sb.toString());
            mDetailTextArea.setScrollTop(scrollTop);
        }
        else
        {
            mDetailTextArea.setText(EMPTY_DETAILS);
        }
    }
}
