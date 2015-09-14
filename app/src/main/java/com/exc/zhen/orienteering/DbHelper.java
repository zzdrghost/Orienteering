package com.exc.zhen.orienteering;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by ZHEN on 2015/9/7 0007.
 * 自定义SQLiteOpenHelper类
 */
public class DbHelper extends SQLiteOpenHelper {
    private final static int dbVersion = 1;

    /**构造函数
     *
     * */
    DbHelper(Context context, String dbName){
        //CursorFactory设置为null,使用默认值
        super(context,dbName,null,dbVersion);
    }

    /**
     * Called when the database is created for the first time. This is where the
     * creation of tables and the initial population of the tables should happen.
     *
     * @param db The database.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        //第一次建库时创建mission表和point表
        db.execSQL("CREATE TABLE IF NOT EXISTS mission " +
                "(mission_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT, " +
                "state TEXT, " +
                "limit_time TEXT, " +
                "start_time TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS ms_point " +
                "(mission_id INTEGER, " +
                "order_num INTEGER, " +
                "state TEXT, " +
                "latitude REAL, " +
                "longitude REAL, " +
                "height REAL, " +
                "question TEXT, " +
                "answer TEXT, " +
                "img_address TEXT, " +
                "orientation REAL)");

    }

    /**
     * Called when the database needs to be upgraded. The implementation
     * should use this method to drop tables, add tables, or do anything else it
     * needs to upgrade to the new schema version.
     * <p/>
     * <p>
     * The SQLite ALTER TABLE documentation can be found
     * <a href="http://sqlite.org/lang_altertable.html">here</a>. If you add new columns
     * you can use ALTER TABLE to insert them into a live table. If you rename or remove columns
     * you can use ALTER TABLE to rename the old table, then create the new table and then
     * populate the new table with the contents of the old table.
     * </p><p>
     * This method executes within a transaction.  If an exception is thrown, all changes
     * will automatically be rolled back.
     * </p>
     *
     * @param db         The database.
     * @param oldVersion The old database version.
     * @param newVersion The new database version.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
