package com.utilitylab.easydebloat

import com.utilitylab.easydebloat.util.PackageNameValidator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PackageNameValidatorTest {
    @Test fun acceptsNormalPackage() = assertTrue(PackageNameValidator.isValid("com.valid.package"))
    @Test fun rejectsShellSeparator() = assertFalse(PackageNameValidator.isValid("com.example; reboot"))
    @Test fun rejectsCommandSubstitution() = assertFalse(PackageNameValidator.isValid("$(reboot)"))
    @Test fun rejectsWhitespace() = assertFalse(PackageNameValidator.isValid("com.bad package"))
}
