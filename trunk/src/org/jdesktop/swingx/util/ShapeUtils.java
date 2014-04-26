/*
 * $Id: ShapeUtils.java 4082 2011-11-15 18:39:43Z kschaefe $
 *
 * Copyright 2006 Sun Microsystems, Inc., 4150 Network Circle,
 * Santa Clara, California 95054, U.S.A. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.jdesktop.swingx.util;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/**
 * 
 * @author joshy
 */
public final class ShapeUtils {

    /** Creates a new instance of ShapeUtils */
    private ShapeUtils() {
    }

    /**
     * Generates a polygon with an inside radius of 0.
     * @param sides number of sides
     * @param outsideRadius the outside radius
     * @param normalize normalize
     * @return the generated shape
     */
    public static Shape generatePolygon(int sides, int outsideRadius, boolean normalize) {
        return generatePolygon(sides, outsideRadius, 0, normalize);
    }

    /**
     * Generates a polygon
     * @param sides number of sides
     * @param outsideRadius the outside radius
     * @param insideRadius the inside radius
     * @param normalize normalize
     * @return the generated shape
     */
    public static Shape generatePolygon(int sides, int outsideRadius, int insideRadius,
            boolean normalize) {
        Shape shape = generatePolygon(sides, outsideRadius, insideRadius);
        if (normalize) {
            Rectangle2D bounds = shape.getBounds2D();
            GeneralPath path = new GeneralPath(shape);
            shape = path.createTransformedShape(AffineTransform.getTranslateInstance(
                    -bounds.getX(), -bounds.getY()));
        }
        return shape;
    }

    /**
     * Generates a polygon
     * @param sides number of sides
     * @param outsideRadius the outside radius
     * @param insideRadius the inside radius
     * @return the generated shape
     */
    public static Shape generatePolygon(int sides, int outsideRadius, int insideRadius) {
        if (sides < 3) {
            return new Ellipse2D.Float(0, 0, 10, 10);
        }

        AffineTransform trans = new AffineTransform();
        Polygon poly = new Polygon();
        for (int i = 0; i < sides; i++) {
            trans.rotate(Math.PI * 2 / sides / 2);
            Point2D out = trans.transform(new Point2D.Float(0, outsideRadius), null);
            poly.addPoint((int) out.getX(), (int) out.getY());
            trans.rotate(Math.PI * 2 / sides / 2);
            if (insideRadius > 0) {
                Point2D in = trans.transform(new Point2D.Float(0, insideRadius), null);
                poly.addPoint((int) in.getX(), (int) in.getY());
            }
        }

        return poly;
    }

    /**
     * @param font the font
     * @param ch a single character
     * @return the shape
     */
    public static Shape generateShapeFromText(Font font, char ch) {
        return generateShapeFromText(font, String.valueOf(ch));
    }

    /**
     * @param font the font
     * @param string the text string
     * @return the shape
     */
    public static Shape generateShapeFromText(Font font, String string) {
        BufferedImage img = GraphicsUtilities.createCompatibleTranslucentImage(1, 1);
        Graphics2D g2 = img.createGraphics();

        try {
            GlyphVector vect = font.createGlyphVector(g2.getFontRenderContext(), string);
            Shape shape = vect.getOutline(0f, (float) -vect.getVisualBounds().getY());

            return shape;
        } finally {
            g2.dispose();
        }
    }

    /**
     * Sets the clip on a graphics object by merging a supplied clip with the existing one. The new
     * clip will be an intersection of the old clip and the supplied clip. The old clip shape will
     * be returned. This is useful for resetting the old clip after an operation is performed.
     * 
     * @param g
     *            the graphics object to update
     * @param clip
     *            a new clipping region to add to the graphics clip.
     * @return the current clipping region of the supplied graphics object. This may return
     *         {@code null} if the current clip is {@code null}.
     * @throws NullPointerException
     *             if any parameter is {@code null}
     */
    public static Shape mergeClip(Graphics g, Shape clip) {
        Shape oldClip = g.getClip();
        if (oldClip == null) {
            g.setClip(clip);
            return null;
        }
        Area area = new Area(oldClip);
        area.intersect(new Area(clip));// new Rectangle(0,0,width,height)));
        g.setClip(area);
        return oldClip;
    }
}
