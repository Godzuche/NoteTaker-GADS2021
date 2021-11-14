package com.godzuche.notetaker.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.godzuche.notetaker.R
import com.godzuche.notetaker.databinding.ActivityNewNoteBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class NewNoteActivity : AppCompatActivity() {
private lateinit var binding: ActivityNewNoteBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewNoteBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val user = Firebase.auth.currentUser
        if (user == null || user.isAnonymous){
            val intent = Intent(this, MainActivity::class.java)
                .putExtra(MainActivity.SIGNIN_MESSAGE, "Sign-in to create a new note")
            signInLauncher.launch(intent)
        }

        binding.btnSave.setOnClickListener {
            val resultIntent = Intent()

            if (TextUtils.isEmpty(binding.etTitle.text) || TextUtils.isEmpty(binding.etBody.text)) {
                setResult(Activity.RESULT_CANCELED, resultIntent)
            } else {
                val title = binding.etTitle.text.toString()
                val body = binding.etBody.text.toString()

                resultIntent.putExtra(NEW_TITLE, title)
                resultIntent.putExtra(NEW_BODY, body)
                setResult(Activity.RESULT_OK, resultIntent)
            }
            finish()
        }
    }

    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        this.onSignInResult(result)
    }

    private fun onSignInResult(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_CANCELED) {
            finish()
        } else
            Toast.makeText(this, "You can now create and save notes!", Toast.LENGTH_LONG).show()
    }

    companion object {
        const val NEW_TITLE = "new_title"
        const val NEW_BODY = "new_body"
    }
}
