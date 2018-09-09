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
package io.github.dsheirer.gui.instrument;

import io.github.dsheirer.gui.instrument.decoder.DecoderPaneFactory;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.p25.P25Decoder;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.prefs.Preferences;

/**
 * Application for viewing and working with demodulators and recording files.
 */
public class DemodulatorViewerFX extends Application
{
    private final static Logger mLog = LoggerFactory.getLogger(DemodulatorViewerFX.class);

    private static final String PREFERENCE_FILE_OPEN_DEFAULT_FOLDER = "default.folder";

    private Preferences mPreferences = Preferences.userNodeForPackage(DemodulatorViewerFX.class);
    private Stage mStage;
    private MenuBar mMenuBar;
    private RecentFilesMenu mRecentFilesMenu;
    private ViewerDesktop mViewerDesktop;

    @Override
    public void start(Stage primaryStage) throws Exception
    {
        mStage = primaryStage;
        setTitle();

        BorderPane borderPane = new BorderPane();
        borderPane.setTop(getMenuBar());
        borderPane.setCenter(getViewerDesktop());

        Scene scene = new Scene(borderPane, 1500, 900);

        mStage.setScene(scene);
        mStage.show();
    }

    private void setTitle()
    {
        mStage.setTitle("Demodulator Viewer FX");
    }

    private void setTitle(DecoderType decoderType)
    {
        if(decoderType != null)
        {
            mStage.setTitle("Demodulator Viewer FX - " + decoderType.getDisplayString());
        }
        else
        {
            setTitle();
        }
    }

    private void setTitle(P25Decoder.Modulation modulation)
    {
        if(modulation != null)
        {
            mStage.setTitle("Demodulator Viewer FX - P25 " + modulation.getLabel());
        }
        else
        {
            setTitle();
        }
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

            Menu fileMenu = new Menu("File");
            MenuItem openMenuItem = new MenuItem("Open ...");
            openMenuItem.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    FileChooser fileChooser = new FileChooser();
                    fileChooser.setTitle("Open an I/Q recording file");

                    String directory = mPreferences.get(PREFERENCE_FILE_OPEN_DEFAULT_FOLDER, null);

                    if(directory != null)
                    {
                        fileChooser.setInitialDirectory(new File(directory));
                    }

                    File file = fileChooser.showOpenDialog(mStage);

                    if(file != null)
                    {
                        getViewerDesktop().load(file);
                        getRecentFilesMenu().add(file);
                        mPreferences.put(PREFERENCE_FILE_OPEN_DEFAULT_FOLDER, file.getParent());
                    }
                }
            });

            MenuItem closeMenuItem = new MenuItem("Close");
            closeMenuItem.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    getViewerDesktop().close();
                }
            });

            MenuItem exitMenuItem = new MenuItem("Exit");
            exitMenuItem.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    Platform.exit();
                }
            });

            fileMenu.getItems().addAll(openMenuItem, getRecentFilesMenu(), closeMenuItem, new SeparatorMenuItem(),
                    exitMenuItem);

            mMenuBar.getMenus().add(fileMenu);

            Menu decoderMenu = new Menu("Decoder");

            for(DecoderType decoderType: DecoderType.values())
            {
                if(DecoderPaneFactory.isSupported(decoderType))
                {
                    if(decoderType == DecoderType.P25_PHASE1)
                    {
                        MenuItem c4fmDecoderMenuItem = new MenuItem("P25 Phase 1 C4FM");

                        c4fmDecoderMenuItem.setOnAction(new EventHandler<ActionEvent>()
                        {
                            @Override
                            public void handle(ActionEvent event)
                            {
                                getViewerDesktop().setP25Phase1Decoder(P25Decoder.Modulation.C4FM);
                                setTitle(decoderType);
                            }
                        });

                        decoderMenu.getItems().add(c4fmDecoderMenuItem);

                        MenuItem lsmDecoderMenuItem = new MenuItem("P25 Phase 1 LSM");

                        lsmDecoderMenuItem.setOnAction(new EventHandler<ActionEvent>()
                        {
                            @Override
                            public void handle(ActionEvent event)
                            {
                                getViewerDesktop().setP25Phase1Decoder(P25Decoder.Modulation.CQPSK);
                                setTitle(decoderType);
                            }
                        });

                        decoderMenu.getItems().add(lsmDecoderMenuItem);
                    }
                    else
                    {
                        MenuItem decoderMenuItem = new MenuItem(decoderType.getShortDisplayString());

                        decoderMenuItem.setOnAction(new EventHandler<ActionEvent>()
                        {
                            @Override
                            public void handle(ActionEvent event)
                            {
                                getViewerDesktop().setDecoder(decoderType);
                                setTitle(decoderType);
                            }
                        });

                        decoderMenu.getItems().add(decoderMenuItem);
                    }
                }
            }

            mMenuBar.getMenus().add(decoderMenu);
        }

        return mMenuBar;
    }

    private RecentFilesMenu getRecentFilesMenu()
    {
        if(mRecentFilesMenu == null)
        {
            mRecentFilesMenu = new RecentFilesMenu("Recent","demodulator.viewer", 5);
            mRecentFilesMenu.setFileSelectionListener(new RecentFilesMenu.IFileSelectionListener()
            {
                @Override
                public void fileSelected(File file)
                {
                    getViewerDesktop().load(file);
                }
            });
        }

        return mRecentFilesMenu;
    }

    public static void main(String[] args)
    {
        launch(args);
    }
}
