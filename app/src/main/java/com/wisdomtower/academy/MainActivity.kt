package com.wisdomtower.academy

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.wisdomtower.academy.ui.navigation.WisdomNavHost
import com.wisdomtower.academy.ui.theme.WisdomTowerTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // Enforce anti-leak / anti-piracy: Block screenshots and screen recording across the app
    // TODO: re-enable FLAG_SECURE before production release
    // window.setFlags(
    //   WindowManager.LayoutParams.FLAG_SECURE,
    //   WindowManager.LayoutParams.FLAG_SECURE
    // )
    enableEdgeToEdge()
    setContent {
      WisdomTowerTheme {
        WisdomNavHost()
      }
    }
  }
}

