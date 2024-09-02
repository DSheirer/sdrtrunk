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

package io.github.dsheirer.service.radioreference;

public class RadioReferenceTest
{

    /**
     * Tests the handling of Radio Reference's Premium Expiration date to Account Statuses
     * Necessary because DateTime math gets easy to do incorrectly, and we don't want to
     * lock a user out of access due to an off-by-one Date issue.
     */
//    @Test
    void checkExpDate()
    {
//        SimpleDateFormat RRDateFormatter = new SimpleDateFormat("MM-dd-yyyy");
//        Date now = new Date();
//
//        Calendar c = Calendar.getInstance();
//        c.setTime(now);
//
//        // Never had premium (expired)
//        Assertions.assertEquals(RadioReference.LoginStatus.EXPIRED_PREMIUM, CheckExpDate("12-31-1969"));
//
//        // Expiring today (valid)
//        Assertions.assertEquals(RadioReference.LoginStatus.VALID_PREMIUM, CheckExpDate(RRDateFormatter.format(c.getTime())));
//
//        // Expiring next week (valid)
//        c.add(Calendar.DAY_OF_MONTH, 7);
//        Assertions.assertEquals(RadioReference.LoginStatus.VALID_PREMIUM, CheckExpDate(RRDateFormatter.format(c.getTime())));
//
//        // Expired yesterday (valid)
//        c.setTime(now);
//        c.add(Calendar.DAY_OF_MONTH, -1);
//        Assertions.assertEquals(RadioReference.LoginStatus.VALID_PREMIUM, CheckExpDate(RRDateFormatter.format(c.getTime())));
//
//        // Expired last week (expired)
//        c.setTime(now);
//        c.add(Calendar.DAY_OF_MONTH, -7);
//        Assertions.assertEquals(RadioReference.LoginStatus.EXPIRED_PREMIUM, CheckExpDate(RRDateFormatter.format(c.getTime())));
//
//        // Broadcastify user? Admin? Any string that's not a Date is considered valid (valid)
//        Assertions.assertEquals(RadioReference.LoginStatus.VALID_PREMIUM, CheckExpDate("Not a date"));
    }
}
