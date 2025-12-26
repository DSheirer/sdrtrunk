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

import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.MutableIdentifierCollection;
import io.github.dsheirer.identifier.configuration.AliasListConfigurationIdentifier;
import io.github.dsheirer.identifier.configuration.FrequencyConfigurationIdentifier;
import io.github.dsheirer.identifier.configuration.SiteConfigurationIdentifier;
import io.github.dsheirer.identifier.configuration.SystemConfigurationIdentifier;
import io.github.dsheirer.module.decode.dmr.identifier.DMRRadio;
import io.github.dsheirer.module.decode.event.DecodeEventType;
import io.github.dsheirer.module.decode.event.PlottableDecodeEvent;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25RadioIdentifier;
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
    private static final GeoPosition DEFAULT_START_POSITION = new GeoPosition(43.048, -76.147);
    private final MapService mMapService;
    private final List<TrackElementGenerator> mTrackElementGenerators = new ArrayList<>();

    /**
     * Constructs an instance to publish tracks to the specified map service
     * @param mapService to receive test tracks.
     */
    public TrackGenerator(MapService mapService)
    {
        mMapService = mapService;

        Random random = new Random();

        int trackCount = 25;
        for(int x = 0; x < trackCount; x++)
        {
            MutableIdentifierCollection mic = new MutableIdentifierCollection();
            mic.update(SystemConfigurationIdentifier.create("Test System"));
            mic.update(SiteConfigurationIdentifier.create("Test Site"));
            mic.update(FrequencyConfigurationIdentifier.create(155000000l));
            if(x % 2 == 1)
            {
                mic.update(AliasListConfigurationIdentifier.create("DMR " + x + " LIST"));
                mic.update(DMRRadio.createFrom(x + 1));
            }
            else
            {
                mic.update(AliasListConfigurationIdentifier.create("P25 " + x + " LIST"));
                mic.update(APCO25RadioIdentifier.createFrom(x + 1));
            }
            double baseSpeedKPH = 20.0;
            double speedKPH = baseSpeedKPH + random.nextDouble(15);
            mTrackElementGenerators.add(new TrackElementGenerator(speedKPH, mic));
        }
    }

    /**
     * Starts the test generator
     */
    public void start()
    {
        for(TrackElementGenerator generator: mTrackElementGenerators)
        {
            generator.start();
        }
    }

    /**
     * Stops the test generator
     */
    public void stop()
    {
        for(TrackElementGenerator generator: mTrackElementGenerators)
        {
            generator.stop();
        }
    }

    /**
     * Test track element generator
     */
    public class TrackElementGenerator
    {
        public static double EARTH_RADIUS_KM = 6378.137;
        public static double ONE_SECOND = 1.0 / 60.0 / 60.0; //1 hour divided by 60 minutes divided by 60 seconds.
        private IdentifierCollection mIdentifierCollection;
        private double mSpeedKPH;
        private GeoPosition mPosition;
        private Random mRandom = new Random();
        private double mHeading = 360.0 * mRandom.nextDouble();
        private ScheduledFuture<?> mGeneratorFuture;

        /**
         * Constructs an instance
         * @param speedKPH speed in KPH
         * @param identifierCollection for the track
         */
        public TrackElementGenerator(double speedKPH, IdentifierCollection identifierCollection)
        {
            mSpeedKPH = speedKPH;
            mIdentifierCollection = identifierCollection;
            double latOffset = (1.0 - (mRandom.nextFloat() * 2)) / 800;
            double lonOffset = (1.0 - (mRandom.nextFloat() * 2)) / 800;
            mPosition = new GeoPosition(DEFAULT_START_POSITION.getLatitude() + latOffset,
                    DEFAULT_START_POSITION.getLongitude() + lonOffset);
        }

        /**
         * Starts this track generator
         */
        public void start()
        {
            if(mGeneratorFuture == null)
            {
                long intervalMS = 500 + (mRandom.nextLong(1000));
                mSpeedKPH /= (intervalMS / 1000.0);
                System.out.println("Speed: " + mSpeedKPH);
                mGeneratorFuture = ThreadPool.SCHEDULED.scheduleAtFixedRate(this::update, 0, intervalMS, TimeUnit.MILLISECONDS);
            }
        }

        /**
         * Stops this track generator
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
         * Pushes a new update to the map service.
         */
        public void update()
        {
            mHeading = mHeading + (30.0 - (mRandom.nextDouble(60)));
            if(mHeading < 0)
            {
                mHeading += 360;
            }
            else if(mHeading >= 360)
            {
                mHeading -= 360;
            }
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
            mMapService.receive(event);
        }
    }
}
