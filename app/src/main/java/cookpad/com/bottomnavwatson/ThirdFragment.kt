package cookpad.com.bottomnavwatson

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment.findNavController
import kotlinx.android.synthetic.main.third_fragment.textViewThirdTab

class ThirdFragment : Fragment(R.layout.third_fragment) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        textViewThirdTab.setOnClickListener {
            findNavController(this)
                .navigate(NavGraphDirections.actionDetailFragment())
        }
    }
}
