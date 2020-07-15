package cookpad.com.bottomnavwatson

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment.findNavController
import kotlinx.android.synthetic.main.first_fragment.*

class FirstFragment : Fragment(R.layout.first_fragment) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        textViewFirstTab.setOnClickListener {
            findNavController(this)
                .navigate(NavGraphDirections.actionDetailFragment())
        }
    }
}
