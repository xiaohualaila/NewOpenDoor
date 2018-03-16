package ug.newpoendoor.greendaodemo.greendao.gen;

import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;

import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.Property;
import org.greenrobot.greendao.internal.DaoConfig;
import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.database.DatabaseStatement;

import ug.newopendoor.activity.bean.WhiteList;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.
/** 
 * DAO for table "WHITE_LIST".
*/
public class WhiteListDao extends AbstractDao<WhiteList, Long> {

    public static final String TABLENAME = "WHITE_LIST";

    /**
     * Properties of entity WhiteList.<br/>
     * Can be used for QueryBuilder and for referencing column names.
     */
    public static class Properties {
        public final static Property _id = new Property(0, Long.class, "_id", true, "_id");
        public final static Property Xin_id = new Property(1, String.class, "xin_id", false, "XIN_ID");
        public final static Property Name = new Property(2, String.class, "name", false, "NAME");
        public final static Property Company = new Property(3, String.class, "company", false, "COMPANY");
    }


    public WhiteListDao(DaoConfig config) {
        super(config);
    }
    
    public WhiteListDao(DaoConfig config, DaoSession daoSession) {
        super(config, daoSession);
    }

    /** Creates the underlying database table. */
    public static void createTable(Database db, boolean ifNotExists) {
        String constraint = ifNotExists? "IF NOT EXISTS ": "";
        db.execSQL("CREATE TABLE " + constraint + "\"WHITE_LIST\" (" + //
                "\"_id\" INTEGER PRIMARY KEY AUTOINCREMENT ," + // 0: _id
                "\"XIN_ID\" TEXT," + // 1: xin_id
                "\"NAME\" TEXT," + // 2: name
                "\"COMPANY\" TEXT);"); // 3: company
    }

    /** Drops the underlying database table. */
    public static void dropTable(Database db, boolean ifExists) {
        String sql = "DROP TABLE " + (ifExists ? "IF EXISTS " : "") + "\"WHITE_LIST\"";
        db.execSQL(sql);
    }

    @Override
    protected final void bindValues(DatabaseStatement stmt, WhiteList entity) {
        stmt.clearBindings();
 
        Long _id = entity.get_id();
        if (_id != null) {
            stmt.bindLong(1, _id);
        }
 
        String xin_id = entity.getXin_id();
        if (xin_id != null) {
            stmt.bindString(2, xin_id);
        }
 
        String name = entity.getName();
        if (name != null) {
            stmt.bindString(3, name);
        }
 
        String company = entity.getCompany();
        if (company != null) {
            stmt.bindString(4, company);
        }
    }

    @Override
    protected final void bindValues(SQLiteStatement stmt, WhiteList entity) {
        stmt.clearBindings();
 
        Long _id = entity.get_id();
        if (_id != null) {
            stmt.bindLong(1, _id);
        }
 
        String xin_id = entity.getXin_id();
        if (xin_id != null) {
            stmt.bindString(2, xin_id);
        }
 
        String name = entity.getName();
        if (name != null) {
            stmt.bindString(3, name);
        }
 
        String company = entity.getCompany();
        if (company != null) {
            stmt.bindString(4, company);
        }
    }

    @Override
    public Long readKey(Cursor cursor, int offset) {
        return cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0);
    }    

    @Override
    public WhiteList readEntity(Cursor cursor, int offset) {
        WhiteList entity = new WhiteList( //
            cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0), // _id
            cursor.isNull(offset + 1) ? null : cursor.getString(offset + 1), // xin_id
            cursor.isNull(offset + 2) ? null : cursor.getString(offset + 2), // name
            cursor.isNull(offset + 3) ? null : cursor.getString(offset + 3) // company
        );
        return entity;
    }
     
    @Override
    public void readEntity(Cursor cursor, WhiteList entity, int offset) {
        entity.set_id(cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0));
        entity.setXin_id(cursor.isNull(offset + 1) ? null : cursor.getString(offset + 1));
        entity.setName(cursor.isNull(offset + 2) ? null : cursor.getString(offset + 2));
        entity.setCompany(cursor.isNull(offset + 3) ? null : cursor.getString(offset + 3));
     }
    
    @Override
    protected final Long updateKeyAfterInsert(WhiteList entity, long rowId) {
        entity.set_id(rowId);
        return rowId;
    }
    
    @Override
    public Long getKey(WhiteList entity) {
        if(entity != null) {
            return entity.get_id();
        } else {
            return null;
        }
    }

    @Override
    public boolean hasKey(WhiteList entity) {
        return entity.get_id() != null;
    }

    @Override
    protected final boolean isEntityUpdateable() {
        return true;
    }
    
}
