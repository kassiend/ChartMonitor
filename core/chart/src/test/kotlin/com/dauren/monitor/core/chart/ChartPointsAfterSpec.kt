package com.dauren.monitor.core.chart

import com.dauren.monitor.core.common.model.Points
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.property.Arb
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll

class ChartPointsAfterSpec :
    FunSpec({

        test("empty series returns Empty") {
            Points.Empty.after(afterMs = 0L) shouldBeSameInstanceAs Points.Empty
        }

        test("all points older → size 0") {
            val series = Points(longArrayOf(1_000L, 2_000L, 3_000L), doubleArrayOf(0.1, 0.2, 0.3))

            series.after(afterMs = 3_000L).size shouldBe 0
            series.after(afterMs = 10_000L).size shouldBe 0
        }

        test("exact match on last timestamp → size 0 (strict >)") {
            val series = Points(longArrayOf(1_000L, 2_000L), doubleArrayOf(0.1, 0.2))

            series.after(afterMs = 2_000L).size shouldBe 0
        }

        test("exact match on interior timestamp → suffix starting after that index") {
            val series = Points(
                longArrayOf(1_000L, 2_000L, 3_000L, 4_000L),
                doubleArrayOf(0.1, 0.2, 0.3, 0.4),
            )

            val tail = series.after(afterMs = 2_000L)
            tail.timestampsMs.toList() shouldBe listOf(3_000L, 4_000L)
            tail.values.toList() shouldBe listOf(0.3, 0.4)
        }

        test("afterMs below min → identity (same instance, no copy)") {
            val series = Points(longArrayOf(1_000L, 2_000L), doubleArrayOf(0.1, 0.2))

            series.after(afterMs = -1L) shouldBeSameInstanceAs series
            series.after(afterMs = 999L) shouldBeSameInstanceAs series
        }

        test("afterMs between points → cuts at the gap") {
            val series = Points(
                longArrayOf(1_000L, 2_000L, 5_000L, 6_000L),
                doubleArrayOf(0.1, 0.2, 0.5, 0.6),
            )

            series.after(afterMs = 3_500L).timestampsMs.toList() shouldBe listOf(5_000L, 6_000L)
        }

        test("result is a subset of the original and all strictly > afterMs") {
            checkAll(
                Arb.list(Arb.long(0L..10_000L), 0..200),
                Arb.long(-1L..10_001L),
            ) { raw, threshold ->
                val sorted = raw.distinct().sorted().toLongArray()
                val series = Points(sorted, DoubleArray(sorted.size) { it.toDouble() })

                val expected = sorted.filter { it > threshold }
                series.after(threshold).timestampsMs.toList() shouldBe expected
            }
        }
    })
