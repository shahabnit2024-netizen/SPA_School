package com.example

import com.example.data.model.daysUntilEligible
import com.example.data.model.isEligibleBloodDonor
import com.example.data.model.maskPhoneNumber
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date
import java.util.concurrent.TimeUnit

class BloodEligibilityAndPrivacyTest {

    @Test
    fun `donor with null lastBloodDonateDate is eligible`() {
        assertTrue(isEligibleBloodDonor(null))
        assertEquals(0L, daysUntilEligible(null))
    }

    @Test
    fun `donor with 90 days ago donation is not yet eligible`() {
        val now = 1_700_000_000_000L
        val ninetyDaysAgo = Timestamp(Date(now - TimeUnit.DAYS.toMillis(90)))
        assertFalse(isEligibleBloodDonor(ninetyDaysAgo, now))
        assertEquals(1L, daysUntilEligible(ninetyDaysAgo, now))
    }

    @Test
    fun `donor with 91 days ago donation is eligible`() {
        val now = 1_700_000_000_000L
        val ninetyOneDaysAgo = Timestamp(Date(now - TimeUnit.DAYS.toMillis(91)))
        assertTrue(isEligibleBloodDonor(ninetyOneDaysAgo, now))
        assertEquals(0L, daysUntilEligible(ninetyOneDaysAgo, now))
    }

    @Test
    fun `maskPhoneNumber hides prefix and keeps last 3 digits`() {
        assertEquals("••••••678", maskPhoneNumber("+8801712345678"))
        assertEquals("••••", maskPhoneNumber("123"))
    }
}
