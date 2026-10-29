package com.t9dialer.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.t9dialer.app.data.AppPrefs
import com.t9dialer.app.data.PhoneRepository
import com.t9dialer.app.data.PhoneState
import com.t9dialer.app.data.Prefs
import com.t9dialer.app.data.Search
import com.t9dialer.app.data.SearchResult
import com.t9dialer.app.data.SpeedDial
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DialerViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = PhoneRepository(app)
    private val appPrefs = AppPrefs(app)
    val prefs: StateFlow<Prefs> = appPrefs.prefs

    /** Digits typed on the keypad. */
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /** Letters typed in keyboard search; null when keyboard search is closed. */
    private val _text = MutableStateFlow<String?>(null)
    val text: StateFlow<String?> = _text.asStateFlow()

    private val _state = MutableStateFlow(PhoneState())
    val state: StateFlow<PhoneState> = _state.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val results: StateFlow<SearchResult> = combine(_query, _text, _state) { q, t, s -> Triple(q, t, s) }
        .mapLatest { (q, t, s) -> if (t != null) Search.runText(t, s) else Search.run(q, s) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, SearchResult.EMPTY)

    private var loadJob: Job? = null

    /** Re-reads contacts and call history. Runs every time the app comes to the foreground. */
    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.value = withContext(Dispatchers.IO) { repo.load() }
        }
    }

    fun press(key: Char) {
        if (_query.value.length < MAX_LENGTH) _query.value += key
    }

    fun backspace() {
        _query.value = _query.value.dropLast(1)
    }

    fun clear() {
        _query.value = ""
    }

    fun setQuery(raw: String) {
        _query.value = raw.filter { it.isDigit() || it in "+*#" }.take(MAX_LENGTH)
    }

    fun openTextSearch() { _text.value = "" }
    fun setText(t: String) { _text.value = t }
    fun closeTextSearch() { _text.value = null }

    fun format(number: String): String = repo.format(number)

    fun updatePrefs(change: (Prefs) -> Prefs) = appPrefs.update(change)

    fun setSpeedDial(slot: Int, name: String, number: String) = appPrefs.update { p ->
        // One slot per number: moving a number to a new slot frees its old one
        val kept = p.speedDials.filterValues { com.t9dialer.app.data.numberKey(it.number) != com.t9dialer.app.data.numberKey(number) }
        p.copy(speedDials = kept + (slot to SpeedDial(slot, name, number)))
    }

    fun removeSpeedDial(slot: Int) = appPrefs.update { it.copy(speedDials = it.speedDials - slot) }

    private companion object {
        const val MAX_LENGTH = 40
    }
}
