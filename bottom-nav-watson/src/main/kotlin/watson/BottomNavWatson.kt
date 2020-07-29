package watson

import androidx.annotation.NavigationRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
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
): LiveData<NavController> = NavControllerMultipleBackStacks(
    activity = activity,
    graphResId = graphResId,
    containerId = containerId,
    enabledTabs = enabledTabs,
    initialSelectedTabId = initialSelectedTabId
).onBottomNavigationView(
    bottomNavigationView = this,
    destinationChangedListener = destinationChangedListener,
    navigationItemReselectedListener = navigationItemReselectedListener
)
