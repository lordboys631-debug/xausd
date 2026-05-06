package com.bthr.backtest.model

enum class Timeframe(val displayName: String, val minutes: Int) {
    M1("1m", 1),
    M5("5m", 5),
    M10("10m", 10),
    M15("15m", 15),
    M30("30m", 30),
    H1("1H", 60),
    H4("4H", 240),
    D1("1D", 1440),
    W1("1W", 10080),
    MN1("1MN", 43200); // Approximation 30 dayss

    companion object {
        fun fromDisplayName(name: String): Timeframe {
            return values().find { it.displayName.equals(name, ignoreCase = true) } ?: H1
        }
    }
}
