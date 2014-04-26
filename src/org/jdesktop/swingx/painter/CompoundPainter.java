/*
 * $Id: CompoundPainter.java 4147 2012-02-01 17:13:24Z kschaefe $
 *
 * Copyright 2004 Sun Microsystems, Inc., 4150 Network Circle,
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

package org.jdesktop.swingx.painter;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

//import org.jdesktop.beans.JavaBean;

/**
 * <p>A {@link Painter} implementation composed of an array of <code>Painter</code>s.
 * <code>CompoundPainter</code> provides a means for combining several individual
 * <code>Painter</code>s, or groups of them, into one logical unit. Each of the
 * <code>Painter</code>s are executed in order. BufferedImageOp filter effects can
 * be applied to them together as a whole. The entire set of painting operations
 * may be cached together.</p>
 *
 * <p></p>
 *
 * <p>For example, if I want to create a CompoundPainter that started with a blue
 * background, had pinstripes on it running at a 45 degree angle, and those
 * pinstripes appeared to "fade in" from left to right, I would write the following:
 * <pre><code>
 *  Color blue = new Color(0x417DDD);
 *  Color translucent = new Color(blue.getRed(), blue.getGreen(), blue.getBlue(), 0);
 *  panel.setBackground(blue);
 *  panel.setForeground(Color.LIGHT_GRAY);
 *  GradientPaint blueToTranslucent = new GradientPaint(
 *    new Point2D.Double(.4, 0),
 *    blue,
 *    new Point2D.Double(1, 0),
 *    translucent);
 *  MattePainter veil = new MattePainter(blueToTranslucent);
 *  veil.setPaintStretched(true);
 *  Painter pinstripes = new PinstripePainter(45);
 *  Painter backgroundPainter = new RectanglePainter(this.getBackground(), null);
 *  Painter p = new CompoundPainter(backgroundPainter, pinstripes, veil);
 *  panel.setBackgroundPainter(p);
 * </code></pre></p>
 *
 * @author rbair
 * @param <T> an optional configuration parameter
 */
//@JavaBean
public class CompoundPainter<T> extends AbstractPainter<T>
{
	private static class Handler implements PropertyChangeListener
	{
		private final WeakReference<CompoundPainter<?>> ref;

		public Handler(CompoundPainter<?> painter)
		{
			ref = new WeakReference<CompoundPainter<?>>(painter);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void propertyChange(PropertyChangeEvent evt)
		{
			CompoundPainter<?> painter = ref.get();

			if (painter == null)
			{
				AbstractPainter<?> src = (AbstractPainter<?>) evt.getSource();
				src.removePropertyChangeListener(this);
			}
			else
			{
				String property = evt.getPropertyName();

				if ("dirty".equals(property) && evt.getNewValue() == Boolean.FALSE)
				{
					return;
				}

				painter.setDirty(true);
			}
		}
	}

	private Handler handler;

	private List<Painter<T>> painters = new CopyOnWriteArrayList<Painter<T>>();
	private AffineTransform transform;
	private boolean clipPreserved = false;

	private boolean checkForDirtyChildPainters = true;

	/** Creates a new instance of CompoundPainter */
	public CompoundPainter()
	{
		this((Painter<T>[]) null);
	}

	/**
	 * Convenience constructor for creating a CompoundPainter for an array
	 * of painters. A defensive copy of the given array is made, so that future
	 * modification to the array does not result in changes to the CompoundPainter.
	 *
	 * @param painters array of painters, which will be painted in order
	 */
	public CompoundPainter(Painter<T>... painters)
	{
		handler = new Handler(this);

		setPainters(painters);
	}

	/**
	 * Convenience constructor for creating a CompoundPainter for a list
	 * of painters. A defensive copy of the given array is made, so that future
	 * modification to the list does not result in changes to the CompoundPainter.
	 *
	 * @param painters array of painters, which will be painted in order
	 */
	public CompoundPainter(List<? extends Painter<T>> painters)
	{
		handler = new Handler(this);

		setPainters(painters);
	}
	/**
	 * Sets the array of Painters to use. These painters will be executed in
	 * order. A null value will be treated as an empty array. To prevent unexpected 
	 * behavior all values in provided array are copied to internally held array. 
	 * Any changes to the original array will not be reflected.
	 *
	 * @param painters array of painters, which will be painted in order
	 */
	public void setPainters(List<? extends Painter<T>> painters)
	{
		Collection<Painter<T>> old = new ArrayList<Painter<T>>(getPainters());

		for (Painter<T> p : old)
		{
			if (p instanceof AbstractPainter)
			{
				((AbstractPainter<?>) p).removePropertyChangeListener(handler);
			}
		}

		this.painters.clear();
		this.painters.addAll(painters);

		for (Painter<T> p : this.painters)
		{
			if (p instanceof AbstractPainter)
			{
				((AbstractPainter<?>) p).addPropertyChangeListener(handler);
			}
		}

		setDirty(true);
		firePropertyChange("painters", old, getPainters());
	}

