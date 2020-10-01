package com.cookpad.watson

import android.content.Intent
import android.os.Parcelable
import androidx.appcompat.app.AppCompatActivity
import androidx.core.util.forEach
import androidx.core.util.set
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.parcel.Parcelize

/**
 * NavController setup for BottomNavigationView with multiple back stacks, one single navigation graph
 * and lazy initialisation. This implementations is an adaptation from the one that Google provides
 * in its architecture-components-samples that differs from it in that this one allows to use a single nav graph
 * and the NavController of each tab is lazy initialized.
 * Original source: https://github.com/android/architectureºº-components-samples/blob/master/NavigationAdvancedSample
 * /app/src/main/java/com/example/android/navigationadvancedsample/NavigationExtensions.kt
 */
internal class MultipleBackStacks(
    private val fragmentTagsViewModel: FragmentTagsViewModel,
    private val graphResId: Int,
    private val activity: AppCompatActivity,
    private val initialSelectedTabId: Int,
    private val enabledTabs: List<Int>,
    private val containerId: Int
) {
    private val fragmentManager = activity.supportFragmentManager
    private val selectedNavController = MutableLiveData<NavController>()
    private val initialSelectedTabIndex = enabledTabs.indexOf(initialSelectedTabId)
    private val initialFragmentTag = getFragmentTag(initialSelectedTabIndex)
    private val isOnInitialFragment: Boolean
        get() = fragmentTagsViewModel.selectedFragmentTag?.name == initialFragmentTag

    fun onBottomNavigationView(
        bottomNavigationView: BottomNavigationView,
        destinationChangedListener: NavController.OnDestinationChangedListener? = null,
        navigationItemReselectedListener: BottomNavigationView.OnNavigationItemReselectedListener? = null
    ): MutableLiveData<NavController> {
        bottomNavigationView.selectedItemId = initialSelectedTabId

        if (fragmentTagsViewModel.tabIdToFragmentTag[initialSelectedTabId] == null) {
            initNavController(
                index = initialSelectedTabIndex,
                tabId = initialSelectedTabId,
                destinationChangedListener = destinationChangedListener
            )
        } else {
            restoreNavController(destinationChangedListener)
        }

        bottomNavigationView.apply {
            setupOnNavigationItemSelectedListener(destinationChangedListener)
            setupItemReselected(navigationItemReselectedListener, destinationChangedListener)
            setupOnBackStackChangedListener()
        }

        obtainNavHostFragment(getFragmentTag(initialSelectedTabIndex), initialSelectedTabId)
            .navController.handleDeepLink(activity.intent)

        return selectedNavController
    }

    private fun restoreNavController(destinationChangedListener: NavController.OnDestinationChangedListener?) {
        val selectedFragment = fragmentManager.primaryNavigationFragment as NavHostFragment
        selectedNavController.value = selectedFragment.navController.apply {
            destinationChangedListener?.let { addOnDestinationChangedListener(it) }
        }
    }

    private fun BottomNavigationView.setupOnNavigationItemSelectedListener(
        destinationChangedListener: NavController.OnDestinationChangedListener?
    ) {
        setOnNavigationItemSelectedListener { item ->
            if (fragmentManager.isStateSaved) {
                false
            } else {
                val newlySelectedFragmentTag = if (fragmentTagsViewModel.tabIdToFragmentTag[item.itemId] == null) {
                    // Lazy initialization
                    initNavController(
                        index = enabledTabs.indexOf(item.itemId),
                        tabId = item.itemId,
                        destinationChangedListener = destinationChangedListener
                    )
                    fragmentTagsViewModel.tabIdToFragmentTag[item.itemId]
                } else {
                    fragmentTagsViewModel.tabIdToFragmentTag[item.itemId]
                }

                if (fragmentTagsViewModel.selectedFragmentTag != newlySelectedFragmentTag) {
                    val selectedFragment = fragmentManager.findFragmentByTag(newlySelectedFragmentTag.name)
                            as NavHostFragment

                    switchBackStack(newlySelectedFragmentTag.name, selectedFragment)

                    destinationChangedListener?.let {
                        selectedNavController.value?.removeOnDestinationChangedListener(destinationChangedListener)
                    }
                    fragmentTagsViewModel.selectedFragmentTag = newlySelectedFragmentTag
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
    }

    /**
     * Commit a transaction that cleans the back stack and adds the initial fragment
     * to it, creating the fixed started destination.
     */
    private fun switchBackStack(newlySelectedFragmentTag: String, selectedFragment: NavHostFragment) {
        // Pop everything above the initial fragment (the "fixed start destination")
        fragmentManager.popBackStack(
            initialFragmentTag,
            FragmentManager.POP_BACK_STACK_INCLUSIVE
        )

        fragmentManager.beginTransaction()
            .attach(selectedFragment)
            .setPrimaryNavigationFragment(selectedFragment)
            .apply {
                // Detach all other Fragments
                fragmentTagsViewModel.tabIdToFragmentTag.forEach { _, fragmentTag ->
                    if (fragmentTag.name != newlySelectedFragmentTag) {
                        detach(fragmentManager.findFragmentByTag(fragmentTag.name)!!)
                    }
                }
            }
            .apply {
                if (initialFragmentTag != newlySelectedFragmentTag) {
                    addToBackStack(initialFragmentTag)
                    setReorderingAllowed(true)
                }
            }
            .commit()
    }

    private fun initNavController(
        destinationChangedListener: NavController.OnDestinationChangedListener?,
        index: Int,
        tabId: Int
    ) {
        val fragmentManager = activity.supportFragmentManager
        val fragmentTag = getFragmentTag(index)

        // Find or create the Navigation host fragment
        val navHostFragment = obtainNavHostFragment(fragmentTag, tabId)

        // Save to the map
        fragmentTagsViewModel.tabIdToFragmentTag[tabId] = FragmentTag(fragmentTag)

        // Update livedata with the selected graph
        selectedNavController.value = navHostFragment.navController.apply {
            destinationChangedListener?.let { addOnDestinationChangedListener(it) }
        }

        fragmentManager.beginTransaction()
            .attach(navHostFragment)
            .apply { setPrimaryNavigationFragment(navHostFragment) }
            .commitNow()
    }

    /**
     * Find or create the Navigation host fragment
     */
    private fun obtainNavHostFragment(
        fragmentTag: String,
        startDestination: Int
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
        val originalIntent = activity.intent
        activity.intent = Intent()

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

        activity.intent = originalIntent

        return navHostFragment
    }

    private fun BottomNavigationView.setupItemReselected(
        navigationItemReselectedListener: BottomNavigationView.OnNavigationItemReselectedListener?,
        destinationChangedListener: NavController.OnDestinationChangedListener? = null
    ) {
        setOnNavigationItemReselectedListener { item ->
            navigationItemReselectedListener?.onNavigationItemReselected(item)

            // We should not need this here, but we got a NPE if we don't init the nav controller for the item id
            // in some cases that we're not able to replicate but that nevertheless are reported on Crashlytics
            if (fragmentTagsViewModel.tabIdToFragmentTag[item.itemId] == null) {
                initNavController(
                    index = enabledTabs.indexOf(item.itemId),
                    tabId = item.itemId,
                    destinationChangedListener = destinationChangedListener
                )
            }

            val newlySelectedItemTag = fragmentTagsViewModel.tabIdToFragmentTag[item.itemId]
            (fragmentManager.findFragmentByTag(newlySelectedItemTag.name) as? NavHostFragment)?.let {
                val navController = it.navController
                // Pop the back stack to the start destination of the current navController graph
                navController.popBackStack(navController.graph.startDestination, false)
            }
        }
    }

    private fun BottomNavigationView.setupOnBackStackChangedListener() {
        // Finally, ensure that we update our BottomNavigationView when the back stack changes
        fragmentManager.addOnBackStackChangedListener {
            if (!isOnInitialFragment && !fragmentManager.isOnBackStack(initialFragmentTag)) {
                selectedItemId = initialSelectedTabId
            }

            // Reset the graph if the currentDestination is not valid (happens when the back
            // stack is popped after using the back button).
            selectedNavController.value?.let { controller ->
                if (controller.currentDestination == null) {
                    controller.navigate(controller.graph.id)
                }
            }
        }
    }

    private fun getFragmentTag(index: Int) = "bottomNavigation#$index"
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

@Parcelize
data class FragmentTag(val name: String) : Parcelable
