/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

package io.github.dsheirer.identifier.patch;

import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manager for (temporary) patch groups aka super groups. This manager monitors patch group additions and deletions and
 * maintains a map of the current state of each patch group active in the system.
 *
 * Traffic channel calls will reference a patch group by simply using the patch group identifier.  This
 * manager will replace that reference with the current state of the full patch group so that the call event has the
 * full patch group including all patched talkgroups or individual radios, which may not have been included in the
 * patch group reference on the control or traffic channel.
 */
public class PatchGroupManager
{
    private static final long PATCH_GROUP_FRESHNESS_THRESHOLD_MS = Duration.ofSeconds(30).toMillis();
    private Map<Integer,PatchGroupTracker> mPatchGroupTrackerMap = new HashMap<>();

    /**
     * Constructs an instance
     */
    public PatchGroupManager()
    {
    }

    /**
     * Summary listing of active patch groups
     */
    public String getPatchGroupSummary()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Active Patch Groups\n");
        List<PatchGroupTracker> trackers = new ArrayList<>(mPatchGroupTrackerMap.values());

        if(trackers.isEmpty())
        {
            sb.append("  None\n");
        }
        else
        {
            for(PatchGroupTracker tracker : trackers)
            {
                sb.append("  ").append(tracker.mPatchGroupIdentifier).append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * Clears any existing patch groups
     */
    public void clear()
    {
        mPatchGroupTrackerMap.clear();
    }

    /**
     * Adds the patch group identifier to the set of managed patch groups.  If the patch group is already being
     * managed, the new patch group argument is checked for additional patched talkgroups and they are added to the
     * existing managed patch group.  Where the patch group version (super group sequence number) is different, the
     * existing patch group is discarded and replaced with the updated version of the patch group.
     *
     * @param patchGroupIdentifier to add or update
     * @return true if the group was added or updated
     */
    public synchronized boolean addPatchGroup(PatchGroupIdentifier patchGroupIdentifier, long timestamp)
    {
        PatchGroup update = patchGroupIdentifier.getValue();

        int patchGroup = update.getPatchGroup().getValue();

        if(patchGroup > 0)
        {
            if(mPatchGroupTrackerMap.containsKey(patchGroup))
            {
                PatchGroupTracker tracker = mPatchGroupTrackerMap.get(patchGroup);

                if(tracker.isStale(timestamp))
                {
                    //Replace the existing patch group if it is stale.
                    mPatchGroupTrackerMap.put(patchGroup, new PatchGroupTracker(patchGroupIdentifier, timestamp));
                    return true;
                }
                else
                {
                    //Update the existing patch group.
                    return tracker.add(patchGroupIdentifier, timestamp);
                }
            }
            else
            {
                mPatchGroupTrackerMap.put(patchGroup, new PatchGroupTracker(patchGroupIdentifier, timestamp));
                return true;
            }
        }

        return false;
    }

    /**
     * Adds any patch group identifiers contained in the list.
     * @param referenceTimestamp as a reference for checking staleness of existing patch groups.
     */
    public synchronized void addPatchGroups(List<Identifier> identifiers, long referenceTimestamp)
    {
        for(Identifier identifier : identifiers)
        {
            if(identifier instanceof PatchGroupIdentifier)
            {
                addPatchGroup((PatchGroupIdentifier)identifier, referenceTimestamp);
            }
        }
    }

    /**
     * Removes the patch group from this manager if it is currently being managed.
     *
     * @param patchGroupIdentifier to remove
     * @return true if the patch group was removed.
     */
    public synchronized boolean removePatchGroup(PatchGroupIdentifier patchGroupIdentifier)
    {
        int id = patchGroupIdentifier.getValue().getPatchGroup().getValue();
        return mPatchGroupTrackerMap.remove(id) != null;
    }

    /**
     * Removes any patch group identifiers contained in the list.
     */
    public synchronized void removePatchGroups(List<Identifier> identifiers)
    {
        for(Identifier identifier : identifiers)
        {
            if(identifier instanceof PatchGroupIdentifier)
            {
                removePatchGroup((PatchGroupIdentifier)identifier);
            }
        }
    }

    /**
     * Updates the list of identifiers by replacing any talkgroups or patch groups with the current
     * version of the patch group and a complete listing of the patched talkgroups.
     * @param identifiers to update
     * @param referenceTimestamp as a reference for checking staleness of existing patch groups.
     * @return list of identifiers
     */
    public List<Identifier> update(List<Identifier> identifiers, long referenceTimestamp)
    {
        List<Identifier> updated = new ArrayList<>();

        for(Identifier identifier: identifiers)
        {
            updated.add(update(identifier, referenceTimestamp));
        }

        return updated;
    }

    /**
     * Checks the PATCH GROUP identifier and replaces the identifier with the current patch group
     * if the identifier matches a currently managed patch group.
     *
     * @param identifier for a talkgroup or a patch group.
     * @param referenceTimestamp as a reference for checking staleness of existing patch groups.
     * @return current patch group or the original identifier
     */
    public Identifier update(Identifier identifier, long referenceTimestamp)
    {
        if(identifier != null && identifier.getIdentifierClass() == IdentifierClass.USER && identifier.getRole() == Role.TO)
        {
            switch(identifier.getForm())
            {
                case TALKGROUP:
                    if(identifier instanceof TalkgroupIdentifier talkgroupIdentifier)
                    {
                        int id = talkgroupIdentifier.getValue();

                        PatchGroupTracker tracker = mPatchGroupTrackerMap.get(id);

                        if(tracker != null)
                        {
                            if(tracker.isStale(referenceTimestamp))
                            {
                                mPatchGroupTrackerMap.remove(id);
                            }
                            else
                            {
                                //Perform substitution - return patch group instead of the original talkgroup
                                return tracker.getPatchGroupIdentifier(referenceTimestamp);
                            }
                        }
                    }
                    break;
                case PATCH_GROUP:
                    if(identifier instanceof PatchGroupIdentifier patchGroupIdentifier)
                    {
                        int id = patchGroupIdentifier.getValue().getPatchGroup().getValue();

                        PatchGroupTracker tracker = mPatchGroupTrackerMap.get(id);

                        if(tracker != null)
                        {
                            if(tracker.isStale(referenceTimestamp))
                            {
                                mPatchGroupTrackerMap.put(id, new PatchGroupTracker(patchGroupIdentifier, referenceTimestamp));
                                mPatchGroupTrackerMap.remove(id);
                            }
                            else if(tracker.getPatchGroupIdentifier(referenceTimestamp).getValue().getVersion() !=
                                            patchGroupIdentifier.getValue().getVersion())
                            {
                                mPatchGroupTrackerMap.put(id, new PatchGroupTracker(patchGroupIdentifier, referenceTimestamp));
                                mPatchGroupTrackerMap.remove(id);
                            }
                            else
                            {
                                //Perform substitution - return patch group instead of the original talkgroup
                                return tracker.getPatchGroupIdentifier(referenceTimestamp);
                            }
                        }
                    }
                    break;
            }
        }

        return identifier;
    }

    /**
     * Tracks the freshness of a patch group and any member talkgroup and radio identifiers to ensure that stale
     * patch group identifiers are removed.
     */
    public class PatchGroupTracker
    {
        private PatchGroupIdentifier mPatchGroupIdentifier;
        private long mLastUpdateTimestamp;
        private Map<TalkgroupIdentifier,Long> mTalkgroupTimestampMap = new HashMap<>();
        private Map<RadioIdentifier,Long> mRadioTimestampMap = new HashMap<>();

        /**
         * Constructs an instance
         * @param patchGroupIdentifier
         */
        public PatchGroupTracker(PatchGroupIdentifier patchGroupIdentifier, long timestamp)
        {
            mPatchGroupIdentifier = patchGroupIdentifier;
            add(patchGroupIdentifier, timestamp);
        }

        /**
         * Patch group monitored by this tracker.  This method will cleanup and remove any stale radio or talkgroup
         * identifiers on access.
         *
         * Note: this tracker should be checked for freshness by first checking the isStale() method before accessing
         * the tracked patch group by this method.
         *
         * @param referenceTimestamp for comparing stored radio and talkgroup values for staleness.
         * @return current version of the talkgroup
         */
        public PatchGroupIdentifier getPatchGroupIdentifier(long referenceTimestamp)
        {
            //Remove stale talkgroups
            List<Map.Entry<TalkgroupIdentifier,Long>> toRemove = mTalkgroupTimestampMap.entrySet().stream()
                    .filter(entry -> isStale(entry.getValue(), referenceTimestamp)).collect(Collectors.toList());

            for(Map.Entry<TalkgroupIdentifier,Long> entry: toRemove)
            {
                mTalkgroupTimestampMap.remove(entry.getKey());
                mPatchGroupIdentifier.getValue().removePatchedTalkgroup(entry.getKey());
            }

            //Remove stale radios
            List<Map.Entry<RadioIdentifier,Long>> toRemove2 = mRadioTimestampMap.entrySet().stream()
                    .filter(entry -> isStale(entry.getValue(), referenceTimestamp)).collect(Collectors.toList());

            for(Map.Entry<RadioIdentifier,Long> entry: toRemove2)
            {
                mRadioTimestampMap.remove(entry.getKey());
                mPatchGroupIdentifier.getValue().removePatchedRadio(entry.getKey());
            }

            return mPatchGroupIdentifier;
        }

        /**
         * Indicates if the stored timestamp is stale relative to the reference timestamp.
         * @param storedTimestamp to compare
         * @param referenceTimestamp representing the current time.
         * @return true of the stored timestamp is stale.
         */
        private boolean isStale(long storedTimestamp, long referenceTimestamp)
        {
            return referenceTimestamp - storedTimestamp > PATCH_GROUP_FRESHNESS_THRESHOLD_MS;
        }

        /**
         * Indicates if the tracked patch group is stale and hasn't received any add/updates within a period of time.
         * @param referenceTimestamp representing now for comparison against the patch groups update timestamp.
         * @return true if the tracked patch group is stale.
         */
        public boolean isStale(long referenceTimestamp)
        {
            return isStale(mLastUpdateTimestamp, referenceTimestamp);
        }

        /**
         * Add or update the patch group and update the freshness timestamp.
         * @param patchGroupIdentifier containing an add value.
         */
        public boolean add(PatchGroupIdentifier patchGroupIdentifier, long timestamp)
        {
            mLastUpdateTimestamp = timestamp;

            boolean added = false;

            for(TalkgroupIdentifier talkgroup: patchGroupIdentifier.getValue().getPatchedTalkgroupIdentifiers())
            {
                mTalkgroupTimestampMap.put(talkgroup, timestamp);
                added |= mPatchGroupIdentifier.getValue().addPatchedTalkgroup(talkgroup);
            }

            for(RadioIdentifier radio: patchGroupIdentifier.getValue().getPatchedRadioIdentifiers())
            {
                mRadioTimestampMap.put(radio, timestamp);
                added |= mPatchGroupIdentifier.getValue().addPatchedRadio(radio);
            }

            return added;
        }
    }
}
