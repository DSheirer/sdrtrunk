/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

package io.github.dsheirer.gui;

import io.github.dsheirer.settings.Setting;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.audio.playback.AudioPlaybackManager;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.gui.audio.AudioView;
import io.github.dsheirer.gui.broadcast.BroadcastStatusView;
import io.github.dsheirer.gui.icon.ViewIconManagerRequest;
import io.github.dsheirer.gui.map.MapView;
import io.github.dsheirer.gui.nowplaying.NowPlayingView;
import io.github.dsheirer.gui.playlist.ViewPlaylistRequest;
import io.github.dsheirer.gui.playlist.radioreference.RadioReferencePlaylistActions;
import io.github.dsheirer.gui.settings.ViewSettingsEditorRequest;
import io.github.dsheirer.gui.spectrum.MainSpectralDisplay;
import io.github.dsheirer.gui.theme.ThemeManager;
import io.github.dsheirer.gui.viewer.ViewRecordingViewerRequest;
import io.github.dsheirer.icon.IconModel;
import io.github.dsheirer.map.MapService;
import io.github.dsheirer.monitor.DiagnosticMonitor;
import io.github.dsheirer.monitor.ResourceMonitor;
import io.github.dsheirer.monitor.StatusBox;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.settings.Settings;
import io.github.dsheirer.properties.SystemProperties;
import io.github.dsheirer.source.tuner.Tuner;
import io.github.dsheirer.source.tuner.TunerEvent;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.util.ThreadPool;
import io.github.dsheirer.util.TimeStamp;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javax.imageio.ImageIO;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.javafx.IconFontFX;
import jiconfont.javafx.IconNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main application window: menu bar, optional toolbar, audio playback display, the spectrum and waterfall, the Now Playing, Map and
 * Tuners tabs, the optional streaming status and the optional resource status bar.
 *
 * The spectrum and waterfall can be hidden from the View menu, the hide button in the display settings panel or a
 * keyboard shortcut.  A hidden spectrum is disposed so that it uses no processing or memory.
 */
public class MainWindow
{
    private static final Logger mLog = LoggerFactory.getLogger(MainWindow.class);
    private static final double DEFAULT_SPECTRAL_DIVIDER = 0.38;
    private static final double DEFAULT_BROADCAST_FRACTION = 0.18;
    static final KeyCombination SPECTRAL_TOGGLE_SHORTCUT = new KeyCodeCombination(KeyCode.S,
        KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN);
    private static final KeyCombination SCREEN_CAPTURE_SHORTCUT = new KeyCodeCombination(KeyCode.C,
        KeyCombination.ALT_DOWN);
    private static final KeyCombination COMPACT_VIEW_SHORTCUT = new KeyCodeCombination(KeyCode.C,
        KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN);
    private static final KeyCombination SETTINGS_SHORTCUT = new KeyCodeCombination(KeyCode.COMMA,
        KeyCombination.SHORTCUT_DOWN);
    private static final String WEBSITE_URL = "https://github.com/DSheirer/sdrtrunk";
    private static final String USER_GUIDE_URL = WEBSITE_URL + "/wiki";
    private static final String ISSUES_URL = WEBSITE_URL + "/issues";

    private final Settings mSettings;
    private final PlaylistManager mPlaylistManager;
    private final TunerManager mTunerManager;
    private final DiagnosticMonitor mDiagnosticMonitor;
    private final ResourceMonitor mResourceMonitor;
    private final Runnable mExitHandler;
    private final String mTitle;

    private final Stage mStage;
    private final BorderPane mRoot = new BorderPane();
    private final SplitPane mSplitPane = new SplitPane();
    private final VBox mCenter = new VBox();
    private final AudioView mAudioView;
    private final MainSpectralDisplay mSpectralDisplay;
    private final HBox mSpectralHiddenBar;
    private final NowPlayingView mNowPlayingView;
    private final TabPane mTabPane = new TabPane();
    private BroadcastStatusView mBroadcastStatusView;
    private StatusBox mStatusBox;
    private final CheckMenuItem mToolbarMenuItem = new CheckMenuItem("Toolbar");
    private final CheckMenuItem mSpectralMenuItem = new CheckMenuItem("Spectrum & Waterfall");
    private final CheckMenuItem mBroadcastMenuItem = new CheckMenuItem("Streaming Status");
    private final CheckMenuItem mResourceMenuItem = new CheckMenuItem("Resource Status Bar");
    private final CheckMenuItem mNowPlayingDetailsMenuItem = new CheckMenuItem("Now Playing Channel Details");
    private final CheckMenuItem mAutoHideMenuItem = new CheckMenuItem("Auto-hide Menu Bar");
    private final CheckMenuItem mCompactMenuItem = new CheckMenuItem("Compact View");
    private final VBox mTop = new VBox();
    private ToolBar mToolBar;
    private MenuBar mMenuBar;
    private MenuBarAutoHide mMenuBarAutoHide;
    private final List<ButtonBase> mToolbarButtons = new ArrayList<>();
    private double mSpectralDivider;
    private double mBroadcastFraction;
    private boolean mUpdatingDividers;

    static
    {
        IconFontFX.register(FontAwesome.getIconFont());
    }

