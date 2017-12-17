/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014-2016 Dennis Sheirer
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
package io.github.dsheirer.alias.id;

import io.github.dsheirer.alias.id.broadcast.BroadcastChannel;
import io.github.dsheirer.alias.id.esn.Esn;
import io.github.dsheirer.alias.id.fleetsync.FleetsyncID;
import io.github.dsheirer.alias.id.lojack.LoJackFunctionAndID;
import io.github.dsheirer.alias.id.mdc.MDC1200ID;
import io.github.dsheirer.alias.id.mobileID.Min;
import io.github.dsheirer.alias.id.mpt1327.MPT1327ID;
import io.github.dsheirer.alias.id.nonrecordable.NonRecordable;
import io.github.dsheirer.alias.id.priority.Priority;
import io.github.dsheirer.alias.id.siteID.SiteID;
import io.github.dsheirer.alias.id.status.StatusID;
import io.github.dsheirer.alias.id.talkgroup.TalkgroupID;
import io.github.dsheirer.alias.id.uniqueID.UniqueID;
import io.github.dsheirer.playlist.version1.Group;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlTransient;

@SuppressWarnings( "deprecation" )
@XmlSeeAlso( {BroadcastChannel.class, Esn.class, FleetsyncID.class, Group.class, LoJackFunctionAndID.class,
			   MDC1200ID.class, Min.class, MPT1327ID.class, NonRecordable.class,
			   Priority.class, SiteID.class, StatusID.class, TalkgroupID.class,
			   UniqueID.class } )
@XmlRootElement( name = "id" )
public abstract class AliasID
{
	public AliasID()
	{
	}
	
	public abstract AliasIDType getType();
	
	public abstract boolean matches( AliasID id );

	@XmlTransient
	public abstract boolean isValid();
}
