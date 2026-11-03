package com.samuelcruz.bangtinnha.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.samuelcruz.bangtinnha.core.VN_ZONE
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** A line chart (all Vico code lives in this file). [xLabel] turns an x value into the bottom-axis label. */
@Composable
fun LineChart(xs: List<Double>, ys: List<Double>, xLabel: (Double) -> String, modifier: Modifier = Modifier) {
    if (xs.size < 2) return
    val producer = remember { CartesianChartModelProducer() }
    LaunchedEffect(xs, ys) {
        producer.runTransaction { lineModel { series(x = xs, y = ys) } }
    }
    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = CartesianValueFormatter { _, value, _ -> xLabel(value) }),
        ),
        modelProducer = producer,
        modifier = modifier.fillMaxWidth().height(220.dp),
    )
}

private val dayFormat = DateTimeFormatter.ofPattern("dd/MM")

/**
 * Price history as a day-by-day line. Prices only get a row when they change, so the last value is carried to
 * today; several changes on one day keep the last.
 */
@Composable
fun PriceHistoryChart(history: List<Pair<Long, Long>>, toValue: (Long) -> Double, modifier: Modifier = Modifier) {
    val byDay = history.associate { (time, price) ->
        Instant.ofEpochMilli(time).atZone(VN_ZONE).toLocalDate().toEpochDay() to toValue(price)
    }.toSortedMap()
    if (byDay.isEmpty()) return
    val today = LocalDate.now(VN_ZONE).toEpochDay()
    if (byDay.lastKey() < today) byDay[today] = byDay.getValue(byDay.lastKey())
    LineChart(
        xs = byDay.keys.map { it.toDouble() },
        ys = byDay.values.toList(),
        xLabel = { LocalDate.ofEpochDay(it.toLong()).format(dayFormat) },
        modifier = modifier,
    )
}
