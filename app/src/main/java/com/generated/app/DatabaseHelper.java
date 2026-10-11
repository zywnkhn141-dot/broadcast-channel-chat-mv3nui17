package com.generated.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "channel_chat.db";
    private static final int DATABASE_VERSION = 1;

    // Posts table
    public static final String TABLE_POSTS = "posts";
    public static final String COL_ID = "_id";
    public static final String COL_CONTENT = "content";
    public static final String COL_AUTHOR = "author";
    public static final String COL_TIMESTAMP = "timestamp";
    public static final String COL_IS_PINNED = "is_pinned";
    public static final String COL_CATEGORY = "category"; // עדכון, הודעה, קישור, סקר
    public static final String COL_POLL_QUESTION = "poll_question";
    public static final String COL_POLL_OPT1 = "poll_opt1";
    public static final String COL_POLL_OPT2 = "poll_opt2";
    public static final String COL_POLL_VOTES1 = "poll_votes1";
    public static final String COL_POLL_VOTES2 = "poll_votes2";
    public static final String COL_VIEWS = "views";

    // Comments table
    public static final String TABLE_COMMENTS = "comments";
    public static final String COL_C_ID = "_id";
    public static final String COL_C_POST_ID = "post_id";
    public static final String COL_C_AUTHOR = "author";
    public static final String COL_C_CONTENT = "content";
    public static final String COL_C_TIME = "timestamp";

    // Reactions table
    public static final String TABLE_REACTIONS = "reactions";
    public static final String COL_R_POST_ID = "post_id";
    public static final String COL_R_USER = "user_email";
    public static final String COL_R_EMOJI = "emoji";

    // Saved / Bookmarks table
    public static final String TABLE_SAVED = "saved_posts";
    public static final String COL_S_POST_ID = "post_id";
    public static final String COL_S_USER = "user_email";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_POSTS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_CONTENT + " TEXT, " +
                COL_AUTHOR + " TEXT, " +
                COL_TIMESTAMP + " INTEGER, " +
                COL_IS_PINNED + " INTEGER DEFAULT 0, " +
                COL_CATEGORY + " TEXT, " +
                COL_POLL_QUESTION + " TEXT, " +
                COL_POLL_OPT1 + " TEXT, " +
                COL_POLL_OPT2 + " TEXT, " +
                COL_POLL_VOTES1 + " INTEGER DEFAULT 0, " +
                COL_POLL_VOTES2 + " INTEGER DEFAULT 0, " +
                COL_VIEWS + " INTEGER DEFAULT 1);");

        db.execSQL("CREATE TABLE " + TABLE_COMMENTS + " (" +
                COL_C_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_C_POST_ID + " INTEGER, " +
                COL_C_AUTHOR + " TEXT, " +
                COL_C_CONTENT + " TEXT, " +
                COL_C_TIME + " INTEGER);");

        db.execSQL("CREATE TABLE " + TABLE_REACTIONS + " (" +
                COL_R_POST_ID + " INTEGER, " +
                COL_R_USER + " TEXT, " +
                COL_R_EMOJI + " TEXT, " +
                "PRIMARY KEY (" + COL_R_POST_ID + ", " + COL_R_USER + ", " + COL_R_EMOJI + "));");

        db.execSQL("CREATE TABLE " + TABLE_SAVED + " (" +
                COL_S_POST_ID + " INTEGER, " +
                COL_S_USER + " TEXT, " +
                "PRIMARY KEY (" + COL_S_POST_ID + ", " + COL_S_USER + "));");

        // Seed initial welcoming announcements
        seedInitialData(db);
    }

    private void seedInitialData(SQLiteDatabase db) {
        long now = System.currentTimeMillis();

        ContentValues cv1 = new ContentValues();
        cv1.put(COL_CONTENT, "ברוכים הבאים לערוץ הרשמי שלי! 🎉\nכאן אפרסם באופן בלעדי עדכונים, פרויקטים חדשים וסקרים. רק מנהל הערוץ יכול לפרסם, אך אתם מוזמנים להגיב, להצביע בסקרים ולהוסיף תגובות רגש (Reactions)!");
        cv1.put(COL_AUTHOR, "מנהל הערוץ");
        cv1.put(COL_TIMESTAMP, now - 3600000 * 5);
        cv1.put(COL_IS_PINNED, 1);
        cv1.put(COL_CATEGORY, "הודעה נעוצה");
        cv1.put(COL_VIEWS, 42);
        long p1 = db.insert(TABLE_POSTS, null, cv1);

        ContentValues cv2 = new ContentValues();
        cv2.put(COL_CONTENT, "סקר קהילה מהיר: איזה נושא תרצו שנכסה בעדכון הבא?");
        cv2.put(COL_AUTHOR, "מנהל הערוץ");
        cv2.put(COL_TIMESTAMP, now - 3600000 * 2);
        cv2.put(COL_IS_PINNED, 0);
        cv2.put(COL_CATEGORY, "סקר");
        cv2.put(COL_POLL_QUESTION, "איזה פיצ'ר הכי חשוב לכם באפליקציה?");
        cv2.put(COL_POLL_OPT1, "מערכת התראות מותאמת 🔔");
        cv2.put(COL_POLL_OPT2, "שיתוף מדיה וקבצים 📂");
        cv2.put(COL_POLL_VOTES1, 14);
        cv2.put(COL_POLL_VOTES2, 9);
        cv2.put(COL_VIEWS, 86);
        long p2 = db.insert(TABLE_POSTS, null, cv2);

        // Add some sample reactions
        insertReactionRaw(db, p1, "subscriber1@gmail.com", "❤️");
        insertReactionRaw(db, p1, "subscriber2@gmail.com", "🔥");
        insertReactionRaw(db, p1, "subscriber3@gmail.com", "👍");
        insertReactionRaw(db, p2, "subscriber1@gmail.com", "💡");

        // Add sample comment
        ContentValues c1 = new ContentValues();
        c1.put(COL_C_POST_ID, p1);
        c1.put(COL_C_AUTHOR, "ישראל ישראלי");
        c1.put(COL_C_CONTENT, "מעולה! בהצלחה עם הערוץ החדש!");
        c1.put(COL_C_TIME, now - 3600000 * 3);
        db.insert(TABLE_COMMENTS, null, c1);
    }

    private void insertReactionRaw(SQLiteDatabase db, long postId, String user, String emoji) {
        ContentValues cv = new ContentValues();
        cv.put(COL_R_POST_ID, postId);
        cv.put(COL_R_USER, user);
        cv.put(COL_R_EMOJI, emoji);
        db.insertWithOnConflict(TABLE_REACTIONS, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_POSTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_COMMENTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_REACTIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SAVED);
        onCreate(db);
    }

    public long addPost(String content, String category, String pollQ, String opt1, String opt2) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_CONTENT, content);
        cv.put(COL_AUTHOR, "מנהל הערוץ");
        cv.put(COL_TIMESTAMP, System.currentTimeMillis());
        cv.put(COL_IS_PINNED, 0);
        cv.put(COL_CATEGORY, category);
        if (pollQ != null && !pollQ.trim().isEmpty()) {
            cv.put(COL_POLL_QUESTION, pollQ);
            cv.put(COL_POLL_OPT1, opt1);
            cv.put(COL_POLL_OPT2, opt2);
            cv.put(COL_POLL_VOTES1, 0);
            cv.put(COL_POLL_VOTES2, 0);
        }
        cv.put(COL_VIEWS, 1);
        return db.insert(TABLE_POSTS, null, cv);
    }

    public void togglePin(long postId, boolean currentPinned) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_IS_PINNED, currentPinned ? 0 : 1);
        db.update(TABLE_POSTS, cv, COL_ID + "=?", new String[]{String.valueOf(postId)});
    }

    public void deletePost(long postId) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_POSTS, COL_ID + "=?", new String[]{String.valueOf(postId)});
        db.delete(TABLE_COMMENTS, COL_C_POST_ID + "=?", new String[]{String.valueOf(postId)});
        db.delete(TABLE_REACTIONS, COL_R_POST_ID + "=?", new String[]{String.valueOf(postId)});
        db.delete(TABLE_SAVED, COL_S_POST_ID + "=?", new String[]{String.valueOf(postId)});
    }

    public void incrementViews(long postId) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("UPDATE " + TABLE_POSTS + " SET " + COL_VIEWS + " = " + COL_VIEWS + " + 1 WHERE " + COL_ID + "=" + postId);
    }

    public void votePoll(long postId, int optionIndex) {
        SQLiteDatabase db = getWritableDatabase();
        if (optionIndex == 1) {
            db.execSQL("UPDATE " + TABLE_POSTS + " SET " + COL_POLL_VOTES1 + " = " + COL_POLL_VOTES1 + " + 1 WHERE " + COL_ID + "=" + postId);
        } else {
            db.execSQL("UPDATE " + TABLE_POSTS + " SET " + COL_POLL_VOTES2 + " = " + COL_POLL_VOTES2 + " + 1 WHERE " + COL_ID + "=" + postId);
        }
    }

    public boolean toggleReaction(long postId, String userEmail, String emoji) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor c = db.query(TABLE_REACTIONS, null,
                COL_R_POST_ID + "=? AND " + COL_R_USER + "=? AND " + COL_R_EMOJI + "=?",
                new String[]{String.valueOf(postId), userEmail, emoji}, null, null, null);
        boolean exists = c.moveToFirst();
        c.close();

        if (exists) {
            db.delete(TABLE_REACTIONS,
                    COL_R_POST_ID + "=? AND " + COL_R_USER + "=? AND " + COL_R_EMOJI + "=?",
                    new String[]{String.valueOf(postId), userEmail, emoji});
            return false;
        } else {
            ContentValues cv = new ContentValues();
            cv.put(COL_R_POST_ID, postId);
            cv.put(COL_R_USER, userEmail);
            cv.put(COL_R_EMOJI, emoji);
            db.insert(TABLE_REACTIONS, null, cv);
            return true;
        }
    }

    public int getReactionCount(long postId, String emoji) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_REACTIONS + " WHERE " +
                COL_R_POST_ID + "=? AND " + COL_R_EMOJI + "=?", new String[]{String.valueOf(postId), emoji});
        int count = 0;
        if (c.moveToFirst()) {
            count = c.getInt(0);
        }
        c.close();
        return count;
    }

    public boolean hasUserReacted(long postId, String userEmail, String emoji) {
        if (userEmail == null) return false;
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_REACTIONS, null,
                COL_R_POST_ID + "=? AND " + COL_R_USER + "=? AND " + COL_R_EMOJI + "=?",
                new String[]{String.valueOf(postId), userEmail, emoji}, null, null, null);
        boolean has = c.moveToFirst();
        c.close();
        return has;
    }

    public boolean toggleSaved(long postId, String userEmail) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor c = db.query(TABLE_SAVED, null,
                COL_S_POST_ID + "=? AND " + COL_S_USER + "=?",
                new String[]{String.valueOf(postId), userEmail}, null, null, null);
        boolean exists = c.moveToFirst();
        c.close();

        if (exists) {
            db.delete(TABLE_SAVED,
                    COL_S_POST_ID + "=? AND " + COL_S_USER + "=?",
                    new String[]{String.valueOf(postId), userEmail});
            return false;
        } else {
            ContentValues cv = new ContentValues();
            cv.put(COL_S_POST_ID, postId);
            cv.put(COL_S_USER, userEmail);
            db.insert(TABLE_SAVED, null, cv);
            return true;
        }
    }

    public boolean isSaved(long postId, String userEmail) {
        if (userEmail == null) return false;
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_SAVED, null,
                COL_S_POST_ID + "=? AND " + COL_S_USER + "=?",
                new String[]{String.valueOf(postId), userEmail}, null, null, null);
        boolean saved = c.moveToFirst();
        c.close();
        return saved;
    }

    public void addComment(long postId, String author, String content) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_C_POST_ID, postId);
        cv.put(COL_C_AUTHOR, author);
        cv.put(COL_C_CONTENT, content);
        cv.put(COL_C_TIME, System.currentTimeMillis());
        db.insert(TABLE_COMMENTS, null, cv);
    }

    public int getCommentCount(long postId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_COMMENTS + " WHERE " +
                COL_C_POST_ID + "=?", new String[]{String.valueOf(postId)});
        int count = 0;
        if (c.moveToFirst()) {
            count = c.getInt(0);
        }
        c.close();
        return count;
    }

    public List<CommentItem> getComments(long postId) {
        List<CommentItem> list = new ArrayList<CommentItem>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_COMMENTS, null,
                COL_C_POST_ID + "=?", new String[]{String.valueOf(postId)},
                null, null, COL_C_TIME + " ASC");
        if (c.moveToFirst()) {
            do {
                CommentItem item = new CommentItem();
                item.id = c.getLong(c.getColumnIndex(COL_C_ID));
                item.author = c.getString(c.getColumnIndex(COL_C_AUTHOR));
                item.content = c.getString(c.getColumnIndex(COL_C_CONTENT));
                item.timestamp = c.getLong(c.getColumnIndex(COL_C_TIME));
                list.add(item);
            } while (c.moveToNext());
        }
        c.close();
        return list;
    }

    public static class CommentItem {
        public long id;
        public String author;
        public String content;
        public long timestamp;
    }
}
