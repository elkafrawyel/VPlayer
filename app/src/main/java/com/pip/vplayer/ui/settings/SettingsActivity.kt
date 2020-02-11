package com.pip.vplayer.ui.settings

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.pip.vplayer.R
import com.pip.vplayer.uitiles.PreferencesHelper
import kotlinx.android.synthetic.main.activity_settings.*

class SettingsActivity : AppCompatActivity(), View.OnClickListener {

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
            4 -> setTheme(R.style.AppThemeFour)
            5 -> setTheme(R.style.AppThemeFive)
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
        themeOne.setOnClickListener(this)
        themeTwo.setOnClickListener(this)
        themeThree.setOnClickListener(this)
        themeFour.setOnClickListener(this)
        themeFive.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.themeOne -> {
                if (PreferencesHelper(this).themeNumber != 1) {
                    PreferencesHelper(this).themeNumber = 1
                    showMessageInDialog(
                        getString(R.string.restartApp),
                        { restartApplication() },
                        {})
                }
            }
            R.id.themeTwo -> {
                if (PreferencesHelper(this).themeNumber != 2) {
                    PreferencesHelper(this).themeNumber = 2
                    showMessageInDialog(
                        getString(R.string.restartApp),
                        { restartApplication() },
                        {})
                }
            }
            R.id.themeThree -> {
                if (PreferencesHelper(this).themeNumber != 3) {
                    PreferencesHelper(this).themeNumber = 3
                    showMessageInDialog(
                        getString(R.string.restartApp),
                        { restartApplication() },
                        {})
                }
            }
            R.id.themeFour -> {
                if (PreferencesHelper(this).themeNumber != 4) {
                    PreferencesHelper(this).themeNumber = 4
                    showMessageInDialog(
                        getString(R.string.restartApp),
                        { restartApplication() },
                        {})
                }
            }
            R.id.themeFive -> {
                if (PreferencesHelper(this).themeNumber != 5) {
                    PreferencesHelper(this).themeNumber = 5
                    showMessageInDialog(
                        getString(R.string.restartApp),
                        { restartApplication() },
                        {})
                }
            }

        }
    }

    private fun Context.restartApplication() {
        val intent = applicationContext.packageManager.getLaunchIntentForPackage(
            applicationContext.packageName
        )
        intent!!.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent)
        finish()
    }


    private fun Context.showMessageInDialog(
        message: String,
        okAction: () -> Unit,
        cancelAction: () -> Unit
    ) {
        MaterialAlertDialogBuilder(this).setTitle(getString(R.string.app_name))
            .setMessage(message)
            .setPositiveButton(getString(R.string.ok)) { dialog, which ->
                okAction.invoke()
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.notNow)) { dialog, which ->
                cancelAction.invoke()
                dialog.dismiss()
            }.show()

    }


}
