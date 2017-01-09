
/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
 *
 ******************************************************************************/
package map;

import icon.IconManager;
import org.jdesktop.swingx.JXMapViewer;
import org.jdesktop.swingx.painter.AbstractPainter;

import java.awt.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PlottableEntityPainter extends AbstractPainter<JXMapViewer>
{
    private PlottableEntityRenderer mRenderer;
    private Set<PlottableEntity> mEntities = new HashSet<PlottableEntity>();

    public PlottableEntityPainter(IconManager iconManager)
    {
        mRenderer = new PlottableEntityRenderer(iconManager);
        setAntialiasing(true);
        setCacheable(false);
    }

    public void addEntity(PlottableEntity entity)
    {
        mEntities.add(entity);
    }

    public void removeEntity(PlottableEntity entity)
    {
        mEntities.remove(entity);
    }

    public void clearEntities()
    {
        mEntities.clear();
    }

    private Set<PlottableEntity> getEntities()
    {
        return Collections.unmodifiableSet(mEntities);
    }

    @Override
    protected void doPaint(Graphics2D g, JXMapViewer map, int width, int height)
    {
        Rectangle viewportBounds = map.getViewportBounds();

        g.translate(-viewportBounds.getX(), -viewportBounds.getY());

        Set<PlottableEntity> entities = getEntities();

        for(PlottableEntity entity : entities)
        {
            mRenderer.paintPlottableEntity(g, map, entity, true);
        }

        g.translate(viewportBounds.getX(), viewportBounds.getY());
    }
}
