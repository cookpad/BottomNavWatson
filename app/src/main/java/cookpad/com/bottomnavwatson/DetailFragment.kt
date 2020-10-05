package cookpad.com.bottomnavwatson

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.NavDeepLinkBuilder
import androidx.navigation.fragment.NavHostFragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import kotlinx.android.synthetic.main.detail_fragment.toolbar
import kotlinx.android.synthetic.main.detail_fragment.tvTestDeepLinks
import kotlinx.android.synthetic.main.detail_fragment.tvWithoutBottomMenu
import java.util.Random

class DetailFragment : Fragment(R.layout.detail_fragment) {

    private val args: DetailFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.setupWithNavController(
            findNavController(this),
            AppBarConfiguration(findNavController(this).graph)
        )

        (requireContext()
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.watson_deep_links),
                NotificationManager.IMPORTANCE_LOW
            )
        )

        tvTestDeepLinks.setOnClickListener {
            val explicitDeepLink = NavDeepLinkBuilder(requireContext())
                .setGraph(R.navigation.nav_graph)
                .setDestination(R.id.detailFragment)
                .setArguments(args.toBundle())
                .setComponentName(HomeActivity::class.java)
                .createPendingIntent()

            NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                .setContentTitle(getString(R.string.watson_deep_links))
                .setContentText(getString(R.string.watson_deep_links_desc))
                .setSmallIcon(R.drawable.ic_notifications)
                .setAutoCancel(true)
                .setContentIntent(explicitDeepLink)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT).let { builder ->
                    with(NotificationManagerCompat.from(requireContext())) {
                        notify(Random().nextInt(), builder.build())
                    }
                }
        }

        toolbar.setTitle(
            if (args.hideNavBar) R.string.detail_without_bottom_menu_fragment else R.string.detail_fragment
        )
        tvWithoutBottomMenu.isVisible = !args.hideNavBar
        if (!args.hideNavBar) {
            tvWithoutBottomMenu.setOnClickListener {
                findNavController(this)
                    .navigate(NavGraphDirections.actionDetailFragment().setHideNavBar(true))
            }
        }
    }

    companion object {
        private const val CHANNEL_ID = "1"
    }
}
