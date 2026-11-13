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

package io.github.dsheirer.gui.spectrum;

import io.github.dsheirer.dsp.window.WindowType;
import io.github.dsheirer.gui.spectrum.layer.DecibelLayer;
import io.github.dsheirer.gui.spectrum.layer.FrequencyLayer;
import io.github.dsheirer.properties.SystemProperties;
import io.github.dsheirer.spectrum.ComplexDftProcessor;
import io.github.dsheirer.spectrum.DFTSize;
import io.github.dsheirer.spectrum.converter.FullScaleDecibelConverter;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;

/**
 * Base class for a spectral display fed by DFT processing of a complex sample stream: the spectral display with the
 * decibel and frequency layers plus any layers the subclass adds, the DFT processor and dBFS converter, the
 * collapsible configuration panel, and saving the display settings under a property key prefix.
 *
 * The color theme is shared by all spectral displays in the application.
 *
 * Construct on the JavaFX application thread.  Subclasses call installConfigPanel() at the end of their constructor.
 */
public abstract class DftSpectralDisplay extends StackPane
{
    public static final int[] FRAME_RATES = {10, 14, 16, 18, 20, 25, 30, 40, 50, 60};
    public static final String PROPERTY_THEME = "spectral.display.theme";
    private static final String DFT_SIZE = "dft.size";
    private static final String WINDOW_TYPE = "window.type";
    private static final String FRAME_RATE = "frame.rate";
    private static final String AVERAGING = "averaging";
    private static final String PEAK_HOLD = "peak.hold";
    private static final String SPECTRUM_ENABLED = "spectrum.enabled";
    private static final String WATERFALL_ENABLED = "waterfall.enabled";
    private static final String DIVIDER_POSITION = "divider.position";
    private static final String DECIBEL_SCALE = "decibel.scale";
    private static final String CONFIG_PINNED = "config.pinned";
    private static ObjectProperty<SpectralDisplayTheme> sSharedTheme;

    private final SpectralDisplay mSpectralDisplay;
    private final DecibelLayer mDecibelLayer = new DecibelLayer();
    private final ComplexDftProcessor mDftProcessor = new ComplexDftProcessor();
    private final FullScaleDecibelConverter mDecibelConverter = new FullScaleDecibelConverter();
    private final String mSettingsPrefix;
    private final boolean mSaveSettings;
    private final ObjectProperty<DFTSize> mDftSize = new SimpleObjectProperty<>(DFTSize.FFT04096);
    private final ObjectProperty<WindowType> mWindowType = new SimpleObjectProperty<>(WindowType.BLACKMAN_HARRIS_7);
    private final IntegerProperty mFrameRate = new SimpleIntegerProperty(20);
    private SpectralDisplayConfigPanel mConfigPanel;
    private volatile boolean mProcessingEnabled;
    private volatile boolean mDftRunning;

    /**
     * Constructs an instance.
     * @param settingsPrefix for the saved setting keys, for example "spectral.display"
     * @param saveSettings true to save setting changes, false to load the saved settings but not change them
     * @param layers to draw after the decibel scale and frequency layers
     */
    protected DftSpectralDisplay(String settingsPrefix, boolean saveSettings, SpectralDisplayLayer... layers)
    {
        mSettingsPrefix = settingsPrefix;
        mSaveSettings = saveSettings;

        SpectralDisplayLayer[] all = new SpectralDisplayLayer[layers.length + 2];
        all[0] = mDecibelLayer;
        all[1] = new FrequencyLayer();
        System.arraycopy(layers, 0, all, 2, layers.length);
        mSpectralDisplay = new SpectralDisplay(all);

        mDftProcessor.setUnityGainWindow(true);
        mDftProcessor.addConverter(mDecibelConverter);
        mDecibelConverter.addListener(mSpectralDisplay::receive);
        //The processor starts its timer on construction - leave it stopped until there is a sample source
        mDftProcessor.stop();

        loadSettings();

        mDftSize.addListener((ob, ov, nv) -> {
            mDftProcessor.setDFTSize(nv);
            mSpectralDisplay.getModel().resetZoom();
            save(DFT_SIZE, nv.name());
        });
        mWindowType.addListener((ob, ov, nv) -> {
            mDftProcessor.setWindowType(nv);
            mSpectralDisplay.getModel().noiseBandwidthProperty().set(ComplexDftProcessor.getNoiseBandwidth(nv));
            save(WINDOW_TYPE, nv.name());
        });
        mFrameRate.addListener((ob, ov, nv) -> {
            mDftProcessor.setFrameRateNoSave(nv.intValue());

            if(!mDftRunning)
            {
                //Changing the frame rate restarts the timer - keep it stopped while not processing
                mDftProcessor.stop();
            }

            save(FRAME_RATE, nv.intValue());
        });
        mSpectralDisplay.averagingProperty().addListener((ob, ov, nv) -> save(AVERAGING, nv.intValue()));
        mSpectralDisplay.peakHoldProperty().addListener((ob, ov, nv) -> save(PEAK_HOLD, nv));
        mSpectralDisplay.dividerPositionProperty().addListener((ob, ov, nv) ->
                save(DIVIDER_POSITION, String.valueOf(nv.doubleValue())));
        mDecibelLayer.visibleProperty().addListener((ob, ov, nv) -> save(DECIBEL_SCALE, nv));
        mSpectralDisplay.spectrumEnabledProperty().addListener((ob, ov, nv) -> {
            save(SPECTRUM_ENABLED, nv);
            updateProcessingEnabled();
        });
        mSpectralDisplay.waterfallEnabledProperty().addListener((ob, ov, nv) -> {
            save(WATERFALL_ENABLED, nv);
            updateProcessingEnabled();
        });
        mProcessingEnabled = mSpectralDisplay.spectrumEnabledProperty().get() ||
                mSpectralDisplay.waterfallEnabledProperty().get();

        mSpectralDisplay.themeProperty().bindBidirectional(getSharedTheme());

        getChildren().add(mSpectralDisplay);
        setMinSize(0, 0);
    }

