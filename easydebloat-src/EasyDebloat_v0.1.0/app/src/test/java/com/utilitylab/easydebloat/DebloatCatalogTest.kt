package com.utilitylab.easydebloat

import com.utilitylab.easydebloat.data.DebloatCatalog
import com.utilitylab.easydebloat.model.RiskLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DebloatCatalogTest {
    @Test fun systemUiIsProtected() {
        assertEquals(RiskLevel.DANGEROUS, DebloatCatalog.classify("com.android.systemui").risk)
        assertTrue(DebloatCatalog.isHardBlocked("com.android.systemui"))
    }

    @Test fun playServicesIsProtected() = assertEquals(RiskLevel.DANGEROUS, DebloatCatalog.classify("com.google.android.gms").risk)
    @Test fun securityCenterIsProtected() = assertEquals(RiskLevel.DANGEROUS, DebloatCatalog.classify("com.miui.securitycenter").risk)
    @Test fun miVideoIsSafe() = assertEquals(RiskLevel.SAFE, DebloatCatalog.classify("com.miui.videoplayer").risk)
    @Test fun miMusicIsSafe() = assertEquals(RiskLevel.SAFE, DebloatCatalog.classify("com.miui.player").risk)
    @Test fun unknownIsNeverSafe() = assertEquals(RiskLevel.UNKNOWN, DebloatCatalog.classify("com.unknown.vendor.service").risk)
}
