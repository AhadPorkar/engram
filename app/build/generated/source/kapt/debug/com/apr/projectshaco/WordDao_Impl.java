package com.apr.projectshaco;

import android.arch.lifecycle.ComputableLiveData;
import android.arch.lifecycle.LiveData;
import android.arch.persistence.db.SupportSQLiteStatement;
import android.arch.persistence.room.EntityInsertionAdapter;
import android.arch.persistence.room.InvalidationTracker.Observer;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.RoomSQLiteQuery;
import android.arch.persistence.room.SharedSQLiteStatement;
import android.database.Cursor;
import android.support.annotation.NonNull;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@SuppressWarnings("unchecked")
public class WordDao_Impl implements WordDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter __insertionAdapterOfWord;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public WordDao_Impl(RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfWord = new EntityInsertionAdapter<Word>(__db) {
      @Override
      public String createQuery() {
        return "INSERT OR ABORT INTO `word_table`(`id`,`word`,`meaning`,`synonyms`) VALUES (nullif(?, 0),?,?,?)";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, Word value) {
        stmt.bindLong(1, value.getId());
        if (value.getWord() == null) {
          stmt.bindNull(2);
        } else {
          stmt.bindString(2, value.getWord());
        }
        if (value.getMeaning() == null) {
          stmt.bindNull(3);
        } else {
          stmt.bindString(3, value.getMeaning());
        }
        if (value.getSynonyms() == null) {
          stmt.bindNull(4);
        } else {
          stmt.bindString(4, value.getSynonyms());
        }
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      public String createQuery() {
        final String _query = "DELETE FROM word_table";
        return _query;
      }
    };
  }

  @Override
  public void insert(Word word) {
    __db.beginTransaction();
    try {
      __insertionAdapterOfWord.insert(word);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void deleteAll() {
    final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAll.acquire();
    __db.beginTransaction();
    try {
      _stmt.executeUpdateDelete();
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
      __preparedStmtOfDeleteAll.release(_stmt);
    }
  }

  @Override
  public LiveData<List<Word>> getTopToEndWords() {
    final String _sql = "SELECT * from word_table ORDER BY id ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return new ComputableLiveData<List<Word>>() {
      private Observer _observer;

      @Override
      protected List<Word> compute() {
        if (_observer == null) {
          _observer = new Observer("word_table") {
            @Override
            public void onInvalidated(@NonNull Set<String> tables) {
              invalidate();
            }
          };
          __db.getInvalidationTracker().addWeakObserver(_observer);
        }
        final Cursor _cursor = __db.query(_statement);
        try {
          final int _cursorIndexOfId = _cursor.getColumnIndexOrThrow("id");
          final int _cursorIndexOfWord = _cursor.getColumnIndexOrThrow("word");
          final int _cursorIndexOfMeaning = _cursor.getColumnIndexOrThrow("meaning");
          final int _cursorIndexOfSynonyms = _cursor.getColumnIndexOrThrow("synonyms");
          final List<Word> _result = new ArrayList<Word>(_cursor.getCount());
          while(_cursor.moveToNext()) {
            final Word _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpWord;
            _tmpWord = _cursor.getString(_cursorIndexOfWord);
            final String _tmpMeaning;
            _tmpMeaning = _cursor.getString(_cursorIndexOfMeaning);
            final String _tmpSynonyms;
            _tmpSynonyms = _cursor.getString(_cursorIndexOfSynonyms);
            _item = new Word(_tmpId,_tmpWord,_tmpMeaning,_tmpSynonyms);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    }.getLiveData();
  }

  @Override
  public LiveData<List<Word>> getEndToTopWords() {
    final String _sql = "SELECT * from word_table ORDER BY id DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return new ComputableLiveData<List<Word>>() {
      private Observer _observer;

      @Override
      protected List<Word> compute() {
        if (_observer == null) {
          _observer = new Observer("word_table") {
            @Override
            public void onInvalidated(@NonNull Set<String> tables) {
              invalidate();
            }
          };
          __db.getInvalidationTracker().addWeakObserver(_observer);
        }
        final Cursor _cursor = __db.query(_statement);
        try {
          final int _cursorIndexOfId = _cursor.getColumnIndexOrThrow("id");
          final int _cursorIndexOfWord = _cursor.getColumnIndexOrThrow("word");
          final int _cursorIndexOfMeaning = _cursor.getColumnIndexOrThrow("meaning");
          final int _cursorIndexOfSynonyms = _cursor.getColumnIndexOrThrow("synonyms");
          final List<Word> _result = new ArrayList<Word>(_cursor.getCount());
          while(_cursor.moveToNext()) {
            final Word _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpWord;
            _tmpWord = _cursor.getString(_cursorIndexOfWord);
            final String _tmpMeaning;
            _tmpMeaning = _cursor.getString(_cursorIndexOfMeaning);
            final String _tmpSynonyms;
            _tmpSynonyms = _cursor.getString(_cursorIndexOfSynonyms);
            _item = new Word(_tmpId,_tmpWord,_tmpMeaning,_tmpSynonyms);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    }.getLiveData();
  }

  @Override
  public LiveData<List<Word>> getRandomWords() {
    final String _sql = "SELECT * from word_table ORDER BY RANDOM()";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return new ComputableLiveData<List<Word>>() {
      private Observer _observer;

      @Override
      protected List<Word> compute() {
        if (_observer == null) {
          _observer = new Observer("word_table") {
            @Override
            public void onInvalidated(@NonNull Set<String> tables) {
              invalidate();
            }
          };
          __db.getInvalidationTracker().addWeakObserver(_observer);
        }
        final Cursor _cursor = __db.query(_statement);
        try {
          final int _cursorIndexOfId = _cursor.getColumnIndexOrThrow("id");
          final int _cursorIndexOfWord = _cursor.getColumnIndexOrThrow("word");
          final int _cursorIndexOfMeaning = _cursor.getColumnIndexOrThrow("meaning");
          final int _cursorIndexOfSynonyms = _cursor.getColumnIndexOrThrow("synonyms");
          final List<Word> _result = new ArrayList<Word>(_cursor.getCount());
          while(_cursor.moveToNext()) {
            final Word _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpWord;
            _tmpWord = _cursor.getString(_cursorIndexOfWord);
            final String _tmpMeaning;
            _tmpMeaning = _cursor.getString(_cursorIndexOfMeaning);
            final String _tmpSynonyms;
            _tmpSynonyms = _cursor.getString(_cursorIndexOfSynonyms);
            _item = new Word(_tmpId,_tmpWord,_tmpMeaning,_tmpSynonyms);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    }.getLiveData();
  }
}
