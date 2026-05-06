package com.bthr.backtest.data.network

import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    /**
     * Fetch K-line (candlestick) data from Binance.
     * symbol: e.g., "PAXGUSDT" (Gold/Dollar proxy)
     * interval: 1m, 5m, 15m, 1h, 4h, 1d, etc.
     */
    @GET("api/v3/klines")
    suspend fun getCandles(
        @Query("symbol") symbol: String = "PAXGUSDT",
        @Query("interval") interval: String,
        @Query("limit") limit: Int = 1000
    ): List<List<Any>>
}
