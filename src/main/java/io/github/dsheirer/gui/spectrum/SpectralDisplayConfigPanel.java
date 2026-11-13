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
import io.github.dsheirer.source.tuner.Tuner;
import io.github.dsheirer.spectrum.DFTSize;
import java.net.URL;
import java.util.List;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.StringConverter;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.javafx.IconFontFX;
import jiconfont.javafx.IconNode;
import org.controlsfx.control.ToggleSwitch;

/**
 * Translucent configuration panel that floats over the right side of the spectral display.  It shows as a small
 * settings tab while collapsed and expands while the mouse is over it.  The pin button keeps it expanded.
 *
 * Controls: tuner selection, spectrum and waterfall on/off switches, waterfall pause, DFT size, window, frame rate,
 * averaging, peak hold, the settings of each display layer that has settings, and the color theme.
 */
public class SpectralDisplayConfigPanel extends VBox
{
    private static final String STYLESHEET = "/spectral_display.css";
    private static final double PANEL_WIDTH = 360;
    private static final double SWITCH_WIDTH = 40;
    private static final java.text.DecimalFormat RESOLUTION_HZ_FORMAT = new java.text.DecimalFormat("0.#");
    private static final java.text.DecimalFormat RESOLUTION_KHZ_FORMAT = new java.text.DecimalFormat("0.##");
    private static final Duration COLLAPSE_DELAY = Duration.millis(500);
    private static final Duration FADE_DURATION = Duration.millis(150);

    static
    {
        IconFontFX.register(FontAwesome.getIconFont());
    }

    private final DftSpectralDisplay mHost;
    private final TunerSpectralDisplay mTunerSpectralDisplay;
    private final SpectralDisplay mSpectralDisplay;
    private final BooleanProperty mExpanded = new SimpleBooleanProperty(false);
    private final BooleanProperty mPinned = new SimpleBooleanProperty(false);
    private final PauseTransition mCollapseTimer = new PauseTransition(COLLAPSE_DELAY);
    private final IconNode mTabIcon = new IconNode(FontAwesome.SLIDERS);
    private final VBox mExpandedContent = new VBox();
    private final MenuButton mTunerMenuButton = new MenuButton();
    private int mPopupsShowing = 0;

    /**
     * Constructs an instance.
     * @param host display to configure.  A tuner display adds a tuner menu to the panel.
     */
    public SpectralDisplayConfigPanel(DftSpectralDisplay host)
    {
        mHost = host;
        mTunerSpectralDisplay = host instanceof TunerSpectralDisplay tunerDisplay ? tunerDisplay : null;
        mSpectralDisplay = host.getSpectralDisplay();

        URL stylesheet = SpectralDisplayConfigPanel.class.getResource(STYLESHEET);

        if(stylesheet != null)
        {
            getStylesheets().add(stylesheet.toExternalForm());
        }

        getStyleClass().add("spectral-config-panel");
        setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        setFillWidth(true);

        mTabIcon.setIconSize(16);
        mTabIcon.getStyleClass().add("spectral-config-icon");

        buildExpandedContent();

        mCollapseTimer.setOnFinished(e -> {
            if(!isHover() && mPopupsShowing == 0 && !mPinned.get())
            {
                mExpanded.set(false);
            }
        });

        setOnMouseEntered(e -> {
            mCollapseTimer.stop();
            mExpanded.set(true);
        });
        setOnMouseExited(e -> scheduleCollapse());
        mExpanded.addListener((ob, ov, nv) -> updateExpanded());
        mPinned.addListener((ob, ov, nv) -> {
            if(nv)
            {
                mExpanded.set(true);
            }
            else
            {
                scheduleCollapse();
            }
        });

        updateExpanded();
    }

    /**
     * Keeps the panel expanded when true.
     */
    public BooleanProperty pinnedProperty()
    {
        return mPinned;
    }

    /**
     * Indicates if the panel is expanded.
     */
    public BooleanProperty expandedProperty()
    {
        return mExpanded;
    }

    /**
     * Releases resources.
     */
    public void dispose()
    {
        mCollapseTimer.stop();
    }

