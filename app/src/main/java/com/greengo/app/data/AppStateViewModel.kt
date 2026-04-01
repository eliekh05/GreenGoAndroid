package com.greengo.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.core.content.edit

class AppStateViewModel : ViewModel() {

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences("greengo_prefs", Context.MODE_PRIVATE)
        _theme.value        = AppTheme.fromRawValue(prefs.getString("appTheme", "mint") ?: "mint")
        _triviaScore.value  = prefs.getInt("score_trivia", 0)
        _memoryScore.value  = prefs.getInt("score_memory", 0)
        _oceanScore.value   = prefs.getInt("score_ocean", 0)
        _skipMapInfo.value  = prefs.getBoolean("skip_mapInfo", false)
        _skipOceanInfo.value = prefs.getBoolean("skip_oceanInfo", false)
        _skipMemoryInfo.value = prefs.getBoolean("skip_memoryInfo", false)
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private val _screen = MutableStateFlow<Screen>(Screen.Splash)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    private val backStack = mutableListOf<Screen>()

    fun navigate(screen: Screen) {
        backStack.add(_screen.value)
        _screen.value = screen
    }

    fun navigateBack() {
        if (backStack.isNotEmpty()) {
            _screen.value = backStack.removeLast()
        }
    }

    fun canNavigateBack(): Boolean {
        return backStack.isNotEmpty()
    }

    // ── Theme ─────────────────────────────────────────────────────────────────

    private val _theme = MutableStateFlow(AppTheme.MINT)
    val theme: StateFlow<AppTheme> = _theme.asStateFlow()

    fun setTheme(t: AppTheme) {
        _theme.value = t
        prefs.edit { putString("appTheme", t.rawValue) }
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
        prefs.edit { putInt("score_trivia", best) }
    }

    fun saveMemoryScore(v: Int) {
        val best = maxOf(_memoryScore.value, v)
        _memoryScore.value = best
        prefs.edit { putInt("score_memory", best) }
    }

    fun saveOceanScore(v: Int) {
        val best = maxOf(_oceanScore.value, v)
        _oceanScore.value = best
        prefs.edit { putInt("score_ocean", best) }
    }

    fun resetAllScores() {
        _triviaScore.value = 0; _memoryScore.value = 0; _oceanScore.value = 0
        prefs.edit {
            putInt("score_trivia", 0)
            putInt("score_memory", 0)
            putInt("score_ocean", 0)
        }
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
        prefs.edit { putBoolean("skip_mapInfo", v) }
    }

    fun setSkipOceanInfo(v: Boolean) {
        _skipOceanInfo.value = v
        prefs.edit { putBoolean("skip_oceanInfo", v) }
    }

    fun setSkipMemoryInfo(v: Boolean) {
        _skipMemoryInfo.value = v
        prefs.edit { putBoolean("skip_memoryInfo", v) }
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