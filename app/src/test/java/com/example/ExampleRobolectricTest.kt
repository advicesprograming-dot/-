package com.example

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppSettingsEntity
import com.example.ui.adhan.DuaVideoActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("صلاتي", appName)
  }

  @Test
  fun `verify dua video settings default values`() {
    val settings = AppSettingsEntity()
    assertTrue(settings.autoPlayDuaAfterAdhan)
    assertTrue(settings.duaVideoFillScreen)
  }

  @Test
  fun `verify dua video intent creation`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val intent = Intent(context, DuaVideoActivity::class.java).apply {
      putExtra("EXTRA_PRAYER_ID", "FAJR")
      putExtra("EXTRA_PRAYER_NAME", "الفجر")
      putExtra("EXTRA_VIDEO_URI", "content://media/external/video/media/1")
    }
    assertNotNull(intent.component)
    assertEquals("FAJR", intent.getStringExtra("EXTRA_PRAYER_ID"))
    assertEquals("الفجر", intent.getStringExtra("EXTRA_PRAYER_NAME"))
    assertEquals("content://media/external/video/media/1", intent.getStringExtra("EXTRA_VIDEO_URI"))
  }
}
