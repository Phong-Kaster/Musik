package com.example.musik.Playerpage

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.musik.Homepage.HomeActivity.Companion.me
import com.example.musik.R

class PlayerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music_player)

        println("====================PlayerActivity")
        println(me)

    }
}