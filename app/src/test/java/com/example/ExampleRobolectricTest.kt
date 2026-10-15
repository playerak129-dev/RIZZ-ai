package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context verifies app identity and phase 2 resources`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    val appEdition = context.getString(R.string.app_edition)
    val phase4Status = context.getString(R.string.status_phase_4)
    val themeDarkLabel = context.getString(R.string.theme_mode_dark)

    assertEquals("RIZZ AI 2.0", appName)
    assertEquals("C4RL3NTZ EDITION", appEdition)
    assertEquals("Phase 4 • Moteur de Styles & Rizz Rapide", phase4Status)
    assertNotNull(themeDarkLabel)
  }

  @Test
  fun `mainActivity launches and renders liquid glass and orb without crashing`() {
    val controller = Robolectric.buildActivity(MainActivity::class.java).create()
    val activity = controller.get()
    assertEquals(false, activity.isFinishing)
    assertNotNull(activity.window.decorView)
  }
}
