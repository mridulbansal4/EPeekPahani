package io.sc.eppCordova.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import io.sc.eppCordova.R
import io.sc.eppCordova.databinding.FragmentLoginBinding
import io.sc.eppCordova.ui.SharedViewModel

@AndroidEntryPoint
class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val sharedViewModel: SharedViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSendOtp.setOnClickListener {
            val mobile = binding.etMobile.text.toString().trim()

            if (mobile.length != 10) {
                Snackbar.make(view, "कृपया 10 अंकी मोबाइल नंबर प्रविष्ट करा", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.btnSendOtp.isEnabled = false
            val originalText = binding.btnSendOtp.text
            binding.btnSendOtp.text = "शोधत आहे..."

            sharedViewModel.sendOtp(mobile) { success, farmer ->
                binding.btnSendOtp.isEnabled = true
                binding.btnSendOtp.text = originalText

                if (farmer != null) {
                    Snackbar.make(view, "शेतकरी सापडला: ${farmer.name}", Snackbar.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_login_to_otp)
                } else {
                    Snackbar.make(view, "नोंदणीकृत शेतकरी आढळला नाही / Phone number not registered", Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
