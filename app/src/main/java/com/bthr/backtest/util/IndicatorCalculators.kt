package com.bthr.backtest.util

import com.bthr.backtest.model.Candle
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object IndicatorCalculators {

    private fun getValues(candles: List<Candle>, source: String): List<Float> {
        return candles.map { 
            when (source) {
                "Open" -> it.open
                "High" -> it.high
                "Low" -> it.low
                "HL2" -> (it.high + it.low) / 2f
                "HLC3" -> (it.high + it.low + it.close) / 3f
                "OHLC4" -> (it.open + it.high + it.low + it.close) / 4f
                else -> it.close
            }
        }
    }

    fun calculateSMA(candles: List<Candle>, period: Int, source: String = "Close"): List<Float?> {
        val values = getValues(candles, source)
        return calculateSMAFromValues(values.map { it as Float? }, period)
    }

    fun calculateSMAFromValues(values: List<Float?>, period: Int): List<Float?> {
        val result = MutableList<Float?>(values.size) { null }
        if (values.size < period) return result

        var sum = 0f
        var count = 0
        for (i in values.indices) {
            val v = values[i]
            if (v != null) {
                sum += v
                count++
                if (count > period) {
                    val oldV = values[i - period]
                    if (oldV != null) {
                        sum -= oldV
                        count--
                    }
                }
                if (count == period) {
                    result[i] = sum / period
                }
            } else {
                sum = 0f
                count = 0
            }
        }
        return result
    }

    fun calculateEMA(candles: List<Candle>, period: Int, source: String = "Close"): List<Float?> {
        val values = getValues(candles, source)
        return calculateEMAFromValues(values.map { it as Float? }, period)
    }

    fun calculateEMAFromValues(values: List<Float?>, period: Int): List<Float?> {
        val result = MutableList<Float?>(values.size) { null }
        val alpha = 2f / (period + 1)
        var ema: Float? = null
        var count = 0
        var sum = 0f

        for (i in values.indices) {
            val v = values[i]
            if (v != null) {
                if (ema == null) {
                    sum += v
                    count++
                    if (count == period) {
                        ema = sum / period
                        result[i] = ema
                    }
                } else {
                    ema = (v - ema!!) * alpha + ema!!
                    result[i] = ema
                }
            } else {
                ema = null
                sum = 0f
                count = 0
            }
        }
        return result
    }

    fun calculateWMA(candles: List<Candle>, period: Int, source: String = "Close"): List<Float?> {
        val values = getValues(candles, source)
        return calculateWMAFromValues(values.map { it as Float? }, period)
    }

    private fun calculateWMAFromValues(values: List<Float?>, period: Int): List<Float?> {
        val result = MutableList<Float?>(values.size) { null }
        val weightSum = (period * (period + 1)) / 2f
        for (i in values.indices) {
            if (i >= period - 1) {
                var sum = 0f
                var valid = true
                for (j in 0 until period) {
                    val v = values[i - j]
                    if (v == null) { valid = false; break }
                    sum += v * (period - j)
                }
                if (valid) result[i] = sum / weightSum
            }
        }
        return result
    }

    fun calculateHMA(candles: List<Candle>, period: Int, source: String = "Close"): List<Float?> {
        val values = getValues(candles, source).map { it as Float? }
        if (values.size < period) return List(values.size) { null }
        
        val halfPeriod = period / 2
        val sqrtPeriod = sqrt(period.toDouble()).toInt()
        
        val wmaHalf = calculateWMAFromValues(values, halfPeriod)
        val wmaFull = calculateWMAFromValues(values, period)
        
        val diffList = MutableList<Float?>(values.size) { i ->
            val h = wmaHalf[i]
            val f = wmaFull[i]
            if (h != null && f != null) 2 * h - f else null
        }
        
        return calculateWMAFromValues(diffList, sqrtPeriod)
    }

    data class BollingerBandsResult(
        val upper: List<Float?>,
        val middle: List<Float?>,
        val lower: List<Float?>
    )

    fun calculateBollingerBands(candles: List<Candle>, period: Int, stdDev: Float): BollingerBandsResult {
        val values = candles.map { it.close }
        val middle = calculateSMAFromValues(values.map { it as Float? }, period)
        val upper = MutableList<Float?>(values.size) { null }
        val lower = MutableList<Float?>(values.size) { null }

        for (i in values.indices) {
            val mid = middle[i]
            if (mid != null) {
                val subset = values.subList(i - period + 1, i + 1)
                val variance = subset.map { (it - mid) * (it - mid) }.sum() / period
                val dev = sqrt(variance) * stdDev
                upper[i] = mid + dev
                lower[i] = mid - dev
            }
        }
        return BollingerBandsResult(upper, middle, lower)
    }

    data class ATRBandsResult(
        val upper: List<Float?>,
        val lower: List<Float?>,
        val tpUpper: List<Float?>,
        val tpLower: List<Float?>
    )

    fun calculateATRBands(
        candles: List<Candle>,
        period: Int,
        multiplier: Float,
        source: String = "Close",
        showTP: Boolean = false,
        tpScale: Float = 1.5f
    ): ATRBandsResult {
        val atr = calculateWilderATR(candles, period)
        val n = candles.size
        val upper = MutableList<Float?>(n) { null }
        val lower = MutableList<Float?>(n) { null }
        val tpUpper = MutableList<Float?>(n) { null }
        val tpLower = MutableList<Float?>(n) { null }

        for (i in candles.indices) {
            val a = atr[i] ?: continue
            val scaledAtr = a * multiplier
            
            val srcValUpper = when (source) {
                "Wicks" -> candles[i].high
                else -> candles[i].close
            }
            val srcValLower = when (source) {
                "Wicks" -> candles[i].low
                else -> candles[i].close
            }
            
            upper[i] = srcValUpper + scaledAtr
            lower[i] = srcValLower - scaledAtr

            if (showTP) {
                tpUpper[i] = candles[i].close + ((candles[i].close - lower[i]!!) * tpScale)
                tpLower[i] = candles[i].close - ((upper[i]!! - candles[i].close) * tpScale)
            }
        }
        return ATRBandsResult(upper, lower, tpUpper, tpLower)
    }

    fun calculateWilderATR(candles: List<Candle>, period: Int): List<Float?> {
        if (candles.isEmpty()) return emptyList()
        val tr = MutableList(candles.size) { i ->
            if (i == 0) candles[i].high - candles[i].low
            else maxOf(candles[i].high - candles[i].low, abs(candles[i].high - candles[i - 1].close), abs(candles[i].low - candles[i - 1].close))
        }
        val result = MutableList<Float?>(candles.size) { null }
        if (tr.size < period) return result

        var currentAtr = tr.take(period).sum() / period
        result[period - 1] = currentAtr
        val alpha = 1f / period
        for (i in period until tr.size) {
            currentAtr = alpha * tr[i] + (1 - alpha) * currentAtr
            result[i] = currentAtr
        }
        return result
    }

    fun calculateRSI(candles: List<Candle>, period: Int, source: String = "Close"): List<Float?> {
        val values = getValues(candles, source)
        val result = MutableList<Float?>(values.size) { null }
        if (values.size < period + 1) return result

        val gains = MutableList(values.size - 1) { i -> max(0f, values[i + 1] - values[i]) }
        val losses = MutableList(values.size - 1) { i -> max(0f, values[i] - values[i + 1]) }

        var avgGain = gains.take(period).sum() / period
        var avgLoss = losses.take(period).sum() / period

        fun calcRsi(g: Float, l: Float): Float {
            if (l == 0f) return 100f
            return 100f - (100f / (1f + g / l))
        }

        result[period] = calcRsi(avgGain, avgLoss)

        for (i in period until gains.size) {
            avgGain = (avgGain * (period - 1) + gains[i]) / period
            avgLoss = (avgLoss * (period - 1) + losses[i]) / period
            result[i + 1] = calcRsi(avgGain, avgLoss)
        }
        return result
    }

    data class MACDResult(
        val macdLine: List<Float?>,
        val signalLine: List<Float?>,
        val histogram: List<Float?>
    )

    fun calculateMACD(candles: List<Candle>, fastPeriod: Int, slowPeriod: Int, signalPeriod: Int): MACDResult {
        val values = candles.map { it.close as Float? }
        val fastEma = calculateEMAFromValues(values, fastPeriod)
        val slowEma = calculateEMAFromValues(values, slowPeriod)
        
        val macdLine = MutableList<Float?>(values.size) { i ->
            val f = fastEma[i]; val s = slowEma[i]
            if (f != null && s != null) f - s else null
        }
        val signalLine = calculateEMAFromValues(macdLine, signalPeriod)
        val histogram = MutableList<Float?>(values.size) { i ->
            val m = macdLine[i]; val s = signalLine[i]
            if (m != null && s != null) m - s else null
        }
        return MACDResult(macdLine, signalLine, histogram)
    }

    data class StochResult(
        val k: List<Float?>,
        val d: List<Float?>
    )

    fun calculateStochastic(candles: List<Candle>, kPeriod: Int, kSmoothing: Int, dPeriod: Int): StochResult {
        val rawK = MutableList<Float?>(candles.size) { i ->
            if (i < kPeriod - 1) null
            else {
                val subset = candles.subList(i - kPeriod + 1, i + 1)
                val high = subset.maxOf { it.high }
                val low = subset.minOf { it.low }
                if (high == low) 100f else (candles[i].close - low) / (high - low) * 100f
            }
        }
        val kLine = calculateSMAFromValues(rawK, kSmoothing)
        val dLine = calculateSMAFromValues(kLine, dPeriod)
        return StochResult(kLine, dLine)
    }

    fun calculateATR(candles: List<Candle>, period: Int): List<Float?> {
        if (candles.isEmpty()) return emptyList()
        val tr = MutableList(candles.size) { i ->
            if (i == 0) candles[i].high - candles[i].low
            else maxOf(candles[i].high - candles[i].low, abs(candles[i].high - candles[i - 1].close), abs(candles[i].low - candles[i - 1].close))
        }
        val result = MutableList<Float?>(candles.size) { null }
        if (tr.size < period) return result

        var currentAtr = tr.take(period).sum() / period
        result[period - 1] = currentAtr
        for (i in period until tr.size) {
            currentAtr = (currentAtr * (period - 1) + tr[i]) / period
            result[i] = currentAtr
        }
        return result
    }

    data class SupertrendResult(
        val values: List<Float?>,
        val isUp: List<Boolean>
    )

    fun calculateSupertrend(candles: List<Candle>, period: Int, multiplier: Float): SupertrendResult {
        val n = candles.size
        if (n < period) return SupertrendResult(List(n) { null }, List(n) { true })

        val atr = calculateATR(candles, period)
        val hl2 = candles.map { (it.high + it.low) / 2f }
        val supertrend = MutableList<Float?>(n) { null }
        val isUp = MutableList(n) { true }
        val upperBand = MutableList(n) { 0f }
        val lowerBand = MutableList(n) { 0f }

        for (i in 0 until n) {
            val a = atr[i] ?: continue
            val ub = hl2[i] + (multiplier * a)
            val lb = hl2[i] - (multiplier * a)
            
            if (i > 0 && supertrend[i-1] != null) {
                upperBand[i] = if (ub < upperBand[i-1] || candles[i-1].close > upperBand[i-1]) ub else upperBand[i-1]
                lowerBand[i] = if (lb > lowerBand[i-1] || candles[i-1].close < lowerBand[i-1]) lb else lowerBand[i-1]
            } else {
                upperBand[i] = ub; lowerBand[i] = lb
            }

            if (i > 0 && supertrend[i-1] != null) {
                isUp[i] = if (supertrend[i-1] == upperBand[i-1]) candles[i].close > upperBand[i] else candles[i].close >= lowerBand[i]
            }
            supertrend[i] = if (isUp[i]) lowerBand[i] else upperBand[i]
        }
        return SupertrendResult(supertrend, isUp)
    }

    data class AlligatorResult(
        val jaw: List<Float?>,
        val teeth: List<Float?>,
        val lips: List<Float?>
    )

    fun calculateAlligator(
        candles: List<Candle>,
        jawPeriod: Int = 13, jawOffset: Int = 8,
        teethPeriod: Int = 8, teethOffset: Int = 5,
        lipsPeriod: Int = 5, lipsOffset: Int = 3
    ): AlligatorResult {
        val hl2 = candles.map { (it.high + it.low) / 2f }
        val rawJaw = calculateSMMA(hl2, jawPeriod)
        val rawTeeth = calculateSMMA(hl2, teethPeriod)
        val rawLips = calculateSMMA(hl2, lipsPeriod)

        fun applyOffset(values: List<Float?>, offset: Int): List<Float?> {
            val result = MutableList<Float?>(values.size + offset) { null }
            for (i in values.indices) {
                result[i + offset] = values[i]
            }
            return result
        }

        return AlligatorResult(
            jaw = applyOffset(rawJaw, jawOffset),
            teeth = applyOffset(rawTeeth, teethOffset),
            lips = applyOffset(rawLips, lipsOffset)
        )
    }

    fun calculateSMMA(values: List<Float>, period: Int): List<Float?> {
        val result = MutableList<Float?>(values.size) { null }
        if (values.size < period) return result
        var currentSmma = values.take(period).sum() / period
        result[period - 1] = currentSmma
        for (i in period until values.size) {
            currentSmma = (currentSmma * (period - 1) + values[i]) / period
            result[i] = currentSmma
        }
        return result
    }

    data class IchimokuResult(
        val tenkanSen: List<Float?>,
        val kijunSen: List<Float?>,
        val senkouSpanA: List<Float?>,
        val senkouSpanB: List<Float?>,
        val chikouSpan: List<Float?>
    )

    fun calculateIchimoku(
        candles: List<Candle>,
        tenkanPeriod: Int = 9,
        kijunPeriod: Int = 26,
        senkouBPeriod: Int = 52,
        displacement: Int = 26
    ): IchimokuResult {
        val n = candles.size
        val tenkanSen = MutableList<Float?>(n) { null }
        val kijunSen = MutableList<Float?>(n) { null }
        val senkouSpanA = MutableList<Float?>(n + displacement) { null }
        val senkouSpanB = MutableList<Float?>(n + displacement) { null }
        val chikouSpan = MutableList<Float?>(n) { null }
        val highPrices = candles.map { it.high }; val lowPrices = candles.map { it.low }; val closePrices = candles.map { it.close }

        for (i in 0 until n) {
            if (i >= tenkanPeriod - 1) {
                tenkanSen[i] = (highPrices.subList(i - tenkanPeriod + 1, i + 1).max() + lowPrices.subList(i - tenkanPeriod + 1, i + 1).min()) / 2f
            }
            if (i >= kijunPeriod - 1) {
                kijunSen[i] = (highPrices.subList(i - kijunPeriod + 1, i + 1).max() + lowPrices.subList(i - kijunPeriod + 1, i + 1).min()) / 2f
            }
        }
        for (i in 0 until n) {
            if (tenkanSen[i] != null && kijunSen[i] != null) {
                val targetIdx = i + displacement
                if (targetIdx < n + displacement) senkouSpanA[targetIdx] = (tenkanSen[i]!! + kijunSen[i]!!) / 2f
            }
            if (i >= senkouBPeriod - 1) {
                val targetIdx = i + displacement
                if (targetIdx < n + displacement) senkouSpanB[targetIdx] = (highPrices.subList(i - senkouBPeriod + 1, i + 1).max() + lowPrices.subList(i - senkouBPeriod + 1, i + 1).min()) / 2f
            }
            val pastIdx = i - displacement
            if (pastIdx >= 0) chikouSpan[pastIdx] = closePrices[i]
        }
        return IchimokuResult(tenkanSen, kijunSen, senkouSpanA, senkouSpanB, chikouSpan)
    }

    fun calculateVolumeMA(candles: List<Candle>, length: Int, type: String): List<Float?> {
        val volumes = candles.map { it.volume as Float? }
        return if (type == "EMA") calculateEMAFromValues(volumes, length) else calculateSMAFromValues(volumes, length)
    }

    data class VWAPResult(
        val vwap: List<Float?>,
        val upper1: List<Float?>,
        val lower1: List<Float?>,
        val upper2: List<Float?>,
        val lower2: List<Float?>,
        val upper3: List<Float?>,
        val lower3: List<Float?>
    )

    fun calculateVWAP(
        candles: List<Candle>,
        anchor: String,
        source: String = "HLC3",
        mult1: Float = 1.0f,
        mult2: Float = 2.0f,
        mult3: Float = 3.0f
    ): VWAPResult {
        val n = candles.size
        val vwapValues = MutableList<Float?>(n) { null }
        val upper1 = MutableList<Float?>(n) { null }
        val lower1 = MutableList<Float?>(n) { null }
        val upper2 = MutableList<Float?>(n) { null }
        val lower2 = MutableList<Float?>(n) { null }
        val upper3 = MutableList<Float?>(n) { null }
        val lower3 = MutableList<Float?>(n) { null }

        if (n == 0) return VWAPResult(vwapValues, upper1, lower1, upper2, lower2, upper3, lower3)

        val srcValues = getValues(candles, source)
        
        var sumPV = 0f
        var sumV = 0f
        var sumP2V = 0f

        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val prevCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))

        for (i in 0 until n) {
            val candle = candles[i]
            val src = srcValues[i]
            val vol = candle.volume

            var isNewPeriod = false
            if (i > 0) {
                cal.timeInMillis = candle.timestamp
                prevCal.timeInMillis = candles[i - 1].timestamp
                
                isNewPeriod = when (anchor) {
                    "Session" -> cal.get(Calendar.DAY_OF_YEAR) != prevCal.get(Calendar.DAY_OF_YEAR)
                    "Week" -> cal.get(Calendar.WEEK_OF_YEAR) != prevCal.get(Calendar.WEEK_OF_YEAR)
                    "Month" -> cal.get(Calendar.MONTH) != prevCal.get(Calendar.MONTH)
                    "Quarter" -> (cal.get(Calendar.MONTH) / 3) != (prevCal.get(Calendar.MONTH) / 3)
                    "Year" -> cal.get(Calendar.YEAR) != prevCal.get(Calendar.YEAR)
                    "Decade" -> (cal.get(Calendar.YEAR) / 10) != (prevCal.get(Calendar.YEAR) / 10)
                    "Century" -> (cal.get(Calendar.YEAR) / 100) != (prevCal.get(Calendar.YEAR) / 100)
                    else -> false
                }
            } else {
                isNewPeriod = true
            }

            if (isNewPeriod) {
                sumPV = 0f
                sumV = 0f
                sumP2V = 0f
            }

            sumPV += src * vol
            sumV += vol
            sumP2V += src * src * vol

            if (sumV > 0) {
                val vwap = sumPV / sumV
                vwapValues[i] = vwap
                
                val variance = (sumP2V / sumV) - (vwap * vwap)
                val stdev = sqrt(max(0f, variance))
                
                upper1[i] = vwap + stdev * mult1
                lower1[i] = vwap - stdev * mult1
                upper2[i] = vwap + stdev * mult2
                lower2[i] = vwap - stdev * mult2
                upper3[i] = vwap + stdev * mult3
                lower3[i] = vwap - stdev * mult3
            }
        }

        return VWAPResult(vwapValues, upper1, lower1, upper2, lower2, upper3, lower3)
    }

    data class RibbonResult(
        val mas: List<List<Float?>>,
        val refMa: List<Float?>
    )

    fun calculateRibbon(
        candles: List<Candle>,
        isExponential: Boolean,
        source: String,
        refPeriod: Int = 100
    ): RibbonResult {
        val srcValues = getValues(candles, source).map { it as Float? }
        val periods = listOf(15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90)
        
        val mas = periods.map { p ->
            if (isExponential) calculateEMAFromValues(srcValues, p)
            else calculateSMAFromValues(srcValues, p)
        }
        
        val refMa = if (isExponential) calculateEMAFromValues(srcValues, refPeriod)
                    else calculateSMAFromValues(srcValues, refPeriod)
                    
        return RibbonResult(mas, refMa)
    }
}
