package cookpad.com.bottomnavwatson

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.NavController.OnDestinationChangedListener
import androidx.navigation.NavDestination
import com.cookpad.watson.setupWithNavController
import kotlinx.android.synthetic.main.home_activity.bottomNavigation

class HomeActivity : AppCompatActivity(R.layout.home_activity) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bottomNavigation.setupWithNavController(
            graphResId = R.navigation.nav_graph,
            activity = this,
            initialSelectedTabId = R.id.firstTabFragment,
            enabledTabs = listOf(R.id.firstTabFragment, R.id.secondTabFragment, R.id.thirdTabFragment),
            containerId = R.id.navigationHostFragment,
            destinationChangedListener = destinationChangedListener()
        )
    }

    private fun destinationChangedListener(): OnDestinationChangedListener {
        return OnDestinationChangedListener { controller: NavController, destination: NavDestination, args: Bundle? ->
            bottomNavigation.isVisible = when (destination.id) {
                R.id.detailWithoutBottomFragment -> false
                else -> true
            }
        }
    }
}
