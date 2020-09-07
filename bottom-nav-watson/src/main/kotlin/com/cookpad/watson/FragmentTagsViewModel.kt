package com.cookpad.watson

import android.util.SparseArray
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

internal class FragmentTagsViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
    var tabIdToFragmentTag: SparseArray<FragmentTag>
        get() {
            if (savedStateHandle.get<SparseArray<FragmentTag>?>(KEY_TAB_ID_TO_FRAGMENT_TAG) == null) {
                savedStateHandle[KEY_TAB_ID_TO_FRAGMENT_TAG] = SparseArray<FragmentTag>()
            }
            return savedStateHandle[KEY_TAB_ID_TO_FRAGMENT_TAG]!!
        }
        set(value) = savedStateHandle.set(KEY_TAB_ID_TO_FRAGMENT_TAG, value)

    var selectedFragmentTag: FragmentTag?
        get() = savedStateHandle[KEY_SELECTED_FRAGMENT_TAG]
        set(value) = savedStateHandle.set(KEY_SELECTED_FRAGMENT_TAG, value)

    companion object {
        private const val KEY_TAB_ID_TO_FRAGMENT_TAG = "keyTabIdToFragmentTag"
        private const val KEY_SELECTED_FRAGMENT_TAG = "keySelectedFragmentTag"
    }
}
