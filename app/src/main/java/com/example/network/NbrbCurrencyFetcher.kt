package com.example.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

data class CurrencyRateResult(
    val rate: Double,
    val source: String,
    val date: String? = null
)

object NbrbCurrencyFetcher {
    private const val TAG = "NbrbCurrencyFetcher"

    // 1. Official HTML Webpage of daily rates on nbrb.by
    private const val NBRB_HTML_URL = "https://www.nbrb.by/statistics/rates/ratesdaily"

    // 2. Official NBRB REST JSON APIs
    private val NBRB_API_ENDPOINTS = listOf(
        "https://api.nbrb.by/exrates/rates/431",
        "https://www.nbrb.by/api/exrates/rates/431",
        "https://api.nbrb.by/exrates/rates/USD?parammode=2",
        "https://www.nbrb.by/api/exrates/rates/USD?parammode=2",
        "https://api.nbrb.by/exrates/rates?periodicity=0",
        "https://www.nbrb.by/api/exrates/rates?periodicity=0"
    )

    // 3. Fallback international rate providers in case nbrb.by is unreachable
    private val FALLBACK_ENDPOINTS = listOf(
        "https://open.er-api.com/v6/latest/USD",
        "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/usd.json"
    )

    suspend fun fetchUsdRateWithDetails(): CurrencyRateResult? = withContext(Dispatchers.IO) {
        // Step 1: Try HTML scraping from the official nbrb.by/statistics/rates/ratesdaily page
        try {
            val htmlResult = fetchFromNbrbHtmlPage(NBRB_HTML_URL)
            if (htmlResult != null) {
                Log.d(TAG, "Successfully extracted USD rate from HTML page: ${htmlResult.rate}")
                return@withContext htmlResult
            }
        } catch (e: Exception) {
            Log.w(TAG, "HTML parser error: ${e.message}")
        }

        // Step 2: Try direct NBRB JSON endpoints
        for (apiUrl in NBRB_API_ENDPOINTS) {
            try {
                val res = if (apiUrl.contains("periodicity=0")) {
                    fetchFromNbrbList(apiUrl)
                } else {
                    fetchFromNbrbDirect(apiUrl)
                }
                if (res != null) {
                    Log.d(TAG, "Successfully fetched from NBRB API $apiUrl: ${res.rate}")
                    return@withContext res
                }
            } catch (e: Exception) {
                Log.w(TAG, "NBRB API endpoint $apiUrl error: ${e.message}")
            }
        }

        // Step 3: Fallback providers
        for (fallbackUrl in FALLBACK_ENDPOINTS) {
            try {
                val res = fetchFromFallback(fallbackUrl)
                if (res != null) {
                    Log.d(TAG, "Fetched from fallback $fallbackUrl: ${res.rate}")
                    return@withContext res
                }
            } catch (e: Exception) {
                Log.w(TAG, "Fallback $fallbackUrl error: ${e.message}")
            }
        }

        Log.e(TAG, "All rate fetch attempts failed.")
        return@withContext null
    }

    suspend fun fetchUsdRate(): Double? {
        return fetchUsdRateWithDetails()?.rate
    }

    private fun openConnection(urlString: String): HttpURLConnection {
        val url = URL(urlString)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 9000
            readTimeout = 9000
            instanceFollowRedirects = true
            setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            )
            setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml,application/json;q=0.9,*/*;q=0.8")
            setRequestProperty("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7")
        }
        return conn
    }

    /**
     * Parses the official rates page HTML: https://www.nbrb.by/statistics/rates/ratesdaily
     * Finds the row for USD / Доллар США and extracts the official rate.
     */
    private fun fetchFromNbrbHtmlPage(urlString: String): CurrencyRateResult? {
        var conn: HttpURLConnection? = null
        try {
            conn = openConnection(urlString)
            val code = conn.responseCode
            if (code in 200..299) {
                val html = conn.inputStream.bufferedReader().use { it.readText() }

                // 1. Search for USD row in the table
                // Pattern for rows like: <td>Доллар США</td>...<td>3,2545</td> or USD ... 3.2545
                val usdIndex = html.indexOf("USD", ignoreCase = true).let {
                    if (it != -1) it else html.indexOf("Доллар США", ignoreCase = true)
                }

                if (usdIndex != -1) {
                    val snippet = html.substring(usdIndex, (usdIndex + 600).coerceAtMost(html.length))
                    // Look for decimal number like 3,2545 or 3.2545 or 3,2500
                    val rateRegex = Pattern.compile("(\\d{1,2}[.,]\\d{2,4})")
                    val matcher = rateRegex.matcher(snippet)
                    while (matcher.find()) {
                        val numStr = matcher.group(1)?.replace(',', '.') ?: continue
                        val parsedRate = numStr.toDoubleOrNull()
                        // USD to BYN is typically between 1.5 and 10.0
                        if (parsedRate != null && parsedRate > 1.5 && parsedRate < 15.0) {
                            return CurrencyRateResult(
                                rate = parsedRate,
                                source = "nbrb.by/statistics/rates/ratesdaily"
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "fetchFromNbrbHtmlPage error: ${e.message}")
        } finally {
            conn?.disconnect()
        }
        return null
    }

    private fun fetchFromNbrbDirect(urlString: String): CurrencyRateResult? {
        var conn: HttpURLConnection? = null
        try {
            conn = openConnection(urlString)
            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val officialRate = json.optDouble("Cur_OfficialRate", -1.0)
                val scale = json.optDouble("Cur_Scale", 1.0)
                val date = json.optString("Date", null)
                if (officialRate > 0) {
                    val rate = officialRate / (if (scale > 0) scale else 1.0)
                    return CurrencyRateResult(rate = rate, source = "НБ РБ (nbrb.by)", date = date)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "fetchFromNbrbDirect $urlString error: ${e.message}")
        } finally {
            conn?.disconnect()
        }
        return null
    }

    private fun fetchFromNbrbList(urlString: String): CurrencyRateResult? {
        var conn: HttpURLConnection? = null
        try {
            conn = openConnection(urlString)
            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(body)
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    val abbr = item.optString("Cur_Abbreviation")
                    val curId = item.optInt("Cur_ID")
                    if (abbr.equals("USD", ignoreCase = true) || curId == 431) {
                        val officialRate = item.optDouble("Cur_OfficialRate", -1.0)
                        val scale = item.optDouble("Cur_Scale", 1.0)
                        val date = item.optString("Date", null)
                        if (officialRate > 0) {
                            val rate = officialRate / (if (scale > 0) scale else 1.0)
                            return CurrencyRateResult(rate = rate, source = "НБ РБ (nbrb.by)", date = date)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "fetchFromNbrbList $urlString error: ${e.message}")
        } finally {
            conn?.disconnect()
        }
        return null
    }

    private fun fetchFromFallback(urlString: String): CurrencyRateResult? {
        var conn: HttpURLConnection? = null
        try {
            conn = openConnection(urlString)
            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)

                if (json.has("rates")) {
                    val rates = json.getJSONObject("rates")
                    val byn = rates.optDouble("BYN", -1.0)
                    if (byn > 0) {
                        return CurrencyRateResult(rate = byn, source = "Курс BYN/USD")
                    }
                }

                if (json.has("usd")) {
                    val usd = json.getJSONObject("usd")
                    val byn = usd.optDouble("byn", -1.0)
                    if (byn > 0) {
                        return CurrencyRateResult(rate = byn, source = "Курс BYN/USD")
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "fetchFromFallback $urlString error: ${e.message}")
        } finally {
            conn?.disconnect()
        }
        return null
    }
}
