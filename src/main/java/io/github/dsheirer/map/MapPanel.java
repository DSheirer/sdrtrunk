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
package io.github.dsheirer.map;

import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.icon.IconModel;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.map.MapPreference;
import io.github.dsheirer.preference.radioreference.RadioReferencePreference;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.gui.playlist.radioreference.RadioReferencePlaylistActions;
import io.github.dsheirer.service.radioreference.RadioReferenceFrequencyIndex;
import io.github.dsheirer.service.radioreference.SiteRecord;
import java.awt.EventQueue;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import net.miginfocom.swing.MigLayout;
import org.jdesktop.swingx.JXMapViewer;
import org.jdesktop.swingx.OSMTileFactoryInfo;
import org.jdesktop.swingx.input.PanKeyListener;
import org.jdesktop.swingx.input.ZoomMouseWheelListenerCursor;
import org.jdesktop.swingx.mapviewer.DefaultTileFactory;
import org.jdesktop.swingx.mapviewer.GeoPosition;
import org.jdesktop.swingx.mapviewer.TileFactoryInfo;
import org.jdesktop.swingx.painter.CompoundPainter;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.ToolTipManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Swing map panel.
 */
public class MapPanel extends JPanel implements IPlottableUpdateListener
{
    private static final long serialVersionUID = 1L;

    private static final int ZOOM_MINIMUM = 1;
    private static final int ZOOM_MAXIMUM = 16;
    //Delay after the last pan or zoom before the map view is stored
    private static final int SAVE_VIEW_DELAY_MS = 500;

    private static final String FOLLOW = "Follow";
    private static final String UNFOLLOW = "Unfollow";
    private static final String SELECT_A_TRACK = "(select a track)";
    private static final String NO_SYSTEM_NAME = "(no system name)";
    private static final String LOCATION_NOT_SET = "Not set";
    private Timer mSaveViewTimer;
    private final RadioReferenceFrequencyIndex mFrequencyIndex;
    private final RadioReferenceSitePainter mSitePainter;
    private final RadioReferencePlaylistActions mPlaylistActions;
    private JCheckBox mShowSitesCheckBox;
    private JLabel mSiteCountLabel;
    private int mDefaultTooltipDismissDelay = -1;
    private UserPreferences mUserPreferences;
    private final UserLocationPainter mUserLocationPainter = new UserLocationPainter();
    private JLabel mUserLocationLabel;
    private JButton mSetUserLocationButton;
    private JButton mCenterOnUserLocationButton;
    //Location most recently chosen on the map, which doesn't re-center the map when the preference update arrives
    private GeoPosition mMapChosenPosition;
    private MapService mMapService;
    private JXMapViewer mMapViewer;
    private PlottableEntityPainter mMapPainter;
    private TrackGenerator mTrackGenerator;
    private JToggleButton mTrackGeneratorToggle;
    private JTable mPlottedTracksTable;
    private JButton mClearMapButton;
    private JButton mReplotAllTracksButton;
    private JButton mDeleteAllTracksButton;
    private JButton mDeleteTrackButton;
    private JButton mFollowButton;
    private JLabel mFollowedEntityLabel;
    private JCheckBox mCenterOnSelectedCheckBox;
    private PlottableEntityHistory mFollowedTrack;
    private JComboBox<Integer> mTrackHistoryLengthComboBox;
    private JSpinner mMapZoomSpinner;
    private SpinnerNumberModel mMapZoomSpinnerModel;
    private JTable mTrackHistoryTable;
    private JLabel mSelectedTrackSystemLabel;
    private final TrackHistoryModel EMPTY_HISTORY = new TrackHistoryModel();

