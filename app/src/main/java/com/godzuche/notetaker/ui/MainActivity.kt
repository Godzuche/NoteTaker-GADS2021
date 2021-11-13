package com.godzuche.notetaker.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.BuildConfig
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.godzuche.notetaker.databinding.ActivityMainBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

/*Type in the commands on the terminal in the java sdk directory to get the hash keys...Use the release key hash
//development key hash
keytool -exportcert -alias androiddebugkey -keystore "C:\Users\Administrator\.android\debug.keystore" | "C:\Program Files\OpenSSL-Win64\bin\openssl" sha1 -binary | "C:\Program Files\OpenSSL-Win64\bin\openssl" base64

// generating release key hash
keytool -exportcert -alias androiddebugkey -keystore ~/.android/debug.keystore | "C:\Program Files\OpenSSL-Win64\bin\openssl.exe" sha1 -binary | "C:\Program Files\OpenSSL-Win64\bin\openssl.exe" base64*/

class MainActivity : AppCompatActivity() {
    private val TAG = MainActivity::class.qualifiedName
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.btnSignIn.setOnClickListener {
            val providers = arrayListOf(
                AuthUI.IdpConfig.EmailBuilder().build(),
                AuthUI.IdpConfig.GoogleBuilder().build(),
                AuthUI.IdpConfig.FacebookBuilder().build()
            )

            val signInIntent = Intent(AuthUI.getInstance().createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .setIsSmartLockEnabled(!BuildConfig.DEBUG, true)
                .build())

            signInLauncher.launch(signInIntent)
        }

        val auth = Firebase.auth //FirebaseAuth.getInstance()

        if (auth.currentUser != null) {
            val intent = Intent(this, ListActivity::class.java)
            intent.putExtra(USER_ID, auth.currentUser!!.uid)
            startActivity(intent)
        }


    }

    private val signInLauncher =
        registerForActivityResult(FirebaseAuthUIActivityResultContract()) { result ->
            this.onSignInResult(result)
        }

    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        val response = result.idpResponse

        if (result.resultCode == RESULT_OK ) {
            //Successfully signed in
            val user = Firebase.auth.currentUser
            val intent = Intent(this, ListActivity::class.java).apply {
                putExtra(USER_ID, user!!.uid)
            }
            startActivity(intent)
        } else {
            // Sign in failed. If response is null the user canceled the
            // sign-in flow using the back button. Otherwise check
            // response.getError().getErrorCode() and handle the error.
            if (response?.error?.errorCode  == null ) {
                Log.e(TAG, "Back button pressed")
            } else {
                Log.e(TAG, "Sign-in failed due to: ${response.error!!.errorCode}", response.error)
            }
            // ...
        }
    }

    companion object {
        const val USER_ID = "user_id"
    }
}