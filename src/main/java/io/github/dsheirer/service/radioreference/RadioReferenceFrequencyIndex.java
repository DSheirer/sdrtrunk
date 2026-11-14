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

package io.github.dsheirer.service.radioreference;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.radioreference.RadioReferencePreference;
import io.github.dsheirer.properties.SystemProperties;
import io.github.dsheirer.rrapi.RadioReferenceException;
import io.github.dsheirer.rrapi.RadioReferenceJson;
import io.github.dsheirer.rrapi.RadioReferenceService;
import io.github.dsheirer.rrapi.type.Agency;
import io.github.dsheirer.rrapi.type.AgencyInfo;
import io.github.dsheirer.rrapi.type.Category;
import io.github.dsheirer.rrapi.type.County;
import io.github.dsheirer.rrapi.type.CountyInfo;
import io.github.dsheirer.rrapi.type.Coverage;
import io.github.dsheirer.rrapi.type.Flavor;
import io.github.dsheirer.rrapi.type.Frequency;
import io.github.dsheirer.rrapi.type.Mode;
import io.github.dsheirer.rrapi.type.Site;
import io.github.dsheirer.rrapi.type.SiteFrequency;
import io.github.dsheirer.rrapi.type.StateInfo;
import io.github.dsheirer.rrapi.type.SubCategory;
import io.github.dsheirer.rrapi.type.Type;
import io.github.dsheirer.rrapi.type.Voice;
import io.github.dsheirer.util.ThreadPool;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Index of the radio reference frequencies near the user's location, for looking up the candidates for a signal by
 * frequency without calling the web service.
 *
 * The radio reference web service can't search by location, so the index is built in the background for the states
 * around the user's location (see {@link StateBounds}): the conventional frequencies of the counties within
 * {@link #RADIUS_KILOMETERS} of the user, the trunked sites within that distance of the county and statewide systems,
 * and the nearby frequencies of the county and state agencies.  The index is saved to disk and refreshed weekly.  It's
 * stored by source (county, system, agency) with each source's radio reference update date, so a refresh only fetches
 * the frequencies of the sources that changed.  Requests are spaced out, nearest sources first, and the index is
 * usable while it's being built.
 *
 * Builds only with a premium account and a location set by the user.  Call {@link #start()} on the JavaFX application
 * thread to begin.
 */
public class RadioReferenceFrequencyIndex
{
    private static final Logger mLog = LoggerFactory.getLogger(RadioReferenceFrequencyIndex.class);

    /**
     * Distance from the user's location that the index covers
     */
    public static final double RADIUS_KILOMETERS = 80.0;

    private static final int FILE_VERSION = 2;
    private static final String FILE_NAME = "frequency-index.json";
    private static final Duration REFRESH_INTERVAL = Duration.ofDays(7);
    private static final Duration AREAS_MAX_AGE = Duration.ofDays(90);
    //The saved index is reused when the user's location moves less than this
    private static final double RELOCATE_KILOMETERS = 10.0;
    //Spacing between web service requests, to go easy on the shared application key
    private static final long REQUEST_INTERVAL_MS = 700;
    private static final int SAVE_EVERY_REQUESTS = 50;
    private static final int MAX_RETRIES = 5;
    private static final Duration FIRST_RETRY_DELAY = Duration.ofSeconds(30);
    private static final Duration MAX_RETRY_DELAY = Duration.ofMinutes(10);

    private final RadioReference mRadioReference;
    private final UserPreferences mUserPreferences;
    private final ReadOnlyStringWrapper mStatus = new ReadOnlyStringWrapper("Not started");
    private final AtomicBoolean mStarted = new AtomicBoolean();
    private final AtomicLong mGeneration = new AtomicLong();
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "sdrtrunk radio reference frequency index");
        thread.setDaemon(true);
        return thread;
    });
    private volatile NavigableMap<Long,List<FrequencyCandidate>> mFrequencies = Collections.emptyNavigableMap();
    private volatile List<SiteRecord> mSites = Collections.emptyList();
    private final List<Runnable> mUpdateListeners = new java.util.concurrent.CopyOnWriteArrayList<>();
    private volatile BuildParameters mParameters;
    private volatile long mRefreshed;
    private Future<?> mBuild;
    private long mLastRequestMillis;
    private int mRequestCount;

    /**
     * What the index is built for
     * @param latitude of the user
     * @param longitude of the user
     */
    record BuildParameters(double latitude, double longitude)
    {
    }

    /**
     * Index file
     * @param version of the file format
     * @param latitude of the user when the index was built
     * @param longitude of the user when the index was built
     * @param radiusKilometers that the index covers
     * @param refreshed time of the last complete build, milliseconds since the epoch, or 0 for a partial build
     * @param areas of the states around the user
     * @param sources of the frequencies
     */
    public record IndexFile(int version, double latitude, double longitude, double radiusKilometers, long refreshed,
                            Areas areas, List<Source> sources)
    {
    }

    /**
     * Radio reference state ids and county areas, which rarely change, kept for months
     * @param stateIdsRefreshed time the state ids were fetched, milliseconds since the epoch
     * @param stateIds radio reference state ids by country and state code, e.g. US-NY
     * @param states county areas of the states around the user
     */
    public record Areas(long stateIdsRefreshed, Map<String,Integer> stateIds, List<StateArea> states)
    {
    }

    /**
     * Counties of a state
     * @param stateId radio reference state id
     * @param name of the state
     * @param refreshed time the counties were fetched, milliseconds since the epoch
     * @param counties of the state with a location
     */
    public record StateArea(int stateId, String name, long refreshed, List<CountyArea> counties)
    {
    }

    /**
     * County center and coverage radius
     */
    public record CountyArea(int countyId, int stateId, String name, double latitude, double longitude,
                             double rangeKilometers)
    {
        double distanceKilometers(double latitude, double longitude)
        {
            return Math.max(0.0, Coverage.distanceKilometers(latitude(), longitude(), latitude, longitude) -
                rangeKilometers());
        }
    }

    /**
     * Frequencies of one county, system or agency
     * @param key such as county:123, system:456 or agency:789
     * @param lastUpdated radio reference update time of the source, milliseconds since the epoch, or 0 when unknown
     * @param candidates of the source within the index radius
     * @param sites of a system source within the index radius, or null for a county or agency source, or a system
     * source saved before the index stored sites
     */
    public record Source(String key, long lastUpdated, List<FrequencyCandidate> candidates, List<SiteRecord> sites)
    {
        boolean isSystem()
        {
            return key.startsWith("system:");
        }
    }

    /**
     * Agency to index, with the location to use for its frequencies when radio reference has none
     * @param agency to index
     * @param stateId of the state or county that lists the agency
     * @param latitude of the county that lists the agency, or null for a statewide agency
     * @param longitude of the county that lists the agency, or null for a statewide agency
     */
    private record AgencyListing(Agency agency, int stateId, Double latitude, Double longitude)
    {
    }

    /**
     * Candidate ranked for a lookup
     * @param candidate frequency
     * @param distanceKilometers from the user to the candidate's location, or NaN when the candidate has no location
     * @param offsetHz between the looked up frequency and the candidate
     */
    public record RankedCandidate(FrequencyCandidate candidate, double distanceKilometers, long offsetHz)
    {
        public boolean hasDistance()
        {
            return !Double.isNaN(distanceKilometers);
        }
    }

    /**
     * Stops the build because it was cancelled or can't continue
     */
    private static class BuildStoppedException extends Exception
    {
        BuildStoppedException(String message)
        {
            super(message);
        }
    }

    /**
     * Constructs an instance
     * @param radioReference service
     * @param userPreferences for the user's location
     */
    public RadioReferenceFrequencyIndex(RadioReference radioReference, UserPreferences userPreferences)
    {
        mRadioReference = radioReference;
        mUserPreferences = userPreferences;
    }

    /**
     * Status for display, such as the number of frequencies or what the index needs to build.  Updated on the JavaFX
     * application thread.
     */
    public ReadOnlyStringProperty statusProperty()
    {
        return mStatus.getReadOnlyProperty();
    }

    /**
     * Starts the index: logs in with the stored credentials if needed, and builds the index whenever the premium
     * account and the user's location allow.  Call on the JavaFX application thread.  Calling again does nothing.
     */
    public void start()
    {
        if(mStarted.compareAndSet(false, true))
        {
            mRadioReference.premiumAccountProperty().addListener((observable, oldValue, newValue) -> update());
            MyEventBus.getGlobalEventBus().register(this);
            //Checks for the weekly refresh while the application runs
            ThreadPool.SCHEDULED.scheduleAtFixedRate(this::update, 6, 6, TimeUnit.HOURS);
            mRadioReference.loginWithStoredCredentials();
            update();
        }
    }

    /**
     * Rebuilds the index when the user's location changes
     */
    @Subscribe
    public void preferenceUpdated(PreferenceType preferenceType)
    {
        if(preferenceType == PreferenceType.RADIO_REFERENCE)
        {
            update();
        }
    }

    /**
     * Starts, stops or refreshes the build to match the account and the user's location
     */
    private synchronized void update()
    {
        RadioReferencePreference.Location location = mUserPreferences.getRadioReferencePreference().getLocation();

        if(!mRadioReference.premiumAccountProperty().get())
        {
            stopBuild();
            setStatus("Requires a premium Radio Reference login");
            return;
        }

        if(location == null)
        {
            stopBuild();
            mFrequencies = Collections.emptyNavigableMap();
            mSites = Collections.emptyList();
            mParameters = null;
            notifyUpdated();
            setStatus("Set your location in the playlist editor's Radio Reference tab");
            return;
        }

        BuildParameters parameters = new BuildParameters(location.latitude(), location.longitude());
        boolean building = mBuild != null && !mBuild.isDone();
        boolean stale = System.currentTimeMillis() - mRefreshed > REFRESH_INTERVAL.toMillis();

        if(parameters.equals(mParameters) && (building || !stale))
        {
            return;
        }

        stopBuild();
        mParameters = parameters;
        long generation = mGeneration.incrementAndGet();
        mBuild = mExecutor.submit(() -> build(generation, parameters));
    }

    private void stopBuild()
    {
        mGeneration.incrementAndGet();

        if(mBuild != null)
        {
            mBuild.cancel(true);
            mBuild = null;
        }
    }

    /**
     * Candidates within the tolerance of the frequency, nearest to the user first, then nearest in frequency.
     * Doesn't call the web service, so it can be called on the JavaFX application thread.
     * @param frequency in Hertz
     * @param toleranceHz maximum difference between the frequency and a candidate
     * @return candidates, or an empty list without a premium account or a user location
     */
    public List<RankedCandidate> lookup(long frequency, long toleranceHz)
    {
        RadioReferencePreference.Location location = mUserPreferences.getRadioReferencePreference().getLocation();

        if(location == null || !mRadioReference.premiumAccountProperty().get())
        {
            return Collections.emptyList();
        }

        return rank(mFrequencies, frequency, toleranceHz, location.latitude(), location.longitude());
    }

    /**
     * Candidates within the tolerance of the frequency, nearest to the location first, then nearest in frequency.
     * Candidates without a location are last.  Duplicate listings of the same frequency and title are removed.
     */
    @VisibleForTesting
    static List<RankedCandidate> rank(NavigableMap<Long,List<FrequencyCandidate>> frequencies, long frequency,
                                      long toleranceHz, double latitude, double longitude)
    {
        List<RankedCandidate> ranked = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for(Map.Entry<Long,List<FrequencyCandidate>> entry:
            frequencies.subMap(frequency - toleranceHz, true, frequency + toleranceHz, true).entrySet())
        {
            for(FrequencyCandidate candidate: entry.getValue())
            {
                if(seen.add(candidate.frequencyHz() + "|" + candidate.title() + "|" + candidate.source()))
                {
                    double distance = candidate.hasLocation() ? Coverage.distanceKilometers(latitude, longitude,
                        candidate.latitude(), candidate.longitude()) : Double.NaN;
                    ranked.add(new RankedCandidate(candidate, distance, Math.abs(entry.getKey() - frequency)));
                }
            }
        }

        ranked.sort(Comparator.comparing(RankedCandidate::hasDistance).reversed()
            .thenComparingDouble(c -> c.hasDistance() ? c.distanceKilometers() : 0.0)
            .thenComparingLong(RankedCandidate::offsetHz));
        return ranked;
    }

    /**
     * Trunked system sites near the user, as the index has them so far.  Can be called from any thread.
     */
    public List<SiteRecord> getSites()
    {
        return mSites;
    }

    /**
     * Adds a listener that's called, on the index thread, each time the index contents change
     */
    public void addUpdateListener(Runnable listener)
    {
        mUpdateListeners.add(listener);
    }

    public void removeUpdateListener(Runnable listener)
    {
        mUpdateListeners.remove(listener);
    }

    private void notifyUpdated()
    {
        for(Runnable listener: mUpdateListeners)
        {
            try
            {
                listener.run();
            }
            catch(RuntimeException e)
            {
                mLog.error("Error notifying radio reference frequency index listener", e);
            }
        }
    }

    /**
     * Number of frequencies in the index
     */
    public int size()
    {
        int count = 0;

        for(List<FrequencyCandidate> candidates: mFrequencies.values())
        {
            count += candidates.size();
        }

        return count;
    }

    /**
     * Builds or refreshes the index.  Runs on the index thread.
     */
    private void build(long generation, BuildParameters parameters)
    {
        mRequestCount = 0;

        try
        {
            Path path = SystemProperties.getInstance().getApplicationFolder("radioreference").resolve(FILE_NAME);
            IndexFile file = read(path);
            boolean sameLocation = file != null && file.radiusKilometers() == RADIUS_KILOMETERS &&
                Coverage.distanceKilometers(file.latitude(), file.longitude(), parameters.latitude(),
                    parameters.longitude()) <= RELOCATE_KILOMETERS;
            Map<String,Source> previous = new LinkedHashMap<>();

            if(sameLocation)
            {
                for(Source source: nonNull(file.sources()))
                {
                    previous.put(source.key(), source);
                }

                //Usable right away, while it's refreshed
                publish(generation, previous.values(), parameters);
                mRefreshed = file.refreshed();
                //An index saved before sites were stored refreshes now, fetching only the system sites
                boolean missingSites = previous.values().stream().anyMatch(this::needsSiteRefresh);

                if(!missingSites && System.currentTimeMillis() - file.refreshed() < REFRESH_INTERVAL.toMillis())
                {
                    setReadyStatus(generation);
                    return;
                }
            }
            else
            {
                mRefreshed = 0;
                publish(generation, Collections.emptyList(), parameters);
            }

            setStatus(generation, "Updating");

            if(refresh(generation, parameters, path, file != null ? file.areas() : null, previous))
            {
                setReadyStatus(generation);
            }
        }
        catch(BuildStoppedException e)
        {
            if(mGeneration.get() == generation)
            {
                mLog.info("Radio reference frequency index stopped - " + e.getMessage());
                setStatus(generation, "Stopped: " + e.getMessage());
            }
        }
        catch(RuntimeException e)
        {
            mLog.error("Error building the radio reference frequency index", e);
            setStatus(generation, "Error: " + e.getMessage());
        }
    }

    /**
     * Fetches the sources near the user, reusing the previous sources that haven't changed, and saves the index.
     * @param previousAreas from the saved index, or null
     * @return true when the index was built, false when there are no counties near the user
     */
    private boolean refresh(long generation, BuildParameters parameters, Path path, Areas previousAreas,
                            Map<String,Source> previous) throws BuildStoppedException
    {
        double latitude = parameters.latitude();
        double longitude = parameters.longitude();
        List<StateBounds> candidateStates = StateBounds.near(latitude, longitude, RADIUS_KILOMETERS);

        if(candidateStates.isEmpty())
        {
            publish(generation, Collections.emptyList(), parameters);
            setStatus(generation, "Your location isn't in the United States or Canada");
            return false;
        }

        Map<Integer,Mode> modes = call(generation, RadioReferenceService::getModesMap);
        Map<Integer,Type> types = call(generation, RadioReferenceService::getTypesMap);
        Map<Integer,Flavor> flavors = call(generation, RadioReferenceService::getFlavorsMap);
        Map<Integer,Voice> voices = call(generation, RadioReferenceService::getVoicesMap);

        //Radio reference ids of the states around the user
        Map<String,Integer> stateIds = new HashMap<>();
        long stateIdsRefreshed = 0;

        if(previousAreas != null && previousAreas.stateIds() != null &&
            System.currentTimeMillis() - previousAreas.stateIdsRefreshed() < AREAS_MAX_AGE.toMillis())
        {
            stateIds.putAll(previousAreas.stateIds());
            stateIdsRefreshed = previousAreas.stateIdsRefreshed();
        }

        if(!hasStateIds(stateIds, candidateStates))
        {
            stateIds.putAll(fetchStateIds(generation, candidateStates));
            stateIdsRefreshed = System.currentTimeMillis();
        }

        //County areas of each state, kept for months
        Map<Integer,StateArea> previousStates = new HashMap<>();

        if(previousAreas != null)
        {
            for(StateArea state: nonNull(previousAreas.states()))
            {
                previousStates.put(state.stateId(), state);
            }
        }

        List<StateArea> states = new ArrayList<>();
        Map<Integer,StateInfo> stateInfos = new HashMap<>();
        Map<Integer,CountyInfo> countyInfos = new HashMap<>();

        for(StateBounds bounds: candidateStates)
        {
            Integer stateId = stateIds.get(bounds.key());

            if(stateId == null)
            {
                mLog.debug("Radio Reference has no state for " + bounds.key());
                continue;
            }

            StateArea state = previousStates.get(stateId);

            if(state == null || System.currentTimeMillis() - state.refreshed() > AREAS_MAX_AGE.toMillis())
            {
                //The state's county list.  States with counties near the user are fetched again for their
                //statewide systems and agencies only when they aren't fetched here.
                StateInfo stateInfo = call(generation, service -> service.getStateInfo(stateId));

                if(stateInfo == null)
                {
                    continue;
                }

                stateInfos.put(stateId, stateInfo);
                state = fetchCountyAreas(generation, stateInfo, countyInfos);
            }

            states.add(state);
        }

        Areas areas = new Areas(stateIdsRefreshed, stateIds, states);
        List<CountyArea> countyAreas = new ArrayList<>();

        Map<Integer,String> countyNames = new HashMap<>();

        for(StateArea state: states)
        {
            countyAreas.addAll(nonNull(state.counties()));

            for(CountyArea county: nonNull(state.counties()))
            {
                countyNames.put(county.countyId(), countyName(county.name()));
            }
        }

        List<CountyArea> nearby = nearby(countyAreas, parameters);

        if(nearby.isEmpty())
        {
            publish(generation, Collections.emptyList(), parameters);
            write(path, new IndexFile(FILE_VERSION, latitude, longitude, RADIUS_KILOMETERS, 0, areas,
                Collections.emptyList()));
            setStatus(generation, "No Radio Reference counties within " + (int)RADIUS_KILOMETERS +
                " km of your location");
            return false;
        }

        //The state with the nearest county.  Statewide agencies without a location are only indexed for this state.
        int homeStateId = nearby.get(0).stateId();
        Map<String,Source> sources = new LinkedHashMap<>();
        //Systems and agencies in the order they're found: nearest counties first, then statewide
        Map<Integer,io.github.dsheirer.rrapi.type.System> systems = new LinkedHashMap<>();
        Map<Integer,AgencyListing> agencies = new LinkedHashMap<>();
        Set<Integer> nearbyStates = new java.util.LinkedHashSet<>();
        int saveCount = mRequestCount;

        for(CountyArea area: nearby)
        {
            nearbyStates.add(area.stateId());
            CountyInfo countyInfo = countyInfos.get(area.countyId());

            if(countyInfo == null)
            {
                countyInfo = call(generation, service -> service.getCountyInfo(area.countyId()));
            }

            if(countyInfo == null)
            {
                continue;
            }

            String key = "county:" + area.countyId();
            long updated = time(countyInfo.getLastUpdated());
            Source source = previous.get(key);

            if(source == null || updated == 0 || source.lastUpdated() != updated)
            {
                source = new Source(key, updated, countyCandidates(generation, countyInfo, area, modes, parameters),
                    null);
            }

            sources.put(key, source);

            for(io.github.dsheirer.rrapi.type.System system: nonNull(countyInfo.getSystems()))
            {
                systems.putIfAbsent(system.getSystemId(), system);
            }

            for(Agency agency: nonNull(countyInfo.getAgencies()))
            {
                agencies.putIfAbsent(agency.getAgencyId(), new AgencyListing(agency, area.stateId(), area.latitude(),
                    area.longitude()));
            }

            progress(generation, parameters, path, areas, sources, previous, saveCount);
            saveCount = savePoint(saveCount);
        }

        //Statewide systems and agencies of the states with counties near the user
        for(Integer stateId: nearbyStates)
        {
            StateInfo stateInfo = stateInfos.get(stateId);

            if(stateInfo == null)
            {
                stateInfo = call(generation, service -> service.getStateInfo(stateId));
            }

            if(stateInfo == null)
            {
                continue;
            }

            for(io.github.dsheirer.rrapi.type.System system: nonNull(stateInfo.getSystems()))
            {
                systems.putIfAbsent(system.getSystemId(), system);
            }

            for(Agency agency: nonNull(stateInfo.getAgencies()))
            {
                agencies.putIfAbsent(agency.getAgencyId(), new AgencyListing(agency, stateId, null, null));
            }
        }

        for(io.github.dsheirer.rrapi.type.System system: systems.values())
        {
            String key = "system:" + system.getSystemId();
            long updated = time(system.getLastUpdated());
            Source source = previous.get(key);

            if(source == null || updated == 0 || source.lastUpdated() != updated || needsSiteRefresh(source))
            {
                List<Site> sites = call(generation, service -> service.getSites(system.getSystemId()));
                String protocol = protocol(system, types, flavors);
                Voice voice = voices != null ? voices.get(system.getVoiceId()) : null;
                source = new Source(key, updated, siteCandidates(sites, system.getSystemId(), system.getName(),
                    protocol, parameters),
                    siteRecords(sites, system, protocol, voice != null ? blankToNull(voice.getName()) : null,
                        countyNames, parameters));
            }

            sources.put(key, source);
            progress(generation, parameters, path, areas, sources, previous, saveCount);
            saveCount = savePoint(saveCount);
        }

        for(AgencyListing listing: agencies.values())
        {
            int agencyId = listing.agency().getAgencyId();
            AgencyInfo agencyInfo = call(generation, service -> service.getAgencyInfo(agencyId));

            if(agencyInfo == null)
            {
                continue;
            }

            String key = "agency:" + agencyId;
            long updated = time(agencyInfo.getLastUpdated());
            Source source = previous.get(key);

            if(source == null || updated == 0 || source.lastUpdated() != updated)
            {
                source = new Source(key, updated, agencyCandidates(generation, agencyInfo, listing, homeStateId,
                    modes, parameters), null);
            }

            sources.put(key, source);
            progress(generation, parameters, path, areas, sources, previous, saveCount);
            saveCount = savePoint(saveCount);
        }

        //Complete: sources that weren't found again are dropped
        long refreshed = System.currentTimeMillis();
        publish(generation, sources.values(), parameters);
        mRefreshed = refreshed;
        write(path, new IndexFile(FILE_VERSION, latitude, longitude, RADIUS_KILOMETERS, refreshed, areas,
            new ArrayList<>(sources.values())));
        mLog.info("Radio reference frequency index updated with " + mRequestCount + " requests - " + size() +
            " frequencies");
        return true;
    }

    private static boolean hasStateIds(Map<String,Integer> stateIds, List<StateBounds> states)
    {
        for(StateBounds state: states)
        {
            if(!stateIds.containsKey(state.key()))
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Radio reference state ids of the countries of the states, by country and state code, e.g. US-NY
     */
    private Map<String,Integer> fetchStateIds(long generation, List<StateBounds> states) throws BuildStoppedException
    {
        Map<String,Integer> stateIds = new HashMap<>();
        Set<String> countryCodes = new java.util.LinkedHashSet<>();

        for(StateBounds state: states)
        {
            countryCodes.add(state.countryCode());
        }

        List<io.github.dsheirer.rrapi.type.Country> countries = call(generation, RadioReferenceService::getCountries);

        for(String countryCode: countryCodes)
        {
            io.github.dsheirer.rrapi.type.Country country = findCountry(countries, countryCode);

            if(country == null)
            {
                mLog.warn("Radio Reference has no country for code " + countryCode);
                continue;
            }

            io.github.dsheirer.rrapi.type.CountryInfo countryInfo = call(generation,
                service -> service.getCountryInfo(country.getCountryId()));

            if(countryInfo != null)
            {
                for(io.github.dsheirer.rrapi.type.State state: nonNull(countryInfo.getStates()))
                {
                    if(state.getStateCode() != null)
                    {
                        stateIds.put(countryCode + "-" + state.getStateCode().trim().toUpperCase(), state.getStateId());
                    }
                }
            }
        }

        return stateIds;
    }

    /**
     * Country with the ISO code, matched by code or, failing that, by name
     */
    private static io.github.dsheirer.rrapi.type.Country findCountry(List<io.github.dsheirer.rrapi.type.Country> countries,
                                                                     String countryCode)
    {
        String name = switch(countryCode)
        {
            case "US" -> "united states";
            case "CA" -> "canada";
            default -> countryCode.toLowerCase();
        };

        for(io.github.dsheirer.rrapi.type.Country country: nonNull(countries))
        {
            if(countryCode.equalsIgnoreCase(country.getCountryCode()))
            {
                return country;
            }
        }

        for(io.github.dsheirer.rrapi.type.Country country: nonNull(countries))
        {
            if(country.getName() != null && country.getName().toLowerCase().startsWith(name))
            {
                return country;
            }
        }

        return null;
    }

    /**
     * Center and range of every county in the state
     * @param countyInfos receives the fetched county information, so it isn't fetched twice
     */
    private StateArea fetchCountyAreas(long generation, StateInfo stateInfo, Map<Integer,CountyInfo> countyInfos)
        throws BuildStoppedException
    {
        List<CountyArea> counties = new ArrayList<>();
        List<County> list = nonNull(stateInfo.getCounties());
        int count = 0;

        for(County county: list)
        {
            CountyInfo countyInfo = call(generation, service -> service.getCountyInfo(county.getCountyId()));
            setStatus(generation, "Updating: finding " + stateInfo.getName() + " counties, " + ++count + " of " +
                list.size());

            if(countyInfo != null)
            {
                countyInfos.put(county.getCountyId(), countyInfo);
                Coverage coverage = countyInfo.getCoverage();

                if(coverage.hasLocation())
                {
                    counties.add(new CountyArea(county.getCountyId(), stateInfo.getStateId(), countyInfo.getName(),
                        coverage.latitude(), coverage.longitude(), coverage.rangeKilometers()));
                }
            }
        }

        return new StateArea(stateInfo.getStateId(), stateInfo.getName(), System.currentTimeMillis(), counties);
    }

    /**
     * Counties within the index radius of the user, nearest first
     */
    private static List<CountyArea> nearby(List<CountyArea> countyAreas, BuildParameters parameters)
    {
        List<CountyArea> nearby = new ArrayList<>();

        for(CountyArea area: nonNull(countyAreas))
        {
            if(area.distanceKilometers(parameters.latitude(), parameters.longitude()) <= RADIUS_KILOMETERS)
            {
                nearby.add(area);
            }
        }

        nearby.sort(Comparator.comparingDouble(area -> area.distanceKilometers(parameters.latitude(),
            parameters.longitude())));
        return nearby;
    }

    /**
     * Publishes the sources found so far, together with the previous sources not yet refreshed, and saves them as a
     * partial index every few requests so a restart doesn't repeat them.
     */
    private void progress(long generation, BuildParameters parameters, Path path, Areas areas,
                          Map<String,Source> sources, Map<String,Source> previous, int saveCount)
    {
        Map<String,Source> combined = new LinkedHashMap<>(previous);
        combined.putAll(sources);
        publish(generation, combined.values(), parameters);
        setStatus(generation, "Updating: " + NumberFormat.getIntegerInstance().format(size()) + " frequencies, " +
            mRequestCount + " requests");

        if(mRequestCount - saveCount >= SAVE_EVERY_REQUESTS)
        {
            //Not complete, so the next start refreshes again, reusing these sources
            write(path, new IndexFile(FILE_VERSION, parameters.latitude(), parameters.longitude(), RADIUS_KILOMETERS,
                0, areas, new ArrayList<>(combined.values())));
        }
    }

    private int savePoint(int saveCount)
    {
        return mRequestCount - saveCount >= SAVE_EVERY_REQUESTS ? mRequestCount : saveCount;
    }

    /**
     * Conventional frequencies of the county's subcategories within the index radius.  Subcategories without a
     * location use the county center.
     */
    private List<FrequencyCandidate> countyCandidates(long generation, CountyInfo countyInfo, CountyArea area,
                                                      Map<Integer,Mode> modes, BuildParameters parameters)
        throws BuildStoppedException
    {
        String county = countyName(countyInfo.getName());
        return categoryCandidates(generation, countyInfo.getCategories(), county, area.latitude(), area.longitude(),
            modes, parameters);
    }

    /**
     * Conventional frequencies of the agency's subcategories within the index radius, or none when the agency's
     * coverage is outside the radius.  Subcategories without a location use the agency's location, or else the
     * location of the county that lists the agency.  A statewide agency without a location is only indexed for the
     * user's home state, since its frequencies could be anywhere in the state.
     */
    private List<FrequencyCandidate> agencyCandidates(long generation, AgencyInfo agencyInfo, AgencyListing listing,
                                                      int homeStateId, Map<Integer,Mode> modes,
                                                      BuildParameters parameters) throws BuildStoppedException
    {
        Coverage coverage = agencyInfo.getCoverage();
        Double latitude = listing.latitude();
        Double longitude = listing.longitude();

        if(coverage.hasLocation())
        {
            if(coverage.distanceToCoverageKilometers(parameters.latitude(), parameters.longitude()) > RADIUS_KILOMETERS)
            {
                return Collections.emptyList();
            }

            latitude = coverage.latitude();
            longitude = coverage.longitude();
        }
        else if(latitude == null && listing.stateId() != homeStateId)
        {
            return Collections.emptyList();
        }

        return categoryCandidates(generation, agencyInfo.getCategories(), agencyInfo.getName(), latitude, longitude,
            modes, parameters);
    }

    private List<FrequencyCandidate> categoryCandidates(long generation, List<Category> categories, String owner,
                                                        Double defaultLatitude, Double defaultLongitude,
                                                        Map<Integer,Mode> modes, BuildParameters parameters)
        throws BuildStoppedException
    {
        List<FrequencyCandidate> candidates = new ArrayList<>();

        for(Category category: nonNull(categories))
        {
            for(SubCategory subCategory: nonNull(category.getSubCategories()))
            {
                Coverage coverage = subCategory.getCoverage();
                Double latitude = defaultLatitude;
                Double longitude = defaultLongitude;

                if(coverage.hasLocation())
                {
                    if(coverage.distanceToCoverageKilometers(parameters.latitude(), parameters.longitude()) >
                        RADIUS_KILOMETERS)
                    {
                        continue;
                    }

                    latitude = coverage.latitude();
                    longitude = coverage.longitude();
                }

                List<Frequency> frequencies = call(generation,
                    service -> service.getSubCategoryFrequencies(subCategory.getSubCategoryId()));
                String source = join(" › ", owner, category.getName(), subCategory.getName());

                for(Frequency frequency: nonNull(frequencies))
                {
                    long hz = frequency.getDownlinkHz();

                    if(hz > 0)
                    {
                        candidates.add(new FrequencyCandidate(hz,
                            firstNonBlank(frequency.getDescription(), frequency.getAlphaTag(), subCategory.getName()),
                            blankToNull(frequency.getAlphaTag()), modeName(frequency.getMode(), modes),
                            blankToNull(frequency.getTone()), null, frequency.getEncryption() > 0, source, latitude,
                            longitude, null, null));
                    }
                }
            }
        }

        return candidates;
    }

    /**
     * Frequencies of the system's sites within the index radius
     */
    @VisibleForTesting
    static List<FrequencyCandidate> siteCandidates(List<Site> sites, int systemId, String systemName,
                                                   String systemType, BuildParameters parameters)
    {
        List<FrequencyCandidate> candidates = new ArrayList<>();

        for(Site site: nonNull(sites))
        {
            Coverage coverage = site.getCoverage();

            if(!coverage.hasLocation() ||
                coverage.distanceToCoverageKilometers(parameters.latitude(), parameters.longitude()) > RADIUS_KILOMETERS)
            {
                continue;
            }

            String source = join(" · ", "Site " + site.getSiteNumber(), blankToNull(site.getDescription()));

            for(SiteFrequency siteFrequency: nonNull(site.getSiteFrequencies()))
            {
                long hz = siteFrequency.getFrequencyHz();

                if(hz > 0)
                {
                    candidates.add(new FrequencyCandidate(hz, systemName, null, systemType, null,
                        siteFrequencyUse(siteFrequency.getUse()), false, source, coverage.latitude(),
                        coverage.longitude(), systemId, site.getSiteId()));
                }
            }
        }

        return candidates;
    }

    /**
     * Sites of the system within the index radius
     */
    @VisibleForTesting
    static List<SiteRecord> siteRecords(List<Site> sites, io.github.dsheirer.rrapi.type.System system,
                                        String protocol, String voice, Map<Integer,String> countyNames,
                                        BuildParameters parameters)
    {
        List<SiteRecord> records = new ArrayList<>();

        for(Site site: nonNull(sites))
        {
            Coverage coverage = site.getCoverage();

            if(!coverage.hasLocation() ||
                coverage.distanceToCoverageKilometers(parameters.latitude(), parameters.longitude()) > RADIUS_KILOMETERS)
            {
                continue;
            }

            List<SiteRecord.SiteChannel> channels = new ArrayList<>();

            for(SiteFrequency siteFrequency: nonNull(site.getSiteFrequencies()))
            {
                long hz = siteFrequency.getFrequencyHz();

                if(hz > 0)
                {
                    channels.add(new SiteRecord.SiteChannel(hz, siteFrequency.getLogicalChannelNumber(),
                        siteFrequencyUse(siteFrequency.getUse())));
                }
            }

            channels.sort(Comparator.comparingLong(SiteRecord.SiteChannel::frequencyHz));
            records.add(new SiteRecord(system.getSystemId(), system.getName(), protocol, voice, site.getSiteId(),
                site.getSiteNumber(), blankToNull(site.getDescription()), countyNames.get(site.getCountyId()),
                coverage.latitude(), coverage.longitude(), coverage.rangeKilometers(), blankToNull(site.getNac()),
                site.getRfss(), site.getZoneNumber(), channels));
        }

        return records;
    }

    /**
     * Indicates if a saved system source predates the site records or the site ids of its frequencies, so it's
     * fetched again
     */
    private boolean needsSiteRefresh(Source source)
    {
        return source.isSystem() && (source.sites() == null ||
            nonNull(source.candidates()).stream().anyMatch(candidate -> !candidate.isSiteFrequency()));
    }

    /**
     * Site record by system and site id
     * @return site, or null when the index doesn't have it
     */
    public SiteRecord getSite(int systemId, int siteId)
    {
        for(SiteRecord site: mSites)
        {
            if(site.systemId() == systemId && site.siteId() == siteId)
            {
                return site;
            }
        }

        return null;
    }

    /**
     * System protocol: the radio reference system type and flavor, e.g. Project 25 Phase II
     */
    private static String protocol(io.github.dsheirer.rrapi.type.System system, Map<Integer,Type> types,
                                   Map<Integer,Flavor> flavors)
    {
        Type type = types != null ? types.get(system.getTypeId()) : null;
        Flavor flavor = flavors != null ? flavors.get(system.getFlavorId()) : null;
        String typeName = type != null ? blankToNull(type.getName()) : null;
        String flavorName = flavor != null ? blankToNull(flavor.getName()) : null;

        if(typeName != null && flavorName != null && !typeName.toLowerCase().contains(flavorName.toLowerCase()))
        {
            return typeName + " " + flavorName;
        }

        return typeName != null ? typeName : flavorName;
    }

    /**
     * Description of a site frequency use code.  Radio reference marks control channels with c or d and alternate
     * control channels with a; other frequencies are voice channels.
     */
    @VisibleForTesting
    static String siteFrequencyUse(String use)
    {
        if(use == null || use.isBlank())
        {
            return null;
        }

        String code = use.trim().toLowerCase();

        if(code.startsWith("c") || code.startsWith("d"))
        {
            return "Control";
        }
        else if(code.startsWith("a"))
        {
            return "Alternate control";
        }

        return null;
    }

    private static String modeName(String mode, Map<Integer,Mode> modes)
    {
        if(mode == null || mode.isBlank())
        {
            return null;
        }

        try
        {
            Mode value = modes != null ? modes.get(Integer.parseInt(mode.trim())) : null;
            return value != null ? value.getName() : null;
        }
        catch(NumberFormatException e)
        {
            return mode.trim();
        }
    }

    /**
     * County name for display: radio reference names counties without the word county
     */
    private static String countyName(String name)
    {
        return name == null ? null : name.toLowerCase().endsWith("county") ? name : name + " County";
    }

    /**
     * Calls the web service, spacing out the requests.  Waits and tries again after errors that are usually
     * temporary.
     * @return result, or null when the request fails with an error that retrying won't fix, e.g. an invalid id
     * @throws BuildStoppedException when the build is cancelled or the account can't use the service
     */
    private <T> T call(long generation, RadioReferenceService.ServiceCall<T> serviceCall) throws BuildStoppedException
    {
        int attempt = 0;

        while(true)
        {
            checkCancelled(generation);
            sleep(generation, mLastRequestMillis + REQUEST_INTERVAL_MS - System.currentTimeMillis());
            mLastRequestMillis = System.currentTimeMillis();
            mRequestCount++;

            try
            {
                return serviceCall.call(mRadioReference.getService());
            }
            catch(RadioReferenceException rre)
            {
                switch(rre.getErrorType())
                {
                    case AUTHENTICATION, SUBSCRIPTION, APPLICATION_KEY ->
                        throw new BuildStoppedException(rre.getMessage());
                    case INTERRUPTED -> throw new BuildStoppedException("cancelled");
                    default ->
                    {
                        if(!rre.isRetryable() || ++attempt > MAX_RETRIES)
                        {
                            mLog.warn("Radio reference frequency index request failed - " + rre.getMessage());
                            return null;
                        }

                        Duration delay = rre.getRetryAfter().orElse(FIRST_RETRY_DELAY.multipliedBy(1L << (attempt - 1)));

                        if(delay.compareTo(MAX_RETRY_DELAY) > 0)
                        {
                            delay = MAX_RETRY_DELAY;
                        }

                        setStatus(generation, "Waiting to retry in " + delay.toSeconds() + " seconds: " +
                            rre.getMessage());
                        sleep(generation, delay.toMillis());
                    }
                }
            }
        }
    }

    private void checkCancelled(long generation) throws BuildStoppedException
    {
        if(mGeneration.get() != generation || Thread.currentThread().isInterrupted())
        {
            throw new BuildStoppedException("cancelled");
        }
    }

    private void sleep(long generation, long millis) throws BuildStoppedException
    {
        if(millis > 0)
        {
            try
            {
                Thread.sleep(millis);
            }
            catch(InterruptedException e)
            {
                Thread.currentThread().interrupt();
                throw new BuildStoppedException("cancelled");
            }
        }

        checkCancelled(generation);
    }

    /**
     * Makes the sources the current index, unless the build was cancelled
     */
    private void publish(long generation, Iterable<Source> sources, BuildParameters parameters)
    {
        if(mGeneration.get() == generation && parameters.equals(mParameters))
        {
            mFrequencies = toFrequencies(sources);
            List<SiteRecord> sites = new ArrayList<>();

            for(Source source: sources)
            {
                sites.addAll(nonNull(source.sites()));
            }

            mSites = Collections.unmodifiableList(sites);
            notifyUpdated();
        }
    }

    @VisibleForTesting
    static NavigableMap<Long,List<FrequencyCandidate>> toFrequencies(Iterable<Source> sources)
    {
        TreeMap<Long,List<FrequencyCandidate>> frequencies = new TreeMap<>();

        for(Source source: sources)
        {
            for(FrequencyCandidate candidate: nonNull(source.candidates()))
            {
                frequencies.computeIfAbsent(candidate.frequencyHz(), frequency -> new ArrayList<>()).add(candidate);
            }
        }

        return Collections.unmodifiableNavigableMap(frequencies);
    }

    private IndexFile read(Path path)
    {
        if(Files.exists(path))
        {
            try
            {
                IndexFile file = RadioReferenceJson.read(path, IndexFile.class);

                if(file != null && file.version() == FILE_VERSION && file.sources() != null)
                {
                    return file;
                }
            }
            catch(Exception e)
            {
                mLog.warn("Ignoring unreadable radio reference frequency index [" + path + "] - " + e.getMessage());
            }
        }

        return null;
    }

    private void write(Path path, IndexFile file)
    {
        try
        {
            RadioReferenceJson.write(path, file);
        }
        catch(Exception e)
        {
            mLog.warn("Unable to save the radio reference frequency index [" + path + "] - " + e.getMessage());
        }
    }

    private void setReadyStatus(long generation)
    {
        setStatus(generation, NumberFormat.getIntegerInstance().format(size()) + " frequencies within " +
            (int)RADIUS_KILOMETERS + " km");
    }

    private void setStatus(long generation, String status)
    {
        if(mGeneration.get() == generation)
        {
            setStatus(status);
        }
    }

    private void setStatus(String status)
    {
        if(Platform.isFxApplicationThread())
        {
            mStatus.set(status);
        }
        else
        {
            try
            {
                Platform.runLater(() -> mStatus.set(status));
            }
            catch(IllegalStateException e)
            {
                //JavaFX isn't running
                mStatus.set(status);
            }
        }
    }

    private static long time(Date date)
    {
        return date != null ? date.getTime() : 0;
    }

    private static <T> List<T> nonNull(List<T> list)
    {
        return list != null ? list : Collections.emptyList();
    }

    private static String blankToNull(String text)
    {
        return text == null || text.isBlank() ? null : text.trim();
    }

    private static String firstNonBlank(String... values)
    {
        for(String value: values)
        {
            if(value != null && !value.isBlank())
            {
                return value.trim();
            }
        }

        return null;
    }

    private static String join(String separator, String... values)
    {
        StringBuilder sb = new StringBuilder();

        for(String value: values)
        {
            if(value != null && !value.isBlank())
            {
                if(!sb.isEmpty())
                {
                    sb.append(separator);
                }

                sb.append(value.trim());
            }
        }

        return sb.isEmpty() ? null : sb.toString();
    }
}
