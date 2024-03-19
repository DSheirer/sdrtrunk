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

package io.github.dsheirer.gui.viewer;

import io.github.dsheirer.controller.channel.event.ChannelStartProcessingRequest;
import javafx.geometry.HPos;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/**
 * Viewer for channel start processing requests
 */
public class ChannelStartProcessingRequestViewer extends HBox
{
    private Label mChannelConfigLabel;
    private Label mChannelDescriptorLabel;
    private Label mTrafficChannelManagerLabel;
    private Label mPreloadDataContentLabel;
    private Label mParentDecodeHistoryLabel;
    private Label mChildDecodeHistoryLabel;
    private IdentifierCollectionViewer mIdentifierCollectionViewer;

    /**
     * Constructs an instance
     */
    public ChannelStartProcessingRequestViewer()
    {
        GridPane gridPane = new GridPane();
        gridPane.setVgap(5);
        gridPane.setHgap(5);

        gridPane.add(new Label("Configuration:"), 0, 0);
        GridPane.setHgrow(getChannelConfigLabel(), Priority.ALWAYS);
        gridPane.add(getChannelConfigLabel(), 1, 0);

        gridPane.add(new Label("Channel:"), 0, 1);
        GridPane.setHgrow(getChannelDescriptorLabel(), Priority.ALWAYS);
        gridPane.add(getChannelDescriptorLabel(), 1, 1);

        gridPane.add(new Label("TCM:"), 0, 2);
        GridPane.setHgrow(getTrafficChannelManagerLabel(), Priority.ALWAYS);
        gridPane.add(getTrafficChannelManagerLabel(), 1, 2);

        gridPane.add(new Label("Preload Items:"), 0, 3);
        GridPane.setHgrow(getPreloadDataContentLabel(), Priority.ALWAYS);
        gridPane.add(getPreloadDataContentLabel(), 1, 3);

        gridPane.add(new Label("Parent History:"), 0, 4);
        GridPane.setHgrow(getParentDecodeHistoryLabel(), Priority.ALWAYS);
        gridPane.add(getParentDecodeHistoryLabel(), 1, 4);

        gridPane.add(new Label("Child History:"), 0, 5);
        GridPane.setHgrow(getChildDecodeHistoryLabel(), Priority.ALWAYS);
        gridPane.add(getChildDecodeHistoryLabel(), 1, 5);

        gridPane.add(new Label("Identifiers"), 0, 6);
        GridPane.setHgrow(getIdentifierCollectionViewer(), Priority.ALWAYS);
        getIdentifierCollectionViewer().setPrefHeight(120);
        gridPane.add(getIdentifierCollectionViewer(), 0, 7, 2, 1);

        ColumnConstraints cc0 = new ColumnConstraints();
        cc0.setHalignment(HPos.RIGHT);
        gridPane.getColumnConstraints().add(cc0);

        getChildren().add(gridPane);
    }

    public void set(ChannelStartProcessingRequest request)
    {
        if(request != null)
        {
            if(request.getChannel() != null)
            {
                getChannelConfigLabel().setText(request.getChannel().toString());
            }
            else
            {
                getChannelConfigLabel().setText("None");
            }

            if(request.getChannelDescriptor() != null)
            {
                getChannelDescriptorLabel().setText(request.getChannelDescriptor().toString() + " " +
                        request.getChannelDescriptor().getDownlinkFrequency() + " MHz");
            }
            else
            {
                getChannelDescriptorLabel().setText("None");
            }

            if(request.getChildDecodeEventHistory() != null)
            {
                getChildDecodeHistoryLabel().setText(request.getChildDecodeEventHistory().getItems().size() + " items");
            }
            else
            {
                getChildDecodeHistoryLabel().setText("None");
            }

            if(request.getParentDecodeEventHistory() != null)
            {
                getParentDecodeHistoryLabel().setText(request.getParentDecodeEventHistory().getItems().size() + " items");
            }
            else
            {
                getParentDecodeHistoryLabel().setText("None");
            }

            if(request.getPreloadDataContents() != null)
            {
                getPreloadDataContentLabel().setText(request.getPreloadDataContents().size() + " items");
            }
            else
            {
                getPreloadDataContentLabel().setText("None");
            }

            if(request.getTrafficChannelManager() != null)
            {
                getTrafficChannelManagerLabel().setText(request.getTrafficChannelManager().getClass().getName());
            }
            else
            {
                getTrafficChannelManagerLabel().setText("None:");
            }

            if(request.getIdentifierCollection() != null)
            {
                getIdentifierCollectionViewer().set(request.getIdentifierCollection());
            }
        }
        else
        {
            getChannelDescriptorLabel().setText(null);
            getChildDecodeHistoryLabel().setText(null);
            getChannelConfigLabel().setText(null);
            getIdentifierCollectionViewer().set(null);
            getParentDecodeHistoryLabel().setText(null);
            getPreloadDataContentLabel().setText(null);
            getTrafficChannelManagerLabel().setText(null);
        }
    }

    public Label getChannelConfigLabel()
    {
        if(mChannelConfigLabel == null)
        {
            mChannelConfigLabel = new Label();
        }

        return mChannelConfigLabel;
    }

    public Label getChannelDescriptorLabel()
    {
        if(mChannelDescriptorLabel == null)
        {
            mChannelDescriptorLabel = new Label();
        }

        return mChannelDescriptorLabel;
    }

    private Label getTrafficChannelManagerLabel()
    {
        if(mTrafficChannelManagerLabel == null)
        {
            mTrafficChannelManagerLabel = new Label();

        }

        return mTrafficChannelManagerLabel;
    }

    private Label getPreloadDataContentLabel()
    {
        if(mPreloadDataContentLabel == null)
        {
            mPreloadDataContentLabel = new Label();
        }

        return mPreloadDataContentLabel;
    }

    private Label getParentDecodeHistoryLabel()
    {
        if(mParentDecodeHistoryLabel == null)
        {
            mParentDecodeHistoryLabel = new Label();
        }

        return mParentDecodeHistoryLabel;
    }

    private Label getChildDecodeHistoryLabel()
    {
        if(mChildDecodeHistoryLabel == null)
        {
            mChildDecodeHistoryLabel = new Label();
        }

        return mChildDecodeHistoryLabel;
    }

    private IdentifierCollectionViewer getIdentifierCollectionViewer()
    {
        if(mIdentifierCollectionViewer == null)
        {
            mIdentifierCollectionViewer = new IdentifierCollectionViewer();
        }

        return mIdentifierCollectionViewer;
    }
}
