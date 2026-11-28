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

package io.github.dsheirer.vector.calibrate;

import io.github.dsheirer.settings.Setting;
import io.github.dsheirer.settings.Settings;
import io.github.dsheirer.settings.calibration.VectorCalibrationSettings;
import io.github.dsheirer.vector.calibrate.channelizer.ChannelDFTCalibration;
import io.github.dsheirer.vector.calibrate.demodulator.FmDemodulatorCalibration;
import io.github.dsheirer.vector.calibrate.filter.FirFilterCalibration;
import io.github.dsheirer.vector.calibrate.filter.RealHalfBandFilterCalibration;
import io.github.dsheirer.vector.calibrate.gain.ComplexGainCalibration;
import io.github.dsheirer.vector.calibrate.mixer.ComplexMixerCalibration;
import io.github.dsheirer.vector.calibrate.oscillator.ComplexOscillatorCalibration;
import io.github.dsheirer.vector.calibrate.oscillator.RealOscillatorCalibration;
import io.github.dsheirer.vector.calibrate.sync.DMRSoftSyncCalibration;
import io.github.dsheirer.vector.calibrate.sync.NXDNSoftSyncCalibration;
import io.github.dsheirer.vector.calibrate.sync.P25P1SoftSyncCalibration;
import io.github.dsheirer.vector.calibrate.sync.P25P2SoftSyncCalibration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;
import jdk.incubator.vector.FloatVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Determines the optimal (scalar vs vector) class to use for the current CPU architecture.
 */
public class CalibrationManager
{
    private static final Logger mLog = LoggerFactory.getLogger(CalibrationManager.class);
    private Map<CalibrationType, Calibration> mCalibrationMap = new HashMap<>();
    private final Map<CalibrationType,Implementation> mImplementationOverrides = new ConcurrentHashMap<>();
    private static CalibrationManager sInstance;
    private static VectorCalibrationSettings sVectorCalibrationSettings;

    /**
     * Uses the singleton pattern to construct a single instance.
     */
    private CalibrationManager()
    {
    }

    /**
     * Access a singleton instance of this class, initializing the instance with the
     * specified User Preferences instance, if it hasn't already been intitialized.
     *
     * Note: invoke this method first with a preferences instance to ensure it is the one that is used.
     */
    public static synchronized CalibrationManager getInstance(Settings settings)
    {
        if(sVectorCalibrationSettings == null)
        {
            sVectorCalibrationSettings = settings.getVectorCalibrationSettings();
        }

        return getInstance();
    }

    /**
     * Access a singleton instance of this class.
     */
    public static synchronized CalibrationManager getInstance()
    {
        if(sInstance == null)
        {
            if(sVectorCalibrationSettings == null)
            {
                sVectorCalibrationSettings = new Settings().getVectorCalibrationSettings();
            }

            CalibrationManager manager = new CalibrationManager();

            for(Calibration calibration : createCalibrations())
            {
                manager.add(calibration);
            }

            manager.removeStaleSettings();
            manager.checkEnvironment();
            manager.logStatus();
            sInstance = manager;
        }

        return sInstance;
    }

    /**
     * Removes stored calibration results that don't belong to a current calibration type and version, such as results
     * from removed calibrations and from earlier versions of a calibration.
     */
    private void removeStaleSettings()
    {
        Set<String> current = new HashSet<>();

        for(CalibrationType type : mCalibrationMap.keySet())
        {
            current.add(type.getPreferenceKey());
        }

        for(Setting setting : List.of(Setting.VECTOR_CALIBRATION, Setting.VECTOR_CALIBRATION_RESULT))
        {
            try
            {
                Preferences node = setting.getNode();

                for(String key : node.keys())
                {
                    if(!current.contains(key))
                    {
                        node.remove(key);
                        mLog.info("Removed stale calibration setting [" + key + "]");
                    }
                }
            }
            catch(BackingStoreException | IllegalStateException e)
            {
                mLog.warn("Unable to remove stale calibration settings", e);
            }
        }
    }

    /**
     * Resets all calibrations when the computing environment (CPU, vector width, JDK or operating system) differs from
     * the environment of the stored calibrations, since the optimal implementations can differ.  When no environment
     * is stored, such as after upgrading from a version that didn't store it, the current environment is stored and
     * the existing calibrations are kept.
     */
    private void checkEnvironment()
    {
        String current = getEnvironment();
        String stored = Setting.VECTOR_CALIBRATION_ENVIRONMENT.get(null);

        if(stored != null && !stored.equals(current))
        {
            mLog.info("Computing environment changed from [" + stored + "] to [" + current +
                "] - resetting calibrations");
            reset();
        }

        Setting.VECTOR_CALIBRATION_ENVIRONMENT.put(current);
    }

