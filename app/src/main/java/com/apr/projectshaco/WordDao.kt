package com.apr.projectshaco


import android.arch.lifecycle.LiveData
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query


@Dao
interface WordDao {


    @Query("SELECT * from word_table ORDER BY id ASC")
    fun getTopToEndWords(): LiveData<List<Word>>

    @Query("SELECT * from word_table ORDER BY id DESC")
    fun getEndToTopWords(): LiveData<List<Word>>

    @Query("SELECT * from word_table ORDER BY RANDOM()")
    fun getRandomWords(): LiveData<List<Word>>

    @Insert
    fun insert(word: Word)

    @Query("DELETE FROM word_table")
    fun deleteAll()
}
