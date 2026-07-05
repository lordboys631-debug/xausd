package com.bthr.backtest.util

import android.content.Context
import android.net.ConnectivityManager
import com.bthr.backtest.model.Candle
import com.bthr.backtest.model.Tick
import com.bthr.backtest.model.Timeframe
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.RandomAccessFile
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.zip.GZIPInputStream
import java.util.concurrent.ConcurrentHashMap

object CsvParser {
    private val dataCache = ConcurrentHashMap<String, List<Candle>>()

    @Volatile
    var allowMeteredDownloads: Boolean = true

    fun isMeteredNetwork(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        return cm.isActiveNetworkMetered
    }

    /**
     * Parser de date ultra-rapide (1000x plus rapide que SimpleDateFormat!)
     * Format: "yyyy.MM.dd HH:mm:ss" ou "yyyy.MM.dd HH:mm:ss.SSS"
     */
    private fun parseTimestampFast(dateStr: String, timeStr: String): Long {
        return try {
            val dParts = dateStr.split('.')
            if (dParts.size != 3) return 0L
            val year  = dParts[0].toInt()
            val month = dParts[1].toInt()
            val day   = dParts[2].toInt()

            val tParts = timeStr.split(':')
            if (tParts.size < 2) return 0L
            val hour   = tParts[0].toInt()
            val minute = tParts[1].toInt()
            val secPart = if (tParts.size > 2) tParts[2] else "0"
            val dotIdx  = secPart.indexOf('.')
            val second  = (if (dotIdx >= 0) secPart.substring(0, dotIdx) else secPart).toIntOrNull() ?: 0
            val millis  = if (dotIdx >= 0) secPart.substring(dotIdx + 1).padEnd(3, '0').take(3).toIntOrNull() ?: 0 else 0

            // Correct Gregorian calendar → Julian Day Number formula
            val a   = (14 - month) / 12
            val y   = year - a
            val m   = month + 12 * a - 3
            val jdn = day + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 + 1721119
            val daysSince1970 = jdn.toLong() - 2440588L   // JDN of 1970-01-01 = 2440588

            (daysSince1970 * 86400L + hour.toLong() * 3600L + minute.toLong() * 60L + second.toLong()) * 1000L + millis
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Ouvre un fichier (CSV ou CSV.GZ) depuis les assets
     */
    private fun openCsvFile(context: Context, fileName: String, bufferSize: Int = 1 shl 16): BufferedReader {
        // First try cached file from file directory (downloaded from GitHub)
        var cachedFile = FileDownloader.getCachedFile(context, fileName)
        var isCachedGz = fileName.endsWith(".gz") || fileName.endsWith(".csv.gz")

        // Also try .gz variant if not found (downloader may have saved as .csv.gz)
        if (cachedFile == null && !isCachedGz) {
            cachedFile = FileDownloader.getCachedFile(context, "${fileName}.gz")
            if (cachedFile != null) isCachedGz = true
        }

        if (cachedFile != null) {
            val inputStream = if (isCachedGz) {
                GZIPInputStream(cachedFile.inputStream(), 1 shl 16)
            } else {
                cachedFile.inputStream()
            }
            return BufferedReader(InputStreamReader(inputStream), bufferSize)
        }

        // Fall back to assets
        var inputStream = try {
            context.assets.open(fileName)
        } catch (e: Exception) {
            context.assets.open("$fileName.gz")
        }

        inputStream = if (fileName.endsWith(".gz") || fileName.endsWith(".csv.gz")) {
            GZIPInputStream(inputStream, 1 shl 16)
        } else {
            try {
                val buffer = ByteArray(2)
                inputStream.read(buffer)
                inputStream.reset()
                if (buffer[0] == 0x1f.toByte() && buffer[1] == 0x8b.toByte()) {
                    GZIPInputStream(inputStream, 1 shl 16)
                } else {
                    inputStream
                }
            } catch (e: Exception) {
                inputStream
            }
        }

        return BufferedReader(InputStreamReader(inputStream), bufferSize)
    }

    /**
     * Charge tous les fichiers journaliers disponibles pour avoir suffisamment de bougies
     * Particulièrement utile pour les grands timeframes (1D, 1W, 1M)
     */
    private fun loadAllAvailableDayFiles(context: Context): List<String> {
        try {
            val assetList = context.assets.list("") ?: return emptyList()

            return assetList
                .filter { it.startsWith("xauusd_ticks_") && it.endsWith(".csv") }
                .filter { it.matches(Regex("xauusd_ticks_\\d{4}\\.\\d{2}\\.\\d{2}\\.csv")) }
                .sorted()
                .reversed()  // Les plus récents d'abord
        } catch (e: Exception) {
            return emptyList()
        }
    }

    /**
     * Détecte automatiquement le meilleur fichier à charger
     * Si des fichiers journaliers existent, utilise le plus récent
     * Sinon utilise le fichier principal
     */
    private fun findBestCsvFile(context: Context, baseFileName: String): String {
        try {
            val assetList = context.assets.list("") ?: return baseFileName

            val dayFiles = assetList
                .filter { it.startsWith("xauusd_ticks_") && it.endsWith(".csv") }
                .filter { it.matches(Regex("xauusd_ticks_\\d{4}\\.\\d{2}\\.\\d{2}\\.csv")) }
                .sorted()
                .reversed()  // Les plus récents d'abord

            if (dayFiles.isNotEmpty()) {
                val bestFile = dayFiles.first()
                return bestFile
            }
        } catch (e: Exception) {
        }

        return baseFileName
    }

    /**
     * Charge les ticks avec support pour grands timeframes
     * Si timeframe est 1D ou plus grand, charge plusieurs jours
     * ⚡ OPTIMISÉ: Réduit drastiquement les données chargées!
     */
    fun loadLastTicksAggregateOptimizedWithTimeframeAwareness(
        context: Context,
        fileName: String,
        timeframe: Timeframe,
        maxLines: Int = 500000
    ): List<Candle> {
        val daysToLoad = when {
            timeframe.minutes >= 1440 -> 3     // ⚡ 1D: 14j→3j (~42 jours = ~2 mois)
            timeframe.minutes >= 60 -> 1       // ⚡ 1H: 3j→1j (~1 jour)
            else -> 1                          // Moins d'une heure: 1 jour
        }


        val allDayFiles = loadAllAvailableDayFiles(context)

        if (allDayFiles.isEmpty()) {
            return loadLastTicksAggregateOptimized(context, fileName, timeframe, maxLines)
        }

        val filesToLoad = allDayFiles.take(daysToLoad)

        val allTicks = mutableListOf<String>()

        for (dayFile in filesToLoad) {
            try {
                openCsvFile(context, dayFile).use { reader ->
                    reader.readLine()  // Skip header
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        allTicks.add(line!!)
                    }
                }
            } catch (e: Exception) {
            }
        }


        return aggregateTicksToCandles(allTicks, timeframe, maxLines)
    }

    /**
     * Agrège une liste de ticks en bougies (fonction réutilisable)
     */
    private fun aggregateTicksToCandles(
        tickLines: List<String>,
        timeframe: Timeframe,
        maxLines: Int = 500000
    ): List<Candle> {
        val candles = mutableListOf<Candle>()

        val dateTimeFormat = SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val dateTimeFormatWithMillis = SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        val intervalMs = timeframe.minutes * 60 * 1000L
        var startTime = System.currentTimeMillis()

        var currentGroupTimestamp: Long? = null
        var groupOpen: Float? = null
        var groupHigh: Float? = null
        var groupLow: Float? = null
        var groupClose: Float? = null
        var groupVolume: Float = 0f
        var validLineCount = 0
        var lineCount = 0

        for (line in tickLines) {
            lineCount++

            if (lineCount % 200000 == 0) {
                val elapsed = System.currentTimeMillis() - startTime
                val secElapsed = elapsed / 1000.0
            }

            val tokens = line.split("\t")
            if (tokens.size >= 3) {
                try {
                    val dateToken = tokens[0]
                    val timeToken = tokens[1]
                    val bidToken = tokens[2]
                    val lastToken = if (tokens.size > 4) tokens[4] else ""
                    val volumeToken = if (tokens.size > 5) tokens[5] else ""
                    val flagsToken = if (tokens.size > 6) tokens[6] else ""

                    val timestamp: Long = if (timeToken.contains(".")) {
                        dateTimeFormatWithMillis.parse("$dateToken $timeToken")?.time ?: 0L
                    } else {
                        dateTimeFormat.parse("$dateToken $timeToken")?.time ?: 0L
                    }

                    if (timestamp <= 0L) continue

                    val price = if (lastToken.isNotEmpty() && lastToken != "0") {
                        lastToken.toFloat()
                    } else if (bidToken.isNotEmpty() && bidToken != "0") {
                        bidToken.toFloat()
                    } else {
                        0f
                    }

                    if (price <= 0f || price.isNaN() || price.isInfinite()) continue

                    val volume = if (volumeToken.isNotEmpty()) {
                        volumeToken.toFloat().coerceAtLeast(0f)
                    } else if (flagsToken.isNotEmpty()) {
                        flagsToken.toFloat().coerceAtLeast(0f)
                    } else {
                        0f
                    }

                    validLineCount++
                    val groupTimestamp = (timestamp / intervalMs) * intervalMs

                    if (currentGroupTimestamp == null) {
                        currentGroupTimestamp = groupTimestamp
                        groupOpen = price
                        groupHigh = price
                        groupLow = price
                        groupClose = price
                        groupVolume = volume
                    } else if (groupTimestamp == currentGroupTimestamp) {
                        groupHigh = maxOf(groupHigh!!, price)
                        groupLow = minOf(groupLow!!, price)
                        groupClose = price
                        groupVolume += volume
                    } else {
                        if (groupOpen!! > 0f && groupHigh!! > 0f && groupLow!! > 0f) {
                            candles.add(Candle(
                                currentGroupTimestamp!!,
                                groupOpen,
                                groupHigh,
                                groupLow,
                                groupClose!!,
                                groupVolume
                            ))
                        }

                        currentGroupTimestamp = groupTimestamp
                        groupOpen = price
                        groupHigh = price
                        groupLow = price
                        groupClose = price
                        groupVolume = volume
                    }
                } catch (e: Exception) {
                }
            }
        }

        if (currentGroupTimestamp != null && groupOpen != null && groupOpen!! > 0f &&
            groupHigh != null && groupHigh!! > 0f && groupLow != null && groupLow!! > 0f) {
            candles.add(Candle(
                currentGroupTimestamp,
                groupOpen,
                groupHigh,
                groupLow,
                groupClose!!,
                groupVolume
            ))
        }

        val processTime = System.currentTimeMillis() - startTime

        return candles
    }

    fun loadCandlesFromAssets(context: Context, fileName: String): List<Candle> {
        val candles = mutableListOf<Candle>()
        
        val dateTimeFormat = SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val dateOnlyFormat = SimpleDateFormat("yyyy.MM.dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        try {
            openCsvFile(context, fileName).use { reader ->
                val header = reader.readLine() // Skip header

                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val tokens = line!!.split(Regex("[ \t]+")) // Split by spaces or tabs

                    if (tokens.size >= 4) {
                        try {
                            val firstToken = tokens[0] // <DATE>

                            val nextToken = tokens[1]

                            val timestamp: Long
                            val open: Float

                            if (nextToken.contains(":")) {
                                timestamp = dateTimeFormat.parse("$firstToken $nextToken")?.time ?: 0L
                                open = tokens[2].toFloat()
                            } else {
                                timestamp = dateOnlyFormat.parse(firstToken)?.time ?: 0L
                                open = nextToken.toFloat()
                            }

                            candles.add(
                                Candle(
                                    timestamp = timestamp,
                                    open = open,
                                    high = tokens[if (nextToken.contains(":")) 3 else 2].toFloat(),
                                    low = tokens[if (nextToken.contains(":")) 4 else 3].toFloat(),
                                    close = tokens[if (nextToken.contains(":")) 5 else 4].toFloat(),
                                    volume = tokens[if (nextToken.contains(":")) 6 else 5].toFloat()
                                )
                            )
                        } catch (e: Exception) {
                        }
                    }
                }
            }
        } catch (e: Exception) {
        }
        return candles
    }

