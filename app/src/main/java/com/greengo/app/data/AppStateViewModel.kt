package com.greengo.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - AppStateViewModel
// Mirrors iOS AppState: navigation, scores, theme, skip-info flags.
// All state is backed by SharedPreferences (equivalent to UserDefaults).
// ─────────────────────────────────────────────────────────────────────────────

class AppStateViewModel : ViewModel() {

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences("greengo_prefs", Context.MODE_PRIVATE)
        _theme.value        = AppTheme.fromRawValue(prefs.getString("appTheme", "mint") ?: "mint")
        _triviaScore.value  = prefs.getInt("score_trivia", 0)
        _memoryScore.value  = prefs.getInt("score_memory", 0)
        _oceanScore.value   = prefs.getInt("score_ocean", 0)
        _skipMapInfo.value  = prefs.getBoolean("skip_mapInfo", false)
        _skipOceanInfo.value  = prefs.getBoolean("skip_oceanInfo", false)
        _skipMemoryInfo.value = prefs.getBoolean("skip_memoryInfo", false)
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private val _screen = MutableStateFlow<Screen>(Screen.Splash)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    fun navigate(screen: Screen) { _screen.value = screen }

    // ── Theme ─────────────────────────────────────────────────────────────────

    private val _theme = MutableStateFlow(AppTheme.MINT)
    val theme: StateFlow<AppTheme> = _theme.asStateFlow()

    fun setTheme(t: AppTheme) {
        _theme.value = t
        prefs.edit().putString("appTheme", t.rawValue).apply()
    }

    // ── High Scores ───────────────────────────────────────────────────────────

    private val _triviaScore = MutableStateFlow(0)
    val triviaScore: StateFlow<Int> = _triviaScore.asStateFlow()

    private val _memoryScore = MutableStateFlow(0)
    val memoryScore: StateFlow<Int> = _memoryScore.asStateFlow()

    private val _oceanScore = MutableStateFlow(0)
    val oceanScore: StateFlow<Int> = _oceanScore.asStateFlow()

    fun saveTriviaScore(v: Int) {
        val best = maxOf(_triviaScore.value, v)
        _triviaScore.value = best
        prefs.edit().putInt("score_trivia", best).apply()
    }

    fun saveMemoryScore(v: Int) {
        val best = maxOf(_memoryScore.value, v)
        _memoryScore.value = best
        prefs.edit().putInt("score_memory", best).apply()
    }

    fun saveOceanScore(v: Int) {
        val best = maxOf(_oceanScore.value, v)
        _oceanScore.value = best
        prefs.edit().putInt("score_ocean", best).apply()
    }

    fun resetAllScores() {
        _triviaScore.value = 0; _memoryScore.value = 0; _oceanScore.value = 0
        prefs.edit()
            .putInt("score_trivia", 0)
            .putInt("score_memory", 0)
            .putInt("score_ocean", 0)
            .apply()
    }

    // ── "Don't show again" flags ──────────────────────────────────────────────

    private val _skipMapInfo = MutableStateFlow(false)
    val skipMapInfo: StateFlow<Boolean> = _skipMapInfo.asStateFlow()

    private val _skipOceanInfo = MutableStateFlow(false)
    val skipOceanInfo: StateFlow<Boolean> = _skipOceanInfo.asStateFlow()

    private val _skipMemoryInfo = MutableStateFlow(false)
    val skipMemoryInfo: StateFlow<Boolean> = _skipMemoryInfo.asStateFlow()

    fun setSkipMapInfo(v: Boolean) {
        _skipMapInfo.value = v
        prefs.edit().putBoolean("skip_mapInfo", v).apply()
    }

    fun setSkipOceanInfo(v: Boolean) {
        _skipOceanInfo.value = v
        prefs.edit().putBoolean("skip_oceanInfo", v).apply()
    }

    fun setSkipMemoryInfo(v: Boolean) {
        _skipMemoryInfo.value = v
        prefs.edit().putBoolean("skip_memoryInfo", v).apply()
    }

    fun resetPreferences() {
        setSkipMapInfo(false); setSkipOceanInfo(false); setSkipMemoryInfo(false)
    }

    fun resetAllAppData() {
        resetAllScores()
        resetPreferences()
        setTheme(AppTheme.MINT)
    }
}
