package cookpad.com.bottomnavwatson

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import bottom_nav_watson.setupWithNavController
import kotlinx.android.synthetic.main.home_activity.*

class HomeActivity : AppCompatActivity(R.layout.home_activity) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bottomNavigation.setupWithNavController(
            graphResId = R.navigation.nav_graph,
            activity = this,
            initialSelectedTabId = R.id.firstTabFragment,
            enabledTabs = listOf(R.id.firstTabFragment, R.id.secondTabFragment, R.id.thirdTabFragment),
            containerId = R.id.navigationHostFragment
        )
    }
}