    private void scheduleCollapse()
    {
        if(!mPinned.get())
        {
            mCollapseTimer.playFromStart();
        }
    }

    /**
     * Keeps the panel expanded while a combo box, menu or color picker popup is showing, since the mouse leaves the
     * panel to use the popup.
     */
    private void trackPopup(ReadOnlyBooleanProperty showing)
    {
        showing.addListener((ob, ov, nv) -> {
            mPopupsShowing = Math.max(0, mPopupsShowing + (nv ? 1 : -1));

            if(!nv)
            {
                scheduleCollapse();
            }
        });
    }

    private void updateExpanded()
    {
        boolean expanded = mExpanded.get() || mPinned.get();

        if(expanded)
        {
            getChildren().setAll(mExpandedContent);
            getStyleClass().remove("collapsed");
            setPrefWidth(PANEL_WIDTH);
            mExpandedContent.setOpacity(0);
            FadeTransition fade = new FadeTransition(FADE_DURATION, mExpandedContent);
            fade.setToValue(1.0);
            fade.play();
        }
        else
        {
            getChildren().setAll(mTabIcon);
            if(!getStyleClass().contains("collapsed"))
            {
                getStyleClass().add("collapsed");
            }
            setPrefWidth(Region.USE_COMPUTED_SIZE);
        }
    }