    /**
     * Constructs the main window.  JavaFX thread only.
     * @param settings for the views
     * @param playlistManager for the channels, aliases and streams
     * @param tunerManager for the tuners
     * @param audioPlaybackManager for the audio display
     * @param aliasModel for alias lookup
     * @param iconModel for alias icons
     * @param mapService for the map
     * @param resourceMonitor for the status bar
     * @param diagnosticMonitor for the reports
     * @param exitHandler that shuts down the application, invoked on a background thread
     */
    public MainWindow(Settings settings, PlaylistManager playlistManager, TunerManager tunerManager,
                      AudioPlaybackManager audioPlaybackManager, AliasModel aliasModel, IconModel iconModel,
                      MapService mapService, ResourceMonitor resourceMonitor, DiagnosticMonitor diagnosticMonitor,
                      Runnable exitHandler)
    {
        mSettings = settings;
        mPlaylistManager = playlistManager;
        mTunerManager = tunerManager;
        mResourceMonitor = resourceMonitor;
        mDiagnosticMonitor = diagnosticMonitor;
        mExitHandler = exitHandler;
        mTitle = SystemProperties.getInstance().getApplicationName();
        mSpectralDivider = clamp(Setting.MAIN_WINDOW_SPECTRAL_DIVIDER.getDouble(DEFAULT_SPECTRAL_DIVIDER));
        mBroadcastFraction = clamp(Setting.MAIN_WINDOW_BROADCAST_DIVIDER.getDouble(DEFAULT_BROADCAST_FRACTION));

        mAudioView = new AudioView(audioPlaybackManager, aliasModel, iconModel, settings);

        boolean spectralVisible = Setting.MAIN_WINDOW_SPECTRAL_DISPLAY_VISIBLE.getBoolean(true);
        mSpectralDisplay = new MainSpectralDisplay(playlistManager, tunerManager.getDiscoveredTunerModel(),
            spectralVisible);
        mSpectralDisplay.setHideRequestHandler(() -> setSpectralDisplayVisible(false));
        mSpectralHiddenBar = createSpectralHiddenBar();

        boolean detailsVisible = Setting.MAIN_WINDOW_NOW_PLAYING_DETAILS_VISIBLE.getBoolean(true);
        mNowPlayingView = new NowPlayingView(playlistManager, iconModel, settings, detailsVisible);
        createTabs(mapService, iconModel);

        URL stylesheet = MainWindow.class.getResource("/main_window.css");

        if(stylesheet != null)
        {
            mRoot.getStylesheets().add(stylesheet.toExternalForm());
        }

        mRoot.getStyleClass().add("main-window");
        mSplitPane.setOrientation(javafx.geometry.Orientation.VERTICAL);
        mSplitPane.getStyleClass().add("main-split-pane");
        //The split pane moves its dividers while it lays out, so only store the positions after the user drags one
        mSplitPane.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_RELEASED, event -> storeDividers());
        mSpectralDisplay.setMinHeight(80);
        mTabPane.setMinHeight(150);
        VBox.setVgrow(mSplitPane, Priority.ALWAYS);
        mCenter.getChildren().addAll(mSpectralHiddenBar, mSplitPane);
        mMenuBar = createMenuBar();
        mToolBar = createToolBar();
        mTop.getChildren().addAll(mMenuBar, mToolBar, mAudioView);
        mRoot.setTop(mTop);
        mRoot.setCenter(mCenter);

        mSpectralMenuItem.setSelected(spectralVisible);
        mNowPlayingDetailsMenuItem.setSelected(detailsVisible);
        mBroadcastMenuItem.setSelected(Setting.MAIN_WINDOW_BROADCAST_STATUS_VISIBLE.getBoolean(false));
        mResourceMenuItem.setSelected(Setting.MAIN_WINDOW_RESOURCE_STATUS_VISIBLE.getBoolean(true));
        mToolbarMenuItem.setSelected(Setting.MAIN_WINDOW_TOOLBAR_VISIBLE.getBoolean(true));
        mCompactMenuItem.setSelected(Setting.MAIN_WINDOW_COMPACT_VIEW.getBoolean(false));
        installViewToggles();
        updateToolBar();
        updateLayout();

        mStage = new Stage();
        mStage.setTitle(mTitle);
        ApplicationIcons.apply(mStage);
        Scene scene = new Scene(mRoot, 1280, 800);
        ThemeManager.getInstance().register(scene);
        scene.getAccelerators().put(SPECTRAL_TOGGLE_SHORTCUT, () -> {
            if(!isCompact())
            {
                setSpectralDisplayVisible(!mSpectralMenuItem.isSelected());
            }
        });

        if(isAutoHideSupported())
        {
            mMenuBarAutoHide = new MenuBarAutoHide(mMenuBar, scene);
            mAutoHideMenuItem.setSelected(Setting.MAIN_WINDOW_MENU_BAR_AUTO_HIDE.getBoolean(false));
            mMenuBarAutoHide.enabledProperty().bind(mAutoHideMenuItem.selectedProperty());
            mAutoHideMenuItem.selectedProperty().addListener((observable, oldValue, selected) ->
                Setting.MAIN_WINDOW_MENU_BAR_AUTO_HIDE.putBoolean(selected));
        }
        mStage.setScene(scene);
        mStage.setMinWidth(640);
        mStage.setMinHeight(480);
        mStage.setOnCloseRequest(event -> {
            event.consume();
            exit();
        });

        migrateSwingWindowLocation();
        mSettings.getJavaFxSettings().monitor(mStage, Setting.Window.MAIN_WINDOW);