    /**
     * Constructs an instance
     * @param mapService for accessing entities to plot
     * @param aliasModel for alias lookup
     * @param iconModel for icon lookup
     * @param userPreferences for the map view and the user's location, which is shared with the radio reference editor
     * @param frequencyIndex of the radio reference frequencies near the user, for the trunked system sites, or null
     * @param playlistActions to add sites to the playlist or enable their channels, or null
     */
    public MapPanel(MapService mapService, AliasModel aliasModel, IconModel iconModel, UserPreferences userPreferences,
                    RadioReferenceFrequencyIndex frequencyIndex, RadioReferencePlaylistActions playlistActions)
    {
        mUserPreferences = userPreferences;
        mFrequencyIndex = frequencyIndex;
        mPlaylistActions = playlistActions;
        mSitePainter = new RadioReferenceSitePainter(frequencyIndex != null ? frequencyIndex::getSites :
            Collections::emptyList);
        mSitePainter.setShowSites(userPreferences.getMapPreference().isShowRadioReferenceSites());
        mMapService = mapService;
        mMapPainter = new PlottableEntityPainter(aliasModel, iconModel);

        init();

        //Show the stored location without moving the map, which starts at the stored default location
        RadioReferencePreference.Location location = mUserPreferences.getRadioReferencePreference().getLocation();
        mUserLocationPainter.setPosition(location != null ? toGeoPosition(location) : null);
        updateUserLocationLabel(location);

        if(mFrequencyIndex != null)
        {
            //The index fills in the background
            mFrequencyIndex.addUpdateListener(() -> EventQueue.invokeLater(this::sitesUpdated));
        }

        sitesUpdated();
        MyEventBus.getGlobalEventBus().register(this);
    }

    /**
     * Updates the site count and repaints the sites.  Call on the Swing thread.
     */
    private void sitesUpdated()
    {
        int count = mFrequencyIndex != null ? mFrequencyIndex.getSites().size() : 0;
        getSiteCountLabel().setText(count == 1 ? "1 site" : count + " sites");
        getMapViewer().repaint();
    }

    private JCheckBox getShowSitesCheckBox()
    {
        if(mShowSitesCheckBox == null)
        {
            mShowSitesCheckBox = new JCheckBox("Radio Reference Sites", mSitePainter.isShowSites());
            mShowSitesCheckBox.setEnabled(mFrequencyIndex != null);
            mShowSitesCheckBox.setToolTipText("Show the trunked radio system sites within " +
                (int)RadioReferenceFrequencyIndex.RADIUS_KILOMETERS + " km of your location.  Requires a premium " +
                "Radio Reference login and your location.");
            mShowSitesCheckBox.addActionListener(e -> {
                boolean show = mShowSitesCheckBox.isSelected();
                mSitePainter.setShowSites(show);
                mUserPreferences.getMapPreference().setShowRadioReferenceSites(show);
                getMapViewer().repaint();
            });
        }

        return mShowSitesCheckBox;
    }

    private JLabel getSiteCountLabel()
    {
        if(mSiteCountLabel == null)
        {
            mSiteCountLabel = new JLabel();
        }

        return mSiteCountLabel;
    }

