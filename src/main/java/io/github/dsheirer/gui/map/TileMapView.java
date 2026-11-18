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

package io.github.dsheirer.gui.map;

import io.github.dsheirer.map.GeoPosition;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.ZoomEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

/**
 * JavaFX slippy map: tiles from a {@link TileSource} drawn on a canvas, with {@link MapLayer} overlays.  While a tile
 * loads, the matching part of a loaded tile from a farther zoom level stands in for it.  Past the source's highest
 * zoom level, its closest tiles are enlarged.
 *
 * Mouse: drag with the primary button to pan, scroll to zoom at the mouse, double-click or middle-click to center and
 * zoom in.  Keys: arrows pan, plus and minus zoom.  Use on the JavaFX thread.
 */
public class TileMapView extends Region
{
    private static final Color BACKGROUND = Color.rgb(221, 221, 221);
    private static final Color DARK_BACKGROUND = Color.rgb(38, 38, 38);
    private static final Color ATTRIBUTION_BACKGROUND = Color.rgb(255, 255, 255, 0.75);
    private static final Color DARK_ATTRIBUTION_BACKGROUND = Color.rgb(30, 30, 30, 0.75);
    private static final Color ATTRIBUTION_TEXT = Color.rgb(40, 40, 40);
    private static final Color DARK_ATTRIBUTION_TEXT = Color.rgb(210, 210, 210);
    private static final Font ATTRIBUTION_FONT = Font.font(11);
    private static final Font MESSAGE_FONT = Font.font(14);
    private static final int FALLBACK_LEVELS = 5;
    //Scroll distance for one zoom step: one mouse wheel notch, or several trackpad events
    private static final double SCROLL_STEP = 40.0;
    private static final double PINCH_STEP = 1.4;
    private static final double KEY_PAN_PIXELS = 64.0;

    private final Canvas mCanvas = new Canvas();
    private final MapViewport mViewport = new MapViewport();
    private final TileLoader mTileLoader;
    private final List<MapLayer> mLayers = new ArrayList<>();
    private final List<Runnable> mViewListeners = new CopyOnWriteArrayList<>();
    private final ReadOnlyIntegerWrapper mZoom = new ReadOnlyIntegerWrapper(mViewport.getZoom());
    private final Text mMeasure = new Text();
    private TileSource mSource;
    private boolean mRedrawPending;
    private double mDragX = Double.NaN;
    private double mDragY = Double.NaN;
    private double mScrollAccumulator;
    private double mPinchAccumulator = 1.0;

    /**
     * Constructs an instance
     * @param cacheDirectory for the map tiles on disk, or null to not keep tiles on disk
     * @param loadTiles true to download tiles, or false for a blank map (tests and off-screen rendering)
     */
    public TileMapView(Path cacheDirectory, boolean loadTiles)
    {
        mTileLoader = loadTiles ? new TileLoader(cacheDirectory, key -> Platform.runLater(this::requestRedraw)) : null;
        getChildren().add(mCanvas);
        setFocusTraversable(true);
        setMinSize(0, 0);
        setPrefSize(600, 400);

        addEventHandler(MouseEvent.MOUSE_PRESSED, this::mousePressed);
        addEventHandler(MouseEvent.MOUSE_DRAGGED, this::mouseDragged);
        addEventHandler(MouseEvent.MOUSE_RELEASED, this::mouseReleased);
        addEventHandler(MouseEvent.MOUSE_CLICKED, this::mouseClicked);
        addEventHandler(ScrollEvent.SCROLL, this::scrolled);
        addEventHandler(ZoomEvent.ZOOM_STARTED, event -> mPinchAccumulator = 1.0);
        addEventHandler(ZoomEvent.ZOOM, this::pinched);
        addEventHandler(KeyEvent.KEY_PRESSED, this::keyPressed);
    }

    /**
     * Viewport, for converting positions to screen pixels
     */
    public MapViewport getViewport()
    {
        return mViewport;
    }

