package com.vitalcore.app.domain.calculations

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BaselineEngineTest {

    @Test
    fun `empty history returns null baseline`() {
        assertNull(BaselineEngine.compute(emptyList()))
    }

    @Test
    fun `single sample produces a baseline with zero spread`() {
        val stats = BaselineEngine.compute(listOf(50.0))!!
        assertEquals(50.0, stats.mean, 0.001)
        assertEquals(0.0, stats.standardDeviation, 0.001)
        assertEquals(1, stats.sampleCount)
    }

    @Test
    fun `maturity reflects sample count thresholds`() {
        assertEquals(BaselineMaturity.COLLECTING, BaselineEngine.compute(List(3) { 50.0 })!!.maturity)
        assertEquals(BaselineMaturity.INITIAL, BaselineEngine.compute(List(10) { 50.0 })!!.maturity)
        assertEquals(BaselineMaturity.BUILDING, BaselineEngine.compute(List(20) { 50.0 })!!.maturity)
        assertEquals(BaselineMaturity.ROBUST, BaselineEngine.compute(List(35) { 50.0 })!!.maturity)
    }

    @Test
    fun `window is capped at 60 most recent samples`() {
        val history = (1..100).map { it.toDouble() }
        val stats = BaselineEngine.compute(history)!!
        assertEquals(60, stats.sampleCount)
        // Only the last 60 values (41..100) should be represented -> mean 70.5
        assertEquals(70.5, stats.mean, 0.001)
    }

    @Test
    fun `outlier rejection removes values beyond 4 standard deviations`() {
        val history = List(20) { 50.0 } + listOf(5000.0) // one wild outlier
        val cleaned = BaselineEngine.withoutOutliers(history)
        assertTrue(5000.0 !in cleaned)
    }

    @Test
    fun `outlier rejection is a no-op with too little history`() {
        val history = listOf(10.0, 5000.0, 20.0)
        assertEquals(history, BaselineEngine.withoutOutliers(history))
    }

    @Test
    fun `deviation is null when standard deviation is zero`() {
        val stats = BaselineEngine.compute(listOf(50.0, 50.0, 50.0))!!
        assertNull(stats.deviation(60.0))
    }

    @Test
    fun `percentDelta reflects positive and negative deviation`() {
        val stats = BaselineEngine.compute(listOf(40.0, 50.0, 60.0))!! // mean = 50
        assertEquals(20.0, stats.percentDelta(60.0)!!, 0.01)
        assertEquals(-20.0, stats.percentDelta(40.0)!!, 0.01)
    }
}
