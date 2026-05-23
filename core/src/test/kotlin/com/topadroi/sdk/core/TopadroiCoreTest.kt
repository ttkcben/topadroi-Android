package com.topadroi.sdk.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TopadroiCoreTest {

    // ── buildUserData ──
    @Test fun userData_alwaysIncludesAnonId() {
        val ud = TopadroiCore.buildUserData(null, null, null, false, "anon-1", null)
        assertEquals("anon-1", ud["anon_id"])
    }

    @Test fun userData_mapsTraits() {
        val ud = TopadroiCore.buildUserData("a@b.com", "+15555550123", "crm1", false, "x", null)
        assertEquals("a@b.com", ud["em"])
        assertEquals("+15555550123", ud["ph"])
        assertEquals("crm1", ud["external_id"])
    }

    @Test fun userData_gaidOnlyWhenAdTrackingGranted() {
        val denied = TopadroiCore.buildUserData(null, null, null, false, "x", "GAID-1")
        assertNull("ad tracking 未同意不得帶 gaid", denied["gaid"])
        val granted = TopadroiCore.buildUserData(null, null, null, true, "x", "GAID-1")
        assertEquals("GAID-1", granted["gaid"])
    }

    @Test fun userData_emptyGaidNotIncluded() {
        val ud = TopadroiCore.buildUserData(null, null, null, true, "x", "")
        assertNull(ud["gaid"])
    }

    // ── buildEvent ──
    @Test fun event_shapeMatchesBackendSchema() {
        val ev = TopadroiCore.buildEvent(
            "site_abc", "Purchase", "evt-1", 1_715_000_000L,
            mapOf("value" to 9.99, "currency" to "USD"),
            mapOf("anon_id" to "x"), mapOf("platform" to "android"),
            adTracking = true, analytics = false,
        )
        assertEquals("site_abc", ev["site_id"])
        assertEquals("Purchase", ev["event_name"])
        assertEquals("evt-1", ev["event_id"])
        assertEquals(1_715_000_000L, ev["event_time"])
        @Suppress("UNCHECKED_CAST")
        val consent = ev["consent"] as Map<String, Any?>
        assertEquals(true, consent["ad_tracking"])
        assertEquals(false, consent["analytics"])
        @Suppress("UNCHECKED_CAST")
        val appMeta = ev["app_metadata"] as Map<String, Any?>
        assertEquals("android", appMeta["platform"])
    }

    // ── EventQueue ──
    @Test fun queue_enqueueAndSize() {
        val q = EventQueue(100)
        q.enqueue(mapOf("event_id" to "1"))
        q.enqueue(mapOf("event_id" to "2"))
        assertEquals(2, q.size)
    }

    @Test fun queue_evictsOldestOverMax() {
        val q = EventQueue(2)
        q.enqueue(mapOf("event_id" to "1"))
        q.enqueue(mapOf("event_id" to "2"))
        val evicted = q.enqueue(mapOf("event_id" to "3"))
        assertTrue(evicted)
        assertEquals(2, q.size)
        assertEquals("2", q.snapshot().first()["event_id"])
    }

    @Test fun queue_removeByIdsAfterFlush() {
        val q = EventQueue()
        listOf("1", "2", "3").forEach { q.enqueue(mapOf("event_id" to it)) }
        q.removeByIds(setOf("1", "3"))
        assertEquals(1, q.size)
        assertEquals("2", q.snapshot().first()["event_id"])
    }

    @Test fun queue_headDoesNotMutate() {
        val q = EventQueue()
        listOf("1", "2", "3").forEach { q.enqueue(mapOf("event_id" to it)) }
        assertEquals(2, q.head(2).size)
        assertEquals(3, q.size)
    }

    @Test fun queue_loadReplaces() {
        val q = EventQueue()
        q.enqueue(mapOf("event_id" to "old"))
        q.load(listOf(mapOf("event_id" to "a"), mapOf("event_id" to "b")))
        assertEquals(2, q.size)
        assertFalse(q.snapshot().any { it["event_id"] == "old" })
    }

    // ── RetryPolicy（指數退避）──
    @Test fun retry_noFailureUsesBaseInterval() {
        assertEquals(30.0, RetryPolicy.nextDelaySeconds(0), 0.0)
        assertEquals(30.0, RetryPolicy.nextDelaySeconds(-5), 0.0)
    }

    @Test fun retry_exponentialBackoff() {
        assertEquals(30.0, RetryPolicy.nextDelaySeconds(1), 0.0)
        assertEquals(60.0, RetryPolicy.nextDelaySeconds(2), 0.0)
        assertEquals(120.0, RetryPolicy.nextDelaySeconds(3), 0.0)
        assertEquals(240.0, RetryPolicy.nextDelaySeconds(4), 0.0)
    }

    @Test fun retry_capsAtMax() {
        assertEquals(3600.0, RetryPolicy.nextDelaySeconds(8), 0.0)   // 30*2^7=3840 → 封頂
        assertEquals(3600.0, RetryPolicy.nextDelaySeconds(99), 0.0)
        assertEquals(1920.0, RetryPolicy.nextDelaySeconds(7), 0.0)   // 30*2^6=1920
    }
}
