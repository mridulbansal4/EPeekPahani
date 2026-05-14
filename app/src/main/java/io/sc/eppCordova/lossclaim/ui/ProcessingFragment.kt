package io.sc.eppCordova.lossclaim.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import io.sc.eppCordova.R
import io.sc.eppCordova.databinding.FragmentProcessingBinding

class ProcessingFragment : Fragment() {

    private var _binding: FragmentProcessingBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProcessingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val steps = listOf(
            "Checking satellite NDVI...",
            "Weather verification...",
            "Crop analysis...",
            "Fraud detection...",
            "Compensation scoring..."
        )

        var stepIndex = 0
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                if (stepIndex < steps.size) {
                    binding.tvProcessStep.text = steps[stepIndex]
                    stepIndex++
                    handler.postDelayed(this, 1000)
                } else {
                    findNavController().navigate(R.id.action_processing_to_result)
                }
            }
        }
        handler.postDelayed(runnable, 1000)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}