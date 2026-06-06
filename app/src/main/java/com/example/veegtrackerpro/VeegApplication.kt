package com.example.veegtrackerpro

import android.app.Application
import androidx.room.Room
import com.example.veegtrackerpro.data.local.VeegDatabase

import com.example.veegtrackerpro.data.local.init.AssetGpxImporter
import com.example.veegtrackerpro.data.repository.SyncRepository
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration

class VeegApplication : Application() {
    lateinit var database: VeegDatabase
        private set

    private val applicationScope = CoroutineScope(SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        Configuration.getInstance().userAgentValue = packageName

        if (BuildConfig.DEBUG) {
            deleteDatabase(VeegDatabase.DATABASE_NAME)
        }

        database = Room.databaseBuilder(
            this,
            VeegDatabase::class.java,
            VeegDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration().build()

        applicationScope.launch {
            AssetGpxImporter(this@VeegApplication, database).importFromAssets()
            SyncRepository(database.veegDao(), applicationScope).startSync()
        }
    }
}
