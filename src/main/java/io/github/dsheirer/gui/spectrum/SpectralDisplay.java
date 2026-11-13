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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javafx.animation.AnimationTimer;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

/**
 * JavaFX spectral display with a spectrum (frequency domain) section above a scrolling waterfall section.  Either
 * section can be turned off independently.  Optional layers draw on top of each section to add frequency labels,
 * channels, cursor readouts, etc.
 *
 * Send DFT results in dB to receive(float[]) from any thread.  Set the frequency, sample rate and sample size with
 * the model on the JavaFX application thread.  The display redraws on the JavaFX pulse only when something changed.
 *
 * Mouse: the scroll wheel zooms around the mouse position, a primary button drag pans the zoomed display and the
 * secondary button opens a context menu with each layer's menu items.
 */
public class SpectralDisplay extends StackPane
{
    public static final int AVERAGING_MINIMUM = SpectrumAverager.AVERAGING_MINIMUM;
    public static final int AVERAGING_MAXIMUM = SpectrumAverager.AVERAGING_MAXIMUM;
    public static final int AVERAGING_DEFAULT = SpectrumAverager.AVERAGING_DEFAULT;
    private static final int WATERFALL_MIN_ROWS = 256;
    private static final int WATERFALL_MAX_ROWS = 4096;
    private static final int WATERFALL_QUEUE_LIMIT = 256;
    //Below the smallest maximum texture size of common GPUs (8192)
    private static final int WATERFALL_TILE_WIDTH = 4096;
    private static final Font MESSAGE_FONT = Font.font(13);
    private static final Font ZOOM_FONT = Font.font(11);

    private final SpectralDisplayModel mModel = new SpectralDisplayModel();
    private final ObjectProperty<SpectralDisplayTheme> mTheme =
            new SimpleObjectProperty<>(SpectralDisplayTheme.getDefault());
    private final List<SpectralDisplayLayer> mLayers = new CopyOnWriteArrayList<>();
    private final SplitPane mSplitPane = new SplitPane();
    private final CanvasPane mSpectrumPane = new CanvasPane(DisplaySection.SPECTRUM);
    private final CanvasPane mWaterfallPane = new CanvasPane(DisplaySection.WATERFALL);
    private final StackPane mDisabledPane = new StackPane();

    private final BooleanProperty mSpectrumEnabled = new SimpleBooleanProperty(true);
    private final BooleanProperty mWaterfallEnabled = new SimpleBooleanProperty(true);
    private final BooleanProperty mWaterfallPaused = new SimpleBooleanProperty(false);
    private final IntegerProperty mAveraging = new SimpleIntegerProperty(AVERAGING_DEFAULT);
    private final BooleanProperty mPeakHold = new SimpleBooleanProperty(false);
    private final DoubleProperty mDividerPosition = new SimpleDoubleProperty(0.5);
    private final StringProperty mMessage = new SimpleStringProperty();

    //Values read on the DFT results thread
    private volatile boolean mSpectrumActive = true;
    private volatile boolean mWaterfallActive = true;
    private volatile boolean mWaterfallPausedFlag = false;
    private final SpectrumAverager mAverager = new SpectrumAverager();

    //Hand off from the DFT results thread to the JavaFX thread
    private final AtomicReference<SpectrumAverager.Frame> mLatestSpectrum = new AtomicReference<>();
    private final ConcurrentLinkedQueue<byte[]> mWaterfallRows = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean mRenderRequested = new AtomicBoolean(true);
    private final AtomicBoolean mClearRequested = new AtomicBoolean();

    //JavaFX thread state
    private final AnimationTimer mAnimationTimer;
    private SpectrumAverager.Frame mSpectrum;
    //Waterfall image split into side by side tiles, since GPUs limit texture width (as low as 8192 pixels)
    private WritableImage[] mWaterfallTiles;
    private int mWaterfallWidth;
    private int mWaterfallHeight;
    private byte[] mWaterfallIndexes;
    private int[] mWaterfallPixels;
    private int mWaterfallWriteRow = 0;
    private double[] mPolygonX = new double[0];
    private double[] mPolygonY = new double[0];
    private DisplayCursor mCursor;
    private ContextMenu mContextMenu;
    private double mDragStartX;
    private double mDragStartOffset;
    private boolean mDragging;
    private double mScrollAccumulator;
    private boolean mDisposed;

