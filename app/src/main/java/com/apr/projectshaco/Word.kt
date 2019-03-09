package com.apr.projectshaco


import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey



@Entity(tableName = "word_table")
data class Word(@PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Int
                , @ColumnInfo(name = "word") val word: String
                , @ColumnInfo(name = "meaning") val meaning: String
                , @ColumnInfo(name = "synonyms") val synonyms: String)
