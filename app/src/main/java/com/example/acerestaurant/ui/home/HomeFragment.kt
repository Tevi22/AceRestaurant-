package com.example.acerestaurant.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.acerestaurant.databinding.FragmentHomeBinding
import com.example.acerestaurant.R

/**
 * Entry screen with a simple CTA to begin ordering.
 * HCI: Minimal friction to reach the menu; single clear primary action.
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.loginButton.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_menu)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}