    /**
     * Constructs an instance.
     * @param layers to draw on the display, in drawing order
     */
    public SpectralDisplay(SpectralDisplayLayer... layers)
    {
        mSplitPane.setOrientation(Orientation.VERTICAL);
        mSplitPane.getStyleClass().add("spectral-display-split-pane");
        java.net.URL stylesheet = SpectralDisplay.class.getResource("/spectral_display.css");

        if(stylesheet != null)
        {
            getStylesheets().add(stylesheet.toExternalForm());
        }

        Label disabledLabel = new Label("Spectrum and waterfall are turned off");
        mDisabledPane.getChildren().add(disabledLabel);
        mTheme.addListener((ob, ov, nv) -> themeChanged(disabledLabel));
        themeChanged(disabledLabel);
        getChildren().addAll(mDisabledPane, mSplitPane);
        setMinSize(0, 0);

        mSpectrumEnabled.addListener((ob, ov, nv) -> updateSections());
        mWaterfallEnabled.addListener((ob, ov, nv) -> updateSections());
        mWaterfallPaused.addListener((ob, ov, nv) -> {
            mWaterfallPausedFlag = nv;
            requestRender();
        });
        mMessage.addListener((ob, ov, nv) -> requestRender());
        mAveraging.addListener((ob, ov, nv) -> mAverager.setAveraging(nv.intValue()));
        mPeakHold.addListener((ob, ov, nv) -> {
            mAverager.setPeakHold(nv);
            requestRender();
        });
        mModel.centerFrequencyProperty().addListener((ob, ov, nv) -> {
            mAverager.resetPeak();
            requestRender();
        });
        mModel.sampleRateProperty().addListener((ob, ov, nv) -> {
            mAverager.resetPeak();
            requestRender();
        });
        mModel.zoomProperty().addListener((ob, ov, nv) -> requestRender());
        mModel.zoomOffsetProperty().addListener((ob, ov, nv) -> requestRender());
        mModel.binCountProperty().addListener((ob, ov, nv) -> requestRender());
        mModel.sampleSizeProperty().addListener((ob, ov, nv) -> requestRender());
        mModel.noiseBandwidthProperty().addListener((ob, ov, nv) -> requestRender());

        //Track user changes to the divider position so it can be restored after the waterfall is turned off and on
        mSplitPane.getDividers().addListener((javafx.collections.ListChangeListener<SplitPane.Divider>)change -> {
            while(change.next())
            {
                for(SplitPane.Divider divider: change.getAddedSubList())
                {
                    divider.positionProperty().addListener((ob, ov, nv) -> {
                        if(mSplitPane.getItems().size() == 2)
                        {
                            mDividerPosition.set(nv.doubleValue());
                        }
                    });
                }
            }
        });

        updateSections();

        for(SpectralDisplayLayer layer: layers)
        {
            addLayer(layer);
        }

        mAnimationTimer = new AnimationTimer()
        {
            @Override
            public void handle(long now)
            {
                pulse();
            }
        };
        mAnimationTimer.start();
    }

    /**
     * Frequency and zoom model.  Access on the JavaFX application thread.
     */
    public SpectralDisplayModel getModel()
    {
        return mModel;
    }

    /**
     * Color theme for the display, its layers and the waterfall.  JavaFX thread only.
     */
    public ObjectProperty<SpectralDisplayTheme> themeProperty()
    {
        return mTheme;
    }

    /**
     * Current color theme.  JavaFX thread only.
     */
    public SpectralDisplayTheme getTheme()
    {
        SpectralDisplayTheme theme = mTheme.get();
        return theme != null ? theme : SpectralDisplayTheme.getDefault();
    }

    /**
     * Applies a new theme: recolors the placeholder and the waterfall history, then redraws.
     */
    private void themeChanged(Label disabledLabel)
    {
        SpectralDisplayTheme theme = getTheme();
        mDisabledPane.setBackground(new Background(new BackgroundFill(theme.getBackground(), null, null)));
        disabledLabel.setTextFill(theme.getLine());
        repaintWaterfallImage();
        requestRender();
    }

    /**
     * Adds a layer that draws after the layers already added.
     */
    public void addLayer(SpectralDisplayLayer layer)
    {
        if(layer != null && !mLayers.contains(layer))
        {
            mLayers.add(layer);
            layer.attach(this);
            requestRender();
        }
    }

    /**
     * Removes the layer from the display.
     */
    public void removeLayer(SpectralDisplayLayer layer)
    {
        if(mLayers.remove(layer))
        {
            layer.detach();
            requestRender();
        }
    }

