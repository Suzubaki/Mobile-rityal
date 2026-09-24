package com.example

import com.example.data.*
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
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

    @Test
    fun testShelfCalculations() {
        val l = 70.0
        val w = 15.0
        val t = 5.0
        val volumeM3 = (l * w * t) / 1_000_000.0
        val massKg = volumeM3 * 2900.0

        assertEquals(0.00525, volumeM3, 0.0001)
        assertEquals(15.225, massKg, 0.01)
    }

    @Test
    fun testTwoPartPlinthWithDifferentStones() {
        // Part 1: 60 x 20 x 15 cm in Shanxi Black (price 3500 BYN/m³)
        val l1 = 60.0
        val w1 = 20.0
        val h1 = 15.0
        val v1 = (l1 * w1 * h1) / 1_000_000.0 // 0.018 m³
        val mass1 = v1 * 2900.0 // 52.2 kg
        val price1 = v1 * 3500.0 // 63.0 BYN

        assertEquals(0.018, v1, 0.0001)
        assertEquals(52.2, mass1, 0.01)
        assertEquals(63.0, price1, 0.01)

        // Part 2: 40 x 20 x 15 cm in Aurora / Red Granite (price 4200 BYN/m³)
        val l2 = 40.0
        val w2 = 20.0
        val h2 = 15.0
        val v2 = (l2 * w2 * h2) / 1_000_000.0 // 0.012 m³
        val mass2 = v2 * 2900.0 // 34.8 kg
        val price2 = v2 * 4200.0 // 50.4 BYN

        assertEquals(0.012, v2, 0.0001)
        assertEquals(34.8, mass2, 0.01)
        assertEquals(50.4, price2, 0.01)

        val totalV = v1 + v2
        val totalMass = mass1 + mass2
        val totalPrice = price1 + price2

        assertEquals(0.030, totalV, 0.0001)
        assertEquals(87.0, totalMass, 0.01)
        assertEquals(113.4, totalPrice, 0.01)
    }

    @Test
    fun testFlowerbedModesCalculation() {
        val fbL = 100.0
        val fbW = 60.0
        val fbT = 8.0
        val fbBarW = 5.0
        val stonePricePerM3 = 3000.0

        // 1) OPEN_JOINT: 2 * L * T * barW + (W - 2*barW) * T * barW
        val longBarsCm3 = 2.0 * fbL * fbT * fbBarW // 2 * 100 * 8 * 5 = 8000 cm3
        val innerCrossW = fbW - 2.0 * fbBarW // 60 - 10 = 50 cm
        val crossBarJointCm3 = innerCrossW * fbT * fbBarW // 50 * 8 * 5 = 2000 cm3
        val jointTotalCm3 = longBarsCm3 + crossBarJointCm3 // 10000 cm3 = 0.01 m3
        val jointVolM3 = jointTotalCm3 / 1_000_000.0

        assertEquals(0.010, jointVolM3, 0.0001)
        assertEquals(30.0, jointVolM3 * stonePricePerM3, 0.01)

        // 2) OPEN_SUM: 2 * L * T * barW + W * T * barW
        val sumTotalCm3 = (2.0 * fbL * fbT * fbBarW) + (fbW * fbT * fbBarW) // 8000 + 2400 = 10400 cm3 = 0.0104 m3
        val sumVolM3 = sumTotalCm3 / 1_000_000.0
        assertEquals(0.0104, sumVolM3, 0.0001)

        // 3) CLOSED_4: 2 * L * T * barW + 2 * (W - 2*barW) * T * barW
        val closed4Cm3 = longBarsCm3 + (2.0 * innerCrossW * fbT * fbBarW) // 8000 + 4000 = 12000 cm3 = 0.012 m3
        val closed4VolM3 = closed4Cm3 / 1_000_000.0
        assertEquals(0.012, closed4VolM3, 0.0001)
    }

    @Test
    fun testTwoPartTombstoneSlabWithDifferentStones() {
        // Part 1 (Shanxi Black): 100 x 30 x 5 cm @ 3500 BYN/m³
        val l1 = 100.0
        val w1 = 30.0
        val t1 = 5.0
        val v1 = (l1 * w1 * t1) / 1_000_000.0 // 0.015 m³
        val mass1 = v1 * 2900.0 // 43.5 kg
        val price1 = v1 * 3500.0 // 52.5 BYN

        assertEquals(0.015, v1, 0.0001)
        assertEquals(43.5, mass1, 0.01)
        assertEquals(52.5, price1, 0.01)

        // Part 2 (Gabbro/Karelia): 100 x 30 x 5 cm @ 4000 BYN/m³
        val l2 = 100.0
        val w2 = 30.0
        val t2 = 5.0
        val v2 = (l2 * w2 * t2) / 1_000_000.0 // 0.015 m³
        val mass2 = v2 * 2900.0 // 43.5 kg
        val price2 = v2 * 4000.0 // 60.0 BYN

        assertEquals(0.015, v2, 0.0001)
        assertEquals(43.5, mass2, 0.01)
        assertEquals(60.0, price2, 0.01)

        val totalSlabV = v1 + v2 // 0.030 m³
        val totalSlabMass = mass1 + mass2 // 87.0 kg
        val totalSlabPrice = price1 + price2 // 112.5 BYN

        assertEquals(0.030, totalSlabV, 0.0001)
        assertEquals(87.0, totalSlabMass, 0.01)
        assertEquals(112.5, totalSlabPrice, 0.01)
    }
}
