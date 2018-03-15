package com.trakam.trakam.db

import android.arch.persistence.room.*
import android.content.Context

@Entity(tableName = "blacklist")
data class Person(
        @PrimaryKey(autoGenerate = true)
        var id: Int = 0,

        @ColumnInfo(name = "first_name")
        var firstName: String,

        @ColumnInfo(name = "last_name")
        var lastName: String
)

@Dao
interface PeopleDao {
    @Query("SELECT * FROM blacklist")
    fun getAll(): List<Person>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg person: Person)

    @Query("DELETE FROM blacklist WHERE first_name = :firstName AND last_name = :lastName")
    fun delete(firstName: String, lastName: String)
}

@Database(entities = [(Person::class)], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun peopleDao(): PeopleDao

    companion object {
        private var sInstance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            if (sInstance == null) {
                synchronized(AppDatabase::class) {
                    sInstance = Room.databaseBuilder(context.applicationContext,
                            AppDatabase::class.java, "blacklist.db")
                            .build()
                }
            }
            return sInstance!!
        }
    }
}