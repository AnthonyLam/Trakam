package com.trakam.trakam.db

import android.arch.persistence.room.*
import android.content.Context

@Entity(tableName = "people")
data class Person(
        @PrimaryKey
        var uuid: String,

        @ColumnInfo(name = "first_name")
        var firstName: String,

        @ColumnInfo(name = "last_name")
        var lastName: String,

        @ColumnInfo(name = "blacklisted")
        var blackListed: Int
)

@Dao
interface PeopleDao {
    @Query("SELECT * FROM people")
    fun getAll(): List<Person>

    @Query("SELECT * FROM people WHERE blacklisted = 1")
    fun getBlackList(): List<Person>

    @Query("SELECT * FROM people WHERE blacklisted = 0")
    fun getWhiteList(): List<Person>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg person: Person)

    @Query("DELETE FROM people WHERE first_name = :firstName AND last_name = :lastName")
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
                            AppDatabase::class.java, "main.db")
                            .build()
                }
            }
            return sInstance!!
        }
    }
}