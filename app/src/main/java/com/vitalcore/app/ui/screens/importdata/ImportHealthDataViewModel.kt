package com.vitalcore.app.ui.screens.importdata

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalcore.app.integrations.applehealth.AppleHealthCsvImporter
import com.vitalcore.app.integrations.applehealth.AppleHealthImportResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ImportUiState(
    val importing: Boolean = false,
    val result: AppleHealthImportResult? = null,
    val error: String? = null,
)

@HiltViewModel
class ImportHealthDataViewModel @Inject constructor(
    private val importer: AppleHealthCsvImporter,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImportUiState())
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    fun importFile(contentResolver: ContentResolver, uri: Uri) {
        _uiState.value = ImportUiState(importing = true)
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.bufferedReader()?.use { importer.import(it) }
                }
                _uiState.value = if (result != null) {
                    ImportUiState(result = result)
                } else {
                    ImportUiState(error = "Couldn't open that file.")
                }
            } catch (t: Throwable) {
                _uiState.value = ImportUiState(error = t.message ?: "Import failed.")
            }
        }
    }
}
