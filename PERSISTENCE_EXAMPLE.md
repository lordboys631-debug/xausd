# Exemple d'implémentation - Persistance des lignes de tendance

Ce fichier fournit des exemples de code pour les améliorations futures.

## 1. Modèle de données pour persistance

```kotlin
// File: app/src/main/java/com/bthr/backtest/model/TrendLineDrawing.kt

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.compose.ui.graphics.Color
import java.util.UUID

@Entity(tableName = \"trend_line_drawings\")
data class TrendLineDrawing(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val chartSymbol: String,
    val timeframe: String,
    val startTimestamp: Long,
    val startPrice: Float,
    val endTimestamp: Long,
    val endPrice: Float,
    val color: Long = Color.Yellow.value,  // Stocké comme Long pour Room
    val strokeWidth: Float = 2f,
    val createdAt: Long = System.currentTimeMillis(),
    val isVisible: Boolean = true
)
```

## 2. Repository pour la gestion des données

```kotlin
// File: app/src/main/java/com/bthr/backtest/repository/TrendLineRepository.kt

import com.bthr.backtest.db.TrendLineDao
import com.bthr.backtest.model.TrendLineDrawing
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TrendLineRepository @Inject constructor(
    private val trendLineDao: TrendLineDao
) {
    
    fun getTrendLines(symbol: String, timeframe: String): Flow<List<TrendLineDrawing>> {
        return trendLineDao.getTrendLinesByChart(symbol, timeframe)
    }
    
    suspend fun saveTrendLine(drawing: TrendLineDrawing) {
        trendLineDao.insert(drawing)
    }
    
    suspend fun updateTrendLine(drawing: TrendLineDrawing) {
        trendLineDao.update(drawing)
    }
    
    suspend fun deleteTrendLine(id: String) {
        trendLineDao.deleteById(id)
    }
    
    suspend fun deleteAllTrendLines(symbol: String, timeframe: String) {
        trendLineDao.deleteByChart(symbol, timeframe)
    }
}
```

## 3. DAO (Data Access Object)

```kotlin
// File: app/src/main/java/com/bthr/backtest/db/TrendLineDao.kt

import androidx.room.*
import com.bthr.backtest.model.TrendLineDrawing
import kotlinx.coroutines.flow.Flow

@Dao
interface TrendLineDao {
    
    @Query(\"SELECT * FROM trend_line_drawings WHERE chartSymbol = :symbol AND timeframe = :timeframe AND isVisible = 1 ORDER BY createdAt DESC\")
    fun getTrendLinesByChart(symbol: String, timeframe: String): Flow<List<TrendLineDrawing>>
    
    @Query(\"SELECT * FROM trend_line_drawings WHERE id = :id\")
    suspend fun getTrendLineById(id: String): TrendLineDrawing?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(drawing: TrendLineDrawing)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(drawings: List<TrendLineDrawing>)
    
    @Update
    suspend fun update(drawing: TrendLineDrawing)
    
    @Query(\"UPDATE trend_line_drawings SET isVisible = 0 WHERE id = :id\")
    suspend fun deleteById(id: String)
    
    @Query(\"DELETE FROM trend_line_drawings WHERE chartSymbol = :symbol AND timeframe = :timeframe\")
    suspend fun deleteByChart(symbol: String, timeframe: String)
}
```

## 4. ViewModel pour gérer l'état

```kotlin
// File: app/src/main/java/com/bthr/backtest/ui/viewmodel/TrendLineViewModel.kt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bthr.backtest.model.TrendLineDrawing
import com.bthr.backtest.repository.TrendLineRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

@HiltViewModel
class TrendLineViewModel @Inject constructor(
    private val repository: TrendLineRepository
) : ViewModel() {
    
    // État local du traçage actuel
    var isTrendLineMode = mutableStateOf(false)
    var trendLinePoint1 = mutableStateOf<Offset?>(null)
    var trendLinePoint2 = mutableStateOf<Offset?>(null)
    var trendLineCrosshair = mutableStateOf<Offset?>(null)
    var currentColor = mutableStateOf(Color.Yellow)
    var currentStrokeWidth = mutableStateOf(2f)
    
    fun getTrendLines(symbol: String, timeframe: String): StateFlow<List<TrendLineDrawing>> {
        return repository.getTrendLines(symbol, timeframe)
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    }
    
    fun saveTrendLine(
        symbol: String,
        timeframe: String,
        startOffset: Offset,
        endOffset: Offset,
        normalizeX: (Float) -> Long,  // Fonction pour convertir pixel en timestamp
        normalizeY: (Float) -> Float   // Fonction pour convertir pixel en prix
    ) {
        viewModelScope.launch {
            val drawing = TrendLineDrawing(
                chartSymbol = symbol,
                timeframe = timeframe,
                startTimestamp = normalizeX(startOffset.x),
                startPrice = normalizeY(startOffset.y),
                endTimestamp = normalizeX(endOffset.x),
                endPrice = normalizeY(endOffset.y),
                color = currentColor.value.value,
                strokeWidth = currentStrokeWidth.value
            )
            repository.saveTrendLine(drawing)
        }
    }
    
    fun deleteTrendLine(id: String) {
        viewModelScope.launch {
            repository.deleteTrendLine(id)
        }
    }
    
    fun updateTrendLine(drawing: TrendLineDrawing) {
        viewModelScope.launch {
            repository.updateTrendLine(drawing)
        }
    }
    
    fun resetDrawingState() {
        isTrendLineMode.value = false
        trendLinePoint1.value = null
        trendLinePoint2.value = null
        trendLineCrosshair.value = null
    }
}
```

