/*
 * $Id: AbstractSerializableBean.java 4088 2011-11-17 19:53:49Z kschaefe $
 *
 * Copyright 2008 Sun Microsystems, Inc., 4150 Network Circle,
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
package org.jdesktop.beans;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.VetoableChangeListener;
import java.beans.VetoableChangeSupport;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * This subclass enhances {@code AbstractBean} by implementing the
 * {@code Serializable} interface. {@code AbstractSerializableBean} correctly
 * serializes all {@code Serializable} listeners that it contains. Implementors
 * that need to extends {@code AbstractBean} or one of its subclasses and
 * require serialization should use this class if possible. If it is not
 * possible to extend this class, the implementation can guide implementors on
 * how to properly serialize the listeners.
 * 
 * @author Karl George Schaefer
 * 
 * @see Serializable
 * @see ObjectInputStream
 * @see ObjectOutputStream
 */
@SuppressWarnings("serial")
public abstract class AbstractSerializableBean extends AbstractBean implements
        Serializable {
    /**
     * Creates a new instance of {@code AbstractSerializableBean}.
     */
    protected AbstractSerializableBean() {
        super();
    }

    /**
     * Creates a new instance of {@code AbstractSerializableBean}, using the
     * supplied support delegates. Neither of these may be {@code null}.
     * 
     * @param pcs
     *            the property change support class to use
     * @param vcs
     *            the vetoable change support class to use
     * @throws NullPointerException
     *             if any parameter is {@code null}
     */
    protected AbstractSerializableBean(PropertyChangeSupport pcs,
            VetoableChangeSupport vcs) {
        super(pcs, vcs);
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();

        for (PropertyChangeListener l : getPropertyChangeListeners()) {
            if (l instanceof Serializable) {
                s.writeObject(l);
            }
        }

        for (VetoableChangeListener l : getVetoableChangeListeners()) {
            if (l instanceof Serializable) {
                s.writeObject(l);
            }
        }

        s.writeObject(null);
    }

    private void readObject(ObjectInputStream s) throws ClassNotFoundException,
            IOException {
        s.defaultReadObject();

        Object listenerOrNull;
        while (null != (listenerOrNull = s.readObject())) {
            if (listenerOrNull instanceof PropertyChangeListener) {
                addPropertyChangeListener((PropertyChangeListener) listenerOrNull);
            } else if (listenerOrNull instanceof VetoableChangeListener) {
                addVetoableChangeListener((VetoableChangeListener) listenerOrNull);
            }
        }
    }
}
