package com.apr.projectshaco;

import java.lang.System;

@kotlin.Metadata(mv = {1, 1, 13}, bv = {1, 0, 3}, k = 1, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0002\b\u0003\u0018\u00002\u00020\u0001B\u0015\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\u0019\u0010\u000f\u001a\u00020\u00102\u0006\u0010\u0011\u001a\u00020\nH\u0087@\u00f8\u0001\u0000\u00a2\u0006\u0002\u0010\u0012R&\u0010\u0007\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\n0\t0\bX\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u000b\u0010\f\"\u0004\b\r\u0010\u000eR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u0082\u0002\u0004\n\u0002\b\u0019\u00a8\u0006\u0013"}, d2 = {"Lcom/apr/projectshaco/WordRepository;", "", "wordDao", "Lcom/apr/projectshaco/WordDao;", "type", "", "(Lcom/apr/projectshaco/WordDao;I)V", "allWords", "Landroid/arch/lifecycle/LiveData;", "", "Lcom/apr/projectshaco/Word;", "getAllWords", "()Landroid/arch/lifecycle/LiveData;", "setAllWords", "(Landroid/arch/lifecycle/LiveData;)V", "insert", "", "word", "(Lcom/apr/projectshaco/Word;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
public final class WordRepository {
    @org.jetbrains.annotations.NotNull()
    private android.arch.lifecycle.LiveData<java.util.List<com.apr.projectshaco.Word>> allWords;
    private final com.apr.projectshaco.WordDao wordDao = null;
    
    @org.jetbrains.annotations.NotNull()
    public final android.arch.lifecycle.LiveData<java.util.List<com.apr.projectshaco.Word>> getAllWords() {
        return null;
    }
    
    public final void setAllWords(@org.jetbrains.annotations.NotNull()
    android.arch.lifecycle.LiveData<java.util.List<com.apr.projectshaco.Word>> p0) {
    }
    
    @org.jetbrains.annotations.Nullable()
    @android.support.annotation.WorkerThread()
    @kotlin.Suppress(names = {"RedundantSuspendModifier"})
    public final java.lang.Object insert(@org.jetbrains.annotations.NotNull()
    com.apr.projectshaco.Word word, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> p1) {
        return null;
    }
    
    public WordRepository(@org.jetbrains.annotations.NotNull()
    com.apr.projectshaco.WordDao wordDao, int type) {
        super();
    }
}