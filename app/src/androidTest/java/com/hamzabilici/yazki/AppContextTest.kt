package com.hamzabilici.yazki

import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppContextTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun packageName() {
        assertEquals("com.hamzabilici.yazki", context.packageName)
    }

    /** Gizlilik vaadinin koruması: hiçbir bağımlılık internet izni eklememeli. */
    @Test
    fun noInternetPermission() {
        val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        val permissions = info.requestedPermissions.orEmpty().toList()
        assertFalse(permissions.toString(), "android.permission.INTERNET" in permissions)
    }
}
