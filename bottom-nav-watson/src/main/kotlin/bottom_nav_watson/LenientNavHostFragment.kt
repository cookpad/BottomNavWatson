package bottom_nav_watson

import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment

/**
 * We need to subclass NavHostFragment to try/catch the call to super::onDestroyView
 * as it throws an IllegalStateException when a deep link is launched starting from version
 * 2.3.0 of the navigation component. Specifically, it crashes when the NavHostFragment
 * tries to dispose itself by finding its associated NavController and there is none.
 */
class LenientNavHostFragment : NavHostFragment() {

    override fun onDestroyView() {
        try {
            super.onDestroyView()
        } catch (e: IllegalStateException) {
            // We only sallow the exception when it contains the message related with the issue that we know.
            // Otherwise we rethrow, this is brittle as it ties the implementation to a private reporting message error,
            // but we prefer this way to avoid hiding unrelated exceptions.
            if (e.message?.contains("does not have a NavController set") == false) {
                throw e
            }
        }
    }

    companion object {
        // We need to duplicate here the key as it's a private field in the library, which is brittle but it is what it is.
        private const val KEY_GRAPH_ID = "android-support-nav:fragment:graphId"

        fun create(graphResId: Int): NavHostFragment = LenientNavHostFragment().apply {
            arguments = Bundle().apply { putInt(KEY_GRAPH_ID, graphResId) }
        }
    }

}
