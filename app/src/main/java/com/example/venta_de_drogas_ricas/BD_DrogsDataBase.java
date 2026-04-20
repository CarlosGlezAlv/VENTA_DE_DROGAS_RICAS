package com.example.venta_de_drogas_ricas;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class BD_DrogsDataBase extends SQLiteOpenHelper {
   private static final String DB_NAME = "BD_Drogs";
   private static final int DB_VERSION = 2; // Incremented version

   public BD_DrogsDataBase(Context context) {
       super(context, DB_NAME, null, DB_VERSION);
   }

   @Override
    public void onCreate(SQLiteDatabase db) {
       db.execSQL("CREATE TABLE productos(id INTEGER PRIMARY KEY, nombre TEXT, descripcion TEXT, cantidad REAL, precio REAL)");
   }

   @Override
   public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
       db.execSQL("DROP TABLE IF EXISTS productos");
       onCreate(db);
   }
}