    /**
     * Layers in drawing order.
     */
    public List<SpectralDisplayLayer> getLayers()
    {
        return new ArrayList<>(mLayers);
    }

    /**
     * Turns the spectrum section on or off.
     */
    public BooleanProperty spectrumEnabledProperty()
    {
        return mSpectrumEnabled;
    }

    /**
     * Turns the waterfall section on or off.
     */
    public BooleanProperty waterfallEnabledProperty()
    {
        return mWaterfallEnabled;
    }

    /**
     * Pauses the waterfall scrolling.  New DFT results are dropped while paused.
     */
    public BooleanProperty waterfallPausedProperty()
    {
        return mWaterfallPaused;
    }

    /**
     * Spectrum trace averaging, from AVERAGING_MINIMUM (fast, no averaging) to AVERAGING_MAXIMUM (slow).
     * @see SpectrumAverager
     */
    public IntegerProperty averagingProperty()
    {
        return mAveraging;
    }

    /**
     * Shows a peak hold trace: the highest recent value of each bin, falling slowly.
     */
    public BooleanProperty peakHoldProperty()
    {
        return mPeakHold;
    }

    /**
     * Divider position (0 - 1.0) between the spectrum and the waterfall when both are showing.
     */
    public DoubleProperty dividerPositionProperty()
    {
        return mDividerPosition;
    }

    /**
     * Message shown in the middle of the display, for example when no tuner is selected.  Null for no message.
     */
    public StringProperty messageProperty()
    {
        return mMessage;
    }

    /**
     * Mouse cursor over the display, or null when the mouse is not over the display.  JavaFX thread only.
     */
    public DisplayCursor getDisplayCursor()
    {
        return mCursor;
    }

    /**
     * Asks the display to redraw on the next pulse.  Safe to call from any thread.
     */
    public void requestRender()
    {
        mRenderRequested.set(true);
    }

    /**
     * Clears the spectrum trace, the averaging history and the waterfall.  Safe to call from any thread.
     */
    public void clear()
    {
        mAverager.reset();

        mLatestSpectrum.set(null);
        mWaterfallRows.clear();
        mClearRequested.set(true);
        requestRender();
    }

    /**
     * Receives DFT results in dB (0 dB is full scale) in display order.  Safe to call from any thread.
     */
    public void receive(float[] bins)
    {
        if(bins == null || bins.length == 0 || mDisposed)
        {
            return;
        }

        //The first DFT results on startup can be NaN or infinite
        if(Float.isNaN(bins[0]) || Float.isInfinite(bins[0]))
        {
            bins = new float[bins.length];
        }

        if(mSpectrumActive)
        {
            mLatestSpectrum.set(mAverager.process(bins));
        }

        if(mWaterfallActive && !mWaterfallPausedFlag)
        {
            mWaterfallRows.offer(toWaterfallRow(bins));

            while(mWaterfallRows.size() > WATERFALL_QUEUE_LIMIT)
            {
                mWaterfallRows.poll();
            }
        }
    }

    /**
     * Stops rendering and releases the layers and listeners.
     */
    public void dispose()
    {
        mDisposed = true;
        mAnimationTimer.stop();

        for(SpectralDisplayLayer layer: mLayers)
        {
            layer.detach();
            layer.dispose();
        }

        mLayers.clear();
        mLatestSpectrum.set(null);
        mWaterfallRows.clear();
        mWaterfallTiles = null;
        mWaterfallIndexes = null;
    }

    /**
     * Converts the DFT results to a row of waterfall intensity indexes (0 - 255).  Each bin is scaled relative to the
     * average bin value so the noise floor stays at the bottom of the palette regardless of tuner gain.
     */
    private static byte[] toWaterfallRow(float[] bins)
    {
        double sum = 0.0;

        for(float bin: bins)
        {
            sum += bin;
        }

        float average = (float)(sum / bins.length) - 1.0f;
        float scale = 256.0f / average;
        byte[] row = new byte[bins.length];

        for(int x = 0; x < bins.length; x++)
        {
            int value = (int)((average - bins[x]) * scale);
            row[x] = (byte)Math.max(0, Math.min(255, value));
        }

        return row;
    }

