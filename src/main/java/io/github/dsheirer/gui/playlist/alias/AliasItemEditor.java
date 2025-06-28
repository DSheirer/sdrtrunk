/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

package io.github.dsheirer.gui.playlist.alias;

import com.google.common.collect.Ordering;
import com.google.common.eventbus.Subscribe;
import impl.org.controlsfx.autocompletion.AutoCompletionTextFieldBinding;
import impl.org.controlsfx.autocompletion.SuggestionProvider;
import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasFactory;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.alias.action.AliasAction;
import io.github.dsheirer.alias.action.AliasActionType;
import io.github.dsheirer.alias.action.beep.BeepAction;
import io.github.dsheirer.alias.action.clip.ClipAction;
import io.github.dsheirer.alias.action.script.ScriptAction;
import io.github.dsheirer.alias.id.AliasID;
import io.github.dsheirer.alias.id.AliasIDType;
import io.github.dsheirer.alias.id.broadcast.BroadcastChannel;
import io.github.dsheirer.alias.id.dcs.Dcs;
import io.github.dsheirer.alias.id.esn.Esn;
import io.github.dsheirer.alias.id.lojack.LoJackFunctionAndID;
import io.github.dsheirer.alias.id.radio.P25FullyQualifiedRadio;
import io.github.dsheirer.alias.id.radio.Radio;
import io.github.dsheirer.alias.id.radio.RadioFormatter;
import io.github.dsheirer.alias.id.radio.RadioRange;
import io.github.dsheirer.alias.id.status.UnitStatusID;
import io.github.dsheirer.alias.id.status.UserStatusID;
import io.github.dsheirer.alias.id.talkgroup.P25FullyQualifiedTalkgroup;
import io.github.dsheirer.alias.id.talkgroup.StreamAsTalkgroup;
import io.github.dsheirer.alias.id.talkgroup.Talkgroup;
import io.github.dsheirer.alias.id.talkgroup.TalkgroupFormatter;
import io.github.dsheirer.alias.id.talkgroup.TalkgroupRange;
import io.github.dsheirer.alias.id.tone.TonesID;
import io.github.dsheirer.audio.broadcast.ConfiguredBroadcast;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.gui.control.IntegerFormatter;
import io.github.dsheirer.gui.playlist.Editor;
import io.github.dsheirer.gui.playlist.alias.action.ActionEditor;
import io.github.dsheirer.gui.playlist.alias.action.ActionEditorFactory;
import io.github.dsheirer.gui.playlist.alias.action.EmptyActionEditor;
import io.github.dsheirer.gui.playlist.alias.identifier.EmptyIdentifierEditor;
import io.github.dsheirer.gui.playlist.alias.identifier.IdentifierEditor;
import io.github.dsheirer.gui.playlist.alias.identifier.IdentifierEditorFactory;
import io.github.dsheirer.icon.Icon;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.identifier.IntegerFormat;
import io.github.dsheirer.protocol.Protocol;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.util.Callback;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.javafx.IconNode;
import org.controlsfx.control.ToggleSwitch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Editor for configuring individual aliases
 */
public class AliasItemEditor extends Editor<Alias>
{
    private static final Logger mLog = LoggerFactory.getLogger(AliasItemEditor.class);

    private PlaylistManager mPlaylistManager;
    private UserPreferences mUserPreferences;
    private EditorModificationListener mEditorModificationListener = new EditorModificationListener();
    private IdentifierEditorModificationListener mIdentifierEditorModificationListener = new IdentifierEditorModificationListener();
    private ActionEditorModificationListener mActionEditorModificationListener = new ActionEditorModificationListener();
    private TextField mGroupField;
    private TextField mNameField;
    private GridPane mTextFieldPane;
    private Button mSaveButton;
    private Button mResetButton;
    private VBox mButtonBox;
    private ToggleSwitch mMonitorAudioToggleSwitch;
    private ComboBox<Integer> mMonitorPriorityComboBox;
    private ToggleSwitch mRecordAudioToggleSwitch;
    private ColorPicker mColorPicker;
    private ComboBox<Icon> mIconNodeComboBox;
    private SuggestionProvider<String> mGroupSuggestionProvider;
    private VBox mTitledPanesBox;
    private TitledPane mIdentifierPane;
    private TitledPane mStreamPane;
    private TitledPane mActionPane;
    private ListView<String> mAvailableStreamsView;
    private ListView<BroadcastChannel> mSelectedStreamsView;
    private ListView<AliasID> mIdentifiersList;
    private ListView<AliasAction> mActionsList;
    private Button mAddStreamButton;
    private Button mRemoveStreamButton;
    private MenuButton mAddIdentifierButton;
    private Button mDeleteIdentifierButton;
    private Button mShowOverlapButton;
    private MenuButton mAddActionButton;
    private Button mDeleteActionButton;
    private VBox mActionEditorBox;
    private VBox mIdentifierEditorBox;
    private TextField mStreamAsTalkgroupField;
    private TextFormatter<Integer> mStreamAsIntegerTextFormatter = new IntegerFormatter(1,0xFFFF);

    private Map<AliasIDType,IdentifierEditor> mIdentifierEditorMap = new HashMap<>();
    private EmptyIdentifierEditor mEmptyIdentifierEditor = new EmptyIdentifierEditor();
    private IdentifierEditor mIdentifierEditor;

    private Map<AliasActionType,ActionEditor> mActionEditorMap = new HashMap<>();
    private EmptyActionEditor mEmptyActionEditor = new EmptyActionEditor();
    private ActionEditor mActionEditor;


    public AliasItemEditor(PlaylistManager playlistManager, UserPreferences userPreferences)
    {
        mPlaylistManager = playlistManager;
        mUserPreferences = userPreferences;

        //Listen for changes to the stream configurations and refresh the stream lists
        mPlaylistManager.getBroadcastModel().getConfiguredBroadcasts()
            .addListener((ListChangeListener<ConfiguredBroadcast>)c -> updateStreamViews());

        MyEventBus.getGlobalEventBus().register(this);

        setMaxWidth(Double.MAX_VALUE);

        VBox vbox = new VBox();
        vbox.getChildren().addAll(getTextFieldPane(), getTitledPanesBox());

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setContent(vbox);

        HBox hbox = new HBox();
        hbox.setMaxWidth(Double.MAX_VALUE);
        hbox.setSpacing(10);
        HBox.setHgrow(scrollPane, Priority.ALWAYS);
        HBox.setHgrow(getButtonBox(), Priority.NEVER);
        hbox.getChildren().addAll(scrollPane, getButtonBox());

        getChildren().add(hbox);
    }


    @Subscribe
    public void preferenceUpdated(PreferenceType preferenceType)
    {
        if(preferenceType == PreferenceType.TALKGROUP_FORMAT)
        {
            //When the talkgroup format changes, refresh any alias identifiers in the identifier list so that the
            //display string is correctly formatted.
            for(AliasID id: mIdentifiersList.getItems())
            {
                id.valueProperty().setValue(null);
                id.updateValueProperty();
            }
        }

        //Re-set the selected identifier to force it to update controls with the preference change.
        AliasID selected = getIdentifiersList().getSelectionModel().getSelectedItem();

        if(selected != null)
        {
            getIdentifierEditor().setItem(selected);
        }
    }