    private void buildExpandedContent()
    {
        //Header: icon, title and pin button
        IconNode headerIcon = new IconNode(FontAwesome.SLIDERS);
        headerIcon.setIconSize(14);
        headerIcon.getStyleClass().add("spectral-config-icon");
        Label title = new Label("Display Settings");
        title.getStyleClass().add("spectral-config-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        IconNode pinIcon = new IconNode(FontAwesome.THUMB_TACK);
        pinIcon.setIconSize(12);
        pinIcon.getStyleClass().add("spectral-config-icon");
        ToggleButton pinButton = new ToggleButton(null, pinIcon);
        pinButton.getStyleClass().add("spectral-config-pin");
        pinButton.selectedProperty().bindBidirectional(mPinned);
        pinButton.setTooltip(new Tooltip("Keep the settings panel open"));
        HBox header = new HBox(6, headerIcon, title, spacer, pinButton);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(2, 2, 4, 4));

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(5);
        grid.setPadding(new Insets(0, 8, 6, 6));
        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setHalignment(HPos.RIGHT);
        //Labels keep their full width - the controls column absorbs any shortfall
        labelColumn.setMinWidth(Region.USE_PREF_SIZE);
        ColumnConstraints controlColumn = new ColumnConstraints();
        controlColumn.setHgrow(Priority.ALWAYS);
        controlColumn.setFillWidth(true);
        grid.getColumnConstraints().addAll(labelColumn, controlColumn);
        int row = 0;

        //Tuner
        if(mTunerSpectralDisplay != null)
        {
            grid.add(sectionLabel("Source"), 0, row++, 2, 1);
            mTunerMenuButton.setMaxWidth(Double.MAX_VALUE);
            mTunerMenuButton.setOnShowing(e -> updateTunerMenu());
            trackPopup(mTunerMenuButton.showingProperty());
            mTunerMenuButton.textProperty().bind(Bindings.createStringBinding(() -> {
                Tuner tuner = mTunerSpectralDisplay.displayedTunerProperty().get();
                return tuner != null ? tuner.getPreferredName() : "None";
            }, mTunerSpectralDisplay.displayedTunerProperty()));
            row = addRow(grid, row, "Tuner", mTunerMenuButton);
        }

        //Display sections
        grid.add(sectionLabel("Display"), 0, row++, 2, 1);
        ToggleSwitch spectrumSwitch = toggleSwitch("Spectrum", mSpectralDisplay.spectrumEnabledProperty());
        ToggleSwitch waterfallSwitch = toggleSwitch("Waterfall", mSpectralDisplay.waterfallEnabledProperty());
        ToggleSwitch pauseSwitch = toggleSwitch("Pause", mSpectralDisplay.waterfallPausedProperty());
        pauseSwitch.disableProperty().bind(mSpectralDisplay.waterfallEnabledProperty().not());
        pauseSwitch.setTooltip(new Tooltip("Pause the waterfall"));
        HBox switches = new HBox(12, labeled(spectrumSwitch), labeled(waterfallSwitch), labeled(pauseSwitch));
        switches.setAlignment(Pos.CENTER);
        grid.add(switches, 0, row++, 2, 1);

        ComboBox<SpectralDisplayTheme> theme = comboBox(SpectralDisplayTheme.values());
        theme.valueProperty().bindBidirectional(mSpectralDisplay.themeProperty());
        theme.setCellFactory(listView -> new ThemeCell());
        theme.setButtonCell(new ThemeCell());
        row = addRow(grid, row, "Theme", theme);

        //Layer settings without a section of their own
        for(SpectralDisplayLayer layer: mSpectralDisplay.getLayers())
        {
            if(layer.getConfigurationTitle() == null)
            {
                row = addLayerRows(grid, row, layer.getConfigurationRows());
            }
        }

        //DFT
        grid.add(sectionLabel("FFT"), 0, row++, 2, 1);
        ComboBox<DFTSize> dftSize = comboBox(DFTSize.values());
        dftSize.setConverter(dftSizeConverter());
        //Refresh the resolution shown with each size when the sample rate changes
        mSpectralDisplay.getModel().sampleRateProperty().addListener((ob, ov, nv) ->
                dftSize.setConverter(dftSizeConverter()));
        dftSize.setTooltip(new Tooltip("FFT size and the resulting frequency resolution (bandwidth of each bin)"));
        dftSize.valueProperty().bindBidirectional(mHost.dftSizeProperty());
        row = addRow(grid, row, "Size", dftSize);

        ComboBox<WindowType> windowType = comboBox(java.util.Arrays.stream(WindowType.values())
                .filter(DftSpectralDisplay::isWindowSupported).toArray(WindowType[]::new));
        windowType.valueProperty().bindBidirectional(mHost.windowTypeProperty());
        row = addRow(grid, row, "Window", windowType);

        ComboBox<Integer> frameRate = new ComboBox<>();
        for(int rate: DftSpectralDisplay.FRAME_RATES)
        {
            frameRate.getItems().add(rate);
        }
        if(!frameRate.getItems().contains(mHost.frameRateProperty().get()))
        {
            frameRate.getItems().add(mHost.frameRateProperty().get());
            frameRate.getItems().sort(Integer::compare);
        }
        frameRate.setMaxWidth(Double.MAX_VALUE);
        frameRate.setConverter(converter(rate -> rate + " fps"));
        frameRate.setValue(mHost.frameRateProperty().get());
        frameRate.valueProperty().addListener((ob, ov, nv) -> {
            if(nv != null)
            {
                mHost.frameRateProperty().set(nv);
            }
        });
        trackPopup(frameRate.showingProperty());
        row = addRow(grid, row, "Frame Rate", frameRate);

        Slider averaging = new Slider(SpectralDisplay.AVERAGING_MINIMUM, SpectralDisplay.AVERAGING_MAXIMUM,
                mSpectralDisplay.averagingProperty().get());
        averaging.setBlockIncrement(1);
        averaging.setMajorTickUnit(1);
        averaging.setMinorTickCount(0);
        averaging.setSnapToTicks(true);
        averaging.valueProperty().addListener((ob, ov, nv) ->
                mSpectralDisplay.averagingProperty().set((int)Math.round(nv.doubleValue())));
        HBox.setHgrow(averaging, Priority.ALWAYS);
        Label fast = new Label("Fast");
        fast.getStyleClass().add("spectral-config-hint");
        Label slow = new Label("Slow");
        slow.getStyleClass().add("spectral-config-hint");
        HBox averagingBox = new HBox(4, fast, averaging, slow);
        averagingBox.setAlignment(Pos.CENTER_LEFT);
        Tooltip.install(averagingBox, new Tooltip("Spectrum averaging: signals rise quickly and fade slowly.  " +
                "Slower settings give a steadier noise floor."));
        row = addRow(grid, row, "Averaging", averagingBox);

        ConfigurationRow peakHold = ConfigurationRow.toggle("Peak Hold", mSpectralDisplay.peakHoldProperty());
        Tooltip.install(peakHold.control(), new Tooltip("Show a line at the recent peak of each frequency, " +
                "falling slowly"));
        row = addRow(grid, row, peakHold.label(), peakHold.control());

        //Layer settings
        for(SpectralDisplayLayer layer: mSpectralDisplay.getLayers())
        {
            List<ConfigurationRow> rows = layer.getConfigurationRows();

            if(layer.getConfigurationTitle() != null && rows != null && !rows.isEmpty())
            {
                grid.add(sectionLabel(layer.getConfigurationTitle()), 0, row++, 2, 1);
                row = addLayerRows(grid, row, rows);
            }
        }

        VBox content = new VBox(4, grid);
        content.setPadding(new Insets(0, 0, 4, 0));

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("spectral-config-scroll");

        //Limit the panel to the display height and scroll the controls when they don't fit
        scrollPane.prefViewportHeightProperty().bind(content.heightProperty());
        scrollPane.maxHeightProperty().bind(Bindings.max(40, mHost.heightProperty()
                .subtract(header.heightProperty()).subtract(24)));

        mExpandedContent.getChildren().addAll(header, scrollPane);
    }