    /**
     * Updates the split pane to show the enabled sections.
     */
    private void updateSections()
    {
        boolean spectrum = mSpectrumEnabled.get();
        boolean waterfall = mWaterfallEnabled.get();
        mSpectrumActive = spectrum;
        mWaterfallActive = waterfall;

        List<Pane> items = new ArrayList<>();

        if(spectrum)
        {
            items.add(mSpectrumPane);
        }
        else
        {
            mLatestSpectrum.set(null);
            mSpectrum = null;
        }

        if(waterfall)
        {
            items.add(mWaterfallPane);
        }
        else
        {
            mWaterfallRows.clear();
        }

        mSplitPane.getItems().setAll(items);
        mSplitPane.setVisible(!items.isEmpty());

        if(items.size() == 2)
        {
            mSplitPane.setDividerPositions(mDividerPosition.get());
        }

        if(mCursor != null && ((mCursor.section() == DisplaySection.SPECTRUM && !spectrum) ||
                (mCursor.section() == DisplaySection.WATERFALL && !waterfall)))
        {
            mCursor = null;
        }

        requestRender();
    }

    /**
     * Called each JavaFX pulse to move new data to the display and redraw when needed.
     */
    private void pulse()
    {
        boolean render = mRenderRequested.getAndSet(false);

        if(mClearRequested.getAndSet(false))
        {
            mSpectrum = null;

            if(mWaterfallTiles != null)
            {
                mWaterfallTiles = null;
                mWaterfallIndexes = null;
                mWaterfallWriteRow = 0;
            }

            render = true;
        }

        SpectrumAverager.Frame spectrum = mLatestSpectrum.getAndSet(null);

        if(spectrum != null)
        {
            mSpectrum = spectrum;
            mModel.setBinCount(spectrum.average().length);
            render = true;
        }

        byte[] row;
        boolean waterfallUpdated = false;

        while((row = mWaterfallRows.poll()) != null)
        {
            if(!mWaterfallPaused.get())
            {
                mModel.setBinCount(row.length);
                writeWaterfallRow(row);
                waterfallUpdated = true;
            }
        }

        if(render || waterfallUpdated)
        {
            renderSpectrum();
            renderWaterfall();
        }
    }

    /**
     * Writes the row into the waterfall ring buffer.  The newest row is at mWaterfallWriteRow and older rows follow
     * it, wrapping around the end of the image.  The intensity indexes are kept alongside the image so the history
     * can be recolored when the theme changes.
     */
    private void writeWaterfallRow(byte[] row)
    {
        int rows = getWaterfallRowsNeeded();

        if(mWaterfallTiles == null || mWaterfallWidth != row.length)
        {
            mWaterfallIndexes = new byte[row.length * rows];
            createWaterfallTiles(row.length, rows);
            mWaterfallWriteRow = 0;
            repaintWaterfallImage();
        }
        else if(mWaterfallHeight < rows)
        {
            growWaterfall(rows);
        }

        int width = row.length;
        mWaterfallWriteRow = (mWaterfallWriteRow - 1 + mWaterfallHeight) % mWaterfallHeight;
        System.arraycopy(row, 0, mWaterfallIndexes, mWaterfallWriteRow * width, width);
        writeWaterfallPixels(mWaterfallWriteRow);
    }

    /**
     * Creates the waterfall image tiles, each at most WATERFALL_TILE_WIDTH wide.
     */
    private void createWaterfallTiles(int width, int height)
    {
        int count = (width + WATERFALL_TILE_WIDTH - 1) / WATERFALL_TILE_WIDTH;
        mWaterfallTiles = new WritableImage[count];

        for(int tile = 0; tile < count; tile++)
        {
            int tileWidth = Math.min(WATERFALL_TILE_WIDTH, width - (tile * WATERFALL_TILE_WIDTH));
            mWaterfallTiles[tile] = new WritableImage(tileWidth, height);
        }

        mWaterfallWidth = width;
        mWaterfallHeight = height;
    }

    /**
     * Colors one row of the waterfall image tiles from the stored intensity indexes.
     */
    private void writeWaterfallPixels(int imageRow)
    {
        int width = mWaterfallWidth;

        if(mWaterfallPixels == null || mWaterfallPixels.length != width)
        {
            mWaterfallPixels = new int[width];
        }

        SpectralDisplayTheme theme = getTheme();
        int offset = imageRow * width;

        for(int x = 0; x < width; x++)
        {
            mWaterfallPixels[x] = theme.getWaterfallColor(mWaterfallIndexes[offset + x]);
        }

        for(int tile = 0; tile < mWaterfallTiles.length; tile++)
        {
            WritableImage image = mWaterfallTiles[tile];
            mWaterfallTiles[tile].getPixelWriter().setPixels(0, imageRow, (int)image.getWidth(), 1,
                    PixelFormat.getIntArgbInstance(), mWaterfallPixels, tile * WATERFALL_TILE_WIDTH, width);
        }
    }

