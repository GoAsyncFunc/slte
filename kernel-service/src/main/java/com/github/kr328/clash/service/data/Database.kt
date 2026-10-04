package com.github.kr328.clash.service.data

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.github.kr328.clash.common.Global
import com.github.kr328.clash.service.data.migrations.LEGACY_MIGRATION
import com.github.kr328.clash.service.data.migrations.MIGRATIONS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.room.Database as DB

@DB(
    version = 2,
    entities = [Imported::class, Pending::class, Selection::class],
    exportSchema = false,
)
abstract class Database : RoomDatabase() {
    abstract fun openImportedDao(): ImportedDao
    abstract fun openPendingDao(): PendingDao
    abstract fun openSelectionProxyDao(): SelectionDao

    companion object {
        // 进程内常驻单例：SoftReference 会在内存压力下丢弃已打开的数据库，
        // 导致下一次访问整套重新 open，没有任何收益；Room 自己管理 WAL 与连接池
        @Volatile
        private var instance: Database? = null

        val database: Database
            @Synchronized get() = instance ?: open(Global.application).also { instance = it }

        private fun open(context: Context): Database {
            return Room.databaseBuilder(
                context.applicationContext,
                Database::class.java,
                "profiles"
            ).addMigrations(*MIGRATIONS).build()
        }

        init {
            Global.launch(Dispatchers.IO) {
                LEGACY_MIGRATION(Global.application)
            }
        }
    }
}
