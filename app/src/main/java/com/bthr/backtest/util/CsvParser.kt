package com.bthr.backtest.util

import android.content.Context
import com.bthr.backtest.model.Candle
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.StringTokenizer

object CsvParser {
    fun loadCandlesFromAssets(context: Context, fileName: String): List<Candle> {
        val candles = mutableListOf<Candle>()
        
        // formats possibles
        val dateTimeFormat = SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val dateOnlyFormat = SimpleDateFormat("yyyy.MM.dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        try {
            context.assets.open(fileName).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    val header = reader.readLine() // Skip header
                    
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val st = StringTokenizer(line!!, " \t")
                        val tokenCount = st.countTokens()
                        
                        if (tokenCount >= 6) {
                            try {
                                val firstToken = st.nextToken() // <DATE>
                                
                                // Si le deuxième token contient ":" c'est une heure, sinon c'est le prix OPEN
                                val nextToken = st.nextToken()
                                
                                val timestamp: Long
                                val open: Float
                                
                                if (nextToken.contains(":")) {
                                    // Format avec HEURE (ex: 2024.01.01 12:00:00)
                                    timestamp = dateTimeFormat.parse("$firstToken $nextToken")?.time ?: 0L
                                    open = st.nextToken().toFloat()
                                } else {
                                    // Format DATE SEULE (ex: 2024.01.01) -> Souvent le cas en 1D, 1W, 1MN
                                    timestamp = dateOnlyFormat.parse(firstToken)?.time ?: 0L
                                    open = nextToken.toFloat()
                                }
                                
                                candles.add(
                                    Candle(
                                        timestamp = timestamp,
                                        open = open,
                                        high = st.nextToken().toFloat(),
                                        low = st.nextToken().toFloat(),
                                        close = st.nextToken().toFloat(),
                                        volume = st.nextToken().toFloat()
                                    )
                                )
                            } catch (e: Exception) {
                                // Skip invalid lines
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // File might not exist or other error
        }
        return candles
    }
}