    /**
     * Converter that labels each DFT size with its bin bandwidth at the current sample rate.
     */
    private StringConverter<DFTSize> dftSizeConverter()
    {
        return converter(size -> {
            int sampleRate = mSpectralDisplay.getModel().getSampleRate();

            if(sampleRate <= 0)
            {
                return size.getLabel();
            }

            return size.getLabel() + " \u00B7 " + formatResolution((double)sampleRate / size.getSize());
        });
    }

    /**
     * Formats a bin bandwidth in Hz or kHz.
     */
    static String formatResolution(double hertz)
    {
        if(hertz >= 1000.0)
        {
            return RESOLUTION_KHZ_FORMAT.format(hertz / 1000.0) + " kHz/bin";
        }

        return RESOLUTION_HZ_FORMAT.format(hertz) + " Hz/bin";
    }

    /**
     * Rebuilds the tuner menu with the currently available tuners.
     */
    private void updateTunerMenu()
    {
        mTunerMenuButton.getItems().clear();
        ToggleGroup group = new ToggleGroup();
        Tuner current = mTunerSpectralDisplay.displayedTunerProperty().get();
        List<Tuner> tuners = mTunerSpectralDisplay.getAvailableTuners();

        for(Tuner tuner: tuners)
        {
            RadioMenuItem item = new RadioMenuItem(tuner.getPreferredName());
            item.setToggleGroup(group);
            item.setSelected(tuner == current);
            item.setOnAction(e -> mTunerSpectralDisplay.tunerSelected(tuner));
            mTunerMenuButton.getItems().add(item);
        }

        if(!tuners.isEmpty())
        {
            mTunerMenuButton.getItems().add(new SeparatorMenuItem());
        }

        RadioMenuItem none = new RadioMenuItem("None");
        none.setToggleGroup(group);
        none.setSelected(current == null);
        none.setOnAction(e -> mTunerSpectralDisplay.tunerSelected(null));
        mTunerMenuButton.getItems().add(none);
    }

    /**
     * Adds a layer's configuration rows to the grid.
     * @return next row index
     */
    private int addLayerRows(GridPane grid, int row, List<ConfigurationRow> rows)
    {
        if(rows != null)
        {
            for(ConfigurationRow configurationRow: rows)
            {
                if(configurationRow.control() instanceof ComboBox<?> comboBox)
                {
                    trackPopup(comboBox.showingProperty());
                }

                row = addRow(grid, row, configurationRow.label(), configurationRow.control());
            }
        }

        return row;
    }

    private int addRow(GridPane grid, int row, String label, Node control)
    {
        grid.add(new Label(label), 0, row);
        grid.add(control, 1, row);
        GridPane.setHgrow(control, Priority.ALWAYS);
        return row + 1;
    }

    private static Label sectionLabel(String text)
    {
        Label label = new Label(text);
        label.getStyleClass().add("spectral-config-section");
        return label;
    }

