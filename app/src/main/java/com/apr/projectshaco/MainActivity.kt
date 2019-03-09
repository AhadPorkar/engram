package com.apr.projectshaco


import android.Manifest
import android.app.Dialog
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.support.v4.app.ActivityCompat
import android.content.pm.PackageManager
import android.app.Activity
import android.app.Application


class MainActivity : AppCompatActivity() {


    private lateinit var wordViewModel: WordViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)




        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerview)
        val adapter = WordListAdapter(this)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)



        wordViewModel = ViewModelProviders.of(this, MyViewModelFactory(this.application, 1)).get(WordViewModel::class.java)


        wordViewModel.allWords.observe(this, Observer { words ->

            words?.let { adapter.setWords(it) }
        })

        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener {

            var dialog = Dialog(this)
            dialog.setContentView(R.layout.word_input_dialog)
            dialog.setTitle("New Word")
            dialog.show()

            val savebtn = dialog.findViewById(R.id.save_btn) as Button
            val word_edittext = dialog.findViewById(R.id.word_edittext) as EditText
            val meaning_edittext = dialog.findViewById(R.id.meaning_edittext) as EditText
            val synosyms_edittext = dialog.findViewById(R.id.synosyms_edittext) as EditText
            savebtn.setOnClickListener {
                val word = Word(0
                        , word_edittext.text.toString()
                        , meaning_edittext.text.toString()
                        , synosyms_edittext.text.toString())
                wordViewModel.insert(word)

                dialog.hide()
            }


        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {

        val inflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    private val REQUEST_EXTERNAL_STORAGE = 1
    private val PERMISSIONS_STORAGE = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {

            R.id.action_flashcard -> {
                val intent = Intent(this@MainActivity, FLashCardSettingActivity::class.java)
                startActivity(intent)
                return true
            }
            R.id.action_backup -> {
                var dbutil = DBUtil(this)
                verifyStoragePermissions(this)
                dbutil.SaveDB()
                return true
            }
            R.id.action_import -> {

                return true
            }

        }
        return super.onOptionsItemSelected(item)
    }


    private fun verifyStoragePermissions(activity: Activity) {

        val permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)

        if (permission != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            )
        }
    }
}
