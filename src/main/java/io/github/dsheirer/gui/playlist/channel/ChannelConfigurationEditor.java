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

package io.github.dsheirer.gui.playlist.channel;

import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.ChannelException;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.gui.control.MaxLengthUnaryOperator;
import io.github.dsheirer.gui.playlist.Editor;
import io.github.dsheirer.gui.preference.PreferenceEditorType;
import io.github.dsheirer.gui.preference.ViewUserPreferenceEditorRequest;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.config.AuxDecodeConfiguration;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.module.log.config.EventLogConfiguration;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.record.config.RecordConfiguration;
import io.github.dsheirer.source.config.SourceConfigTuner;
import io.github.dsheirer.source.config.SourceConfiguration;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.util.ThreadPool;
import java.util.Optional;
import java.util.function.Predicate;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.javafx.IconNode;
import org.controlsfx.control.ToggleSwitch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Channel configuration editor
 */
public abstract class ChannelConfigurationEditor extends Editor<Channel>
{
    private final static Logger mLog = LoggerFactory.getLogger(ChannelConfigurationEditor.class);

    private PlaylistManager mPlaylistManager;
    protected TunerManager mTunerManager;
    protected UserPreferences mUserPreferences;
    protected EditorModificationListener mEditorModificationListener = new EditorModificationListener();
    private Button mPlayButton;
    private TextField mSystemField;
    private TextField mSiteField;
    private TextField mNameField;
    private ComboBox<String> mAliasListComboBox;
    private Button mNewAliasListButton;
    private GridPane mTextFieldPane;
    private Button mSaveButton;
    private Button mResetButton;
    private VBox mButtonBox;
    private ScrollPane mTitledPanesScrollPane;
    private VBox mTitledPanesBox;
    private ToggleSwitch mAutoStartSwitch;
    private Spinner<Integer> mAutoStartOrderSpinner;
    private IconNode mPlayGraphicNode;
    private IconNode mStopGraphicNode;
    private ChannelProcessingMonitor mChannelProcessingMonitor = new ChannelProcessingMonitor();
    private IFilterProcessor mFilterProcessor;

    /**
     * Constructs an instance
     * @param playlistManager for playlists
     * @param tunerManager for tuners
     * @param userPreferences for preferences
     */
    public ChannelConfigurationEditor(PlaylistManager playlistManager, TunerManager tunerManager,
                                      UserPreferences userPreferences, IFilterProcessor filterProcessor)
    {
        mPlaylistManager = playlistManager;
        mTunerManager = tunerManager;
        mUserPreferences = userPreferences;
        mFilterProcessor = filterProcessor;

        setMaxWidth(Double.MAX_VALUE);

        HBox hbox = new HBox();
        hbox.setSpacing(10);
        hbox.setPadding(new Insets(10,10,10,10));
        hbox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(getTextFieldPane(), Priority.ALWAYS);
        HBox.setHgrow(getButtonBox(), Priority.NEVER);
        hbox.getChildren().addAll(getTextFieldPane(), getButtonBox());
        VBox.setVgrow(getTitledPanesScrollPane(), Priority.ALWAYS);

        getChildren().addAll(hbox, getTitledPanesScrollPane());
    }

    /**
     * Provides subclass access to the playlist manager and related components
     */
    protected PlaylistManager getPlaylistManager()
    {
        return mPlaylistManager;
    }

    @Override
    public void dispose()
    {
    }

    /**
     * Starts the current channel when the play button is not disabled.
     */
    public void startChannel()
    {
        if(!getPlayButton().disabledProperty().get())
        {
            getPlayButton().fire();
        }
    }

    public abstract DecoderType getDecoderType();

