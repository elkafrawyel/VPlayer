package com.pip.vplayer.ui.splash

import android.os.Bundle
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.pip.vplayer.R
import com.pip.vplayer.ui.home.MainActivity
import com.pip.vplayer.uitiles.PreferencesHelper
import kotlinx.android.synthetic.main.activity_splash.*

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setAppTheme()
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_splash)

        val myFadeInAnimation: Animation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        imageView.startAnimation(myFadeInAnimation)

        myFadeInAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationRepeat(animation: Animation?) {

            }

            override fun onAnimationEnd(animation: Animation?) {
                MainActivity.start(this@SplashActivity)
                finish()
            }

            override fun onAnimationStart(animation: Animation?) {
            }
        })
    }

    private fun setAppTheme() {
        when (PreferencesHelper(this).themeNumber) {
            1 -> setTheme(R.style.AppThemeOne)
            2 -> setTheme(R.style.AppThemeTwo)
            3 -> setTheme(R.style.AppThemeThree)
            4 -> setTheme(R.style.AppThemeFour)
            5 -> setTheme(R.style.AppThemeFive)
        }
    }
}
