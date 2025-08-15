package com.example.lumka_app

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class OngoingCallFragment : DialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Make the DialogFragment fullscreen without title
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.BLACK))
    }

    override fun onResume() {
        super.onResume()
        requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)?.visibility = View.GONE
        // Hide header
        requireActivity().findViewById<View>(R.id.main_header)?.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()
        requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)?.visibility = View.VISIBLE
        // Restore header
        requireActivity().findViewById<View>(R.id.main_header)?.visibility = View.VISIBLE
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_ongoing_call, container, false)

        val btnReject = view.findViewById<ImageButton>(R.id.btnEndCall)
        btnReject.setOnClickListener {
            dismiss() // This closes the DialogFragment properly
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, DangerAlertsFragment())
                .addToBackStack(null) // optional
                .commit()

            // Update BottomNavigationView menu if needed
            val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)
            bottomNav?.menu?.findItem(R.id.nav_danger_alert)?.isChecked = true
        }

        return view
    }
}
