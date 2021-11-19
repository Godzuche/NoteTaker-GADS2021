package com.godzuche.notetaker.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import com.firebase.ui.auth.AuthUI
import com.godzuche.notetaker.BaseActivity
import com.godzuche.notetaker.R
import com.godzuche.notetaker.databinding.ActivitySettingsBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class SettingsActivity : BaseActivity() {
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        actionBar?.setDisplayHomeAsUpEnabled(true)

        signInMessage = "Sign-in to see your settings"
        val authUI = AuthUI.getInstance()

        binding.tvName.setOnClickListener {
            val alertDialogBuilder = MaterialAlertDialogBuilder(this)
            val alertView = layoutInflater.inflate(R.layout.change_name_dialog, null)
            alertDialogBuilder.setView(alertView)

            val name = alertView.findViewById<TextInputEditText>(R.id.etDisplayName)
            val btnUpdate = alertView.findViewById<MaterialButton>(R.id.btnUpdate)

            name.setText(binding.tvName.text)

            name.addTextChangedListener(object: TextWatcher{
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

                }

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    btnUpdate.isEnabled = user?.displayName != p0.toString()
                }

                override fun afterTextChanged(p0: Editable?) {

                }

            })
            val alertDialog = alertDialogBuilder.create()

            btnUpdate.setOnClickListener {
                binding.tvName.text = name.text
                updateName()
                alertDialog.dismiss()
            }
            alertDialog.show()
        }

        binding.btnSignOut.setOnClickListener {
            authUI.signOut(this)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Successfully signed out", Toast.LENGTH_LONG).show()
                        startActivity(Intent(this, MainActivity::class.java))
                    }
                    else {
                        Log.e(TAG, "Sign-out failed!", task.exception)
                        Toast.makeText(this, "Sign-out failed!", Toast.LENGTH_LONG).show()
                    }
                }
        }

        binding.btnDelete.setOnClickListener {
            MaterialAlertDialogBuilder(this, R.style.DialogTheme)
                .setTitle("Delete Account")
                .setMessage("This is permanent, are you sure?")
                .setPositiveButton("Yes") {_,_ ->
                    authUI.delete(this)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful)
                                startActivity(Intent(this, MainActivity::class.java))
                            else {
                                Log.e(TAG, "Delete account failed!", task.exception)
                                Toast.makeText(this, "Delete account failed", Toast.LENGTH_LONG).show()
                            }
                        }
                }
                .setNegativeButton("No", null)
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.tvName.text = user?.displayName
    }

/*    override fun onPause() {
        super.onPause()
//        updateName()
    }*/

    private fun updateName() {
        val profile = UserProfileChangeRequest.Builder()
            .setDisplayName(binding.tvName.text.toString())
            .build()

        if (user != null) {
            user!!.updateProfile(profile)
                .addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        Log.e(TAG, "Failed to upgrade display name", task.exception)
                        Toast.makeText(this, "Failed to upgrade display name", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }

    companion object {
        private val TAG = SettingsActivity::class.qualifiedName
    }
}