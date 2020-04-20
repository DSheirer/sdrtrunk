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

package io.github.dsheirer.gui.preference;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.protocol.Protocol;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.Map;

/**
 * Preferences editor dialog
 */
public class PreferencesEditor extends Application
{
    private final static Logger mLog = LoggerFactory.getLogger(PreferencesEditor.class);

    private Map<PreferenceEditorType,Node> mEditors = new EnumMap<>(PreferenceEditorType.class);
    private UserPreferences mUserPreferences;
    private HBox mParentBox;
    private TreeView mEditorSelectionTreeView;
    private VBox mEditorAndButtonsBox;
    private Node mEditor;
    private HBox mButtonsBox;

    public Stage getStage()
    {
        try
        {
            Window window = getParentBox().getScene().getWindow();

            if(window instanceof Stage)
            {
                return (Stage)window;
            }
        }
        catch(Throwable t)
        {
            mLog.debug("Error", t);
        }

        return null;
    }

    public PreferencesEditor(UserPreferences userPreferences)
    {
        mUserPreferences = userPreferences;
    }

    private UserPreferences getUserPreferences()
    {
        if(mUserPreferences == null)
        {
            mUserPreferences = new UserPreferences();
        }

        return mUserPreferences;
    }

    @Override
    public void start(Stage stage) throws Exception
    {
        stage.setTitle("Preferences");
        Scene scene = new Scene(getParentBox(), 900, 500);
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Shows the editor specified in the request by scrolling the editor view tree to the selected item.
     */
    public void showEditor(PreferenceEditorViewRequest request)
    {
        TreeItem toSelect = recursivelyFindEditorType(getEditorSelectionTreeView().getRoot(), request.getPreferenceType());

        if(toSelect != null)
        {
            getEditorSelectionTreeView().getSelectionModel().select(toSelect);
        }
    }

    private TreeItem recursivelyFindEditorType(TreeItem parent, PreferenceEditorType type)
    {

        for (TreeItem treeItem : (Iterable<TreeItem>) parent.getChildren()) {
            if (treeItem.getValue() instanceof PreferenceEditorType && (treeItem.getValue()).equals(type)) {
                return treeItem;
            } else {
                TreeItem item = recursivelyFindEditorType(treeItem, type);

                if (item != null) {
                    return item;
                }
            }
        }

        return null;
    }


    /**
     * Primary layout for the editor window
     */
    private HBox getParentBox()
    {
        if(mParentBox == null)
        {
            mParentBox = new HBox();
            mParentBox.getChildren().add(getEditorSelectionTreeView());
            HBox.setHgrow(getEditorAndButtonsBox(), Priority.ALWAYS);
            mParentBox.getChildren().add(getEditorAndButtonsBox());
        }

        return mParentBox;
    }

    private VBox getEditorAndButtonsBox()
    {
        if(mEditorAndButtonsBox == null)
        {
            mEditorAndButtonsBox = new VBox();
            mEditor = getDefaultEditor();
            VBox.setVgrow(getDefaultEditor(), Priority.ALWAYS);
            VBox.setVgrow(getButtonsBox(), Priority.NEVER);
            mEditorAndButtonsBox.getChildren().addAll(getDefaultEditor(), getButtonsBox());
        }

        return mEditorAndButtonsBox;
    }

    private Node getDefaultEditor()
    {
        Node editor = mEditors.get(PreferenceEditorType.DEFAULT);

        if(editor == null)
        {
            VBox defaultEditor = new VBox();
            defaultEditor.setPadding(new Insets(10, 10, 10, 10));
            Label label = new Label("Please select a preference ...");
            defaultEditor.getChildren().add(label);
            mEditors.put(PreferenceEditorType.DEFAULT, defaultEditor);
            editor = defaultEditor;
        }

        return editor;
    }

    /**
     * Preference type selection list
     */
    private TreeView getEditorSelectionTreeView()
    {
        if(mEditorSelectionTreeView == null)
        {
            TreeItem<String> treeRoot = new TreeItem<>("Root node");

            TreeItem<String> audioItem = new TreeItem<>("Audio");
            audioItem.getChildren().add(new TreeItem(PreferenceEditorType.AUDIO_PLAYBACK));
            audioItem.getChildren().add(new TreeItem(PreferenceEditorType.AUDIO_RECORD));
            treeRoot.getChildren().add(audioItem);
            audioItem.setExpanded(true);

            TreeItem<String> decoderItem = new TreeItem<>("Decoder");
            decoderItem.getChildren().add(new TreeItem(PreferenceEditorType.JMBE_LIBRARY));
            treeRoot.getChildren().add(decoderItem);
            decoderItem.setExpanded(true);

            TreeItem<String> displayItem = new TreeItem<>("Display");
            displayItem.getChildren().add(new TreeItem(PreferenceEditorType.CHANNEL_EVENT));
            displayItem.getChildren().add(new TreeItem(PreferenceEditorType.TALKGROUP_FORMAT));
            treeRoot.getChildren().add(displayItem);
            displayItem.setExpanded(true);

            TreeItem<String> storageItem = new TreeItem<>("File Storage");
            storageItem.getChildren().add(new TreeItem(PreferenceEditorType.DIRECTORY));
            treeRoot.getChildren().add(storageItem);
            storageItem.setExpanded(true);

            TreeItem<String> sourceItem = new TreeItem<>("Source");
            sourceItem.getChildren().add(new TreeItem(PreferenceEditorType.SOURCE_CHANNEL_MULTIPLE_FREQUENCY));
            sourceItem.getChildren().add(new TreeItem(PreferenceEditorType.SOURCE_TUNER_CHANNELIZER));
            treeRoot.getChildren().add(sourceItem);
            sourceItem.setExpanded(true);

            mEditorSelectionTreeView = new TreeView();
            mEditorSelectionTreeView.setShowRoot(false);
            mEditorSelectionTreeView.setRoot(treeRoot);
            treeRoot.setExpanded(true);
            mEditorSelectionTreeView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
            mEditorSelectionTreeView.getSelectionModel().selectedItemProperty().addListener(new EditorTreeSelectionListener());

            mEditorSelectionTreeView.setMinWidth(Control.USE_PREF_SIZE);
        }

        return mEditorSelectionTreeView;
    }

    /**
     * Control box with OK button.
     */
    private HBox getButtonsBox()
    {
        if(mButtonsBox == null)
        {
            mButtonsBox = new HBox();
            mButtonsBox.setMaxWidth(Double.MAX_VALUE);
            Button okButton = new Button("Ok");
            okButton.setOnAction(event -> {
                Stage stage = (Stage)getParentBox().getScene().getWindow();
                stage.close();
            });
            HBox.setMargin(okButton, new Insets(5, 5, 5, 5));
            mButtonsBox.setAlignment(Pos.CENTER_RIGHT);
            mButtonsBox.getChildren().add(okButton);
        }

        return mButtonsBox;
    }

    private void setEditor(PreferenceEditorType type)
    {
        Node editor = mEditors.get(type);

        if(editor == null)
        {
            if(type == PreferenceEditorType.DEFAULT)
            {
                editor = getDefaultEditor();
            }
            else
            {
                editor = PreferenceEditorFactory.getEditor(type, getUserPreferences());
                mEditors.put(type, editor);
            }
        }

        getEditorAndButtonsBox().getChildren().remove(mEditor);
        VBox.setVgrow(editor, Priority.ALWAYS);
        mEditor = editor;
        getEditorAndButtonsBox().getChildren().add(0, mEditor);
    }

    /**
     * Listens for editor tree selection events and creates a preference editor instance for each type as needed.
     *
     * Constructed editors are cached for reuse.
     */
    public class EditorTreeSelectionListener implements ChangeListener
    {

        @Override
        public void changed(ObservableValue observable, Object oldValue, Object newValue)
        {
            if(newValue instanceof TreeItem)
            {
                Object value = ((TreeItem)newValue).getValue();

                if(value instanceof PreferenceEditorType)
                {
                    setEditor((PreferenceEditorType)value);
                    return;
                }
            }

            setEditor(PreferenceEditorType.DEFAULT);
        }
    }

    public static void main(String[] args)
    {
        launch(args);
    }
}
