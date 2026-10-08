package com.dauren.monitor.feature.chartmonitor.ui

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import io.kotest.property.Arb
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll

class FormatRemainingSpec :
    FunSpec({

        test("timer is always mm:ss for valid inputs") {
            checkAll(Arb.long(0L..1_800_000L)) { ms ->
                formatRemaining(ms) shouldMatch Regex("""\d{2}:\d{2}""")
            }
        }

        test("timer formats boundary values correctly") {
            formatRemaining(1_799_999L) shouldBe "29:59"
            formatRemaining(1_000L) shouldBe "00:01"
            formatRemaining(999L) shouldBe "00:00"
            formatRemaining(0L) shouldBe "00:00"
            formatRemaining(-5L) shouldBe "00:00"
        }

        test("formatValue uses signed two-decimal format") {
            formatValue(0.0) shouldBe "+0.00"
            formatValue(0.42) shouldBe "+0.42"
            formatValue(-1.5) shouldBe "-1.50"
            formatValue(10.0) shouldBe "+10.00"
        }
    })
