package io.sc.eppCordova.ui.myclaims

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import io.sc.eppCordova.R
import io.sc.eppCordova.data.remote.dto.ClaimResponse
import io.sc.eppCordova.databinding.FragmentMyClaimsBinding
import io.sc.eppCordova.databinding.ItemClaimCardBinding
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MyClaimsFragment : Fragment() {

    private var _binding: FragmentMyClaimsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MyClaimsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyClaimsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        val rv = RecyclerView(requireContext())
        rv.layoutManager = LinearLayoutManager(requireContext())

        val ll = binding.viewPager.parent as ViewGroup
        ll.removeView(binding.viewPager)
        ll.addView(rv)

        val adapter = ClaimAdapter { claim ->
            viewModel.selectClaim(claim)
            // findNavController().navigate(R.id.action_myClaims_to_claimDetail)
            android.widget.Toast.makeText(requireContext(), "Claim Detail selected", android.widget.Toast.LENGTH_SHORT).show()
        }
        rv.adapter = adapter

        viewModel.claimsState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ClaimsLoadState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.tvEmptyState.visibility = View.GONE
                }
                is ClaimsLoadState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    if (state.claims.isEmpty()) {
                        binding.tvEmptyState.visibility = View.VISIBLE
                    } else {
                        binding.tvEmptyState.visibility = View.GONE
                        adapter.submitList(state.claims)
                    }
                }
                is ClaimsLoadState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.tvEmptyState.visibility = View.VISIBLE
                    binding.tvEmptyState.text = "त्रुटी: ${state.message}\nपुन्हा प्रयत्न करण्यासाठी खाली खेचा"
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    class ClaimAdapter(
        private val onItemClick: (ClaimResponse) -> Unit
    ) : RecyclerView.Adapter<ClaimAdapter.ViewHolder>() {

        private var list = listOf<ClaimResponse>()

        fun submitList(newList: List<ClaimResponse>) {
            list = newList
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemClaimCardBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return ViewHolder(binding, onItemClick)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(list[position])
        }

        override fun getItemCount() = list.size

        class ViewHolder(
            val binding: ItemClaimCardBinding,
            private val onItemClick: (ClaimResponse) -> Unit
        ) : RecyclerView.ViewHolder(binding.root) {

            fun bind(claim: ClaimResponse) {
                binding.tvClaimRefId.text = claim.claimId ?: "दावा #${adapterPosition + 1}"
                val claimTypeStr = claim.claimType ?: ""
                val cropTypeStr = claim.cropType ?: ""
                binding.tvClaimDesc.text = if (claimTypeStr.isNotEmpty() || cropTypeStr.isNotEmpty()) {
                    "$claimTypeStr | $cropTypeStr"
                } else {
                    "-"
                }
                binding.tvClaimDate.text = claim.createdAt ?: ""
                val (stageText, colorRes) = workflowStageDisplay(claim.workflowStage)
                binding.chipClaimStatus.text = stageText
                binding.chipClaimStatus.setTextColor(
                    ContextCompat.getColor(binding.root.context, colorRes)
                )
                val indicatorColor = workflowStageColor(claim.workflowStage)
                val drawable = binding.viewStatusIndicator.background as? GradientDrawable
                drawable?.setColor(indicatorColor)

                binding.root.setOnClickListener { onItemClick(claim) }
            }

            private fun workflowStageDisplay(stage: String?): Pair<String, Int> {
                return when {
                    stage == null -> Pair("नवीन", R.color.text_secondary)
                    stage.contains("SUBMITTED", true) -> Pair("Under Verification", R.color.primary)
                    stage.contains("GEO_VERIFIED", true) -> Pair("Geo Verification Completed", R.color.status_green)
                    stage.contains("OFFICER_REVIEW", true) -> Pair("Awaiting Officer Review", R.color.primary)
                    stage.contains("DBT_INITIATED", true) -> Pair("DBT Initiated", R.color.primary)
                    stage.contains("COMPENSATED", true) || stage.contains("RELEASED", true) -> Pair("Compensation Released", R.color.status_green)
                    stage.contains("REJECTED", true) -> Pair("Rejected", R.color.status_red)
                    else -> Pair(stage.replace("_", " "), R.color.text_secondary)
                }
            }

            private fun workflowStageColor(stage: String?): Int {
                val ctx = binding.root.context
                return when {
                    stage == null -> ContextCompat.getColor(ctx, R.color.text_secondary)
                    stage.contains("SUBMITTED", true) -> ContextCompat.getColor(ctx, R.color.primary)
                    stage.contains("GEO_VERIFIED", true) -> ContextCompat.getColor(ctx, R.color.status_green)
                    stage.contains("OFFICER_REVIEW", true) -> ContextCompat.getColor(ctx, R.color.primary)
                    stage.contains("DBT_INITIATED", true) -> ContextCompat.getColor(ctx, R.color.primary)
                    stage.contains("COMPENSATED", true) -> ContextCompat.getColor(ctx, R.color.status_green)
                    stage.contains("REJECTED", true) -> ContextCompat.getColor(ctx, R.color.status_red)
                    else -> ContextCompat.getColor(ctx, R.color.text_secondary)
                }
            }
        }
    }
}