    /**
     * Context menu items for the radio reference sites under the point: add a site to the playlist and start it, or
     * enable or disable the playlist channel that already decodes it
     */
    private List<JMenuItem> getSiteMenuItems(Point point)
    {
        List<JMenuItem> items = new ArrayList<>();

        if(mPlaylistActions == null)
        {
            return items;
        }

        List<SiteRecord> sites = mSitePainter.getSitesAt(getMapViewer(), point);

        for(SiteRecord site: sites.subList(0, Math.min(sites.size(), 3)))
        {
            String label = site.systemName() + " \u00B7 Site " + site.siteNumber() +
                (site.description() != null ? " " + site.description() : "");
            String unsupported = RadioReferencePlaylistActions.getUnsupportedReason(site);

            if(unsupported != null)
            {
                JMenuItem item = new JMenuItem(label + " \u2013 " + unsupported);
                item.setEnabled(false);
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
                JMenuItem item = new JMenuItem((processing ? "Disable: " : "Enable: ") + existing.getShortTitle());
                item.addActionListener(e -> {
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
                JMenuItem item = new JMenuItem("Add and Start: " + label);
                item.addActionListener(e -> mPlaylistActions.addSite(site, this::showSiteError));
                items.add(item);
            }
        }

        return items;
    }

    /**
     * Menu items for a site whose repeaters each have a channel (LTR): add and start the repeaters that aren't in the
     * playlist, and enable or disable the site's channels
     */
    private void addRepeaterSiteItems(List<JMenuItem> items, SiteRecord site, String label)
    {
        List<Channel> existing = mPlaylistActions.findSiteChannels(site);
        int missing = mPlaylistActions.getMissingRepeaterFrequencies(site).size();
        boolean anyDecoding = existing.stream().anyMatch(Channel::isProcessing);
        boolean anyStopped = existing.stream().anyMatch(channel -> !channel.isProcessing());

        if(missing > 0)
        {
            JMenuItem item = new JMenuItem("Add and Start: " + label + " (" + missing + " repeater" +
                (missing > 1 ? "s" : "") + ")");
            item.addActionListener(e -> mPlaylistActions.addSite(site, this::showSiteError));
            items.add(item);
        }
        else if(anyStopped)
        {
            JMenuItem item = new JMenuItem("Enable: " + label + " (" + existing.size() + " channels)");
            item.addActionListener(e -> mPlaylistActions.enableAll(existing, this::showSiteError));
            items.add(item);
        }

        if(anyDecoding)
        {
            long decoding = existing.stream().filter(Channel::isProcessing).count();
            JMenuItem item = new JMenuItem("Disable: " + label + " (" + decoding + " channel" +
                (decoding > 1 ? "s" : "") + ")");
            item.addActionListener(e -> mPlaylistActions.disableAll(existing, this::showSiteError));
            items.add(item);
        }
    }

    /**
     * Shows an error from a site action.  Can be called from any thread.
     */
    private void showSiteError(String message)
    {
        EventQueue.invokeLater(() -> JOptionPane.showMessageDialog(this, message, "Radio Reference",
            JOptionPane.ERROR_MESSAGE));
    }

    /**
     * Card for the radio reference sites under the point, or null when there are none
     */
    private String getSiteCard(Point point)
    {
        List<SiteRecord> sites = mSitePainter.getSitesAt(getMapViewer(), point);
        return sites.isEmpty() ? null : RadioReferenceSiteCard.html(sites,
            mUserPreferences.getRadioReferencePreference().getLocation());
    }

    /**
     * Updates the user's location on the map when it changes, here or in the radio reference editor
     * @param preferenceType that was updated
     */
    @Subscribe
    public void preferenceUpdated(PreferenceType preferenceType)
    {
        if(preferenceType == PreferenceType.RADIO_REFERENCE)
        {
            EventQueue.invokeLater(this::updateUserLocation);
        }
    }

    /**
     * Shows the user's location on the map.  When the location changes, the map centers on it, unless it was chosen on
     * the map.  Clearing the location removes the pin and leaves the map as is.  Call on the Swing thread.
     */
    private void updateUserLocation()
    {
        RadioReferencePreference.Location location = mUserPreferences.getRadioReferencePreference().getLocation();
        GeoPosition position = location != null ? toGeoPosition(location) : null;
        updateUserLocationLabel(location);

        if(!Objects.equals(position, mUserLocationPainter.getPosition()))
        {
            mUserLocationPainter.setPosition(position);

            //A location chosen on the map is already in view
            if(position != null && !position.equals(mMapChosenPosition))
            {
                getMapViewer().setAddressLocation(position);
            }

            mMapChosenPosition = null;

            getMapViewer().repaint();
        }
    }

    private void updateUserLocationLabel(RadioReferencePreference.Location location)
    {
        getUserLocationLabel().setText(location != null ? location.coordinates() : LOCATION_NOT_SET);
        getCenterOnUserLocationButton().setEnabled(location != null);
        getUserLocationLabel().setToolTipText(location != null ? location.describe() :
            "Set your location with the Set Current button or in the playlist editor's Radio Reference tab");
    }

    private JLabel getUserLocationLabel()
    {
        if(mUserLocationLabel == null)
        {
            mUserLocationLabel = new JLabel(LOCATION_NOT_SET);
        }

        return mUserLocationLabel;
    }

    /**
     * Button that sets the user's location to the center of the map
     */
    private JButton getSetUserLocationButton()
    {
        if(mSetUserLocationButton == null)
        {
            mSetUserLocationButton = new JButton("Set Current");
            mSetUserLocationButton.setToolTipText("Sets your location, which is also used by the playlist editor's " +
                "Radio Reference tab, to the center of the map");
            mSetUserLocationButton.addActionListener(e -> setUserLocation(getMapViewer().getCenterPosition()));
        }

        return mSetUserLocationButton;
    }

    /**
     * Button that centers the map on the user's location.  Enabled when the location is set.
     */
    private JButton getCenterOnUserLocationButton()
    {
        if(mCenterOnUserLocationButton == null)
        {
            mCenterOnUserLocationButton = new JButton("Center On");
            mCenterOnUserLocationButton.setToolTipText("Centers the map on your location");
            mCenterOnUserLocationButton.setEnabled(false);
            mCenterOnUserLocationButton.addActionListener(e -> {
                GeoPosition position = mUserLocationPainter.getPosition();

                if(position != null)
                {
                    getMapViewer().setCenterPosition(position);
                }
            });
        }

        return mCenterOnUserLocationButton;
    }

    /**
     * Sets the user's location to a point chosen on the map, with the Set Current button or the map's 'Set My Location
     * Here' menu item.  The pin, the location label and the radio reference editor follow through the preference
     * update.  The map isn't re-centered, since the point is already in view.
     * Call on the Swing thread.
     * @param position chosen on the map
     */
    private void setUserLocation(GeoPosition position)
    {
        double latitude = round(position.getLatitude());
        double longitude = round(normalizeLongitude(position.getLongitude()));

        if(RadioReferencePreference.Location.isValid(latitude, longitude))
        {
            mMapChosenPosition = new GeoPosition(latitude, longitude);
            mUserPreferences.getRadioReferencePreference()
                .setLocation(new RadioReferencePreference.Location(latitude, longitude, null));
        }
    }

    /**
     * Stores the map's center point and zoom level, so the map shows the same view when the application starts.
     * Call on the Swing thread.
     */
    private void saveView()
    {
        GeoPosition center = getMapViewer().getCenterPosition();
        MapPreference preference = mUserPreferences.getMapPreference();

        try
        {
            preference.setCenter(new MapPreference.Center(round(center.getLatitude()),
                round(normalizeLongitude(center.getLongitude()))));
        }
        catch(IllegalArgumentException e)
        {
            //Not a storable position - keep the previous center
        }

        preference.setZoom(getMapViewer().getZoom());
    }

    /**
     * Wraps a longitude into -180 to 180 degrees, since the map can be panned past +/-180 degrees
     */
    private static double normalizeLongitude(double longitude)
    {
        return ((longitude + 180.0) % 360.0 + 360.0) % 360.0 - 180.0;
    }

    /**
     * Rounds degrees to six decimal places (about 0.1 meter)
     */
    private static double round(double degrees)
    {
        return Math.round(degrees * 1_000_000.0) / 1_000_000.0;
    }

    private static GeoPosition toGeoPosition(RadioReferencePreference.Location location)
    {
        return new GeoPosition(location.latitude(), location.longitude());
    }

    private void init()
    {
        setLayout(new MigLayout("insets 0 0 0 0", "[grow,fill]", "[grow,fill]"));
        mMapService.addListener(this);

        JPanel cp = new JPanel();
        cp.setLayout(new MigLayout("insets 5", "[grow,fill][grow,fill][grow,fill]",
                "[][][][][grow,fill][][][][][][grow,fill][]"));

        //The columns stretch the labels to the column width, so the label aligns its own text
        cp.add(new JLabel("My Location:", SwingConstants.RIGHT), "align right");
        cp.add(getUserLocationLabel(), "span 2, wrap");

        cp.add(getSetUserLocationButton());
        cp.add(getCenterOnUserLocationButton(), "wrap");

        cp.add(new JLabel("Zoom:", SwingConstants.RIGHT), "align right");
        //Spinner at its natural width, rather than filling the column
        cp.add(getMapZoomSpinner(), "wmax pref, wrap");

        cp.add(getShowSitesCheckBox(), "span 2");
        cp.add(getSiteCountLabel(), "wrap");

        cp.add(new JScrollPane(getPlottedTracksTable()), "span 3,wrap");

        cp.add(getDeleteTrackButton());
        cp.add(getDeleteAllTracksButton());
        cp.add(getClearMapButton(), "wrap");

        cp.add(getReplotAllTracksButton());
        cp.add(getFollowButton());
        cp.add(getFollowedEntityLabel(), "wrap");

        cp.add(getTrackHistoryLengthComboBox());
        cp.add(new JLabel("Track History Length"), "span 2, wrap");

        cp.add(getCenterOnSelectedCheckBox(), "span 3, wrap");

        cp.add(new JLabel("Selected System:", SwingConstants.RIGHT), "align right");
        cp.add(getSelectedTrackSystemLabel(), "span 2, wrap");

        cp.add(new JScrollPane(getTrackHistoryTable()), "span 3,wrap");

//Uncomment this line to add a debug track generator button to the display
//        cp.add(getTrackGeneratorToggle(), "span 3,wrap");

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(300);
        splitPane.add(cp);
        splitPane.add(getMapViewer());
        add(splitPane, "span");
    }

    private void setSelected(PlottableEntityHistory selected)
    {
        if(selected != null)
        {
            getTrackHistoryTable().setModel(selected.getTrackHistoryModel());

            Identifier system = selected.getIdentifierCollection().getIdentifier(IdentifierClass.CONFIGURATION, Form.SYSTEM, Role.ANY);

            if(system != null)
            {
                getSelectedTrackSystemLabel().setText(system.toString());
            }
            else
            {
                getSelectedTrackSystemLabel().setText(NO_SYSTEM_NAME);
            }

            if(mMapPainter.addEntity(getSelected()))
            {
                getMapViewer().repaint();
            }
        }
        else
        {
            getTrackHistoryTable().setModel(EMPTY_HISTORY);
            getSelectedTrackSystemLabel().setText(SELECT_A_TRACK);
        }

        if(getCenterOnSelectedCheckBox().isSelected())
        {
            centerOn(selected);
        }
    }

    /**
     * Replots all tracks to the map (after a clear operation).
     * @return button
     */
    private JButton getReplotAllTracksButton()
    {
        if(mReplotAllTracksButton == null)
        {
            mReplotAllTracksButton = new JButton("Replot All");
            mReplotAllTracksButton.addActionListener(e ->
            {
                boolean added = mMapPainter.addAll(mMapService.getPlottableEntityModel().getAll());

                if(added)
                {
                    getMapViewer().repaint();
                }
            });
        }

        return mReplotAllTracksButton;
    }

    private JLabel getSelectedTrackSystemLabel()
    {
        if(mSelectedTrackSystemLabel == null)
        {
            mSelectedTrackSystemLabel = new JLabel(SELECT_A_TRACK);
        }

        return mSelectedTrackSystemLabel;
    }

    private JTable getTrackHistoryTable()
    {
        if(mTrackHistoryTable == null)
        {
            mTrackHistoryTable = new JTable(EMPTY_HISTORY);
            mTrackHistoryTable.getSelectionModel().addListSelectionListener(e ->
            {
                if(getCenterOnSelectedCheckBox().isSelected())
                {
                    int modelIndex = getTrackHistoryTable().convertRowIndexToModel(getTrackHistoryTable().getSelectedRow());
                    TimestampedGeoPosition geo = ((TrackHistoryModel)(getTrackHistoryTable().getModel())).get(modelIndex);

                    if(geo != null)
                    {
                        mMapViewer.setCenterPosition(geo);
                    }
                }
            });
        }

        return mTrackHistoryTable;
    }

    /**
     * Map zoom level combo box
     */
    private JSpinner getMapZoomSpinner()
    {
        if(mMapZoomSpinner == null)
        {
            mMapZoomSpinnerModel = new SpinnerNumberModel(2, ZOOM_MINIMUM, ZOOM_MAXIMUM, 1);
            mMapZoomSpinnerModel.addChangeListener(new ChangeListener()
            {
                @Override
                public void stateChanged(ChangeEvent e)
                {
                    Number number = mMapZoomSpinnerModel.getNumber();
                    mMapViewer.setZoom(number.intValue());
                }
            });

            mMapZoomSpinner = new JSpinner(mMapZoomSpinnerModel);
        }

        return mMapZoomSpinner;
    }

    /**
     * Plotted track history trail length selection combo box.
     */
    private JComboBox<Integer> getTrackHistoryLengthComboBox()
    {
        if(mTrackHistoryLengthComboBox == null)
        {
            List<Integer> lengths = new ArrayList<>();
            for(int length = 1; length <= 10; length++)
            {
                lengths.add(length);
            }

            mTrackHistoryLengthComboBox = new JComboBox<>(lengths.toArray(new Integer[]{lengths.size()}));
            mTrackHistoryLengthComboBox.setSelectedItem(mMapPainter.getTrackHistoryLength());

            mTrackHistoryLengthComboBox.addActionListener(e ->
            {
                int length = (int)getTrackHistoryLengthComboBox().getSelectedItem();
                mMapPainter.setTrackHistoryLength(length);
            });
        }

        return mTrackHistoryLengthComboBox;
    }

    /**
     * Label to show the followed entity
     */
    private JLabel getFollowedEntityLabel()
    {
        if(mFollowedEntityLabel == null)
        {
            mFollowedEntityLabel = new JLabel(" ");
        }

        return mFollowedEntityLabel;
    }

    /**
     * Toggles the following state for an entity.
     */
    private JButton getFollowButton()
    {
        if(mFollowButton == null)
        {
            mFollowButton = new JButton(FOLLOW);
            mFollowButton.setEnabled(false);
            mFollowButton.addActionListener(e ->
            {
                if(getFollowButton().getText().equals(FOLLOW))
                {
                    follow(getSelected());
                }
                else
                {
                    follow(null);
                }
            });
        }

        return mFollowButton;
    }

    private JButton getClearMapButton()
    {
        if(mClearMapButton == null)
        {
            mClearMapButton = new JButton("Clear Map");
            mClearMapButton.addActionListener(e ->
            {
                mMapPainter.clearAllEntities();
                repaint();
            });
        }

        return mClearMapButton;
    }

    /**
     * Toggles the behavior of centering on the selected track when a user selects a track in the table.
     * @return check box.
     */
    private JToggleButton getCenterOnSelectedCheckBox()
    {
        if(mCenterOnSelectedCheckBox == null)
        {
            mCenterOnSelectedCheckBox = new JCheckBox("Center on Selection");
            mCenterOnSelectedCheckBox.setSelected(true);
        }

        return mCenterOnSelectedCheckBox;
    }

    private JButton getDeleteAllTracksButton()
    {
        if(mDeleteAllTracksButton == null)
        {
            mDeleteAllTracksButton = new JButton("Delete All");
            mDeleteAllTracksButton.addActionListener(e -> {
                mMapService.getPlottableEntityModel().deleteAllTracks();
                mMapPainter.clearAllEntities();
                //Clear followed entity
                follow(null);
                getMapViewer().repaint();
            });
        }

        return mDeleteAllTracksButton;
    }

    private JButton getDeleteTrackButton()
    {
        if(mDeleteTrackButton == null)
        {
            mDeleteTrackButton = new JButton("Delete");
            mDeleteTrackButton.setEnabled(false);
            mDeleteTrackButton.addActionListener(e -> {

                List<PlottableEntityHistory> toDelete = new ArrayList<>();
                int[] selectedIndices = getPlottedTracksTable().getSelectionModel().getSelectedIndices();

                for(int selectedIndex : selectedIndices)
                {
                    int modelIndex = getPlottedTracksTable().convertRowIndexToModel(selectedIndex);
                    PlottableEntityHistory entity = mMapService.getPlottableEntityModel().get(modelIndex);
                    if(entity != null)
                    {
                        toDelete.add(entity);

                        //Clear followed entity if it's being deleted
                        if(entity.equals(mFollowedTrack))
                        {
                            follow(null);
                        }
                    }
                }
                mMapService.getPlottableEntityModel().delete(toDelete);
                mMapPainter.clearEntities(toDelete);
                getMapViewer().repaint();
            });
        }

        return mDeleteTrackButton;
    }

    /**
     * Access the selected entity history.
     * @return selected entity or null of one is not selected.
     */
    private PlottableEntityHistory getSelected()
    {
        if(getPlottedTracksTable().getSelectedRow() >= 0)
        {
            int modelIndex = getPlottedTracksTable().convertRowIndexToModel(getPlottedTracksTable().getSelectedRow());
            return mMapService.getPlottableEntityModel().get(modelIndex);
        }

        return null;
    }

    /**
     * Centers on the plottable entity.
     * @param entityHistory to center on
     */
    private void centerOn(PlottableEntityHistory entityHistory)
    {
        if(entityHistory != null)
        {
            GeoPosition geoPosition = entityHistory.getLatestPosition();

            if(geoPosition != null)
            {
                mMapViewer.setCenterPosition(geoPosition);
            }
        }
    }

    /**
     * Follow or unfollow an entity.
     * @param entityHistory to follow or null to unfollow.
     */
    private void follow(PlottableEntityHistory entityHistory)
    {
        mFollowedTrack = entityHistory;

        if(mFollowedTrack != null)
        {
            centerOn(mFollowedTrack);
            getFollowButton().setText(UNFOLLOW);
            getFollowButton().setEnabled(true);
            getFollowedEntityLabel().setText("Following: " + mFollowedTrack.getIdentifier());
            getCenterOnSelectedCheckBox().setEnabled(false); //Disabled while we're following
        }
        else
        {
            getFollowButton().setText(FOLLOW);
            getFollowButton().setEnabled(getSelected() != null);
            getFollowedEntityLabel().setText(null);
            getCenterOnSelectedCheckBox().setEnabled(true);
        }
    }

    private JTable getPlottedTracksTable()
    {
        if(mPlottedTracksTable == null)
        {
            mPlottedTracksTable = new JTable(mMapService.getPlottableEntityModel());
            mPlottedTracksTable.setAutoCreateRowSorter(true);

            //Register selection listener to update button/label states
            mPlottedTracksTable.getSelectionModel().addListSelectionListener(e ->
            {
                //Toggle the enabled state of the delete (single) track button
                int count = mPlottedTracksTable.getSelectionModel().getSelectedItemsCount();
                getDeleteTrackButton().setEnabled(count > 0);

                PlottableEntityHistory selected = getSelected();
                setSelected(selected);

                //Refresh the followed entity button/label states
                follow(mFollowedTrack);
            });
        }

        return mPlottedTracksTable;
    }

    private JToggleButton getTrackGeneratorToggle()
    {
        if(mTrackGeneratorToggle == null)
        {
            mTrackGeneratorToggle = new JToggleButton("Track Generator");
            mTrackGeneratorToggle.addActionListener(e -> {
                if(mTrackGeneratorToggle.isSelected())
                {
                    getTrackGenerator().start();
                }
                else
                {
                    getTrackGenerator().stop();
                }
            });
        }

        return mTrackGeneratorToggle;
    }

    /**
     * Optional test track generator
     */
    private TrackGenerator getTrackGenerator()
    {
        if(mTrackGenerator == null)
        {
            mTrackGenerator = new TrackGenerator(mMapService);
        }

        return mTrackGenerator;
    }

    public JXMapViewer getMapViewer()
    {
        if(mMapViewer == null)
        {
            //The tooltip is the card of the radio reference sites under the mouse
            mMapViewer = new JXMapViewer()
            {
                @Override
                public String getToolTipText(MouseEvent event)
                {
                    return getSiteCard(event.getPoint());
                }

                @Override
                public Point getToolTipLocation(MouseEvent event)
                {
                    return new Point(event.getX() + 16, event.getY() + 16);
                }
            };
            ToolTipManager.sharedInstance().registerComponent(mMapViewer);
            //Keep the card up long enough to read the channels while the mouse is over the map
            mMapViewer.addMouseListener(new MouseAdapter()
            {
                @Override
                public void mouseEntered(MouseEvent event)
                {
                    mDefaultTooltipDismissDelay = ToolTipManager.sharedInstance().getDismissDelay();
                    ToolTipManager.sharedInstance().setDismissDelay(60_000);
                }

                @Override
                public void mouseExited(MouseEvent event)
                {
                    if(mDefaultTooltipDismissDelay >= 0)
                    {
                        ToolTipManager.sharedInstance().setDismissDelay(mDefaultTooltipDismissDelay);
                    }
                }
            });

            /**
             * Set the entity painter as the overlay painter and register this panel to receive new messages (plots)
             */
            //Radio reference sites under the tracks, and the user's location pin over the tracks
            mMapViewer.setOverlayPainter(new CompoundPainter<>(mSitePainter, mMapPainter, mUserLocationPainter));

            /**
             * Map image source
             */
            TileFactoryInfo info = new OSMTileFactoryInfo();
            DefaultTileFactory tileFactory = new DefaultTileFactory(info);
            mMapViewer.setTileFactory(tileFactory);

            /**
             * Defines how many threads will be used to fetch the background map tiles (graphics)
             */
            tileFactory.setThreadPoolSize(8);

            /**
             * Set initial location and zoom for the map upon display: the last view, or else the user's location, or
             * else Syracuse, NY.
             */
            MapPreference mapPreference = mUserPreferences.getMapPreference();
            MapPreference.Center center = mapPreference.getCenter();
            RadioReferencePreference.Location location = mUserPreferences.getRadioReferencePreference().getLocation();
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
                initial = new GeoPosition(43.048, -76.147);
            }

            //Set the zoom on the map too: the spinner doesn't update the map when the value equals its initial value
            mMapViewer.setZoom(mapPreference.getZoom());
            mMapViewer.setAddressLocation(initial);
            mMapZoomSpinnerModel.setValue(mapPreference.getZoom());

            //Remember the view after each pan or zoom, once the map stops moving
            mSaveViewTimer = new Timer(SAVE_VIEW_DELAY_MS, e -> saveView());
            mSaveViewTimer.setRepeats(false);
            mMapViewer.addPropertyChangeListener(event -> {
                if("centerPosition".equals(event.getPropertyName()) || "zoom".equals(event.getPropertyName()))
                {
                    mSaveViewTimer.restart();
                }
            });

            /**
             * Add a mouse adapter for panning and scrolling
             */
            MapMouseListener listener = new MapMouseListener(mMapViewer, this::setUserLocation, this::getSiteMenuItems);
            mMapViewer.addMouseListener(listener);
            mMapViewer.addMouseMotionListener(listener);

            /* Map zoom listener */
            mMapViewer.addMouseWheelListener(new ZoomMouseWheelListenerCursor(this));

            /* Keyboard panning listener */
            mMapViewer.addKeyListener(new PanKeyListener(mMapViewer));

            /**
             * Add a selection listener
             */
            SelectionAdapter sa = new SelectionAdapter(mMapViewer);
            mMapViewer.addMouseListener(sa);
            mMapViewer.addMouseMotionListener(sa);
        }

        return mMapViewer;
    }

    /**
     * Changes the zoom level by the specified value.
     * @param adjustment zoom value.
     */
    public void adjustZoom(int adjustment)
    {
        Number currentZoom = mMapZoomSpinnerModel.getNumber();
        int updatedZoom = currentZoom.intValue() + adjustment;

        if(ZOOM_MINIMUM <= updatedZoom && updatedZoom <= ZOOM_MAXIMUM)
        {
            mMapZoomSpinnerModel.setValue(currentZoom.intValue() + adjustment);
        }
    }

    @Override
    public void entitiesUpdated()
    {
        EventQueue.invokeLater(() -> mMapViewer.repaint());
    }

    @Override
    public void addPlottableEntity(PlottableEntityHistory entity)
    {
        mMapPainter.addEntity(entity);
        entitiesUpdated();
    }

    @Override
    public void removePlottableEntity(PlottableEntityHistory entity)
    {
        mMapPainter.removeEntity(entity);
        entitiesUpdated();
    }
}
