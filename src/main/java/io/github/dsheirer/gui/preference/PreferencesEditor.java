/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/

package io.github.dsheirer.gui.preference;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.properties.SystemProperties;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Map;

/**
 * Preferences editor dialog
 */
public class PreferencesEditor extends Application
{
    private UserPreferences mUserPreferences;
    private BorderPane mBorderPane;
    private TreeView mEditorSelectionTreeView;
    private DecodeEventViewPreferenceEditor mDecodeEventViewPreferenceEditor;
    private VBox mDefaultEditor;
    private HBox mControlBox;

    /**
     * Constructs a preferences editor instance
     */
    public PreferencesEditor()
    {
    }

    public Stage getStage()
    {
        return (Stage) getBorderPane().getScene().getWindow();
    }

    public PreferencesEditor(UserPreferences userPreferences)
    {
        mUserPreferences = userPreferences;
    }

    private UserPreferences getUserPreferences()
    {
        if(mUserPreferences == null)
        {
            mUserPreferences = new UserPreferences(SystemProperties.getInstance());
        }

        return mUserPreferences;
    }

    @Override
    public void start(Stage stage) throws Exception
    {
        stage.setTitle("Preferences");
        Scene scene = new Scene(getBorderPane(), 800, 500);
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Shows the editor specified in the request by scrolling the editor view tree to the selected item.
     */
    public void showEditor(PreferenceEditorViewRequest request)
    {
        for(Object item: getEditorSelectionTreeView().getRoot().getChildren())
        {
            if(item instanceof TreeItem &&
                ((TreeItem)item).getValue() instanceof PreferenceEditorType &&
                ((TreeItem)item).getValue() == request.getPreferenceType())
            {
                getEditorSelectionTreeView().getSelectionModel().select(item);
            }
        }
    }


    /**
     * Primary layout for the editor window
     */
    private BorderPane getBorderPane()
    {
        if(mBorderPane == null)
        {
            mBorderPane = new BorderPane();
            mBorderPane.setLeft(getEditorSelectionTreeView());
            mBorderPane.setCenter(getDefaultEditor());
            mBorderPane.setBottom(getControlBox());
        }

        return mBorderPane;
    }

    private VBox getDefaultEditor()
    {
        if(mDefaultEditor == null)
        {
            mDefaultEditor = new VBox();
            mDefaultEditor.setPadding(new Insets(10, 10, 10, 10));
            Label label = new Label("Please select a preference ...");

            mDefaultEditor.getChildren().add(label);
        }

        return mDefaultEditor;
    }

    /**
     * Preference type selection list
     */
    private TreeView getEditorSelectionTreeView()
    {
        if(mEditorSelectionTreeView == null)
        {
            TreeItem<String> treeRoot = new TreeItem<>("Root node");

            TreeItem<String> displayItem = new TreeItem<>("Display");
            displayItem.getChildren().add(new TreeItem(PreferenceEditorType.CHANNEL_EVENT));
            displayItem.getChildren().add(new TreeItem(PreferenceEditorType.TALKGROUP_FORMAT));
            treeRoot.getChildren().add(displayItem);
            displayItem.setExpanded(true);

            mEditorSelectionTreeView = new TreeView();
            mEditorSelectionTreeView.setShowRoot(false);
            mEditorSelectionTreeView.setRoot(treeRoot);
            treeRoot.setExpanded(true);
            mEditorSelectionTreeView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
            mEditorSelectionTreeView.getSelectionModel().selectedItemProperty().addListener(new EditorTreeSelectionListener());
        }

        return mEditorSelectionTreeView;
    }

    /**
     * Decode event view preferences editor
     */
    private DecodeEventViewPreferenceEditor getDecodeEventViewPreferenceEditor()
    {
        if(mDecodeEventViewPreferenceEditor == null)
        {
            mDecodeEventViewPreferenceEditor = new DecodeEventViewPreferenceEditor(getUserPreferences());
        }

        return mDecodeEventViewPreferenceEditor;
    }

    /**
     * Control box with OK button.
     */
    private HBox getControlBox()
    {
        if(mControlBox == null)
        {
            mControlBox = new HBox();
            Button okButton = new Button("Ok");
            okButton.setOnAction(event -> {
                Stage stage = (Stage) getBorderPane().getScene().getWindow();
                stage.close();
            });
            HBox.setMargin(okButton, new Insets(5, 5, 5, 5));
            mControlBox.setAlignment(Pos.CENTER_RIGHT);
            mControlBox.getChildren().add(okButton);
        }

        return mControlBox;
    }

    /**
     * Listens for editor tree selection events and creates a preference editor instance for each type as needed.
     *
     * Constructed editors are cached for reuse.
     */
    public class EditorTreeSelectionListener implements ChangeListener
    {
        private Map<PreferenceEditorType, Node> mEditors = new HashMap<>();

        @Override
        public void changed(ObservableValue observable, Object oldValue, Object newValue)
        {
            Node editor = getEditor(newValue);
            BorderPane.setAlignment(editor, Pos.CENTER_LEFT);
            getBorderPane().setCenter(editor);
        }

        private Node getEditor(Object treeNodeItem)
        {
            if(treeNodeItem instanceof TreeItem)
            {
                Object value = ((TreeItem) treeNodeItem).getValue();

                if(value instanceof PreferenceEditorType)
                {
                    PreferenceEditorType type = (PreferenceEditorType) value;

                    Node editor = mEditors.get(type);

                    if(editor != null)
                    {
                        return editor;
                    }

                    editor = PreferenceEditorFactory.getEditor(type, getUserPreferences());

                    if(editor != null)
                    {
                        mEditors.put(type, editor);
                        return editor;
                    }
                }
            }

            return getDefaultEditor();
        }
    }

    public static void main(String[] args)
    {
        launch(args);
    }
}
