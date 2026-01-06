package com.strivacity.android.native_sdk.mocks

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.time.Duration
import kotlin.time.toJavaDuration

class MutableTestClock(
    private var currentTime: Instant = Instant.now(),
    private val _zone: ZoneId = systemUTC().zone,
) : Clock() {

  override fun getZone(): ZoneId = _zone

  override fun instant(): Instant = currentTime

  override fun withZone(zone: ZoneId): Clock {
    throw NotImplementedError()
  }

  fun set(now: Instant) {
    currentTime = now
  }

  fun advanceBy(amount: Duration) {
    currentTime += amount.toJavaDuration()
  }
}
