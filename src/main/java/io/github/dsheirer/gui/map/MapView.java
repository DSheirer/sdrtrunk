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

package io.github.dsheirer.gui.map;

import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.gui.playlist.radioreference.RadioReferencePlaylistActions;
import io.github.dsheirer.icon.IconModel;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.map.GeoPosition;
import io.github.dsheirer.map.IPlottableUpdateListener;
import io.github.dsheirer.map.MapService;
import io.github.dsheirer.map.PlottableEntityHistory;
import io.github.dsheirer.map.PlottableEntityModel;
import io.github.dsheirer.map.TimestampedGeoPosition;
import io.github.dsheirer.gui.settings.SettingsPageType;
import io.github.dsheirer.gui.settings.ViewSettingsEditorRequest;
import io.github.dsheirer.settings.SettingsType;
import io.github.dsheirer.settings.Settings;
import io.github.dsheirer.settings.map.MapSettings;
import io.github.dsheirer.settings.radioreference.RadioReferenceSettings;
import io.github.dsheirer.service.radioreference.RadioReferenceFrequencyIndex;
import io.github.dsheirer.service.radioreference.SiteRecord;
import io.github.dsheirer.util.DesktopLauncher;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Map tab: plotted tracks of the radios that reported locations, the user's location and the radio reference trunked
 * system sites near the user, with the track lists and map controls beside the map.  Use on the JavaFX thread.
 */
public class MapView extends SplitPane
{
    private static final Duration SAVE_VIEW_DELAY = Duration.millis(500);
    private static final Duration HOVER_DELAY = Duration.millis(400);
    private static final double CONTROLS_WIDTH = 300;
    private static final double CARD_OFFSET = 16;
    private static final double CARD_MARGIN = 2;
    private static final int MAX_MENU_SITES = 3;
    //Shown when no location was ever stored: Syracuse, NY
    private static final GeoPosition DEFAULT_CENTER = new GeoPosition(43.048, -76.147);
    private static final String FOLLOW = "Follow";
    private static final String UNFOLLOW = "Unfollow";
    private static final String SELECT_A_TRACK = "(select a track)";
    private static final String NO_SYSTEM_NAME = "(no system name)";
    private static final String LOCATION_NOT_SET = "Not set";
    private static final Logger mLog = LoggerFactory.getLogger(MapView.class);

    private final MapService mMapService;
    private final PlottableEntityModel mEntityModel;
    private final Settings mSettings;
    private final RadioReferenceFrequencyIndex mFrequencyIndex;
    private final RadioReferencePlaylistActions mPlaylistActions;
    private final TileMapView mMap;
    private final TrackLayer mTrackLayer;
    private final SiteLayer mSiteLayer;
    private final UserLocationLayer mUserLocationLayer = new UserLocationLayer();
    private final Pane mCardPane = new Pane();
    private final PauseTransition mSaveViewTimer = new PauseTransition(SAVE_VIEW_DELAY);
    private final PauseTransition mHoverTimer = new PauseTransition(HOVER_DELAY);
    private final ObservableList<PlottableEntityHistory> mTracks = FXCollections.observableArrayList();
    private final ObservableList<TimestampedGeoPosition> mHistory = FXCollections.observableArrayList();

    private final Label mUserLocationLabel = new Label(LOCATION_NOT_SET);
    private final Button mSetUserLocationButton = new Button("Set Current");
    private final Button mCenterOnUserLocationButton = new Button("Center On");
    private final Spinner<Integer> mZoomSpinner = new Spinner<>();
    private final CheckBox mShowSitesCheckBox = new CheckBox("Radio Reference Sites");
    private final Label mSiteCountLabel = new Label();
    private final TableView<PlottableEntityHistory> mTrackTable = new TableView<>();
    private final Button mDeleteTrackButton = new Button("Delete");
    private final Button mDeleteAllTracksButton = new Button("Delete All");
    private final Button mClearMapButton = new Button("Clear Map");
    private final Button mReplotAllTracksButton = new Button("Replot All");
    private final Button mFollowButton = new Button(FOLLOW);
    private final Label mFollowedEntityLabel = new Label();
    private final ComboBox<Integer> mTrackHistoryLengthComboBox = new ComboBox<>();
    private final CheckBox mCenterOnSelectedCheckBox = new CheckBox("Center on Selection");
    private final Label mSelectedTrackSystemLabel = new Label(SELECT_A_TRACK);
    private final TableView<TimestampedGeoPosition> mHistoryTable = new TableView<>(mHistory);
    private final ComboBox<TileChoice> mTileServerComboBox = new ComboBox<>();
    private final TextField mTileServerKeyField = new TextField();
    private final Hyperlink mTileServerKeyLink = new Hyperlink("Get a free key");

    private PlottableEntityHistory mSelectedTrack;
    private PlottableEntityHistory mFollowedTrack;
    //Location most recently chosen on the map, which doesn't re-center the map when the preference update arrives
    private GeoPosition mMapChosenPosition;
    private ContextMenu mContextMenu;
    private Region mCard;
    private List<SiteRecord> mCardSites = Collections.emptyList();
    private double mHoverX;
    private double mHoverY;
    private boolean mRestoringHistorySelection;