    @Override
    public void setItem(Alias alias)
    {
        super.setItem(alias);

        refreshAutoCompleteBindings();

        boolean disable = (alias == null);
        getGroupField().setDisable(disable);
        getNameField().setDisable(disable);
        getRecordAudioToggleSwitch().setDisable(disable);
        getColorPicker().setDisable(disable);
        getMonitorAudioToggleSwitch().setDisable(disable);
        getIconNodeComboBox().setDisable(disable);

        getIdentifiersList().setDisable(disable);
        getIdentifiersList().getItems().clear();
        getAddIdentifierButton().setDisable(disable);

        getActionsList().setDisable(disable);
        getActionsList().getItems().clear();
        getAddActionButton().setDisable(disable);

        updateStreamViews();

        if(alias != null)
        {
            getGroupField().setText(alias.getGroup());
            getNameField().setText(alias.getName());
            getRecordAudioToggleSwitch().setSelected(alias.isRecordable());

            Icon icon = null;
            String iconName = alias.getIconName();
            if(iconName != null)
            {
                icon = mPlaylistManager.getIconModel().getIcon(iconName);
            }
            getIconNodeComboBox().getSelectionModel().select(icon);

            int monitorPriority = alias.getPlaybackPriority();

            boolean canMonitor = (monitorPriority != io.github.dsheirer.alias.id.priority.Priority.DO_NOT_MONITOR);
            getMonitorAudioToggleSwitch().setSelected(canMonitor);

            if(canMonitor && monitorPriority != io.github.dsheirer.alias.id.priority.Priority.DEFAULT_PRIORITY)
            {
                getMonitorPriorityComboBox().getSelectionModel().select(monitorPriority);
            }
            else
            {
                getMonitorPriorityComboBox().getSelectionModel().select(null);
            }

            Color color = ColorUtil.fromInteger(alias.getColor());
            getColorPicker().setValue(color);

            //Only add non-audio identifiers to the list -- audio identifiers are managed separately
            for(AliasID aliasID: alias.getAliasIdentifiers())
            {
                if(!aliasID.isAudioIdentifier())
                {
                    AliasID copy = AliasFactory.copyOf(aliasID);

                    if(copy != null)
                    {
                        getIdentifiersList().getItems().add(copy);
                    }
                    else
                    {
                        //Use the original, but changes won't be reversible.  This should only impact legacy
                        //identifiers which can't be edited anyway
                        getIdentifiersList().getItems().add(aliasID);
                    }
                }
            }

            for(AliasAction original: alias.getAliasActions())
            {
                AliasAction copy = AliasFactory.copyOf(original);

                if(copy != null)
                {
                    getActionsList().getItems().add(copy);
                }
                else
                {
                    mLog.warn("Unable to create copy of alias action [" + original.getType() +
                        "] for alias [" + alias.getName() + "] - action will be lost if alias is saved");
                }
            }
        }
        else
        {
            getGroupField().setText(null);
            getNameField().setText(null);
            getRecordAudioToggleSwitch().setSelected(false);
            getColorPicker().setValue(Color.BLACK);
            getMonitorPriorityComboBox().getSelectionModel().select(null);
            getMonitorAudioToggleSwitch().setSelected(false);
        }

        modifiedProperty().set(false);
    }

    @Override
    public void save()
    {
        if(modifiedProperty().get())
        {
            Alias alias = getItem();

            if(alias != null)
            {
                alias.setRecordable(getRecordAudioToggleSwitch().isSelected());
                alias.setColor(ColorUtil.toInteger(getColorPicker().getValue()));

                Icon icon = getIconNodeComboBox().getSelectionModel().getSelectedItem();
                alias.setIconName(icon != null ? icon.getName() : null);

                boolean canMonitor = getMonitorAudioToggleSwitch().isSelected();
                Integer priority = getMonitorPriorityComboBox().getSelectionModel().getSelectedItem();
                if(canMonitor)
                {
                    if(priority == null)
                    {
                        priority = io.github.dsheirer.alias.id.priority.Priority.DEFAULT_PRIORITY;
                    }

                    alias.setCallPriority(priority);
                }
                else
                {
                    alias.setCallPriority(io.github.dsheirer.alias.id.priority.Priority.DO_NOT_MONITOR);
                }

                //Store broadcast streaming audio channels
                alias.removeAllBroadcastChannels();
                for(BroadcastChannel selected: getSelectedStreamsView().getItems())
                {
                    alias.addAliasID(selected);
                }

                //Set or clear the 'Stream As Talkgroup' value.
                Integer streamAsTalkgroup = mStreamAsIntegerTextFormatter.getValue();
                alias.setStreamTalkgroupAlias(streamAsTalkgroup != null ? new StreamAsTalkgroup(streamAsTalkgroup) : null);

                //Remove and replace the remaining non-audio identifiers
                alias.removeNonAudioIdentifiers();

                for(AliasID aliasID: getIdentifiersList().getItems())
                {
                    //Create a copy of the identifier so that the alias and the editor don't have the same instance
                    //and reset the overlap flag (if set) so that the alias list can reevaluate the overlap state.
                    AliasID copy = AliasFactory.copyOf(aliasID);

                    if(copy != null)
                    {
                        copy.setOverlap(false);
                        alias.addAliasID(copy);
                    }
                    else
                    {
                        mLog.warn("Unable to create a copy of alias ID: " + aliasID);
                    }
                }

                //Remove and replace alias actions
                alias.removeAllActions();
                for(AliasAction aliasAction: getActionsList().getItems())
                {
                    alias.addAliasAction(aliasAction);
                }

                //Hack: because we're using a sorted list for the alias editor, sometimes setting
                //name and or group can cause list errors.  So, we wrap each of these in a try/catch
                //block to mitigate the error and effect the changes.
                try
                {
                    alias.setName(getNameField().getText());
                }
                catch(Exception e)
                {
                    mLog.error("Error while updating alias name.", e);
                }

                try
                {
                    alias.setGroup(getGroupField().getText());
                }
                catch(Exception e)
                {
                    mLog.error("Error while updating alias group value", e);
                }
            }

            AliasList aliasList = mPlaylistManager.getAliasModel().getAliasList(alias.getAliasListName());
            aliasList.updateAlias(alias);

            //Reset the alias to refresh the editor.
            setItem(alias);

            modifiedProperty().set(false);
        }
    }

    @Override
    public void dispose()
    {
        MyEventBus.getGlobalEventBus().unregister(this);
    }

    /**
     * Editor field for the stream this alias as a different talkgroup value
     */
    private TextField getStreamAsTalkgroupField()
    {
        if(mStreamAsTalkgroupField == null)
        {
            mStreamAsTalkgroupField = new TextField();
            mStreamAsTalkgroupField.setDisable(true);
            mStreamAsTalkgroupField.setTextFormatter(mStreamAsIntegerTextFormatter);
            mStreamAsTalkgroupField.setTooltip(new Tooltip("When specified, all streamed call audio will use this " +
                    "talkgroup value in place of the decoded talkgroup value."));
            mStreamAsTalkgroupField.textProperty().addListener((o, old, newV) -> {
                if(getItem() != null)
                {
                    modifiedProperty().set(true);
                }
            });
        }

        return mStreamAsTalkgroupField;
    }

