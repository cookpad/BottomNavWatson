package cookpad.com.bottomnavwatson

import android.content.pm.ActivityInfo
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.agoda.kakao.bottomnav.KBottomNavigationView
import com.agoda.kakao.screen.Screen
import com.agoda.kakao.screen.Screen.Companion.onScreen
import com.agoda.kakao.text.KTextView
import com.kaspersky.components.kautomator.component.common.views.UiView
import com.kaspersky.components.kautomator.screen.UiScreen
import com.kaspersky.components.kautomator.system.UiSystem
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

class HomeScreen : Screen<HomeScreen>() {
    val bottomNavigationView = KBottomNavigationView { withId(R.id.bottomNavigation) }
    val textViewFirstTab = KTextView { withId(R.id.textViewFirstTab) }
    val textViewSecondTab = KTextView { withId(R.id.textViewSecondTab) }
    val textViewThirdTab = KTextView { withId(R.id.textViewThirdTab) }
    val tvTestDeepLinks = KTextView { withId(R.id.tvTestDeepLinks) }
    val tvWithoutBottomMenu = KTextView { withId(R.id.tvWithoutBottomMenu) }
}

object SystemScreen : UiScreen<SystemScreen>() {
    private val context by lazy { InstrumentationRegistry.getInstrumentation().targetContext }
    override val packageName: String
        get() = context.packageName
    val watsonNotification =
        UiView { withText(this@SystemScreen.context.getString(R.string.watson_deep_links)) }
}

class HomeTest : TestCase() {
    @get:Rule
    val activityRule = ActivityTestRule(HomeActivity::class.java)

    @Test
    fun verifyMultipleBackStacks() {
        run {
            step("Select first tab and navigate to detail") {
                onScreen<HomeScreen> {
                    bottomNavigationView { setSelectedItem(R.id.firstTabFragment) }
                    textViewFirstTab { click() }
                    tvTestDeepLinks { isDisplayed() }
                }
            }
            step("Select second tab") {
                onScreen<HomeScreen> {
                    bottomNavigationView { setSelectedItem(R.id.secondTabFragment) }
                    tvTestDeepLinks { doesNotExist() }
                }
            }
            step("Select first tab again and check it keeps the detail screen") {
                onScreen<HomeScreen> {
                    bottomNavigationView { setSelectedItem(R.id.firstTabFragment) }
                    tvTestDeepLinks { isDisplayed() }
                }
            }
        }
    }

    @Test
    fun verifyNotFragmentOverlapping() {
        run {
            step("Select first tab") {
                onScreen<HomeScreen> {
                    bottomNavigationView { setSelectedItem(R.id.firstTabFragment) }
                }
            }
            step("Select second tab") {
                onScreen<HomeScreen> {
                    bottomNavigationView { setSelectedItem(R.id.secondTabFragment) }
                }
            }
            step("Select third tab") {
                onScreen<HomeScreen> {
                    bottomNavigationView { setSelectedItem(R.id.thirdTabFragment) }
                }
            }

            step("Select first tab and check contents") {
                onScreen<HomeScreen> {
                    bottomNavigationView { setSelectedItem(R.id.firstTabFragment) }
                    textViewFirstTab { isDisplayed() }
                    textViewSecondTab { doesNotExist() }
                    textViewThirdTab { doesNotExist() }
                }
            }

            step("Select second tab and check contents") {
                onScreen<HomeScreen> {
                    bottomNavigationView { setSelectedItem(R.id.secondTabFragment) }
                    textViewFirstTab { doesNotExist() }
                    textViewSecondTab { isDisplayed() }
                    textViewThirdTab { doesNotExist() }
                }
            }

            step("Select third tab and check contents") {
                onScreen<HomeScreen> {
                    bottomNavigationView { setSelectedItem(R.id.thirdTabFragment) }
                    textViewFirstTab { doesNotExist() }
                    textViewSecondTab { doesNotExist() }
                    textViewThirdTab { isDisplayed() }
                }
            }
        }
    }

    @Test
    @Ignore("This test fails on Bitrise but it passes locally")
    fun verifyExplicitDeepLink() {
        run {
            step("Send explicit deep link") {
                onScreen<HomeScreen> {
                    bottomNavigationView { setSelectedItem(R.id.firstTabFragment) }
                    textViewFirstTab { click() }
                    tvTestDeepLinks { click() }
                }
            }

            step("Click on notification") {
                SystemScreen {
                    UiSystem.openNotification()
                    watsonNotification { click() }
                }
            }

            step("Check current screen is the deep link target destination") {
                onScreen<HomeScreen> {
                    tvTestDeepLinks { isDisplayed() }
                }
            }

            step("Check that after going back it is the first tab") {
                onScreen<HomeScreen> {
                    pressBack()
                    textViewFirstTab { isDisplayed() }
                    textViewSecondTab { doesNotExist() }
                    textViewThirdTab { doesNotExist() }
                }
            }

            step("Check second tab did not add the deep link target destination") {
                onScreen<HomeScreen> {
                    bottomNavigationView { setSelectedItem(R.id.secondTabFragment) }
                    tvTestDeepLinks { doesNotExist() }
                }
            }
        }
    }

    @Test
    fun verifyConfigChangeByRotatingDevice() {
        run {
            step("Rotate the device to detonate a config change") {
                activityRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }

            step("Check the app did not crash by asserting on any view") {
                onScreen<HomeScreen> {
                    textViewFirstTab { isDisplayed() }
                }
            }
        }
    }

    @Test
    fun verifyCanSelectFirstTabAfterScreenRotate() {
        run {
            step("Select first tab") {
                onScreen<HomeScreen> {
                    bottomNavigationView { setSelectedItem(R.id.firstTabFragment) }
                }
            }
            step("Select second tab") {
                onScreen<HomeScreen> {
                    bottomNavigationView { setSelectedItem(R.id.secondTabFragment) }
                }
            }
            step("Rotate the device to detonate a config change") {
                activityRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }

            step("Select first tab and check contents") {
                onScreen<HomeScreen> {
                    bottomNavigationView { setSelectedItem(R.id.firstTabFragment) }
                    textViewFirstTab { isDisplayed() }
                    textViewSecondTab { doesNotExist() }
                    textViewThirdTab { doesNotExist() }
                }
            }
        }
    }

    @Test
    fun verifyBottomMenuNotVisibleAfterScreenRotate() {
        run {
            step("Select second tab and navigate to detail without bottom menu") {
                onScreen<HomeScreen> {
                    bottomNavigationView { setSelectedItem(R.id.secondTabFragment) }
                    textViewSecondTab { click() }
                    tvTestDeepLinks { isDisplayed() }
                    tvWithoutBottomMenu { click() }
                    bottomNavigationView { isNotDisplayed() }
                }
            }
            step("Rotate the device to detonate a config change") {
                activityRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }

            step("Check that bottom menu is not displayed") {
                onScreen<HomeScreen> {
                    bottomNavigationView { isNotDisplayed() }
                }
            }
        }
    }
}
