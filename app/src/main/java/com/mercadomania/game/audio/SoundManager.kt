package com.mercadomania.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.mercadomania.game.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/** Every sound effect the game can play. */
enum class Sfx(internal val resId: Int, internal val volume: Float) {
    CLICK(R.raw.click, 0.55f),
    CORRECT(R.raw.correct, 0.80f),
    WRONG(R.raw.wrong, 0.75f),
    MATCH(R.raw.match, 0.80f),
    WIN(R.raw.win, 0.90f),
    LOSE(R.raw.lose, 0.80f)
}

/**
 * Short SFX playback on top of [SoundPool].
 *
 * Created once in [com.mercadomania.game.MercadoApp]. Loading is asynchronous,
 * so a sample is only played once SoundPool reports it ready; a tap during the
 * first few hundred milliseconds is silently dropped rather than crashing.
 *
 * [enabled] is mirrored from the persisted setting at startup and kept in sync
 * live. When it is false nothing is played at all.
 */
class SoundManager(context: Context) {

    @Volatile
    var enabled: Boolean = true

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(MAX_STREAMS)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val sampleIds = ConcurrentHashMap<Sfx, Int>()
    private val readyIds = ConcurrentHashMap.newKeySet<Int>()

    /**
     * How many samples have finished loading. The loading screen watches this,
     * because SoundPool decoding is genuine startup work.
     */
    private val _loadedSamples = MutableStateFlow(0)
    val loadedSamples: StateFlow<Int> = _loadedSamples.asStateFlow()

    /** How many samples were actually handed to SoundPool. */
    @Volatile
    var expectedSamples: Int = 0
        private set

    @Volatile
    private var released = false

    init {
        pool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0 && readyIds.add(sampleId)) {
                _loadedSamples.value = readyIds.size
            }
        }
        val appContext = context.applicationContext
        var requested = 0
        Sfx.entries.forEach { sfx ->
            runCatching { pool.load(appContext, sfx.resId, 1) }
                .onSuccess { id ->
                    if (id != 0) {
                        sampleIds[sfx] = id
                        requested++
                    }
                }
        }
        expectedSamples = requested
    }

    /** Plays [sfx] if sound is on and the sample finished loading. */
    fun play(sfx: Sfx) {
        if (!enabled || released) return
        val id = sampleIds[sfx] ?: return
        if (id !in readyIds) return
        runCatching { pool.play(id, sfx.volume, sfx.volume, 1, 0, 1f) }
    }

    /** Stops everything currently sounding, e.g. when the game is paused. */
    fun stopAll() {
        if (released) return
        runCatching { pool.autoPause() }
    }

    /** Releases the native pool. Safe to call more than once. */
    fun release() {
        if (released) return
        released = true
        runCatching { pool.release() }
        sampleIds.clear()
        readyIds.clear()
    }

    private companion object {
        const val MAX_STREAMS = 4
    }
}
