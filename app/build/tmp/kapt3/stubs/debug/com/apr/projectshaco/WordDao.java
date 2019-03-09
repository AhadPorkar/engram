package com.apr.projectshaco;

import java.lang.System;

@android.arch.persistence.room.Dao()
@kotlin.Metadata(mv = {1, 1, 13}, bv = {1, 0, 3}, k = 1, d1 = {"\u0000 \n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0005\bg\u0018\u00002\u00020\u0001J\b\u0010\u0002\u001a\u00020\u0003H\'J\u0014\u0010\u0004\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00070\u00060\u0005H\'J\u0014\u0010\b\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00070\u00060\u0005H\'J\u0014\u0010\t\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00070\u00060\u0005H\'J\u0010\u0010\n\u001a\u00020\u00032\u0006\u0010\u000b\u001a\u00020\u0007H\'\u00a8\u0006\f"}, d2 = {"Lcom/apr/projectshaco/WordDao;", "", "deleteAll", "", "getEndToTopWords", "Landroid/arch/lifecycle/LiveData;", "", "Lcom/apr/projectshaco/Word;", "getRandomWords", "getTopToEndWords", "insert", "word", "app_debug"})
public abstract interface WordDao {
    
    @org.jetbrains.annotations.NotNull()
    @android.arch.persistence.room.Query(value = "SELECT * from word_table ORDER BY id ASC")
    public abstract android.arch.lifecycle.LiveData<java.util.List<com.apr.projectshaco.Word>> getTopToEndWords();
    
    @org.jetbrains.annotations.NotNull()
    @android.arch.persistence.room.Query(value = "SELECT * from word_table ORDER BY id DESC")
    public abstract android.arch.lifecycle.LiveData<java.util.List<com.apr.projectshaco.Word>> getEndToTopWords();
    
    @org.jetbrains.annotations.NotNull()
    @android.arch.persistence.room.Query(value = "SELECT * from word_table ORDER BY RANDOM()")
    public abstract android.arch.lifecycle.LiveData<java.util.List<com.apr.projectshaco.Word>> getRandomWords();
    
    @android.arch.persistence.room.Insert()
    public abstract void insert(@org.jetbrains.annotations.NotNull()
    com.apr.projectshaco.Word word);
    
    @android.arch.persistence.room.Query(value = "DELETE FROM word_table")
    public abstract void deleteAll();
}