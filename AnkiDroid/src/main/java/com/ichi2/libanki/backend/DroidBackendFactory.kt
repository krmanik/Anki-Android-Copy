/*
 *  Copyright (c) 2020 David Allison <davidallisongithub@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation; either version 3 of the License, or (at your option) any later
 *  version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY
 *  WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.ichi2.libanki.backend

import android.system.Os
import androidx.annotation.VisibleForTesting
import com.ichi2.anki.AnkiDroidApp
import com.ichi2.anki.CrashReportService
import com.ichi2.libanki.Consts
import net.ankiweb.rsdroid.BackendFactory
import net.ankiweb.rsdroid.RustBackendFailedException
import net.ankiweb.rsdroid.RustCleanup
import timber.log.Timber

/** Responsible for selection of either the Rust or Java-based backend  */
object DroidBackendFactory {
    @JvmStatic
    private var sBackendForTesting: DroidBackend? = null

    /**
     * Obtains an instance of a [DroidBackend].
     * Each call to this method will generate a separate instance which can handle a new Anki collection
     */
    @JvmStatic
    @RustCleanup("Change back to a constant SYNC_VER")
    fun getInstance(useBackend: Boolean): DroidBackend {
        // Prevent sqlite throwing error 6410 due to the lack of /tmp
        val dir = AnkiDroidApp.getInstance().applicationContext.cacheDir
        Os.setenv("TMPDIR", dir.path, false)
        if (sBackendForTesting != null) {
            return sBackendForTesting!!
        }
        var backendFactory: BackendFactory? = null
        if (useBackend) {
            try {
                backendFactory = BackendFactory.createInstance()
            } catch (e: RustBackendFailedException) {
                Timber.w(e, "Rust backend failed to load - falling back to Java")
                CrashReportService.sendExceptionReport(e, "DroidBackendFactory::getInstance")
            }
        }
        val instance = getInstance(backendFactory)
        // Update the Sync version if we can load the Rust
        Consts.SYNC_VER = if (backendFactory == null) 9 else 10
        return instance
    }

    @JvmStatic
    private fun getInstance(backendFactory: BackendFactory?): DroidBackend {
        if (backendFactory == null) {
            return JavaDroidBackend()
        }
        return if (AnkiDroidApp.TESTING_USE_V16_BACKEND) {
            RustDroidV16Backend(backendFactory)
        } else {
            RustDroidBackend(backendFactory)
        }
    }

    @JvmStatic
    @VisibleForTesting
    fun setOverride(backend: DroidBackend?) {
        sBackendForTesting = backend
    }
}
