package com.example.acerestaurant.ui.checkout

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.text.method.DigitsKeyListener
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.acerestaurant.databinding.FragmentCheckoutBinding
import com.example.acerestaurant.ui.cart.CartViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Handles the checkout workflow:
 *  - Displays order summary & totals
 *  - Collects payment and delivery details
 *  - Validates card fields (length, Luhn), expiry, CVV, ZIP
 *  - Navigates to confirmation with generated order id and ETA
 *
 * Ethical programming:
 *  - No PII logging; inline validation; "Cancel" exit; explicit privacy dialog link.
 * HCI:
 *  - Input masking/formatting that respects focus and IME actions.
 *  - Disables "Place Order" when cart is empty.
 */
class CheckoutFragment : Fragment() {

    private var _binding: FragmentCheckoutBinding? = null
    private val b get() = _binding!!

    private val cartVm: CartViewModel by activityViewModels()
    private lateinit var adapter: CheckoutAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCheckoutBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // ---- Cart summary ----
        adapter = CheckoutAdapter(
            data = emptyList(),
            onPlus = { cartVm.increment(it) },
            onMinus = { cartVm.decrement(it) },
            onDelete = { cartVm.removeAt(it) }
        )
        b.summaryList.layoutManager = LinearLayoutManager(requireContext())
        b.summaryList.adapter = adapter

        // ---- Totals & Place Order state ----
        cartVm.items.observe(viewLifecycleOwner) { list ->
            adapter.submit(list)
            b.subtotal.text = "Subtotal: ${cartVm.formatCurrency(cartVm.subtotal())}"
            b.tax.text = "Tax & Fees: ${cartVm.formatCurrency(cartVm.tax())}"
            b.total.text = "Total: ${cartVm.formatCurrency(cartVm.total())}"
            b.btnPlaceOrder.isEnabled = list.isNotEmpty()
        }

        // -------------------------------------------------------------------
        // Payment fields: robust formatting that doesn't steal focus
        // -------------------------------------------------------------------

        // -------------------- CARD NUMBER --------------------
        // Allow digits + spaces; format as #### #### #### #### while typing.
        // Max 16 digits (19 with spaces). Auto-advance to Expiry ONLY when user adds the 16th digit.
        b.cardNumber.keyListener = DigitsKeyListener.getInstance("0123456789 ")
        b.cardNumber.filters = arrayOf(InputFilter.LengthFilter(19)) // "#### #### #### ####" (19 chars incl. spaces)
        b.cardNumber.imeOptions = EditorInfo.IME_ACTION_NEXT

        var ccFormatting = false
        var ccWasDeleting = false

