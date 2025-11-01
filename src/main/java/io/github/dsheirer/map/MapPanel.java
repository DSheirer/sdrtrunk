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
package io.github.dsheirer.map;

import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.icon.IconModel;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.settings.MapViewSetting;
import io.github.dsheirer.settings.SettingsManager;
import net.miginfocom.swing.MigLayout;
import org.jdesktop.swingx.JXMapViewer;
import org.jdesktop.swingx.OSMTileFactoryInfo;
import org.jdesktop.swingx.input.PanKeyListener;
import org.jdesktop.swingx.input.ZoomMouseWheelListenerCursor;
import org.jdesktop.swingx.mapviewer.DefaultTileFactory;
import org.jdesktop.swingx.mapviewer.GeoPosition;
import org.jdesktop.swingx.mapviewer.TileFactoryInfo;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.List;

/**
 * Swing map panel.
 */
public class MapPanel extends JPanel implements IPlottableUpdateListener
{
    private static final long serialVersionUID = 1L;

    private static final int ZOOM_MINIMUM = 1;
    private static final int ZOOM_MAXIMUM = 16;

    private static final String FOLLOW = "Follow";
    private static final String UNFOLLOW = "Unfollow";
    private static final String SELECT_A_TRACK = "(select a track)";
    private static final String NO_SYSTEM_NAME = "(no system name)";
    private SettingsManager mSettingsManager;
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
    private TrackHistoryModel mTrackHistoryModel = new TrackHistoryModel();
    private JTable mTrackHistoryTable;
    private JLabel mSelectedTrackSystemLabel;

    private io.github.dsheirer.preference.UserPreferences mUserPreferences;

    /**
     * Constructs an instance
     * @param mapService for accessing entities to plot
     * @param aliasModel for alias lookup
     * @param iconModel for icon lookup
     * @param settingsManager for user specified options/settings.
     * @param userPreferences for accessing user preferences like dark mode
     */
    public MapPanel(MapService mapService, AliasModel aliasModel, IconModel iconModel, SettingsManager settingsManager, io.github.dsheirer.preference.UserPreferences userPreferences)
    {
        mSettingsManager = settingsManager;
        mMapService = mapService;
        mMapPainter = new PlottableEntityPainter(aliasModel, iconModel);
        mUserPreferences = userPreferences;

        init();
    }

    private void init()
    {
        setLayout(new MigLayout("insets 0 0 0 0", "[grow,fill]", "[grow,fill]"));
        mMapService.addListener(this);

        JPanel cp = new JPanel();
        cp.setLayout(new MigLayout("insets 5", "[grow,fill][grow,fill][grow,fill]",
                "[grow,fill][][][][][][grow,fill][]"));
        cp.add(new JScrollPane(getPlottedTracksTable()), "span 3,wrap");

        cp.add(getMapZoomSpinner());
        JPanel zoomCenterPanel = new JPanel();
        zoomCenterPanel.setLayout(new MigLayout("insets 0", "[]5[grow,fill]", ""));
        zoomCenterPanel.add(new JLabel("Zoom"));
        zoomCenterPanel.add(getCenterOnSelectedCheckBox());
        cp.add(zoomCenterPanel, "span 2, wrap");

        cp.add(getDeleteTrackButton());
        cp.add(getDeleteAllTracksButton());
        cp.add(getClearMapButton(), "wrap");

        cp.add(getReplotAllTracksButton());
        cp.add(getFollowButton());
        cp.add(getFollowedEntityLabel(), "wrap");

        cp.add(getTrackHistoryLengthComboBox());
        cp.add(new JLabel("Track History Length"), "span 2, wrap");

        cp.add(new JLabel("Selected System:"), "align right");
        cp.add(getSelectedTrackSystemLabel(), "span 2, wrap");

        cp.add(new JScrollPane(getTrackHistoryTable()), "span 3,wrap");

//        cp.add(getTrackGeneratorToggle(), "span 3,wrap");

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(300);
        splitPane.add(cp);
        splitPane.add(getMapViewer());
        add(splitPane, "span");
    }

