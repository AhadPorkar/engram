package com.apr.projectshaco

import android.arch.lifecycle.ViewModel
import android.app.Application
import android.arch.lifecycle.ViewModelProvider


class MyViewModelFactory(private val mApplication: Application, private val mParam: Int) : ViewModelProvider.Factory {


    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return WordViewModel(mApplication, mParam) as T
    }
}