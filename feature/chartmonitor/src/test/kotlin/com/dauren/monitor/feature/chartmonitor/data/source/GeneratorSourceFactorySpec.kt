package com.dauren.monitor.feature.chartmonitor.data.source

import com.dauren.monitor.core.common.time.MonotonicClock
import com.dauren.monitor.core.common.time.WallClock
import com.dauren.monitor.feature.chartmonitor.domain.model.SourceStatus
import io.kotest.core.spec.style.FunSpec
import io.kotest.inspectors.forAll
import io.kotest.matchers.collections.shouldBeUnique
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.longs.shouldBeInRange
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlin.random.Random

class GeneratorSourceFactorySpec :
    FunSpec({

        val sources =
            GeneratorSourceFactory(
                config = GeneratorConfig(colorsArgb = (1..10).toList()),
                random = Random(7),
                clock = MonotonicClock { 0L },
                wallClock = WallClock { 0L },
                dispatcher = Dispatchers.Unconfined,
            ).create()

        test("creates exactly ten generators") {
            sources shouldHaveSize 10
        }

        test("names and colors are unique across generators") {
            sources.map { it.info.name }.shouldBeUnique()
            sources.map { it.info.colorArgb }.shouldBeUnique()
            sources.first().info.name shouldBe "Generator #1"
            sources.last().info.name shouldBe "Generator #10"
        }

        test("lifetime is randomised between 1 and 30 minutes") {
            sources
                .map { (it.status.value as SourceStatus.Active).deadlineMs }
                .forAll { it shouldBeInRange 60_000L..1_800_000L }
        }
    })
