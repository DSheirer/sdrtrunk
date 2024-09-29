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

import io.github.dsheirer.channel.state.DecoderStateEvent;
import io.github.dsheirer.module.decode.event.DecodeEventSnapshot;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * JavaFX message package details viewer
 */
public class MessagePackageViewer extends VBox
{
    private Label mMessageLabel;
    private TableView<DecoderStateEvent> mDecoderStateEventTableView;
    private TableView<DecodeEventSnapshot> mDecodeEventTableView;
    private IdentifierCollectionViewer mIdentifierCollectionViewer;
    private IdentifierCollectionViewer mAudioSegmentIdCollectionViewer;
    private ChannelStartProcessingRequestViewer mChannelStartProcessingRequestViewer;

    /**
     * Constructs an instance
     */
    public MessagePackageViewer()
    {
        GridPane gridPane = new GridPane();
        gridPane.setHgap(5);
        gridPane.setVgap(5);
        Label messageLabel = new Label("Message:");
        gridPane.add(messageLabel, 0, 0);
        GridPane.setHgrow(getMessageLabel(), Priority.ALWAYS);
        gridPane.add(getMessageLabel(), 1, 0);

        gridPane.add(new Label("Decoder State Events"), 0, 1);
        getDecoderStateEventTableView().setPrefHeight(120);
        GridPane.setHgrow(getDecoderStateEventTableView(), Priority.NEVER);
        gridPane.add(getDecoderStateEventTableView(), 0, 2);

        gridPane.add(new Label("Decode Events"), 1, 1);
        getDecodeEventTableView().setPrefHeight(120);
        GridPane.setHgrow(getDecodeEventTableView(), Priority.ALWAYS);
        gridPane.add(getDecodeEventTableView(), 1, 2);

        TabPane tabPane = new TabPane();
        Tab startTab = new Tab("Channel Start Processing Request");
        startTab.setContent(getChannelStartProcessingRequestViewer());
        Tab audioTab = new Tab("Audio Segment");
        audioTab.setContent(getAudioSegmentIdCollectionViewer());
        tabPane.getTabs().addAll(audioTab, startTab);

        gridPane.add(tabPane, 0, 4);

        getIdentifierCollectionViewer().setPrefHeight(120);
        gridPane.add(new Label("Selected Decode Event Identifiers"), 1, 3);
        gridPane.add(getIdentifierCollectionViewer(), 1, 4);

        getChildren().add(gridPane);
    }

    public void set(MessagePackage messagePackage)
    {
        getMessageLabel().setText(null);
        getDecoderStateEventTableView().getItems().clear();
        getDecodeEventTableView().getItems().clear();
        getChannelStartProcessingRequestViewer().set(null);

        if(messagePackage != null)
        {
            String message = messagePackage.toString();
            if(message.length() > 40)
            {
                message = message.substring(0, 40) + "...";
            }
            getMessageLabel().setText(message);
            getDecoderStateEventTableView().getItems().addAll(messagePackage.getDecoderStateEvents());
            getDecodeEventTableView().getItems().addAll(messagePackage.getDecodeEvents());
            getChannelStartProcessingRequestViewer().set(messagePackage.getChannelStartProcessingRequest());

            if(getDecodeEventTableView().getItems().size() > 0)
            {
                getDecodeEventTableView().getSelectionModel().select(0);
            }

            if(messagePackage.getAudioSegment() != null)
            {
                getAudioSegmentIdCollectionViewer().set(messagePackage.getAudioSegment().getIdentifierCollection());
            }
            else
            {
                getAudioSegmentIdCollectionViewer().set(null);
            }
        }
    }

    private IdentifierCollectionViewer getIdentifierCollectionViewer()
    {
        if(mIdentifierCollectionViewer == null)
        {
            mIdentifierCollectionViewer = new IdentifierCollectionViewer();
        }

        return mIdentifierCollectionViewer;
    }

    private IdentifierCollectionViewer getAudioSegmentIdCollectionViewer()
    {
        if(mAudioSegmentIdCollectionViewer == null)
        {
            mAudioSegmentIdCollectionViewer = new IdentifierCollectionViewer();
        }

        return mAudioSegmentIdCollectionViewer;
    }

