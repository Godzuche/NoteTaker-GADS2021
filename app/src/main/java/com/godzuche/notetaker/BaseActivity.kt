package com.godzuche.notetaker

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.godzuche.notetaker.ui.MainActivity
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@Suppress("Registered")
open class BaseActivity: AppCompatActivity() {
    protected var userId = "-1"
    private set
    protected var user: FirebaseUser? = null
    private set
    protected var signInMessage = "This action requires signing-in"

    override fun onResume() {
        super.onResume()
        user = Firebase.auth.currentUser
        if (user == null || user?.isAnonymous == true /*|| user?.providerData?.size ?: 0 == 0*/) {
            val intent = Intent(this, MainActivity::class.java)
                .putExtra(MainActivity.SIGNIN_MESSAGE, signInMessage)
            signInLauncher.launch(intent)
        }
    }

    private val signInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            this.onSignInResult(result)
        }

    private fun onSignInResult(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_CANCELED) {
            finish()
        } else {
            val data = result.data
            if (data != null && data.hasExtra(MainActivity.USER_ID))
                userId = data.getStringExtra(MainActivity.USER_ID)!!
            Toast.makeText(this, "You can now create and save notes!", Toast.LENGTH_LONG).show()
        }
    }


}