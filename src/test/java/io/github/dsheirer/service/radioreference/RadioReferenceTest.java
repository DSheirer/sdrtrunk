package io.github.dsheirer.service.radioreference;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static io.github.dsheirer.service.radioreference.RadioReference.CheckExpDate;
import static org.junit.Assert.*;

public class RadioReferenceTest
{

    /**
     * Tests the handling of Radio Reference's Premium Expiration date to Account Statuses
     * Necessary because DateTime math gets easy to do incorrectly, and we don't want to
     * lock a user out of access due to an off-by-one Date issue.
     */
    @Test
    void checkExpDate()
    {
        SimpleDateFormat RRDateFormatter = new SimpleDateFormat("MM-dd-yyyy");
        Date now = new Date();

        Calendar c = Calendar.getInstance();
        c.setTime(now);

        // Never had premium (expired)
        Assertions.assertEquals(RadioReference.LoginStatus.EXPIRED_PREMIUM, CheckExpDate("12-31-1969"));

        // Expiring today (valid)
        Assertions.assertEquals(RadioReference.LoginStatus.VALID_PREMIUM, CheckExpDate(RRDateFormatter.format(c.getTime())));

        // Expiring next week (valid)
        c.add(Calendar.DAY_OF_MONTH, 7);
        Assertions.assertEquals(RadioReference.LoginStatus.VALID_PREMIUM, CheckExpDate(RRDateFormatter.format(c.getTime())));

        // Expired yesterday (valid)
        c.setTime(now);
        c.add(Calendar.DAY_OF_MONTH, -1);
        Assertions.assertEquals(RadioReference.LoginStatus.VALID_PREMIUM, CheckExpDate(RRDateFormatter.format(c.getTime())));

        // Expired last week (expired)
        c.setTime(now);
        c.add(Calendar.DAY_OF_MONTH, -7);
        Assertions.assertEquals(RadioReference.LoginStatus.EXPIRED_PREMIUM, CheckExpDate(RRDateFormatter.format(c.getTime())));

        // Broadcastify user? Admin? Any string that's not a Date is considered valid (valid)
        Assertions.assertEquals(RadioReference.LoginStatus.VALID_PREMIUM, CheckExpDate("Not a date"));

    }
}