package bottom_nav_watson

import android.content.Intent
import android.util.SparseArray
import androidx.annotation.NavigationRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.util.forEach
import androidx.core.util.set
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * NavController setup for BottomNavigationView with multiple back stacks and one single navigation graph.
 * This implementations is an adaptation from the one that Google provides in its architecture-components-samples
 * that differs from it only in that this one allows to use a single nav graph for the entire app.
 * Original source: https://github.com/android/architecture-components-samples/blob/master/NavigationAdvancedSample
 * /app/src/main/java/com/example/android/navigationadvancedsample/NavigationExtensions.kt
 *
 * selectedTabId parameter will only be taken into consideration when there isn't a deep link, otherwise it will set
 * the first tab as the selected one. This is required to avoid messing up the back stack, which seems to not work
 * properly when the tab is changed during the navController setup + presence of deep link.
 */
fun BottomNavigationView.setupWithNavController(
    @NavigationRes graphResId: Int,
    activity: AppCompatActivity,
    selectedTabId: Int,
    enabledTabs: List<Int>,
    containerId: Int,
    destinationChangedListener: NavController.OnDestinationChangedListener? = null,
    navigationItemReselectedListener: BottomNavigationView.OnNavigationItemReselectedListener? = null
): LiveData<NavController> {
    val fragmentManager = activity.supportFragmentManager

    // Map of tags
    val graphIdToTagMap = SparseArray<String>()
    // Result. Mutable live data with the selected controlled
    val selectedNavController = MutableLiveData<NavController>()

    var firstFragmentGraphId = 0

    // First create a NavHostFragment for each NavGraph ID
    enabledTabs.forEachIndexed { index, tabId ->
        val fragmentTag = getFragmentTag(index)

        // Find or create the Navigation host fragment
        val navHostFragment = activity.obtainNavHostFragment(
            graphResId,
            tabId,
            fragmentManager,
            fragmentTag,
            containerId
        )

        if (index == 0) {
            firstFragmentGraphId = tabId
        }

        // Save to the map
        graphIdToTagMap[tabId] = fragmentTag

        // Attach or detach nav host fragment depending on whether it's the selected item.
        if (this.selectedItemId == tabId) {
            // Update livedata with the selected graph
            selectedNavController.value = navHostFragment.navController.apply {
                destinationChangedListener?.let { addOnDestinationChangedListener(it) }
            }
            attachNavHostFragment(
                fragmentManager,
                navHostFragment,
                index == 0
            )
        } else {
            detachNavHostFragment(fragmentManager, navHostFragment)
        }
    }

    // Now connect selecting an item with swapping Fragments
    var selectedItemTag = graphIdToTagMap[this.selectedItemId]
    val firstFragmentTag = graphIdToTagMap[firstFragmentGraphId]
    var isOnFirstFragment = selectedItemTag == firstFragmentTag

    // When a navigation item is selected
    setOnNavigationItemSelectedListener { item ->
        // Don't do anything if the state is state has already been saved.
        if (fragmentManager.isStateSaved) {
            false
        } else {
            val newlySelectedItemTag = graphIdToTagMap[item.itemId]
            if (selectedItemTag != newlySelectedItemTag) {
                // Pop everything above the first fragment (the "fixed start destination")
                fragmentManager.popBackStack(
                    firstFragmentTag,
                    FragmentManager.POP_BACK_STACK_INCLUSIVE
                )
                val selectedFragment = fragmentManager.findFragmentByTag(newlySelectedItemTag)
                        as NavHostFragment

                // Exclude the first fragment tag because it's always in the back stack.
                if (firstFragmentTag != newlySelectedItemTag) {
                    // Commit a transaction that cleans the back stack and adds the first fragment
                    // to it, creating the fixed started destination.
                    fragmentManager.beginTransaction()
                        .attach(selectedFragment)
                        .setPrimaryNavigationFragment(selectedFragment)
                        .apply {
                            // Detach all other Fragments
                            graphIdToTagMap.forEach { _, fragmentTagIter ->
                                if (fragmentTagIter != newlySelectedItemTag) {
                                    detach(fragmentManager.findFragmentByTag(firstFragmentTag)!!)
                                }
                            }
                        }
                        .addToBackStack(firstFragmentTag)
                        .setReorderingAllowed(true)
                        .commit()
                }

                destinationChangedListener?.let {
                    selectedNavController.value?.removeOnDestinationChangedListener(
                        destinationChangedListener
                    )
                }
                selectedItemTag = newlySelectedItemTag
                isOnFirstFragment = selectedItemTag == firstFragmentTag
                selectedNavController.value = selectedFragment.navController.apply {
                    destinationChangedListener?.let {
                        addOnDestinationChangedListener(destinationChangedListener)
                    }
                }
                true
            } else {
                false
            }
        }
    }

    // Optional: on item reselected, pop back stack to the destination of the graph
    setupItemReselected(navigationItemReselectedListener, graphIdToTagMap, fragmentManager)

    val isThereDeepLink = addDestinationFragmentFromDeepLinkIfNeeded(
        graphResId,
        activity,
        enabledTabs,
        fragmentManager,
        containerId
    )
    // Only select the specified tab if there wasn't a deep link -otherwise the back-stack is messed up.
    if (!isThereDeepLink) {
        selectedItemId = selectedTabId
    }

    // Finally, ensure that we update our BottomNavigationView when the back stack changes
    fragmentManager.addOnBackStackChangedListener {
        if (!isOnFirstFragment && !fragmentManager.isOnBackStack(firstFragmentTag)) {
            this.selectedItemId = firstFragmentGraphId
        }

        // Reset the graph if the currentDestination is not valid (happens when the back
        // stack is popped after using the back button).
        selectedNavController.value?.let { controller ->
            if (controller.currentDestination == null) {
                controller.navigate(controller.graph.id)
            }
        }
    }

    return selectedNavController
}

