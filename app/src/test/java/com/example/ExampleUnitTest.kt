package com.example

import com.example.data.*
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testMonumentMaterialsCatalog() {
        val materials = MonumentCatalogData.materials
        assertTrue(materials.isNotEmpty())
        val gabbro = materials.find { it.id == "gabbro" }
        assertNotNull(gabbro)
        assertEquals(1.0, gabbro!!.priceMultiplier, 0.001)

        val dymovsky = materials.find { it.id == "dymovsky" }
        assertNotNull(dymovsky)
        assertTrue(dymovsky!!.priceMultiplier > 1.0)
    }

    @Test
    fun testMonumentSizePresets() {
        val presets = MonumentCatalogData.sizePresets
        assertTrue(presets.isNotEmpty())
        val standard = presets.find { it.id == "100x50x8" }
        assertNotNull(standard)
        assertEquals(100, standard!!.heightCm)
        assertEquals(50, standard.widthCm)
        assertEquals(8, standard.thicknessCm)
        assertTrue(standard.steleBasePrice > 0)
    }

    @Test
    fun testSteleVolumeAndMassCalculations() {
        val h = 100.0
        val w = 50.0
        val t = 8.0
        val volumeM3 = (h * w * t) / 1_000_000.0
        val massKg = volumeM3 * 2900.0

        assertEquals(0.04, volumeM3, 0.001)
        assertEquals(116.0, massKg, 1.0)
    }
}
