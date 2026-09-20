package com.mercadomania.game.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.gameDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "mercado_mania_store"
)

/**
 * The single source of truth for persisted state.
 *
 * Everything lives in one DataStore Preferences entry holding a serialized
 * [GameData] JSON string. Reads never throw: a missing file, an empty store, a
 * deleted key, or a corrupted payload all resolve to [GameData] defaults.
 * Writes never throw either - if storage fails the game keeps playing, it just
 * does not record that round.
 *
 * Created once in [com.mercadomania.game.MercadoApp] and handed to ViewModels
 * through plain ViewModelProvider.Factory instances. No DI framework.
 */
class GameRepository(context: Context) {

    private val appContext = context.applicationContext
    private val store get() = appContext.gameDataStore

    /** Observable game state. Emits [GameData] defaults when storage is empty. */
    val data: Flow<GameData> = store.data
        .catch {
            // IOException from a corrupted/unreadable file, and anything else a
            // broken store could raise, degrade to "no data yet".
            emit(emptyPreferences())
        }
        .map { prefs -> GameSerializer.decode(prefs[KEY_GAME_DATA]) }
        .catch { emit(GameData()) }

    /** Records [score] for [categoryId] when it beats the stored best. */
    suspend fun saveQuizBest(categoryId: String, score: Int) {
        if (categoryId.isBlank()) return
        val clean = score.coerceAtLeast(0)
        update { current ->
            val previous = current.quizBestFor(categoryId)
            if (clean <= previous) current
            else current.copy(quizBest = current.quizBest + (categoryId to clean))
        }
    }

    /** Records [matchedPairs] for [level] when it beats the stored best. */
    suspend fun savePairsBest(level: Int, matchedPairs: Int) {
        if (level <= 0) return
        val key = level.toString()
        val clean = matchedPairs.coerceAtLeast(0)
        update { current ->
            val previous = current.pairsBestFor(level)
            if (clean <= previous) current
            else current.copy(pairsBest = current.pairsBest + (key to clean))
        }
    }

    /** Persists the sound on/off preference. */
    suspend fun setSoundEnabled(enabled: Boolean) {
        update { current -> current.copy(settings = current.settings.copy(soundEnabled = enabled)) }
    }

    /** Wipes every best score and unlocked level, keeping nothing behind. */
    suspend fun resetAll() {
        runCatching {
            store.edit { prefs -> prefs.remove(KEY_GAME_DATA) }
        }
    }

    private suspend fun update(transform: (GameData) -> GameData) {
        runCatching {
            store.edit { prefs ->
                val current = GameSerializer.decode(prefs[KEY_GAME_DATA])
                val next = transform(current)
                if (next != current) {
                    prefs[KEY_GAME_DATA] = GameSerializer.encode(next)
                }
            }
        }
    }

    private companion object {
        val KEY_GAME_DATA = stringPreferencesKey("game_data_json")
    }
}
