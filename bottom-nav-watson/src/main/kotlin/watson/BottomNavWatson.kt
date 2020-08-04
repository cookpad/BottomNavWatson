package watson

import androidx.annotation.NavigationRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import com.google.android.material.bottomnavigation.BottomNavigationView

@Suppress("LongParameterList")
fun BottomNavigationView.setupWithNavController(
    @NavigationRes graphResId: Int,
    activity: AppCompatActivity,
    initialSelectedTabId: Int,
    enabledTabs: List<Int>,
    containerId: Int,
    destinationChangedListener: NavController.OnDestinationChangedListener? = null,
    navigationItemReselectedListener: BottomNavigationView.OnNavigationItemReselectedListener? = null
): LiveData<NavController> {
    val viewModel = ViewModelProvider(
        activity,
        // Required to avoid recreating fragments (overlapping issue) during process dead
        object : AbstractSavedStateViewModelFactory(activity, null) {
            override fun <T : ViewModel> create(
                key: String,
                modelClass: Class<T>,
                savedStateHandle: SavedStateHandle
            ): T {
                return MultipleBackStacksViewModel(
                    savedStateHandle = savedStateHandle,
                    graphResId = graphResId,
                    activity = activity,
                    initialSelectedTabId = initialSelectedTabId,
                    enabledTabs = enabledTabs,
                    containerId = containerId
                ) as T
            }
        }
    ).get(MultipleBackStacksViewModel::class.java)

    return viewModel.onBottomNavigationView(
        bottomNavigationView = this,
        destinationChangedListener = destinationChangedListener,
        navigationItemReselectedListener = navigationItemReselectedListener
    )
}
