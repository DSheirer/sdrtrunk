/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.instrument.gui.viewer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

/**
 * Application for viewing and working with demodulators and recording files.
 */
public class DemodulatorViewerFX extends Application
{
    private Stage mStage;
    private MenuBar mMenuBar;
    private RecentFilesMenuItem mRecentFilesMenuItem;
    private ViewerDesktop mViewerDesktop;

    @Override
    public void start(Stage primaryStage) throws Exception
    {
        mStage = primaryStage;
        mStage.setTitle("Demodulator Viewer FX");

        BorderPane borderPane = new BorderPane();
        borderPane.setTop(getMenuBar());
        borderPane.setCenter(getViewerDesktop());

        Scene scene = new Scene(borderPane, 1200, 800);

        mStage.setScene(scene);
        mStage.show();
    }

    private ViewerDesktop getViewerDesktop()
    {
        if(mViewerDesktop == null)
        {
            mViewerDesktop = new ViewerDesktop();
        }

        return mViewerDesktop;
    }

    private MenuBar getMenuBar()
    {
        if(mMenuBar == null)
        {
            mMenuBar = new MenuBar();

            Menu menuFile = new Menu("File");
            MenuItem menuItemOpen = new MenuItem("Open ...");
            menuItemOpen.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    FileChooser fileChooser = new FileChooser();
                    fileChooser.setTitle("Open an I/Q recording file");
                    File file = fileChooser.showOpenDialog(mStage);
                    getViewerDesktop().load(file);
                    getRecentFilesMenuItem().add(file);
                }
            });

            MenuItem menuItemClose = new MenuItem("Close");
            menuItemClose.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    getViewerDesktop().close();
                }
            });

            MenuItem menuItemExit = new MenuItem("Exit");
            menuItemExit.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    Platform.exit();
                }
            });

            mRecentFilesMenuItem = new RecentFilesMenuItem("demodulator.viewer", 5);

            menuFile.getItems().addAll(menuItemOpen, getRecentFilesMenuItem(), menuItemClose, new SeparatorMenuItem(),
                    menuItemExit);

            mMenuBar.getMenus().addAll(menuFile);
        }

        return mMenuBar;
    }

    private RecentFilesMenuItem getRecentFilesMenuItem()
    {
        if(mRecentFilesMenuItem == null)
        {
            mRecentFilesMenuItem = new RecentFilesMenuItem("demodulator.viewer", 5);
            mRecentFilesMenuItem.setFileSelectionListener(new RecentFilesMenuItem.IFileSelectionListener()
            {
                @Override
                public void fileSelected(File file)
                {
                    getViewerDesktop().load(file);
                }
            });
        }

        return mRecentFilesMenuItem;
    }

    public static void main(String[] args)
    {
        launch(args);
    }
}
