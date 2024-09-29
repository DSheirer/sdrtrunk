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

import io.github.dsheirer.preference.UserPreferences;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility application to load and view .bits recording file with the messages fully parsed.
 *
 * Supported Protocols: DMR, APCO25 Phase 1 and Phase 2.
 */
public class MessageRecordingViewer extends VBox
{
    private static final Logger mLog = LoggerFactory.getLogger(MessageRecordingViewer.class);
    private MenuBar mMenuBar;
    private TabPane mTabPane;
    private int mTabCounterDmr = 1;
    private int mTabCounterP25P1 = 1;
    private int mTabCounterP25P2 = 1;
    private UserPreferences mUserPreferences = new UserPreferences();

    /**
     * Constructs an instance
     */
    public MessageRecordingViewer()
    {
        VBox.setVgrow(getTabPane(), Priority.ALWAYS);
        getChildren().addAll(getMenuBar(), getTabPane());
    }

    public MenuBar getMenuBar()
    {
        if(mMenuBar == null)
        {
            mMenuBar = new MenuBar();
            Menu fileMenu = new Menu("File");

            Menu createNewViewerMenu = new Menu("New Viewer ...");
            MenuItem dmrMenuItem = new MenuItem("DMR");
            dmrMenuItem.onActionProperty().set(event -> {
                Tab tab = new LabeledTab("DMR-" + mTabCounterDmr++, new DmrViewer());
                getTabPane().getTabs().add(tab);
                getTabPane().getSelectionModel().select(tab);
            });
            MenuItem p25p1MenuItem = new MenuItem("P25 Phase 1");
            p25p1MenuItem.onActionProperty().set(event -> {
                Tab tab = new LabeledTab("P25P1-" + mTabCounterP25P1++, new P25P1Viewer(mUserPreferences));
                getTabPane().getTabs().add(tab);
                getTabPane().getSelectionModel().select(tab);
            });
            MenuItem p25p2MenuItem = new MenuItem("P25 Phase 2");
            p25p2MenuItem.onActionProperty().set(event -> {
                Tab tab = new LabeledTab("P25P2-" + mTabCounterP25P2++, new P25P2Viewer());
                getTabPane().getTabs().add(tab);
                getTabPane().getSelectionModel().select(tab);
            });
            createNewViewerMenu.getItems().addAll(dmrMenuItem, p25p1MenuItem, p25p2MenuItem);

            MenuItem exitMenu = new MenuItem("Exit");
            exitMenu.onActionProperty().set(event -> ((Stage)getScene().getWindow()).close());
            fileMenu.getItems().addAll(createNewViewerMenu, new SeparatorMenuItem(), exitMenu);
            mMenuBar.getMenus().add(fileMenu);
        }

        return mMenuBar;
    }

    /**
     * Tab pane for each viewer instance
     */
    public TabPane getTabPane()
    {
        if(mTabPane == null)
        {
            mTabPane = new TabPane();
            mTabPane.setMaxHeight(Double.MAX_VALUE);
            mTabPane.getTabs().add(new LabeledTab("DMR-" + mTabCounterDmr++, new DmrViewer()));
            mTabPane.getTabs().add(new LabeledTab("P25P1-" + mTabCounterP25P1++, new P25P1Viewer(mUserPreferences)));
            mTabPane.getTabs().add(new LabeledTab("P25P2-" + mTabCounterP25P2++, new P25P2Viewer()));
        }

        return mTabPane;
    }

    /**
     * Decorates the tab with label renaming feature using double-click or right-click to change text
     */
    public class LabeledTab extends Tab
    {
        private Label mLabel = new Label();
        private TextField mTextField = new TextField();

        /**
         * Constructs an instance
         * @param label to use initially
         * @param node for content
         */
        public LabeledTab(String label, Node node)
        {
            super(null, node);

            mLabel.setText(label);
            setGraphic(mLabel);

            mLabel.setOnMouseClicked(event -> {
                if((event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() > 1) ||
                        event.getButton().equals(MouseButton.SECONDARY))
                {
                    setGraphic(mTextField);
                    mTextField.setText(mLabel.getText());
                    mTextField.selectAll();
                    mTextField.requestFocus();
                }
            });

            mTextField.setOnAction(event -> {
                mLabel.setText(mTextField.getText());
                LabeledTab.this.setGraphic(mLabel);
            });

            mTextField.focusedProperty().addListener((observable, oldValue, newValue) -> {
                if(!newValue)
                {
                    mLabel.setText(mTextField.getText());
                    LabeledTab.this.setGraphic(mLabel);
                }
            });
        }
    }

    public static void main(String[] args)
    {
        Application viewer = new Application()
        {
            @Override
            public void start(Stage primaryStage) throws Exception
            {
                Scene scene = new Scene(new MessageRecordingViewer(), 1100, 800);
                primaryStage.setTitle("Message Recording Viewer (.bits)");
                primaryStage.setScene(scene);
                primaryStage.show();
            }
        };

        Runnable r = () -> {
            try
            {
                viewer.start(new Stage());
            }
            catch(Exception e)
            {
                mLog.error("Error starting message recording viewer application", e);
            }
        };

        Platform.startup(r);
    }
}
