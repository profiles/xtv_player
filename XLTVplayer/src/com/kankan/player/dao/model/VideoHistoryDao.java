package com.kankan.player.dao.model;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import de.greenrobot.dao.internal.DaoConfig;

import com.kankan.player.dao.model.VideoHistory;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.
/** 
 * DAO for table VIDEO_HISTORY.
*/
public class VideoHistoryDao extends AbstractDao<VideoHistory, Long> {

    public static final String TABLENAME = "VIDEO_HISTORY";

    /**
     * Properties of entity VideoHistory.<br/>
     * Can be used for QueryBuilder and for referencing column names.
    */
    public static class Properties {
        public final static Property Id = new Property(0, Long.class, "id", true, "_id");
        public final static Property Cid = new Property(1, String.class, "cid", false, "CID");
        public final static Property FilePath = new Property(2, String.class, "filePath", false, "FILE_PATH");
        public final static Property FileName = new Property(3, String.class, "fileName", false, "FILE_NAME");
        public final static Property SubtitlePath = new Property(4, String.class, "subtitlePath", false, "SUBTITLE_PATH");
        public final static Property Width = new Property(5, Integer.class, "width", false, "WIDTH");
        public final static Property Height = new Property(6, Integer.class, "height", false, "HEIGHT");
        public final static Property Duration = new Property(7, Integer.class, "duration", false, "DURATION");
        public final static Property Progress = new Property(8, Integer.class, "progress", false, "PROGRESS");
        public final static Property ThumbnailPath = new Property(9, String.class, "thumbnailPath", false, "THUMBNAIL_PATH");
        public final static Property Timestamp = new Property(10, long.class, "timestamp", false, "TIMESTAMP");
        public final static Property DeviceName = new Property(11, String.class, "deviceName", false, "DEVICE_NAME");
        public final static Property DeviceType = new Property(12, Integer.class, "deviceType", false, "DEVICE_TYPE");
        public final static Property DisplayMode = new Property(13, Integer.class, "displayMode", false, "DISPLAY_MODE");
        public final static Property AudioMode = new Property(14, Integer.class, "audioMode", false, "AUDIO_MODE");
        public final static Property SubtitleType = new Property(15, Integer.class, "subtitleType", false, "SUBTITLE_TYPE");
    };


    public VideoHistoryDao(DaoConfig config) {
        super(config);
    }
    
    public VideoHistoryDao(DaoConfig config, DaoSession daoSession) {
        super(config, daoSession);
    }

    /** Creates the underlying database table. */
    public static void createTable(SQLiteDatabase db, boolean ifNotExists) {
        String constraint = ifNotExists? "IF NOT EXISTS ": "";
        db.execSQL("CREATE TABLE " + constraint + "'VIDEO_HISTORY' (" + //
                "'_id' INTEGER PRIMARY KEY ," + // 0: id
                "'CID' TEXT NOT NULL UNIQUE ," + // 1: cid
                "'FILE_PATH' TEXT NOT NULL UNIQUE ," + // 2: filePath
                "'FILE_NAME' TEXT," + // 3: fileName
                "'SUBTITLE_PATH' TEXT," + // 4: subtitlePath
                "'WIDTH' INTEGER," + // 5: width
                "'HEIGHT' INTEGER," + // 6: height
                "'DURATION' INTEGER," + // 7: duration
                "'PROGRESS' INTEGER," + // 8: progress
                "'THUMBNAIL_PATH' TEXT," + // 9: thumbnailPath
                "'TIMESTAMP' INTEGER NOT NULL ," + // 10: timestamp
                "'DEVICE_NAME' TEXT," + // 11: deviceName
                "'DEVICE_TYPE' INTEGER," + // 12: deviceType
                "'DISPLAY_MODE' INTEGER," + // 13: displayMode
                "'AUDIO_MODE' INTEGER," + // 14: audioMode
                "'SUBTITLE_TYPE' INTEGER);"); // 15: subtitleType
    }

    /** Drops the underlying database table. */
    public static void dropTable(SQLiteDatabase db, boolean ifExists) {
        String sql = "DROP TABLE " + (ifExists ? "IF EXISTS " : "") + "'VIDEO_HISTORY'";
        db.execSQL(sql);
    }