    /**
     * Description of the computing environment that calibrations depend on.
     */
    public static String getEnvironment()
    {
        StringBuilder sb = new StringBuilder();
        String cpu = getCpuName();

        if(!cpu.isEmpty())
        {
            sb.append(cpu).append(", ");
        }

        sb.append(System.getProperty("os.name")).append(" ").append(System.getProperty("os.arch"));
        sb.append(", Java ").append(Runtime.version().feature());
        sb.append(", ").append(FloatVector.SPECIES_PREFERRED.vectorBitSize()).append("-bit vectors");
        return sb.toString();
    }

    /**
     * CPU model name, where the operating system provides it without running a program.
     * @return name or an empty string
     */
    private static String getCpuName()
    {
        String identifier = System.getenv("PROCESSOR_IDENTIFIER"); //Windows

        if(identifier != null && !identifier.isBlank())
        {
            return identifier.trim();
        }

        Path cpuinfo = Path.of("/proc/cpuinfo"); //Linux

        if(Files.isReadable(cpuinfo))
        {
            try(Stream<String> lines = Files.lines(cpuinfo))
            {
                Optional<String> model = lines.filter(line -> line.startsWith("model name") || line.startsWith("Model"))
                    .map(line -> line.substring(line.indexOf(':') + 1).trim())
                    .filter(name -> !name.isEmpty())
                    .findFirst();

                if(model.isPresent())
                {
                    return model.get();
                }
            }
            catch(IOException | UncheckedIOException e)
            {
                //CPU name is optional
            }
        }

        return "";
    }

    /**
     * One line summary of each calibration's selected implementation and its speedup over scalar.
     */
    public String getSummary()
    {
        StringBuilder sb = new StringBuilder("Vector calibrations for [").append(getEnvironment()).append("]:");

        for(CalibrationType type : getCalibrationTypes().stream().sorted().toList())
        {
            Calibration calibration = getCalibration(type);
            sb.append(" ").append(type.name()).append("=").append(calibration.getImplementation());
            calibration.getResult().ifPresent(result ->
                sb.append(String.format(Locale.ROOT, " (%.2fx)", result.getSelectedSpeedup())));
        }

        return sb.toString();
    }

    /**
     * Creates an instance of each calibration used by the system.
     */
    public static List<Calibration> createCalibrations()
    {
        return List.of(
            new ChannelDFTCalibration(),
            new ComplexGainCalibration(),
            new ComplexOscillatorCalibration(),
            new ComplexMixerCalibration(),
            new DMRSoftSyncCalibration(),
            new FirFilterCalibration(),
            new FmDemodulatorCalibration(),
            new NXDNSoftSyncCalibration(),
            new P25P1SoftSyncCalibration(),
            new P25P2SoftSyncCalibration(),
            RealHalfBandFilterCalibration.tap11(),
            RealHalfBandFilterCalibration.tap15(),
            RealHalfBandFilterCalibration.tap23(),
            RealHalfBandFilterCalibration.tap63(),
            RealHalfBandFilterCalibration.defaultLength(),
            new RealOscillatorCalibration());
    }

    /**
     * Adds the calibration to the map of calibrations for this manager.
      * @param calibration to add
     */
    private void add(Calibration calibration)
    {
        if(mCalibrationMap.containsKey(calibration.getType()))
        {
            throw new IllegalStateException("Calibration Type [" + calibration.getType() +
                    "] is already registered for [" + mCalibrationMap.get(calibration.getType()).getClass());
        }

        mCalibrationMap.put(calibration.getType(), calibration);
    }

    /**
     * List of calibration types available
     */
    public List<CalibrationType> getCalibrationTypes()
    {
        return List.copyOf(mCalibrationMap.keySet());
    }

    /**
     * Access a calibration by type
     * @param type of calibration
     * @return calibration instance, or null.
     */
    public Calibration getCalibration(CalibrationType type)
    {
        return mCalibrationMap.get(type);
    }

    /**
     * Identifies the optimal operation for the calibration type.
     * @param type of calibration
     * @return operation
     */
    public Implementation getImplementation(CalibrationType type)
    {
        Implementation override = mImplementationOverrides.get(type);

        if(override != null)
        {
            return override;
        }

        if(sVectorCalibrationSettings.isVectorEnabled())
        {
            Calibration calibration = getCalibration(type);

            if(calibration != null)
            {
                return calibration.getImplementation();
            }

            return Implementation.UNCALIBRATED;
        }

        return Implementation.SCALAR;
    }

