/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014 Dennis Sheirer
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package controller.system;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;

@XmlSeeAlso( { controller.system.System.class } )

@XmlRootElement( name = "system_list" )
public class SystemList
{
    private ArrayList<System> mSystem = new ArrayList<System>();

    public SystemList()
    {
    }
    
    public ArrayList<System> getSystem()
    {
        return mSystem;
    }
    
    public void setSystem( ArrayList<System> systems )
    {
    	mSystem = systems;
    }
    
    public void addSystem( System system )
    {
        mSystem.add( system );
    }
    
    public void removeSystem( System system )
    {
        mSystem.remove( system );
    }
    
    public void clearSystems()
    {
        mSystem.clear();
    }
}
