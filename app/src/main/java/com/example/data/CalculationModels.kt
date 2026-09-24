package com.example.data

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@JsonClass(generateAdapter = true)
data class CalculatedItemData(
    val priceItemId: Long? = null,
    val category: String,
    val name: String,
    val unit: String,
    val unitPrice: Double,
    val quantity: Double,
    val totalPrice: Double,
    val customNote: String = ""
) {
    val subtotal: Double
        get() = totalPrice
}

object FormatUtils {
    private val russianFormat = NumberFormat.getNumberInstance(Locale("ru", "RU")).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 0
    }

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("ru", "RU"))
    private val dateShortFormat = SimpleDateFormat("dd.MM.yyyy", Locale("ru", "RU"))

    fun formatPrice(amount: Double): String {
        return "${russianFormat.format(amount)} BYN"
    }

    fun formatRub(amount: Double): String {
        return formatPrice(amount)
    }

    fun formatByn(amount: Double): String {
        return formatPrice(amount)
    }

    fun formatBynAndUsd(amountByn: Double, usdRate: Double): String {
        val usdAmount = if (usdRate > 0) amountByn / usdRate else 0.0
        return "${formatPrice(amountByn)} (${russianFormat.format(usdAmount)} $)"
    }

    fun formatEur(amountEur: Double): String {
        return "${russianFormat.format(amountEur)} €"
    }

    fun formatBynAndEur(amountByn: Double, eurRate: Double): String {
        val eurAmount = if (eurRate > 0) amountByn / eurRate else 0.0
        return "${formatPrice(amountByn)} (${russianFormat.format(eurAmount)} €)"
    }

    fun formatNumber(value: Double): String {
        return russianFormat.format(value)
    }

    fun formatDate(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }

    fun formatDateShort(timestamp: Long): String {
        return dateShortFormat.format(Date(timestamp))
    }
}

object PriceFormatter {
    fun formatRub(amount: Double): String = FormatUtils.formatPrice(amount)
    fun formatNumber(value: Double): String = FormatUtils.formatNumber(value)
    fun formatWithUsd(amountByn: Double, usdRate: Double): String = FormatUtils.formatBynAndUsd(amountByn, usdRate)
    fun formatEur(amountEur: Double): String = FormatUtils.formatEur(amountEur)
    fun formatWithEur(amountByn: Double, eurRate: Double): String = FormatUtils.formatBynAndEur(amountByn, eurRate)
}

object OrderJsonAdapter {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val listType = Types.newParameterizedType(List::class.java, CalculatedItemData::class.java)
    private val adapter = moshi.adapter<List<CalculatedItemData>>(listType)

    fun toJson(items: List<CalculatedItemData>): String {
        return try {
            adapter.toJson(items)
        } catch (e: Exception) {
            "[]"
        }
    }

    fun fromJson(json: String): List<CalculatedItemData> {
        return try {
            adapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
