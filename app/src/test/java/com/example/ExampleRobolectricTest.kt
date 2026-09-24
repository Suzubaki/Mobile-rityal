package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.DefaultCatalog
import com.example.data.FormatUtils
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
  fun `default constructor service prices are initialized`() {
    val servicePrices = DefaultCatalog.getDefaultConstructorServicePrices()
    assertTrue(servicePrices.isNotEmpty())
    assertTrue(servicePrices.any { it.key == "letter_standard" })
    assertTrue(servicePrices.any { it.key == "usd_exchange_rate" })
  }

  @Test
  fun `format price utils output`() {
    val formatted = PriceFormatter.formatRub(38000.0)
    assertTrue(formatted.contains("38") && formatted.contains("000") && formatted.contains("BYN"))
  }
}