    @Override
    public void setItem(Channel channel)
    {
        if(getItem() != null)
        {
            getItem().processingProperty().removeListener(mChannelProcessingMonitor);
        }

        super.setItem(channel);

        if(getItem() != null)
        {
            setPlayButtonState(getItem().processingProperty().get());
            getItem().processingProperty().addListener(mChannelProcessingMonitor);
        }

        boolean disable = (channel == null);

        getPlayButton().setDisable(disable);
        getSystemField().setDisable(disable);
        getSiteField().setDisable(disable);
        getNameField().setDisable(disable);
        getAliasListComboBox().setDisable(disable);
        getNewAliasListButton().setDisable(disable);
        getAutoStartSwitch().setDisable(disable);

        if(channel != null)
        {
            getSystemField().setText(channel.getSystem());
            getSiteField().setText(channel.getSite());
            getNameField().setText(channel.getName());
            String aliasListName = channel.getAliasListName();

            if(aliasListName != null)
            {
                if(!getAliasListComboBox().getItems().contains(aliasListName))
                {
                    mPlaylistManager.getAliasModel().addAliasList(aliasListName);
                }

                getAliasListComboBox().getSelectionModel().select(aliasListName);
            }
            else
            {
                getAliasListComboBox().getSelectionModel().select(null);
            }

            getAutoStartSwitch().selectedProperty().set(channel.isAutoStart());
            getAutoStartOrderSpinner().setDisable(!channel.isAutoStart());
            Integer order = channel.getAutoStartOrder();
            getAutoStartOrderSpinner().getValueFactory().setValue(order != null ? order : 0);

            setDecoderConfiguration(channel.getDecodeConfiguration());

            SourceConfiguration sourceConfiguration = channel.getSourceConfiguration();
            if(sourceConfiguration == null)
            {
                sourceConfiguration = new SourceConfigTuner();
            }
            setSourceConfiguration(sourceConfiguration);

            AuxDecodeConfiguration auxDecodeConfiguration = channel.getAuxDecodeConfiguration();
            if(auxDecodeConfiguration == null)
            {
                auxDecodeConfiguration = new AuxDecodeConfiguration();
            }
            setAuxDecoderConfiguration(auxDecodeConfiguration);

            EventLogConfiguration eventLogConfiguration = channel.getEventLogConfiguration();
            if(eventLogConfiguration == null)
            {
                eventLogConfiguration = new EventLogConfiguration();
            }
            setEventLogConfiguration(eventLogConfiguration);

            RecordConfiguration recordConfiguration = channel.getRecordConfiguration();
            if(recordConfiguration == null)
            {
                recordConfiguration = new RecordConfiguration();
            }
            setRecordConfiguration(recordConfiguration);
        }
        else
        {
            getSystemField().setText(null);
            getSiteField().setText(null);
            getNameField().setText(null);
            getAliasListComboBox().getSelectionModel().select(null);
            getAutoStartSwitch().selectedProperty().set(false);
            getAutoStartOrderSpinner().setDisable(true);
            getAutoStartOrderSpinner().getValueFactory().setValue(0);

            setDecoderConfiguration(null);
            setAuxDecoderConfiguration(null);
            setEventLogConfiguration(null);
            setRecordConfiguration(null);
            setSourceConfiguration(null);
        }

        modifiedProperty().setValue(false);
    }

    @Override
    public void save()
    {
        if(modifiedProperty().get())
        {
            getItem().setSystem(getSystemField().getText());
            getItem().setSite(getSiteField().getText());

            //Hack - change the name to something else and then set it to the real value to trigger change events
            getItem().setName(" ");
            getItem().setName(getNameField().getText());
            getItem().setAliasListName(getAliasListComboBox().getSelectionModel().getSelectedItem());
            getItem().setAutoStart(getAutoStartSwitch().isSelected());

            Integer order = getAutoStartOrderSpinner().getValue();

            if(order == null || order < 1)
            {
                getItem().setAutoStartOrder(null);
            }
            else
            {
                getItem().setAutoStartOrder(getAutoStartOrderSpinner().getValue());
            }

            saveDecoderConfiguration();
            saveAuxDecoderConfiguration();
            saveEventLogConfiguration();
            saveRecordConfiguration();
            saveSourceConfiguration();

            modifiedProperty().set(false);
        }
    }