    /**
     * Sets the tiles to show, dropping the tiles of the previous source
     * @param source of the tiles, or null to show none
     */
    public void setSource(TileSource source)
    {
        mSource = source;

        if(mTileLoader != null)
        {
            mTileLoader.setSource(source);
        }

        requestRedraw();
    }

    /**
     * Source of the tiles, or null
     */
    public TileSource getSource()
    {
        return mSource;
    }

    /**
     * Tile zoom level, updated when the map zooms
     */
    public ReadOnlyIntegerProperty zoomProperty()
    {
        return mZoom.getReadOnlyProperty();
    }

    public int getZoom()
    {
        return mViewport.getZoom();
    }

    /**
     * Sets the tile zoom level, keeping the center point
     */
    public void setZoom(int zoom)
    {
        if(zoom != mViewport.getZoom())
        {
            mViewport.setZoom(zoom);
            viewChanged();
        }
    }

    /**
     * Changes the zoom level, keeping the position under the screen point in place
     */
    public void zoomAt(int zoom, double x, double y)
    {
        if(zoom != mViewport.getZoom())
        {
            mViewport.zoomAt(zoom, x, y);
            viewChanged();
        }
    }

    public GeoPosition getCenter()
    {
        return mViewport.getCenter();
    }

    /**
     * Centers the map on the position
     */
    public void setCenter(GeoPosition position)
    {
        if(position != null)
        {
            mViewport.setCenter(position);
            viewChanged();
        }
    }

    /**
     * Adds a layer over the tiles and the layers added before it
     */
    public void addLayer(MapLayer layer)
    {
        mLayers.add(layer);
        requestRedraw();
    }

    /**
     * Adds a listener that's notified on the JavaFX thread each time the map pans or zooms
     */
    public void addViewListener(Runnable listener)
    {
        mViewListeners.add(listener);
    }

    /**
     * Redraws the map on the next JavaFX pulse.  Can be called often: the requests are combined.
     */
    public void requestRedraw()
    {
        if(!mRedrawPending)
        {
            mRedrawPending = true;
            Platform.runLater(() -> {
                mRedrawPending = false;
                draw();
            });
        }
    }

    /**
     * Stops loading tiles
     */
    public void dispose()
    {
        if(mTileLoader != null)
        {
            mTileLoader.dispose();
        }
    }

