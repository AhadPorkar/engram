package com.apr.projectshaco


import android.arch.lifecycle.LiveData
import android.support.annotation.WorkerThread


class WordRepository(private val wordDao: WordDao,type :Int) {


    var allWords: LiveData<List<Word>> = (
            when(type){
                0 ->  wordDao.getRandomWords()
                1 ->  wordDao.getTopToEndWords()
                2 ->  wordDao.getEndToTopWords()
                3 ->  wordDao.getTopToEndWords()
                4 ->  wordDao.getTopToEndWords()
                else -> wordDao.getTopToEndWords()
            }

          )





    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun insert(word: Word) {
        wordDao.insert(word)
    }
}