    protected abstract void setAuxDecoderConfiguration(AuxDecodeConfiguration config);
    protected abstract void saveAuxDecoderConfiguration();
    protected abstract void setDecoderConfiguration(DecodeConfiguration config);
    protected abstract void saveDecoderConfiguration();
    protected abstract void setEventLogConfiguration(EventLogConfiguration config);
    protected abstract void saveEventLogConfiguration();
    protected abstract void setRecordConfiguration(RecordConfiguration config);
    protected abstract void saveRecordConfiguration();
    protected abstract void setSourceConfiguration(SourceConfiguration config);
    protected abstract void saveSourceConfiguration();

    private Button getPlayButton()
    {
        if(mPlayButton == null)
        {
            mPlayGraphicNode = new IconNode(FontAwesome.PLAY);
            mPlayGraphicNode.setFill(Color.GREEN);
            mPlayGraphicNode.setIconSize(24);

            mStopGraphicNode = new IconNode(FontAwesome.STOP);
            mStopGraphicNode.setFill(Color.RED);
            mStopGraphicNode.setIconSize(24);

            mPlayButton = new Button("Play");
            mPlayButton.setMaxWidth(Double.MAX_VALUE);
            mPlayButton.setMaxHeight(Double.MAX_VALUE);
            mPlayButton.setDisable(true);
            mPlayButton.setOnAction((ActionEvent event) -> {
                if(getItem() != null)
                {
                    if(modifiedProperty().get())
                    {
                        Alert alert = new Alert(Alert.AlertType.WARNING,
                            "Do you want to save these changes?", ButtonType.YES, ButtonType.NO);
                        alert.setTitle("Channel Configuration Modified");
                        alert.setHeaderText("Channel configuration has unsaved changes");
                        alert.initOwner((getPlayButton()).getScene().getWindow());
                        alert.showAndWait().ifPresent(buttonType -> {
                            if(buttonType == ButtonType.YES)
                            {
                                save();
                            }
                        });
                    }

                    if(requiresJmbeLibrarySetup() &&
                       mUserPreferences.getJmbeLibraryPreference().getAlertIfMissingLibraryRequired() &&
                       !getItem().processingProperty().get())
                    {
                        String content = "The decoder for this channel configuration requires the (optional) JMBE " +
                            "library to produce audio and the JMBE library is not currently setup.  Do you want to " +
                            "setup the JMBE library?";

                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, content, ButtonType.YES, ButtonType.NO);
                        alert.setTitle("JMBE Library");
                        alert.setHeaderText("Setup JMBE Library?");

                        Label label = new Label(content);
                        label.setMaxWidth(Double.MAX_VALUE);
                        label.setMaxHeight(Double.MAX_VALUE);
                        label.getStyleClass().add("content");
                        label.setWrapText(true);

                        CheckBox checkBox = new CheckBox("Don't Show This Again");
                        checkBox.setOnAction(event1 -> {
                            boolean dontShowAgain = checkBox.isSelected();
                            mUserPreferences.getJmbeLibraryPreference().setAlertIfMissingLibraryRequired(!dontShowAgain);
                        });

                        VBox contentBox = new VBox();
                        contentBox.setPrefWidth(360);
                        contentBox.setSpacing(10);
                        contentBox.getChildren().addAll(label, checkBox);
                        alert.getDialogPane().setContent(contentBox);
                        alert.initOwner(getPlayButton().getScene().getWindow());
                        Optional<ButtonType> optionalButtonType = alert.showAndWait();

                        if(optionalButtonType.isPresent() && optionalButtonType.get() == ButtonType.YES)
                        {
                            MyEventBus.getGlobalEventBus().post(new ViewUserPreferenceEditorRequest(PreferenceEditorType.JMBE_LIBRARY));
                            return;
                        }
                    }

                    if(!getItem().processingProperty().get())
                    {
                        ThreadPool.CACHED.execute(() -> {
                            try
                            {
                                mPlaylistManager.getChannelProcessingManager().start(getItem());
                            }
                            catch(ChannelException ce)
                            {
                                mLog.error("Error starting channel [" + getItem().getName() + "] - " + ce.getMessage());

                                Platform.runLater(() -> {
                                    Alert alert = new Alert(Alert.AlertType.ERROR, "Error: " + ce.getMessage(), ButtonType.OK);
                                    alert.setTitle("Channel Play Error");
                                    alert.setHeaderText("Can't play channel");
                                    alert.initOwner((getPlayButton()).getScene().getWindow());
                                    alert.showAndWait();
                                });
                            }
                        });
                    }
                    else
                    {
                        ThreadPool.CACHED.execute(() -> {
                            try
                            {
                                mPlaylistManager.getChannelProcessingManager().stop(getItem());
                            }
                            catch(ChannelException ce)
                            {
                                mLog.error("Error stopping channel [" + getItem().getName() + "] - " + ce.getMessage());

                                Platform.runLater(() -> {
                                    Alert alert = new Alert(Alert.AlertType.ERROR, "Error: " + ce.getMessage(), ButtonType.OK);
                                    alert.setTitle("Channel Stop Error");
                                    alert.setHeaderText("Can't stop channel");
                                    alert.initOwner((getPlayButton()).getScene().getWindow());
                                    alert.showAndWait();
                                });
                            }
                        });
                    }
                }
            });
        }

