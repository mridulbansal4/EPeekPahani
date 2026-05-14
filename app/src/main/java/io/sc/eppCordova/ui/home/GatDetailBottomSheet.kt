package io.sc.eppCordova.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.sc.eppCordova.R
import io.sc.eppCordova.data.local.entity.LandRecord
import io.sc.eppCordova.databinding.BottomSheetGatDetailBinding

class GatDetailBottomSheet(
    private val landRecord: LandRecord
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetGatDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetGatDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.tvDetailGatNumber.text = "गट क्रमांक: ${landRecord.gutNo}"
        binding.tvDetailArea.text = "क्षेत्र: ${landRecord.areaHectares} Ha."
        binding.tvDetailOwner.text = "खातेदार: ${landRecord.ownerName}"
        

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
