package io.sc.eppCordova.ui.lossclaim

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.card.MaterialCardView
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import io.sc.eppCordova.R
import io.sc.eppCordova.databinding.FragmentLossClaimStep1Binding
import io.sc.eppCordova.ui.SharedViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class LossClaimStep1Fragment : Fragment() {

    private var _binding: FragmentLossClaimStep1Binding? = null
    private val binding get() = _binding!!
    private val viewModel: LossClaimStep1ViewModel by viewModels()
    private val sharedViewModel: SharedViewModel by activityViewModels()

    private var selectedLossCard: MaterialCardView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLossClaimStep1Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupLossTypeCards()
        setupDatePicker()
        setupAreaSlider()
        setupGatDropdown()
        setupNextButton()
        observeData()
    }

    private fun highlightLossCard(card: MaterialCardView, lossType: String) {
        selectedLossCard?.let {
            it.strokeWidth = resources.getDimensionPixelSize(R.dimen.card_stroke_unselected)
            it.strokeColor = resources.getColor(R.color.outline_variant, null)
        }
        card.strokeWidth = resources.getDimensionPixelSize(R.dimen.card_stroke_selected)
        card.strokeColor = resources.getColor(R.color.primary, null)
        selectedLossCard = card
        viewModel.selectedLossType.value = lossType
    }

    private fun setupLossTypeCards() {
        binding.cardLossHeavyRain.setOnClickListener {
            highlightLossCard(binding.cardLossHeavyRain, "अतिवृष्टी")
        }
        binding.cardLossFlood.setOnClickListener {
            highlightLossCard(binding.cardLossFlood, "पूर")
        }
        binding.cardLossDrought.setOnClickListener {
            highlightLossCard(binding.cardLossDrought, "दुष्काळ")
        }
        binding.cardLossPest.setOnClickListener {
            highlightLossCard(binding.cardLossPest, "कीड")
        }
        binding.cardLossHail.setOnClickListener {
            highlightLossCard(binding.cardLossHail, "गारपीट")
        }
        binding.cardLossDisease.setOnClickListener {
            highlightLossCard(binding.cardLossDisease, "रोग")
        }
    }

    private fun setupDatePicker() {
        binding.etIncidentDate.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker().build()
            picker.addOnPositiveButtonClickListener { time ->
                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(time))
                binding.etIncidentDate.setText(dateStr)
                viewModel.incidentDate.value = dateStr
                viewModel.selectedLossType.value?.let { lossType ->
                    viewModel.checkWeather(dateStr, lossType)
                }
            }
            picker.show(parentFragmentManager, "DATE_PICKER")
        }
    }

    private fun setupAreaSlider() {
        binding.seekbarAffectedArea.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = progress / 10.0
                val formatted = String.format("%.1f हेक्टर", value)
                binding.tvAffectedAreaValue.text = formatted
                viewModel.affectedArea.value = value
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupGatDropdown() {
        viewModel.verifiedGats.observe(viewLifecycleOwner) { gats ->
            val gatNumbers = gats.map { "गट क्र. ${it.landRecord?.gutNo ?: ""}" }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, gatNumbers)
            binding.acvLossGat.setAdapter(adapter)

            binding.acvLossGat.setOnItemClickListener { _, _, position, _ ->
                val selected = gats[position]
                viewModel.selectedGat.value = selected
                binding.tvLossSeason.text = "खरीप २०२५ (Auto-filled)"
                viewModel.generateClaimId("NSK", "NIP", selected.landRecord?.gutNo ?: "000")
            }
        }
    }

    private fun setupNextButton() {
        binding.btnLossNext.setOnClickListener {
            if (viewModel.validateAndProceed()) {
                val gat = viewModel.selectedGat.value
                sharedViewModel.setClaimFormData(
                    lossType = viewModel.selectedLossType.value ?: "",
                    incidentDate = viewModel.incidentDate.value ?: "",
                    affectedAreaHa = viewModel.affectedArea.value ?: 0.0,
                    gatNumber = gat?.landRecord?.gutNo ?: "",
                    cropType = gat?.cropRecord?.cropType ?: "",
                    cropName = gat?.cropRecord?.cropName ?: ""
                )
                findNavController().navigate(R.id.action_lossClaimStep1_to_lossClaimStep2)
            } else {
                Snackbar.make(requireView(), "कृपया सर्व माहिती भरा", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeData() {
        viewModel.weatherCheckResult.observe(viewLifecycleOwner) { result ->
            if (result != null) {
                binding.layoutWeatherBanner.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
