package com.dauren.monitor.feature.chartmonitor.data.buffer

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll

class PointRingBufferSpec :
    FunSpec({

        test("keeps only the latest points in order for any capacity and load") {
            checkAll(Arb.int(1..64), Arb.int(0..500)) { capacity, count ->
                val buffer = PointRingBuffer(capacity, initialCapacity = 4)

                repeat(count) { buffer.append(it.toLong(), it.toDouble()) }

                val expected = (maxOf(0, count - capacity) until count).map(Int::toLong)
                buffer.snapshot().timestampsMs.toList() shouldBe expected
            }
        }

        test("empty buffer returns Points.Empty") {
            val buffer = PointRingBuffer(capacity = 10)

            buffer.snapshot().timestampsMs.toList().shouldBeEmpty()
            buffer.snapshot().values.toList().shouldBeEmpty()
            buffer.currentSize shouldBe 0
        }

        test("rejects non-positive capacity") {
            shouldThrow<IllegalArgumentException> { PointRingBuffer(0) }
            shouldThrow<IllegalArgumentException> { PointRingBuffer(-1) }
        }

        test("rejects non-positive initialCapacity") {
            shouldThrow<IllegalArgumentException> { PointRingBuffer(10, initialCapacity = 0) }
        }

        test("initialCapacity capped at capacity: tiny capacity works without wasting memory") {
            val buffer = PointRingBuffer(capacity = 3, initialCapacity = 1_024)

            repeat(5) { buffer.append(it.toLong(), it.toDouble()) }

            buffer.snapshot().timestampsMs.toList() shouldBe listOf(2L, 3L, 4L)
        }
    })