    /**
     * Recolors the whole waterfall image from the stored intensity indexes using the current theme.
     */
    private void repaintWaterfallImage()
    {
        if(mWaterfallTiles != null && mWaterfallIndexes != null)
        {
            for(int y = 0; y < mWaterfallHeight; y++)
            {
                writeWaterfallPixels(y);
            }

            requestRender();
        }
    }

    /**
     * Number of waterfall history rows to keep, based on the height of the waterfall section.
     */
    private int getWaterfallRowsNeeded()
    {
        int rows = (int)Math.ceil(mWaterfallPane.getHeight());
        return Math.max(WATERFALL_MIN_ROWS, Math.min(WATERFALL_MAX_ROWS, rows));
    }

    /**
     * Replaces the waterfall with a taller one and copies the history in newest to oldest order.
     */
    private void growWaterfall(int rows)
    {
        int width = mWaterfallWidth;
        int previousHeight = mWaterfallHeight;
        byte[] indexes = new byte[width * rows];

        for(int y = 0; y < previousHeight; y++)
        {
            int source = (mWaterfallWriteRow + y) % previousHeight;
            System.arraycopy(mWaterfallIndexes, source * width, indexes, y * width, width);
        }

        mWaterfallIndexes = indexes;
        createWaterfallTiles(width, rows);
        mWaterfallWriteRow = 0;
        repaintWaterfallImage();
    }

    /**
     * Height of the spectrum plot area above the inset reserved by the layers.
     */
    private double getSpectrumInset()
    {
        double inset = 0;

        for(SpectralDisplayLayer layer: mLayers)
        {
            inset = Math.max(inset, layer.getSpectrumBottomInset());
        }

        return inset;
    }

    private void renderSpectrum()
    {
        if(!mSpectrumEnabled.get())
        {
            return;
        }

        Canvas canvas = mSpectrumPane.getCanvas();
        double width = canvas.getWidth();
        double height = canvas.getHeight();

        if(width < 1 || height < 1)
        {
            return;
        }

        GraphicsContext gc = canvas.getGraphicsContext2D();
        double plotHeight = Math.max(0, height - getSpectrumInset());
        RenderArea area = new RenderArea(width, height, plotHeight);

        gc.setFill(getTheme().getBackground());
        gc.fillRect(0, 0, width, height);

        for(SpectralDisplayLayer layer: mLayers)
        {
            gc.save();
            layer.renderSpectrumBackground(gc, area);
            gc.restore();
        }

        if(mSpectrum != null && mModel.hasSource() && plotHeight > 0)
        {
            renderTrace(gc, area, mSpectrum.average());

            if(mSpectrum.peak() != null && mPeakHold.get())
            {
                renderPeak(gc, area, mSpectrum.peak());
            }
        }

        gc.setStroke(getTheme().getLine());
        gc.setLineWidth(1.0);
        gc.strokeLine(0, Math.floor(plotHeight) + 0.5, width, Math.floor(plotHeight) + 0.5);

        for(SpectralDisplayLayer layer: mLayers)
        {
            gc.save();
            layer.renderSpectrumForeground(gc, area);
            gc.restore();
        }

        renderZoomIndicator(gc, 8, 8);
        renderMessage(gc, area);
    }

    /**
     * Draws the filled spectrum trace.
     */
    private void renderTrace(GraphicsContext gc, RenderArea area, float[] bins)
    {
        double width = area.width();
        double plotHeight = area.plotHeight();
        int count = buildTracePoints(bins, width, plotHeight);

        LinearGradient gradient = new LinearGradient(0, plotHeight / 2.0, 0, area.height(), false,
                CycleMethod.NO_CYCLE, new Stop(0, getTheme().getGradientTop()),
                new Stop(1, getTheme().getGradientBottom()));

        gc.save();
        gc.beginPath();
        gc.rect(0, 0, width, plotHeight);
        gc.clip();
        gc.setFill(gradient);
        gc.fillPolygon(mPolygonX, mPolygonY, count);
        gc.setStroke(getTheme().getTrace());
        gc.setLineWidth(1.0);
        strokeTracePoints(gc, count);
        gc.restore();
    }

