// app/src/main/java/com/quantum_prof/vtscansuite/VTExpressApplication.kt
package com.quantum_prof.vtscansuite

import android.app.Application
import com.quantum_prof.vtscansuite.data.repository.PrefsRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class VTExpressApplication : Application() {

    @Inject
    lateinit var prefsRepository: PrefsRepository

    override fun onCreate() {
        super.onCreate()
        // Einmalige Migration: ein noch im Klartext abgelegter API-Key (Versionen <= 2.2)
        // wird in die Keystore-verschlüsselte Form überführt und im Klartext gelöscht.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching { prefsRepository.migrateLegacyKey() }
        }
    }
}
