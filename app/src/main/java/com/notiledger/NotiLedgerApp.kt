package com.notiledger

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.notiledger.data.backup.BackupManager
import com.notiledger.data.db.NotiDatabase
import com.notiledger.data.repository.NotiRepository
import com.notiledger.data.repository.SettingsRepository

class NotiLedgerApp : Application() {

    companion object {
        const val CHANNEL_ID = "notiledger_service"
    }

    lateinit var database: NotiDatabase
        private set

    lateinit var repository: NotiRepository
        private set

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var backupManager: BackupManager
        private set

    override fun onCreate() {
        super.onCreate()

        database = NotiDatabase.getInstance(this)
        repository = NotiRepository(
            recordDao = database.notiRecordDao(),
            filterDao = database.appFilterDao()
        )
        settingsRepository = SettingsRepository(this)
        backupManager = BackupManager(this, repository)

        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_desc)
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