        return mPlayButton;
    }

    /**
     * Indicates if the decoder type for the channel configuration requires the JMBE library and if the
     * application is not currently setup for the JMBE library.
     */
    private boolean requiresJmbeLibrarySetup()
    {
        return getItem() != null &&
               getItem().getDecodeConfiguration().getDecoderType().providesMBEAudioFrames() &&
               !mUserPreferences.getJmbeLibraryPreference().hasJmbeLibraryPath();
    }

    /**
     * Toggles the text and graphic of the channel play button to reflect the playing state of the channel
     */
    private void setPlayButtonState(boolean playing)
    {
        if(playing)
        {
            getPlayButton().setText("Stop");
            getPlayButton().setGraphic(mStopGraphicNode);
        }
        else
        {
            getPlayButton().setText("Play");
            getPlayButton().setGraphic(mPlayGraphicNode);
        }
    }

    private ToggleSwitch getAutoStartSwitch()
    {
        if(mAutoStartSwitch == null)
        {
            mAutoStartSwitch = new ToggleSwitch();
            mAutoStartSwitch.setDisable(true);
            mAutoStartSwitch.setDisable(true);
            mAutoStartSwitch.selectedProperty().addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mAutoStartSwitch;
    }

    private Spinner<Integer> getAutoStartOrderSpinner()
    {
        if(mAutoStartOrderSpinner == null)
        {
            mAutoStartOrderSpinner = new Spinner();
            mAutoStartOrderSpinner.setPrefWidth(100);
            mAutoStartOrderSpinner.setDisable(true);
            getAutoStartSwitch().selectedProperty().addListener(new ChangeListener<Boolean>()
            {
                @Override
                public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue)
                {
                    getAutoStartOrderSpinner().setDisable(!getAutoStartSwitch().selectedProperty().getValue());
                }
            });
            SpinnerValueFactory svf = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 99);
            mAutoStartOrderSpinner.setValueFactory(svf);
            mAutoStartOrderSpinner.getStyleClass().add(Spinner.STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL);
            mAutoStartOrderSpinner.valueProperty().addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mAutoStartOrderSpinner;
    }

    protected VBox getTitledPanesBox()
    {
        if(mTitledPanesBox == null)
        {
            mTitledPanesBox = new VBox();
            mTitledPanesBox.setMaxWidth(Double.MAX_VALUE);
        }

        return mTitledPanesBox;
    }

    private ScrollPane getTitledPanesScrollPane()
    {
        if(mTitledPanesScrollPane == null)
        {
            mTitledPanesScrollPane = new ScrollPane();
            mTitledPanesScrollPane.setFitToWidth(true);
            mTitledPanesScrollPane.setContent(getTitledPanesBox());
        }

        return mTitledPanesScrollPane;
    }



    private GridPane getTextFieldPane()
    {
        if(mTextFieldPane == null)
        {
            mTextFieldPane = new GridPane();
            mTextFieldPane.setVgap(10);
            mTextFieldPane.setHgap(10);

            int row = 0;

            Label systemLabel = new Label("System");
            GridPane.setHalignment(systemLabel, HPos.RIGHT);
            GridPane.setConstraints(systemLabel, 0, row);
            mTextFieldPane.getChildren().add(systemLabel);

            GridPane.setConstraints(getSystemField(), 1, row);
            GridPane.setHgrow(getSystemField(), Priority.ALWAYS);
            mTextFieldPane.getChildren().add(getSystemField());

            Label autoStartLabel = new Label("Auto-Start");
            GridPane.setHalignment(autoStartLabel, HPos.RIGHT);
            GridPane.setConstraints(autoStartLabel, 2, row);
            mTextFieldPane.getChildren().add(autoStartLabel);

            GridPane.setConstraints(getAutoStartSwitch(), 3, row);
            GridPane.setHalignment(getAutoStartSwitch(), HPos.LEFT);
            mTextFieldPane.getChildren().add(getAutoStartSwitch());

            GridPane.setConstraints(getPlayButton(), 4, row, 1, 2);
            mTextFieldPane.getChildren().add(getPlayButton());

            Label siteLabel = new Label("Site");
            GridPane.setHalignment(siteLabel, HPos.RIGHT);
            GridPane.setConstraints(siteLabel, 0, ++row);
            mTextFieldPane.getChildren().add(siteLabel);

            GridPane.setConstraints(getSiteField(), 1, row);
            GridPane.setHgrow(getSiteField(), Priority.ALWAYS);
            mTextFieldPane.getChildren().add(getSiteField());

            Label autoStartOrderLabel = new Label("Start Order");
            GridPane.setHalignment(autoStartOrderLabel, HPos.RIGHT);
            GridPane.setConstraints(autoStartOrderLabel, 2, row);
            mTextFieldPane.getChildren().add(autoStartOrderLabel);

            GridPane.setConstraints(getAutoStartOrderSpinner(), 3, row);
            GridPane.setHalignment(getAutoStartOrderSpinner(), HPos.LEFT);
            mTextFieldPane.getChildren().add(getAutoStartOrderSpinner());

            Label nameLabel = new Label("Name");
            GridPane.setHalignment(nameLabel, HPos.RIGHT);
            GridPane.setConstraints(nameLabel, 0, ++row);
            mTextFieldPane.getChildren().add(nameLabel);

            GridPane.setConstraints(getNameField(), 1, row);
            GridPane.setHgrow(getNameField(), Priority.ALWAYS);
            mTextFieldPane.getChildren().add(getNameField());

            Label aliasListLabel = new Label("Alias List");
            GridPane.setHalignment(aliasListLabel, HPos.RIGHT);
            GridPane.setConstraints(aliasListLabel, 2, row);
            mTextFieldPane.getChildren().add(aliasListLabel);

            GridPane.setConstraints(getAliasListComboBox(), 3, row);
            GridPane.setHgrow(getAliasListComboBox(), Priority.ALWAYS);
            mTextFieldPane.getChildren().add(getAliasListComboBox());

            GridPane.setConstraints(getNewAliasListButton(), 4, row);
            mTextFieldPane.getChildren().add(getNewAliasListButton());
        }

        return mTextFieldPane;
    }

    protected TextField getSystemField()
    {
        if(mSystemField == null)
        {
            mSystemField = new TextField();
            mSystemField.setDisable(true);
            mSystemField.setMaxWidth(Double.MAX_VALUE);
            mSystemField.textProperty().addListener(mEditorModificationListener);
        }

        return mSystemField;
    }

    protected TextField getSiteField()
    {
        if(mSiteField == null)
        {
            mSiteField = new TextField();
            mSiteField.setDisable(true);
            mSiteField.setMaxWidth(Double.MAX_VALUE);
            mSiteField.textProperty().addListener(mEditorModificationListener);
        }

        return mSiteField;
    }

    protected TextField getNameField()
    {
        if(mNameField == null)
        {
            mNameField = new TextField();
            mNameField.setDisable(true);
            mNameField.setMaxWidth(Double.MAX_VALUE);
            mNameField.textProperty().addListener(mEditorModificationListener);
        }

        return mNameField;
    }

    protected ComboBox<String> getAliasListComboBox()
    {
        if(mAliasListComboBox == null)
        {
            Predicate<String> filterPredicate = s -> !s.contentEquals(AliasModel.NO_ALIAS_LIST);
            FilteredList<String> filteredChannelList =
                new FilteredList<>(mPlaylistManager.getAliasModel().aliasListNames(), filterPredicate);
            mAliasListComboBox = new ComboBox<>(filteredChannelList);
            mAliasListComboBox.setPrefWidth(150);
            mAliasListComboBox.setDisable(true);
            mAliasListComboBox.setEditable(false);
            mAliasListComboBox.setMaxWidth(Double.MAX_VALUE);
            mAliasListComboBox.setOnAction(event -> modifiedProperty().set(true));
        }

        return mAliasListComboBox;
    }

    private Button getNewAliasListButton()
    {
        if(mNewAliasListButton == null)
        {
            mNewAliasListButton = new Button("New Alias List");
            mNewAliasListButton.setDisable(true);
            mNewAliasListButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    TextInputDialog dialog = new TextInputDialog();
                    dialog.setTitle("Create New Alias List");
                    dialog.setHeaderText("Please enter an alias list name (max 25 chars).");
                    dialog.setContentText("Name:");
                    dialog.getEditor().setTextFormatter(new TextFormatter<String>(new MaxLengthUnaryOperator(25)));
                    Optional<String> result = dialog.showAndWait();

                    result.ifPresent(s -> {
                        String name = result.get();

                        if(name != null && !name.isEmpty())
                        {
                            name = name.trim();
                            mPlaylistManager.getAliasModel().addAliasList(name);
                            getAliasListComboBox().getSelectionModel().select(name);
                        }
                    });
                }
            });
        }

        return mNewAliasListButton;
    }

    private VBox getButtonBox()
    {
        if(mButtonBox == null)
        {
            mButtonBox = new VBox();
            mButtonBox.setSpacing(10);
            mButtonBox.getChildren().addAll(getSaveButton(), getResetButton());
        }

        return mButtonBox;
    }

    private Button getSaveButton()
    {
        if(mSaveButton == null)
        {
            mSaveButton = new Button("     Save     ");
            mSaveButton.setMaxWidth(Double.MAX_VALUE);
            mSaveButton.disableProperty().bind(modifiedProperty().not());
            mSaveButton.setOnAction(event -> {
                if(mFilterProcessor != null)
                {
                    mFilterProcessor.clearFilter();
                    save();
                    mFilterProcessor.restoreFilter();
                }
                else
                {
                    save();
                }

                if(getItem().isProcessing())
                {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Would you like to restart the channel?", ButtonType.YES, ButtonType.NO);
                    alert.setTitle("Restart Channel?");
                    alert.setHeaderText("Channel configuration has changed");
                    alert.initOwner((getPlayButton()).getScene().getWindow());
                    Optional<ButtonType> result = alert.showAndWait();

                    if(result.get() == ButtonType.YES)
                    {
                        try
                        {
                            mPlaylistManager.getChannelProcessingManager().stop(getItem());
                            mPlaylistManager.getChannelProcessingManager().start(getItem());
                        }
                        catch(ChannelException se)
                        {
                            mLog.error("Error restarting channel", se);
                        }
                    }
                }
            });
        }

        return mSaveButton;
    }

    private Button getResetButton()
    {
        if(mResetButton == null)
        {
            mResetButton = new Button("Reset");
            mResetButton.setMaxWidth(Double.MAX_VALUE);
            mResetButton.disableProperty().bind(modifiedProperty().not());
            mResetButton.setOnAction(event -> {
                modifiedProperty().set(false);
                setItem(getItem());
            });
        }

        return mResetButton;
    }


    /**
     * Simple string change listener that sets the editor modified flag to true any time text fields are edited.
     */
    public class EditorModificationListener implements ChangeListener<String>
    {
        @Override
        public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue)
        {
            modifiedProperty().set(true);
        }
    }

    public class ChannelProcessingMonitor implements ChangeListener<Boolean>
    {
        @Override
        public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue)
        {
            if(getItem() != null && newValue != null)
            {
                setPlayButtonState(newValue);
            }
        }
    }
}
