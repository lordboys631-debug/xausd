package com.bthr.backtest.util

import com.bthr.backtest.model.Candle
import com.bthr.backtest.model.Timeframe

object CandleUtil {
    fun aggregate(candles: List<Candle>, timeframe: Timeframe): List<Candle> {
        if (candles.isEmpty() || timeframe == Timeframe.M1) return candles

        val intervalMs = timeframe.minutes * 60 * 1000L
        val result = mutableListOf<Candle>()
        
        var currentGroupTimestamp = (candles[0].timestamp / intervalMs) * intervalMs
        var groupOpen = candles[0].open
        var groupHigh = candles[0].high
        var groupLow = candles[0].low
        var groupClose = candles[0].close
        var groupVolume = candles[0].volume

        for (i in 1 until candles.size) {
            val candle = candles[i]
            val groupTimestamp = (candle.timestamp / intervalMs) * intervalMs

            if (groupTimestamp == currentGroupTimestamp) {
                groupHigh = maxOf(groupHigh, candle.high)
                groupLow = minOf(groupLow, candle.low)
                groupClose = candle.close
                groupVolume += candle.volume
            } else {
                result.add(Candle(currentGroupTimestamp, groupOpen, groupHigh, groupLow, groupClose, groupVolume))
                
                currentGroupTimestamp = groupTimestamp
                groupOpen = candle.open
                groupHigh = candle.high
                groupLow = candle.low
                groupClose = candle.close
                groupVolume = candle.volume
            }
        }
        
        // Add the last group
        result.add(Candle(currentGroupTimestamp, groupOpen, groupHigh, groupLow, groupClose, groupVolume))

        return result
    }
}