    fun loadTicksFromAssets(context: Context, fileName: String): List<Tick> {
        val ticks = mutableListOf<Tick>()
        
        val dateTimeFormat = SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val dateTimeFormatWithMillis = SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val dateOnlyFormat = SimpleDateFormat("yyyy.MM.dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        try {
            openCsvFile(context, fileName).use { reader ->
                val header = reader.readLine() // Skip header

                var line: String?
                var lineCount = 0
                var validLineCount = 0
                while (reader.readLine().also { line = it } != null) {
                     lineCount++
                     val tokens = line!!.split("\t") // Tab separator - preserves empty columns

                     if (tokens.size >= 3) {
                         try {
                             val dateToken = tokens[0] // <DATE>
                             val timeToken = tokens[1] // <TIME>
                             val bidToken = tokens[2] // <BID>

                             val askToken = if (tokens.size > 3) tokens[3] else ""
                             val lastToken = if (tokens.size > 4) tokens[4] else ""
                             val volumeToken = if (tokens.size > 5) tokens[5] else ""
                             val flagsToken = if (tokens.size > 6) tokens[6] else ""

                            val timestamp: Long
                            val price: Float
                            val volume: Float

                            if (timeToken.contains(".")) {
                                timestamp = dateTimeFormatWithMillis.parse("$dateToken $timeToken")?.time ?: 0L
                            } else {
                                timestamp = dateTimeFormat.parse("$dateToken $timeToken")?.time ?: 0L
                            }

                             price = if (lastToken.isNotEmpty() && lastToken != "0") {
                                 lastToken.toFloat()
                             } else if (bidToken.isNotEmpty() && bidToken != "0") {
                                 bidToken.toFloat()
                             } else {
                                 0f
                             }

                             volume = if (volumeToken.isNotEmpty()) {
                                 volumeToken.toFloat().coerceAtLeast(0f)
                             } else if (flagsToken.isNotEmpty()) {
                                 flagsToken.toFloat().coerceAtLeast(0f)
                             } else {
                                 0f
                             }

                            if (timestamp > 0L && price > 0f && !price.isNaN() && !price.isInfinite()) {
                                ticks.add(Tick(timestamp, price, volume))
                                validLineCount++
                            }
                        } catch (e: Exception) {
                        }
                    }
                }
            }
        } catch (e: Exception) {
        }
        return ticks
    }

    /**
     * ⚡ Lecture RAPIDE des ticks (assets OU fichier téléchargé dans filesDir).
     * - Utilise openCsvFile: cherche d'abord le fichier téléchargé (FileDownloader), sinon les assets, gère GZIP.
     * - Utilise parseTimestampFast au lieu de SimpleDateFormat (~10-100x plus rapide).
     * - Gros buffer de lecture (256 KB) et parsing sans regex.
     *
     * @param fileName  nom du fichier (ex: "xauusd_ticks_2024.01.15.csv" ou "xauusd_1m.csv")
     * @param maxTicks  limite optionnelle du nombre de ticks à charger (0 = illimité)
     */
    fun loadTicksFast(context: Context, fileName: String, maxTicks: Int = 0): List<Tick> {
        val ticks = ArrayList<Tick>(if (maxTicks > 0) maxTicks else 100000)

        try {
            openCsvFile(context, fileName).use { reader ->
                reader.readLine() // Skip header

                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    if (maxTicks > 0 && ticks.size >= maxTicks) break

                    val tokens = line!!.split("\t")
                    if (tokens.size < 3) continue

                    val timestamp = parseTimestampFast(tokens[0], tokens[1])
                    if (timestamp <= 0L) continue

                    val bidToken = tokens[2]
                    val lastToken = if (tokens.size > 4) tokens[4] else ""
                    val volumeToken = if (tokens.size > 5) tokens[5] else ""
                    val flagsToken = if (tokens.size > 6) tokens[6] else ""

                    val price = if (lastToken.isNotEmpty() && lastToken != "0") {
                        lastToken.toFloatOrNull() ?: 0f
                    } else if (bidToken.isNotEmpty() && bidToken != "0") {
                        bidToken.toFloatOrNull() ?: 0f
                    } else {
                        0f
                    }
                    if (price <= 0f || price.isNaN() || price.isInfinite()) continue

                    val volume = if (volumeToken.isNotEmpty()) {
                        (volumeToken.toFloatOrNull() ?: 0f).coerceAtLeast(0f)
                    } else if (flagsToken.isNotEmpty()) {
                        (flagsToken.toFloatOrNull() ?: 0f).coerceAtLeast(0f)
                    } else {
                        0f
                    }

                    ticks.add(Tick(timestamp, price, volume))
                }
            }
        } catch (e: Exception) {
        }