    /**
     * Creates the configuration panel.  Subclasses call this at the end of their constructor, after their own
     * settings are loaded, since the panel reads the subclass's controls.
     */
    protected void installConfigPanel()
    {
        mConfigPanel = new SpectralDisplayConfigPanel(this);
        mConfigPanel.pinnedProperty().set(SystemProperties.getInstance().get(key(CONFIG_PINNED), false));
        mConfigPanel.pinnedProperty().addListener((ob, ov, nv) -> save(CONFIG_PINNED, nv));
        StackPane.setAlignment(mConfigPanel, Pos.TOP_RIGHT);
        StackPane.setMargin(mConfigPanel, new Insets(6));
        getChildren().add(mConfigPanel);
    }

    /**
     * Color theme shared by all spectral displays, loaded from and saved to the theme setting.  JavaFX thread only.
     */
    public static ObjectProperty<SpectralDisplayTheme> getSharedTheme()
    {
        if(sSharedTheme == null)
        {
            SpectralDisplayTheme theme = parse(SpectralDisplayTheme.class, SystemProperties.getInstance()
                    .get(PROPERTY_THEME, SpectralDisplayTheme.getDefault().name()), SpectralDisplayTheme.getDefault());
            sSharedTheme = new SimpleObjectProperty<>(theme);
            sSharedTheme.addListener((ob, ov, nv) -> {
                if(nv != null)
                {
                    SystemProperties.getInstance().set(PROPERTY_THEME, nv.name());
                }
            });
        }

        return sSharedTheme;
    }

    private void loadSettings()
    {
        SystemProperties properties = SystemProperties.getInstance();
        DFTSize dftSize = parse(DFTSize.class, properties.get(key(DFT_SIZE), DFTSize.FFT04096.name()),
                DFTSize.FFT04096);
        mDftSize.set(dftSize);
        mDftProcessor.setDFTSize(dftSize);

        WindowType windowType = parse(WindowType.class, properties.get(key(WINDOW_TYPE),
                WindowType.BLACKMAN_HARRIS_7.name()), WindowType.BLACKMAN_HARRIS_7);

        if(!isWindowSupported(windowType))
        {
            windowType = WindowType.BLACKMAN_HARRIS_7;
        }

        mWindowType.set(windowType);
        mDftProcessor.setWindowType(windowType);
        mSpectralDisplay.getModel().noiseBandwidthProperty().set(ComplexDftProcessor.getNoiseBandwidth(windowType));

        int frameRate = Math.max(1, Math.min(100, properties.get(key(FRAME_RATE), mDftProcessor.getFrameRate())));
        mFrameRate.set(frameRate);
        mDftProcessor.setFrameRateNoSave(frameRate);
        mDftProcessor.stop();

        int averaging = properties.get(key(AVERAGING), SpectralDisplay.AVERAGING_DEFAULT);
        mSpectralDisplay.averagingProperty().set(Math.max(SpectralDisplay.AVERAGING_MINIMUM,
                Math.min(SpectralDisplay.AVERAGING_MAXIMUM, averaging)));
        mSpectralDisplay.peakHoldProperty().set(properties.get(key(PEAK_HOLD), false));
        mDecibelLayer.visibleProperty().set(properties.get(key(DECIBEL_SCALE), true));

        try
        {
            double divider = Double.parseDouble(properties.get(key(DIVIDER_POSITION), "0.5"));
            mSpectralDisplay.dividerPositionProperty().set(Math.max(0.05, Math.min(0.95, divider)));
        }
        catch(NumberFormatException nfe)
        {
            mSpectralDisplay.dividerPositionProperty().set(0.5);
        }

        mSpectralDisplay.spectrumEnabledProperty().set(properties.get(key(SPECTRUM_ENABLED), true));
        mSpectralDisplay.waterfallEnabledProperty().set(properties.get(key(WATERFALL_ENABLED), true));
    }

