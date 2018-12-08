
/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */
package io.github.dsheirer.map;

import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.icon.IconManager;
import org.jdesktop.swingx.JXMapViewer;
import org.jdesktop.swingx.painter.AbstractPainter;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PlottableEntityPainter extends AbstractPainter<JXMapViewer>
{
    private PlottableEntityRenderer mRenderer;
    private Set<PlottableEntityHistory> mEntities = new HashSet<>();

    public PlottableEntityPainter(AliasModel aliasModel, IconManager iconManager)
    {
        mRenderer = new PlottableEntityRenderer(aliasModel, iconManager);
        setAntialiasing(true);
        setCacheable(false);
    }

    public void addEntity(PlottableEntityHistory entity)
    {
        mEntities.add(entity);
    }

    public void removeEntity(PlottableEntityHistory entity)
    {
        mEntities.remove(entity);
    }

    public void clearEntities()
    {
        mEntities.clear();
    }

    private Set<PlottableEntityHistory> getEntities()
    {
        return Collections.unmodifiableSet(mEntities);
    }

    @Override
    protected void doPaint(Graphics2D g, JXMapViewer map, int width, int height)
    {
        Rectangle viewportBounds = map.getViewportBounds();

        g.translate(-viewportBounds.getX(), -viewportBounds.getY());

        Set<PlottableEntityHistory> entities = getEntities();

        for(PlottableEntityHistory entity : entities)
        {
            mRenderer.paintPlottableEntity(g, map, entity, true);
        }

        g.translate(viewportBounds.getX(), viewportBounds.getY());
    }
}
