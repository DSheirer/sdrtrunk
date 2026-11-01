/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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
package io.github.dsheirer.spectrum;

import io.github.dsheirer.settings.ColorSetting;
import io.github.dsheirer.settings.ColorSetting.ColorSettingName;
import io.github.dsheirer.settings.Setting;
import io.github.dsheirer.settings.SettingChangeListener;
import io.github.dsheirer.settings.SettingsManager;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.text.DecimalFormat;
import java.util.Arrays;
import org.apache.commons.math3.util.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JPanel;

public class WaterfallPanel extends JPanel implements DFTResultsListener,
    Pausable,
    SettingChangeListener
{
    private static final long serialVersionUID = 1L;

    private final static Logger mLog =
        LoggerFactory.getLogger(WaterfallPanel.class);

    private static DecimalFormat CURSOR_FORMAT = new DecimalFormat("0.00000");
    private static final String PAUSED = "PAUSED - Right Click to Unpause";
    private static final String DISABLED = "DISABLED - Right Click to Select a Tuner";

    private byte[] mPixels;
    private byte[] mPausedPixels;
    private int mDFTSize = 4096;
    private int mImageHeight = 700;
    private IndexColorModel mColorModel = WaterfallColorModel.getDefaultColorModel();
    private Color mColorSpectrumCursor;
    private BufferedImage mVisibleImage;
    private byte[] mVisiblePixels;
    private boolean mIsDisposed = false;

    private Point mCursorLocation = new Point(0, 0);
    private boolean mCursorVisible = false;
    private long mCursorFrequency = 0;
    private boolean mPaused = false;
    private boolean mDisabled = true;
    private int mZoom = 0;
    private int mDFTZoomWindowOffset = 0;

    private SettingsManager mSettingsManager;

    /**
     * Displays a scrolling window of multiple DFT frequency bin outputs over
     * time.  Maps DFT frequency bin decibel values into a 256 bucket color map
     * for display.
     *
     * @param settingsManager
     */
    public WaterfallPanel(SettingsManager settingsManager)
    {
        super();
        mSettingsManager = settingsManager;
        mSettingsManager.addListener(this);
        mColorSpectrumCursor = getColor(ColorSettingName.SPECTRUM_CURSOR);
        reset();
    }

    /**
     * Prepares this instance for disposal
     */
    public void dispose()
    {
        if(mSettingsManager != null)
        {
            mSettingsManager.removeListener(this);
        }

        mSettingsManager = null;
        mIsDisposed = true;
    }

    /**
     * Resets the byte backing arrays when the DFT point size has changed
     */
    private void reset()
    {
        mPixels = new byte[mDFTSize * mImageHeight];

        if(mPaused)
        {
            mPausedPixels = mPixels.clone();
        }

        repaint();
    }

    /**
     * Pausable interface - pauses updates to the waterfall
     */
    public void setPaused(boolean paused)
    {
        if(paused)
        {
            mPausedPixels = mPixels.clone();
        }

        mPaused = paused;

        repaint();
    }

    /**
     * Returns current pause state
     *
     * @return true if paused, false otherwise
     */
    public boolean isPaused()
    {
        return mPaused;
    }

    /**
     * Indicates if the waterfall is currently disabled.
     */
    public boolean isDisabled()
    {
        return mDisabled;
    }

    /**
     * Sets the current zoom level (2^zoom)
     *
     * 0 	No Zoom
     * 1	2x Zoom
     * 2	4x Zoom
     * 3	8x Zoom
     * 4	16x Zoom
     * 5	32x Zoom
     * 6	64x Zoom
     *
     * @param zoom level, 0 - 6.
     */
    public void setZoom(int zoom)
    {
        mZoom = zoom;
    }

    /**
     * Multiplier for the current zoom level
     */
    private int getZoomMultiplier()
    {
        return (int) FastMath.pow(2.0, mZoom);
    }

    /**
     * Sets the zoom window offset from zero
     *
     * @param offset in DFT bins
     */
    public void setZoomWindowOffset(int offset)
    {
        mDFTZoomWindowOffset = offset;
    }

    /**
     * Fetches a named color setting from the settings manager.  If the setting
     * doesn't exist, creates the setting using the defaultColor
     */
    private Color getColor(ColorSettingName name)
    {
        ColorSetting setting = mSettingsManager.getColorSetting(name);

        return setting.getColor();
    }

    /**
     * Monitors for setting changes.  Colors can be changed by external actions
     * and will automatically update in this class
     */
    @Override
    public void settingChanged(Setting setting)
    {
        if(setting instanceof ColorSetting colorSetting)
        {
            if(colorSetting.getColorSettingName() == ColorSettingName.SPECTRUM_CURSOR)
            {
                mColorSpectrumCursor = colorSetting.getColor();
            }
        }
    }

    @Override
    public void settingDeleted(Setting setting)
    { /* Not implemented */ }

    /**
     * Sets the display location of the cursor.  Cursor location monitoring is
     * handled external to this class.
     *
     * @param point
     */
    public void setCursorLocation(Point point)
    {
        mCursorLocation = point;
        repaint();
    }

    /**
     * Sets the current cursor display frequency.  Cursor location frequency
     * monitoring is handled external to this class.
     *
     * @param frequency
     */
    public void setCursorFrequency(long frequency)
    {
        mCursorFrequency = frequency;
    }

    /**
     * Toggles the visibility of the cursor
     *
     * @param visible
     */
    public void setCursorVisible(boolean visible)
    {
        mCursorVisible = visible;
        repaint();
    }

    /**
     * Copies the DFT bins that are visible at the current zoom level and offset into an image that is sized to hold
     * only those bins.  The whole waterfall can't be scaled to the zoomed width and clipped to the panel because the
     * XRender pipeline silently renders nothing once the scaled width of the source image reaches 65,536 pixels.
     *
     * The image's pixel array is accessed directly, so Java2D doesn't cache it and each paint renders the current bins.
     *
     * @param multiplier - current zoom multiplier
     */
    private void updateVisibleImage(int multiplier)
    {
        int bins = mDFTSize / multiplier;

        if(mVisibleImage == null || mVisibleImage.getWidth() != bins)
        {
            mVisibleImage = new BufferedImage(bins, mImageHeight, BufferedImage.TYPE_BYTE_INDEXED, mColorModel);
            mVisiblePixels = ((DataBufferByte)mVisibleImage.getRaster().getDataBuffer()).getData();
        }

        byte[] source = mPaused ? mPausedPixels : mPixels;

        //The display is shifted one bin to the left, to align with the spectrum.  Bins past the end are set to zero.
        int firstBin = Math.min(mDFTZoomWindowOffset + 1, mDFTSize);
        int length = Math.min(bins, mDFTSize - firstBin);

        for(int row = 0; row < mImageHeight; row++)
        {
            int destination = row * bins;
            System.arraycopy(source, (row * mDFTSize) + firstBin, mVisiblePixels, destination, length);
            Arrays.fill(mVisiblePixels, destination + length, destination + bins, (byte)0);
        }
    }

    /**
     * Renders the screen at each refresh
     */
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        updateVisibleImage(getZoomMultiplier());
        g.drawImage(mVisibleImage, 0, 0, getWidth(), mImageHeight, null);
        Graphics2D graphics = (Graphics2D)g;
        graphics.setColor(mColorSpectrumCursor);

        if(mCursorVisible)
        {
            graphics.draw(new Line2D.Float(mCursorLocation.x, 0, mCursorLocation.x, (float)(getSize().getHeight())));
            String frequency = CURSOR_FORMAT.format(mCursorFrequency / 1000000.0D);
            graphics.drawString(frequency, mCursorLocation.x + 5, mCursorLocation.y);
        }

        if(mDisabled)
        {
            graphics.drawString(DISABLED, 20, 20);
        }
        else if(mPaused)
        {
            graphics.drawString(PAUSED, 20, 20);
        }

        paintZoomIndicator(graphics);
        graphics.dispose();
    }

    /**
     * When zoom level is greater than zero, paints a small indicator at the
     * bottom center of the screen showing the location of the zoom window
     * within the overall DFT results window
     */
    private void paintZoomIndicator(Graphics2D graphics)
    {
        if(mZoom != 0)
        {
            int width = getWidth() / 4;
            int x = (getWidth() / 2) - (width / 2);

            //Draw the outer window
            graphics.drawRect(x, getHeight() - 12, width, 10);
            int zoomWidth = width / getZoomMultiplier();
            int windowOffset = 0;

            if(mDFTZoomWindowOffset != 0)
            {
                windowOffset = (int)(((double)mDFTZoomWindowOffset / (double)mDFTSize) * width);
            }

            //Draw the zoom window
            graphics.fillRect(x + windowOffset, getHeight() - 12, zoomWidth, 10);

            //Draw the zoom text
            graphics.drawString("Zoom: " + getZoomMultiplier() + "x", x + width + 3, getHeight() - 2);
        }
    }

    /**
     * Implements the DFT results listener interface method.  This is the primary method for receiving new frequency bin results.
     */
    @Override
    public void receive(float[] update)
    {
        mDisabled = false;

        byte[] newPixels = new byte[update.length];

        /**
         * Find the average value and scale the display to it
         */
        double sum = 0.0d;

        for(int x = 0; x < update.length - 1; x++)
        {
            sum += update[x];
        }

        float average = (float)(sum / (double)update.length - 1);
        float scale = 256.0f / average;

        for(int x = 0; x < update.length - 1; x++)
        {
            float value = (average - update[x]) * scale;

            if(value < 0)
            {
                newPixels[x] = 0;
            }
            else if(value > 255)
            {
                newPixels[x] = (byte)255;
            }
            else
            {
                newPixels[x] = (byte)value;
            }
        }

        //Task the swing event thread to add the new pixels to the pixel array and update the display
        EventQueue.invokeLater(() -> {
            if(!mIsDisposed)
            {
                //If our FFT size changes, reset our pixel map
                if(mDFTSize != newPixels.length)
                {
                    mDFTSize = newPixels.length;
                    reset();
                }

                //Move the pixels down a row and add in the new pixels row
                System.arraycopy(mPixels, 0, mPixels, mDFTSize, mPixels.length - mDFTSize);
                System.arraycopy(newPixels, 0, mPixels, 0, newPixels.length);
                repaint();
            }
        });
    }

    public void clearWaterfall()
    {
        mDisabled = true;

        EventQueue.invokeLater(() -> {
            Arrays.fill(mPixels, (byte)0);

            if(mPausedPixels != null)
            {
                Arrays.fill(mPausedPixels, (byte)0);
            }

            repaint();
        });
    }
}