    /**
     * Full property key for the setting name.
     */
    protected String key(String name)
    {
        return mSettingsPrefix + "." + name;
    }

    /**
     * Indicates if the window type can be used for the spectral display.  Kaiser windows need extra parameters.
     */
    public static boolean isWindowSupported(WindowType windowType)
    {
        return windowType != null && windowType != WindowType.KAISER;
    }

    /**
     * Parses an enum setting value, or returns the default for a missing or unrecognized value.
     */
    protected static <E extends Enum<E>> E parse(Class<E> type, String value, E defaultValue)
    {
        if(value != null)
        {
            try
            {
                return Enum.valueOf(type, value);
            }
            catch(IllegalArgumentException iae)
            {
                //Fall through to the default
            }
        }

        return defaultValue;
    }

    protected void save(String name, String value)
    {
        if(mSaveSettings)
        {
            SystemProperties.getInstance().set(key(name), value);
        }
    }

    protected void save(String name, int value)
    {
        if(mSaveSettings)
        {
            SystemProperties.getInstance().set(key(name), value);
        }
    }

    protected void save(String name, boolean value)
    {
        if(mSaveSettings)
        {
            SystemProperties.getInstance().set(key(name), value);
        }
    }

    /**
     * The spectral display.
     */
    public SpectralDisplay getSpectralDisplay()
    {
        return mSpectralDisplay;
    }

    /**
     * Collapsible configuration panel, or null before installConfigPanel() is called.
     */
    public SpectralDisplayConfigPanel getConfigPanel()
    {
        return mConfigPanel;
    }

    /**
     * DFT processor.  Register it with the sample source to feed the display.
     */
    protected ComplexDftProcessor getDftProcessor()
    {
        return mDftProcessor;
    }

    /**
     * DFT size.
     */
    public ObjectProperty<DFTSize> dftSizeProperty()
    {
        return mDftSize;
    }

    /**
     * DFT window type.
     */
    public ObjectProperty<WindowType> windowTypeProperty()
    {
        return mWindowType;
    }

    /**
     * DFT frames per second.
     */
    public IntegerProperty frameRateProperty()
    {
        return mFrameRate;
    }

    /**
     * Indicates if the spectrum or the waterfall is turned on.  Safe to read from any thread.
     */
    protected boolean isProcessingEnabled()
    {
        return mProcessingEnabled;
    }

    private void updateProcessingEnabled()
    {
        mProcessingEnabled = mSpectralDisplay.spectrumEnabledProperty().get() ||
                mSpectralDisplay.waterfallEnabledProperty().get();
        processingEnabledChanged();
    }

    /**
     * Called on the JavaFX thread when the spectrum and waterfall are both turned off, or one is turned back on.
     * Subclasses start or stop their sample source with isProcessingEnabled().
     */
    protected abstract void processingEnabledChanged();

    /**
     * Starts the DFT processor timer.
     */
    protected void startDft()
    {
        if(!mDftRunning)
        {
            mDftRunning = true;
            mDftProcessor.clearBuffer();
            mDftProcessor.start();
        }
    }

    /**
     * Stops the DFT processor timer and clears the display.
     */
    protected void stopDft()
    {
        if(mDftRunning)
        {
            mDftRunning = false;
            mDftProcessor.stop();
            mDftProcessor.clearBuffer();
            mSpectralDisplay.clear();
        }
    }

    /**
     * Indicates if the DFT processor timer is running.
     */
    protected boolean isDftRunning()
    {
        return mDftRunning;
    }

    /**
     * Runs the runnable on the JavaFX application thread, ignoring the request after the JavaFX toolkit has exited
     * during application shutdown.
     */
    protected static void runOnFxThread(Runnable runnable)
    {
        try
        {
            Platform.runLater(runnable);
        }
        catch(IllegalStateException ise)
        {
            //JavaFX toolkit has exited
        }
    }

    /**
     * Releases the DFT processing and the display.  Subclasses disconnect their sample source first.
     */
    public void dispose()
    {
        stopDft();
        mDftProcessor.dispose();
        mDecibelConverter.dispose();
        runOnFxThread(() -> {
            mSpectralDisplay.themeProperty().unbindBidirectional(getSharedTheme());

            if(mConfigPanel != null)
            {
                mConfigPanel.dispose();
            }

            mSpectralDisplay.dispose();
        });
    }
}