    /**
     * Constructs an instance
     * @param mapService for the entities to plot
     * @param iconModel for alias icons
     * @param settings for the map view and the user's location, which is shared with the radio reference editor
     * @param frequencyIndex of the radio reference frequencies near the user, for the trunked system sites, or null
     * @param playlistActions to add sites to the playlist or enable their channels, or null
     * @param loadTiles true to download map tiles, or false for a blank map (tests and off-screen rendering)
     */
    public MapView(MapService mapService, IconModel iconModel, Settings settings,
                   RadioReferenceFrequencyIndex frequencyIndex, RadioReferencePlaylistActions playlistActions,
                   boolean loadTiles)
    {
        mMapService = mapService;
        mEntityModel = mapService.getPlottableEntityModel();
        mSettings = settings;
        mFrequencyIndex = frequencyIndex;
        mPlaylistActions = playlistActions;

        Path tileCache = settings.getDirectorySettings().getDirectoryApplicationRoot().resolve("map_tiles");
        mMap = new TileMapView(tileCache, loadTiles);
        mSiteLayer = new SiteLayer(frequencyIndex != null ? frequencyIndex::getSites : Collections::emptyList);
        mSiteLayer.setShowSites(settings.getMapSettings().isShowRadioReferenceSites());
        mTrackLayer = new TrackLayer(mEntityModel, iconModel);

        //Radio reference sites under the tracks, and the user's location pin over the tracks
        mMap.addLayer(mSiteLayer);
        mMap.addLayer(mTrackLayer);
        mMap.addLayer(mUserLocationLayer);

        initMap();
        initControls();

        mCardPane.setMouseTransparent(true);
        mCardPane.setPickOnBounds(false);
        StackPane mapPane = new StackPane(mMap, mCardPane);
        mapPane.setMinSize(0, 0);

        initTileServerControls();
        updateTileSource();

        TabPane controls = createControls();
        SplitPane.setResizableWithParent(controls, false);
        getItems().addAll(controls, mapPane);

        //Start the divider at the controls' width once the split pane has a width
        widthProperty().addListener((observable, oldValue, newValue) -> {
            if(oldValue.doubleValue() <= 0 && newValue.doubleValue() > 0)
            {
                setDividerPositions(Math.min(0.5, CONTROLS_WIDTH / newValue.doubleValue()));
            }
        });

        //Show the stored location without moving the map, which starts at the stored view
        RadioReferenceSettings.Location location = mSettings.getRadioReferenceSettings().getLocation();
        mUserLocationLayer.setPosition(location != null ? toGeoPosition(location) : null);
        updateUserLocationLabel(location);

        if(mFrequencyIndex != null)
        {
            //The index fills in the background
            mFrequencyIndex.addUpdateListener(() -> Platform.runLater(this::sitesUpdated));
        }

        sitesUpdated();

        //Listen before taking the existing tracks, so none is missed.  The FX thread drops duplicates.
        mMapService.addListener(new EntityListener());

        for(PlottableEntityHistory entity: mEntityModel.getAll())
        {
            entityAdded(entity);
        }

        MyEventBus.getGlobalEventBus().register(this);
    }

    /**
     * Map, for tests and off-screen rendering
     */
    public TileMapView getMap()
    {
        return mMap;
    }

