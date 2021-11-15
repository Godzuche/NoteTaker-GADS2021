package com.godzuche.notetaker.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.firebase.ui.auth.AuthUI
import com.godzuche.notetaker.R
import com.godzuche.notetaker.databinding.ActivityListBinding
import com.google.firebase.firestore.*
import com.google.firebase.firestore.EventListener
import com.godzuche.notetaker.data.Note
import com.godzuche.notetaker.data.NoteViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*

class ListActivity : AppCompatActivity() {
    private val TAG = ListActivity::class.qualifiedName
    private lateinit var noteViewModel: NoteViewModel
    private lateinit var adapter: ListRecyclerAdapter
    private lateinit var firestoreDB: FirebaseFirestore
    private lateinit var firestoreNotesListener: ListenerRegistration
    private lateinit var announcementsCollection: CollectionReference
    private lateinit var userId: String

    private lateinit var binding: ActivityListBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        setSupportActionBar(binding.toolbar)

        userId = intent.getStringExtra(MainActivity.USER_ID)!!

        binding.fab.setOnClickListener {
            val noteResultIntent = Intent(this, NewNoteActivity::class.java)
            noteResultLauncher.launch(noteResultIntent)
        }

        loadData()

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_list, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_sync -> true
            R.id.action_logout -> {
                logout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun logout() {
        AuthUI.getInstance().signOut(this)
            .addOnCompleteListener { task ->
                if (task.isSuccessful)
                    startActivity(Intent(this, MainActivity::class.java))
                Toast.makeText(this, "Successfully Signed out!", Toast.LENGTH_SHORT).show()
            }
            .addOnSuccessListener {
                invalidateOptionsMenu()
            }
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val auth = Firebase.auth
        if (auth.currentUser != null && !auth.currentUser!!.isAnonymous) {
            val menuItem = menu?.findItem(R.id.action_logout)
            menuItem?.isVisible = true
        }
        return super.onPrepareOptionsMenu(menu)
    }

    //region data code
    private fun parseDocument(document: DocumentSnapshot): Note {
        return Note(
            document.id,
            document.getString("title")!!,
            document.getString("body")!!,
            document.getLong("date")!!,
            document.getBoolean("announcement")!!
        )
    }

    private fun addNoteToFirestore(note: Note, collection: CollectionReference) {
        collection
            .add(note)
            .addOnSuccessListener { result ->
                Log.d(TAG, "Note added with ID:" + result.id)
                if (note.announcement) {
                    adapter.addNote(note)
                }
            }
            .addOnFailureListener { e -> Log.e(TAG, "Error adding note", e) }
    }

    private fun loadData() {
        invalidateOptionsMenu()
        noteViewModel = ViewModelProvider(this)[NoteViewModel::class.java]
        adapter = ListRecyclerAdapter(this)
        binding.contentList.listNotes.layoutManager = LinearLayoutManager(this)
        binding.contentList.listNotes.adapter = adapter

        firestoreDB = Firebase.firestore // FirebaseFirestore.getInstance()
        announcementsCollection = firestoreDB.collection("announcements")

        announcementsCollection
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                Log.d(TAG, "Retrieving announcements")
                val announcementsList = ArrayList<Note>()
                for (document in result) {
                    announcementsList.add(parseDocument(document))
                }

                if (announcementsList.size == 0) {
                    seedAnnouncements()
                } else {
                    adapter.addNotes(announcementsList)
                    loadNotes()
                }

            }
            .addOnFailureListener { e -> Log.e(TAG, "Error getting announcements", e) }

    }

    private fun seedAnnouncements() {
        var note =
            Note(
                "",
                "Welcome to Note Taker",
                "This is a great way to learn about Firebase Authentication",
                Calendar.getInstance().timeInMillis,
                true
            )

        addNoteToFirestore(note, announcementsCollection)

        note = Note(
            "",
            "Pluralsight",
            "This is one of many great Pluralsight courses on Android",
            Calendar.getInstance().timeInMillis - 10000,
            true
        )

        addNoteToFirestore(note, announcementsCollection)
    }

    private fun loadNotes() {
        if (userId == "-1") {
            noteViewModel.allNotes.observe(this, Observer { notes ->
                notes?.let {
                    adapter.clearNotes(false)
                    adapter.addNotes(notes as ArrayList<Note>)
                }
            })
        } else {
            firestoreNotesListener = firestoreDB.collection(userId)
                .addSnapshotListener(EventListener<QuerySnapshot> { snapshots, e ->
                    if (e != null) {
                        Log.e(TAG, "Failed to listen for new notes", e)
                        return@EventListener
                    }

                    for (dc in snapshots!!.documentChanges) {
                        if (dc.type == DocumentChange.Type.ADDED) {
                            adapter.addNote(parseDocument(dc.document))
                        }
                    }
                })
        }
    }

    private val noteResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val intent = result.data
                val id = UUID.randomUUID().toString()
                val title = intent?.getStringExtra(NewNoteActivity.NEW_TITLE)
                val body = intent?.getStringExtra(NewNoteActivity.NEW_BODY)

                val note = Note(id,
                    title!!,
                    body!!,
                    Calendar.getInstance().timeInMillis,
                    false)

                if (intent.hasExtra(MainActivity.USER_ID) && intent.getStringExtra(
                        MainActivity.USER_ID) != userId && intent.getStringExtra(MainActivity.USER_ID) != "-1"
                ) {
                    userId = result.data!!.getStringExtra(MainActivity.USER_ID)!!
                    loadData()
                }

                if (userId == "-1") {
                    noteViewModel.insert(note)
                } else {
                    addNoteToFirestore(note, firestoreDB.collection(userId))
                }

                Toast.makeText(
                    applicationContext,
                    R.string.saved,
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    applicationContext,
                    R.string.not_saved,
                    Toast.LENGTH_LONG
                ).show()
                if (result.data!!.hasExtra(MainActivity.USER_ID) && result.data!!.getStringExtra(
                        MainActivity.USER_ID) != userId && result.data!!.getStringExtra(MainActivity.USER_ID) != "-1"
                ) {
                    userId = result.data!!.getStringExtra(MainActivity.USER_ID)!!
                    loadData()
                }
            }
        }

    //endregion

}
