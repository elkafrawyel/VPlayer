package com.media.vplayer.ui.settings

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.media.vplayer.R
import com.media.vplayer.uitiles.PreferencesHelper
import kotlinx.android.synthetic.main.activity_settings.*

class SettingsActivity : AppCompatActivity() {

    companion object {

        fun start(context: Context) {
            val intent = Intent(context, SettingsActivity::class.java)
            context.startActivity(intent)
        }
    }

    private fun setAppTheme() {
        when (PreferencesHelper(this).themeNumber) {
            1 -> setTheme(R.style.AppThemeOne)
            2 -> setTheme(R.style.AppThemeTwo)
            3 -> setTheme(R.style.AppThemeThree)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setAppTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        back.setOnClickListener {
            finish()
        }

        pipSwitch.isEnabled = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE))

        pipSwitch.isChecked = PreferencesHelper(this).pipMode == 1
        pipSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                PreferencesHelper(this).pipMode = 1
            } else {
                PreferencesHelper(this).pipMode = 0
            }
        }

        audioSwitch.isChecked = PreferencesHelper(this).audioAllowed == 1
        audioSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                PreferencesHelper(this).audioAllowed = 1
            } else {
                PreferencesHelper(this).audioAllowed = 0
            }

            Toast.makeText(this, getString(R.string.refreshHome), Toast.LENGTH_LONG).show()
        }

        themeOne.setOnClickListener {
            if (PreferencesHelper(this).themeNumber != 1) {
                PreferencesHelper(this).themeNumber = 1
                showMessageInDialog(
                    getString(R.string.restartApp),
                    { restartApplication() },
                    {})
            }
        }

        themeTwo.setOnClickListener {
            if (PreferencesHelper(this).themeNumber != 2) {
                PreferencesHelper(this).themeNumber = 2
                showMessageInDialog(
                    getString(R.string.restartApp),
                    { restartApplication() },
                    {})
            }
        }
        themeThree.setOnClickListener {
            if (PreferencesHelper(this).themeNumber != 3) {
                PreferencesHelper(this).themeNumber = 3
                showMessageInDialog(
                    getString(R.string.restartApp),
                    { restartApplication() },
                    {})
            }
        }
    }

    private fun Context.restartApplication() {
        val intent = applicationContext.packageManager.getLaunchIntentForPackage(
            applicationContext.packageName
        )
        intent!!.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }


    fun Context.showMessageInDialog(
        message: String,
        okAction: () -> Unit,
        cancelAction: () -> Unit
    ) {
        val dialog = AlertDialog.Builder(this)
            .setMessage(message)
            .setTitle(getString(R.string.app_name))
            .setCancelable(true)
            .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                okAction.invoke()
                dialog.dismiss()
            }.setNegativeButton(getString(R.string.notNow)) { dialog, _ ->
                cancelAction.invoke()
                dialog.dismiss()
            }.create()

        dialog.show()

    }
}
