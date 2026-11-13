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

import java.net.URL;
import java.util.function.DoubleFunction;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.beans.property.DoubleProperty;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Vertical slider that floats along the right edge of a display.  While collapsed it shows as a thin strip with a
 * marker at the current value.  Moving the mouse over the strip expands it into a translucent panel with a title, a
 * vertical slider and the formatted value.  It collapses again shortly after the mouse leaves, unless the slider is
 * being dragged.
 *
 * Place it in a StackPane aligned to the right edge.  The slider's minimum is at the bottom.
 */
public class EdgeSlider extends StackPane
{
    private static final String STYLESHEET = "/spectral_display.css";
    private static final double COLLAPSED_WIDTH = 14;
    private static final double STRIP_WIDTH = 4;
    private static final double MARKER_HEIGHT = 10;
    private static final double EXPANDED_WIDTH = 64;
    private static final Duration COLLAPSE_DELAY = Duration.millis(400);
    private static final Duration FADE_DURATION = Duration.millis(120);

    private final Slider mSlider;
    private final Label mValueLabel = new Label();
    private final DoubleFunction<String> mFormatter;
    private final VBox mExpanded;
    private final Pane mCollapsed = new Pane();
    private final Region mStrip = new Region();
    private final Region mMarker = new Region();
    private final PauseTransition mCollapseTimer = new PauseTransition(COLLAPSE_DELAY);
    private boolean mIsExpanded = false;

    /**
     * Constructs an instance.
     * @param title shown above the slider
     * @param tooltip describing the control
     * @param min value, at the bottom of the slider
     * @param max value, at the top of the slider
     * @param step between values
     * @param formatter for the value shown below the slider
     */
    public EdgeSlider(String title, String tooltip, double min, double max, double step,
                      DoubleFunction<String> formatter)
    {
        mFormatter = formatter;
        URL stylesheet = EdgeSlider.class.getResource(STYLESHEET);

        if(stylesheet != null)
        {
            getStylesheets().add(stylesheet.toExternalForm());
        }

        getStyleClass().add("spectral-edge-slider");
        setMaxWidth(Region.USE_PREF_SIZE);
        setMinWidth(Region.USE_PREF_SIZE);
        setPickOnBounds(true);

        mSlider = new Slider(min, max, min);
        mSlider.setOrientation(Orientation.VERTICAL);
        mSlider.setMajorTickUnit(step);
        mSlider.setMinorTickCount(0);
        mSlider.setBlockIncrement(step);
        mSlider.setSnapToTicks(true);
        mSlider.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(mSlider, Priority.ALWAYS);
        mSlider.valueProperty().addListener((ob, ov, nv) -> {
            refreshLabel();
            requestLayout();
        });

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("spectral-edge-slider-title");
        mValueLabel.getStyleClass().add("spectral-edge-slider-value");
        mExpanded = new VBox(4, titleLabel, mSlider, mValueLabel);
        mExpanded.setAlignment(Pos.CENTER);
        mExpanded.getStyleClass().add("spectral-edge-slider-panel");
        mExpanded.setPrefWidth(EXPANDED_WIDTH);
        Tooltip.install(mExpanded, new Tooltip(tooltip));

        mStrip.getStyleClass().add("spectral-edge-slider-strip");
        mMarker.getStyleClass().add("spectral-edge-slider-marker");
        //Unmanaged so the pane doesn't resize them to their (empty) preferred size - layoutChildren() places them
        mStrip.setManaged(false);
        mMarker.setManaged(false);
        mCollapsed.getChildren().addAll(mStrip, mMarker);
        mCollapsed.setPrefWidth(COLLAPSED_WIDTH);
        mCollapsed.setMinWidth(COLLAPSED_WIDTH);

        mCollapseTimer.setOnFinished(e -> {
            if(!isHover() && !mSlider.isValueChanging() && !mSlider.isPressed())
            {
                setExpanded(false);
            }
        });
        setOnMouseEntered(e -> {
            mCollapseTimer.stop();
            setExpanded(true);
        });
        setOnMouseExited(e -> mCollapseTimer.playFromStart());
        mSlider.valueChangingProperty().addListener((ob, ov, nv) -> {
            if(!nv && !isHover())
            {
                mCollapseTimer.playFromStart();
            }
        });

        getChildren().add(mCollapsed);
        refreshLabel();
    }

    /**
     * Slider value.
     */
    public DoubleProperty valueProperty()
    {
        return mSlider.valueProperty();
    }

    /**
     * Updates the value text, for example when something the formatter depends on changes.
     */
    public void refreshLabel()
    {
        mValueLabel.setText(mFormatter != null ? mFormatter.apply(mSlider.getValue()) :
                String.valueOf(mSlider.getValue()));
    }

    /**
     * Indicates if the slider panel is expanded.
     */
    public boolean isExpanded()
    {
        return mIsExpanded;
    }

    /**
     * Expands or collapses the slider panel.
     */
    public void setExpanded(boolean expanded)
    {
        if(expanded != mIsExpanded)
        {
            mIsExpanded = expanded;

            if(expanded)
            {
                getChildren().setAll(mExpanded);
                mExpanded.setOpacity(0);
                FadeTransition fade = new FadeTransition(FADE_DURATION, mExpanded);
                fade.setToValue(1.0);
                fade.play();
            }
            else
            {
                getChildren().setAll(mCollapsed);
            }
        }
    }

    @Override
    protected void layoutChildren()
    {
        super.layoutChildren();

        if(!mIsExpanded)
        {
            //Strip along the right side of the hover area, with the marker at the value position
            double width = mCollapsed.getWidth();
            double height = mCollapsed.getHeight();
            double stripX = width - STRIP_WIDTH - 2;
            mStrip.resizeRelocate(stripX, 0, STRIP_WIDTH, height);

            double range = mSlider.getMax() - mSlider.getMin();
            double fraction = range > 0 ? (mSlider.getValue() - mSlider.getMin()) / range : 0;
            double markerY = (1.0 - fraction) * Math.max(0, height - MARKER_HEIGHT);
            mMarker.resizeRelocate(stripX - 2, markerY, STRIP_WIDTH + 4, MARKER_HEIGHT);
        }
    }
}