    /**
     * Draws the peak hold trace as a line.
     */
    private void renderPeak(GraphicsContext gc, RenderArea area, float[] bins)
    {
        double width = area.width();
        double plotHeight = area.plotHeight();
        int count = buildTracePoints(bins, width, plotHeight);
        Color cursor = getTheme().getCursor();

        gc.save();
        gc.beginPath();
        gc.rect(0, 0, width, plotHeight);
        gc.clip();
        gc.setStroke(Color.color(cursor.getRed(), cursor.getGreen(), cursor.getBlue(), 0.75));
        gc.setLineWidth(1.0);
        strokeTracePoints(gc, count);
        gc.restore();
    }

    /**
     * Strokes the trace points, skipping the two base points so the line only follows the trace.
     */
    private void strokeTracePoints(GraphicsContext gc, int count)
    {
        gc.beginPath();
        gc.moveTo(mPolygonX[1], mPolygonY[1]);

        for(int x = 2; x < count - 1; x++)
        {
            gc.lineTo(mPolygonX[x], mPolygonY[x]);
        }

        gc.stroke();
    }

    /**
     * Fills the polygon point arrays with the trace for the visible bins, starting and ending with points on the
     * bottom of the plot.  When there are more visible bins than pixels, each pixel column uses the largest bin value
     * in that column so narrow signals stay visible.
     * @return number of points
     */
    private int buildTracePoints(float[] bins, double width, double plotHeight)
    {
        double scale = plotHeight / mModel.getDynamicRange();
        double offset = mModel.getZoomOffset();
        int visible = mModel.getVisibleBinCount();
        double pixelsPerBin = mModel.getPixelsPerBin(width);
        int count;

        if(pixelsPerBin >= 1.0)
        {
            int first = Math.max(0, (int)Math.floor(offset) - 1);
            int last = Math.min(bins.length - 1, (int)Math.ceil(offset + visible) + 1);
            ensurePolygonCapacity(last - first + 5);
            count = 0;
            mPolygonX[count] = 0;
            mPolygonY[count++] = plotHeight;

            for(int bin = first; bin <= last; bin++)
            {
                mPolygonX[count] = (bin - offset + 0.5) * pixelsPerBin;
                mPolygonY[count++] = getY(bins[bin], scale, plotHeight);
            }
        }
        else
        {
            int columns = (int)Math.ceil(width);
            ensurePolygonCapacity(columns + 5);
            count = 0;
            mPolygonX[count] = 0;
            mPolygonY[count++] = plotHeight;
            double binsPerPixel = 1.0 / pixelsPerBin;

            for(int column = 0; column < columns; column++)
            {
                int start = (int)Math.floor(offset + (column * binsPerPixel));
                int end = (int)Math.floor(offset + ((column + 1) * binsPerPixel));
                start = Math.max(0, Math.min(bins.length - 1, start));
                end = Math.max(start + 1, Math.min(bins.length, end));
                float max = bins[start];

                for(int bin = start + 1; bin < end; bin++)
                {
                    if(bins[bin] > max)
                    {
                        max = bins[bin];
                    }
                }

                mPolygonX[count] = column + 0.5;
                mPolygonY[count++] = getY(max, scale, plotHeight);
            }
        }

        mPolygonX[count] = width;
        mPolygonY[count++] = plotHeight;
        return count;
    }

    private static double getY(float decibels, double scale, double plotHeight)
    {
        double y = -decibels * scale;
        return Math.max(0, Math.min(plotHeight, y));
    }

    private void ensurePolygonCapacity(int size)
    {
        if(mPolygonX.length < size)
        {
            mPolygonX = new double[size];
            mPolygonY = new double[size];
        }
    }