	/**
	 * Sets the array of Painters to use. These painters will be executed in
	 * order. A null value will be treated as an empty array. To prevent unexpected 
	 * behavior all values in provided array are copied to internally held array. 
	 * Any changes to the original array will not be reflected.
	 *
	 * @param painters array of painters, which will be painted in order
	 */
	public void setPainters(Painter<T>... painters)
	{
		List<? extends Painter<T>> l;

		if (painters == null)
			l = Collections.emptyList();
		else
			l = Arrays.asList(painters);

		setPainters(l);
	}
	
	/**
	 * Adds a painter to the queue of painters
	 * @param painter the painter that is added
	 */
	public void addPainter(Painter<T> painter)
	{
		Collection<Painter<T>> old = new ArrayList<Painter<T>>(getPainters());
		
		this.painters.add(painter);	
		
		if (painter instanceof AbstractPainter)
		{
			((AbstractPainter<?>) painter).addPropertyChangeListener(handler);
		}

		setDirty(true);
		firePropertyChange("painters", old, getPainters());
	}
	
	/**
	 * Removes a painter from the queue of painters
	 * @param painter the painter that is added
	 */
	public void removePainter(Painter<T> painter)
	{
		Collection<Painter<T>> old = new ArrayList<Painter<T>>(getPainters());
		
		this.painters.remove(painter);
		
		if (painter instanceof AbstractPainter)
		{
			((AbstractPainter<?>) painter).removePropertyChangeListener(handler);
		}

		setDirty(true);
		firePropertyChange("painters", old, getPainters());
	}
	

	/**
	 * Gets the array of painters used by this CompoundPainter
	 * @return a defensive copy of the painters used by this CompoundPainter.
	 *         This will never be null.
	 */
	public final Collection<Painter<T>> getPainters()
	{
		return Collections.unmodifiableCollection(painters);
	}

	/**
	 * Indicates if the clip produced by any painter is left set once it finishes painting. 
	 * Normally the clip will be reset between each painter. Setting clipPreserved to
	 * true can be used to let one painter mask other painters that come after it.
	 * @return if the clip should be preserved
	 * @see #setClipPreserved(boolean)
	 */
	public boolean isClipPreserved()
	{
		return clipPreserved;
	}

	/**
	 * Sets if the clip should be preserved.
	 * Normally the clip will be reset between each painter. Setting clipPreserved to
	 * true can be used to let one painter mask other painters that come after it.
	 * 
	 * @param shouldRestoreState new value of the clipPreserved property
	 * @see #isClipPreserved()
	 */
	public void setClipPreserved(boolean shouldRestoreState)
	{
		boolean oldShouldRestoreState = isClipPreserved();
		this.clipPreserved = shouldRestoreState;
		setDirty(true);
		firePropertyChange("clipPreserved", oldShouldRestoreState, shouldRestoreState);
	}

	/**
	 * Gets the current transform applied to all painters in this CompoundPainter. May be null.
	 * @return the current AffineTransform
	 */
	public AffineTransform getTransform()
	{
		return transform;
	}

	/**
	 * Set a transform to be applied to all painters contained in this CompoundPainter
	 * @param transform a new AffineTransform
	 */
	public void setTransform(AffineTransform transform)
	{
		AffineTransform old = getTransform();
		this.transform = transform;
		setDirty(true);
		firePropertyChange("transform", old, transform);
	}

	/**
	 * <p>Iterates over all child <code>Painter</code>s and gives them a chance
	 * to validate themselves. If any of the child painters are dirty, then
	 * this <code>CompoundPainter</code> marks itself as dirty.</p>
	 */
	@Override
	protected void validate(T object)
	{
		boolean dirty = false;
		for (Painter<T> p : painters)
		{
			if (p instanceof AbstractPainter)
			{
				AbstractPainter<T> ap = (AbstractPainter<T>) p;
				ap.validate(object);
				if (ap.isDirty())
				{
					dirty = true;
					break;
				}
			}
		}
		clearLocalCacheOnly = true;
		setDirty(dirty); //super will call clear cache
		clearLocalCacheOnly = false;
	}

