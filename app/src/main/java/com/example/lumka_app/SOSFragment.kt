package com.example.lumka_app

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lumka_app.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class SOSFragment : Fragment() {

    private lateinit var displayName: TextView
    private lateinit var displayEmail: TextView
    private lateinit var initials: TextView
    private lateinit var auth: FirebaseAuth

    private lateinit var tvCountdown: TextView
    private lateinit var btnCancelCall: Button
    private var countdownTimer: CountDownTimer? = null

    private lateinit var adapter: EmergencyContactAdapter
    private val contacts = mutableListOf<EmergencyContact>()

    private val CALL_PHONE_PERMISSION = 1

    override fun onCreateView(
        inflater: android.view.LayoutInflater, container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): android.view.View? {
        val v = inflater.inflate(R.layout.fragment_s_o_s, container, false)
        auth = FirebaseAuth.getInstance()

        // --- UI ---
        displayName = v.findViewById(R.id.displayName)
        displayEmail = v.findViewById(R.id.displayEmail)
        initials = v.findViewById(R.id.initial)
        tvCountdown = v.findViewById(R.id.tvCountdown)
        btnCancelCall = v.findViewById(R.id.btnCancelCall)
        val defaultContact = getDefaultContact()
        defaultContact?.let { makePhoneCall(it.phoneNumber) }

        btnCancelCall.setOnClickListener {
            countdownTimer?.cancel()
            tvCountdown.text = "Call cancelled"
        }

        // Request call permission on fragment launch
        checkCallPermission()

        fetchUserDetails()
        setupRecyclerView(v)
        fetchContacts()  // Load contacts from Firebase

        // FAB click
        val fabAddContact = v.findViewById<FloatingActionButton>(R.id.fabAddContact)
        fabAddContact.setOnClickListener {
            showAddContactDialog()
        }

        return v
    }

    // Check and request CALL_PHONE permission
    private fun checkCallPermission() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CALL_PHONE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.CALL_PHONE),
                CALL_PHONE_PERMISSION
            )
        }
    }

    // Handle permission result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CALL_PHONE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(requireContext(), "Permission granted!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Permission denied. Calls won't work.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getDefaultContact(): EmergencyContact? {
        if (contacts.isEmpty()) return null
        // Try to find the main contact first
        val mainContact = contacts.find { it.isMain }
        return mainContact ?: contacts.minByOrNull { it.timestamp } // oldest contact
    }

    private fun setupRecyclerView(view: View) {
        val rvContacts = view.findViewById<RecyclerView>(R.id.rvContacts)
        adapter = EmergencyContactAdapter(
            requireContext(),
            contacts,
            onCallClick = { contact ->
                makePhoneCall(contact.phoneNumber)
            },
            onSetMainClick = { contact ->
                setMainContact(contact)
            }
        )
        rvContacts.layoutManager = LinearLayoutManager(requireContext())
        rvContacts.adapter = adapter
    }





    private fun setMainContact(contact: EmergencyContact) {
        val userId = auth.currentUser?.uid ?: return
        val ref = FirebaseDatabase.getInstance().getReference("emergency_contacts").child(userId)

        // First, unset any existing main contact
        contacts.forEach { it.isMain = it.id == contact.id }

        // Update all contacts in Firebase
        contacts.forEach { c ->
            c.id?.let { ref.child(it).child("isMain").setValue(c.isMain) }
        }

        adapter.updateList(contacts)
        Toast.makeText(requireContext(), "${contact.name} is now the main contact", Toast.LENGTH_SHORT).show()
    }

    private fun fetchContacts() {
        val userId = auth.currentUser?.uid ?: return
        val ref = FirebaseDatabase.getInstance().getReference("emergency_contacts").child(userId)
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                contacts.clear()
                for (child in snapshot.children) {
                    val contact = child.getValue(EmergencyContact::class.java)
                    contact?.let { contacts.add(it) }
                }
                adapter.updateList(contacts)

                // Start countdown AFTER contacts are loaded
                startCountdown()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun startCountdown() {
        val defaultContact = getDefaultContact()
        if (defaultContact == null) {
            tvCountdown.text = "No contacts available"
            return
        }

        countdownTimer?.cancel() // cancel previous if any

        countdownTimer = object : CountDownTimer(5000, 1000) { // 5-second countdown
            override fun onTick(millisUntilFinished: Long) {
                tvCountdown.text = "Calling ${defaultContact.name} in ${millisUntilFinished / 1000} sec"
            }

            override fun onFinish() {
                makePhoneCall(defaultContact.phoneNumber)
            }
        }.start()
    }


    private fun showAddContactDialog() {
        val builder = AlertDialog.Builder(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_add_contact, null)
        val etName = view.findViewById<EditText>(R.id.etContactName)
        val etPhone = view.findViewById<EditText>(R.id.etContactPhone)

        builder.setView(view)
            .setTitle("Add Contact")
            .setPositiveButton("Add") { dialog, _ ->
                val name = etName.text.toString()
                val phone = etPhone.text.toString()
                if (name.isNotEmpty() && phone.isNotEmpty()) {
                    addEmergencyContact(name, phone)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun addEmergencyContact(name: String, phone: String) {
        val userId = auth.currentUser?.uid ?: return
        val ref = FirebaseDatabase.getInstance().getReference("emergency_contacts").child(userId)
        val key = ref.push().key ?: return
        val contact = EmergencyContact(id = key, userId = userId, name = name, phoneNumber = phone)
        ref.child(key).setValue(contact).addOnSuccessListener {
            Toast.makeText(requireContext(), "Contact added", Toast.LENGTH_SHORT).show()
            contacts.add(contact)
            adapter.updateList(contacts)
        }
    }

    private fun makePhoneCall(number: String) {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CALL_PHONE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            checkCallPermission()
            return
        }
        val callIntent = Intent(Intent.ACTION_CALL)
        callIntent.data = Uri.parse("tel:$number")
        startActivity(callIntent)
    }

    private fun fetchUserDetails() {
        val userId = auth.currentUser?.uid ?: return
        val userRef = FirebaseDatabase.getInstance().getReference("users").child(userId)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val username = snapshot.child("username").getValue(String::class.java)
                val email = snapshot.child("email").getValue(String::class.java)
                displayName.text = username ?: "No username"
                displayEmail.text = email ?: "No email"
                initials.text = username?.firstOrNull()?.toString() ?: ""
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
