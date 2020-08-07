package watson

import android.util.SparseArray
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

internal class FragmentTagsViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
    fun getTabIdToFragmentTag() = savedStateHandle[KEY_TAB_ID_TO_FRAGMENT_TAG] ?: SparseArray<FragmentTag>()
    fun updateTabIdToFragmentTag(tabIdToFragmentTag: SparseArray<FragmentTag>) =
        savedStateHandle.set(KEY_TAB_ID_TO_FRAGMENT_TAG, tabIdToFragmentTag)

    companion object {
        private const val KEY_TAB_ID_TO_FRAGMENT_TAG = "keyTabIdToFragmentTag"
    }
}
