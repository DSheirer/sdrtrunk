/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */

package io.github.dsheirer.gui.playlist.channel;

import impl.org.controlsfx.autocompletion.AutoCompletionTextFieldBinding;
import impl.org.controlsfx.autocompletion.SuggestionProvider;
import io.github.dsheirer.alias.AliasEvent;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.gui.playlist.Editor;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.config.AuxDecodeConfiguration;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.module.log.config.EventLogConfiguration;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.record.config.RecordConfiguration;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.config.SourceConfigTuner;
import io.github.dsheirer.source.config.SourceConfiguration;
import io.github.dsheirer.source.tuner.TunerModel;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.controlsfx.control.ToggleSwitch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * Channel configuration editor
 */
public abstract class ChannelConfigurationEditor extends Editor<Channel>
{
    private final static Logger mLog = LoggerFactory.getLogger(ChannelConfigurationEditor.class);

    private PlaylistManager mPlaylistManager;
    protected EditorModificationListener mEditorModificationListener = new EditorModificationListener();
    private AliasModelChangeListener mAliasModelChangeListener = new AliasModelChangeListener();
    private TextField mSystemField;
    private TextField mSiteField;
    private TextField mNameField;
    private ComboBox<String> mAliasListComboBox;
    private GridPane mTextFieldPane;
    private Button mSaveButton;
    private Button mResetButton;
    private VBox mButtonBox;
    private ScrollPane mTitledPanesScrollPane;
    private VBox mTitledPanesBox;
    private ToggleSwitch mAutoStartSwitch;
    private Spinner<Integer> mAutoStartOrderSpinner;
    private SuggestionProvider<String> mSystemSuggestionProvider;
    private SuggestionProvider<String> mSiteSuggestionProvider;

    public ChannelConfigurationEditor(PlaylistManager playlistManager)
    {
        mPlaylistManager = playlistManager;

        //Listen for alias change events so we can update the alias list combo box
        mPlaylistManager.getAliasModel().addListener(mAliasModelChangeListener);

        setMaxWidth(Double.MAX_VALUE);

        HBox hbox = new HBox();
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
        mPlaylistManager.getAliasModel().removeListener(mAliasModelChangeListener);
    }

    public abstract DecoderType getDecoderType();