    /**
     * Sets the starting view: the last view, else the user's location, else the default center
     */
    private void initMap()
    {
        MapSettings mapSettings = mSettings.getMapSettings();
        MapSettings.Center center = mapSettings.getCenter();
        RadioReferenceSettings.Location location = mSettings.getRadioReferenceSettings().getLocation();
        GeoPosition initial;

        if(center != null)
        {
            initial = new GeoPosition(center.latitude(), center.longitude());
        }
        else if(location != null)
        {
            initial = toGeoPosition(location);
        }
        else
        {
            initial = DEFAULT_CENTER;
        }

        mMap.setZoom(mapSettings.getTileZoom());
        mMap.setCenter(initial);

        //Remember the view after each pan or zoom, once the map stops moving
        mSaveViewTimer.setOnFinished(event -> saveView());
        mMap.addViewListener(() -> {
            mSaveViewTimer.playFromStart();
            hideCard();
        });

        mMap.setOnContextMenuRequested(this::showContextMenu);
        mMap.addEventHandler(MouseEvent.MOUSE_MOVED, this::mouseMoved);
        mMap.addEventHandler(MouseEvent.MOUSE_EXITED, event -> hideCard());
        mMap.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            hideCard();

            if(mContextMenu != null && !event.isPopupTrigger())
            {
                mContextMenu.hide();
            }
        });
        mMap.addEventHandler(ScrollEvent.SCROLL, event -> hideCard());
        mHoverTimer.setOnFinished(event -> showCard());
    }

    private void initControls()
    {
        mUserLocationLabel.setTooltip(new Tooltip());
        mSetUserLocationButton.setTooltip(new Tooltip("Sets your location, which is also used by the playlist " +
            "editor's Radio Reference tab, to the center of the map"));
        mSetUserLocationButton.setOnAction(event -> setUserLocation(mMap.getCenter()));
        mCenterOnUserLocationButton.setTooltip(new Tooltip("Centers the map on your location"));
        mCenterOnUserLocationButton.setDisable(true);
        mCenterOnUserLocationButton.setOnAction(event -> mMap.setCenter(mUserLocationLayer.getPosition()));

        SpinnerValueFactory.IntegerSpinnerValueFactory zoomFactory = new SpinnerValueFactory
            .IntegerSpinnerValueFactory(MapSettings.MINIMUM_TILE_ZOOM, MapSettings.MAXIMUM_TILE_ZOOM, mMap.getZoom());
        mZoomSpinner.setValueFactory(zoomFactory);
        mZoomSpinner.setPrefWidth(80);
        mZoomSpinner.setTooltip(new Tooltip("Map zoom level: higher is closer.  Scroll over the map to zoom."));
        zoomFactory.valueProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue != null)
            {
                mMap.setZoom(newValue);
            }
        });
        mMap.zoomProperty().addListener((observable, oldValue, newValue) -> zoomFactory.setValue(newValue.intValue()));

        mShowSitesCheckBox.setSelected(mSiteLayer.isShowSites());
        mShowSitesCheckBox.setDisable(mFrequencyIndex == null);
        mShowSitesCheckBox.setTooltip(new Tooltip("Show the trunked radio system sites within " +
            (int)RadioReferenceFrequencyIndex.RADIUS_KILOMETERS + " km of your location.  Requires a premium " +
            "Radio Reference login and your location."));
        mShowSitesCheckBox.setOnAction(event -> {
            boolean show = mShowSitesCheckBox.isSelected();
            mSiteLayer.setShowSites(show);
            mSettings.getMapSettings().setShowRadioReferenceSites(show);
            hideCard();
            mMap.requestRedraw();
        });

        initTrackTable();

        mDeleteTrackButton.setDisable(true);
        mDeleteTrackButton.setOnAction(event ->
            mEntityModel.delete(new ArrayList<>(mTrackTable.getSelectionModel().getSelectedItems())));
        mDeleteAllTracksButton.setOnAction(event -> {
            mEntityModel.deleteAllTracks();
            mTrackLayer.clear();
            follow(null);
            mMap.requestRedraw();
        });
        mClearMapButton.setTooltip(new Tooltip("Removes the tracks from the map.  A track returns when it reports " +
            "a new location, or with Replot All."));
        mClearMapButton.setOnAction(event -> {
            mTrackLayer.clear();
            mMap.requestRedraw();
        });
        mReplotAllTracksButton.setOnAction(event -> {
            if(mTrackLayer.addAll(mEntityModel.getAll()))
            {
                mMap.requestRedraw();
            }
        });
        mFollowButton.setDisable(true);
        mFollowButton.setTooltip(new Tooltip("Keeps the map centered on the selected track as it moves"));
        mFollowButton.setOnAction(event -> follow(mFollowedTrack == null ? mSelectedTrack : null));

        for(int length = 1; length <= 10; length++)
        {
            mTrackHistoryLengthComboBox.getItems().add(length);
        }

        mTrackHistoryLengthComboBox.setValue(mTrackLayer.getTrackHistoryLength());
        mTrackHistoryLengthComboBox.setOnAction(event -> {
            Integer length = mTrackHistoryLengthComboBox.getValue();

            if(length != null)
            {
                mTrackLayer.setTrackHistoryLength(length);
                mMap.requestRedraw();
            }
        });

        mCenterOnSelectedCheckBox.setSelected(true);

        initHistoryTable();
    }

    private void initTrackTable()
    {
        TableColumn<PlottableEntityHistory,String> id = new TableColumn<>("ID");
        id.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getIdentifier() != null ?
            cell.getValue().getIdentifier().toString() : "(no ID)"));
        TableColumn<PlottableEntityHistory,String> alias = new TableColumn<>("Alias");
        alias.setCellValueFactory(cell -> new ReadOnlyStringWrapper(mEntityModel.getAliasName(cell.getValue())));
        TableColumn<PlottableEntityHistory,String> list = new TableColumn<>("List");
        list.setCellValueFactory(cell ->
            new ReadOnlyStringWrapper(PlottableEntityModel.getAliasListName(cell.getValue())));
        mTrackTable.getColumns().addAll(List.of(id, alias, list));
        mTrackTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        mTrackTable.setPlaceholder(new Label("No tracks"));
        mTrackTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        SortedList<PlottableEntityHistory> sorted = new SortedList<>(mTracks);
        sorted.comparatorProperty().bind(mTrackTable.comparatorProperty());
        mTrackTable.setItems(sorted);

        mTrackTable.getSelectionModel().getSelectedItems().addListener(
            (ListChangeListener<PlottableEntityHistory>)change ->
                mDeleteTrackButton.setDisable(mTrackTable.getSelectionModel().getSelectedItems().isEmpty()));
        //The selected item is updated after the selected items list changes, so it has its own listener
        mTrackTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue != mSelectedTrack)
            {
                setSelected(newValue);
            }

            //Refresh the followed entity button and label
            follow(mFollowedTrack);
        });
    }

    private void initHistoryTable()
    {
        SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        DecimalFormat degreeFormat = new DecimalFormat("0.00000");
        TableColumn<TimestampedGeoPosition,String> time = new TableColumn<>("Time");
        time.setCellValueFactory(cell -> new ReadOnlyStringWrapper(timeFormat.format(cell.getValue().getTimestamp())));
        TableColumn<TimestampedGeoPosition,String> latitude = new TableColumn<>("Latitude");
        latitude.setCellValueFactory(cell ->
            new ReadOnlyStringWrapper(degreeFormat.format(cell.getValue().getLatitude())));
        TableColumn<TimestampedGeoPosition,String> longitude = new TableColumn<>("Longitude");
        longitude.setCellValueFactory(cell ->
            new ReadOnlyStringWrapper(degreeFormat.format(cell.getValue().getLongitude())));
        time.setPrefWidth(130);
        latitude.setPrefWidth(75);
        longitude.setPrefWidth(80);
        mHistoryTable.getColumns().addAll(List.of(time, latitude, longitude));
        mHistoryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        mHistoryTable.setPlaceholder(new Label("Select a track"));
        mHistoryTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue != null && !mRestoringHistorySelection && mCenterOnSelectedCheckBox.isSelected() &&
                mFollowedTrack == null)
            {
                mMap.setCenter(newValue);
            }
        });
    }

    /**
     * Tabs beside the map: Settings for how the map looks, and Tracks for the plotted tracks
     */
    private TabPane createControls()
    {
        HBox server = row(new Label("Tile Server:"), mTileServerComboBox);
        HBox serverKey = row(mTileServerKeyField, mTileServerKeyLink);
        HBox location = row(new Label("My Location:"), mUserLocationLabel);
        HBox locationButtons = row(mSetUserLocationButton, mCenterOnUserLocationButton);
        HBox zoom = row(new Label("Zoom:"), mZoomSpinner);
        HBox sites = row(mShowSitesCheckBox, mSiteCountLabel);
        HBox historyLength = row(new Label("Track History Length:"), mTrackHistoryLengthComboBox);

        HBox.setHgrow(mTileServerComboBox, Priority.ALWAYS);
        mTileServerComboBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(mTileServerKeyField, Priority.ALWAYS);
        serverKey.visibleProperty().bind(mTileServerKeyField.visibleProperty());
        serverKey.managedProperty().bind(mTileServerKeyField.managedProperty());

        Label layers = new Label("Layers");
        layers.setStyle("-fx-font-weight: bold;");

        Hyperlink moreSettings = new Hyperlink("More map settings...");
        moreSettings.setOnAction(event -> MyEventBus.getGlobalEventBus()
            .post(new ViewSettingsEditorRequest(SettingsPageType.MAP)));

        VBox settings = tabContent(server, serverKey, new Separator(), location, locationButtons, new Separator(),
            zoom, new Separator(), layers, sites, new Separator(), moreSettings);

        HBox trackButtons = row(mDeleteTrackButton, mDeleteAllTracksButton, mClearMapButton);
        HBox followButtons = row(mReplotAllTracksButton, mFollowButton, mFollowedEntityLabel);
        HBox selectedSystem = row(new Label("Selected System:"), mSelectedTrackSystemLabel);

        VBox.setVgrow(mTrackTable, Priority.ALWAYS);
        VBox.setVgrow(mHistoryTable, Priority.ALWAYS);
        mTrackTable.setMinHeight(80);
        mHistoryTable.setMinHeight(80);

        VBox tracks = tabContent(mTrackTable, trackButtons, followButtons, historyLength, mCenterOnSelectedCheckBox,
            selectedSystem, mHistoryTable);

//Uncomment this line to add a debug track generator button to the tracks tab
//        tracks.getChildren().add(createTrackGeneratorToggle());

        Tab settingsTab = new Tab("Settings", settings);
        Tab tracksTab = new Tab("Tracks", tracks);
        TabPane tabs = new TabPane(settingsTab, tracksTab);
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.setMinWidth(220);
        tabs.setPrefWidth(CONTROLS_WIDTH);
        return tabs;
    }

    private static VBox tabContent(javafx.scene.Node... nodes)
    {
        VBox content = new VBox(6, nodes);
        content.setPadding(new Insets(6));
        return content;
    }

    private static HBox row(javafx.scene.Node... nodes)
    {
        HBox row = new HBox(6, nodes);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    /**
     * Debug toggle that starts and stops a generator of test tracks
     */
    @SuppressWarnings("unused")
    private javafx.scene.control.ToggleButton createTrackGeneratorToggle()
    {
        io.github.dsheirer.map.TrackGenerator generator = new io.github.dsheirer.map.TrackGenerator(mMapService);
        javafx.scene.control.ToggleButton toggle = new javafx.scene.control.ToggleButton("Track Generator");
        toggle.setOnAction(event -> {
            if(toggle.isSelected())
            {
                generator.start();
            }
            else
            {
                generator.stop();
            }
        });
        return toggle;
    }

    /**
     * Tile server chooser, with a key field and a link to get a key for a server that needs one
     */
    private void initTileServerControls()
    {
        MapSettings preference = mSettings.getMapSettings();
        mTileServerComboBox.getItems().setAll(TileChoice.CHOICES);
        mTileServerComboBox.setValue(TileChoice.get(preference.getTileServer(), preference.getTileStyle()));
        mTileServerComboBox.setVisibleRowCount(TileChoice.CHOICES.size());
        mTileServerComboBox.setTooltip(new Tooltip("Map tile server.  Auto follows the theme, " +
            "Light and Dark always use that style.  Servers without a dark style are recolored for dark tiles."));
        mTileServerComboBox.setOnAction(event -> {
            TileChoice choice = mTileServerComboBox.getValue();

            if(choice != null)
            {
                preference.setTileServer(choice.server().id());
                preference.setTileStyle(choice.style().name());
                updateTileSource();
            }
        });

        mTileServerKeyField.setPromptText("API key");
        mTileServerKeyField.setTooltip(new Tooltip("Free API key for this tile server, from the link beside it"));
        mTileServerKeyField.setOnAction(event -> saveTileServerKey());
        mTileServerKeyField.focusedProperty().addListener((observable, oldValue, focused) -> {
            if(!focused)
            {
                saveTileServerKey();
            }
        });
        mTileServerKeyLink.setOnAction(event -> {
            TileServer server = getTileServer();

            if(server != null && server.keyUrl() != null)
            {
                browse(server.keyUrl());
            }
        });
    }

    /**
     * Stores the key the user entered for the selected server and reloads the tiles with it
     */
    private void saveTileServerKey()
    {
        TileServer server = getTileServer();

        if(server != null && server.requiresKey())
        {
            mSettings.getMapSettings().setTileServerKey(server.id(), mTileServerKeyField.getText());
            updateTileSource();
        }
    }

    /**
     * Shows the selected server's tiles in the chosen style, or the style for the current theme, and its key field when it needs a key
     */
    private void updateTileSource()
    {
        TileChoice choice = mTileServerComboBox.getValue() != null ? mTileServerComboBox.getValue() :
            TileChoice.CHOICES.getFirst();
        TileServer server = choice.server();
        String key = server.requiresKey() ? mSettings.getMapSettings().getTileServerKey(server.id()) : null;

        mTileServerKeyField.setVisible(server.requiresKey());
        mTileServerKeyField.setManaged(server.requiresKey());

        if(server.requiresKey() && !mTileServerKeyField.isFocused())
        {
            mTileServerKeyField.setText(key != null ? key : "");
        }

        mMap.setSource(choice.getSource(mSettings.getApplicationSettings().isDarkMode(), key));
    }

    /**
     * Applies map settings changed elsewhere, such as on the settings dialog's Map page.  Panning and zooming also
     * update the map settings, so this only changes what differs from the stored settings.
     */
    private void applyMapSettings()
    {
        MapSettings mapSettings = mSettings.getMapSettings();
        TileChoice stored = TileChoice.get(mapSettings.getTileServer(), mapSettings.getTileStyle());

        if(!stored.equals(mTileServerComboBox.getValue()))
        {
            //The combo box action stores the choice and updates the tile source
            mTileServerComboBox.setValue(stored);
        }
        else
        {
            TileServer server = stored.server();

            if(server.requiresKey() && !mTileServerKeyField.isFocused())
            {
                String key = mapSettings.getTileServerKey(server.id());

                if(!mTileServerKeyField.getText().equals(key != null ? key : ""))
                {
                    updateTileSource();
                }
            }
        }

        boolean show = mapSettings.isShowRadioReferenceSites();

        if(show != mShowSitesCheckBox.isSelected())
        {
            mShowSitesCheckBox.setSelected(show);
            mSiteLayer.setShowSites(show);
            hideCard();
            mMap.requestRedraw();
        }
    }

    /**
     * Server of the selected tile choice
     */
    private TileServer getTileServer()
    {
        TileChoice choice = mTileServerComboBox.getValue();
        return choice != null ? choice.server() : TileServer.OPEN_STREET_MAP;
    }

    /**
     * Opens the web page in the user's browser, off the JavaFX thread
     */
    public static void browse(String url)
    {
        Thread.ofVirtual().start(() -> {
            try
            {
                DesktopLauncher.browse(url);
            }
            catch(Exception e)
            {
                mLog.warn("Unable to open [" + url + "] - " + e.getMessage());
            }
        });
    }

    /**
     * Updates the site count and redraws the sites
     */
    private void sitesUpdated()
    {
        int count = mFrequencyIndex != null ? mFrequencyIndex.getSites().size() : 0;
        mSiteCountLabel.setText(count == 1 ? "1 site" : count + " sites");
        hideCard();
        mMap.requestRedraw();
    }

    /**
     * Updates the user's location on the map when it changes, here or in the radio reference editor
     * @param settingsType that was updated
     */
    @Subscribe
    public void settingsUpdated(SettingsType settingsType)
    {
        if(settingsType == SettingsType.RADIO_REFERENCE)
        {
            Platform.runLater(this::updateUserLocation);
        }
        else if(settingsType == SettingsType.APPLICATION)
        {
            //The theme may have changed between light and dark
            Platform.runLater(this::updateTileSource);
        }
        else if(settingsType == SettingsType.MAP)
        {
            //The tile server, key or layers may have changed on the settings dialog's Map page
            Platform.runLater(this::applyMapSettings);
        }
    }

    /**
     * Shows the user's location on the map.  When the location changes, the map centers on it, unless it was chosen on
     * the map.  Clearing the location removes the pin and leaves the map as is.
     */
    private void updateUserLocation()
    {
        RadioReferenceSettings.Location location = mSettings.getRadioReferenceSettings().getLocation();
        GeoPosition position = location != null ? toGeoPosition(location) : null;
        updateUserLocationLabel(location);

        if(!Objects.equals(position, mUserLocationLayer.getPosition()))
        {
            mUserLocationLayer.setPosition(position);

            //A location chosen on the map is already in view
            if(position != null && !position.equals(mMapChosenPosition))
            {
                mMap.setCenter(position);
            }

            mMapChosenPosition = null;
            mMap.requestRedraw();
        }
    }

    private void updateUserLocationLabel(RadioReferenceSettings.Location location)
    {
        mUserLocationLabel.setText(location != null ? location.coordinates() : LOCATION_NOT_SET);
        mCenterOnUserLocationButton.setDisable(location == null);
        mUserLocationLabel.getTooltip().setText(location != null ? location.describe() :
            "Set your location with the Set Current button or in the playlist editor's Radio Reference tab");
    }

    /**
     * Sets the user's location to a point chosen on the map, with the Set Current button or the map's 'Set My Location
     * Here' menu item.  The pin, the location label and the radio reference editor follow through the preference
     * update.  The map isn't re-centered, since the point is already in view.
     * @param position chosen on the map
     */
    private void setUserLocation(GeoPosition position)
    {
        double latitude = round(position.getLatitude());
        double longitude = round(Mercator.normalizeLongitude(position.getLongitude()));

        if(RadioReferenceSettings.Location.isValid(latitude, longitude))
        {
            mMapChosenPosition = new GeoPosition(latitude, longitude);
            mSettings.getRadioReferenceSettings()
                .setLocation(new RadioReferenceSettings.Location(latitude, longitude, null));
        }
    }

    /**
     * Stores the map's center point and zoom level, so the map shows the same view when the application starts
     */
    private void saveView()
    {
        GeoPosition center = mMap.getCenter();
        MapSettings preference = mSettings.getMapSettings();

        try
        {
            preference.setCenter(new MapSettings.Center(round(center.getLatitude()),
                round(Mercator.normalizeLongitude(center.getLongitude()))));
        }
        catch(IllegalArgumentException e)
        {
            //Not a storable position - keep the previous center
        }

        preference.setTileZoom(mMap.getZoom());
    }

    /**
     * Rounds degrees to six decimal places (about 0.1 meter)
     */
    private static double round(double degrees)
    {
        return Math.round(degrees * 1_000_000.0) / 1_000_000.0;
    }

    private static GeoPosition toGeoPosition(RadioReferenceSettings.Location location)
    {
        return new GeoPosition(location.latitude(), location.longitude());
    }

    private void setSelected(PlottableEntityHistory selected)
    {
        mSelectedTrack = selected;
        updateHistory();

        if(selected != null)
        {
            Identifier system = selected.getIdentifierCollection().getIdentifier(IdentifierClass.CONFIGURATION,
                Form.SYSTEM, Role.ANY);
            mSelectedTrackSystemLabel.setText(system != null ? system.toString() : NO_SYSTEM_NAME);

            if(mTrackLayer.add(selected))
            {
                mMap.requestRedraw();
            }

            //While following, the map stays on the followed track
            if(mCenterOnSelectedCheckBox.isSelected() && mFollowedTrack == null)
            {
                centerOn(selected);
            }
        }
        else
        {
            mSelectedTrackSystemLabel.setText(SELECT_A_TRACK);
        }
    }

    /**
     * Shows the selected track's location history
     */
    private void updateHistory()
    {
        if(mSelectedTrack != null)
        {
            TimestampedGeoPosition selected = mHistoryTable.getSelectionModel().getSelectedItem();
            mHistory.setAll(mSelectedTrack.getTrackHistoryModel().getTrackHistory());

            //Keep the selected row, without centering on it again
            if(selected != null && mHistory.contains(selected))
            {
                mRestoringHistorySelection = true;
                mHistoryTable.getSelectionModel().select(selected);
                mRestoringHistorySelection = false;
            }
        }
        else
        {
            mHistory.clear();
        }
    }

    private void centerOn(PlottableEntityHistory entityHistory)
    {
        if(entityHistory != null && entityHistory.getLatestPosition() != null)
        {
            mMap.setCenter(entityHistory.getLatestPosition());
        }
    }

    /**
     * Follows an entity, keeping the map centered on it as it reports new locations, or stops following
     * @param entityHistory to follow, or null to stop following
     */
    private void follow(PlottableEntityHistory entityHistory)
    {
        boolean changed = entityHistory != mFollowedTrack;
        mFollowedTrack = entityHistory;

        if(mFollowedTrack != null)
        {
            if(changed)
            {
                centerOn(mFollowedTrack);
            }

            mFollowButton.setText(UNFOLLOW);
            mFollowButton.setDisable(false);
            mFollowedEntityLabel.setText("Following: " + mFollowedTrack.getIdentifier());
            //Disabled while following
            mCenterOnSelectedCheckBox.setDisable(true);
        }
        else
        {
            mFollowButton.setText(FOLLOW);
            mFollowButton.setDisable(mSelectedTrack == null);
            mFollowedEntityLabel.setText(null);
            mCenterOnSelectedCheckBox.setDisable(false);
        }
    }

    private void entityAdded(PlottableEntityHistory entity)
    {
        if(!mTracks.contains(entity))
        {
            mTracks.add(entity);
        }

        mTrackLayer.add(entity);
        mMap.requestRedraw();
    }

    private void entityUpdated(PlottableEntityHistory entity)
    {
        if(!mTracks.contains(entity))
        {
            //Deleted while the update was on its way
            return;
        }

        //A cleared track returns when it reports a new location
        mTrackLayer.add(entity);
        mTrackTable.refresh();

        if(entity == mSelectedTrack)
        {
            updateHistory();
        }

        if(entity == mFollowedTrack)
        {
            centerOn(entity);
        }

        mMap.requestRedraw();
    }

    private void entitiesRemoved(List<PlottableEntityHistory> entities)
    {
        mTracks.removeAll(entities);
        mTrackLayer.removeAll(entities);

        if(mFollowedTrack != null && entities.contains(mFollowedTrack))
        {
            follow(null);
        }

        mMap.requestRedraw();
    }

    /**
     * Context menu for the point on the map: the radio reference site actions, then 'Set My Location Here'
     */
    private void showContextMenu(ContextMenuEvent event)
    {
        hideCard();

        if(mContextMenu != null)
        {
            mContextMenu.hide();
        }

        GeoPosition position = mMap.getViewport().getPosition(event.getX(), event.getY());
        ContextMenu menu = new ContextMenu();
        List<MenuItem> siteItems = getSiteMenuItems(event.getX(), event.getY());

        if(!siteItems.isEmpty())
        {
            menu.getItems().addAll(siteItems);
            menu.getItems().add(new SeparatorMenuItem());
        }

        MenuItem setLocation = new MenuItem("Set My Location Here");
        setLocation.setOnAction(actionEvent -> setUserLocation(position));
        menu.getItems().add(setLocation);

        mContextMenu = menu;
        menu.show(mMap, event.getScreenX(), event.getScreenY());
        event.consume();
    }

    /**
     * Menu items for the radio reference sites under the point: add a site to the playlist and start it, or enable or
     * disable the playlist channel that already decodes it
     */
    private List<MenuItem> getSiteMenuItems(double x, double y)
    {
        List<MenuItem> items = new ArrayList<>();

        if(mPlaylistActions == null)
        {
            return items;
        }

        List<SiteRecord> sites = mSiteLayer.getSitesAt(mMap.getViewport(), x, y);

        for(SiteRecord site: sites.subList(0, Math.min(sites.size(), MAX_MENU_SITES)))
        {
            String label = site.systemName() + " · Site " + site.siteNumber() +
                (site.description() != null ? " " + site.description() : "");
            String unsupported = RadioReferencePlaylistActions.getUnsupportedReason(site);

            if(unsupported != null)
            {
                MenuItem item = new MenuItem(label + " – " + unsupported);
                item.setDisable(true);
                items.add(item);
                continue;
            }

            if(RadioReferencePlaylistActions.isRepeaterSite(site))
            {
                addRepeaterSiteItems(items, site, label);
                continue;
            }

            Channel existing = mPlaylistActions.findSiteChannel(site);

            if(existing != null)
            {
                boolean processing = existing.isProcessing();
                MenuItem item = new MenuItem((processing ? "Disable: " : "Enable: ") + existing.getShortTitle());
                item.setOnAction(event -> {
                    if(processing)
                    {
                        mPlaylistActions.disable(existing, this::showSiteError);
                    }
                    else
                    {
                        mPlaylistActions.enable(existing, this::showSiteError);
                    }
                });
                items.add(item);
            }
            else
            {
                MenuItem item = new MenuItem("Add and Start: " + label);
                item.setOnAction(event -> mPlaylistActions.addSite(site, this::showSiteError));
                items.add(item);
            }
        }

        return items;
    }

    /**
     * Menu items for a site whose repeaters each have a channel (LTR): add and start the repeaters that aren't in the
     * playlist, and enable or disable the site's channels
     */
    private void addRepeaterSiteItems(List<MenuItem> items, SiteRecord site, String label)
    {
        List<Channel> existing = mPlaylistActions.findSiteChannels(site);
        int missing = mPlaylistActions.getMissingRepeaterFrequencies(site).size();
        boolean anyDecoding = existing.stream().anyMatch(Channel::isProcessing);
        boolean anyStopped = existing.stream().anyMatch(channel -> !channel.isProcessing());

        if(missing > 0)
        {
            MenuItem item = new MenuItem("Add and Start: " + label + " (" + missing + " repeater" +
                (missing > 1 ? "s" : "") + ")");
            item.setOnAction(event -> mPlaylistActions.addSite(site, this::showSiteError));
            items.add(item);
        }
        else if(anyStopped)
        {
            MenuItem item = new MenuItem("Enable: " + label + " (" + existing.size() + " channels)");
            item.setOnAction(event -> mPlaylistActions.enableAll(existing, this::showSiteError));
            items.add(item);
        }

        if(anyDecoding)
        {
            long decoding = existing.stream().filter(Channel::isProcessing).count();
            MenuItem item = new MenuItem("Disable: " + label + " (" + decoding + " channel" +
                (decoding > 1 ? "s" : "") + ")");
            item.setOnAction(event -> mPlaylistActions.disableAll(existing, this::showSiteError));
            items.add(item);
        }
    }

    /**
     * Shows an error from a site action.  Can be called from any thread.
     */
    private void showSiteError(String message)
    {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR, message);
            alert.setTitle("Radio Reference");
            alert.setHeaderText(null);

            if(getScene() != null && getScene().getWindow() != null)
            {
                alert.initOwner(getScene().getWindow());
            }

            alert.show();
        });
    }

    /**
     * Shows the site card once the mouse rests over a tower.  While the card is up, it stays as long as the mouse is
     * over the same sites.
     */
    private void mouseMoved(MouseEvent event)
    {
        mHoverX = event.getX();
        mHoverY = event.getY();

        if(mCard != null)
        {
            List<SiteRecord> sites = mSiteLayer.getSitesAt(mMap.getViewport(), mHoverX, mHoverY);

            if(sites.equals(mCardSites))
            {
                return;
            }

            hideCard();
        }

        mHoverTimer.playFromStart();
    }

    /**
     * Shows the card of the sites under the mouse, if any, beside the mouse and inside the map
     */
    void showCard()
    {
        hideCard();
        List<SiteRecord> sites = mSiteLayer.getSitesAt(mMap.getViewport(), mHoverX, mHoverY);

        if(sites.isEmpty())
        {
            return;
        }

        Region card = SiteCard.create(SiteCard.content(sites,
            mSettings.getRadioReferenceSettings().getLocation()));
        mCardPane.getChildren().add(card);
        card.applyCss();
        card.autosize();

        double width = card.getWidth();
        double height = card.getHeight();
        double x = mHoverX + CARD_OFFSET;
        double y = mHoverY + CARD_OFFSET;

        if(x + width > mCardPane.getWidth() - CARD_MARGIN)
        {
            x = Math.max(CARD_MARGIN, mHoverX - CARD_OFFSET - width);
        }

        if(y + height > mCardPane.getHeight() - CARD_MARGIN)
        {
            y = Math.max(CARD_MARGIN, mHoverY - CARD_OFFSET - height);
        }

        card.relocate(x, y);
        mCard = card;
        mCardSites = sites;
    }

    /**
     * Shows the site card for the point, for tests and off-screen rendering
     */
    void showCard(double x, double y)
    {
        mHoverX = x;
        mHoverY = y;
        showCard();
    }

    private void hideCard()
    {
        mHoverTimer.stop();

        if(mCard != null)
        {
            mCardPane.getChildren().remove(mCard);
            mCard = null;
            mCardSites = Collections.emptyList();
        }
    }

    /**
     * Moves entity changes from the decode event thread to the JavaFX thread
     */
    private class EntityListener implements IPlottableUpdateListener
    {
        @Override
        public void entityAdded(PlottableEntityHistory entity)
        {
            Platform.runLater(() -> MapView.this.entityAdded(entity));
        }

        @Override
        public void entityUpdated(PlottableEntityHistory entity)
        {
            Platform.runLater(() -> MapView.this.entityUpdated(entity));
        }

        @Override
        public void entitiesRemoved(List<PlottableEntityHistory> entities)
        {
            Platform.runLater(() -> MapView.this.entitiesRemoved(entities));
        }
    }
}
