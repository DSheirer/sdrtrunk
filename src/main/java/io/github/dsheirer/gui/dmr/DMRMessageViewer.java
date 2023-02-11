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

package io.github.dsheirer.gui.dmr;

import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.dmr.DMRMessageFramer;
import io.github.dsheirer.module.decode.dmr.DMRMessageProcessor;
import io.github.dsheirer.module.decode.dmr.DecodeConfigDMR;
import io.github.dsheirer.record.binary.BinaryReader;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.prefs.Preferences;
import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DMRMessageViewer extends Application
{
    private static final Logger mLog = LoggerFactory.getLogger(DMRMessageViewer.class);
    private static final String LAST_SELECTED_DIRECTORY = "last.selected.directory";
    private Preferences mPreferences = Preferences.userNodeForPackage(DMRMessageViewer.class);
    private Stage mStage;
    private Scene mScene;
    private VBox mContentBox;
    private Button mSelectFileButton;
    private Label mSelectedFileLabel;
    private TableView<IMessage> mMessageTableView;

    @Override
    public void start(Stage primaryStage) throws Exception
    {
        mStage = primaryStage;
        mStage.setTitle("Recording Message Viewer (.bits)");
        mStage.setScene(getScene());
        mStage.show();
    }

    /**
     * Processes the recording file and loads the content into the viewer
     * @param file containing a .bits recording of decoded DMR data.
     */
    private void load(File file)
    {
        if(file != null && file.exists())
        {
            getSelectedFileLabel().setText(file.getName());
            getMessageTableView().getItems().clear();

            DMRMessageFramer messageFramer = new DMRMessageFramer(null);
            DecodeConfigDMR config = new DecodeConfigDMR();
            DMRMessageProcessor messageProcessor = new DMRMessageProcessor(config);
            messageFramer.setListener(messageProcessor);
            messageProcessor.setMessageListener(message -> getMessageTableView().getItems().add(message));

            try(BinaryReader reader = new BinaryReader(file.toPath(), 200))
            {
                while(reader.hasNext())
                {
                    ByteBuffer buffer = reader.next();
                    messageFramer.receive(buffer);
                }
            }
            catch(Exception ioe)
            {
                ioe.printStackTrace();
            }
        }
    }

    /**
     * Scene for this application
     */
    private Scene getScene()
    {
        if(mScene == null)
        {
            mScene = new Scene(getContent(), 1100, 800);
        }

        return mScene;
    }

    /**
     * Primary content pane.
     */
    private VBox getContent()
    {
        if(mContentBox == null)
        {
            mContentBox = new VBox();
            mContentBox.setPadding(new Insets(5));
            mContentBox.setSpacing(5);
            HBox fileBox = new HBox();
            fileBox.setSpacing(5);
            HBox.setHgrow(getSelectFileButton(), Priority.NEVER);
            HBox.setHgrow(getSelectedFileLabel(), Priority.ALWAYS);
            getSelectedFileLabel().setAlignment(Pos.BASELINE_CENTER);
            fileBox.getChildren().addAll(getSelectFileButton(), getSelectedFileLabel());

            VBox.setVgrow(fileBox, Priority.NEVER);
            VBox.setVgrow(getMessageTableView(), Priority.ALWAYS);

            mContentBox.getChildren().addAll(fileBox, getMessageTableView());
        }

        return mContentBox;
    }

    /**
     * List view control with DMR messages
     */
    private TableView<IMessage> getMessageTableView()
    {
        if(mMessageTableView == null)
        {
            mMessageTableView = new TableView<>();

            TableColumn timeslotColumn = new TableColumn();
            timeslotColumn.setPrefWidth(35);
            timeslotColumn.setText("TS");
            timeslotColumn.setCellValueFactory(new PropertyValueFactory<>("timeslot"));

            TableColumn validColumn = new TableColumn();
            validColumn.setPrefWidth(50);
            validColumn.setText("Valid");
            validColumn.setCellValueFactory(new PropertyValueFactory<>("valid"));

            TableColumn messageColumn = new TableColumn();
            messageColumn.setPrefWidth(1000);
            messageColumn.setText("Message");
            messageColumn.setCellValueFactory((Callback<TableColumn.CellDataFeatures, ObservableValue>) param -> {
                SimpleStringProperty property = new SimpleStringProperty();
                if(param.getValue() instanceof IMessage message)
                {
                    property.set(message.toString());
                }

                return property;
            });

            mMessageTableView.getColumns().addAll(validColumn, timeslotColumn, messageColumn);
        }

        return mMessageTableView;
    }

    /**
     * File selection button
     * @return button
     */
    private Button getSelectFileButton()
    {
        if(mSelectFileButton == null)
        {
            mSelectFileButton = new Button("Select ...");
            mSelectFileButton.onActionProperty().set(event -> {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Select DMR .bits Recording");
                String lastDirectory = mPreferences.get(LAST_SELECTED_DIRECTORY, null);
                if(lastDirectory != null)
                {
                    File file = new File(lastDirectory);
                    if(file.exists() && file.isDirectory())
                    {
                        fileChooser.setInitialDirectory(file);
                    }
                }
                fileChooser.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("sdrtrunk bits recording", "*.bits"));
                File selected = fileChooser.showOpenDialog(mStage);

                if(selected != null)
                {
                    mLog.info("Last Selected Directory updated to [" + selected.getParent() +"]");
                    mPreferences.put(LAST_SELECTED_DIRECTORY, selected.getParent());
                    load(selected);
                }
            });
        }

        return mSelectFileButton;
    }

    /**
     * Selected file path label.
     */
    private Label getSelectedFileLabel()
    {
        if(mSelectedFileLabel == null)
        {
            mSelectedFileLabel = new Label();
        }

        return mSelectedFileLabel;
    }


    public static void main(String[] args)
    {
        launch(args);
    }
}
