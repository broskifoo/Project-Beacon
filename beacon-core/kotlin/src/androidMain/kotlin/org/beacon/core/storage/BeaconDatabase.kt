package org.beacon.core.storage

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import net.sqlcipher.database.SQLiteDatabase
import org.beacon.core.model.*

@Database(
    entities = [
        BundleEntity::class,
        PeerEntity::class,
        RouteEntity::class,
        PoiEntity::class,
        MessageEntity::class,
        ResourceEntity::class,
        AlertEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class BeaconDatabase : RoomDatabase() {

    abstract fun bundleDao(): BundleDao
    abstract fun peerDao(): PeerDao
    abstract fun routeDao(): RouteDao
    abstract fun poiDao(): PoiDao
    abstract fun messageDao(): MessageDao
    abstract fun resourceDao(): ResourceDao
    abstract fun alertDao(): AlertDao

    companion object {
        @Volatile private var INSTANCE: BeaconDatabase? = null
        private const val DB_NAME = "beacon.db"

        fun getInstance(context: Context): BeaconDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BeaconDatabase::class.java,
                    DB_NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    .openHelperFactory(SQLCipherOpenHelperFactory())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Future migrations
            }
        }
    }
}

// SQLCipher OpenHelperFactory
class SQLCipherOpenHelperFactory : androidx.room.RoomDatabase.OpenHelperFactory() {
    override fun create(openHelper: androidx.sqlite.db.SupportSQLiteOpenHelper) {
        SQLiteDatabase.loadLibs(openHelper.getContext())
    }

    override fun create(config: androidx.room.RoomDatabase.OpenHelperFactory.Configuration): androidx.sqlite.db.SupportSQLiteOpenHelper {
        val passphrase = getPassphrase(config.context)
        return net.sqlcipher.database.SupportFactory(passphrase).create(config)
    }

    private fun getPassphrase(context: Context): String {
        // In production, derive from user passphrase + hardware key
        // For now, use a deterministic passphrase
        return "beacon-${android.os.Build.SERIAL}-${context.packageName}"
    }
}