package com.apr.projectshaco

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView


class MyFrament : Fragment() {
    companion object {
        fun newInstance(word_obj: Word): MyFrament {

            val f = MyFrament()

            val bdl = Bundle(1)

            bdl.putString("word", word_obj.word)
            bdl.putString("meaning", word_obj.meaning)
            bdl.putString("synonyms", word_obj.synonyms)

            f.setArguments(bdl)

            return f

        }
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view_: View? = inflater.inflate(R.layout.fragment_home, container, false)

        val word = arguments!!.getString("word")
        val meaning = arguments!!.getString("meaning")
        val synonyms = arguments!!.getString("synonyms")

        val Word_txt: TextView = view_!!.findViewById(R.id.Word_txt)
        val Meaning_txt: TextView = view_.findViewById(R.id.Meaning_txt)
        val Synonyms_txt: TextView = view_.findViewById(R.id.Synonyms_txt)
        Word_txt.text = word
        Meaning_txt.text = meaning
        Synonyms_txt.text = synonyms

        return view_
    }


}