    /**
     * Overrides the implementation that the factories create for the calibration type, regardless of the stored
     * calibration and the vector setting.  This lets tests exercise a specific implementation through the application
     * code paths.  The override isn't stored.
     * @param type of calibration
     * @param implementation to use, or null to remove the override
     */
    public void setImplementationOverride(CalibrationType type, Implementation implementation)
    {
        if(implementation == null)
        {
            mImplementationOverrides.remove(type);
        }
        else
        {
            mImplementationOverrides.put(type, implementation);
        }
    }

    /**
     * Indicates if all calibrations are calibrated.
     * @return true if all calibrations are calibrated or if there are no calibrations registered.
     */
    public boolean isCalibrated()
    {
        for(Calibration calibration: mCalibrationMap.values())
        {
            if(!calibration.isCalibrated())
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Resets all calibrations to an uncalibrated state.
     */
    public void reset()
    {
        for(Calibration calibration: mCalibrationMap.values())
        {
            calibration.reset();
        }
    }

    /**
     * Resets a specific calibration type.
     */
    public void reset(CalibrationType type)
    {
        Calibration calibration = mCalibrationMap.get(type);

        if(calibration != null)
        {
            calibration.reset();
        }
    }

    /**
     * Calibrates each uncalibrated calibration.  A calibration that fails is logged and left uncalibrated, so it uses
     * the scalar implementation, and the remaining calibrations continue.  Logs the summary at debug level when finished.
     */
    public void calibrate()
    {
        List<Calibration> uncalibrated = getUncalibrated();

        if(uncalibrated.isEmpty())
        {
            mLog.info("No additional calibrations are required at this time.");
            return;
        }

        mLog.info("Calibrating software for optimal performance on this computer.");
        mLog.info("*** Please be patient, this may take a few minutes ***");

        int calibrationCounter = 0;
        int failed = 0;

        for(Calibration calibration: uncalibrated)
        {
            mLog.info("Calibrating [" + ++calibrationCounter + " of " + uncalibrated.size() +
                "] Type: " + calibration.getType());

            try
            {
                calibration.calibrate();
            }
            catch(CalibrationException | RuntimeException e)
            {
                failed++;
                mLog.error("Calibration failed for [" + calibration.getType() + "] - it will use the scalar " +
                    "implementation", e);
            }
        }

        mLog.info("Calibration Complete!" + (failed > 0 ? "  [" + failed + "] calibrations failed." : ""));
        mLog.debug(getSummary());
    }

    /**
     * Logs a one-line calibration status at info level and the per-calibration summary at debug level.
     */
    private void logStatus()
    {
        int pending = getUncalibrated().size();
        int total = getCalibrationTypes().size();

        if(pending == 0)
        {
            mLog.info("Vector calibrations loaded for [" + total + "] operations");
        }
        else
        {
            mLog.info("Vector calibrations pending for [" + pending + " of " + total + "] operations");
        }

        if(mLog.isDebugEnabled())
        {
            mLog.debug(getSummary());
        }
    }

    /**
     * Indicates if vector operations are enabled, so that calibration has an effect.
     */
    public boolean isVectorEnabled()
    {
        return sVectorCalibrationSettings.isVectorEnabled();
    }

    /**
     * List of calibrations that need to be performed.
     */
    public List<Calibration> getUncalibrated()
    {
        List<Calibration> uncalibrated = new ArrayList<>();

        for(Calibration calibration: mCalibrationMap.values())
        {
            if(!calibration.isCalibrated())
            {
                uncalibrated.add(calibration);
            }
        }

        //Sort by calibration type
        Collections.sort(uncalibrated, Comparator.comparing(Calibration::getType));

        return uncalibrated;
    }

    public static void main(String[] args)
    {
        CalibrationManager manager = getInstance();
//        manager.reset();

        if(!manager.isCalibrated())
        {
            try
            {
                manager.calibrate();
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }


        StringBuilder sb1 = new StringBuilder();
        sb1.append("Updated Calibration Settings:\n");
        for(CalibrationType type: CalibrationType.values())
        {
            Calibration calibration = manager.getCalibration(type);
            if(calibration != null)
            {
                sb1.append("\t").append(type.name()).append("\t").append(manager.getCalibration(type).getImplementation()).append("\n");
            }
        }
        System.out.println(sb1);

        System.out.println("Complete");
    }
}
