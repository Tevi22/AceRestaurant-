package com.example.acerestaurant.ui.checkout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.acerestaurant.R
import com.example.acerestaurant.databinding.FragmentConfirmationBinding

/**
 * Thank-you screen after successful order placement.
 * HCI: Provides clear acknowledgement, order number, and ETA; next actions are obvious.
 */
class ConfirmationFragment : Fragment() {

    private var _binding: FragmentConfirmationBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConfirmationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val args = ConfirmationFragmentArgs.fromBundle(requireArguments())
        val name = args.customerName.ifBlank { getString(R.string.app_name) }
        val orderId = args.orderId
        val eta = args.etaText

        binding.thanksLine.text = "Thanks, $name! Your order has been placed."
        binding.orderNumber.text = "Order #$orderId"
        binding.etaLine.text = "Estimated delivery: $eta"

        binding.btnHome.setOnClickListener {
            findNavController().popBackStack(R.id.menuFragment, inclusive = false)
        }

        binding.btnTrack.setOnClickListener {
            Toast.makeText(requireContext(), "Tracking not implemented yet.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}