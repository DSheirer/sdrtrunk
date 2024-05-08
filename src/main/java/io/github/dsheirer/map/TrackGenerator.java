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

import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.MutableIdentifierCollection;
import io.github.dsheirer.identifier.configuration.AliasListConfigurationIdentifier;
import io.github.dsheirer.identifier.configuration.FrequencyConfigurationIdentifier;
import io.github.dsheirer.identifier.configuration.SiteConfigurationIdentifier;
import io.github.dsheirer.identifier.configuration.SystemConfigurationIdentifier;
import io.github.dsheirer.module.decode.dmr.identifier.DMRRadio;
import io.github.dsheirer.module.decode.event.DecodeEventType;
import io.github.dsheirer.module.decode.event.PlottableDecodeEvent;
import io.github.dsheirer.protocol.Protocol;
import io.github.dsheirer.util.ThreadPool;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.jdesktop.swingx.mapviewer.GeoPosition;

/**
 * Map test track data generator.
 *
 * Creates plottable events and publishes them to the map service.  Issues periodic updates to the plottable events.
 */
public class TrackGenerator
{
    private static GeoPosition DEFAULT_START_POSITION = new GeoPosition(43.048, -76.147);
    private static final String ALIAS_LIST_NAME = "DMR Test Alias List";
    private MapService mMapService;
    private List<TrackElementGenerator> mTrackElementGenerators = new ArrayList<>();
    private double mBaseSpeedKPH = 40.0;
    private int mTrackCount = 25;
    private ScheduledFuture<?> mGeneratorFuture;

    /**
     * Constructs an instance to publish tracks to the specified map service
     * @param mapService to receive test tracks.
     */
    public TrackGenerator(MapService mapService)
    {
        mMapService = mapService;

        MutableIdentifierCollection base = new MutableIdentifierCollection();
        base.update(SystemConfigurationIdentifier.create("Test System"));
        base.update(SiteConfigurationIdentifier.create("Test Site"));
        base.update(FrequencyConfigurationIdentifier.create(155000000l));
        base.update(AliasListConfigurationIdentifier.create(ALIAS_LIST_NAME));

        Random random = new Random();

        for(int x = 0; x < mTrackCount; x++)
        {
            MutableIdentifierCollection mic = new MutableIdentifierCollection(base.getIdentifiers());
            mic.update(DMRRadio.createFrom(x + 1));
            double speedKPH = mBaseSpeedKPH + (random.nextDouble() * 15);
            mTrackElementGenerators.add(new TrackElementGenerator(speedKPH, mic));
        }
    }

    /**
     * Starts the test generator
     */
    public void start()
    {
        if(mGeneratorFuture == null)
        {
            mGeneratorFuture = ThreadPool.SCHEDULED.scheduleAtFixedRate(() -> update(), 0, 1, TimeUnit.SECONDS);
        }
    }

    /**
     * Stops the test generator
     */
    public void stop()
    {
        if(mGeneratorFuture != null)
        {
            mGeneratorFuture.cancel(true);
            mGeneratorFuture = null;
        }
    }

    /**
     * Updates each of the track generates causing them to dispatch a new decode event to the map service.
     *
     * Invoke this method once per second.
     */
    private void update()
    {
        for(TrackElementGenerator tg: mTrackElementGenerators)
        {
            mMapService.receive(tg.update());
        }
    }

    /**
     * Test track element generator
     */
    public static class TrackElementGenerator
    {
        public static double EARTH_RADIUS_KM = 6378.137;
        public static double ONE_SECOND = 1.0 / 60.0 / 60.0; //1 hour divided by 60 minutes divided by 60 seconds.
        private IdentifierCollection mIdentifierCollection;
        private double mSpeedKPH;
        private GeoPosition mPosition = new GeoPosition(DEFAULT_START_POSITION.getLatitude(), DEFAULT_START_POSITION.getLongitude());
        private Random mRandom = new Random();
        private double mHeading = 360.0 * mRandom.nextDouble();

        /**
         * Constructs an instance
         * @param trackId for the track
         * @param speedKPH speed in KPH
         */
        public TrackElementGenerator(double speedKPH, IdentifierCollection identifierCollection)
        {
            mSpeedKPH = speedKPH;
            mIdentifierCollection = identifierCollection;
        }

        /**
         *
         * @param heading 0-360 degrees
         */
        public PlottableDecodeEvent update()
        {
            mHeading = mHeading + (15.0 - (mRandom.nextDouble() * 30.0));
            mHeading = Math.max(mHeading, 0);
            mHeading = Math.min(mHeading, 360.0);
            double distanceKM = mSpeedKPH * ONE_SECOND;
            double headingRadians = Math.toRadians(mHeading);

            double angularDistance = distanceKM / EARTH_RADIUS_KM;
            double latRadians = Math.toRadians(mPosition.getLatitude());
            double lonRadians = Math.toRadians(mPosition.getLongitude());
            double latitude = Math.asin((Math.sin(latRadians) * Math.cos(angularDistance)) +
                    (Math.cos(latRadians) * Math.sin(angularDistance) * Math.cos(headingRadians)));
            double longitude = lonRadians + Math.atan2(Math.sin(headingRadians) * Math.sin(angularDistance) *
                    Math.cos(latRadians), Math.cos(angularDistance) - Math.sin(latRadians) * Math.sin(latitude));

            latitude = Math.toDegrees(latitude);
            longitude = Math.toDegrees(longitude);
            mPosition = new GeoPosition(latitude, longitude);
            PlottableDecodeEvent event =
                    new PlottableDecodeEvent.PlottableDecodeEventBuilder(DecodeEventType.GPS, System.currentTimeMillis())
                            .heading(mHeading)
                            .speed(mSpeedKPH)
                            .location(mPosition)
                            .protocol(Protocol.DMR)
                            .identifiers(mIdentifierCollection)
                            .build();
            return event;
        }
    }
}