	//indicates whether the local cache should be cleared only, as opposed to the
	//cache's of all of the children. This is needed to optimize the caching strategy
	//when, during validate, the CompoundPainter is marked as dirty
	private boolean clearLocalCacheOnly = false;

	/**
	 * Used by {@link #isDirty()} to check if the child <code>Painter</code>s
	 * should be checked for their <code>dirty</code> flag as part of
	 * processing.<br>
	 * Default value is: <code>true</code><br>
	 * This should be set to </code>false</code> if the cacheable state
	 * of the child <code>Painter</code>s are different from each other.  This
	 * will allow the cacheable == <code>true</code> <code>Painter</code>s to
	 * keep their cached image during regular repaints.  In this case,
	 * client code should call {@link #clearCache()} manually when the cacheable
	 * <code>Painter</code>s should be updated.
	 * @return the dirty check flag
	 *
	 *
	 * @see #isDirty()
	 */
	public boolean isCheckingDirtyChildPainters()
	{
		return checkForDirtyChildPainters;
	}

	/**
	 * Set the flag used by {@link #isDirty()} to check if the 
	 * child <code>Painter</code>s should be checked for their 
	 * <code>dirty</code> flag as part of processing.
	 * @param b the dirty check flag
	 *
	 * @see #isCheckingDirtyChildPainters()
	 * @see #isDirty()
	 */
	public void setCheckingDirtyChildPainters(boolean b)
	{
		boolean old = isCheckingDirtyChildPainters();
		this.checkForDirtyChildPainters = b;
		firePropertyChange("checkingDirtyChildPainters", old, isCheckingDirtyChildPainters());
	}

	/**
	 * <p>This <code>CompoundPainter</code> is dirty if it, or (optionally)
	 * any of its children, are dirty. If the super implementation returns
	 * <code>true</code>, we return <code>true</code>.  Otherwise, if
	 * {@link #isCheckingDirtyChildPainters()} is <code>true</code>, we iterate
	 * over all child <code>Painter</code>s and query them to see
	 * if they are dirty. If so, then <code>true</code> is returned. 
	 * Otherwise, we return <code>false</code>.</p>
	 *
	 * @see #isCheckingDirtyChildPainters()
	 */
	@Override
	protected boolean isDirty()
	{
		boolean dirty = super.isDirty();
		if (dirty)
		{
			return true;
		}
		else if (isCheckingDirtyChildPainters())
		{
			for (Painter<T> p : painters)
			{
				if (p instanceof AbstractPainter)
				{
					AbstractPainter<?> ap = (AbstractPainter<?>) p;
					if (ap.isDirty())
					{
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	protected void setDirty(boolean d)
	{
		boolean old = super.isDirty();
		boolean ours = isDirty();

		super.setDirty(d);

		//must perform this check to ensure we do not double notify
		if (d != old && d == ours)
		{
			firePropertyChange("dirty", old, isDirty());
		}
	}

	/**
	 * <p>Clears the cache of this <code>Painter</code>, and all child
	 * <code>Painters</code>. This is done to ensure that resources
	 * are collected, even if clearCache is called by some framework
	 * or other code that doesn't realize this is a CompoundPainter.</p>
	 *
	 * <p>Call #clearLocalCache if you only want to clear the cache of this
	 * <code>CompoundPainter</code>
	 */
	@Override
	public void clearCache()
	{
		if (!clearLocalCacheOnly)
		{
			for (Painter<T> p : painters)
			{
				if (p instanceof AbstractPainter)
				{
					AbstractPainter<?> ap = (AbstractPainter<?>) p;
					ap.clearCache();
				}
			}
		}
		super.clearCache();
	}

	/**
	 * <p>Clears the cache of this painter only, and not of any of the children.</p>
	 */
	public void clearLocalCache()
	{
		super.clearCache();
	}

	@Override
	protected void doPaint(Graphics2D g, T component, int width, int height)
	{
		for (Painter<T> p : getPainters())
		{
			Graphics2D temp = (Graphics2D) g.create();

			try
			{
				p.paint(temp, component, width, height);
				if (isClipPreserved())
				{
					g.setClip(temp.getClip());
				}
			}
			finally
			{
				temp.dispose();
			}
		}
	}

	@Override
	protected void configureGraphics(Graphics2D g)
	{
		//applies the transform
		AffineTransform tx = getTransform();
		if (tx != null)
		{
			g.setTransform(tx);
		}
	}

	@Override
	protected boolean shouldUseCache()
	{
		return super.shouldUseCache(); // || (painters != null && painters.length > 1);
	}
}