    private Label getMessageLabel()
    {
        if(mMessageLabel == null)
        {
            mMessageLabel = new Label();
            mMessageLabel.setMaxWidth(Double.MAX_VALUE);
        }

        return mMessageLabel;
    }

    public TableView<DecoderStateEvent> getDecoderStateEventTableView()
    {
        if(mDecoderStateEventTableView == null)
        {
            mDecoderStateEventTableView = new TableView<>();

            TableColumn timeslotColumn = new TableColumn();
            timeslotColumn.setPrefWidth(110);
            timeslotColumn.setText("Timeslot");
            timeslotColumn.setCellValueFactory(new PropertyValueFactory<>("timeslot"));

            TableColumn stateColumn = new TableColumn();
            stateColumn.setPrefWidth(110);
            stateColumn.setText("State");
            stateColumn.setCellValueFactory(new PropertyValueFactory<>("state"));

            TableColumn eventColumn = new TableColumn();
            eventColumn.setPrefWidth(110);
            eventColumn.setText("Event");
            eventColumn.setCellValueFactory(new PropertyValueFactory<>("event"));

            TableColumn frequencyColumn = new TableColumn();
            frequencyColumn.setPrefWidth(110);
            frequencyColumn.setText("Frequency");
            frequencyColumn.setCellValueFactory(new PropertyValueFactory<>("frequency"));

            mDecoderStateEventTableView.getColumns().addAll(timeslotColumn, stateColumn, eventColumn, frequencyColumn);
        }

        return mDecoderStateEventTableView;
    }

    private TableView<DecodeEventSnapshot> getDecodeEventTableView()
    {
        if(mDecodeEventTableView == null)
        {
            mDecodeEventTableView = new TableView<>();
            mDecodeEventTableView.setMaxWidth(Double.MAX_VALUE);

            TableColumn startTimeColumn = new TableColumn();
            startTimeColumn.setPrefWidth(110);
            startTimeColumn.setText("Start");
            startTimeColumn.setCellValueFactory(new PropertyValueFactory<>("timeStart"));

            TableColumn durationColumn = new TableColumn();
            durationColumn.setPrefWidth(110);
            durationColumn.setText("Duration");
            durationColumn.setCellValueFactory(new PropertyValueFactory<>("duration"));

            TableColumn typeColumn = new TableColumn();
            typeColumn.setPrefWidth(130);
            typeColumn.setText("Type");
            typeColumn.setCellValueFactory(new PropertyValueFactory<>("eventType"));

            TableColumn channelDescriptorColumn = new TableColumn();
            channelDescriptorColumn.setPrefWidth(110);
            channelDescriptorColumn.setText("Channel");
            channelDescriptorColumn.setCellValueFactory(new PropertyValueFactory<>("channelDescriptor"));

            TableColumn frequencyColumn = new TableColumn();
            frequencyColumn.setPrefWidth(110);
            frequencyColumn.setText("Frequency");
            frequencyColumn.setCellValueFactory(new PropertyValueFactory<>("frequency"));

            TableColumn hashcodeColumn = new TableColumn();
            hashcodeColumn.setPrefWidth(100);
            hashcodeColumn.setText("Hash ID");
            hashcodeColumn.setCellValueFactory(new PropertyValueFactory<>("originalHashCode"));

            TableColumn detailsColumn = new TableColumn();
            detailsColumn.setPrefWidth(500);
            detailsColumn.setText("Details");
            detailsColumn.setCellValueFactory(new PropertyValueFactory<>("details"));

            mDecodeEventTableView.getColumns().addAll(startTimeColumn, durationColumn, typeColumn, channelDescriptorColumn,
                    frequencyColumn, hashcodeColumn, detailsColumn);

            mDecodeEventTableView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) ->
            {
                if(newValue != null)
                {
                    getIdentifierCollectionViewer().set(newValue.getIdentifierCollection());
                }
                else
                {
                    getIdentifierCollectionViewer().set(null);
                }
            });
        }

        return mDecodeEventTableView;
    }

    private ChannelStartProcessingRequestViewer getChannelStartProcessingRequestViewer()
    {
        if(mChannelStartProcessingRequestViewer == null)
        {
            mChannelStartProcessingRequestViewer = new ChannelStartProcessingRequestViewer();
        }

        return mChannelStartProcessingRequestViewer;
    }
}