## 5. Intégration dans CandlestickChart

```kotlin
// Modification du composant CandlestickChart pour utiliser le ViewModel

@Composable
fun CandlestickChart(
    // ... paramètres existants ...
    trendLineViewModel: TrendLineViewModel? = null,
    symbol: String = \"\",
    timeframe: String = \"\"
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    
    // ... code existant ...
    
    // Charger les lignes sauvegardées
    val savedTrendLines = trendLineViewModel?.getTrendLines(symbol, timeframe)?.collectAsState(emptyList())?.value ?: emptyList()
    
    BoxWithConstraints(modifier = modifier) {
        // ... code existant ...
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            // ... code existant ...
            
            // Dessiner les lignes sauvegardées
            savedTrendLines.forEach { trendLine ->
                val x1 = /* calculer à partir de startTimestamp */
                val y1 = normY(trendLine.startPrice)
                val x2 = /* calculer à partir de endTimestamp */
                val y2 = normY(trendLine.endPrice)
                
                drawLine(
                    color = Color(trendLine.color),
                    start = Offset(x1, y1),
                    end = Offset(x2, y2),
                    strokeWidth = trendLine.strokeWidth
                )
            }
        }
    }
}
```

## 6. Migrations Room (optionnel)

```kotlin
// File: app/src/main/java/com/bthr/backtest/db/migrations/Migration_1_2.kt

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            \"CREATE TABLE IF NOT EXISTS trend_line_drawings (\" +
            \"id TEXT PRIMARY KEY NOT NULL, \" +
            \"chartSymbol TEXT NOT NULL, \" +
            \"timeframe TEXT NOT NULL, \" +
            \"startTimestamp INTEGER NOT NULL, \" +
            \"startPrice REAL NOT NULL, \" +
            \"endTimestamp INTEGER NOT NULL, \" +
            \"endPrice REAL NOT NULL, \" +
            \"color INTEGER NOT NULL, \" +
            \"strokeWidth REAL NOT NULL, \" +
            \"createdAt INTEGER NOT NULL, \" +
            \"isVisible INTEGER NOT NULL)\"
        )
    }
}
```

## 7. Configuration de la base de données

```kotlin
// Modification du AppDatabase pour ajouter TrendLineDao

@Database(
    entities = [
        // ... autres entités ...
        TrendLineDrawing::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trendLineDao(): TrendLineDao
    
    companion object {
        @Volatile
        private var instance: AppDatabase? = null
        
        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context,
                    AppDatabase::class.java,
                    \"backtest_db\"
                )
                .addMigration(MIGRATION_1_2)
                .build()
                .also { instance = it }
            }
        }
    }
}
```

## 8. Module Hilt pour l'injection de dépendances

```kotlin
// Modification du module Hilt

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Singleton
    @Provides
    fun provideDatabase(context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }
    
    @Singleton
    @Provides
    fun provideTrendLineDao(database: AppDatabase): TrendLineDao {
        return database.trendLineDao()
    }
    
    @Singleton
    @Provides
    fun provideTrendLineRepository(dao: TrendLineDao): TrendLineRepository {
        return TrendLineRepository(dao)
    }
}
```

## 9. Exemple d'utilisation complète

```kotlin
// Dans votre Activity/Fragment

@Composable
fun ChartScreenWithTrendLines() {
    val viewModel: TrendLineViewModel = hiltViewModel()
    
    CandlestickChart(
        allCandles = /* ... */,
        trendLineViewModel = viewModel,
        symbol = \"EURUSD\",
        timeframe = \"1H\"
        // ... autres params ...
    )
}
```

## Étapes d'implémentation

1. **Créer les entités et DAOs** (Step 3-4)
2. **Mettre en place le Repository** (Step 2)
3. **Créer le ViewModel** (Step 4)
4. **Ajouter la migration** (Step 6)
5. **Configurer la base de données** (Step 7)
6. **Configurer Hilt** (Step 8)
7. **Intégrer dans CandlestickChart** (Step 5)
8. **Tester la persistance**

## Considérations importantes

### Performance
- Les requêtes Room sont asynchrones (Flow/suspend)
- Utilisez `collectAsState()` avec precaution dans Compose
- Envisagez la pagination pour beaucoup de lignes

### Synchronisation
- Gérez les timestamps correctement (fuseau horaire)
- Convertissez correctement entre pixels et données

### Suppression logique
- Utiliser `isVisible = 0` au lieu de DELETE réel pour garder l'historique
- Permettre la récupération de lignes supprimées

### Export/Import
- Implémenter JSON serialization pour partage
- Valider les données importées

## Notes de développement

- Cette implémentation utilise **Room** pour la persistance (recommandé)
- Compatible avec **Hilt** pour l'injection de dépendances
- Utilise les **Flows** pour la réactivité
- Respecte les patterns MVVM

---

**Statut** : 📋 Exemple pour implémentation future  
**Complexité** : Moyenne  
**Durée estimée** : 2-3 jours de développement

