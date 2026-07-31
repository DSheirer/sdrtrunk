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

package com.sun.java.swing.plaf.windows;

import javax.swing.LookAndFeel;
import javax.swing.UIDefaults;

/**
 * No-op stub for the JDK's Windows look-and-feel.
 *
 * <p>JIDE 3.6.18's {@code LookAndFeelFactory} contains compile-time
 * {@code instanceof com.sun.java.swing.plaf.windows.WindowsLookAndFeel} checks throughout its
 * dispatch logic.  On Linux and macOS JDK builds that class is genuinely absent from
 * {@code java.desktop}, so the JVM throws {@link NoClassDefFoundError} the first time JIDE's
 * dispatch code executes the {@code instanceof} bytecode.  That happens during
 * {@link com.jidesoft.swing.JideSplitPane#updateUI()} on every LAF change.
 *
 * <p>This stub lives in the classpath so the class reference resolves on non-Windows JDKs.
 * On Windows JDK builds the real {@code WindowsLookAndFeel} is loaded from the
 * {@code java.desktop} module via the boot class loader and shadows this class.
 *
 * <p>The stub reports itself as not native and not supported - JIDE never instantiates this
 * class, it only uses it for {@code instanceof} comparisons against the active LAF.  Since
 * sdrtrunk never installs this LAF, the {@code instanceof} check always returns false and
 * JIDE proceeds to its next branch.
 */
public class WindowsLookAndFeel extends LookAndFeel
{
    @Override
    public String getName()
    {
        return "Windows (stub)";
    }

    @Override
    public String getID()
    {
        return "WindowsStub";
    }

    @Override
    public String getDescription()
    {
        return "Compatibility stub for JIDE's instanceof checks on non-Windows JDKs.";
    }

    @Override
    public boolean isNativeLookAndFeel()
    {
        return false;
    }

    @Override
    public boolean isSupportedLookAndFeel()
    {
        return false;
    }

    @Override
    public UIDefaults getDefaults()
    {
        return new UIDefaults();
    }
}
