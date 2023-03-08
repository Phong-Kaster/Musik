package com.example.musik.Mutilpurpose

import android.content.Context
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.animation.TranslateAnimation
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


        /**
         * @since 07-03-2023
         * slide the view from below itself to the current position
         */

        fun slideUp(view: View) {
            view.visibility = View.VISIBLE
            val animate = TranslateAnimation(
                0F,  // fromXDelta
                0F,  // toXDelta
                view.height.toFloat(),  // fromYDelta
                0F
            ) // toYDelta
            animate.duration = 400
            animate.fillAfter = true
            view.startAnimation(animate)
        }

        /**
         * @since 07-03-2023
         * slide the view from its current position to below itself
         */
        fun slideDown(view: View) {
            val animate = TranslateAnimation(
                0F,  // fromXDelta
                0F,  // toXDelta
                0F,  // fromYDelta
                view.height.toFloat()
            ) // toYDelta
            animate.duration = 400
            animate.fillAfter = true
            view.startAnimation(animate)
        }

        /**
         * @since 07-03-2023
         * get readable time
         */
        fun getReadableTimestamp(duration: Int): String
        {
            var output = "";

            val hour = duration / (1000*60*60)
            val minute = (duration%(1000*60*60)) / (1000 * 60)
            /*val second = ( ( ( duration % (1000*60*60) ) % (1000 * 60 * 60) ) % (1000*60) ) / 1000*/

            val second = (((duration%(1000*60*60))%(1000*60*60))%(1000*60))/1000
            var hourValue = "00"
            val minuteValue = if( minute < 10 ) "0$minute" else minute.toString()
            val secondValue = if( second < 10)  "0$second" else second.toString()

            if( hour > 1)
            {
                hourValue = "0$minute"
                output += "$hourValue:"
            }


            output += "$minuteValue:$secondValue"
            return output
        }
    }
}