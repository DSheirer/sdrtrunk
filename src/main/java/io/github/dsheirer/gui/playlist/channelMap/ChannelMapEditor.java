/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.gui.playlist.channelMap;

import io.github.dsheirer.controller.channel.map.ChannelMap;
import io.github.dsheirer.controller.channel.map.ChannelMapModel;
import io.github.dsheirer.controller.channel.map.ChannelRange;
import io.github.dsheirer.gui.control.IntegerTextField;
import io.github.dsheirer.gui.playlist.Editor;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Callback;

import java.util.Optional;

/**
 * JavaFX editor for channel maps
 */
public class ChannelMapEditor extends SplitPane
{
    private static final String COPY_NAME = " (copy)";
    private ChannelMapModel mChannelMapModel;
    private SplitPane mSplitPane;
    private MapEditor mMapEditor;
    private HBox mChannelMapManagerBox;
    private VBox mMapButtonsBox;
    private Button mNewMapButton;
    private Button mCloneMapButton;
    private Button mDeleteMapButton;
    private Button mHelpButton;
    private ListView<ChannelMap> mChannelMapListView;
    private GridPane mChannelRangePane;
    private IntegerTextField mFirstField;
    private IntegerTextField mLastField;
    private IntegerTextField mBaseFrequencyField;
    private IntegerTextField mStepSizeField;

    /**
     * Constructs an instance
     *
     * @param channelMapModel for accessing channel maps
     */
    public ChannelMapEditor(ChannelMapModel channelMapModel)
    {
        mChannelMapModel = channelMapModel;
        setOrientation(Orientation.VERTICAL);
        getItems().addAll(getChannelMapManagerBox(), getMapEditor(), getChannelRangePane());
    }

    /**
     * Top pane for selecting and managing channel maps in the list view
     */
    private HBox getChannelMapManagerBox()
    {
        if(mChannelMapManagerBox == null)
        {
            mChannelMapManagerBox = new HBox();
            HBox.setHgrow(getChannelMapListView(), Priority.ALWAYS);
            mChannelMapManagerBox.getChildren().addAll(getChannelMapListView(), getMapButtonsBox());
        }

        return mChannelMapManagerBox;
    }

    private VBox getMapButtonsBox()
    {
        if(mMapButtonsBox == null)
        {
            mMapButtonsBox = new VBox();
            mMapButtonsBox.setPadding(new Insets(10, 10, 10, 10));
            mMapButtonsBox.setSpacing(10);
            mMapButtonsBox.getChildren().addAll(getNewMapButton(), getCloneMapButton(), getDeleteMapButton(), getHelpButton());
        }

        return mMapButtonsBox;
    }