        return ticks
    }

    /**
     * ⚡ Lecture RAPIDE de plusieurs fichiers de ticks journaliers (assets ou téléchargés),
     * du plus récent au plus ancien, retournés triés par timestamp croissant.
     *
     * @param daysToLoad nombre de fichiers journaliers à lire (les plus récents d'abord)
     */
    fun loadTicksFastMultiDay(context: Context, daysToLoad: Int = 1, maxTicks: Int = 0): List<Tick> {
        val allTicks = ArrayList<Tick>()
        val dayFiles = loadAllAvailableDayFiles(context).take(daysToLoad)

        for (dayFile in dayFiles) {
            val remaining = if (maxTicks > 0) maxTicks - allTicks.size else 0
            if (maxTicks > 0 && remaining <= 0) break
            allTicks.addAll(loadTicksFast(context, dayFile, remaining))
        }

        allTicks.sortBy { it.timestamp }
        return allTicks
    }

    fun loadTicksAndAggregateToCandles(context: Context, fileName: String, timeframe: Timeframe): List<Candle> {
        val candles = mutableListOf<Candle>()
        
        val dateTimeFormat = SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val dateTimeFormatWithMillis = SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        val intervalMs = timeframe.minutes * 60 * 1000L
        val maxLinesToProcess = 500000 // Limit for performance: ~3 days of high-frequency data

        try {
            openCsvFile(context, fileName).use { reader ->
                val header = reader.readLine() // Skip header

                var line: String?
                var lineCount = 0
                var validLineCount = 0

                var currentGroupTimestamp: Long? = null
                var groupOpen: Float? = null
                var groupHigh: Float? = null
                var groupLow: Float? = null
                var groupClose: Float? = null
                var groupVolume: Float = 0f

                while (reader.readLine().also { line = it } != null && lineCount < maxLinesToProcess) {
                     lineCount++
                     val tokens = line!!.split("\t") // Tab separator - preserves empty columns

                     if (tokens.size >= 3) {
                         try {
                             val dateToken = tokens[0]
                             val timeToken = tokens[1]
                             val bidToken = tokens[2]

                             val askToken = if (tokens.size > 3) tokens[3] else ""
                             val lastToken = if (tokens.size > 4) tokens[4] else ""
                             val volumeToken = if (tokens.size > 5) tokens[5] else ""
                             val flagsToken = if (tokens.size > 6) tokens[6] else ""

                            val timestamp: Long
                            if (timeToken.contains(".")) {
                                timestamp = dateTimeFormatWithMillis.parse("$dateToken $timeToken")?.time ?: 0L
                            } else {
                                timestamp = dateTimeFormat.parse("$dateToken $timeToken")?.time ?: 0L
                            }

                            if (timestamp <= 0L) continue

                             val price = if (lastToken.isNotEmpty() && lastToken != "0") {
                                 lastToken.toFloat()
                             } else if (bidToken.isNotEmpty() && bidToken != "0") {
                                 bidToken.toFloat()
                             } else {
                                 0f
                             }

                             if (price <= 0f || price.isNaN() || price.isInfinite()) continue

                             val volume = if (volumeToken.isNotEmpty()) {
                                 volumeToken.toFloat().coerceAtLeast(0f)
                             } else if (flagsToken.isNotEmpty()) {
                                 flagsToken.toFloat().coerceAtLeast(0f)
                             } else {
                                 0f
                             }

                            validLineCount++

                            val groupTimestamp = (timestamp / intervalMs) * intervalMs

                            if (currentGroupTimestamp == null) {
                                currentGroupTimestamp = groupTimestamp
                                groupOpen = price
                                groupHigh = price
                                groupLow = price
                                groupClose = price
                                groupVolume = volume
                            } else if (groupTimestamp == currentGroupTimestamp) {
                                groupHigh = maxOf(groupHigh!!, price)
                                groupLow = minOf(groupLow!!, price)
                                groupClose = price
                                groupVolume += volume
                            } else {
                                if (groupOpen!! > 0f && groupHigh!! > 0f && groupLow!! > 0f) {
                                    candles.add(Candle(
                                        currentGroupTimestamp!!,
                                        groupOpen,
                                        groupHigh,
                                        groupLow,
                                        groupClose!!,
                                        groupVolume
                                    ))
                                }

                                currentGroupTimestamp = groupTimestamp
                                groupOpen = price
                                groupHigh = price
                                groupLow = price
                                groupClose = price
                                groupVolume = volume
                            }
                        } catch (e: Exception) {
                        }
                    }
                }

                if (currentGroupTimestamp != null && groupOpen != null && groupOpen!! > 0f && groupHigh != null && groupHigh!! > 0f && groupLow != null && groupLow!! > 0f) {
                    candles.add(Candle(
                        currentGroupTimestamp,
                        groupOpen,
                        groupHigh,
                        groupLow,
                        groupClose!!,
                        groupVolume
                    ))
                }

            }
        } catch (e: Exception) {
        }
        return candles
    }

    fun loadLastTicksAndAggregateToCandles(context: Context, fileName: String, timeframe: Timeframe, maxLines: Int = 100000): List<Candle> {
        val candles = mutableListOf<Candle>()
        
        val dateTimeFormat = SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val dateTimeFormatWithMillis = SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        val intervalMs = timeframe.minutes * 60 * 1000L

        try {
            openCsvFile(context, fileName).use { reader ->
                val header = reader.readLine() // Skip header

                val lineBuffer = ArrayDeque<String>(maxLines)
                var lineCount = 0

                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    lineCount++
                    if (lineBuffer.size >= maxLines) {
                        lineBuffer.removeFirst()
                    }
                    lineBuffer.addLast(line!!)
                }

                var currentGroupTimestamp: Long? = null
                var groupOpen: Float? = null
                var groupHigh: Float? = null
                var groupLow: Float? = null
                var groupClose: Float? = null
                var groupVolume: Float = 0f
                var validLineCount = 0

                for (line in lineBuffer) {
                     val tokens = line.split("\t") // Tab separator - preserves empty columns

                     if (tokens.size >= 3) {
                         try {
                             val dateToken = tokens[0]
                             val timeToken = tokens[1]
                             val bidToken = tokens[2]

                             val askToken = if (tokens.size > 3) tokens[3] else ""
                             val lastToken = if (tokens.size > 4) tokens[4] else ""
                             val volumeToken = if (tokens.size > 5) tokens[5] else ""
                             val flagsToken = if (tokens.size > 6) tokens[6] else ""

                            val timestamp: Long
                            if (timeToken.contains(".")) {
                                timestamp = dateTimeFormatWithMillis.parse("$dateToken $timeToken")?.time ?: 0L
                            } else {
                                timestamp = dateTimeFormat.parse("$dateToken $timeToken")?.time ?: 0L
                            }

                            if (timestamp <= 0L) continue

                             val price = if (lastToken.isNotEmpty() && lastToken != "0") {
                                 lastToken.toFloat()
                             } else if (bidToken.isNotEmpty() && bidToken != "0") {
                                 bidToken.toFloat()
                             } else {
                                 0f
                             }

                             if (price <= 0f || price.isNaN() || price.isInfinite()) {
                                  continue
                             }


                             val volume = if (volumeToken.isNotEmpty()) {
                                 volumeToken.toFloat().coerceAtLeast(0f)
                             } else if (flagsToken.isNotEmpty()) {
                                 flagsToken.toFloat().coerceAtLeast(0f)
                             } else {
                                 0f
                             }

                            validLineCount++

                            val groupTimestamp = (timestamp / intervalMs) * intervalMs

                             if (currentGroupTimestamp == null) {
                                 currentGroupTimestamp = groupTimestamp
                                 groupOpen = price
                                 groupHigh = price
                                 groupLow = price
                                 groupClose = price
                                 groupVolume = volume
                             } else if (groupTimestamp == currentGroupTimestamp) {
                                 groupHigh = maxOf(groupHigh!!, price)
                                 groupLow = minOf(groupLow!!, price)
                                 groupClose = price
                                 groupVolume += volume
                             } else {
                                 if (groupOpen!! > 0f && groupHigh!! > 0f && groupLow!! > 0f) {
                                     candles.add(Candle(
                                         currentGroupTimestamp!!,
                                         groupOpen,
                                         groupHigh,
                                         groupLow,
                                         groupClose!!,
                                         groupVolume
                                     ))
                                 }

                                 currentGroupTimestamp = groupTimestamp
                                 groupOpen = price
                                 groupHigh = price
                                 groupLow = price
                                 groupClose = price
                                 groupVolume = volume
                             }
                        } catch (e: Exception) {
                        }
                    }
                }

                if (currentGroupTimestamp != null && groupOpen != null && groupOpen!! > 0f && groupHigh != null && groupHigh!! > 0f && groupLow != null && groupLow!! > 0f) {
                    candles.add(Candle(
                        currentGroupTimestamp,
                        groupOpen,
                        groupHigh,
                        groupLow,
                        groupClose!!,
                        groupVolume
                    ))
                }

            }
        } catch (e: Exception) {
        }
        return candles
    }

    /**
     * ⚡ VERSION ULTRA-OPTIMISÉE sans buffer massif
     * Stratégie: Agréger directement les ticks en bougies sans stocker les lignes
     * Cela minimise l'usage mémoire et maximise la performance
     */
    fun loadLastTicksAggregateOptimized(
        context: Context,
        fileName: String,
        timeframe: Timeframe,
        maxLines: Int = 500000
    ): List<Candle> {
        val candles = mutableListOf<Candle>()

        val dateTimeFormat = SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val dateTimeFormatWithMillis = SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        val intervalMs = timeframe.minutes * 60 * 1000L
        var startTime = System.currentTimeMillis()

        try {
            val bestFileName = findBestCsvFile(context, fileName)

            openCsvFile(context, bestFileName).use { reader ->
                val header = reader.readLine() // Skip header

                val lineBuffer = mutableListOf<String>()
                val BUFFER_SIZE = 1000  // Petit buffer raisonnable
                var lineCount = 0
                var validLineCount = 0
                var line: String?

                var currentGroupTimestamp: Long? = null
                var groupOpen: Float? = null
                var groupHigh: Float? = null
                var groupLow: Float? = null
                var groupClose: Float? = null
                var groupVolume: Float = 0f

                while (reader.readLine().also { line = it } != null && lineCount < maxLines) {
                    lineCount++

                    if (lineCount % 200000 == 0) {
                        val elapsed = System.currentTimeMillis() - startTime
                        val secElapsed = elapsed / 1000.0
                    }

                    lineBuffer.add(line!!)

                    if (lineBuffer.size >= BUFFER_SIZE) {
                        for (bLine in lineBuffer) {
                            val tokens = bLine.split("\t")
                            if (tokens.size >= 3) {
                                try {
                                    val dateToken = tokens[0]
                                    val timeToken = tokens[1]
                                    val bidToken = tokens[2]
                                    val lastToken = if (tokens.size > 4) tokens[4] else ""
                                    val volumeToken = if (tokens.size > 5) tokens[5] else ""
                                    val flagsToken = if (tokens.size > 6) tokens[6] else ""

                                    val timestamp: Long = if (timeToken.contains(".")) {
                                        dateTimeFormatWithMillis.parse("$dateToken $timeToken")?.time ?: 0L
                                    } else {
                                        dateTimeFormat.parse("$dateToken $timeToken")?.time ?: 0L
                                    }

                                    if (timestamp <= 0L) continue

                                    val price = if (lastToken.isNotEmpty() && lastToken != "0") {
                                        lastToken.toFloat()
                                    } else if (bidToken.isNotEmpty() && bidToken != "0") {
                                        bidToken.toFloat()
                                    } else {
                                        0f
                                    }

                                    if (price <= 0f || price.isNaN() || price.isInfinite()) continue

                                    val volume = if (volumeToken.isNotEmpty()) {
                                        volumeToken.toFloat().coerceAtLeast(0f)
                                    } else if (flagsToken.isNotEmpty()) {
                                        flagsToken.toFloat().coerceAtLeast(0f)
                                    } else {
                                        0f
                                    }

                                    validLineCount++
                                    val groupTimestamp = (timestamp / intervalMs) * intervalMs

                                    if (currentGroupTimestamp == null) {
                                        currentGroupTimestamp = groupTimestamp
                                        groupOpen = price
                                        groupHigh = price
                                        groupLow = price
                                        groupClose = price
                                        groupVolume = volume
                                    } else if (groupTimestamp == currentGroupTimestamp) {
                                        groupHigh = maxOf(groupHigh!!, price)
                                        groupLow = minOf(groupLow!!, price)
                                        groupClose = price
                                        groupVolume += volume
                                    } else {
                                        if (groupOpen!! > 0f && groupHigh!! > 0f && groupLow!! > 0f) {
                                            candles.add(Candle(
                                                currentGroupTimestamp!!,
                                                groupOpen,
                                                groupHigh,
                                                groupLow,
                                                groupClose!!,
                                                groupVolume
                                            ))
                                        }

                                        currentGroupTimestamp = groupTimestamp
                                        groupOpen = price
                                        groupHigh = price
                                        groupLow = price
                                        groupClose = price
                                        groupVolume = volume
                                    }
                                } catch (e: Exception) {
                                }
                            }
                        }
                        lineBuffer.clear()
                    }
                }

                for (bLine in lineBuffer) {
                    val tokens = bLine.split("\t")
                    if (tokens.size >= 3) {
                        try {
                            val dateToken = tokens[0]
                            val timeToken = tokens[1]
                            val bidToken = tokens[2]
                            val lastToken = if (tokens.size > 4) tokens[4] else ""
                            val volumeToken = if (tokens.size > 5) tokens[5] else ""
                            val flagsToken = if (tokens.size > 6) tokens[6] else ""

                            val timestamp: Long = if (timeToken.contains(".")) {
                                dateTimeFormatWithMillis.parse("$dateToken $timeToken")?.time ?: 0L
                            } else {
                                dateTimeFormat.parse("$dateToken $timeToken")?.time ?: 0L
                            }

                            if (timestamp <= 0L) continue

                            val price = if (lastToken.isNotEmpty() && lastToken != "0") {
                                lastToken.toFloat()
                            } else if (bidToken.isNotEmpty() && bidToken != "0") {
                                bidToken.toFloat()
                            } else {
                                0f
                            }

                            if (price <= 0f || price.isNaN() || price.isInfinite()) continue

                            val volume = if (volumeToken.isNotEmpty()) {
                                volumeToken.toFloat().coerceAtLeast(0f)
                            } else if (flagsToken.isNotEmpty()) {
                                flagsToken.toFloat().coerceAtLeast(0f)
                            } else {
                                0f
                            }

                            validLineCount++
                            val groupTimestamp = (timestamp / intervalMs) * intervalMs

                            if (currentGroupTimestamp == null) {
                                currentGroupTimestamp = groupTimestamp
                                groupOpen = price
                                groupHigh = price
                                groupLow = price
                                groupClose = price
                                groupVolume = volume
                            } else if (groupTimestamp == currentGroupTimestamp) {
                                groupHigh = maxOf(groupHigh!!, price)
                                groupLow = minOf(groupLow!!, price)
                                groupClose = price
                                groupVolume += volume
                            } else {
                                if (groupOpen!! > 0f && groupHigh!! > 0f && groupLow!! > 0f) {
                                    candles.add(Candle(
                                        currentGroupTimestamp!!,
                                        groupOpen,
                                        groupHigh,
                                        groupLow,
                                        groupClose!!,
                                        groupVolume
                                    ))
                                }

                                currentGroupTimestamp = groupTimestamp
                                groupOpen = price
                                groupHigh = price
                                groupLow = price
                                groupClose = price
                                groupVolume = volume
                            }
                        } catch (e: Exception) {
                        }
                    }
                }

                if (currentGroupTimestamp != null && groupOpen != null && groupOpen!! > 0f &&
                    groupHigh != null && groupHigh!! > 0f && groupLow != null && groupLow!! > 0f) {
                    candles.add(Candle(
                        currentGroupTimestamp,
                        groupOpen,
                        groupHigh,
                        groupLow,
                        groupClose!!,
                        groupVolume
                    ))
                }

                val processTime = System.currentTimeMillis() - startTime
            }
        } catch (e: Exception) {
        }
        return candles
     }

     /**
     * Cache global pour éviter de recharger les mêmes données
     */
    private val candleCache = mutableMapOf<String, List<Candle>>()

    /**
     * Cache par période - intelligent pour 10+ ans de données
     * Clé: "fileName_startDate_endDate", Valeur: List<Candle>
     */
    private val periodCache = mutableMapOf<String, List<Candle>>()

    /**
     * Charge les données avec cache automatique - optimal pour gros volumes (10+ ans)
     * Stratégie: Ne charge que la fenêtre visible + buffers adjacents
     */
    fun loadTicksWithSmartCache(
        context: Context,
        fileName: String,
        timeframe: Timeframe,
        maxLinesWindow: Int = 500000
    ): List<Candle> {
        val cacheKey = "${fileName}_${timeframe.displayName}"

        candleCache[cacheKey]?.let {
            return it
        }

        val candles = loadLastTicksAndAggregateToCandles(context, fileName, timeframe, maxLinesWindow)
        candleCache[cacheKey] = candles
        return candles
    }

    /**
     * 🎯 SYSTÈME DE PAGINATION INTELLIGENT - pagination par défilement
     * Charge les données par PÉRIODES seulement
     * - Ne charge que ce qui est visible
     * - Précharge les périodes adjacentes
     * - Économise 90% de mémoire!
     */
    data class CandleRange(
        val startDate: Long,
        val endDate: Long,
        val candles: List<Candle>
    )

    private val rangeCache = mutableMapOf<String, CandleRange>()

    /**
     * Charge une période spécifique de données (ex: 1 semaine, 1 mois)
     * Stratégie: Lire depuis le CSV et extraire SEULEMENT la période demandée
     */
    fun loadCandlesByDateRange(
        context: Context,
        fileName: String,
        timeframe: Timeframe,
        startDate: Long,
        endDate: Long
    ): CandleRange {
        val cacheKey = "${fileName}_${timeframe.displayName}_${startDate}_${endDate}"

        rangeCache[cacheKey]?.let {
            return it
        }

        val candles = mutableListOf<Candle>()

        val dateTimeFormat = SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val dateTimeFormatWithMillis = SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        val intervalMs = timeframe.minutes * 60 * 1000L
        var currentGroupTimestamp: Long? = null
        var groupOpen: Float? = null
        var groupHigh: Float? = null
        var groupLow: Float? = null
        var groupClose: Float? = null
        var groupVolume: Float = 0f

        try {
            openCsvFile(context, fileName).use { reader ->
                reader.readLine() // Skip header

                var line: String?
                var lineCount = 0
                var inRangeCount = 0

                while (reader.readLine().also { line = it } != null) {
                    lineCount++

                    if (lineCount % 100000 == 0) {
                    }

                    val tokens = line!!.split("\t")
                    if (tokens.size < 3) continue

                    try {
                        val dateToken = tokens[0]
                        val timeToken = tokens[1]
                        val bidToken = tokens[2]
                        val lastToken = if (tokens.size > 4) tokens[4] else ""
                        val volumeToken = if (tokens.size > 5) tokens[5] else ""
                        val flagsToken = if (tokens.size > 6) tokens[6] else ""

                        val timestamp: Long = if (timeToken.contains(".")) {
                            dateTimeFormatWithMillis.parse("$dateToken $timeToken")?.time ?: 0L
                        } else {
                            dateTimeFormat.parse("$dateToken $timeToken")?.time ?: 0L
                        }

                        if (timestamp <= 0L) continue

                        if (timestamp < startDate) continue

                        if (timestamp > endDate) break

                        val price = if (lastToken.isNotEmpty() && lastToken != "0") {
                            lastToken.toFloat()
                        } else if (bidToken.isNotEmpty() && bidToken != "0") {
                            bidToken.toFloat()
                        } else {
                            0f
                        }

                        if (price <= 0f || price.isNaN() || price.isInfinite()) continue

                        val volume = if (volumeToken.isNotEmpty()) {
                            volumeToken.toFloat().coerceAtLeast(0f)
                        } else if (flagsToken.isNotEmpty()) {
                            flagsToken.toFloat().coerceAtLeast(0f)
                        } else {
                            0f
                        }

                        inRangeCount++
                        val groupTimestamp = (timestamp / intervalMs) * intervalMs

                        if (currentGroupTimestamp == null) {
                            currentGroupTimestamp = groupTimestamp
                            groupOpen = price
                            groupHigh = price
                            groupLow = price
                            groupClose = price
                            groupVolume = volume
                        } else if (groupTimestamp == currentGroupTimestamp) {
                            groupHigh = maxOf(groupHigh!!, price)
                            groupLow = minOf(groupLow!!, price)
                            groupClose = price
                            groupVolume += volume
                        } else {
                            if (groupOpen!! > 0f && groupHigh!! > 0f && groupLow!! > 0f) {
                                candles.add(Candle(
                                    currentGroupTimestamp!!,
                                    groupOpen,
                                    groupHigh,
                                    groupLow,
                                    groupClose!!,
                                    groupVolume
                                ))
                            }

                            currentGroupTimestamp = groupTimestamp
                            groupOpen = price
                            groupHigh = price
                            groupLow = price
                            groupClose = price
                            groupVolume = volume
                        }
                    } catch (e: Exception) {
                    }
                }

                if (currentGroupTimestamp != null && groupOpen != null && groupOpen!! > 0f &&
                    groupHigh != null && groupHigh!! > 0f && groupLow != null && groupLow!! > 0f) {
                    candles.add(Candle(
                        currentGroupTimestamp,
                        groupOpen,
                        groupHigh,
                        groupLow,
                        groupClose!!,
                        groupVolume
                    ))
                }

            }
        } catch (e: Exception) {
        }

        val range = CandleRange(startDate, endDate, candles)
        rangeCache[cacheKey] = range
        return range
    }

    /**
     * Charge les dernières N bougies + buffer pour scroll smooth
     * Parfait pour afficher les données "partie par partie"
     */
    fun loadLastCandlesWithBuffer(
        context: Context,
        fileName: String,
        timeframe: Timeframe,
        targetCount: Int = 100,  // Nombre de bougies visibles
        bufferSize: Int = 50     // Bougies de buffer avant/après
    ): List<Candle> {
        val totalNeeded = targetCount + (bufferSize * 2)
        return loadLastTicksAndAggregateToCandles(
            context,
            fileName,
            timeframe,
            maxLines = totalNeeded * 100  // Estimer les lignes nécessaires
        )
    }

    /**
     * Précharge les périodes adjacentes pour scroll fluide
     */
    fun preloadAdjacentRanges(
        context: Context,
        fileName: String,
        timeframe: Timeframe,
        currentRange: CandleRange
    ) {
        val periodMs = 7 * 24 * 60 * 60 * 1000L  // 1 semaine

        Thread {
            try {
                loadCandlesByDateRange(
                    context,
                    fileName,
                    timeframe,
                    currentRange.startDate - periodMs,
                    currentRange.startDate
                )
                loadCandlesByDateRange(
                    context,
                    fileName,
                    timeframe,
                    currentRange.endDate,
                    currentRange.endDate + periodMs
                )
            } catch (e: Exception) {
            }
        }.start()
    }

    /**
     * Nettoie le cache des ranges pour libérer mémoire
     */
    fun clearRangeCache() {
        rangeCache.clear()
    }

    /**
     * Retourne les stats du cache
     */
    fun getCacheStats(): String {
        val totalRanges = rangeCache.size
        val totalCandles = rangeCache.values.sumOf { it.candles.size }
        return "Cache: $totalRanges ranges, $totalCandles candles"
    }

    /**
     * Nettoie le cache pour libérer de la mémoire
     */
    fun clearCache() {
        candleCache.clear()
        periodCache.clear()
    }

    /**
     * ⚡⚡⚡ ULTRA-RAPIDE: Charge 1 tick/minute comme bougies M1 directement!
     * Puis on agrège M1 → H1/4H/1D (100x plus rapide qu'agrégation depuis ticks!)
     *
     * Stratégie:
     * 1. Charger ticks (1/minute) et créer des bougies M1 directement
     * 2. Retourner M1 bougies
     * 3. Appeler CandleUtil.aggregate() pour convertir M1 → H1/4H/1D
     */
    fun loadTicksAsMinuteCandles(
        context: Context,
        fileName: String,
        maxLines: Int = 500000
    ): List<Candle> {
        val candles = mutableListOf<Candle>()

        val dateTimeFormat = SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val dateTimeFormatWithMillis = SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        var startTime = System.currentTimeMillis()
        var lineCount = 0
        var validLineCount = 0
        
        // Variables pour agréger les ticks en bougies M1
        val intervalMs = 60 * 1000L  // 1 minute
        var currentMinuteTimestamp: Long? = null
        var minuteOpen: Float? = null
        var minuteHigh: Float? = null
        var minuteLow: Float? = null
        var minuteClose: Float? = null
        var minuteVolume = 0f

        try {
            openCsvFile(context, fileName).use { reader ->
                reader.readLine() // Skip header

                var line: String?
                while (reader.readLine().also { line = it } != null && lineCount < maxLines) {
                    lineCount++

                    if (lineCount % 200000 == 0) {
                        val elapsed = System.currentTimeMillis() - startTime
                        val secElapsed = elapsed / 1000.0
                    }

                    val tokens = line!!.split("\t")
                    if (tokens.size >= 3) {
                        try {
                            val dateToken = tokens[0]
                            val timeToken = tokens[1]
                            val bidToken = tokens[2]
                            val lastToken = if (tokens.size > 4) tokens[4] else ""
                            val volumeToken = if (tokens.size > 5) tokens[5] else ""
                            val flagsToken = if (tokens.size > 6) tokens[6] else ""

                            val timestamp: Long = if (timeToken.contains(".")) {
                                dateTimeFormatWithMillis.parse("$dateToken $timeToken")?.time ?: 0L
                            } else {
                                dateTimeFormat.parse("$dateToken $timeToken")?.time ?: 0L
                            }

                            if (timestamp <= 0L) continue

                            val price = if (lastToken.isNotEmpty() && lastToken != "0") {
                                lastToken.toFloat()
                            } else if (bidToken.isNotEmpty() && bidToken != "0") {
                                bidToken.toFloat()
                            } else {
                                0f
                            }

                            if (price <= 0f || price.isNaN() || price.isInfinite()) continue

                            val volume = if (volumeToken.isNotEmpty()) {
                                volumeToken.toFloat().coerceAtLeast(0f)
                            } else if (flagsToken.isNotEmpty()) {
                                flagsToken.toFloat().coerceAtLeast(0f)
                            } else {
                                0f
                            }

                            validLineCount++
                            
                            // Calculer le timestamp de début de minute
                            val minuteTimestamp = (timestamp / intervalMs) * intervalMs
                            
                            if (currentMinuteTimestamp == null || minuteTimestamp != currentMinuteTimestamp) {
                                // Nouvelle minute: enregistrer la bougie précédente
                                if (currentMinuteTimestamp != null && minuteOpen != null) {
                                    candles.add(Candle(
                                        currentMinuteTimestamp,
                                        minuteOpen,
                                        minuteHigh ?: minuteOpen,
                                        minuteLow ?: minuteOpen,
                                        minuteClose ?: minuteOpen,
                                        minuteVolume
                                    ))
                                }
                                
                                // Initialiser la nouvelle minute
                                currentMinuteTimestamp = minuteTimestamp
                                minuteOpen = price
                                minuteHigh = price
                                minuteLow = price
                                minuteClose = price
                                minuteVolume = volume
                            } else {
                                // Même minute: mettre à jour High/Low/Close
                                minuteHigh = maxOf(minuteHigh ?: price, price)
                                minuteLow = minOf(minuteLow ?: price, price)
                                minuteClose = price
                                minuteVolume += volume
                            }
                        } catch (e: Exception) {
                        }
                    }
                }
                
                // Ajouter la dernière bougie
                if (currentMinuteTimestamp != null && minuteOpen != null) {
                    candles.add(Candle(
                        currentMinuteTimestamp,
                        minuteOpen,
                        minuteHigh ?: minuteOpen,
                        minuteLow ?: minuteOpen,
                        minuteClose ?: minuteOpen,
                        minuteVolume
                    ))
                }
            }
        } catch (e: Exception) {
        }

        val processTime = System.currentTimeMillis() - startTime

        return candles
    }

    /**
     * ⚡⚡⚡ HYPER-OPTIMALISÉ pour fichiers 1-tick-par-minute
     * 1. Charge les ticks comme bougies M1 (super rapide)
     * 2. Agrège M1 → timeframe demandé
     *
     * Pour H1: 60 M1 → 1 H1 (simple!)
     * Pour 4H: 240 M1 → 1 4H (simple!)
     * Pour 1D: 1440 M1 → 1 1D (simple!)
     *
     * Gain: 100-1000x plus rapide! ⚡⚡⚡
     */
    fun loadAndAggregateMinuteCandlesOptimized(
        context: Context,
        fileName: String,
        timeframe: Timeframe,
        maxLines: Int = 500000
    ): List<Candle> {
        if (timeframe == Timeframe.M1) {
            return loadTicksAsMinuteCandles(context, fileName, maxLines)
        }


        val minuteCandles = loadTicksAsMinuteCandles(context, fileName, maxLines)
        if (minuteCandles.isEmpty()) {
            return emptyList()
        }


        val startTime = System.currentTimeMillis()
        val aggregated = CandleUtil.aggregate(minuteCandles, timeframe)
        val processTime = System.currentTimeMillis() - startTime


        return aggregated
    }

    fun loadTicksAsMinuteCandlesInRange(
        context: Context,
        fileName: String,
        fromTimestamp: Long,
        toTimestamp: Long = Long.MAX_VALUE,
        maxLines: Int = 200000
    ): List<Candle> {
        val candles = mutableListOf<Candle>()
        val intervalMs = 60 * 1000L
        var currentMinuteTimestamp: Long? = null
        var minuteOpen: Float? = null
        var minuteHigh: Float? = null
        var minuteLow: Float? = null
        var minuteClose: Float? = null
        var minuteVolume = 0f
        var lineCount = 0
        try {
            openCsvFile(context, fileName).use { reader ->
                reader.readLine()
                var line: String?
                while (reader.readLine().also { line = it } != null && lineCount < maxLines) {
                    lineCount++
                    val tokens = line!!.split("\t")
                    if (tokens.size < 3) continue
                    val dateToken = tokens[0]
                    val timeToken = tokens[1]
                    val timestamp = try { parseTimestampFast(dateToken, timeToken) } catch (e: Exception) { continue }
                    if (timestamp <= 0L) continue
                    if (timestamp < fromTimestamp) continue
                    if (timestamp >= toTimestamp) break
                    val bidToken = tokens[2]
                    val lastToken = if (tokens.size > 4) tokens[4] else ""
                    val volumeToken = if (tokens.size > 5) tokens[5] else ""
                    val flagsToken = if (tokens.size > 6) tokens[6] else ""
                    val price = try {
                        if (lastToken.isNotEmpty() && lastToken != "0") lastToken.toFloat()
                        else if (bidToken.isNotEmpty() && bidToken != "0") bidToken.toFloat()
                        else 0f
                    } catch (e: Exception) { 0f }
                    if (price <= 0f || price.isNaN() || price.isInfinite()) continue
                    val volume = try {
                        if (volumeToken.isNotEmpty()) volumeToken.toFloat().coerceAtLeast(0f)
                        else if (flagsToken.isNotEmpty()) flagsToken.toFloat().coerceAtLeast(0f)
                        else 0f
                    } catch (e: Exception) { 0f }
                    val minuteTimestamp = (timestamp / intervalMs) * intervalMs
                    if (currentMinuteTimestamp == null || minuteTimestamp != currentMinuteTimestamp) {
                        if (currentMinuteTimestamp != null && minuteOpen != null) {
                            candles.add(Candle(currentMinuteTimestamp, minuteOpen, minuteHigh ?: minuteOpen, minuteLow ?: minuteOpen, minuteClose ?: minuteOpen, minuteVolume))
                        }
                        currentMinuteTimestamp = minuteTimestamp
                        minuteOpen = price; minuteHigh = price; minuteLow = price; minuteClose = price; minuteVolume = volume
                    } else {
                        minuteHigh = maxOf(minuteHigh ?: price, price)
                        minuteLow = minOf(minuteLow ?: price, price)
                        minuteClose = price
                        minuteVolume += volume
                    }
                }
                if (currentMinuteTimestamp != null && minuteOpen != null) {
                    candles.add(Candle(currentMinuteTimestamp, minuteOpen, minuteHigh ?: minuteOpen, minuteLow ?: minuteOpen, minuteClose ?: minuteOpen, minuteVolume))
                }
            }
        } catch (e: Exception) { }
        return candles
    }

    fun loadCandlesInRange(
        context: Context,
        fileName: String,
        fromTimestamp: Long,
        toTimestamp: Long = Long.MAX_VALUE
    ): List<Candle> {
        val candles = ArrayList<Candle>(200000)
        try {
            openCsvFile(context, fileName, bufferSize = 1 shl 18).use { reader ->
                var isCsvFormat: Boolean? = null
                var line = reader.readLine()
                if (line != null && line.any { it.isLetter() }) {
                    line = reader.readLine() // saute le header
                }
                while (line != null) {
                    val raw = line
                    if (isCsvFormat == null) {
                        isCsvFormat = raw.contains(',') && raw.split(',').size >= 7
                    }
                    val tokens = if (isCsvFormat) splitCsv(raw) else splitFast(raw)
                    val minTokens = if (isCsvFormat) 7 else 4
                    if (tokens.size < minTokens) { line = reader.readLine(); continue }
                    try {
                        val timestamp = if (isCsvFormat) {
                            if (tokens[0].contains('.')) {
                                parseTimestampFast(tokens[0], tokens[1])
                            } else {
                                parseTimestampIso(tokens[1])
                            }
                        } else {
                            val hasTime = tokens[1].contains(":")
                            if (hasTime) parseTimestampFast(tokens[0], tokens[1])
                            else parseTimestampFast(tokens[0], "0:0")
                        }
                        if (timestamp <= 0L) { line = reader.readLine(); continue }
                        if (timestamp < fromTimestamp) { line = reader.readLine(); continue }
                        if (timestamp >= toTimestamp) break
                        val oIdx = if (isCsvFormat) 2 else (if (tokens[1].contains(":")) 2 else 1)
                        val hIdx = oIdx + 1
                        val lIdx = oIdx + 2
                        val cIdx = oIdx + 3
                        val vIdx = oIdx + 4
                        val o = tokens[oIdx].toFloat()
                        val h = tokens[hIdx].toFloat()
                        val l = tokens[lIdx].toFloat()
                        val c = tokens[cIdx].toFloat()
                        val v = tokens.getOrNull(vIdx)?.toFloatOrNull() ?: 0f
                        candles.add(Candle(timestamp, o, h, l, c, v))
                    } catch (e: Exception) { }
                    line = reader.readLine()
                }
            }
        } catch (e: Exception) { }
        return candles
    }

    // ============================================================
    // ⚡⚡ CACHE BINAIRE — lecture quasi-instantanée de plages
    // ============================================================
    // Format: enregistrements fixes de 28 octets (timestamp Long + O/H/L/C/V Float),
    // triés par timestamp. Permet une recherche binaire + lecture directe de la
    // plage demandée sans re-parser/décompresser le CSV (indispensable pour 10 ans de données).

    private const val BIN_RECORD_SIZE = 28
    private const val BIN_MAGIC = 0x42544331 // "BTC1"
    private const val BIN_HEADER_SIZE = 8    // magic (Int) + version/réservé (Int)
    private const val BIN_GZ_BASE_URL = "https://github.com/lordboys631-debug/xausd/raw/main/"

    private fun binCacheFile(context: Context, fileName: String): java.io.File {
        val safe = fileName.replace('/', '_')
        return java.io.File(context.filesDir, "$safe.bin")
    }

    /**
     * Télécharge le .bin.gz depuis GitHub (une seule fois) et le décompresse en cache.
     * Améliorations : timeouts HTTP, reprise (Range) si .tmp partiel, sauvegarde d'ETag (.meta), écriture atomique.
     */
    private fun ensureBinaryCache(context: Context, fileName: String): java.io.File? {
        val binFile = binCacheFile(context, fileName)
        if (binFile.exists() && binFile.length() >= BIN_HEADER_SIZE &&
            (binFile.length() - BIN_HEADER_SIZE) % BIN_RECORD_SIZE == 0L) {
            return binFile
        }
        val tmp = java.io.File(binFile.parentFile, "${binFile.name}.tmp")
        val meta = java.io.File(binFile.parentFile, "${binFile.name}.meta")
        val remoteUrl = "$BIN_GZ_BASE_URL${fileName}.bin.gz"
        try {
            val url = java.net.URL(remoteUrl)
            var connection = (url.openConnection() as java.net.HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 30000
                instanceFollowRedirects = true
                requestMethod = "GET"
            }

            // If we have ETag, ask server if unchanged
            if (meta.exists()) {
                try {
                    val saved = meta.readText(Charsets.UTF_8).trim()
                    if (saved.isNotEmpty()) connection.setRequestProperty("If-None-Match", saved)
                } catch (_: Exception) {}
            }

            // Resume when tmp exists
            val tmpOffset = if (tmp.exists()) tmp.length() else 0L
            if (tmpOffset > 0) {
                connection.setRequestProperty("Range", "bytes=$tmpOffset-")
            }

            connection.connect()
            val code = connection.responseCode
            if (code == java.net.HttpURLConnection.HTTP_NOT_MODIFIED) {
                connection.disconnect()
                // remote unchanged; fall through to decompression if tmp exists
                if (!tmp.exists()) return null
            }

            if (code == java.net.HttpURLConnection.HTTP_PARTIAL) {
                // append to tmp
                java.io.RandomAccessFile(tmp, "rw").use { raf ->
                    raf.seek(tmpOffset)
                    connection.inputStream.use { input ->
                        val buf = ByteArray(1 shl 16)
                        var read = input.read(buf)
                        while (read >= 0) {
                            raf.write(buf, 0, read)
                            read = input.read(buf)
                        }
                    }
                }
            } else if (code in 200..299) {
                // full download (overwrite tmp)
                java.io.FileOutputStream(tmp, false).use { out ->
                    connection.inputStream.use { input ->
                        val buf = ByteArray(1 shl 16)
                        var read = input.read(buf)
                        while (read >= 0) {
                            out.write(buf, 0, read)
                            read = input.read(buf)
                        }
                    }
                }
            } else {
                connection.disconnect()
                return null
            }

            // Save ETag if provided
            try {
                val etag = connection.getHeaderField("ETag") ?: connection.getHeaderField("etag")
                if (!etag.isNullOrEmpty()) meta.writeText(etag)
            } catch (_: Exception) {}

            connection.disconnect()

            // Decompress tmp (.bin.gz) into a new file atomically
            val newFileTmp = java.io.File(binFile.parentFile, "${binFile.name}.new")
            java.io.FileOutputStream(newFileTmp).use { out ->
                java.util.zip.GZIPInputStream(java.io.FileInputStream(tmp), 1 shl 16).use { gzIn ->
                    val buf = ByteArray(1 shl 16)
                    var r = gzIn.read(buf)
                    while (r >= 0) {
                        out.write(buf, 0, r)
                        r = gzIn.read(buf)
                    }
                }
            }
            if (newFileTmp.length() < BIN_HEADER_SIZE) { newFileTmp.delete(); tmp.delete(); return null }
            if (binFile.exists()) binFile.delete()
            if (!newFileTmp.renameTo(binFile)) { newFileTmp.delete(); tmp.delete(); return null }
            tmp.delete()
            return binFile
        } catch (e: Exception) {
            try { tmp.delete() } catch (_: Exception) {}
            return null
        }
    }

    /**
     * ⚡⚡ Charge une plage de bougies quasi-instantanément via le cache binaire.
     * 1er appel: conversion CSV→binaire (une fois). Appels suivants: recherche binaire
     * (O(log n)) + lecture mappée en mémoire de la plage seule — même avec 10 ans de données.
     * Repli automatique sur loadCandlesInRange si le cache binaire échoue.
     */
    /**
     * Lit une plage de bougies depuis un fichier cache binaire via recherche binaire.
     */
    private fun readCandlesFromBin(binFile: java.io.File, fromTimestamp: Long, toTimestamp: Long): List<Candle> {
        try {
            RandomAccessFile(binFile, "r").use { raf ->
                val dataLen = raf.length() - BIN_HEADER_SIZE
                val count = (dataLen / BIN_RECORD_SIZE).toInt()
                if (count <= 0) return emptyList()

                val buf = raf.channel.map(java.nio.channels.FileChannel.MapMode.READ_ONLY, 0, raf.length())
                fun tsAt(i: Int): Long = buf.getLong(BIN_HEADER_SIZE + i * BIN_RECORD_SIZE)

                var lo = 0; var hi = count
                while (lo < hi) {
                    val mid = (lo + hi) ushr 1
                    if (tsAt(mid) < fromTimestamp) lo = mid + 1 else hi = mid
                }
                if (lo >= count) return emptyList()

                val candles = ArrayList<Candle>(minOf(count - lo, 300000))
                var i = lo
                var pos = BIN_HEADER_SIZE + lo * BIN_RECORD_SIZE
                while (i < count) {
                    val ts = buf.getLong(pos)
                    if (ts >= toTimestamp) break
                    candles.add(Candle(
                        ts,
                        buf.getFloat(pos + 8),
                        buf.getFloat(pos + 12),
                        buf.getFloat(pos + 16),
                        buf.getFloat(pos + 20),
                        buf.getFloat(pos + 24)
                    ))
                    i++
                    pos += BIN_RECORD_SIZE
                }
                return candles
            }
        } catch (e: Exception) { return emptyList() }
    }

    fun loadCandlesInRangeFast(
        context: Context,
        fileName: String,
        fromTimestamp: Long,
        toTimestamp: Long = Long.MAX_VALUE
    ): List<Candle> {
        val binFile = ensureBinaryCache(context, fileName) ?: return emptyList()
        return readCandlesFromBin(binFile, fromTimestamp, toTimestamp)
    }

    /**
     * Charge une plage de bougies depuis une série de fichiers annuels (ex: 2003-2026).
     * Chaque année est téléchargée/décompressée indépendamment au premier accès.
     */
    fun loadCandlesFromYearRange(
        context: Context,
        baseName: String,
        startYear: Int,
        endYear: Int,
        fromTimestamp: Long,
        toTimestamp: Long = Long.MAX_VALUE
    ): List<Candle> {
        val years = startYear..endYear
        if (years.isEmpty()) return emptyList()
        val executor = java.util.concurrent.Executors.newFixedThreadPool(years.count().coerceAtMost(4))
        try {
            val futures = years.map { year ->
                executor.submit(java.util.concurrent.Callable {
                    loadCandlesInRangeFast(context, "${baseName}_$year", fromTimestamp, toTimestamp)
                })
            }
            val result = ArrayList<Candle>()
            for (f in futures) {
                result.addAll(f.get())
            }
            return result
        } finally {
            executor.shutdown()
        }
    }

    /**
     * Préchauffe le cache binaire pour une année donnée (télécharge le .bin.gz et le décompresse).
     * Ne bloque pas inutilement : ne lit aucune bougie.
     */
    fun preloadYear(context: Context, baseName: String, year: Int) {
        if (!allowMeteredDownloads && isMeteredNetwork(context)) return
        // Lancer le téléchargement/décompression en tâche de fond pour ne pas bloquer le thread appelant.
        val executor = java.util.concurrent.Executors.newSingleThreadExecutor()
        try {
            executor.submit {
                try { ensureBinaryCache(context, "${baseName}_$year") } catch (_: Exception) {}
            }
        } finally {
            executor.shutdown()
        }
    }

    /**
     * Retourne le premier et le dernier timestamp du cache binaire, ou null si absent.
     */
    fun getCachedDataRange(context: Context, fileName: String): Pair<Long, Long>? {
        val binFile = ensureBinaryCache(context, fileName) ?: return null
        return try {
            java.io.RandomAccessFile(binFile, "r").use { raf ->
                val count = ((raf.length() - BIN_HEADER_SIZE) / BIN_RECORD_SIZE).toInt()
                if (count < 2) return@use null
                val buf = raf.channel.map(java.nio.channels.FileChannel.MapMode.READ_ONLY, 0, raf.length())
                val firstTs = buf.getLong(BIN_HEADER_SIZE)
                val lastTs = buf.getLong(BIN_HEADER_SIZE + (count - 1) * BIN_RECORD_SIZE)
                Pair(firstTs, lastTs)
            }
        } catch (e: Exception) { null }
    }

    /**
     * Parse une date au format ISO "yyyy-MM-dd HH:mm:ss" ou "yyyy-MM-dd HH:mm:ss.SSS".
     */
    private fun parseTimestampIso(dateTimeStr: String): Long {
        return try {
            val parts = dateTimeStr.split(' ')
            if (parts.size < 2) return 0L
            val dParts = parts[0].split('-')
            if (dParts.size != 3) return 0L
            val year  = dParts[0].toInt()
            val month = dParts[1].toInt()
            val day   = dParts[2].toInt()
            val tParts = parts[1].split(':')
            if (tParts.size < 2) return 0L
            val hour   = tParts[0].toInt()
            val minute = tParts[1].toInt()
            val secPart = if (tParts.size > 2) tParts[2] else "0"
            val dotIdx  = secPart.indexOf('.')
            val second  = (if (dotIdx >= 0) secPart.substring(0, dotIdx) else secPart).toIntOrNull() ?: 0
            val millis  = if (dotIdx >= 0) secPart.substring(dotIdx + 1).padEnd(3, '0').take(3).toIntOrNull() ?: 0 else 0
            val a   = (14 - month) / 12
            val y   = year - a
            val m   = month + 12 * a - 3
            val jdn = day + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 + 1721119
            val daysSince1970 = jdn.toLong() - 2440588L
            (daysSince1970 * 86400L + hour.toLong() * 3600L + minute.toLong() * 60L + second.toLong()) * 1000L + millis
        } catch (e: Exception) { 0L }
    }

    /**
     * Split rapide par virgule pour les CSV au format "ID,datetime,O,H,L,C,V,...".
     */
    private fun splitCsv(line: String): List<String> {
        val tokens = ArrayList<String>(9)
        var start = 0
        for (i in line.indices) {
            if (line[i] == ',') {
                tokens.add(line.substring(start, i))
                start = i + 1
            }
        }
        tokens.add(line.substring(start))
        return tokens
    }

    /**
     * ⚡ Split rapide sur espaces/tabulations (équivalent à Regex("[ \t]+") mais sans regex).
     */
    private fun splitFast(line: String): List<String> {
        val tokens = ArrayList<String>(8)
        var start = -1
        for (i in line.indices) {
            val ch = line[i]
            if (ch == ' ' || ch == '\t') {
                if (start >= 0) {
                    tokens.add(line.substring(start, i))
                    start = -1
                }
            } else if (start < 0) {
                start = i
            }
        }
        if (start >= 0) tokens.add(line.substring(start))
        return tokens
    }
}