    private VBox getTitledPanesBox()
    {
        if(mTitledPanesBox == null)
        {
            mTitledPanesBox = new VBox();
            mTitledPanesBox.setMaxWidth(Double.MAX_VALUE);
            mTitledPanesBox.getChildren().addAll(getIdentifierPane(), getStreamPane(), getActionPane());
        }

        return mTitledPanesBox;
    }

    private TitledPane getIdentifierPane()
    {
        if(mIdentifierPane == null)
        {
            VBox buttonsBox = new VBox();
            buttonsBox.setSpacing(10);
            buttonsBox.getChildren().addAll(getAddIdentifierButton(), getDeleteIdentifierButton(),
                getShowOverlapButton());

            HBox identifiersAndButtonsBox = new HBox();
            identifiersAndButtonsBox.setSpacing(10);
            HBox.setHgrow(getIdentifierEditorBox(), Priority.ALWAYS);
            identifiersAndButtonsBox.getChildren().addAll(getIdentifierEditorBox(), buttonsBox);

            mIdentifierPane = new TitledPane("Identifiers", identifiersAndButtonsBox);
        }

        return mIdentifierPane;
    }

    private VBox getIdentifierEditorBox()
    {
        if(mIdentifierEditorBox == null)
        {
            mIdentifierEditorBox = new VBox();
            mIdentifierEditorBox.setSpacing(10);
            mIdentifierEditorBox.getChildren().addAll(getIdentifiersList(), getIdentifierEditor());
        }

        return mIdentifierEditorBox;
    }

    private Editor<AliasID> getIdentifierEditor()
    {
        if(mIdentifierEditor == null)
        {
            mIdentifierEditor = mEmptyIdentifierEditor;
        }

        return mIdentifierEditor;
    }

    private Editor<AliasAction> getActionEditor()
    {
        if(mActionEditor == null)
        {
            mActionEditor = mEmptyActionEditor;
        }

        return mActionEditor;
    }

    private void setAction(AliasAction aliasAction)
    {
        ActionEditor editor = null;

        if(aliasAction != null)
        {
            editor = mActionEditorMap.get(aliasAction.getType());

            if(editor == null)
            {
                editor = ActionEditorFactory.getEditor(aliasAction.getType(), mUserPreferences);
                mActionEditorMap.put(aliasAction.getType(), editor);
            }
        }

        getDeleteActionButton().setDisable(aliasAction == null);

        if(editor == null)
        {
            editor = mEmptyActionEditor;
        }

        //Remove the modification listener from the editor
        if(mActionEditor != null)
        {
            mActionEditor.modifiedProperty().removeListener(mActionEditorModificationListener);
        }

        if(mActionEditor != editor)
        {
            getActionEditorBox().getChildren().remove(mActionEditor);
            mActionEditor = editor;
            getActionEditorBox().getChildren().add(mActionEditor);
        }

        mActionEditor.setItem(aliasAction);

        //Add the modification listener back to the editor
        mActionEditor.modifiedProperty().addListener(mActionEditorModificationListener);
    }

    private ListView<AliasAction> getActionsList()
    {
        if(mActionsList == null)
        {
            mActionsList = new ListView<>(FXCollections.observableArrayList(AliasAction.extractor()));
            mActionsList.setPrefHeight(75);
            mActionsList.setDisable(true);
            mActionsList.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> setAction(newValue));
        }

