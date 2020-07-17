package cookpad.com.bottomnavwatson

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment.findNavController
import kotlinx.android.synthetic.main.second_fragment.*

class SecondFragment : Fragment(R.layout.second_fragment) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        textViewSecondTab.setOnClickListener {
            findNavController(this)
                .navigate(NavGraphDirections.actionDetailFragment())
        }
    }
}