    private void renderWaterfall()
    {
        if(!mWaterfallEnabled.get())
        {
            return;
        }

        Canvas canvas = mWaterfallPane.getCanvas();
        double width = canvas.getWidth();
        double height = canvas.getHeight();

        if(width < 1 || height < 1)
        {
            return;
        }

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(getTheme().getWaterfallBackground());
        gc.fillRect(0, 0, width, height);

        WritableImage[] tiles = mWaterfallTiles;

        if(tiles != null && mModel.hasSource() && mWaterfallWidth == mModel.getBinCount())
        {
            double visibleStart = mModel.getZoomOffset();
            double visibleEnd = visibleStart + mModel.getVisibleBinCount();
            double pixelsPerBin = mModel.getPixelsPerBin(width);
            gc.setImageSmoothing(pixelsPerBin < 1.0);

            //Newest rows run from the write row to the bottom of the image, then older rows wrap to the top
            double firstRows = Math.min(mWaterfallHeight - mWaterfallWriteRow, height);
            double secondRows = Math.min(mWaterfallWriteRow, height - firstRows);

            for(int tile = 0; tile < tiles.length; tile++)
            {
                double tileStart = tile * WATERFALL_TILE_WIDTH;
                double start = Math.max(visibleStart, tileStart);
                double end = Math.min(visibleEnd, tileStart + tiles[tile].getWidth());

                if(start >= end)
                {
                    continue;
                }

                double sourceX = start - tileStart;
                double sourceWidth = end - start;
                double destinationX = (start - visibleStart) * pixelsPerBin;
                double destinationWidth = ((end - visibleStart) * pixelsPerBin) - destinationX;

                gc.drawImage(tiles[tile], sourceX, mWaterfallWriteRow, sourceWidth, firstRows, destinationX, 0,
                        destinationWidth, firstRows);

                if(secondRows > 0)
                {
                    gc.drawImage(tiles[tile], sourceX, 0, sourceWidth, secondRows, destinationX, firstRows,
                            destinationWidth, secondRows);
                }
            }
        }

        RenderArea area = new RenderArea(width, height, height);

        for(SpectralDisplayLayer layer: mLayers)
        {
            gc.save();
            layer.renderWaterfall(gc, area);
            gc.restore();
        }

        if(mWaterfallPaused.get())
        {
            gc.setFill(getTheme().getCursor());
            gc.setFont(MESSAGE_FONT);
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setTextBaseline(VPos.TOP);
            gc.fillText("PAUSED", width / 2.0, 8);
        }

        if(!mSpectrumEnabled.get())
        {
            renderZoomIndicator(gc, 8, 8);
            renderMessage(gc, area);
        }
    }

    /**
     * Draws a small overview bar that shows the zoom window position within the full bandwidth.
     */
    private void renderZoomIndicator(GraphicsContext gc, double x, double y)
    {
        if(mModel.getZoom() == 0 || y < 0)
        {
            return;
        }

        double width = 80;
        double height = 8;
        double windowX = x + (mModel.getZoomOffset() / mModel.getBinCount() * width);
        double windowWidth = Math.max(2, width / mModel.getZoomMultiplier());

        gc.setFill(getTheme().getBackground(0.7));
        gc.fillRoundRect(x - 4, y - 4, width + 68, height + 8, 6, 6);
        gc.setStroke(getTheme().getLine());
        gc.setFill(getTheme().getCursor());
        gc.setLineWidth(1.0);
        gc.strokeRect(x + 0.5, y + 0.5, width, height);
        gc.fillRect(windowX + 0.5, y + 0.5, windowWidth, height);
        gc.setFill(getTheme().getLine());
        gc.setFont(ZOOM_FONT);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText("Zoom " + mModel.getZoomMultiplier() + "x", x + width + 6, y + (height / 2.0) + 0.5);
    }

    private void renderMessage(GraphicsContext gc, RenderArea area)
    {
        String message = mMessage.get();

        if(message != null && !message.isEmpty())
        {
            gc.setFill(getTheme().getCursor());
            gc.setFont(MESSAGE_FONT);
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setTextBaseline(VPos.CENTER);
            gc.fillText(message, area.width() / 2.0, area.plotHeight() / 2.0);
        }
    }

    /**
     * Updates the mouse cursor position.
     */
    private void updateCursor(DisplaySection section, double x, double y, double width)
    {
        mCursor = new DisplayCursor(section, x, y, mModel.getFrequency(x, width));
        requestRender();
    }

    private void handleMouse(CanvasPane pane, MouseEvent event)
    {
        double width = pane.getWidth();

        if(event.getEventType() == MouseEvent.MOUSE_MOVED)
        {
            updateCursor(pane.getSection(), event.getX(), event.getY(), width);
        }
        else if(event.getEventType() == MouseEvent.MOUSE_EXITED)
        {
            if(!mDragging)
            {
                mCursor = null;
                requestRender();
            }
        }
        else if(event.getEventType() == MouseEvent.MOUSE_PRESSED)
        {
            if(mContextMenu != null)
            {
                mContextMenu.hide();
            }

            if(event.getButton() == MouseButton.PRIMARY)
            {
                mDragStartX = event.getX();
                mDragStartOffset = mModel.getZoomOffset();
            }
        }
        else if(event.getEventType() == MouseEvent.MOUSE_DRAGGED)
        {
            if(event.getButton() == MouseButton.PRIMARY && mModel.getZoom() > 0)
            {
                mDragging = true;
                pane.setCursor(Cursor.CLOSED_HAND);
                double bins = (mDragStartX - event.getX()) / mModel.getPixelsPerBin(width);
                mModel.setZoomOffset(mDragStartOffset + bins);
            }

            updateCursor(pane.getSection(), event.getX(), event.getY(), width);
        }
        else if(event.getEventType() == MouseEvent.MOUSE_RELEASED)
        {
            if(mDragging)
            {
                mDragging = false;
                pane.setCursor(Cursor.CROSSHAIR);
            }
        }
    }

