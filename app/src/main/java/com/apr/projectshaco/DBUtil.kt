package com.apr.projectshaco


import android.content.Context
import android.util.Log
import java.io.File
import android.os.Environment
import java.lang.Exception
import java.lang.ref.WeakReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.apache.commons.io.FileUtils
import kotlin.coroutines.CoroutineContext
import android.content.Intent




class DBUtil(context: Context){

    private var mContext: WeakReference<Context> = WeakReference(context)


    private var parentJob = Job()

    private val coroutineContext: CoroutineContext
        get() = parentJob + Dispatchers.Main
    private val scope = CoroutineScope(coroutineContext)
    fun SaveDB(){
        val context_lazy = mContext.get()

            WordRoomDatabase.getDatabase(context_lazy!!,scope).close()

            val db = context_lazy.getDatabasePath("word_database")


            val db2 = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "word_database")

            var success = true
            try {
                FileUtils.copyFile(db, db2)

            } catch (e: Exception) {
                success = false;
                Log.e("SAVEDB", e.toString())
            }

        if(success)
        {
            val intent = Intent(context_lazy, MainActivity::class.java)
            context_lazy.startActivity(intent)
            System.exit(0)
        }

    }

}
