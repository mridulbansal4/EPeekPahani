package io.sc.eppCordova.ui.auth

import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import io.sc.eppCordova.R
import io.sc.eppCordova.databinding.FragmentOtpBinding
import io.sc.eppCordova.ui.SharedViewModel

@AndroidEntryPoint
class OtpFragment : Fragment() {
    private var _binding: FragmentOtpBinding? = null
    private val binding get() = _binding!!
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private var countDownTimer: CountDownTimer? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOtpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Show masked mobile number
        val mobile = sharedViewModel.farmerState.value?.mobile ?: ""
        if (mobile.length == 10) {
            binding.tvPhoneNumber.text = "+91 XXXXXX${mobile.substring(6)}"
        }

        // Back button
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        setupOtpInputs()
        startResendTimer()

        // Verify OTP
        binding.btnVerifyOtp.setOnClickListener {
            val otp = "${binding.etOtp1.text}${binding.etOtp2.text}${binding.etOtp3.text}${binding.etOtp4.text}"
            if (otp.length == 4) {
                findNavController().navigate(R.id.action_otp_to_lossClaimHome)
            } else {
                Snackbar.make(view, "अवैध OTP", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupOtpInputs() {
        val editTexts = listOf(
            binding.etOtp1,
            binding.etOtp2,
            binding.etOtp3,
            binding.etOtp4
        )
        for (i in 0..2) {
            editTexts[i].addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s?.length == 1) editTexts[i + 1].requestFocus()
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
        // Handle backspace: move to previous field
        for (i in 1..3) {
            editTexts[i].setOnKeyListener { _, keyCode, event ->
                if (keyCode == android.view.KeyEvent.KEYCODE_DEL &&
                    event.action == android.view.KeyEvent.ACTION_DOWN &&
                    editTexts[i].text.isNullOrEmpty()) {
                    editTexts[i - 1].requestFocus()
                    true
                } else false
            }
        }
    }

    private fun startResendTimer() {
        binding.tvResendTimer.isEnabled = false
        countDownTimer = object : CountDownTimer(30000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _binding?.tvResendTimer?.text = "पुन्हा OTP पाठवा (${millisUntilFinished / 1000}s)"
            }
            override fun onFinish() {
                _binding?.tvResendTimer?.isEnabled = true
                _binding?.tvResendTimer?.text = "पुन्हा OTP पाठवा"
            }
        }.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
        countDownTimer = null
        _binding = null
    }
}