    /** @inheritdoc */
    @Override
    protected void bindValues(SQLiteStatement stmt, VideoHistory entity) {
        stmt.clearBindings();
 
        Long id = entity.getId();
        if (id != null) {
            stmt.bindLong(1, id);
        }
        stmt.bindString(2, entity.getCid());
        stmt.bindString(3, entity.getFilePath());
 
        String fileName = entity.getFileName();
        if (fileName != null) {
            stmt.bindString(4, fileName);
        }
 
        String subtitlePath = entity.getSubtitlePath();
        if (subtitlePath != null) {
            stmt.bindString(5, subtitlePath);
        }
 
        Integer width = entity.getWidth();
        if (width != null) {
            stmt.bindLong(6, width);
        }
 
        Integer height = entity.getHeight();
        if (height != null) {
            stmt.bindLong(7, height);
        }
 
        Integer duration = entity.getDuration();
        if (duration != null) {
            stmt.bindLong(8, duration);
        }
 
        Integer progress = entity.getProgress();
        if (progress != null) {
            stmt.bindLong(9, progress);
        }
 
        String thumbnailPath = entity.getThumbnailPath();
        if (thumbnailPath != null) {
            stmt.bindString(10, thumbnailPath);
        }
        stmt.bindLong(11, entity.getTimestamp());
 
        String deviceName = entity.getDeviceName();
        if (deviceName != null) {
            stmt.bindString(12, deviceName);
        }
 
        Integer deviceType = entity.getDeviceType();
        if (deviceType != null) {
            stmt.bindLong(13, deviceType);
        }
 
        Integer displayMode = entity.getDisplayMode();
        if (displayMode != null) {
            stmt.bindLong(14, displayMode);
        }
 
        Integer audioMode = entity.getAudioMode();
        if (audioMode != null) {
            stmt.bindLong(15, audioMode);
        }
 
        Integer subtitleType = entity.getSubtitleType();
        if (subtitleType != null) {
            stmt.bindLong(16, subtitleType);
        }
    }

    /** @inheritdoc */
    @Override
    public Long readKey(Cursor cursor, int offset) {
        return cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0);
    }    

    /** @inheritdoc */
    @Override
    public VideoHistory readEntity(Cursor cursor, int offset) {
        VideoHistory entity = new VideoHistory( //
            cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0), // id
            cursor.getString(offset + 1), // cid
            cursor.getString(offset + 2), // filePath
            cursor.isNull(offset + 3) ? null : cursor.getString(offset + 3), // fileName
            cursor.isNull(offset + 4) ? null : cursor.getString(offset + 4), // subtitlePath
            cursor.isNull(offset + 5) ? null : cursor.getInt(offset + 5), // width
            cursor.isNull(offset + 6) ? null : cursor.getInt(offset + 6), // height
            cursor.isNull(offset + 7) ? null : cursor.getInt(offset + 7), // duration
            cursor.isNull(offset + 8) ? null : cursor.getInt(offset + 8), // progress
            cursor.isNull(offset + 9) ? null : cursor.getString(offset + 9), // thumbnailPath
            cursor.getLong(offset + 10), // timestamp
            cursor.isNull(offset + 11) ? null : cursor.getString(offset + 11), // deviceName
            cursor.isNull(offset + 12) ? null : cursor.getInt(offset + 12), // deviceType
            cursor.isNull(offset + 13) ? null : cursor.getInt(offset + 13), // displayMode
            cursor.isNull(offset + 14) ? null : cursor.getInt(offset + 14), // audioMode
            cursor.isNull(offset + 15) ? null : cursor.getInt(offset + 15) // subtitleType
        );
        return entity;
    }
     
    /** @inheritdoc */
    @Override
    public void readEntity(Cursor cursor, VideoHistory entity, int offset) {
        entity.setId(cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0));
        entity.setCid(cursor.getString(offset + 1));
        entity.setFilePath(cursor.getString(offset + 2));
        entity.setFileName(cursor.isNull(offset + 3) ? null : cursor.getString(offset + 3));
        entity.setSubtitlePath(cursor.isNull(offset + 4) ? null : cursor.getString(offset + 4));
        entity.setWidth(cursor.isNull(offset + 5) ? null : cursor.getInt(offset + 5));
        entity.setHeight(cursor.isNull(offset + 6) ? null : cursor.getInt(offset + 6));
        entity.setDuration(cursor.isNull(offset + 7) ? null : cursor.getInt(offset + 7));
        entity.setProgress(cursor.isNull(offset + 8) ? null : cursor.getInt(offset + 8));
        entity.setThumbnailPath(cursor.isNull(offset + 9) ? null : cursor.getString(offset + 9));
        entity.setTimestamp(cursor.getLong(offset + 10));
        entity.setDeviceName(cursor.isNull(offset + 11) ? null : cursor.getString(offset + 11));
        entity.setDeviceType(cursor.isNull(offset + 12) ? null : cursor.getInt(offset + 12));
        entity.setDisplayMode(cursor.isNull(offset + 13) ? null : cursor.getInt(offset + 13));
        entity.setAudioMode(cursor.isNull(offset + 14) ? null : cursor.getInt(offset + 14));
        entity.setSubtitleType(cursor.isNull(offset + 15) ? null : cursor.getInt(offset + 15));
     }
    
    /** @inheritdoc */
    @Override
    protected Long updateKeyAfterInsert(VideoHistory entity, long rowId) {
        entity.setId(rowId);
        return rowId;
    }
    
    /** @inheritdoc */
    @Override
    public Long getKey(VideoHistory entity) {
        if(entity != null) {
            return entity.getId();
        } else {
            return null;
        }
    }

    /** @inheritdoc */
    @Override    
    protected boolean isEntityUpdateable() {
        return true;
    }
    
}
