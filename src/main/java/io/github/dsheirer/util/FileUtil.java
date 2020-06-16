/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.util;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

/**
 * File Utilities
 */
public class FileUtil
{
    /**
     * Recursively searches for the named file starting in the specified directory.
     * @param directory to search for the file
     * @return path to the file or null if the file can't be found
     * @throws IOException if there is a file system error or if the file is not found
     */
    public static Path findFile(Path directory, String name) throws IOException
    {
        if(Files.isDirectory(directory))
        {
            DirectoryStream<Path> stream = Files.newDirectoryStream(directory);
            Iterator<Path> it = stream.iterator();
            Path path = null;
            while(it.hasNext())
            {
                path = it.next();

                if(Files.isDirectory(path))
                {
                    Path candidate = findFile(path, name);

                    if(candidate != null)
                    {
                        return candidate;
                    }
                }
                else
                {
                    if(path.endsWith(name))
                    {
                        return path;
                    }
                }
            }
        }
        else
        {
            throw new IOException("A directory required for this method [" +
                (directory != null ? directory.toString() : "null") + "]");
        }

        return null;
    }
}