    private Button getNewMapButton()
    {
        if(mNewMapButton == null)
        {
            mNewMapButton = new Button("New Map");
            mNewMapButton.setTooltip(new Tooltip("Create a new channel map"));
            mNewMapButton.setMaxWidth(Double.MAX_VALUE);
            mNewMapButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    ChannelMap channelMap = new ChannelMap("New Channel Map");
                    mChannelMapModel.addChannelMap(channelMap);
                    getChannelMapListView().getSelectionModel().select(channelMap);
                }
            });
        }

        return mNewMapButton;
    }

    private Button getCloneMapButton()
    {
        if(mCloneMapButton == null)
        {
            mCloneMapButton = new Button("Clone Map");
            mCloneMapButton.setTooltip(new Tooltip("Create a copy of the selected channel map"));
            mCloneMapButton.setDisable(true);
            mCloneMapButton.setMaxWidth(Double.MAX_VALUE);
            mCloneMapButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    ChannelMap selected = getChannelMapListView().getSelectionModel().getSelectedItem();

                    if(selected != null)
                    {
                        ChannelMap copy = selected.copyOf();

                        String name = selected.getName();

                        if(name == null)
                        {
                            name = COPY_NAME;
                        }
                        else if(!name.toLowerCase().endsWith(COPY_NAME))
                        {
                            name = name + COPY_NAME;
                        }

                        copy.setName(name);
                        mChannelMapModel.addChannelMap(copy);
                        getChannelMapListView().getSelectionModel().select(copy);
                    }
                }
            });
        }

        return mCloneMapButton;
    }

    private void updateChannelMapButtons()
    {
        boolean selected = getChannelMapListView().getSelectionModel().getSelectedItem() != null;
        getCloneMapButton().setDisable(!selected);
        getDeleteMapButton().setDisable(!selected);
    }

    private Button getDeleteMapButton()
    {
        if(mDeleteMapButton == null)
        {
            mDeleteMapButton = new Button("Delete Map");
            mDeleteMapButton.setTooltip(new Tooltip("Delete the selected channel map"));
            mDeleteMapButton.setDisable(true);
            mDeleteMapButton.setMaxWidth(Double.MAX_VALUE);
            mDeleteMapButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                        "Do you want to delete the selected channel map?", ButtonType.NO, ButtonType.YES);
                    alert.setTitle("Delete Channel Map");
                    alert.setHeaderText("Are you sure?");
                    alert.initOwner(((Node)getDeleteMapButton()).getScene().getWindow());

                    Optional<ButtonType> result = alert.showAndWait();

                    if(result.get() == ButtonType.YES)
                    {
                        ChannelMap selected = getChannelMapListView().getSelectionModel().getSelectedItem();

                        if(selected != null)
                        {
                            mChannelMapModel.removeChannelMap(selected);
                            updateChannelMapButtons();
                        }
                    }
                }
            });
        }

        return mDeleteMapButton;
    }

    private Button getHelpButton()
    {
        if(mHelpButton == null)
        {
            mHelpButton = new Button("Help");
            mHelpButton.setMaxWidth(Double.MAX_VALUE);
            mHelpButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);

                    Text text1 = new Text("Channel maps are used by trunking systems to define the transmit frequency " +
                        "used for each channel number.  A channel map is required so that the frequency for each call " +
                        "channel can be calculated correctly");
                    text1.setWrappingWidth(300);
                    Text text2 = new Text("A channel map contains one or more channel ranges.  Each range has a first " +
                        "and last channel number and the channel numbers for each range do NOT overlap.  Each range " +
                        "also has a base frequency and a step or channel size");
                    text2.setWrappingWidth(300);
                    Text text3 = new Text("The frequency for each channel number is calculated as: base frequency + " +
                        "(last - first * stepSize)");
                    text3.setWrappingWidth(300);
                    VBox vbox = new VBox();
                    vbox.setPadding(new Insets(10, 10, 10, 10));
                    vbox.setSpacing(10);
                    vbox.getChildren().addAll(text1, text2, text3);
                    alert.getDialogPane().setContent(vbox);
                    alert.setTitle("Channel Map Help");
                    alert.setHeaderText("Channel Map Help");
                    alert.initOwner(((Node)getHelpButton()).getScene().getWindow());

                    //Workaround for JavaFX KDE on Linux bug in FX 10/11: https://bugs.openjdk.java.net/browse/JDK-8179073
                    alert.setResizable(true);
                    alert.onShownProperty().addListener(e -> {
                        Platform.runLater(() -> alert.setResizable(false));
                    });

                    alert.showAndWait();
                }
            });
        }

        return mHelpButton;
    }

    private ListView<ChannelMap> getChannelMapListView()
    {
        if(mChannelMapListView == null)
        {
            mChannelMapListView = new ListView<>();
            mChannelMapListView.setItems(mChannelMapModel.getChannelMaps());
            mChannelMapListView.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
                    getMapEditor().setItem(newValue);
                    updateChannelMapButtons();
                });
        }

        return mChannelMapListView;
    }

    private MapEditor getMapEditor()
    {
        if(mMapEditor == null)
        {
            mMapEditor = new MapEditor();
        }

        return mMapEditor;
    }

    private ChannelRange getSelectedChannelRange()
    {
        return getMapEditor().getChannelRangeTableView().getSelectionModel().getSelectedItem();
    }

    private IntegerTextField getFirstField()
    {
        if(mFirstField == null)
        {
            mFirstField = new IntegerTextField();
            mFirstField.setDisable(true);
            mFirstField.textProperty()
                .addListener((observable, oldValue, newValue) -> {
                    if(getSelectedChannelRange() != null)
                    {
                        Integer value = getFirstField().get();
                        getSelectedChannelRange().setFirstChannelNumber(value != null ? value : 0);
                        getMapEditor().modifiedProperty().set(true);
                    }
                });
        }

        return mFirstField;
    }

    private IntegerTextField getLastField()
    {
        if(mLastField == null)
        {
            mLastField = new IntegerTextField();
            mLastField.setDisable(true);
            mLastField.textProperty()
                .addListener((observable, oldValue, newValue) -> {
                    if(getSelectedChannelRange() != null)
                    {
                        Integer value = getLastField().get();
                        getSelectedChannelRange().setLastChannelNumber(value != null ? value : 0);
                        getMapEditor().modifiedProperty().set(true);
                    }
                });
        }

        return mLastField;
    }

    private IntegerTextField getBaseFrequencyField()
    {
        if(mBaseFrequencyField == null)
        {
            mBaseFrequencyField = new IntegerTextField();
            mBaseFrequencyField.setDisable(true);
            mBaseFrequencyField.textProperty().addListener((observable, oldValue, newValue) -> {
                    if(getSelectedChannelRange() != null)
                    {
                        Integer value = getBaseFrequencyField().get();
                        getSelectedChannelRange().setBaseFrequency(value != null ? value : 0);
                        getMapEditor().modifiedProperty().set(true);
                    }
                });
        }

        return mBaseFrequencyField;
    }

    private IntegerTextField getStepSizeField()
    {
        if(mStepSizeField == null)
        {
            mStepSizeField = new IntegerTextField();
            mStepSizeField.setDisable(true);
            mStepSizeField.textProperty().addListener((observable, oldValue, newValue) -> {
                    if(getSelectedChannelRange() != null)
                    {
                        Integer value = getStepSizeField().get();
                        getSelectedChannelRange().setStepSize(value != null ? value : 0);
                        getMapEditor().modifiedProperty().set(true);
                    }
                });
        }

        return mStepSizeField;
    }

    private GridPane getChannelRangePane()
    {
        if(mChannelRangePane == null)
        {
            mChannelRangePane = new GridPane();
            mChannelRangePane.setPadding(new Insets(10, 10, 10,10));
            mChannelRangePane.setVgap(10);
            mChannelRangePane.setHgap(5);

            Label firstLabel = new Label("First Channel");
            GridPane.setHalignment(firstLabel, HPos.RIGHT);
            GridPane.setConstraints(firstLabel, 0, 0);
            mChannelRangePane.getChildren().add(firstLabel);

            GridPane.setConstraints(getFirstField(), 1, 0);
            GridPane.setHgrow(getFirstField(), Priority.ALWAYS);
            mChannelRangePane.getChildren().add(getFirstField());

            Label lastLabel = new Label("Last Channel");
            GridPane.setHalignment(lastLabel, HPos.RIGHT);
            GridPane.setConstraints(lastLabel, 0, 1);
            mChannelRangePane.getChildren().add(lastLabel);

            GridPane.setConstraints(getLastField(), 1, 1);
            GridPane.setHgrow(getLastField(), Priority.ALWAYS);
            mChannelRangePane.getChildren().add(getLastField());

            Label baseLabel = new Label("Base Frequency (Hz)");
            GridPane.setHalignment(baseLabel, HPos.RIGHT);
            GridPane.setConstraints(baseLabel, 0, 2);
            mChannelRangePane.getChildren().add(baseLabel);

            GridPane.setConstraints(getBaseFrequencyField(), 1, 2);
            GridPane.setHgrow(getBaseFrequencyField(), Priority.ALWAYS);
            mChannelRangePane.getChildren().add(getBaseFrequencyField());

            Label stepSizeLabel = new Label("Step Size (Hz)");
            GridPane.setHalignment(stepSizeLabel, HPos.RIGHT);
            GridPane.setConstraints(stepSizeLabel, 0, 3);
            mChannelRangePane.getChildren().add(stepSizeLabel);

            GridPane.setConstraints(getStepSizeField(), 1, 3);
            GridPane.setHgrow(getStepSizeField(), Priority.ALWAYS);
            mChannelRangePane.getChildren().add(getStepSizeField());
        }

        return mChannelRangePane;
    }

    public class MapEditor extends Editor<ChannelMap>
    {
        private Button mSaveButton;
        private Button mResetButton;
        private Button mNewRangeButton;
        private Button mDeleteRangeButton;
        private TableView<ChannelRange> mChannelRangeTableView;
        private TextField mNameField;

        public MapEditor()
        {
            HBox nameBox = new HBox();
            nameBox.setPadding(new Insets(10, 0, 10, 10));
            nameBox.setSpacing(10);
            nameBox.setAlignment(Pos.CENTER);
            HBox.setHgrow(getNameField(), Priority.ALWAYS);
            nameBox.getChildren().addAll(new Label("Name"), getNameField());

            VBox contentsBox = new VBox();
            VBox.setVgrow(nameBox, Priority.NEVER);
            VBox.setVgrow(getChannelRangeTableView(), Priority.ALWAYS);
            contentsBox.getChildren().addAll(nameBox, getChannelRangeTableView());

            VBox buttonsBox = new VBox();
            buttonsBox.setPadding(new Insets(10, 10, 10, 10));
            buttonsBox.setSpacing(10);
            buttonsBox.getChildren().addAll(getSaveButton(), getResetButton(), new Separator(),
                getNewRangeButton(), getDeleteRangeButton());

            HBox hbox = new HBox();
            HBox.setHgrow(contentsBox, Priority.ALWAYS);
            hbox.getChildren().addAll(contentsBox, buttonsBox);

            getChildren().add(hbox);

            modifiedProperty().addListener((observable, oldValue, modified) -> {
                getSaveButton().setDisable(!modified);
                getResetButton().setDisable(!modified);
            });
        }

        private void updateChannelRangeButtons()
        {
            getNewRangeButton().setDisable(getItem() == null);
            getDeleteRangeButton().setDisable(getChannelRangeTableView().getSelectionModel().getSelectedItem() == null);
        }

        @Override
        public void save()
        {
            ChannelRange selectedChannelRange = getChannelRangeTableView().getSelectionModel().getSelectedItem();

            if(getItem() != null)
            {
                getItem().nameProperty().set(getNameField().getText());
                getItem().getItems().clear();
                getItem().getItems().addAll(getChannelRangeTableView().getItems());
            }

            modifiedProperty().set(false);


            //Hack - the colorized table cells weren't updating after the channel map was validated
            setItem(getItem());

            if(getItem() != null && !getItem().isValid())
            {
                Alert alert = new Alert(Alert.AlertType.WARNING);

                Text text1 = new Text("Channel range(s) have errors that must be fixed:");
                text1.setWrappingWidth(300);
                Text text2 = new Text("- Verify that the first channel number is less than the last channel number");
                text2.setWrappingWidth(300);
                Text text3 = new Text("- Verify that channel numbers in each range do not overlap with other channel ranges.");
                text3.setWrappingWidth(300);
                VBox vbox = new VBox();
                vbox.setPadding(new Insets(10, 10, 10, 10));
                vbox.setSpacing(10);
                vbox.getChildren().addAll(text1, text2, text3);
                alert.getDialogPane().setContent(vbox);
                alert.setTitle("Channel Map Error");
                alert.setHeaderText("Channel Map has errors");
                alert.initOwner(((Node)getSaveButton()).getScene().getWindow());

                //Workaround for JavaFX KDE on Linux bug in FX 10/11: https://bugs.openjdk.java.net/browse/JDK-8179073
                alert.setResizable(true);
                alert.onShownProperty().addListener(e -> {
                    Platform.runLater(() -> alert.setResizable(false));
                });

                alert.showAndWait();

                for(ChannelRange channelRange: getItem().getRanges())
                {
                    if(channelRange.isOverlapping() || !channelRange.isValid())
                    {
                        getChannelRangeTableView().getSelectionModel().select(channelRange);
                        break;
                    }
                }

            }

            //Re-select the channel range that was selected before the save
            getChannelRangeTableView().getSelectionModel().select(selectedChannelRange);
        }

        @Override
        public void dispose()
        {
            //no-op
        }

        @Override
        public void setItem(ChannelMap item)
        {
            if(modifiedProperty().get())
            {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.getButtonTypes().clear();
                alert.getButtonTypes().addAll(ButtonType.NO, ButtonType.YES);
                alert.setTitle("Save Changes");
                alert.setHeaderText("Channel Map has been modified");
                alert.setContentText("Do you want to save these changes?");
                alert.initOwner(((Node)getSaveButton()).getScene().getWindow());

                Optional<ButtonType> result = alert.showAndWait();

                if(result.get() == ButtonType.YES)
                {
                    save();
                }
            }

            super.setItem(item);

            getNameField().setDisable(item == null);
            getChannelRangeTableView().getItems().clear();
            getChannelRangeTableView().setDisable(item == null);

            if(item != null)
            {
                getNameField().setText(item.getName());
                getChannelRangeTableView().getItems().addAll(item.getRanges());
            }

            updateChannelRangeButtons();

            modifiedProperty().set(false);
        }

        private TextField getNameField()
        {
            if(mNameField == null)
            {
                mNameField = new TextField();
                mNameField.setMaxWidth(Double.MAX_VALUE);
                mNameField.setDisable(true);
                mNameField.textProperty().addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
            }

            return mNameField;
        }


        private TableView<ChannelRange> getChannelRangeTableView()
        {
            if(mChannelRangeTableView == null)
            {
                mChannelRangeTableView = new TableView<>();

                TableColumn<ChannelRange,Integer> firstColumn = new TableColumn("First");
                firstColumn.setCellValueFactory(new PropertyValueFactory<ChannelRange,Integer>("firstChannel"));
                firstColumn.setCellFactory(param -> new ColorTableCell());

                TableColumn<ChannelRange,Integer> lastColumn = new TableColumn("Last");
                lastColumn.setCellValueFactory(new PropertyValueFactory<ChannelRange,Integer>("lastChannel"));
                lastColumn.setCellFactory(param -> new ColorTableCell());

                TableColumn<ChannelRange,Integer> baseColumn = new TableColumn("Base");
                baseColumn.setCellValueFactory(new PropertyValueFactory<ChannelRange,Integer>("baseFrequency"));
                baseColumn.setPrefWidth(95);

                TableColumn<ChannelRange,Integer> stepColumn = new TableColumn("Step");
                stepColumn.setCellValueFactory(new PropertyValueFactory<ChannelRange,Integer>("stepSize"));

                mChannelRangeTableView.getColumns().addAll(firstColumn, lastColumn, baseColumn, stepColumn);

                mChannelRangeTableView.getSelectionModel().selectedItemProperty()
                    .addListener((observable, oldValue, selected) -> {
                    getFirstField().setDisable(selected == null);
                    getLastField().setDisable(selected == null);
                    getBaseFrequencyField().setDisable(selected == null);
                    getStepSizeField().setDisable(selected == null);

                    //Capture the current modified state and reapply it after updating the editor controls
                    boolean modified = modifiedProperty().get();

                    if(selected != null)
                    {
                        getFirstField().set(selected.getFirstChannelNumber());
                        getLastField().set(selected.getLastChannelNumber());
                        getBaseFrequencyField().set(selected.getBaseFrequency());
                        getStepSizeField().set(selected.getStepSize());
                    }
                    else
                    {
                        getFirstField().setText("");
                        getLastField().setText("");
                        getBaseFrequencyField().setText("");
                        getStepSizeField().setText("");
                    }

                    updateChannelRangeButtons();

                    modifiedProperty().set(modified);
                });
            }

            return mChannelRangeTableView;
        }

        private Button getSaveButton()
        {
            if(mSaveButton == null)
            {
                mSaveButton = new Button("Save");
                mSaveButton.setTooltip(new Tooltip("Save changes to the Channel Map"));
                mSaveButton.setMaxWidth(Double.MAX_VALUE);
                mSaveButton.setDisable(true);
                mSaveButton.setOnAction(event -> save());
            }

            return mSaveButton;
        }

        private Button getResetButton()
        {
            if(mResetButton == null)
            {
                mResetButton = new Button("Reset");
                mResetButton.setTooltip(new Tooltip("Undo any changes made to the channel map"));
                mResetButton.setMaxWidth(Double.MAX_VALUE);
                mResetButton.setDisable(true);
                mResetButton.setOnAction(event -> {
                    modifiedProperty().set(false);
                    setItem(getItem());
                });
            }

            return mResetButton;
        }

        private Button getNewRangeButton()
        {
            if(mNewRangeButton == null)
            {
                mNewRangeButton = new Button("New Range");
                mNewMapButton.setTooltip(new Tooltip("Add a new channel range to the channel map"));
                mNewRangeButton.setMaxWidth(Double.MAX_VALUE);
                mNewRangeButton.setDisable(true);
                mNewRangeButton.setOnAction(event -> {
                    if(getItem() != null)
                    {
                        ChannelRange channelRange = new ChannelRange();
                        getChannelRangeTableView().getItems().add(channelRange);
                        getChannelRangeTableView().getSelectionModel().select(channelRange);
                        modifiedProperty().set(true);
                    }
                });
            }

            return mNewRangeButton;
        }

        private Button getDeleteRangeButton()
        {
            if(mDeleteRangeButton == null)
            {
                mDeleteRangeButton = new Button("Delete Range");
                mDeleteRangeButton.setTooltip(new Tooltip("Delete the currently selected channel range"));
                mDeleteRangeButton.setMaxWidth(Double.MAX_VALUE);
                mDeleteRangeButton.setDisable(true);
                mDeleteRangeButton.setOnAction(new EventHandler<ActionEvent>()
                {
                    @Override
                    public void handle(ActionEvent event)
                    {
                        if(getItem() != null)
                        {
                            ChannelRange selected = getChannelRangeTableView().getSelectionModel().getSelectedItem();

                            if(selected != null)
                            {
                                Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                                    "Do you want to delete the selected channel range?", ButtonType.NO, ButtonType.YES);
                                alert.setTitle("Delete Channel Range");
                                alert.setHeaderText("Are you sure?");
                                alert.initOwner(((Node)getDeleteRangeButton()).getScene().getWindow());

                                Optional<ButtonType> result = alert.showAndWait();

                                if(result.get() == ButtonType.YES)
                                {
                                    getChannelRangeTableView().getItems().remove(selected);
                                    updateChannelRangeButtons();
                                    modifiedProperty().set(true);
                                }
                            }
                        }
                    }
                });
            }

            return mDeleteRangeButton;
        }
    }

    /**
     * Table column factory for channel ranges to indicate errors in channel ranges.
     */
    public class ChannelNumberCell implements Callback<TableColumn<ChannelRange,Integer>,TableCell<ChannelRange,Integer>>
    {
        @Override
        public TableCell<ChannelRange,Integer> call(TableColumn<ChannelRange,Integer> param)
        {
            return new ColorTableCell();
        }
    }

    /**
     * Custom table cell that shows a red background when the channel range is invalid or overlapping
     */
    public class ColorTableCell extends TableCell<ChannelRange,Integer>
    {
        private static final String BACKGROUND_COLOR_RED = "-fx-background-color: red;";
        private static final String BACKGROND_COLOR_NONE = "-fx-background-color: null;";

        public ColorTableCell()
        {
        }

        @Override
        protected void updateItem(Integer item, boolean empty)
        {
            super.updateItem(item, empty);

            if(item == null || empty)
            {
                setText(null);
            }
            else
            {
                setText(item.toString());
            }

            boolean invalidRange = getTableRow().getItem() != null &&
                (!getTableRow().getItem().isValid() || getTableRow().getItem().isOverlapping() ||
                    item == null || empty);

            this.setStyle(invalidRange ? BACKGROUND_COLOR_RED : BACKGROND_COLOR_NONE);
        }

        @Override
        protected boolean isItemChanged(Integer oldItem, Integer newItem)
        {
            return super.isItemChanged(oldItem, newItem);
        }
    }

    /**
     * Processes a request to show/edit a channel map
     */
    public void process(ViewChannelMapEditorRequest request)
    {
        String channelMapName = request.getChannelMapName();

        if(channelMapName != null)
        {
            for(ChannelMap channelMap: getChannelMapListView().getItems())
            {
                if(channelMapName.contentEquals(channelMap.getName()))
                {
                    getChannelMapListView().getSelectionModel().select(channelMap);
                    break;
                }
            }
        }
    }
}