        b.cardNumber.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                ccWasDeleting = count > after
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(e: Editable?) {
                if (ccFormatting) return
                val raw = e?.toString().orEmpty()
                // FIX: keep at most 16 DIGITS (not 19). 19 is the *formatted length* incl. spaces.
                val digits = raw.filter { it.isDigit() }.take(16)
                val formatted = digits.chunked(4).joinToString(" ")

                if (raw == formatted) {
                    // Auto-advance when 16 digits are completed and caret is at end (forward typing only)
                    if (!ccWasDeleting &&
                        digits.length == 16 &&
                        b.cardNumber.hasFocus() &&
                        b.cardNumber.selectionStart == raw.length
                    ) {
                        b.expiry.requestFocus()
                    }
                    return
                }

                // Preserve cursor position relative to new formatting
                val sel = b.cardNumber.selectionStart
                ccFormatting = true
                b.cardNumber.setText(formatted)
                val newSel = (sel + (formatted.length - raw.length)).coerceIn(0, formatted.length)
                b.cardNumber.setSelection(newSel)
                ccFormatting = false
            }
        })

        // Passive validation on blur (won't steal focus)
        b.cardNumber.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validateCardNumber(takeFocus = false) else b.cardNumber.error = null
        }

        // Respect IME Next key
        b.cardNumber.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                b.expiry.requestFocus(); true
            } else false
        }

        // -------------------- EXPIRY (MM/YY) --------------------
        // Formats as MM/YY while typing; max 5 chars.
        b.expiry.keyListener = DigitsKeyListener.getInstance("0123456789/")
        b.expiry.filters = arrayOf(InputFilter.LengthFilter(5))

        var expFormatting = false
        b.expiry.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(e: Editable?) {
                if (expFormatting) return
                val digits = e?.toString()?.filter { it.isDigit() }.orEmpty().take(4)
                val out = if (digits.length <= 2) digits else digits.substring(0, 2) + "/" + digits.substring(2)
                if (out != e.toString()) {
                    expFormatting = true
                    b.expiry.setText(out)
                    b.expiry.setSelection(out.length)
                    expFormatting = false
                }
            }
        })
        b.expiry.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validateExpiry() else b.expiry.error = null
        }

        // CVV check on blur
        b.cvv.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) validateCvv() }

        // ZIP check on blur
        b.zip.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) validateZip() }

        // Cancel navigation
        b.btnCancel.setOnClickListener { findNavController().navigateUp() }

        // Place Order
        b.btnPlaceOrder.setOnClickListener {
            if (!validate()) return@setOnClickListener
            b.btnPlaceOrder.isEnabled = false
            hideKeyboard()

            val name = listOf(
                b.firstName.text?.toString()?.trim().orEmpty(),
                b.lastName.text?.toString()?.trim().orEmpty()
            ).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "Guest" }

            val orderId = generateOrderId()
            val etaText = generateEtaText(plusMinutes = 35)

            // Clear cart AFTER generating values
            cartVm.clear()

            val action = CheckoutFragmentDirections.actionCheckoutToConfirmation(
                orderId = orderId,
                etaText = etaText,
                customerName = name
            )
            findNavController().navigate(action)
        }

        // Privacy & Terms link (ethical programming: transparency/acknowledgement)
        b.tvPrivacyTerms.setOnClickListener {
            PrivacyTermsDialogFragment().show(childFragmentManager, "privacy_terms")
        }
    }

    // -------- Validators --------

    /** Returns contiguous CC digits from the Card Number field (no spaces). */
    private fun panDigits(): String = b.cardNumber.text?.toString()?.replace(Regex("\\D"), "") ?: ""

    /**
     * Validates card number using length (16) and Luhn checksum.
     * @param takeFocus when true, requests focus on error (true on submit, false on blur).
     */
    private fun validateCardNumber(takeFocus: Boolean = true): Boolean {
        val pan = panDigits()

        // Use these numbers for testing (non-PII):
        // 4242 4242 4242 4242, 4111 1111 1111 1111, 5555 5555 5555 4444, 6011 1111 1111 1117

        // Enforce 16 digits exactly (formatted length will be up to 19 incl. spaces)
        if (pan.length != 16) {
            b.cardNumber.error = "Card number must be 16 digits"
            if (takeFocus) {
                b.cardNumber.requestFocus()
                b.cardNumber.setSelection(b.cardNumber.text?.length ?: 0)
            }
            return false
        }

        if (!luhnValid(pan)) {
            b.cardNumber.error = "Invalid card number"
            if (takeFocus) {
                b.cardNumber.requestFocus()
                b.cardNumber.setSelection(b.cardNumber.text?.length ?: 0)
            }
            return false
        }

        b.cardNumber.error = null
        return true
    }

    /** Validates expiry date in MM/YY format and ensures it's not expired. */
    private fun validateExpiry(): Boolean {
        val v = b.expiry.text?.toString().orEmpty()
        val parts = v.split("/")
        if (parts.size != 2 || parts[0].length != 2 || parts[1].length != 2) {
            b.expiry.error = "Use MM/YY"
            return false
        }
        val mm = parts[0].toIntOrNull() ?: -1
        val yy = parts[1].toIntOrNull() ?: -1
        if (mm !in 1..12) {
            b.expiry.error = "Month 01–12"
            return false
        }
        val cal = Calendar.getInstance()
        val curYY = cal.get(Calendar.YEAR) % 100
        val curMM = cal.get(Calendar.MONTH) + 1
        val expired = (yy < curYY) || (yy == curYY && mm < curMM)
        return if (expired) {
            b.expiry.error = "Card expired"
            false
        } else {
            b.expiry.error = null
            true
        }
    }

    /** Validates CVV length (3–4 digits). */
    private fun validateCvv(): Boolean {
        val v = b.cvv.text?.toString().orEmpty()
        return if (v.length in 3..4) {
            b.cvv.error = null; true
        } else {
            b.cvv.error = "CVV must be 3–4 digits"; false
        }
    }

    /** Validates ZIP code (5 digits). */
    private fun validateZip(): Boolean {
        val v = b.zip.text?.toString().orEmpty()
        return if (v.length == 5) {
            b.zip.error = null; true
        } else {
            b.zip.error = "5-digit ZIP required"; false
        }
    }

    /** Luhn algorithm to validate card number checksum. */
    private fun luhnValid(number: String): Boolean {
        var sum = 0
        var alt = false
        for (i in number.length - 1 downTo 0) {
            var n = number[i] - '0'
            if (alt) {
                n *= 2
                if (n > 9) n -= 9
            }
            sum += n
            alt = !alt
        }
        return sum % 10 == 0
    }

    /** Runs all required field validators and shows errors inline. */
    private fun validate(): Boolean {
        val required = listOf(
            b.cardName to "Name",
            b.cardNumber to "Card #",
            b.expiry to "Expiry (MM/YY)",
            b.cvv to "CCV",
            b.zip to "Billing Zip",
            b.firstName to "First Name",
            b.lastName to "Last Name",
            b.address1 to "Address",
            b.city to "City",
            b.state to "State",
            b.shipZip to "Zip Code"
        )
        required.forEach { (field, label) ->
            if (field.text.isNullOrBlank()) {
                field.error = "$label required"
                field.requestFocus()
                return false
            } else field.error = null
        }

        if (!validateCardNumber()) return false
        if (!validateExpiry()) return false
        if (!validateCvv()) return false
        if (!validateZip()) return false

        if (cartVm.items.value.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Your cart is empty.", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    // -------- Helpers --------

    /** Hides the soft keyboard to declutter the UI. */
    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    /** Generates a simple readable order ID like "ACE-4827". */
    private fun generateOrderId(): String {
        val n = (System.currentTimeMillis() % 9000 + 1000).toInt()
        return "ACE-$n"
    }

    /** Generates ETA text in user locale (HH:mm on O+, 12h otherwise). */
    private fun generateEtaText(plusMinutes: Int): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            java.time.LocalTime.now().plusMinutes(plusMinutes.toLong()).toString()
        } else {
            val cal = Calendar.getInstance().apply { add(Calendar.MINUTE, plusMinutes) }
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(cal.time)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}