        //The compact window fits its content, which changes when the menu bar or toolbar shows or hides
        mMenuBar.managedProperty().addListener((observable, oldValue, managed) -> fitCompactWindow());
        mToolBar.managedProperty().addListener((observable, oldValue, managed) -> fitCompactWindow());
    }

    /**
     * Main window stage.
     */
    public Stage getStage()
    {
        return mStage;
    }

    /**
     * Spectrum and waterfall display.
     */
    public MainSpectralDisplay getSpectralDisplay()
    {
        return mSpectralDisplay;
    }

    /**
     * Shows the window.  JavaFX thread only.
     */
    public void show()
    {
        mStage.show();

        if(isCompact())
        {
            //The compact size needs the window decoration size, which is only known once the window is showing
            applyCompactWindow();
        }
        else
        {
            //The split pane only applies divider positions once it has a size
            Platform.runLater(this::applyDividers);
        }
    }

    /**
     * Shows the stage's title with the tuner name, or the application name when the tuner is null.  Any thread.
     */
    public void updateTitle(String tunerName)
    {
        String title = tunerName != null ? mTitle + " - " + tunerName : mTitle;
        Platform.runLater(() -> mStage.setTitle(title));
    }

    /**
     * Handles tuner events for the main spectral display.  Any thread.
     */
    public void receive(TunerEvent event)
    {
        switch(event.getEvent())
        {
            case REQUEST_MAIN_SPECTRAL_DISPLAY:
                //The user asked to see a tuner, so show the display if it's hidden
                Platform.runLater(() -> setSpectralDisplayVisible(true));
                updateTitle(event.getTuner().getPreferredName());
                break;
            case REQUEST_CLEAR_MAIN_SPECTRAL_DISPLAY:
                updateTitle(null);
                break;
            case NOTIFICATION_SHUTTING_DOWN:
                Tuner currentTuner = mSpectralDisplay.getTuner();

                if(currentTuner == null || (event.hasTuner() && event.getTuner().equals(currentTuner)))
                {
                    updateTitle(null);
                }
                break;
            default:
                break;
        }
    }

    /**
     * Saves the divider positions and stops the views.  Called during application shutdown.
     */
    public void dispose()
    {
        mAudioView.dispose();

        if(mBroadcastStatusView != null)
        {
            mBroadcastStatusView.dispose();
        }

        mSpectralDisplay.dispose();
    }

    /**
     * Shows or hides the spectrum and waterfall.  A hidden display is disposed.  JavaFX thread only.
     */
    public void setSpectralDisplayVisible(boolean visible)
    {
        Setting.MAIN_WINDOW_SPECTRAL_DISPLAY_VISIBLE.putBoolean(visible);

        //The menu item and toolbar button listen to this, so set it before the display changes
        mSpectralMenuItem.setSelected(visible);
        updateSpectralDisplay();
    }

    /**
     * Creates or disposes the spectral display to match the View menu choice.  Compact view always disposes it,
     * since it isn't shown.
     */
    private void updateSpectralDisplay()
    {
        boolean visible = mSpectralMenuItem.isSelected() && !isCompact();

        if(visible != mSpectralDisplay.displayVisibleProperty().get())
        {
            mSpectralDisplay.setDisplayVisible(visible);

            if(!isCompact())
            {
                updateSplitPaneItems();
                applyDividers();
            }
        }
    }

    /**
     * Indicates if the window shows the compact view: the toolbar and audio display only.
     */
    public boolean isCompact()
    {
        return mCompactMenuItem.isSelected();
    }

    /**
     * Shows the normal or compact view.  The compact view shows the menu bar, toolbar and audio display only and
     * disposes the spectral display.  The window remembers a separate size and location for each view.
     */
    private void updateLayout()
    {
        boolean compact = isCompact();
        updateSpectralDisplay();

        if(compact)
        {
            mRoot.setCenter(null);
        }
        else
        {
            mRoot.setCenter(mCenter);
            updateSplitPaneItems();
        }

        updateStatusBar();
    }

    /**
     * Switches the window between the normal and compact sizes, each stored under its own window key.
     */
    private void applyWindowForView()
    {
        if(isCompact())
        {
            applyCompactWindow();
        }
        else
        {
            mSettings.getJavaFxSettings().unmonitor(mStage);
            mStage.setMinHeight(480);
            mStage.setMaxHeight(Double.MAX_VALUE);
            mSettings.getJavaFxSettings().monitor(mStage, Setting.Window.MAIN_WINDOW);
            //The split pane only applies divider positions once it has its new size
            Platform.runLater(this::applyDividers);
        }
    }

    /**
     * Switches the stage to the compact window and fits its height.  The stored compact location applies when one
     * exists, otherwise the window keeps its location and width.
     */
    private void applyCompactWindow()
    {
        if(mStage.isShowing() && isCompact())
        {
            mSettings.getJavaFxSettings().unmonitor(mStage);
            mStage.setMaximized(false);
            mSettings.getJavaFxSettings().monitor(mStage, Setting.Window.MAIN_WINDOW_COMPACT);
            fitCompactWindow();
        }
    }

    /**
     * Fits the compact window's height to its content and prevents vertical resizing.  Width stays adjustable.
     */
    private void fitCompactWindow()
    {
        if(!isCompact() || !mStage.isShowing())
        {
            return;
        }

        //Wait for the layout pass so that the content and decoration heights are current
        Platform.runLater(() -> {
            if(!isCompact())
            {
                return;
            }

            mRoot.applyCss();
            mRoot.layout();
            double decoration = Math.max(0, mStage.getHeight() - mStage.getScene().getHeight());
            double height = Math.ceil(mTop.prefHeight(mStage.getScene().getWidth()) + decoration);
            mStage.setMinHeight(0);
            mStage.setMaxHeight(Double.MAX_VALUE);
            mStage.setHeight(height);
            mStage.setMinHeight(height);
            mStage.setMaxHeight(height);
        });
    }

    private HBox createSpectralHiddenBar()
    {
        IconNode icon = new IconNode(FontAwesome.EYE_SLASH);
        icon.setIconSize(13);
        icon.getStyleClass().add("main-hidden-bar-icon");
        Label label = new Label("Spectrum and waterfall are hidden and turned off, so they use no processing or memory.");
        label.getStyleClass().add("main-hidden-bar-label");
        label.setMinWidth(0);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        IconNode showIcon = new IconNode(FontAwesome.EYE);
        showIcon.setIconSize(12);
        Button show = new Button("Show Spectrum & Waterfall", showIcon);
        showIcon.fillProperty().bind(show.textFillProperty());
        show.setOnAction(event -> setSpectralDisplayVisible(true));
        HBox bar = new HBox(8, icon, label, spacer, show);
        bar.getStyleClass().add("main-hidden-bar");
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private void createTabs(MapService mapService, IconModel iconModel)
    {
        MapView mapView = new MapView(mapService, iconModel, mSettings,
            mPlaylistManager.getRadioReference() != null ?
                mPlaylistManager.getRadioReference().getFrequencyIndex() : null,
            new RadioReferencePlaylistActions(mPlaylistManager), true);

        Tab nowPlaying = new Tab("Now Playing", mNowPlayingView);
        Tab map = new Tab("Map", mapView);
        Tab tuners = new Tab("Tuners", new io.github.dsheirer.source.tuner.ui.TunerView(mTunerManager,
            mSettings));

        mTabPane.getTabs().addAll(nowPlaying, map, tuners);
        mTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        mTabPane.getStyleClass().add("main-tab-pane");
    }

    /**
     * Updates the split pane to hold the visible panes: spectrum (optional), tabs and streaming status (optional).
     */
    private void updateSplitPaneItems()
    {
        mUpdatingDividers = true;

        List<Node> items = new ArrayList<>();

        boolean spectralVisible = mSpectralDisplay.displayVisibleProperty().get();

        if(spectralVisible)
        {
            items.add(mSpectralDisplay);
        }

        items.add(mTabPane);

        if(mBroadcastMenuItem.isSelected())
        {
            if(mBroadcastStatusView == null)
            {
                mBroadcastStatusView = new BroadcastStatusView(mPlaylistManager.getBroadcastModel(),
                    mSettings.getSwingSettings());
                mBroadcastStatusView.setMinHeight(70);
            }

            items.add(mBroadcastStatusView);
        }

        mSplitPane.getItems().setAll(items);
        SplitPane.setResizableWithParent(mTabPane, true);

        if(mBroadcastStatusView != null)
        {
            SplitPane.setResizableWithParent(mBroadcastStatusView, false);
        }

        mSpectralHiddenBar.setVisible(!spectralVisible);
        mSpectralHiddenBar.setManaged(!spectralVisible);

        mUpdatingDividers = false;
    }

    /**
     * Applies the stored divider positions to the split pane.
     */
    private void applyDividers()
    {
        mUpdatingDividers = true;
        List<SplitPane.Divider> dividers = mSplitPane.getDividers();
        int index = 0;

        if(mSpectralDisplay.displayVisibleProperty().get() && index < dividers.size())
        {
            dividers.get(index++).setPosition(mSpectralDivider);
        }

        if(mBroadcastMenuItem.isSelected() && index < dividers.size())
        {
            dividers.get(index).setPosition(1.0 - mBroadcastFraction);
        }

        mUpdatingDividers = false;
    }

    /**
     * Stores the divider positions after the user moves a divider.
     */
    private void storeDividers()
    {
        if(mUpdatingDividers || !mStage.isShowing() || mSplitPane.getDividers().isEmpty())
        {
            return;
        }

        List<SplitPane.Divider> dividers = mSplitPane.getDividers();
        int index = 0;

        if(mSpectralDisplay.displayVisibleProperty().get() && index < dividers.size())
        {
            mSpectralDivider = clamp(dividers.get(index++).getPosition());
            Setting.MAIN_WINDOW_SPECTRAL_DIVIDER.putDouble(mSpectralDivider);
        }

        if(mBroadcastMenuItem.isSelected() && index < dividers.size())
        {
            mBroadcastFraction = clamp(1.0 - dividers.get(index).getPosition());
            Setting.MAIN_WINDOW_BROADCAST_DIVIDER.putDouble(mBroadcastFraction);
        }
    }

    private static double clamp(double value)
    {
        return Double.isNaN(value) ? 0.3 : Math.max(0.05, Math.min(0.6, value));
    }

    private void updateStatusBar()
    {
        if(mResourceMenuItem.isSelected() && !isCompact())
        {
            if(mStatusBox == null)
            {
                mStatusBox = new StatusBox(mResourceMonitor);
            }

            mRoot.setBottom(mStatusBox);
        }
        else
        {
            mRoot.setBottom(null);
        }
    }

    private MenuBar createMenuBar()
    {
        //File
        MenuItem screenCapture = new MenuItem("Screen Capture", icon(FontAwesome.CAMERA));
        screenCapture.setAccelerator(SCREEN_CAPTURE_SHORTCUT);
        screenCapture.setOnAction(event -> captureScreen());

        Menu folders = new Menu("Open Folder", icon(FontAwesome.FOLDER_OPEN_O),
            folderItem("Application Logs", () -> mSettings.getDirectorySettings().getDirectoryApplicationLog()),
            folderItem("Audio Recordings", () -> mSettings.getDirectorySettings().getDirectoryRecording()),
            folderItem("Channel Event Logs", () -> mSettings.getDirectorySettings().getDirectoryEventLog()),
            folderItem("Screen Captures", () -> mSettings.getDirectorySettings().getDirectoryScreenCapture()));

        MenuItem settings = new MenuItem("Settings...", icon(FontAwesome.COG));
        settings.setAccelerator(SETTINGS_SHORTCUT);
        settings.setOnAction(event -> showSettings());

        MenuItem exit = new MenuItem("Exit", icon(FontAwesome.SIGN_OUT));
        exit.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.SHORTCUT_DOWN));
        exit.setOnAction(event -> exit());

        Menu file = new Menu("File", null, screenCapture, folders, new SeparatorMenuItem(), settings,
            new SeparatorMenuItem(), exit);

        //View: only what this window shows
        mSpectralMenuItem.setAccelerator(SPECTRAL_TOGGLE_SHORTCUT);
        Menu spectrumTuner = new Menu("Spectrum Tuner");
        spectrumTuner.getItems().add(new MenuItem("(none)"));
        spectrumTuner.setOnShowing(event -> updateSpectrumTunerMenu(spectrumTuner));

        mCompactMenuItem.setAccelerator(COMPACT_VIEW_SHORTCUT);
        Menu view = new Menu("View", null, mCompactMenuItem, new SeparatorMenuItem(), mToolbarMenuItem,
            mSpectralMenuItem, mNowPlayingDetailsMenuItem, mBroadcastMenuItem, mResourceMenuItem,
            new SeparatorMenuItem(), spectrumTuner);

        if(isAutoHideSupported())
        {
            view.getItems().add(3, mAutoHideMenuItem);
        }

        //These only apply to the normal view
        for(MenuItem item: List.of(mSpectralMenuItem, mNowPlayingDetailsMenuItem, mBroadcastMenuItem,
            mResourceMenuItem, spectrumTuner))
        {
            item.disableProperty().bind(mCompactMenuItem.selectedProperty());
        }

        //Tools: the other windows
        MenuItem playlistEditor = new MenuItem("Playlist Editor", icon(FontAwesome.PLAY_CIRCLE_O));
        playlistEditor.setOnAction(event -> showPlaylistEditor());
        MenuItem iconManager = new MenuItem("Icon Manager", icon(FontAwesome.PICTURE_O));
        iconManager.setOnAction(event -> MyEventBus.getGlobalEventBus().post(new ViewIconManagerRequest()));
        MenuItem recordingViewer = new MenuItem("Message Recording Viewer (.bits)", icon(FontAwesome.BRAILLE));
        recordingViewer.setOnAction(event -> MyEventBus.getGlobalEventBus().post(new ViewRecordingViewerRequest()));

        MenuItem processingReport = new MenuItem("Processing Diagnostic Report");
        processingReport.setOnAction(event -> createReport("Processing Status Report",
            () -> mDiagnosticMonitor.generateProcessingDiagnosticReport("User initiated diagnostic report")));
        MenuItem threadDumpReport = new MenuItem("Thread Dump Report");
        threadDumpReport.setOnAction(event -> createReport("Thread Dump Report",
            mDiagnosticMonitor::generateThreadDumpReport));
        Menu reports = new Menu("Reports", icon(FontAwesome.LIST_ALT), processingReport, threadDumpReport);

        Menu tools = new Menu("Tools", null, playlistEditor, iconManager, recordingViewer, new SeparatorMenuItem(),
            reports);

        //Help
        MenuItem userGuide = new MenuItem("User Guide", icon(FontAwesome.BOOK));
        userGuide.setOnAction(event -> browse(USER_GUIDE_URL));
        MenuItem reportIssue = new MenuItem("Report an Issue", icon(FontAwesome.BUG));
        reportIssue.setOnAction(event -> browse(ISSUES_URL));
        MenuItem about = new MenuItem("About sdrtrunk", icon(FontAwesome.INFO_CIRCLE));
        about.setOnAction(event -> showAbout());
        Menu help = new Menu("Help", null, userGuide, reportIssue, new SeparatorMenuItem(), about);

        MenuBar menuBar = new MenuBar(file, view, tools, help);
        menuBar.setUseSystemMenuBar(true);
        return menuBar;
    }

    /**
     * Optional toolbar with the most used actions.  Hidden from the View menu or its own context menu.
     */
    private ToolBar createToolBar()
    {
        Button playlistEditor = toolbarButton(FontAwesome.PLAY_CIRCLE_O, "Playlist",
            "Create and edit channels, aliases and streaming", null);
        playlistEditor.getStyleClass().add("main-toolbar-playlist");
        playlistEditor.setOnAction(event -> showPlaylistEditor());

        Button settings = toolbarButton(FontAwesome.COG, "Settings", "Settings", SETTINGS_SHORTCUT);
        settings.setOnAction(event -> showSettings());

        ToggleButton spectrum = toolbarToggle(FontAwesome.AREA_CHART, "Spectrum", "Show spectrum & waterfall",
            SPECTRAL_TOGGLE_SHORTCUT, mSpectralMenuItem);
        ToggleButton streaming = toolbarToggle(FontAwesome.RSS, "Streaming", "Show streaming status", null,
            mBroadcastMenuItem);

        Button capture = toolbarButton(FontAwesome.CAMERA, "Capture", "Screen capture", SCREEN_CAPTURE_SHORTCUT);
        capture.setOnAction(event -> captureScreen());
        Button recordings = toolbarButton(FontAwesome.FOLDER_OPEN_O, "Recordings",
            "Open the audio recordings folder", null);
        recordings.setOnAction(event -> openFolder(() -> mSettings.getDirectorySettings().getDirectoryRecording()));

        ToggleButton compact = toolbarToggle(FontAwesome.COMPRESS, "Compact",
            "Compact view: toolbar and audio only", COMPACT_VIEW_SHORTCUT, mCompactMenuItem);
        //Show the action that a click takes, so the button isn't styled as selected
        compact.getStyleClass().add("main-toolbar-action");
        compact.selectedProperty().addListener((observable, oldValue, selected) -> updateCompactButton(compact));
        updateCompactButton(compact);
        spectrum.disableProperty().bind(mCompactMenuItem.selectedProperty());
        streaming.disableProperty().bind(mCompactMenuItem.selectedProperty());

        mToolbarButtons.addAll(List.of(playlistEditor, settings, spectrum, streaming, capture, recordings, compact));

        ToolBar toolBar = new ToolBar(playlistEditor, settings, new Separator(), spectrum, streaming,
            new Separator(), capture, recordings, new Separator(), compact);
        toolBar.getStyleClass().add("main-toolbar");

        CheckMenuItem labels = new CheckMenuItem("Show Labels");
        labels.setSelected(Setting.MAIN_WINDOW_TOOLBAR_LABELS.getBoolean(true));
        labels.selectedProperty().addListener((observable, oldValue, selected) -> {
            Setting.MAIN_WINDOW_TOOLBAR_LABELS.putBoolean(selected);
            updateToolbarLabels(selected);
        });
        updateToolbarLabels(labels.isSelected());
        MenuItem hide = new MenuItem("Hide Toolbar");
        hide.setOnAction(event -> mToolbarMenuItem.setSelected(false));
        ContextMenu contextMenu = new ContextMenu(labels, hide);

        if(isAutoHideSupported())
        {
            CheckMenuItem autoHide = new CheckMenuItem(mAutoHideMenuItem.getText());
            autoHide.selectedProperty().bindBidirectional(mAutoHideMenuItem.selectedProperty());
            contextMenu.getItems().add(1, autoHide);
        }

        toolBar.setContextMenu(contextMenu);
        return toolBar;
    }

    /**
     * Shows the compact view button as the action a click takes: Compact in the normal view, Expand in the
     * compact view.
     */
    private static void updateCompactButton(ToggleButton button)
    {
        boolean compact = button.isSelected();
        ((IconNode)button.getGraphic()).setIconCode(compact ? FontAwesome.EXPAND : FontAwesome.COMPRESS);
        button.setText(compact ? "Expand" : "Compact");
        String description = compact ? "Normal view: show the spectrum and tabs" :
            "Compact view: toolbar and audio only";
        button.setTooltip(new Tooltip(description + " (" + COMPACT_VIEW_SHORTCUT.getDisplayText() + ")"));
        button.setAccessibleText(description);
    }

    /**
     * Shows the toolbar buttons with labels, or as icons only.  The tooltips and accessible text stay the same.
     */
    private void updateToolbarLabels(boolean labels)
    {
        for(ButtonBase button: mToolbarButtons)
        {
            button.setContentDisplay(labels ? ContentDisplay.LEFT : ContentDisplay.GRAPHIC_ONLY);
        }
    }

    private static IconNode toolbarIcon(FontAwesome code)
    {
        IconNode icon = new IconNode(code);
        icon.setIconSize(15);
        icon.getStyleClass().add("main-toolbar-icon");
        return icon;
    }

    /**
     * Applies the label, the tooltip with the keyboard shortcut, if any, and the accessible text.
     */
    private static <T extends ButtonBase> T configure(T button, String label, String description,
                                                      KeyCombination shortcut)
    {
        button.setText(label);
        button.setTooltip(new Tooltip(shortcut != null ? description + " (" + shortcut.getDisplayText() + ")" :
            description));
        button.setAccessibleText(description);
        return button;
    }

    private static Button toolbarButton(FontAwesome code, String label, String description, KeyCombination shortcut)
    {
        return configure(new Button(null, toolbarIcon(code)), label, description, shortcut);
    }

    /**
     * Toggle button that shares its selected state with the menu item, so either one changes the other.
     */
    private static ToggleButton toolbarToggle(FontAwesome code, String label, String description,
                                              KeyCombination shortcut, CheckMenuItem menuItem)
    {
        ToggleButton button = configure(new ToggleButton(null, toolbarIcon(code)), label, description, shortcut);
        button.selectedProperty().bindBidirectional(menuItem.selectedProperty());
        return button;
    }

    /**
     * Acts on the View menu toggles.  Attached after the stored states are applied, so that startup doesn't
     * re-store them.  The toolbar toggle buttons share these selected properties.
     */
    private void installViewToggles()
    {
        mToolbarMenuItem.selectedProperty().addListener((observable, oldValue, selected) -> {
            Setting.MAIN_WINDOW_TOOLBAR_VISIBLE.putBoolean(selected);
            updateToolBar();
        });
        mSpectralMenuItem.selectedProperty().addListener((observable, oldValue, selected) ->
            setSpectralDisplayVisible(selected));
        mNowPlayingDetailsMenuItem.selectedProperty().addListener((observable, oldValue, selected) -> {
            mNowPlayingView.setDetailTabsVisible(selected);
            Setting.MAIN_WINDOW_NOW_PLAYING_DETAILS_VISIBLE.putBoolean(selected);
        });
        mBroadcastMenuItem.selectedProperty().addListener((observable, oldValue, selected) -> {
            Setting.MAIN_WINDOW_BROADCAST_STATUS_VISIBLE.putBoolean(selected);

            if(!isCompact())
            {
                updateSplitPaneItems();
                applyDividers();
            }
        });
        mResourceMenuItem.selectedProperty().addListener((observable, oldValue, selected) -> {
            Setting.MAIN_WINDOW_RESOURCE_STATUS_VISIBLE.putBoolean(selected);
            updateStatusBar();
        });
        mCompactMenuItem.selectedProperty().addListener((observable, oldValue, selected) -> {
            Setting.MAIN_WINDOW_COMPACT_VIEW.putBoolean(selected);
            updateLayout();
            applyWindowForView();
        });
    }

    /**
     * Indicates if the menu bar can auto-hide.  Not on macOS, where the menu bar is the system menu bar and isn't
     * part of the window.
     */
    private static boolean isAutoHideSupported()
    {
        return !System.getProperty("os.name", "").toLowerCase().startsWith("mac");
    }

    private void updateToolBar()
    {
        mToolBar.setVisible(mToolbarMenuItem.isSelected());
        mToolBar.setManaged(mToolbarMenuItem.isSelected());
    }

    private static void showPlaylistEditor()
    {
        MyEventBus.getGlobalEventBus().post(new ViewPlaylistRequest());
    }

    private static void showSettings()
    {
        MyEventBus.getGlobalEventBus().post(new ViewSettingsEditorRequest());
    }

    /**
     * Lists the available tuners as choices for the main spectral display, with the current tuner checked.
     */
    private void updateSpectrumTunerMenu(Menu menu)
    {
        menu.getItems().clear();
        ToggleGroup group = new ToggleGroup();
        Tuner current = mSpectralDisplay.displayVisibleProperty().get() ? mSpectralDisplay.getTuner() : null;

        for(DiscoveredTuner discoveredTuner: mTunerManager.getAvailableTuners())
        {
            Tuner tuner = discoveredTuner.getTuner();

            if(tuner != null)
            {
                RadioMenuItem item = new RadioMenuItem(tuner.getPreferredName());
                item.setToggleGroup(group);
                item.setSelected(tuner.equals(current));
                item.setOnAction(event -> ThreadPool.CACHED.submit(() -> {
                    MainSpectralDisplay.setTunerEnabled(true);
                    mTunerManager.getDiscoveredTunerModel().broadcast(new TunerEvent(tuner,
                        TunerEvent.Event.REQUEST_MAIN_SPECTRAL_DISPLAY));
                }));
                menu.getItems().add(item);
            }
        }

        if(menu.getItems().isEmpty())
        {
            MenuItem none = new MenuItem("No tuners available");
            none.setDisable(true);
            menu.getItems().add(none);
        }
    }

    private static IconNode icon(FontAwesome code)
    {
        IconNode icon = new IconNode(code);
        icon.setIconSize(12);
        icon.getStyleClass().add("main-menu-icon");
        return icon;
    }

    /**
     * Supplies a path that can throw an IO exception.
     */
    private interface PathSupplier
    {
        Path get() throws IOException;
    }

    private MenuItem folderItem(String title, PathSupplier folder)
    {
        MenuItem item = new MenuItem(title);
        item.setOnAction(event -> openFolder(folder));
        return item;
    }

    /**
     * Opens the folder in the desktop file explorer, or shows where it is when the explorer can't be launched.
     */
    private void openFolder(PathSupplier folder)
    {
        ThreadPool.CACHED.submit(() -> {
            File directory = null;

            try
            {
                directory = folder.get().toFile();
                Desktop.getDesktop().open(directory);
            }
            catch(Exception e)
            {
                mLog.error("Couldn't open file explorer", e);
                String location = directory != null ? directory.toString() : "unknown";
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Can't launch file explorer",
                    "Can't launch the file explorer.  The files are located at:\n\n" + location));
            }
        });
    }

    /**
     * Opens the web page in the desktop browser, or shows the address when the browser can't be launched.
     */
    private void browse(String url)
    {
        ThreadPool.CACHED.submit(() -> {
            try
            {
                Desktop.getDesktop().browse(URI.create(url));
            }
            catch(Exception e)
            {
                mLog.error("Couldn't open web browser for [" + url + "]", e);
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Can't launch web browser",
                    "Can't launch the web browser.  Please visit:\n\n" + url));
            }
        });
    }

    /**
     * Shows the version and environment details, which can be copied into a bug report.
     */
    private void showAbout()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(mTitle).append("\n\n");
        sb.append("Java: ").append(System.getProperty("java.version")).append(" (")
            .append(System.getProperty("java.vendor")).append(")\n");
        sb.append("JavaFX: ").append(System.getProperty("javafx.runtime.version", "unknown")).append("\n");
        sb.append("OS: ").append(System.getProperty("os.name")).append(" ").append(System.getProperty("os.version"))
            .append(" (").append(System.getProperty("os.arch")).append(")\n");
        sb.append("Processors: ").append(Runtime.getRuntime().availableProcessors()).append("  Max memory: ")
            .append(Runtime.getRuntime().maxMemory() / (1024 * 1024)).append(" MB\n");
        String jmbe = Setting.JMBE_LIBRARY_PATH.get(null);
        sb.append("JMBE library: ").append(jmbe != null && !jmbe.isBlank() ? jmbe : "not configured").append("\n");

        sb.append("Application folder: ").append(mSettings.getDirectorySettings().getDirectoryApplicationRoot())
            .append("\n");

        String details = sb.toString();
        TextArea text = new TextArea(details);
        text.setEditable(false);
        text.setWrapText(true);
        text.setPrefRowCount(9);
        text.setPrefColumnCount(60);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About sdrtrunk");
        alert.setHeaderText(mTitle);
        ApplicationIcons.getFxImages().stream().filter(image -> image.getWidth() >= 64).findFirst()
            .ifPresent(image -> {
                ImageView logo = new ImageView(image);
                logo.setFitWidth(64);
                logo.setPreserveRatio(true);
                alert.setGraphic(logo);
            });
        Hyperlink website = new Hyperlink(WEBSITE_URL);
        website.setOnAction(event -> browse(WEBSITE_URL));
        Label license = new Label("Copyright (C) 2014-2026 Dennis Sheirer.  Licensed under the GNU GPL v3.");
        alert.getDialogPane().setContent(new VBox(8, text, new HBox(4, new Label("Website:"), website), license));
        ButtonType copy = new ButtonType("Copy Details", ButtonBar.ButtonData.LEFT);
        alert.getButtonTypes().add(copy);
        alert.initOwner(mStage);
        alert.showAndWait().filter(copy::equals).ifPresent(type -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(details);
            Clipboard.getSystemClipboard().setContent(content);
        });
    }

    private void createReport(String title, PathSupplier report)
    {
        ThreadPool.CACHED.submit(() -> {
            try
            {
                Path path = report.get();
                Platform.runLater(() -> showAlert(Alert.AlertType.INFORMATION, title + " Created",
                    "Report created:\n\n" + path));
            }
            catch(IOException ioe)
            {
                mLog.error("Error creating " + title, ioe);
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, title + " Failed",
                    "Unable to create the report file.  Please see the application log for details."));
            }
        });
    }

    /**
     * Shows a message dialog owned by the main window.  JavaFX thread only.
     */
    public void showAlert(Alert.AlertType type, String title, String message)
    {
        Alert alert = new Alert(type, message);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.initOwner(mStage);
        alert.show();
    }

    /**
     * Warns that the library folder holds more than one version of some libraries.  JavaFX thread only.
     * @param details listing the duplicate libraries
     * @param folder holding the libraries
     */
    public void showDuplicateLibraries(String details, String folder)
    {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Duplicate Library Versions");
        alert.setHeaderText("The sdrtrunk library folder has more than one version of some libraries");
        alert.setContentText("This happens when a new version of sdrtrunk is unzipped over an older one.  sdrtrunk " +
            "can load the older library code, which can cause crashes or errors that the newer version fixed.\n\n" +
            "Close sdrtrunk, delete the sdrtrunk folder that contains this library folder and unzip the release " +
            "into a new, empty folder:\n\n" + folder + "\n\nYour playlist and settings are stored in your user " +
            "folder and won't be affected.");
        TextArea libraries = new TextArea(details);
        libraries.setEditable(false);
        libraries.setPrefRowCount(8);
        alert.getDialogPane().setExpandableContent(libraries);
        alert.getDialogPane().setExpanded(true);
        alert.initOwner(mStage);
        alert.show();
    }

    /**
     * Saves an image of the main window to the screen captures folder.
     */
    private void captureScreen()
    {
        WritableImage image = mStage.getScene().snapshot(null);
        String filename = TimeStamp.getTimeStamp("_") + "_screen_capture.png";

        ThreadPool.CACHED.submit(() -> {
            Path file = null;

            try
            {
                file = mSettings.getDirectorySettings().getDirectoryScreenCapture().resolve(filename);
                ImageIO.write(FxToolkit.toBufferedImage(image), "png", file.toFile());
            }
            catch(Exception e)
            {
                mLog.error("Couldn't write screen capture to file [" + file + "]", e);
            }
        });
    }


    /**
     * Hides the window and shuts down the application on a background thread, since stopping the tuners can block.
     */
    private void exit()
    {
        mStage.hide();
        Thread thread = new Thread(() -> {
            try
            {
                mExitHandler.run();
            }
            catch(Throwable t)
            {
                mLog.error("Error during shutdown", t);
            }

            System.exit(0);
        }, "sdrtrunk shutdown");
        thread.start();
    }

    /**
     * Moves the main window size and location from the Swing window preferences to the JavaFX stage preferences,
     * once, so that users keep their window placement.
     */
    private void migrateSwingWindowLocation()
    {
        if(mSettings.getJavaFxSettings().hasStoredLocation(Setting.Window.MAIN_WINDOW))
        {
            return;
        }

        String swingKey = "sdrtrunk.main.window.frame";
        java.awt.Point location = mSettings.getSwingSettings().getLocation(swingKey);
        java.awt.Dimension size = mSettings.getSwingSettings().getDimension(swingKey);

        if(location != null && size != null)
        {
            mSettings.getJavaFxSettings().storeLocation(Setting.Window.MAIN_WINDOW, location.x, location.y,
                size.width, size.height, mSettings.getSwingSettings().getMaximized(swingKey, false));
        }
        else
        {
            mStage.centerOnScreen();
        }
    }
}