    private void handleScroll(CanvasPane pane, ScrollEvent event)
    {
        double notch = event.getMultiplierY() > 0 ? event.getMultiplierY() : 40.0;
        mScrollAccumulator += event.getDeltaY();
        int steps = (int)(mScrollAccumulator / notch);

        if(steps != 0)
        {
            mScrollAccumulator -= steps * notch;
            double width = pane.getWidth();
            long frequency = mModel.getFrequency(event.getX(), width);
            mModel.setZoom(mModel.getZoom() + steps, frequency, width > 0 ? event.getX() / width : 0.5);
            updateCursor(pane.getSection(), event.getX(), event.getY(), width);
        }

        event.consume();
    }

    private void showContextMenu(CanvasPane pane, ContextMenuEvent event)
    {
        if(mContextMenu != null)
        {
            mContextMenu.hide();
        }

        double width = pane.getWidth();
        long frequency = mModel.getFrequency(event.getX(), width);
        double xFraction = width > 0 ? event.getX() / width : 0.5;
        ContextMenu menu = new ContextMenu();

        for(SpectralDisplayLayer layer: mLayers)
        {
            List<MenuItem> items = layer.getContextMenuItems(pane.getSection(), frequency);

            if(items != null && !items.isEmpty())
            {
                menu.getItems().addAll(items);
                menu.getItems().add(new SeparatorMenuItem());
            }
        }

        Menu zoomMenu = new Menu("Zoom");
        ToggleGroup zoomGroup = new ToggleGroup();

        for(int zoom = SpectralDisplayModel.MIN_ZOOM; zoom <= SpectralDisplayModel.MAX_ZOOM; zoom++)
        {
            final int level = zoom;
            RadioMenuItem item = new RadioMenuItem((1 << zoom) + "x");
            item.setToggleGroup(zoomGroup);
            item.setSelected(mModel.getZoom() == zoom);
            item.setOnAction(e -> mModel.setZoom(level, frequency, xFraction));
            zoomMenu.getItems().add(item);
        }

        menu.getItems().add(zoomMenu);

        if(mWaterfallEnabled.get())
        {
            CheckMenuItem pause = new CheckMenuItem("Pause Waterfall");
            pause.setSelected(mWaterfallPaused.get());
            pause.setOnAction(e -> mWaterfallPaused.set(pause.isSelected()));
            menu.getItems().add(pause);
        }

        mContextMenu = menu;
        menu.show(pane, event.getScreenX(), event.getScreenY());
        event.consume();
    }

    /**
     * Resizable pane that holds a canvas for one display section.
     */
    private class CanvasPane extends Pane
    {
        private final Canvas mCanvas = new Canvas();
        private final DisplaySection mSection;

        CanvasPane(DisplaySection section)
        {
            mSection = section;
            getChildren().add(mCanvas);
            setMinSize(0, 20);
            setPrefSize(400, 150);
            setCursor(Cursor.CROSSHAIR);
            addEventHandler(MouseEvent.ANY, event -> handleMouse(this, event));
            addEventHandler(ScrollEvent.SCROLL, event -> handleScroll(this, event));
            addEventHandler(ContextMenuEvent.CONTEXT_MENU_REQUESTED, event -> showContextMenu(this, event));
        }

        Canvas getCanvas()
        {
            return mCanvas;
        }

        DisplaySection getSection()
        {
            return mSection;
        }

        @Override
        protected void layoutChildren()
        {
            double width = Math.floor(getWidth());
            double height = Math.floor(getHeight());

            if(mCanvas.getWidth() != width || mCanvas.getHeight() != height)
            {
                mCanvas.setWidth(width);
                mCanvas.setHeight(height);
                requestRender();
            }
        }
    }
}
