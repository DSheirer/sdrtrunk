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
package alias.id;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlTransient;

import alias.id.broadcast.BroadcastChannel;
import playlist.version1.Group;
import alias.id.esn.Esn;
import alias.id.fleetsync.FleetsyncID;
import alias.id.lojack.LoJackFunctionAndID;
import alias.id.mdc.MDC1200ID;
import alias.id.mobileID.Min;
import alias.id.mpt1327.MPT1327ID;
import alias.id.nonrecordable.NonRecordable;
import alias.id.priority.Priority;
import alias.id.siteID.SiteID;
import alias.id.status.StatusID;
import alias.id.talkgroup.TalkgroupID;
import alias.id.uniqueID.UniqueID;

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
