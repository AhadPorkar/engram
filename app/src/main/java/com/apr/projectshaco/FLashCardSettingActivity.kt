package com.apr.projectshaco


import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import kotlinx.android.synthetic.main.activity_flashcard_setting.*
import android.widget.RadioButton




class FLashCardSettingActivity : AppCompatActivity() {



    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flashcard_setting)



        start_btn.setOnClickListener {

            val selectedId = typeSelectionFlashcard.checkedRadioButtonId

            // find the radiobutton by returned id
            val selected_radio_btton = findViewById<View>(selectedId) as RadioButton


            var listTypeTag = 0


            if(selected_radio_btton.text == "JumbleWord")
                listTypeTag = 0
            if(selected_radio_btton.text == "StartToEnd")
                listTypeTag = 1
            if(selected_radio_btton.text == "EndToStart")
                listTypeTag = 2
            if(selected_radio_btton.text == "Spaced_repetition")
                listTypeTag = 3
            if(selected_radio_btton.text == "Saved_word")
                listTypeTag = 4



            val intent = Intent(this@FLashCardSettingActivity, FlashCardActivity::class.java)
            intent.putExtra("listTypeTag", listTypeTag)
            startActivity(intent)
        }


    }


}

