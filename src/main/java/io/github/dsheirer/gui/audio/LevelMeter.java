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

package io.github.dsheirer.gui.audio;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

/**
 * Segmented horizontal audio level meter in dBFS with peak hold and level ballistics.  Feed it with peak sample
 * levels via {@link #update(float)} at a steady rate from the JavaFX application thread.
 */
public class LevelMeter extends Region
{
    public static final double MINIMUM_DB = -48.0;
    public static final double MAXIMUM_DB = 0.0;
    private static final double WARNING_DB = -12.0;
    private static final double CLIP_DB = -3.0;
    private static final double SEGMENT_WIDTH = 4.0;
    private static final double SEGMENT_GAP = 2.0;
    private static final double PREFERRED_HEIGHT = 8.0;

    private static final Color NORMAL_ON = Color.web("#3ddc84");
    private static final Color WARNING_ON = Color.web("#f5c542");
    private static final Color CLIP_ON = Color.web("#ff5c5c");
    private static final Color NORMAL_OFF = Color.web("#3ddc84", 0.12);
    private static final Color WARNING_OFF = Color.web("#f5c542", 0.12);
    private static final Color CLIP_OFF = Color.web("#ff5c5c", 0.12);

    private final Canvas mCanvas = new Canvas();
    private final double mFallPerUpdateDb;
    private final int mPeakHoldUpdates;
    private double mLevelDb = MINIMUM_DB;
    private double mPeakDb = MINIMUM_DB;
    private int mPeakHoldCount;
    private boolean mDimmed;
    private boolean mDirty = true;
    private double mDrawnLevelDb = Double.NaN;
    private double mDrawnPeakDb = Double.NaN;

    /**
     * Constructs an instance
     * @param updatesPerSecond rate that {@link #update(float)} is invoked, to scale the level fall and peak hold.
     */
    public LevelMeter(double updatesPerSecond)
    {
        mFallPerUpdateDb = 30.0 / updatesPerSecond; //30 dB per second fall
        mPeakHoldUpdates = (int)Math.round(1.5 * updatesPerSecond); //1.5 second peak hold
        getStyleClass().add("level-meter");
        getChildren().add(mCanvas);
        setMinHeight(PREFERRED_HEIGHT);
        setPrefHeight(PREFERRED_HEIGHT);
        setMaxHeight(PREFERRED_HEIGHT);
        setMinWidth(40);
    }

    /**
     * Updates the meter with the peak sample level measured since the previous update.
     * @param peak absolute sample peak, where 1.0 is full scale.
     */
    public void update(float peak)
    {
        double db = peak > 0.0f ? Math.max(MINIMUM_DB, Math.min(MAXIMUM_DB, 20.0 * Math.log10(peak))) : MINIMUM_DB;
        double level = Math.max(db, mLevelDb - mFallPerUpdateDb);

        if(db >= mPeakDb)
        {
            mPeakDb = db;
            mPeakHoldCount = mPeakHoldUpdates;
        }
        else if(mPeakHoldCount > 0)
        {
            mPeakHoldCount--;
        }
        else
        {
            mPeakDb = Math.max(MINIMUM_DB, mPeakDb - mFallPerUpdateDb);
        }

        mLevelDb = level;

        if(mDirty || mLevelDb != mDrawnLevelDb || mPeakDb != mDrawnPeakDb)
        {
            draw();
        }
    }

    /**
     * Sets the meter to draw in a dimmed state, for example when the channel is muted.
     */
    public void setDimmed(boolean dimmed)
    {
        if(mDimmed != dimmed)
        {
            mDimmed = dimmed;
            mCanvas.setOpacity(dimmed ? 0.45 : 1.0);
        }
    }

    /**
     * Resets the meter to silence.
     */
    public void reset()
    {
        mLevelDb = MINIMUM_DB;
        mPeakDb = MINIMUM_DB;
        mPeakHoldCount = 0;
        draw();
    }

    @Override
    protected void layoutChildren()
    {
        double width = snapSizeX(getWidth());
        double height = snapSizeY(getHeight());

        if(mCanvas.getWidth() != width || mCanvas.getHeight() != height)
        {
            mCanvas.setWidth(width);
            mCanvas.setHeight(height);
            draw();
        }
    }

    @Override
    protected double computePrefWidth(double height)
    {
        return 200;
    }

    /**
     * Fraction of the meter span for the dB value.
     */
    private static double toFraction(double db)
    {
        return (db - MINIMUM_DB) / (MAXIMUM_DB - MINIMUM_DB);
    }

    private void draw()
    {
        mDirty = false;
        mDrawnLevelDb = mLevelDb;
        mDrawnPeakDb = mPeakDb;
        double width = mCanvas.getWidth();
        double height = mCanvas.getHeight();
        GraphicsContext gc = mCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, width, height);

        if(width <= 0 || height <= 0)
        {
            return;
        }

        int segments = (int)Math.floor((width + SEGMENT_GAP) / (SEGMENT_WIDTH + SEGMENT_GAP));

        if(segments <= 0)
        {
            return;
        }

        //Spread the remainder across the gaps so the meter fills the width
        double step = segments > 1 ? (width - SEGMENT_WIDTH) / (segments - 1) : 0;
        double levelFraction = toFraction(mLevelDb);
        double peakFraction = toFraction(mPeakDb);
        int peakSegment = mPeakDb > MINIMUM_DB ? (int)Math.min(segments - 1, Math.floor(peakFraction * segments)) : -1;

        for(int x = 0; x < segments; x++)
        {
            double segmentFraction = (x + 0.5) / segments;
            double segmentDb = MINIMUM_DB + segmentFraction * (MAXIMUM_DB - MINIMUM_DB);
            boolean on = segmentFraction <= levelFraction || x == peakSegment;

            if(segmentDb >= CLIP_DB)
            {
                gc.setFill(on ? CLIP_ON : CLIP_OFF);
            }
            else if(segmentDb >= WARNING_DB)
            {
                gc.setFill(on ? WARNING_ON : WARNING_OFF);
            }
            else
            {
                gc.setFill(on ? NORMAL_ON : NORMAL_OFF);
            }

            gc.fillRoundRect(x * step, 0, SEGMENT_WIDTH, height, 2, 2);
        }
    }
}
