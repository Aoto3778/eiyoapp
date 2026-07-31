package jp.aoto.eiyoapp

import jp.aoto.eiyoapp.data.Nutrients
import org.junit.Assert.assertEquals
import org.junit.Test

class NutrientsTest {
    @Test fun scalesAndAddsAllNutrients() {
        val one = Nutrients(kcal=180.0, protein=6.0, sugar=3.0, water=10.0)
        val total = one * 1.5 + Nutrients(protein=2.0)
        assertEquals(270.0, total.kcal, 0.001)
        assertEquals(11.0, total.protein, 0.001)
        assertEquals(4.5, total.sugar, 0.001)
        assertEquals(15.0, total.water, 0.001)
    }
}
