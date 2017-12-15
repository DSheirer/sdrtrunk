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
package icon;

import javax.swing.filechooser.FileFilter;
import java.io.File;

public class ImageFilter extends FileFilter
{
    //Accept all directories and all gif, jpg, tiff, or png files.
    public boolean accept(File f)
    {
        if(f.isDirectory())
        {
            return true;
        }

        String extension = getExtension(f);

        if(extension != null)
        {
            if(extension.equals("tiff") ||
                extension.equals("tif") ||
                extension.equals("gif") ||
                extension.equals("jpeg") ||
                extension.equals("jpg") ||
                extension.equals("png"))
            {
                return true;
            }
            else
            {
                return false;
            }
        }

        return false;
    }

    //The description of this filter
    public String getDescription()
    {
        return "Images (*.gif,*.jpg,*.png.";
    }

    public String getExtension(File f)
    {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');

        if(i > 0 && i < s.length() - 1)
        {
            ext = s.substring(i + 1).toLowerCase();
        }
        return ext;
    }
}