    private void setSelected(PlottableEntityHistory selected)
    {
        mTrackHistoryModel.load(selected);

        if(selected != null)
        {
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
            mReplotAllTracksButton.setOpaque(true);
            mReplotAllTracksButton.setContentAreaFilled(true);
            if(mUserPreferences != null && mUserPreferences.getColorThemePreference().isDarkModeEnabled())
            {
                mReplotAllTracksButton.setBackground(new java.awt.Color(43, 43, 43));
                mReplotAllTracksButton.setForeground(new java.awt.Color(187, 187, 187));
            }
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
            mTrackHistoryTable = new JTable(mTrackHistoryModel);
//            mTrackHistoryTable.getSelectionModel().addListSelectionListener(e ->
//            {
//                if(getCenterOnSelectedCheckBox().isSelected())
//                {
//                    int modelIndex = getTrackHistoryTable().convertRowIndexToModel(getTrackHistoryTable().getSelectedRow());
//                    TimestampedGeoPosition geo = mTrackHistoryModel.get(modelIndex);
//
//                    if(geo != null)
//                    {
//                        mMapViewer.setCenterPosition(geo);
//                    }
//                }
//            });
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
            mClearMapButton.setOpaque(true);
            mClearMapButton.setContentAreaFilled(true);
            if(mUserPreferences != null && mUserPreferences.getColorThemePreference().isDarkModeEnabled())
            {
                mClearMapButton.setBackground(new java.awt.Color(43, 43, 43));
                mClearMapButton.setForeground(new java.awt.Color(187, 187, 187));
            }
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
            mCenterOnSelectedCheckBox.setOpaque(true);
            if(mUserPreferences != null && mUserPreferences.getColorThemePreference().isDarkModeEnabled())
            {
                mCenterOnSelectedCheckBox.setBackground(new java.awt.Color(43, 43, 43));
                mCenterOnSelectedCheckBox.setForeground(new java.awt.Color(187, 187, 187));
            }
            mCenterOnSelectedCheckBox.setSelected(true);
        }

        return mCenterOnSelectedCheckBox;
    }

    private JButton getDeleteAllTracksButton()
    {
        if(mDeleteAllTracksButton == null)
        {
            mDeleteAllTracksButton = new JButton("Delete All");
            mDeleteAllTracksButton.setOpaque(true);
            mDeleteAllTracksButton.setContentAreaFilled(true);
            if(mUserPreferences != null && mUserPreferences.getColorThemePreference().isDarkModeEnabled())
            {
                mDeleteAllTracksButton.setBackground(new java.awt.Color(43, 43, 43));
                mDeleteAllTracksButton.setForeground(new java.awt.Color(187, 187, 187));
            }
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
            mMapService.getPlottableEntityModel().addTableModelListener(e ->
            {
                //Update the followed entity for DELETE/UPDATE operations
                if(mFollowedTrack != null)
                {
                    if(e.getType() == TableModelEvent.DELETE)
                    {
                        for(int x = e.getFirstRow(); x <= e.getLastRow(); x++)
                        {
                            if(mMapService.getPlottableEntityModel().get(x).equals(mFollowedTrack))
                            {
                                follow(null);
                                return;
                            }
                        }
                    }
                    else if(e.getType() == TableModelEvent.UPDATE)
                    {
                        if(e.getFirstRow() == 0 && e.getLastRow() == Integer.MAX_VALUE)
                        {
                            return;
                        }

                        for(int x = e.getFirstRow(); x <= e.getLastRow(); x++)
                        {
                            PlottableEntityHistory entity = mMapService.getPlottableEntityModel().get(x);

                            if(entity.equals(getSelected()))
                            {
                                mTrackHistoryModel.update();
                            }

                            if(entity != null && entity.equals(mFollowedTrack))
                            {
                                centerOn(mMapService.getPlottableEntityModel().get(x));
                                return;
                            }
                        }
                    }
                }

                if(e.getType() == TableModelEvent.UPDATE && !(e.getFirstRow() == 0 && e.getLastRow() == Integer.MAX_VALUE))
                {
                    for(int x = e.getFirstRow(); x <= e.getLastRow(); x++)
                    {
                        PlottableEntityHistory entity = mMapService.getPlottableEntityModel().get(x);

                        if(entity != null && entity.equals(getSelected()))
                        {
                            mTrackHistoryModel.update();
                        }
                    }
                }
                else if(e.getType() == TableModelEvent.DELETE && getSelected() == null)
                {
                    mTrackHistoryModel.load(null);
                }
            });

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
            mMapViewer = new JXMapViewer();

            /**
             * Set the entity painter as the overlay painter and register this panel to receive new messages (plots)
             */
            mMapViewer.setOverlayPainter(mMapPainter);

            /**
             * Map image source
             * Note: Dark mode map tiles have compatibility issues with JXMapViewer.
             * The map will use standard OpenStreetMap tiles regardless of dark mode setting.
             * All UI controls around the map will still respect dark mode.
             */
            TileFactoryInfo info = new OSMTileFactoryInfo();
            
            DefaultTileFactory tileFactory = new DefaultTileFactory(info);
            mMapViewer.setTileFactory(tileFactory);

            /**
             * Defines how many threads will be used to fetch the background map tiles (graphics)
             */
            tileFactory.setThreadPoolSize(8);

            /**
             * Set initial location and zoom for the map upon display
             */
            GeoPosition syracuse = new GeoPosition(43.048, -76.147);
            int zoom = 7;

            MapViewSetting view = mSettingsManager.getMapViewSetting("Default", syracuse, zoom);

            mMapViewer.setAddressLocation(view.getGeoPosition());
            mMapZoomSpinnerModel.setValue(view.getZoom());

            /**
             * Add a mouse adapter for panning and scrolling
             */
            MapMouseListener listener = new MapMouseListener(mMapViewer, mSettingsManager);
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
