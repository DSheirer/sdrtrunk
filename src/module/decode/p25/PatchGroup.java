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
package module.decode.p25;

import java.util.ArrayList;
import java.util.List;

public class PatchGroup
{
    private long mExpireThreshold = 5000; //5 seconds
    private long mLastUpdated;
    private String mPatchGroupID;
    private List<String> mPatchedGroups = new ArrayList<>();

    /**
     * Constructs a patch group with the specified patch group ID
     * @param patchGroupID that identifies this patch group
     */
    public PatchGroup(String patchGroupID)
    {
        mPatchGroupID = patchGroupID;
        mLastUpdated = System.currentTimeMillis();
    }

    /**
     * Patch group ID for this patch group
     */
    public String getPatchGroupID()
    {
        return mPatchGroupID;
    }

    /**
     * Adds the patched group to this patch group.  Duplicate patched groups are ignored.
     *
     * Note: the patch group ID will not be added as a patched group, to avoid circular dependencies.
     *
     * @return true if the patched group was added to this patch group
     */
    public boolean addPatchedGroup(String patchedGroup)
    {
        if(patchedGroup != null && !patchedGroup.equals(mPatchGroupID) && !mPatchedGroups.contains(patchedGroup))
        {
            mPatchedGroups.add(patchedGroup);
            return true;
        }

        return false;
    }

    /**
     * Patched talkgroups that are part of this patch group.
     */
    public List<String> getPatchedGroups()
    {
        return mPatchedGroups;
    }

    /**
     * Updates the timestamp for this patch group.
     */
    public void updateTimestamp()
    {
        mLastUpdated = System.currentTimeMillis();
    }

    /**
     * Indicates if this patch group is expired, meaning that it hasn't been updated in the past (expire
     * threshold) seconds.
     */
    public boolean isExpired()
    {
        return System.currentTimeMillis() - mLastUpdated > mExpireThreshold;
    }

    /**
     * Sets the patch group expiration threshold (default = 5000 ms).  A patch group is considered expired if
     * no new updates have been received within the past (expire threshold) milliseconds
     *
     * @param expireThreshold in milliseconds
     */
    public void setExpireThreshold(long expireThreshold)
    {
        mExpireThreshold = expireThreshold;
    }
}