        return mActionsList;
    }

    private MenuButton getAddActionButton()
    {
        if(mAddActionButton == null)
        {
            mAddActionButton = new MenuButton("Add Action");
            mAddActionButton.setDisable(true);
            mAddActionButton.setMaxWidth(Double.MAX_VALUE);
            mAddActionButton.getItems().addAll(new AddAudioClipActionItem(), new AddBeepActionItem(),
                new AddScriptActionItem());
        }

        return mAddActionButton;
    }

    private Button getDeleteActionButton()
    {
        if(mDeleteActionButton == null)
        {
            mDeleteActionButton = new Button("Delete Action");
            mDeleteActionButton.setDisable(true);
            mDeleteActionButton.setMaxWidth(Double.MAX_VALUE);
            mDeleteActionButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    AliasAction selected = getActionsList().getSelectionModel().getSelectedItem();

                    if(selected != null)
                    {
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                            "Do you want to delete the selected alias action?", ButtonType.NO, ButtonType.YES);
                        alert.setTitle("Delete Alias Action");
                        alert.setHeaderText("Are you sure?");
                        alert.initOwner(((Node)getDeleteActionButton()).getScene().getWindow());

                        Optional<ButtonType> result = alert.showAndWait();

                        if(result.get() == ButtonType.YES)
                        {
                            getActionsList().getItems().remove(selected);
                            modifiedProperty().set(true);
                        }
                    }
                }
            });
        }

        return mDeleteActionButton;
    }

    /**
     * Updates the editor and loads the alias id for editing/viewing
     * @param aliasID
     */
    private void setIdentifier(AliasID aliasID)
    {
        IdentifierEditor editor = null;

        if(aliasID != null)
        {
            editor = mIdentifierEditorMap.get(aliasID.getType());

            if(editor == null)
            {
                editor = IdentifierEditorFactory.getEditor(aliasID.getType(), mUserPreferences);
                mIdentifierEditorMap.put(aliasID.getType(), editor);
            }
        }

        getDeleteIdentifierButton().setDisable(aliasID == null);
        getShowOverlapButton().setVisible(aliasID != null && aliasID.overlapProperty().get());

        if(editor == null)
        {
            editor = mEmptyIdentifierEditor;
        }

        //Remove the modification listener from the editor
        if(mIdentifierEditor != null)
        {
            mIdentifierEditor.modifiedProperty().removeListener(mIdentifierEditorModificationListener);
        }

        if(mIdentifierEditor != editor)
        {
            getIdentifierEditorBox().getChildren().remove(mIdentifierEditor);
            mIdentifierEditor = editor;
            getIdentifierEditorBox().getChildren().add(mIdentifierEditor);
        }

        mIdentifierEditor.setItem(aliasID);

        //Add the modification listener back to the editor
        mIdentifierEditor.modifiedProperty().addListener(mIdentifierEditorModificationListener);
    }

    private ListView<AliasID> getIdentifiersList()
    {
        if(mIdentifiersList == null)
        {
            mIdentifiersList = new ListView<>(FXCollections.observableArrayList(AliasID.extractor()));
            mIdentifiersList.setDisable(true);
            mIdentifiersList.setPrefHeight(75);
            mIdentifiersList.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> setIdentifier(newValue));
            mIdentifiersList.setCellFactory(param -> new AliasIdentifierCell());
        }

        return mIdentifiersList;
    }

    private Button getShowOverlapButton()
    {
        if(mShowOverlapButton == null)
        {
            mShowOverlapButton = new Button("Show Overlap");
            mShowOverlapButton.setMaxWidth(Double.MAX_VALUE);
            mShowOverlapButton.setVisible(false);
            mShowOverlapButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    AliasID selected = getIdentifiersList().getSelectionModel().getSelectedItem();

                    if(selected != null)
                    {
                        MyEventBus.getGlobalEventBus().post(new ViewAliasIdentifierRequest(selected));
                    }
                }
            });
        }

        return mShowOverlapButton;
    }

    private MenuButton getAddIdentifierButton()
    {
        if(mAddIdentifierButton == null)
        {
            mAddIdentifierButton = new MenuButton("Add Identifier");
            mAddIdentifierButton.setMaxWidth(Double.MAX_VALUE);
            mAddIdentifierButton.setDisable(true);

            Menu amMenu = new ProtocolMenu(Protocol.AM);
            amMenu.getItems().add(new AddTalkgroupItem(Protocol.AM));
            amMenu.getItems().add(new AddTalkgroupRangeItem(Protocol.AM));

            Menu p25Menu = new ProtocolMenu(Protocol.APCO25);
            p25Menu.getItems().add(new AddTalkgroupItem(Protocol.APCO25));
            p25Menu.getItems().add(new AddTalkgroupRangeItem(Protocol.APCO25));
            p25Menu.getItems().add(new AddP25FullyQualifiedTalkgroupItem());
            p25Menu.getItems().add(new AddRadioIdItem(Protocol.APCO25));
            p25Menu.getItems().add(new AddRadioIdRangeItem(Protocol.APCO25));
            p25Menu.getItems().add(new AddP25FullyQualifiedRadioIdItem());
            p25Menu.getItems().add(new SeparatorMenuItem());
            p25Menu.getItems().add(new AddUserStatusItem());
            p25Menu.getItems().add(new AddUnitStatusItem());
            p25Menu.getItems().add(new SeparatorMenuItem());
            p25Menu.getItems().add(new AddTonesItem("AMBE Audio Tones (Phase 2 Only)"));

            Menu dmrMenu = new ProtocolMenu(Protocol.DMR);
            dmrMenu.getItems().add(new AddTalkgroupItem(Protocol.DMR));
            dmrMenu.getItems().add(new AddTalkgroupRangeItem(Protocol.DMR));
            dmrMenu.getItems().add(new AddRadioIdItem(Protocol.DMR));
            dmrMenu.getItems().add(new AddRadioIdRangeItem(Protocol.DMR));
            dmrMenu.getItems().add(new AddTonesItem("AMBE Audio Tones"));


            Menu fleetsyncMenu = new ProtocolMenu(Protocol.FLEETSYNC);
            fleetsyncMenu.getItems().add(new AddTalkgroupItem(Protocol.FLEETSYNC));
            fleetsyncMenu.getItems().add(new AddTalkgroupRangeItem(Protocol.FLEETSYNC));

            Menu ltrMenu = new ProtocolMenu(Protocol.LTR);
            ltrMenu.getItems().add(new AddTalkgroupItem(Protocol.LTR));
            ltrMenu.getItems().add(new AddTalkgroupRangeItem(Protocol.LTR));

            Menu mdcMenu = new ProtocolMenu(Protocol.MDC1200);
            mdcMenu.getItems().add(new AddTalkgroupItem(Protocol.MDC1200));
            mdcMenu.getItems().add(new AddTalkgroupRangeItem(Protocol.MDC1200));

            Menu mptMenu = new ProtocolMenu(Protocol.MPT1327);
            mptMenu.getItems().add(new AddTalkgroupItem(Protocol.MPT1327));
            mptMenu.getItems().add(new AddTalkgroupRangeItem(Protocol.MPT1327));

            Menu nbfmMenu = new ProtocolMenu(Protocol.NBFM);
            nbfmMenu.getItems().add(new AddTalkgroupItem(Protocol.NBFM));
            nbfmMenu.getItems().add(new AddTalkgroupRangeItem(Protocol.NBFM));
            nbfmMenu.getItems().add(new AddDcsItem());

            Menu passportMenu = new ProtocolMenu(Protocol.PASSPORT);
            passportMenu.getItems().add(new AddTalkgroupItem(Protocol.PASSPORT));
            passportMenu.getItems().add(new AddTalkgroupRangeItem(Protocol.PASSPORT));
            passportMenu.getItems().add(new AddRadioIdItem(Protocol.PASSPORT));
            passportMenu.getItems().add(new AddRadioIdRangeItem(Protocol.PASSPORT));

            Menu taitMenu = new ProtocolMenu(Protocol.TAIT1200);
            taitMenu.setDisable(true);  //tbd

            Menu lojackMenu = new ProtocolMenu(Protocol.LOJACK);
            lojackMenu.getItems().add(new AddLojackItem());

            mAddIdentifierButton.getItems().addAll(amMenu, p25Menu, dmrMenu, fleetsyncMenu, ltrMenu, mdcMenu, mptMenu,
                nbfmMenu, passportMenu, taitMenu, new SeparatorMenuItem(), lojackMenu);
        }

        return mAddIdentifierButton;
    }

    private Button getDeleteIdentifierButton()
    {
        if(mDeleteIdentifierButton == null)
        {
            mDeleteIdentifierButton = new Button("Delete Identifier");
            mDeleteIdentifierButton.setMaxWidth(Double.MAX_VALUE);
            mDeleteIdentifierButton.setDisable(true);
            mDeleteIdentifierButton.setOnAction(event -> {
                AliasID selected = getIdentifiersList().getSelectionModel().getSelectedItem();

                if(selected != null)
                {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                        "Do you want to delete the selected alias identifier?", ButtonType.NO, ButtonType.YES);
                    alert.setTitle("Delete Alias Identifier");
                    alert.setHeaderText("Are you sure?");
                    alert.initOwner(((Node)getDeleteIdentifierButton()).getScene().getWindow());

                    Optional<ButtonType> result = alert.showAndWait();

                    if(result.get() == ButtonType.YES)
                    {
                        getIdentifiersList().getItems().remove(selected);
                        modifiedProperty().set(true);
                    }
                }
            });
        }

        return mDeleteIdentifierButton;
    }

    private TitledPane getStreamPane()
    {
        if(mStreamPane == null)
        {
            VBox buttonBox = new VBox();
            buttonBox.setMaxHeight(Double.MAX_VALUE);
            buttonBox.setAlignment(Pos.CENTER);
            buttonBox.setSpacing(5);
            buttonBox.getChildren().addAll(new Label(" "), getAddStreamButton(), getRemoveStreamButton());

            VBox availableBox = new VBox();
            VBox.setVgrow(getAvailableStreamsView(), Priority.ALWAYS);
            availableBox.getChildren().addAll(new Label("Available"), getAvailableStreamsView());

            VBox selectedBox = new VBox();
            VBox.setVgrow(getSelectedStreamsView(), Priority.ALWAYS);
            selectedBox.getChildren().addAll(new Label("Selected"), getSelectedStreamsView());

            HBox hbox = new HBox();
            hbox.setSpacing(10);
            HBox.setHgrow(availableBox, Priority.ALWAYS);
            HBox.setHgrow(selectedBox, Priority.ALWAYS);
            hbox.getChildren().addAll(availableBox, buttonBox, selectedBox);

            VBox vbox = new VBox();
            vbox.setSpacing(10);
            VBox.setVgrow(hbox, Priority.ALWAYS);
            Label label = new Label("Stream this Alias as Talkgroup:");
            HBox streamAsHBox = new HBox();
            streamAsHBox.setAlignment(Pos.CENTER_LEFT);
            streamAsHBox.setSpacing(10);
            streamAsHBox.getChildren().addAll(label, getStreamAsTalkgroupField());
            vbox.getChildren().addAll(hbox, streamAsHBox);

            mStreamPane = new TitledPane("Streaming", vbox);
            mStreamPane.setExpanded(false);
        }

        return mStreamPane;
    }

    private void updateStreamViews()
    {
        Platform.runLater(() -> {
            getAvailableStreamsView().getItems().clear();
            getSelectedStreamsView().getItems().clear();
            getAvailableStreamsView().setDisable(getItem() == null);
            getSelectedStreamsView().setDisable(getItem() == null);
            getStreamAsTalkgroupField().setDisable(getItem() == null);

            if(getItem() != null)
            {
                List<String> availableStreams = mPlaylistManager.getBroadcastModel().getBroadcastConfigurationNames();

                Set<BroadcastChannel> selectedChannels = getItem().getBroadcastChannels();

                for(BroadcastChannel channel: selectedChannels)
                {
                    if(availableStreams.contains(channel.getChannelName()))
                    {
                        availableStreams.remove(channel.getChannelName());
                    }
                }

                getSelectedStreamsView().getItems().addAll(selectedChannels);
                getAvailableStreamsView().getItems().addAll(availableStreams);

                AliasID streamAs = getItem().getStreamTalkgroupAlias();

                if(streamAs instanceof StreamAsTalkgroup sat)
                {
                    mStreamAsIntegerTextFormatter.setValue(sat.getValue());
                }
                else
                {
                    mStreamAsIntegerTextFormatter.setValue(null);
                }
            }
            else
            {
                mStreamAsIntegerTextFormatter.setValue(null);
            }
        });
    }

    private ListView<String> getAvailableStreamsView()
    {
        if(mAvailableStreamsView == null)
        {
            mAvailableStreamsView = new ListView<>();
            mAvailableStreamsView.setDisable(true);
            mAvailableStreamsView.setPrefHeight(75);
        }

        return mAvailableStreamsView;
    }

    private ListView<BroadcastChannel> getSelectedStreamsView()
    {
        if(mSelectedStreamsView == null)
        {
            mSelectedStreamsView = new ListView<>();
            mSelectedStreamsView.setDisable(true);
            mSelectedStreamsView.setPrefHeight(75);
            mSelectedStreamsView.getItems().addListener((ListChangeListener<BroadcastChannel>)c -> {
                String title = "Streaming";

                if(getSelectedStreamsView().getItems().size() > 0)
                {
                    title += " (" + getSelectedStreamsView().getItems().size() + ")";
                }

                getStreamPane().setText(title);
            });
        }

        return mSelectedStreamsView;
    }

    private Button getAddStreamButton()
    {
        if(mAddStreamButton == null)
        {
            mAddStreamButton = new Button();
            mAddStreamButton.disableProperty().bind(Bindings.isEmpty(getAvailableStreamsView().getItems())
                    .or(Bindings.isNull(getAvailableStreamsView().getSelectionModel().selectedItemProperty())));
            mAddStreamButton.setMaxWidth(Double.MAX_VALUE);
            mAddStreamButton.setGraphic(new IconNode(FontAwesome.ANGLE_RIGHT));
            mAddStreamButton.setAlignment(Pos.CENTER);
            mAddStreamButton.setOnAction(event -> {
                String stream = getAvailableStreamsView().getSelectionModel().getSelectedItem();

                if(stream != null)
                {
                    getAvailableStreamsView().getItems().remove(stream);
                    getSelectedStreamsView().getItems().add(new BroadcastChannel(stream));
                    modifiedProperty().set(true);
                }
            });

        }

        return mAddStreamButton;
    }

    private Button getRemoveStreamButton()
    {
        if(mRemoveStreamButton == null)
        {
            mRemoveStreamButton = new Button();
            mRemoveStreamButton.disableProperty().bind(Bindings.isEmpty(getSelectedStreamsView().getItems())
                    .or(Bindings.isNull(getSelectedStreamsView().getSelectionModel().selectedItemProperty())));
            mRemoveStreamButton.setMaxWidth(Double.MAX_VALUE);
            mRemoveStreamButton.setGraphic(new IconNode(FontAwesome.ANGLE_LEFT));
            mRemoveStreamButton.setAlignment(Pos.CENTER);
            mRemoveStreamButton.setOnAction(event -> {
                BroadcastChannel broadcastChannel = getSelectedStreamsView().getSelectionModel().getSelectedItem();

                if(broadcastChannel != null)
                {
                    getSelectedStreamsView().getItems().remove(broadcastChannel);
                    getAvailableStreamsView().getItems().add(broadcastChannel.getChannelName());
                    modifiedProperty().set(true);
                }
            });
        }

        return mRemoveStreamButton;
    }

    private TitledPane getActionPane()
    {
        if(mActionPane == null)
        {
            VBox buttonsBox = new VBox();
            buttonsBox.setSpacing(10);
            buttonsBox.getChildren().addAll(getAddActionButton(), getDeleteActionButton());

            HBox hbox = new HBox();
            hbox.setSpacing(10);
            HBox.setHgrow(getActionEditorBox(), Priority.ALWAYS);
            hbox.getChildren().addAll(getActionEditorBox(), buttonsBox);

            mActionPane = new TitledPane("Actions", hbox);
            mActionPane.setExpanded(false);
        }

        return mActionPane;
    }

    private VBox getActionEditorBox()
    {
        if(mActionEditorBox == null)
        {
            mActionEditorBox = new VBox();
            mActionEditorBox.setSpacing(10);
            mActionEditorBox.getChildren().addAll(getActionsList(), getActionEditor());
        }

        return mActionEditorBox;
    }

    private GridPane getTextFieldPane()
    {
        if(mTextFieldPane == null)
        {
            mTextFieldPane = new GridPane();
            mTextFieldPane.setPadding(new Insets(10, 10, 10,10));
            mTextFieldPane.setVgap(10);
            mTextFieldPane.setHgap(10);

            int row = 0;

            Label nameLabel = new Label("Alias");
            GridPane.setHalignment(nameLabel, HPos.RIGHT);
            GridPane.setConstraints(nameLabel, 0, row);
            mTextFieldPane.getChildren().add(nameLabel);
            GridPane.setConstraints(getNameField(), 1, row);
            GridPane.setHgrow(getNameField(), Priority.ALWAYS);
            mTextFieldPane.getChildren().add(getNameField());

            Label monitorAudioLabel = new Label("Listen");
            GridPane.setHalignment(monitorAudioLabel, HPos.RIGHT);
            GridPane.setConstraints(monitorAudioLabel, 2, row);
            mTextFieldPane.getChildren().add(monitorAudioLabel);
            GridPane.setConstraints(getMonitorAudioToggleSwitch(), 3, row);
            mTextFieldPane.getChildren().add(getMonitorAudioToggleSwitch());

            Label monitorPriorityLabel = new Label("Priority");
            GridPane.setHalignment(monitorPriorityLabel, HPos.RIGHT);
            GridPane.setConstraints(monitorPriorityLabel, 4, row);
            mTextFieldPane.getChildren().add(monitorPriorityLabel);
            GridPane.setConstraints(getMonitorPriorityComboBox(), 5, row);
            mTextFieldPane.getChildren().add(getMonitorPriorityComboBox());

            Label colorLabel = new Label("Color");
            GridPane.setHalignment(colorLabel, HPos.RIGHT);
            GridPane.setConstraints(colorLabel, 6, row);
            mTextFieldPane.getChildren().add(colorLabel);
            GridPane.setConstraints(getColorPicker(), 7, row);
            mTextFieldPane.getChildren().add(getColorPicker());

            Label groupLabel = new Label("Group");
            GridPane.setHalignment(groupLabel, HPos.RIGHT);
            GridPane.setConstraints(groupLabel, 0, ++row);
            mTextFieldPane.getChildren().add(groupLabel);
            GridPane.setConstraints(getGroupField(), 1, row);
            GridPane.setHgrow(getGroupField(), Priority.ALWAYS);
            mTextFieldPane.getChildren().add(getGroupField());

            Label recordAudioLabel = new Label("Record");
            GridPane.setHalignment(recordAudioLabel, HPos.RIGHT);
            GridPane.setConstraints(recordAudioLabel, 2, row);
            mTextFieldPane.getChildren().add(recordAudioLabel);
            GridPane.setConstraints(getRecordAudioToggleSwitch(), 3, row);
            mTextFieldPane.getChildren().add(getRecordAudioToggleSwitch());

            Label iconLabel = new Label("Icon");
            GridPane.setHalignment(iconLabel, HPos.RIGHT);
            GridPane.setConstraints(iconLabel, 4, row);
            mTextFieldPane.getChildren().add(iconLabel);
            GridPane.setConstraints(getIconNodeComboBox(), 5, row, 3, 1);
            mTextFieldPane.getChildren().add(getIconNodeComboBox());
        }

        return mTextFieldPane;
    }

    private ToggleSwitch getMonitorAudioToggleSwitch()
    {
        if(mMonitorAudioToggleSwitch == null)
        {
            mMonitorAudioToggleSwitch = new ToggleSwitch();
            mMonitorAudioToggleSwitch.setDisable(true);
            mMonitorAudioToggleSwitch.selectedProperty()
                .addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mMonitorAudioToggleSwitch;
    }

    private ComboBox<Integer> getMonitorPriorityComboBox()
    {
        if(mMonitorPriorityComboBox == null)
        {
            mMonitorPriorityComboBox = new ComboBox<>();
            mMonitorPriorityComboBox.getItems().add(null);
            for(int x = io.github.dsheirer.alias.id.priority.Priority.MIN_PRIORITY;
                    x < io.github.dsheirer.alias.id.priority.Priority.MAX_PRIORITY; x++)
            {
                mMonitorPriorityComboBox.getItems().add(x);
            }

            mMonitorPriorityComboBox.disableProperty().bind(getMonitorAudioToggleSwitch().selectedProperty().not());
            mMonitorPriorityComboBox.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mMonitorPriorityComboBox;
    }

    private ToggleSwitch getRecordAudioToggleSwitch()
    {
        if(mRecordAudioToggleSwitch == null)
        {
            mRecordAudioToggleSwitch = new ToggleSwitch();
            mRecordAudioToggleSwitch.setDisable(true);
            mRecordAudioToggleSwitch.selectedProperty()
                .addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mRecordAudioToggleSwitch;
    }

    private ColorPicker getColorPicker()
    {
        if(mColorPicker == null)
        {
            mColorPicker = new ColorPicker(Color.BLACK);
            mColorPicker.setDisable(true);
            mColorPicker.setEditable(true);
            mColorPicker.setStyle("-fx-color-rect-width: 60px; -fx-color-label-visible: false;");
            mColorPicker.setOnAction(event -> modifiedProperty().set(true));
        }

        return mColorPicker;
    }

    private ComboBox<Icon> getIconNodeComboBox()
    {
        if(mIconNodeComboBox == null)
        {
            mIconNodeComboBox = new ComboBox<>();
            mIconNodeComboBox.setMaxWidth(Double.MAX_VALUE);
            mIconNodeComboBox.setDisable(true);
            mIconNodeComboBox.setItems(new SortedList(mPlaylistManager.getIconModel().iconsProperty(), Ordering.natural()));
            mIconNodeComboBox.setCellFactory(new IconCellFactory());
            mIconNodeComboBox.getSelectionModel().selectedItemProperty()
                    .addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mIconNodeComboBox;
    }

    /**
     * Refreshes the system and site text field auto-completion lists.
     */
    private void refreshAutoCompleteBindings()
    {
        getGroupSuggestionProvider().clearSuggestions();
        getGroupSuggestionProvider().addPossibleSuggestions(mPlaylistManager.getAliasModel().getGroupNames());
    }

    private SuggestionProvider<String> getGroupSuggestionProvider()
    {
        if(mGroupSuggestionProvider == null)
        {
            mGroupSuggestionProvider = SuggestionProvider.create(mPlaylistManager.getAliasModel().getGroupNames());
        }

        return mGroupSuggestionProvider;
    }

    protected TextField getGroupField()
    {
        if(mGroupField == null)
        {
            mGroupField = new TextField();
            mGroupField.setDisable(true);
            mGroupField.setMaxWidth(Double.MAX_VALUE);
            mGroupField.textProperty().addListener(mEditorModificationListener);
            new AutoCompletionTextFieldBinding<>(mGroupField, getGroupSuggestionProvider());
        }

        return mGroupField;
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

    private VBox getButtonBox()
    {
        if(mButtonBox == null)
        {
            mButtonBox = new VBox();
            mButtonBox.setSpacing(10);
            mButtonBox.setPadding(new Insets(10, 10, 10, 0));
            mButtonBox.getChildren().addAll(getSaveButton(), getResetButton());
        }

        return mButtonBox;
    }

    private Button getSaveButton()
    {
        if(mSaveButton == null)
        {
            mSaveButton = new Button(" Save ");
            mSaveButton.setTextAlignment(TextAlignment.CENTER);
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
            mResetButton.setTextAlignment(TextAlignment.CENTER);
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
     * Simple menut that uses the protocol label for the menu label
     */
    public class ProtocolMenu extends Menu
    {
        public ProtocolMenu(Protocol protocol)
        {
            super(protocol.toString());
        }
    }

    /**
     * Menu Item for adding a new ESN alias identifier
     */
    public class AddEsnItem extends MenuItem
    {
        public AddEsnItem()
        {
            super("ESN");
            setOnAction(event -> {
                Esn esn = new Esn();
                getIdentifiersList().getItems().add(esn);
                getIdentifiersList().getSelectionModel().select(esn);
                getIdentifiersList().scrollTo(esn);
                modifiedProperty().set(true);
            });
        }
    }

    /**
     * Menu Item for adding a new lojack aliasidentifier
     */
    public class AddLojackItem extends MenuItem
    {
        public AddLojackItem()
        {
            super("LoJack Function/ID");
            setOnAction(event -> {
                LoJackFunctionAndID lojack = new LoJackFunctionAndID();
                getIdentifiersList().getItems().add(lojack);
                getIdentifiersList().getSelectionModel().select(lojack);
                getIdentifiersList().scrollTo(lojack);
                modifiedProperty().set(true);
            });
        }
    }

    /**
     * Menu Item for adding a new unit status alias identifier
     */
    public class AddUnitStatusItem extends MenuItem
    {
        public AddUnitStatusItem()
        {
            super("Unit Status");
            setOnAction(event -> {
                UnitStatusID unitStatus = new UnitStatusID();
                getIdentifiersList().getItems().add(unitStatus);
                getIdentifiersList().getSelectionModel().select(unitStatus);
                getIdentifiersList().scrollTo(unitStatus);
                modifiedProperty().set(true);
            });
        }
    }

    /**
     * Menu Item for adding a new user status alias identifier
     */
    public class AddUserStatusItem extends MenuItem
    {
        public AddUserStatusItem()
        {
            super("User Status");
            setOnAction(event -> {
                UserStatusID userStatus = new UserStatusID();
                getIdentifiersList().getItems().add(userStatus);
                getIdentifiersList().getSelectionModel().select(userStatus);
                getIdentifiersList().scrollTo(userStatus);
                modifiedProperty().set(true);
            });
        }
    }

    public class AddTonesItem extends MenuItem
    {
        public AddTonesItem(String label)
        {
            super(label);
            setOnAction(event -> {
                TonesID tonesId = new TonesID();
                getIdentifiersList().getItems().add(tonesId);
                getIdentifiersList().getSelectionModel().select(tonesId);
                getIdentifiersList().scrollTo(tonesId);
                modifiedProperty().set(true);
            });
        }
    }

    /**
     * Add Digital Coded Squelch (DCS) alias identifier menu item
     */
    public class AddDcsItem extends MenuItem
    {
        public AddDcsItem()
        {
            super("Digital Coded Squelch (DCS)");
            setOnAction(event -> {
                Dcs dcs = new Dcs();
                getIdentifiersList().getItems().add(dcs);
                getIdentifiersList().getSelectionModel().select(dcs);
                getIdentifiersList().scrollTo(dcs);
                modifiedProperty().set(true);
            });
        }
    }

    /**
     * Menu Item for adding a new protocol-specific Radio ID alias identifier
     */
    public class AddRadioIdItem extends MenuItem
    {
        private Protocol mProtocol;

        public AddRadioIdItem(Protocol protocol)
        {
            super("Radio ID");
            mProtocol = protocol;
            setOnAction(event -> {
                Radio radioId = new Radio();
                radioId.setProtocol(mProtocol);
                getIdentifiersList().getItems().add(radioId);
                getIdentifiersList().getSelectionModel().select(radioId);
                getIdentifiersList().scrollTo(radioId);
                modifiedProperty().set(true);
            });
        }
    }

    /**
     * Menu Item for adding a new protocol-specific Fully Qualified Radio ID alias identifier
     */
    public class AddP25FullyQualifiedRadioIdItem extends MenuItem
    {
        public AddP25FullyQualifiedRadioIdItem()
        {
            super("Fully Qualified Radio ID");
            setOnAction(event -> {
                P25FullyQualifiedRadio radioId = new P25FullyQualifiedRadio();
                radioId.setProtocol(Protocol.APCO25);
                getIdentifiersList().getItems().add(radioId);
                getIdentifiersList().getSelectionModel().select(radioId);
                getIdentifiersList().scrollTo(radioId);
                modifiedProperty().set(true);
            });
        }
    }

    /**
     * Menu Item for adding a new protocol-specific Fully Qualified Talkgroup alias identifier
     */
    public class AddP25FullyQualifiedTalkgroupItem extends MenuItem
    {
        public AddP25FullyQualifiedTalkgroupItem()
        {
            super("Fully Qualified Talkgroup");
            setOnAction(event -> {
                P25FullyQualifiedTalkgroup talkgroup = new P25FullyQualifiedTalkgroup();
                talkgroup.setProtocol(Protocol.APCO25);
                getIdentifiersList().getItems().add(talkgroup);
                getIdentifiersList().getSelectionModel().select(talkgroup);
                getIdentifiersList().scrollTo(talkgroup);
                modifiedProperty().set(true);
            });
        }
    }

    /**
     * Menu Item for adding a new protocol-specific Radio ID range alias identifier
     */
    public class AddRadioIdRangeItem extends MenuItem
    {
        private Protocol mProtocol;

        public AddRadioIdRangeItem(Protocol protocol)
        {
            super("Radio ID Range");
            mProtocol = protocol;
            setOnAction(event -> {
                RadioRange radioRange = new RadioRange();
                radioRange.setProtocol(mProtocol);
                getIdentifiersList().getItems().add(radioRange);
                getIdentifiersList().getSelectionModel().select(radioRange);
                getIdentifiersList().scrollTo(radioRange);
                modifiedProperty().set(true);
            });
        }
    }

    /**
     * Menu Item for adding a new protocol-specific Talkgroup alias identifier
     */
    public class AddTalkgroupItem extends MenuItem
    {
        private Protocol mProtocol;

        public AddTalkgroupItem(Protocol protocol)
        {
            super("Talkgroup");
            mProtocol = protocol;
            setOnAction(event -> {
                Talkgroup talkgroup = new Talkgroup();
                talkgroup.setProtocol(mProtocol);
                getIdentifiersList().getItems().add(talkgroup);
                getIdentifiersList().getSelectionModel().select(talkgroup);
                getIdentifiersList().scrollTo(talkgroup);
                modifiedProperty().set(true);
            });
        }
    }

    /**
     * Menu Item for adding a new protocol-specific Talkgroup Range alias identifier
     */
    public class AddTalkgroupRangeItem extends MenuItem
    {
        private Protocol mProtocol;

        public AddTalkgroupRangeItem(Protocol protocol)
        {
            super("Talkgroup Range");
            mProtocol = protocol;
            setOnAction(event -> {
                TalkgroupRange talkgroupRange = new TalkgroupRange();
                talkgroupRange.setProtocol(mProtocol);
                getIdentifiersList().getItems().add(talkgroupRange);
                getIdentifiersList().getSelectionModel().select(talkgroupRange);
                getIdentifiersList().scrollTo(talkgroupRange);
                modifiedProperty().set(true);
            });
        }
    }

    /**
     * Menu item to add a new beep alias action
     */
    public class AddBeepActionItem extends MenuItem
    {
        public AddBeepActionItem()
        {
            super("Beep");

            setOnAction(event -> {
                if(getItem() != null)
                {
                    BeepAction beepAction = new BeepAction();
                    getActionsList().getItems().add(beepAction);
                    getActionsList().getSelectionModel().select(beepAction);
                    getActionsList().scrollTo(beepAction);
                    modifiedProperty().set(true);
                }
            });
        }
    }

    /**
     * Menu item to add a new audio clip alias action
     */
    public class AddAudioClipActionItem extends MenuItem
    {
        public AddAudioClipActionItem()
        {
            super("Audio Clip");

            setOnAction(event -> {
                if(getItem() != null)
                {
                    ClipAction clipAction = new ClipAction();
                    getActionsList().getItems().add(clipAction);
                    getActionsList().getSelectionModel().select(clipAction);
                    getActionsList().scrollTo(clipAction);
                    modifiedProperty().set(true);
                }
            });
        }
    }

    /**
     * Menu item to add a new script alias action
     */
    public class AddScriptActionItem extends MenuItem
    {
        public AddScriptActionItem()
        {
            super("Script");

            setOnAction(event -> {
                if(getItem() != null)
                {
                    ScriptAction scriptAction = new ScriptAction();
                    getActionsList().getItems().add(scriptAction);
                    getActionsList().getSelectionModel().select(scriptAction);
                    getActionsList().scrollTo(scriptAction);
                    modifiedProperty().set(true);
                }
            });
        }
    }

    /**
     * Monitors for changes to the identifier editors modified property to in-turn set this editor's modified property
     */
    public class IdentifierEditorModificationListener implements ChangeListener<Boolean>
    {
        @Override
        public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue)
        {
            if(newValue)
            {
                modifiedProperty().set(true);
            }
        }
    }

    /**
     * Monitors for changes to the identifier editors modified property to in-turn set this editor's modified property
     */
    public class ActionEditorModificationListener implements ChangeListener<Boolean>
    {
        @Override
        public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue)
        {
            if(newValue)
            {
                modifiedProperty().set(true);
            }
        }
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

    public class AliasIdentifierCell extends ListCell<AliasID>
    {
        @Override
        protected void updateItem(AliasID item, boolean empty)
        {
            super.updateItem(item, empty);

            if(item != null)
            {
                if(item instanceof P25FullyQualifiedTalkgroup fqt)
                {
                    StringBuilder sb = new StringBuilder();
                    sb.append("APCO-25 Fully Qualified Talkgroup:").append(fqt.getWacn());
                    sb.append(".").append(fqt.getSystem());
                    sb.append(".").append(fqt.getValue());

                    if(fqt.overlapProperty().get())
                    {
                        sb.append(" - Error: Overlap");
                    }

                    if(!fqt.isValid())
                    {
                        sb.append(" **NOT VALID**");
                    }
                    setText(sb.toString());
                }
                else if(item instanceof Talkgroup)
                {
                    Talkgroup talkgroup = (Talkgroup)item;
                    Protocol protocol = talkgroup.getProtocol();
                    IntegerFormat integerFormat = mUserPreferences.getTalkgroupFormatPreference()
                        .getTalkgroupFormat(protocol);
                    String formatted = TalkgroupFormatter.format(protocol, talkgroup.getValue(), integerFormat);
                    StringBuilder sb = new StringBuilder();

                    sb.append("Talkgroup:").append(formatted);
                    sb.append(" Protocol:").append((talkgroup.getProtocol()));

                    if(talkgroup.overlapProperty().get())
                    {
                        sb.append(" - Error: Overlap");
                    }

                    if(!talkgroup.isValid())
                    {
                        sb.append(" **NOT VALID**");
                    }

                    setText(sb.toString());
                }
                else if(item instanceof TalkgroupRange)
                {
                    TalkgroupRange talkgroupRange = (TalkgroupRange)item;
                    Protocol protocol = talkgroupRange.getProtocol();
                    IntegerFormat integerFormat = mUserPreferences.getTalkgroupFormatPreference()
                        .getTalkgroupFormat(protocol);
                    String formattedMin = TalkgroupFormatter.format(protocol, talkgroupRange.getMinTalkgroup(), integerFormat);
                    String formattedMax = TalkgroupFormatter.format(protocol, talkgroupRange.getMaxTalkgroup(), integerFormat);
                    StringBuilder sb = new StringBuilder();
                    sb.append("Talkgroup Range:").append(formattedMin).append(" to ").append(formattedMax);
                    sb.append(" Protocol:").append((talkgroupRange.getProtocol()));

                    if(talkgroupRange.overlapProperty().get())
                    {
                        sb.append(" - Error: Overlap");
                    }

                    if(!talkgroupRange.isValid())
                    {
                        sb.append(" **NOT VALID**");
                    }
                    setText(sb.toString());
                }
                else if(item instanceof P25FullyQualifiedRadio fqr)
                {
                    StringBuilder sb = new StringBuilder();
                    sb.append("APCO-25 Fully Qualified Radio ID:").append(fqr.getWacn());
                    sb.append(".").append(fqr.getSystem());
                    sb.append(".").append(fqr.getValue());

                    if(fqr.overlapProperty().get())
                    {
                        sb.append(" - Error: Overlap");
                    }

                    if(!fqr.isValid())
                    {
                        sb.append(" **NOT VALID**");
                    }
                    setText(sb.toString());
                }
                else if(item instanceof Radio)
                {
                    Radio radio = (Radio)item;
                    Protocol protocol = radio.getProtocol();
                    IntegerFormat integerFormat = mUserPreferences.getTalkgroupFormatPreference()
                        .getTalkgroupFormat(protocol);
                    String formatted = RadioFormatter.format(protocol, radio.getValue(), integerFormat);
                    StringBuilder sb = new StringBuilder();
                    sb.append("Radio ID:").append(formatted);
                    sb.append(" Protocol:").append((radio.getProtocol()));

                    if(radio.overlapProperty().get())
                    {
                        sb.append(" - Error: Overlap");
                    }

                    if(!radio.isValid())
                    {
                        sb.append(" **NOT VALID**");
                    }
                    setText(sb.toString());
                }
                else if(item instanceof RadioRange)
                {
                    RadioRange radioRange = (RadioRange)item;
                    Protocol protocol = radioRange.getProtocol();
                    IntegerFormat integerFormat = mUserPreferences.getTalkgroupFormatPreference()
                        .getTalkgroupFormat(protocol);
                    String formattedMin = TalkgroupFormatter.format(protocol, radioRange.getMinRadio(), integerFormat);
                    String formattedMax = TalkgroupFormatter.format(protocol, radioRange.getMaxRadio(), integerFormat);
                    StringBuilder sb = new StringBuilder();
                    sb.append("Radio ID Range:").append(formattedMin).append(" to ").append(formattedMax);
                    sb.append(" Protocol:").append((radioRange.getProtocol()));

                    if(radioRange.overlapProperty().get())
                    {
                        sb.append(" - Error: Overlap");
                    }

                    if(!radioRange.isValid())
                    {
                        sb.append(" **NOT VALID**");
                    }
                    setText(sb.toString());
                }
                else
                {
                    setText(item.toString());
                }
            }
            else
            {
                setText(null);
            }
        }
    }

    /**
     * Cell factory for combo box for dislaying icon name and graphic
     */
    public class IconCellFactory implements Callback<ListView<Icon>, ListCell<Icon>>
    {
        @Override
        public ListCell<Icon> call(ListView<Icon> param)
        {
            Label iconLabel = new Label();
            Label textLabel = new Label();
            GridPane gridPane = new GridPane();
            gridPane.setHgap(5);
            GridPane.setHalignment(iconLabel, HPos.RIGHT);
            gridPane.getColumnConstraints().add(new ColumnConstraints(50));
            gridPane.add(iconLabel, 0, 0);
            gridPane.add(textLabel,1,0);

            ListCell<Icon> cell = new ListCell<>()
            {
                @Override
                protected void updateItem(Icon item, boolean empty)
                {
                    super.updateItem(item, empty);

                    if(empty)
                    {
                        setText(null);
                        setGraphic(null);
                    }
                    else
                    {
                        textLabel.setText(item.getName());
                        iconLabel.setGraphic(new ImageView(item.getFxImage()));
                        setGraphic(gridPane);
                    }
                }
            };

            return cell;
        }
    }
}
