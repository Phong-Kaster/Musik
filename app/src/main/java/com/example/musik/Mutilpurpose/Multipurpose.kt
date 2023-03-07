package com.example.musik.Mutilpurpose

import android.content.Context
import android.view.Window
import android.view.WindowManager
import androidx.core.content.ContextCompat
import com.example.musik.R

class Multipurpose {
    companion object{
        fun setStatusBarColor(context: Context, window: Window)
        {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.statusBarColor = ContextCompat.getColor(context, R.color.black);
        }
    }
}