    @Override
    public void setItem(Channel channel)
    {
        super.setItem(channel);

        refreshAutoCompleteBindings();

        if(channel != null)
        {
            getSystemField().setDisable(false);
            getSystemField().setText(channel.getSystem());
            getSiteField().setDisable(false);
            getSiteField().setText(channel.getSite());
            getNameField().setDisable(false);
            getNameField().setText(channel.getName());
            getAliasListComboBox().setDisable(false);
            String aliasListName = channel.getAliasListName();

            if(aliasListName != null)
            {
                if(!getAliasListComboBox().getItems().contains(aliasListName))
                {
                    getAliasListComboBox().getItems().add(aliasListName);
                }

                getAliasListComboBox().getSelectionModel().select(aliasListName);
            }
            else
            {
                getAliasListComboBox().getSelectionModel().select(null);
            }

            getAutoStartSwitch().setDisable(false);
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
            getSystemField().setDisable(true);
            getSystemField().setText(null);
            getSiteField().setDisable(true);
            getSiteField().setText(null);
            getNameField().setDisable(true);
            getNameField().setText(null);
            getAliasListComboBox().setDisable(true);
            getAliasListComboBox().getSelectionModel().select(null);
            getAutoStartSwitch().setDisable(true);
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

    protected TunerModel getTunerModel()
    {
        return mPlaylistManager.getTunerModel();
    }

    @Override
    public void save()
    {
        if(modifiedProperty().get())
        {
            getItem().setSystem(getSystemField().getText());
            getItem().setSite(getSiteField().getText());
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
            mTextFieldPane.setPadding(new Insets(10, 5, 10,10));
            mTextFieldPane.setVgap(10);
            mTextFieldPane.setHgap(5);

            Label systemLabel = new Label("System");
            GridPane.setHalignment(systemLabel, HPos.RIGHT);
            GridPane.setConstraints(systemLabel, 0, 0);
            mTextFieldPane.getChildren().add(systemLabel);

            GridPane.setConstraints(getSystemField(), 1, 0);
            GridPane.setHgrow(getSystemField(), Priority.ALWAYS);
            mTextFieldPane.getChildren().add(getSystemField());

            Label siteLabel = new Label("Site");
            GridPane.setHalignment(siteLabel, HPos.RIGHT);
            GridPane.setConstraints(siteLabel, 2, 0);
            mTextFieldPane.getChildren().add(siteLabel);

            GridPane.setConstraints(getSiteField(), 3, 0);
            GridPane.setHgrow(getSiteField(), Priority.ALWAYS);
            mTextFieldPane.getChildren().add(getSiteField());

            Label nameLabel = new Label("Name");
            GridPane.setHalignment(nameLabel, HPos.RIGHT);
            GridPane.setConstraints(nameLabel, 0, 1);
            mTextFieldPane.getChildren().add(nameLabel);

            GridPane.setConstraints(getNameField(), 1, 1);
            GridPane.setHgrow(getNameField(), Priority.ALWAYS);
            mTextFieldPane.getChildren().add(getNameField());

            Label aliasListLabel = new Label("Alias List");
            GridPane.setHalignment(aliasListLabel, HPos.RIGHT);
            GridPane.setConstraints(aliasListLabel, 2, 1);
            mTextFieldPane.getChildren().add(aliasListLabel);

            GridPane.setConstraints(getAliasListComboBox(), 3, 1);
            GridPane.setHgrow(getAliasListComboBox(), Priority.ALWAYS);
            mTextFieldPane.getChildren().add(getAliasListComboBox());

            Label autoStartLabel = new Label("Auto-Start");
            GridPane.setHalignment(autoStartLabel, HPos.RIGHT);
            GridPane.setConstraints(autoStartLabel, 0, 2);
            mTextFieldPane.getChildren().add(autoStartLabel);

            GridPane.setConstraints(getAutoStartSwitch(), 1, 2);
            GridPane.setHalignment(getAutoStartSwitch(), HPos.LEFT);
            mTextFieldPane.getChildren().add(getAutoStartSwitch());

            Label autoStartOrderLabel = new Label("Auto-Start Order");
            GridPane.setHalignment(autoStartOrderLabel, HPos.RIGHT);
            GridPane.setConstraints(autoStartOrderLabel, 2, 2);
            mTextFieldPane.getChildren().add(autoStartOrderLabel);

            GridPane.setConstraints(getAutoStartOrderSpinner(), 3, 2);
            GridPane.setHalignment(getAutoStartOrderSpinner(), HPos.LEFT);
            mTextFieldPane.getChildren().add(getAutoStartOrderSpinner());
        }

        return mTextFieldPane;
    }

    /**
     * Refreshes the system and site text field auto-completion lists.
     */
    private void refreshAutoCompleteBindings()
    {
        getSystemSuggestionProvider().clearSuggestions();
        getSystemSuggestionProvider().addPossibleSuggestions(mPlaylistManager.getChannelModel().getSystemNames());
        getSiteSuggestionProvider().clearSuggestions();
        getSiteSuggestionProvider().addPossibleSuggestions(mPlaylistManager.getChannelModel().getSiteNames());
    }

    protected TextField getSystemField()
    {
        if(mSystemField == null)
        {
            mSystemField = new TextField();
            mSystemField.setDisable(true);
            mSystemField.setMaxWidth(Double.MAX_VALUE);
            mSystemField.textProperty().addListener(mEditorModificationListener);
            new AutoCompletionTextFieldBinding<>(mSystemField, getSystemSuggestionProvider());
        }

        return mSystemField;
    }

    private SuggestionProvider<String> getSystemSuggestionProvider()
    {
        if(mSystemSuggestionProvider == null)
        {
            mSystemSuggestionProvider = SuggestionProvider.create(mPlaylistManager.getChannelModel().getSystemNames());
        }

        return mSystemSuggestionProvider;
    }

    private SuggestionProvider<String> getSiteSuggestionProvider()
    {
        if(mSiteSuggestionProvider == null)
        {
            mSiteSuggestionProvider = SuggestionProvider.create(mPlaylistManager.getChannelModel().getSiteNames());
        }

        return mSiteSuggestionProvider;
    }

    protected TextField getSiteField()
    {
        if(mSiteField == null)
        {
            mSiteField = new TextField();
            mSiteField.setDisable(true);
            mSiteField.setMaxWidth(Double.MAX_VALUE);
            mSiteField.textProperty().addListener(mEditorModificationListener);
            new AutoCompletionTextFieldBinding<>(mSiteField, getSiteSuggestionProvider());
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
            mAliasListComboBox = new ComboBox<>();
            mAliasListComboBox.setDisable(true);
            mAliasListComboBox.setEditable(true);
            mAliasListComboBox.setMaxWidth(Double.MAX_VALUE);
            mAliasListComboBox.getItems().addAll(mPlaylistManager.getAliasModel().getListNames());
            mAliasListComboBox.setOnAction(event -> modifiedProperty().set(true));
        }

        return mAliasListComboBox;
    }

    private VBox getButtonBox()
    {
        if(mButtonBox == null)
        {
            mButtonBox = new VBox();
            mButtonBox.setSpacing(10);
            mButtonBox.setPadding(new Insets(10, 10, 10, 5));
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
            mSaveButton.setOnAction(event -> save());

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

    /**
     * Alias list change listener to update the contents of the alias list combo box in this editor
     */
    public class AliasModelChangeListener implements Listener<AliasEvent>
    {
        @Override
        public void receive(AliasEvent aliasEvent)
        {
            Platform.runLater(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        List<String> aliasListNames = mPlaylistManager.getAliasModel().getListNames();

                        String selected = getAliasListComboBox().getSelectionModel().getSelectedItem();
                        boolean modified = modifiedProperty().get();

                        if(selected != null && !aliasListNames.contains(selected))
                        {
                            aliasListNames.add(selected);
                        }

                        Collections.sort(aliasListNames);

                        getAliasListComboBox().getItems().clear();
                        getAliasListComboBox().getItems().addAll(aliasListNames);

                        if(selected != null)
                        {
                            getAliasListComboBox().getSelectionModel().select(selected);
                        }

                        //Restore the state of the modified flag to what it was before we updated the combo box
                        modifiedProperty().set(modified);
                    }
                    catch(Throwable t)
                    {
                        mLog.error("Error refreshing alias list names in channel configuration editor");
                    }
                }
            });
        }
    }
}
