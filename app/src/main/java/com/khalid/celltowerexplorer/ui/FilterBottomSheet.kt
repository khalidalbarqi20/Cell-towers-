package com.khalid.celltowerexplorer.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.khalid.celltowerexplorer.databinding.BottomSheetFilterBinding

/**
 * تصفية النتائج حسب المشغّل (STC/Mobily/Zain) ونوع الشبكة (4G/5G) — item 16.
 * onApply(operator, networkType): قيمة null تعني "بدون تصفية" لذلك المعيار.
 */
class FilterBottomSheet(
    private val onApply: (operator: String?, networkType: String?) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetFilterBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.applyFilterButton.setOnClickListener {
            val operator = when (binding.operatorChipGroup.checkedChipId) {
                binding.chipOperatorStc.id -> "STC"
                binding.chipOperatorMobily.id -> "Mobily"
                binding.chipOperatorZain.id -> "Zain"
                else -> null
            }
            val networkType = when (binding.networkTypeChipGroup.checkedChipId) {
                binding.chipNetwork4g.id -> "4G"
                binding.chipNetwork5g.id -> "5G"
                else -> null
            }
            onApply(operator, networkType)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