    private void viewChanged()
    {
        mZoom.set(mViewport.getZoom());
        requestRedraw();
        mViewListeners.forEach(Runnable::run);
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
            mViewport.setSize(width, height);
            draw();
        }
    }

    /**
     * Draws the tiles and the layers now
     */
    public void draw()
    {
        GraphicsContext gc = mCanvas.getGraphicsContext2D();
        double width = mCanvas.getWidth();
        double height = mCanvas.getHeight();
        boolean dark = mSource != null && mSource.dark();
        gc.setFill(dark ? DARK_BACKGROUND : BACKGROUND);
        gc.fillRect(0, 0, width, height);

        if(width <= 0 || height <= 0)
        {
            return;
        }

        if(mSource != null && mSource.isAvailable())
        {
            drawTiles(gc, width, height);
        }
        else if(mSource != null && mSource.unavailableReason() != null)
        {
            gc.setFill(dark ? DARK_ATTRIBUTION_TEXT : ATTRIBUTION_TEXT);
            gc.setFont(MESSAGE_FONT);
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setTextBaseline(VPos.CENTER);
            gc.fillText(mSource.unavailableReason(), width / 2.0, height / 2.0);
        }

        for(MapLayer layer: mLayers)
        {
            gc.save();
            layer.draw(gc, this);
            gc.restore();
        }

        if(mSource != null)
        {
            drawAttribution(gc, mSource.attribution(), dark, width, height);
        }
    }

    private void drawTiles(GraphicsContext gc, double width, double height)
    {
        int zoom = mViewport.getZoom();
        int count = Mercator.tileCount(zoom);
        double left = mViewport.getLeft();
        double top = mViewport.getTop();
        int firstColumn = (int)Math.floor(left / Mercator.TILE_SIZE);
        int lastColumn = (int)Math.floor((left + width - 1) / Mercator.TILE_SIZE);
        int firstRow = Math.max(0, (int)Math.floor(top / Mercator.TILE_SIZE));
        int lastRow = Math.min(count - 1, (int)Math.floor((top + height - 1) / Mercator.TILE_SIZE));

        //Past the source's highest zoom level, its closest tiles are enlarged
        int enlarged = Math.max(0, zoom - mSource.maxZoom());
        Set<TileKey> wanted = new HashSet<>();
        List<TileKey> missing = new ArrayList<>();

        for(int row = firstRow; row <= lastRow; row++)
        {
            for(int column = firstColumn; column <= lastColumn; column++)
            {
                TileKey key = new TileKey(zoom, Math.floorMod(column, count), row);
                TileKey source = enlarged > 0 ? key.parent(enlarged) : key;
                double x = column * (double)Mercator.TILE_SIZE - left;
                double y = row * (double)Mercator.TILE_SIZE - top;
                wanted.add(source);

                Image image = mTileLoader != null ? mTileLoader.get(source) : null;

                if(image != null)
                {
                    drawPart(gc, image, key, enlarged, x, y);
                }
                else
                {
                    drawFallback(gc, key, enlarged, x, y);

                    if(!missing.contains(source))
                    {
                        missing.add(source);
                    }
                }
            }
        }

        if(mTileLoader != null)
        {
            mTileLoader.setWanted(wanted);

            //Request the tiles nearest the middle last, so they load first
            double scale = 1 << enlarged;
            int sourceCount = Mercator.tileCount(zoom - enlarged);
            double middleColumn = ((left + width / 2.0) / Mercator.TILE_SIZE) / scale - 0.5;
            double middleRow = ((top + height / 2.0) / Mercator.TILE_SIZE) / scale - 0.5;
            missing.sort((a, b) -> Double.compare(distance(b, middleColumn, middleRow, sourceCount),
                distance(a, middleColumn, middleRow, sourceCount)));
            missing.forEach(mTileLoader::request);
        }
    }

    /**
     * Distance in tiles from the middle of the view, allowing for the world repeating east and west
     */
    private static double distance(TileKey key, double column, double row, int count)
    {
        double dx = Math.abs(key.x() - Math.floorMod((long)Math.floor(column), count));
        dx = Math.min(dx, count - dx);
        return Math.hypot(dx, key.y() - row);
    }

    /**
     * Draws the part of a tile from a farther zoom level that covers the tile, enlarged to the tile's size
     * @param image of the tile from the farther zoom level
     * @param key of the tile to cover
     * @param levels between the tiles' zoom levels, 0 to draw the whole image
     */
    private static void drawPart(GraphicsContext gc, Image image, TileKey key, int levels, double x, double y)
    {
        if(levels == 0)
        {
            gc.drawImage(image, x, y, Mercator.TILE_SIZE, Mercator.TILE_SIZE);
            return;
        }

        TileKey parent = key.parent(levels);
        double size = image.getWidth() / (1 << levels);
        double sourceX = (key.x() - ((long)parent.x() << levels)) * size;
        double sourceY = (key.y() - ((long)parent.y() << levels)) * size;
        gc.drawImage(image, sourceX, sourceY, size, size, x, y, Mercator.TILE_SIZE, Mercator.TILE_SIZE);
    }

    /**
     * Draws the part of a loaded tile from a farther zoom level than the requested tile that covers the tile
     * @param enlarged levels between the tile and the requested tile
     */
    private void drawFallback(GraphicsContext gc, TileKey key, int enlarged, double x, double y)
    {
        if(mTileLoader == null)
        {
            return;
        }

        for(int levels = enlarged + 1; levels <= enlarged + FALLBACK_LEVELS && key.zoom() - levels >= 0; levels++)
        {
            Image image = mTileLoader.get(key.parent(levels));

            if(image != null)
            {
                drawPart(gc, image, key, levels, x, y);
                return;
            }
        }
    }

    /**
     * Attribution in the bottom right corner, required by the tile servers
     */
    private void drawAttribution(GraphicsContext gc, String attribution, boolean dark, double width, double height)
    {
        mMeasure.setText(attribution);
        mMeasure.setFont(ATTRIBUTION_FONT);
        double boxWidth = mMeasure.getLayoutBounds().getWidth() + 8;
        double boxHeight = 16;
        gc.setFill(dark ? DARK_ATTRIBUTION_BACKGROUND : ATTRIBUTION_BACKGROUND);
        gc.fillRect(width - boxWidth, height - boxHeight, boxWidth, boxHeight);
        gc.setFill(dark ? DARK_ATTRIBUTION_TEXT : ATTRIBUTION_TEXT);
        gc.setFont(ATTRIBUTION_FONT);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText(attribution, width - boxWidth + 4, height - boxHeight / 2.0);
    }

    private void mousePressed(MouseEvent event)
    {
        requestFocus();

        if(event.getButton() == MouseButton.PRIMARY)
        {
            mDragX = event.getX();
            mDragY = event.getY();
        }
    }

    private void mouseDragged(MouseEvent event)
    {
        if(event.isPrimaryButtonDown() && !Double.isNaN(mDragX))
        {
            mViewport.pan(mDragX - event.getX(), mDragY - event.getY());
            mDragX = event.getX();
            mDragY = event.getY();
            setCursor(Cursor.MOVE);
            viewChanged();
        }
    }

    private void mouseReleased(MouseEvent event)
    {
        if(event.getButton() == MouseButton.PRIMARY)
        {
            mDragX = Double.NaN;
            mDragY = Double.NaN;
            setCursor(Cursor.DEFAULT);
        }
    }

    private void mouseClicked(MouseEvent event)
    {
        boolean doubleClick = event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2 &&
            event.isStillSincePress();

        if(doubleClick || event.getButton() == MouseButton.MIDDLE)
        {
            mViewport.setCenter(mViewport.getPosition(event.getX(), event.getY()));
            mViewport.setZoom(mViewport.getZoom() + 1);
            viewChanged();
        }
    }

    private void scrolled(ScrollEvent event)
    {
        if(event.getDeltaY() == 0)
        {
            return;
        }

        //Restart when the direction changes
        if(Math.signum(event.getDeltaY()) != Math.signum(mScrollAccumulator))
        {
            mScrollAccumulator = 0;
        }

        mScrollAccumulator += event.getDeltaY();
        int steps = (int)(mScrollAccumulator / SCROLL_STEP);

        if(steps != 0)
        {
            mScrollAccumulator -= steps * SCROLL_STEP;
            zoomAt(mViewport.getZoom() + steps, event.getX(), event.getY());
        }

        event.consume();
    }

    private void pinched(ZoomEvent event)
    {
        mPinchAccumulator *= event.getZoomFactor();

        if(mPinchAccumulator >= PINCH_STEP)
        {
            mPinchAccumulator = 1.0;
            zoomAt(mViewport.getZoom() + 1, event.getX(), event.getY());
        }
        else if(mPinchAccumulator <= 1.0 / PINCH_STEP)
        {
            mPinchAccumulator = 1.0;
            zoomAt(mViewport.getZoom() - 1, event.getX(), event.getY());
        }

        event.consume();
    }

    private void keyPressed(KeyEvent event)
    {
        switch(event.getCode())
        {
            case LEFT, KP_LEFT -> pan(-KEY_PAN_PIXELS, 0);
            case RIGHT, KP_RIGHT -> pan(KEY_PAN_PIXELS, 0);
            case UP, KP_UP -> pan(0, -KEY_PAN_PIXELS);
            case DOWN, KP_DOWN -> pan(0, KEY_PAN_PIXELS);
            case PLUS, EQUALS, ADD -> setZoom(getZoom() + 1);
            case MINUS, SUBTRACT -> setZoom(getZoom() - 1);
            default -> {
                return;
            }
        }

        event.consume();
    }

    private void pan(double dx, double dy)
    {
        mViewport.pan(dx, dy);
        viewChanged();
    }
}