    /**
     * Places the switch's text in a label to the left of the switch.  The switch's own text adds a wide gap.
     */
    private static HBox labeled(ToggleSwitch toggleSwitch)
    {
        Label label = new Label(toggleSwitch.getText());
        label.setMinWidth(Region.USE_PREF_SIZE);
        label.disableProperty().bind(toggleSwitch.disabledProperty());
        label.setOnMouseClicked(e -> {
            if(!toggleSwitch.isDisabled())
            {
                toggleSwitch.setSelected(!toggleSwitch.isSelected());
            }
        });
        toggleSwitch.setText(null);
        //The switch skin reserves space for its text even when there is none, so size it to the thumb track
        toggleSwitch.setMinWidth(SWITCH_WIDTH);
        toggleSwitch.setPrefWidth(SWITCH_WIDTH);
        toggleSwitch.setMaxWidth(SWITCH_WIDTH);
        HBox box = new HBox(4, label, toggleSwitch);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private static ToggleSwitch toggleSwitch(String text, BooleanProperty property)
    {
        ToggleSwitch toggleSwitch = new ToggleSwitch(text);
        toggleSwitch.selectedProperty().bindBidirectional(property);
        return toggleSwitch;
    }

    @SafeVarargs
    private <T> ComboBox<T> comboBox(T... values)
    {
        ComboBox<T> comboBox = new ComboBox<>();
        comboBox.getItems().addAll(values);
        comboBox.setMaxWidth(Double.MAX_VALUE);
        trackPopup(comboBox.showingProperty());
        return comboBox;
    }

    private static <T> StringConverter<T> converter(java.util.function.Function<T,String> function)
    {
        return new StringConverter<>()
        {
            @Override
            public String toString(T value)
            {
                return value != null ? function.apply(value) : "";
            }

            @Override
            public T fromString(String string)
            {
                return null;
            }
        };
    }

    /**
     * Theme list cell with a preview swatch: the spectrum background, a channel highlight and the waterfall palette.
     */
    private static class ThemeCell extends javafx.scene.control.ListCell<SpectralDisplayTheme>
    {
        private static final double SWATCH_WIDTH = 44;
        private static final double SWATCH_HEIGHT = 12;

        @Override
        protected void updateItem(SpectralDisplayTheme theme, boolean empty)
        {
            super.updateItem(theme, empty);

            if(empty || theme == null)
            {
                setText(null);
                setGraphic(null);
                return;
            }

            setText(theme.getLabel());
            setGraphic(createSwatch(theme));
            setGraphicTextGap(8);
        }

        private static Node createSwatch(SpectralDisplayTheme theme)
        {
            javafx.scene.canvas.Canvas canvas = new javafx.scene.canvas.Canvas(SWATCH_WIDTH, SWATCH_HEIGHT);
            javafx.scene.canvas.GraphicsContext gc = canvas.getGraphicsContext2D();
            double half = SWATCH_WIDTH / 2.0;

            //Left half: spectrum background, trace fill and a processing channel highlight
            gc.setFill(theme.getBackground());
            gc.fillRect(0, 0, half, SWATCH_HEIGHT);
            gc.setFill(theme.getChannelProcessing());
            gc.fillRect(half * 0.55, 0, half * 0.3, SWATCH_HEIGHT);
            gc.setFill(theme.getGradientBottom());
            gc.fillRect(0, SWATCH_HEIGHT * 0.65, half, SWATCH_HEIGHT * 0.35);
            gc.setStroke(theme.getTrace());
            gc.strokeLine(0, SWATCH_HEIGHT * 0.65, half, SWATCH_HEIGHT * 0.65);

            //Right half: waterfall palette from the noise floor to the strongest signal
            for(int x = 0; x < half; x++)
            {
                int index = (int)Math.round(x / (half - 1) * (WaterfallPalette.SIZE - 1));
                gc.setFill(WaterfallPalette.toColor(theme.getWaterfallColor(index)));
                gc.fillRect(half + x, 0, 1, SWATCH_HEIGHT);
            }

            gc.setStroke(javafx.scene.paint.Color.gray(0.5, 0.8));
            gc.strokeRect(0.5, 0.5, SWATCH_WIDTH - 1, SWATCH_HEIGHT - 1);
            return canvas;
        }
    }
}
