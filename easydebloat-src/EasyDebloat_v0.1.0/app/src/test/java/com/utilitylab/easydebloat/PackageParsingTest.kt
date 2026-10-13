package com.utilitylab.easydebloat

import com.utilitylab.easydebloat.model.PackageState
import com.utilitylab.easydebloat.util.PackageListParser
import com.utilitylab.easydebloat.util.PackageStateResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PackageParsingTest {
    @Test fun parsesPmOutput() {
        val parsed = PackageListParser.parse("package:com.one\npackage:com.two\nnoise\n")
        assertEquals(setOf("com.one", "com.two"), parsed)
    }

    @Test fun parsesPmOutputWithInstallerSuffix() {
        val parsed = PackageListParser.parse("package:com.one=/system/app/One.apk\n")
        assertTrue("com.one" in parsed)
    }

    @Test fun removedStateWinsWhenNotInstalled() {
        assertEquals(PackageState.REMOVED_FOR_USER, PackageStateResolver.resolve("com.one", emptySet(), setOf("com.one")))
    }

    @Test fun disabledState() {
        assertEquals(PackageState.DISABLED, PackageStateResolver.resolve("com.one", setOf("com.one"), setOf("com.one")))
    }

    @Test fun installedState() {
        assertEquals(PackageState.INSTALLED, PackageStateResolver.resolve("com.one", setOf("com.one"), emptySet()))
    }
}
