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
package io.github.dsheirer.database.call;

/**
 * Type of call, derived from the stored TO and FROM identifier forms.
 */
public enum CallType
{
    /**
     * Call to a talkgroup
     */
    GROUP("Group"),
    /**
     * Call to a patch group (talkgroups patched together)
     */
    PATCH("Patch"),
    /**
     * Private (individual) call from one radio to another radio
     */
    PRIVATE("Private"),
    /**
     * Telephone interconnect call between a radio and a telephone
     */
    TELEPHONE("Telephone"),
    /**
     * Call without a TO identifier that identifies the call type (e.g. conventional analog)
     */
    UNKNOWN("");

    private static final String FORM_TELEPHONE_NUMBER = "TELEPHONE_NUMBER";
    private final String mLabel;

    CallType(String label)
    {
        mLabel = label;
    }

    /**
     * Call type for the TO and FROM identifier forms (Form enumeration names).
     */
    public static CallType from(String toForm, String fromForm)
    {
        if(CallRecord.FORM_PATCH_GROUP.equals(toForm))
        {
            return PATCH;
        }

        if(CallRecord.FORM_TALKGROUP.equals(toForm))
        {
            return GROUP;
        }

        if(FORM_TELEPHONE_NUMBER.equals(toForm) || FORM_TELEPHONE_NUMBER.equals(fromForm))
        {
            return TELEPHONE;
        }

        if(CallRecord.FORM_RADIO.equals(toForm))
        {
            return PRIVATE;
        }

        return UNKNOWN;
    }

    /**
     * SQL condition on the to_form and from_form columns that selects the calls of this type, using the same rules as
     * {@link #from(String, String)}.  The values are constants, so the condition has no parameters.
     */
    public String getWhereClause()
    {
        String patch = "'" + CallRecord.FORM_PATCH_GROUP + "'";
        String group = "'" + CallRecord.FORM_TALKGROUP + "'";
        String radio = "'" + CallRecord.FORM_RADIO + "'";
        String telephone = "'" + FORM_TELEPHONE_NUMBER + "'";
        String fromNotTelephone = "(from_form IS NULL OR from_form <> " + telephone + ")";

        return switch(this)
        {
            case PATCH -> "to_form = " + patch;
            case GROUP -> "to_form = " + group;
            case TELEPHONE -> "(to_form = " + telephone + " OR (from_form = " + telephone + " AND (to_form IS NULL " +
                "OR to_form NOT IN (" + patch + ", " + group + "))))";
            case PRIVATE -> "(to_form = " + radio + " AND " + fromNotTelephone + ")";
            case UNKNOWN -> "((to_form IS NULL OR to_form NOT IN (" + patch + ", " + group + ", " + radio + ", " +
                telephone + ")) AND " + fromNotTelephone + ")";
        };
    }

    /**
     * Label for selecting the type in a search filter (the unknown type is labeled Other)
     */
    public String getFilterLabel()
    {
        return this == UNKNOWN ? "Other" : mLabel;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }
}
