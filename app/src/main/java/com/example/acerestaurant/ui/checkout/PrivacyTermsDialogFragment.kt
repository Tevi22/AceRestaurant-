package com.example.acerestaurant.ui.checkout

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import android.view.LayoutInflater
import com.example.acerestaurant.R

/**
 * Simple modal dialog that shows the app's privacy policy & terms.
 * Ethical programming: transparency and user acknowledgement without coercion.
 */
class PrivacyTermsDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_privacy_terms, null, false)

        return AlertDialog.Builder(requireContext())
            .setView(view)
            .setPositiveButton(R.string.ok, null)
            .create()
    }
}