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

package io.github.dsheirer.module.decode.p25.phase2.message.mac.structure;

import io.github.dsheirer.bits.IntField;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards against the off-by-one where a field is declared as IntField.range(start, start + width) instead of
 * start + width - 1.  IntField.range() end index is inclusive, so the field claims the first bit of the field that
 * follows it (e.g. a 21-bit WACN that includes the first bit of the system ID).  Flags any two fields of the same
 * structure (or of the same continuation fragment) where one field ends on the bit where the other starts.
 */
public class MacStructureFieldBoundaryTest
{
    private static final String FRAGMENT_PREFIX = "FRAGMENT";

    private record NamedField(String name, IntField field)
    {
        boolean isFragment()
        {
            return name.startsWith(FRAGMENT_PREFIX);
        }
    }

    @Test
    public void noFieldEndsOnTheStartBitOfAnotherField() throws Exception
    {
        List<Class<?>> classes = structureClasses();
        assertFalse(classes.isEmpty(), "No MAC structure classes found to inspect");

        List<String> overlaps = new ArrayList<>();

        for(Class<?> clazz : classes)
        {
            List<NamedField> fields = intFields(clazz);

            for(NamedField a : fields)
            {
                for(NamedField b : fields)
                {
                    if(a != b && a.isFragment() == b.isFragment() && a.field().end() == b.field().start())
                    {
                        overlaps.add(clazz.getSimpleName() + "." + a.name() + " [" + a.field().start() + "-" +
                            a.field().end() + "] ends on the start bit of " + b.name() + " [" + b.field().start() +
                            "-" + b.field().end() + "]");
                    }
                }
            }
        }

        assertTrue(overlaps.isEmpty(), "Overlapping MAC structure fields:\n" + String.join("\n", overlaps));
    }

    private static List<NamedField> intFields(Class<?> clazz) throws IllegalAccessException
    {
        List<NamedField> fields = new ArrayList<>();

        for(Field field : clazz.getDeclaredFields())
        {
            if(Modifier.isStatic(field.getModifiers()) && field.getType() == IntField.class)
            {
                field.setAccessible(true);
                fields.add(new NamedField(field.getName(), (IntField)field.get(null)));
            }
        }

        return fields;
    }

    /**
     * Loads every top-level class in this package and its vendor sub-packages from the compiled classes directory.
     */
    private static List<Class<?>> structureClasses() throws IOException, URISyntaxException, ClassNotFoundException
    {
        String packageName = MacStructure.class.getPackageName();
        URL url = MacStructure.class.getResource(MacStructure.class.getSimpleName() + ".class");
        Path root = Path.of(url.toURI()).getParent();
        List<Class<?>> classes = new ArrayList<>();

        try(Stream<Path> paths = Files.walk(root))
        {
            for(Path path : paths.filter(p -> p.toString().endsWith(".class") && !p.toString().contains("$")).toList())
            {
                String relative = root.relativize(path).toString();
                String className = packageName + "." + relative.substring(0, relative.length() - 6)
                    .replace(path.getFileSystem().getSeparator(), ".");
                classes.add(Class.forName(className));
            }
        }

        return classes;
    }
}
