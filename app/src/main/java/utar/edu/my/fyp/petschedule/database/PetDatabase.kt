//package utar.edu.my.fyp.petschedule.database
//
//import android.content.Context
//import androidx.room.Database
//import androidx.room.Room
//import androidx.room.RoomDatabase
//import androidx.room.TypeConverters
//import androidx.room.migration.Migration
//import androidx.sqlite.db.SupportSQLiteDatabase
//import utar.edu.my.fyp.petschedule.Appointment
//import utar.edu.my.fyp.petschedule.AppointmentDao
//import utar.edu.my.fyp.petschedule.Medicine
//import utar.edu.my.fyp.petschedule.MedicineDao
//import utar.edu.my.fyp.petschedule.Pet
//import utar.edu.my.fyp.petschedule.Vaccination
//import utar.edu.my.fyp.petschedule.VaccinationDao
//
//@Database(
//    entities = [
//        Pet::class,
//        Medicine::class,
//        Vaccination::class,
//        Appointment::class
//
//    ],
//    version = 4,
//    exportSchema = false
//)
//@TypeConverters(DateConverter::class)
//abstract class PetDatabase : RoomDatabase() {
//
//    abstract fun petDao(): PetDao
//    abstract fun medicineDao(): MedicineDao
//    abstract fun vaccinationDao(): VaccinationDao
//    abstract fun appointmentDao(): AppointmentDao
//
//    companion object {
//        @Volatile private var INSTANCE: PetDatabase? = null
//
//        // Keep your old forward migration if you had v2 → v3 in the wild
//        private val MIGRATION_2_3 = object : Migration(2, 3) {
//            override fun migrate(db: SupportSQLiteDatabase) {
//                // Example you previously used; keep if devices might be at v2.
//                // Safe if column already exists (will throw otherwise). If you never had v2 in the wild, you can remove this.
//                try {
//                    db.execSQL("ALTER TABLE pets ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
//                } catch (_: Exception) {
//                    // column might already exist — ignore
//                }
//                // IMPORTANT: do NOT touch medicine_doses here anymore
//            }
//        }
//
//        // New: clean up the now-unused table/index from older installs
//        private val MIGRATION_3_4 = object : Migration(3, 4) {
//            override fun migrate(db: SupportSQLiteDatabase) {
//                // Drop index first (if it existed), then drop the table
//                db.execSQL("DROP INDEX IF EXISTS index_medicine_doses_medicineId")
//                db.execSQL("DROP TABLE IF EXISTS medicine_doses")
//            }
//        }
//
//        fun getDatabase(context: Context): PetDatabase =
//            INSTANCE ?: synchronized(this) {
//                INSTANCE ?: Room.databaseBuilder(
//                    context.applicationContext,
//                    PetDatabase::class.java,
//                    "pet_database"
//                )
//                    // Register forward migrations that may be needed on users’ devices
//                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
//                    // Dev convenience so an accidental older APK doesn’t crash
//                    .fallbackToDestructiveMigrationOnDowngrade()
//                    .build()
//                    .also { INSTANCE = it }
//            }
//    }
//}
//


package utar.edu.my.fyp.petschedule.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import utar.edu.my.fyp.petschedule.Appointment
import utar.edu.my.fyp.petschedule.AppointmentDao
import utar.edu.my.fyp.petschedule.Medicine
import utar.edu.my.fyp.petschedule.MedicineDao
import utar.edu.my.fyp.petschedule.Pet
import utar.edu.my.fyp.petschedule.Vaccination
import utar.edu.my.fyp.petschedule.VaccinationDao

@Database(
    entities = [
        Pet::class,
        Medicine::class,
        Vaccination::class,
        Appointment::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class PetDatabase : RoomDatabase() {

    abstract fun petDao(): PetDao
    abstract fun medicineDao(): MedicineDao
    abstract fun vaccinationDao(): VaccinationDao
    abstract fun appointmentDao(): AppointmentDao

    companion object {
        @Volatile private var INSTANCE: PetDatabase? = null

        // Keep your old forward migration if you had v2 → v3 in the wild
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Safe-guard: add userId if coming from very old installs
                try {
                    db.execSQL("ALTER TABLE pets ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
                } catch (_: Exception) {
                    // Column might already exist — ignore
                }
            }
        }

        // Clean up the now-unused table/index from older installs
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP INDEX IF EXISTS index_medicine_doses_medicineId")
                db.execSQL("DROP TABLE IF EXISTS medicine_doses")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1) Create a new table without the 'age' column
                db.execSQL("""
                    CREATE TABLE pets_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        breed TEXT NOT NULL,
                        gender TEXT NOT NULL,
                        dateOfBirth TEXT NOT NULL,
                        petType TEXT NOT NULL,
                        imagePath TEXT,
                        userId TEXT NOT NULL
                    )
                """.trimIndent())

                // 2) Copy data from old table (exclude 'age')
                db.execSQL("""
                    INSERT INTO pets_new (id, name, breed, gender, dateOfBirth, petType, imagePath, userId)
                    SELECT id, name, breed, gender, dateOfBirth, petType, imagePath, userId
                    FROM pets
                """.trimIndent())

                // 3) Replace the old table
                db.execSQL("DROP TABLE pets")
                db.execSQL("ALTER TABLE pets_new RENAME TO pets")
            }
        }

        fun getDatabase(context: Context): PetDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PetDatabase::class.java,
                    "pet_database"
                )
                    // ⬇️ Register all forward migrations, including the new 4→5
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}

