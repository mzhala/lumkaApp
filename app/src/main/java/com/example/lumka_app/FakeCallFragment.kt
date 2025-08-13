package com.example.lumka_app


import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.budgettracker.adapter.TransactionAdapter
import com.example.lumka_app.model.Category
import com.example.lumka_app.model.MonthlyBudget
import com.example.lumka_app.model.Transaction
import com.example.lumka_app.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Calendar
import androidx.viewpager2.widget.ViewPager2
import com.example.lumka_app.adapter.ImageAdapter
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [DashboardFrag.newInstance] factory method to
 * create an instance of this fragment.
 */
class FakeCallFragment : DialogFragment() {
    private lateinit var ringtone: Ringtone

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.BLACK))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Make the DialogFragment fullscreen without title
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    }

    override fun onResume() {
        super.onResume()
        requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)?.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()
        requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)?.visibility = View.VISIBLE
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?

    ): View? {
        val view = inflater.inflate(R.layout.fragment_fake_call, container, false)

        val btnAccept = view.findViewById<ImageButton>(R.id.btnAccept)
        val btnReject = view.findViewById<ImageButton>(R.id.btnReject)

        // Play ringtone
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        ringtone = RingtoneManager.getRingtone(requireContext(), uri)
        ringtone.play()

        btnAccept.setOnClickListener {
            ringtone.stop()
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, OngoingCallFragment())
                .addToBackStack(null) // so user can go back if needed
                .commit()

            dismiss() // close the FakeCallFragment
        }

        btnReject.setOnClickListener {
            ringtone.stop()
            //Toast.makeText(requireContext(), "Call Rejected", Toast.LENGTH_SHORT).show()

            dismiss() // This closes the DialogFragment properly
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, DangerAlertsFragment())
                .addToBackStack(null) // optional
                .commit()

            // Update BottomNavigationView menu if needed
            val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)
            bottomNav?.menu?.findItem(R.id.nav_danger_alert)?.isChecked = true
            //dismiss() // This closes the DialogFragment properly
        }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (ringtone.isPlaying) {
            ringtone.stop()
        }
    }


}
