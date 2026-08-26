package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.DefaultPriceCatalog
import com.example.data.ItemCategory
import com.example.data.PriceFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read app name string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Ритуал Калькулятор", appName)
  }

  @Test
  fun `default catalog items contain ritual categories`() {
    val items = DefaultPriceCatalog.getItems()
    assertTrue(items.isNotEmpty())
    assertTrue(items.any { it.category == ItemCategory.MONUMENTS.displayName })
    assertTrue(items.any { it.category == ItemCategory.ENGRAVING.displayName })
    assertTrue(items.any { it.category == ItemCategory.FENCES_GROUND.displayName })
    assertTrue(items.any { it.category == ItemCategory.BURIAL_SERVICES.displayName })
  }

  @Test
  fun `format price utils output`() {
    val formatted = PriceFormatter.formatRub(38000.0)
    assertTrue(formatted.contains("38") && formatted.contains("000") && formatted.contains("₽"))
  }
}
