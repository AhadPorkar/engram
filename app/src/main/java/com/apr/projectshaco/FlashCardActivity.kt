package com.apr.projectshaco

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.annotation.IntegerRes
import android.support.design.widget.TabLayout
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import java.util.*

class FlashCardActivity : AppCompatActivity() {

    private lateinit var wordViewModel: WordViewModel
    private lateinit var viewpager: ViewPager
    private lateinit var tabs: TabLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flashcard)

        var bundle :Bundle ?=intent.extras
        var type = bundle!!.getInt("listTypeTag")

        initViews()

        initListOfWords(type)
    }

    private fun initViews() {
        tabs = findViewById(R.id.tabs)
        viewpager = findViewById(R.id.viewpager)
    }

    private fun setupViewPager(wordList : List<Word>) {

        val adapter = MyFragmentPagerAdapter(supportFragmentManager)






        wordList.forEach {

            adapter.addFragment(MyFrament.newInstance(it), it.word)
        }






        viewpager.adapter = adapter

        tabs.setupWithViewPager(viewpager)

    }


    private fun initListOfWords(type : Int){

        wordViewModel = ViewModelProviders.of(this, MyViewModelFactory(this.application, type)).get(WordViewModel::class.java)

        wordViewModel.allWords.observe(this, Observer { words ->

            words?.let {setupViewPager(it)}
        })
    }
}