private fun addDestinationFragmentFromDeepLinkIfNeeded(
    @NavigationRes graphResId: Int,
    activity: AppCompatActivity,
    enabledTabs: List<Int>,
    fragmentManager: FragmentManager,
    containerId: Int
): Boolean {
    // Add the target screen from the deep link to the first tab/stack
    val index = 0
    val firstTabId = enabledTabs[index]
    val fragmentTag = getFragmentTag(index)

    // Find or create the Navigation host fragment
    val navHostFragment = activity.obtainNavHostFragment(
        graphResId,
        firstTabId,
        fragmentManager,
        fragmentTag,
        containerId
    )

    return navHostFragment.navController.handleDeepLink(activity.intent)
}

private fun BottomNavigationView.setupItemReselected(
    navigationItemReselectedListener: BottomNavigationView.OnNavigationItemReselectedListener?,
    graphIdToTagMap: SparseArray<String>,
    fragmentManager: FragmentManager
) {
    setOnNavigationItemReselectedListener { item ->
        navigationItemReselectedListener?.onNavigationItemReselected(item)

        val newlySelectedItemTag = graphIdToTagMap[item.itemId]
        (fragmentManager.findFragmentByTag(newlySelectedItemTag) as? NavHostFragment)?.let {
            val navController = it.navController
            // Pop the back stack to the start destination of the current navController graph
            navController.popBackStack(
                navController.graph.startDestination, false
            )
        }
    }
}

private fun detachNavHostFragment(
    fragmentManager: FragmentManager,
    navHostFragment: NavHostFragment
) {
    fragmentManager.beginTransaction()
        .detach(navHostFragment)
        .commitNow()
}

private fun attachNavHostFragment(
    fragmentManager: FragmentManager,
    navHostFragment: NavHostFragment,
    isPrimaryNavFragment: Boolean
) {
    fragmentManager.beginTransaction()
        .attach(navHostFragment)
        .apply {
            if (isPrimaryNavFragment) {
                setPrimaryNavigationFragment(navHostFragment)
            }
        }
        .commitNow()
}

private fun AppCompatActivity.obtainNavHostFragment(
    @NavigationRes graphResId: Int,
    startDestination: Int,
    fragmentManager: FragmentManager,
    fragmentTag: String,
    containerId: Int
): NavHostFragment {
    // If the Nav Host fragment exists, return it
    val existingFragment = fragmentManager.findFragmentByTag(fragmentTag) as NavHostFragment?
    existingFragment?.let { return it }

    // This is required for explicit deep links, aka push notifications.
    // We need to remove temporally the intent of the activity because when the graph is created,
    // NavController::onGraphCreated calls internally NavController::handleDeepLink which adds, based on the intent,
    // the screen destination from the deep link, specifically it relies in the key bundle
    // NavController::KEY_DEEP_LINK_IDS, but because this is an internal property we can't rely on the fact that
    // this won't change, thus we remove the complete intent during the creating execution and we put back
    // the intent to the activity later on.
    val originalIntent = intent
    intent = Intent()

    // Calling NavHostFragment() will work and that would avoid creating an empty fragment as the start destination,
    // but it starts to corrupt the back stack when Don't keep activities is enabled, which is avoided by
    // calling NavHostFragment.create(graphResId) and setting as the start destination of the graph -in the xml
    // declaration- an empty fragment.
    val navHostFragment = LenientNavHostFragment.create(graphResId)
    fragmentManager.beginTransaction()
        .add(containerId, navHostFragment, fragmentTag)
        .commitNow()

    navHostFragment.navController.graph =
        navHostFragment.navController.navInflater.inflate(graphResId)
            .also { graph -> graph.startDestination = startDestination }

    intent = originalIntent

    return navHostFragment
}

private fun FragmentManager.isOnBackStack(backStackName: String): Boolean {
    val backStackCount = backStackEntryCount
    for (index in 0 until backStackCount) {
        if (getBackStackEntryAt(index).name == backStackName) {
            return true
        }
    }
    return false
}

private fun getFragmentTag(index: Int) = "bottomNavigation#$index"
