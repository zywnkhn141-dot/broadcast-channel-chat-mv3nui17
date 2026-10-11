package com.generated.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final String PREFS_NAME = "channel_prefs";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_IS_ADMIN = "is_admin";

    private DatabaseHelper dbHelper;
    private SharedPreferences prefs;

    // Current session
    private String currentUserEmail = null;
    private String currentUserName = null;
    private boolean isAdmin = false;

    // UI elements
    private TextView tvAccountStatus;
    private TextView tvChannelSubscribers;
    private TextView tvPostsCount;
    private Button btnAccountAction;
    private Button btnSwitchRole;
    private EditText etSearch;
    private Button tabAll;
    private Button tabPinned;
    private Button tabSaved;
    private ListView lvPosts;
    private View adminPostBar;
    private TextView tvEmpty;

    private int currentTab = 0; // 0=All, 1=Pinned, 2=Saved
    private String currentFilter = "";
    private List<Post> displayedPosts = new ArrayList<Post>();
    private PostsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = new DatabaseHelper(this);
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Load saved session
        currentUserEmail = prefs.getString(KEY_USER_EMAIL, null);
        currentUserName = prefs.getString(KEY_USER_NAME, null);
        isAdmin = prefs.getBoolean(KEY_IS_ADMIN, false);

        // Build top-notch UI programmatically with framework components
        setContentView(createMainLayout());

        refreshRoleUI();
        loadPosts();
    }

    private View createMainLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#F4F6F9"));
        root.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // 1. Channel Header Card
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setBackgroundResource(R.drawable.bg_header);
        header.setPadding(dp(20), dp(24), dp(20), dp(20));

        // Header Top Row: Channel title + Role Badge
        LinearLayout headerTopRow = new LinearLayout(this);
        headerTopRow.setOrientation(LinearLayout.HORIZONTAL);
        headerTopRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(getString(R.string.channel_title));
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        headerTopRow.addView(tvTitle, titleLp);

        btnSwitchRole = new Button(this);
        btnSwitchRole.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnSwitchRole.setBackgroundResource(R.drawable.bg_pill_admin);
        btnSwitchRole.setPadding(dp(12), dp(4), dp(12), dp(4));
        btnSwitchRole.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRoleSwitchDialog();
            }
        });
        headerTopRow.addView(btnSwitchRole);
        header.addView(headerTopRow);

        // Subtitle
        TextView tvSub = new TextView(this);
        tvSub.setText(getString(R.string.channel_subtitle));
        tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvSub.setTextColor(Color.parseColor("#BBDEFB"));
        tvSub.setPadding(0, dp(4), 0, dp(12));
        header.addView(tvSub);

        // Stats & Google Login Bar
        LinearLayout accountBar = new LinearLayout(this);
        accountBar.setOrientation(LinearLayout.HORIZONTAL);
        accountBar.setGravity(Gravity.CENTER_VERTICAL);

        tvAccountStatus = new TextView(this);
        tvAccountStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvAccountStatus.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams statusLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        accountBar.addView(tvAccountStatus, statusLp);

        btnAccountAction = new Button(this);
        btnAccountAction.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnAccountAction.setBackgroundResource(R.drawable.bg_button_google);
        btnAccountAction.setTextColor(Color.parseColor("#37474F"));
        btnAccountAction.setPadding(dp(14), dp(6), dp(14), dp(6));
        btnAccountAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentUserEmail == null) {
                    showGoogleSignInDialog();
                } else {
                    showSignOutConfirmation();
                }
            }
        });
        accountBar.addView(btnAccountAction);
        header.addView(accountBar);

        root.addView(header);

        // 2. Search & Filter Bar
        LinearLayout filterBar = new LinearLayout(this);
        filterBar.setOrientation(LinearLayout.VERTICAL);
        filterBar.setPadding(dp(16), dp(12), dp(16), dp(6));

        etSearch = new EditText(this);
        etSearch.setHint(getString(R.string.search_hint));
        etSearch.setBackgroundResource(R.drawable.bg_input_field);
        etSearch.setPadding(dp(16), dp(10), dp(16), dp(10));
        etSearch.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        etSearch.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentFilter = s.toString().trim();
                loadPosts();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
        filterBar.addView(etSearch);

        // Filter Tabs
        LinearLayout tabsLayout = new LinearLayout(this);
        tabsLayout.setOrientation(LinearLayout.HORIZONTAL);
        tabsLayout.setPadding(0, dp(10), 0, 0);

        tabAll = createTabButton(getString(R.string.tab_all), 0);
        tabPinned = createTabButton(getString(R.string.tab_pinned), 1);
        tabSaved = createTabButton(getString(R.string.tab_saved), 2);

        tabsLayout.addView(tabAll);
        tabsLayout.addView(tabPinned);
        tabsLayout.addView(tabSaved);
        filterBar.addView(tabsLayout);

        root.addView(filterBar);

        // 3. Posts List & Empty view
        FrameLayout listContainer = new FrameLayout(this);
        LinearLayout.LayoutParams listLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
        listContainer.setLayoutParams(listLp);

        lvPosts = new ListView(this);
        lvPosts.setDivider(null);
        lvPosts.setDividerHeight(dp(12));
        lvPosts.setPadding(dp(16), dp(8), dp(16), dp(16));
        lvPosts.setClipToPadding(false);
        adapter = new PostsAdapter();
        lvPosts.setAdapter(adapter);
        listContainer.addView(lvPosts);

        tvEmpty = new TextView(this);
        tvEmpty.setText(getString(R.string.no_posts));
        tvEmpty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        tvEmpty.setTextColor(Color.parseColor("#9E9E9E"));
        tvEmpty.setGravity(Gravity.CENTER);
        tvEmpty.setVisibility(View.GONE);
        listContainer.addView(tvEmpty);

        root.addView(listContainer);

        // 4. Admin Post Creation Dock (Only active for admin, or informational bar for users)
        adminPostBar = createAdminDock();
        root.addView(adminPostBar);

        return root;
    }

    private Button createTabButton(String text, final int index) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        lp.setMargins(dp(3), 0, dp(3), 0);
        btn.setLayoutParams(lp);
        btn.setPadding(dp(8), dp(6), dp(8), dp(6));
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentTab = index;
                updateTabStyles();
                loadPosts();
            }
        });
        return btn;
    }

    private void updateTabStyles() {
        tabAll.setBackgroundResource(currentTab == 0 ? R.drawable.bg_chip_selected : R.drawable.bg_chip);
        tabAll.setTextColor(currentTab == 0 ? Color.WHITE : Color.parseColor("#455A64"));

        tabPinned.setBackgroundResource(currentTab == 1 ? R.drawable.bg_chip_selected : R.drawable.bg_chip);
        tabPinned.setTextColor(currentTab == 1 ? Color.WHITE : Color.parseColor("#455A64"));

        tabSaved.setBackgroundResource(currentTab == 2 ? R.drawable.bg_chip_selected : R.drawable.bg_chip);
        tabSaved.setTextColor(currentTab == 2 ? Color.WHITE : Color.parseColor("#455A64"));
    }

    private View createAdminDock() {
        LinearLayout dock = new LinearLayout(this);
        dock.setOrientation(LinearLayout.VERTICAL);
        dock.setBackgroundColor(Color.WHITE);
        dock.setElevation(dp(8));
        dock.setPadding(dp(16), dp(12), dp(16), dp(12));

        // When in Admin mode: Rich post composer trigger
        Button btnCompose = new Button(this);
        btnCompose.setText("✍️ פרסם עדכון חדש בערוץ...");
        btnCompose.setBackgroundResource(R.drawable.bg_button_primary);
        btnCompose.setTextColor(Color.WHITE);
        btnCompose.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        btnCompose.setPadding(dp(16), dp(12), dp(16), dp(12));
        btnCompose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCreatePostDialog();
            }
        });

        // When in Subscriber mode: Channel broadcast disclaimer
        TextView tvSubscriberNote = new TextView(this);
        tvSubscriberNote.setText("🔒 ערוץ שידור: רק מנהל הערוץ רשאי לפרסם. ניתן להגיב, להצביע ולהוסיף תגובות רגש.");
        tvSubscriberNote.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvSubscriberNote.setTextColor(Color.parseColor("#78909C"));
        tvSubscriberNote.setGravity(Gravity.CENTER);

        dock.addView(btnCompose);
        dock.addView(tvSubscriberNote);

        dock.setTag(new View[]{btnCompose, tvSubscriberNote});
        return dock;
    }

    private void refreshRoleUI() {
        updateTabStyles();

        if (isAdmin) {
            btnSwitchRole.setText("👑 מנהל (לחץ להחלפה)");
            btnSwitchRole.setTextColor(Color.parseColor("#E65100"));
        } else {
            btnSwitchRole.setText("👥 מנוי (לחץ להחלפה)");
            btnSwitchRole.setTextColor(Color.parseColor("#1565C0"));
        }

        if (currentUserEmail != null) {
            tvAccountStatus.setText("מחובר: " + currentUserName);
            btnAccountAction.setText("התנתק 🚪");
        } else {
            tvAccountStatus.setText("לא מחובר עם Google");
            btnAccountAction.setText("התחבר עם Google G");
        }

        View[] views = (View[]) adminPostBar.getTag();
        if (views != null && views.length == 2) {
            views[0].setVisibility(isAdmin ? View.VISIBLE : View.GONE);
            views[1].setVisibility(isAdmin ? View.GONE : View.VISIBLE);
        }
    }

    private void showRoleSwitchDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("בחירת תפקיד באפליקציה");
        builder.setMessage("באפשרותך לעבור למצב 'מנהל הערוץ' (שמאפשר להעלות תוכן בלעדי, לנהל סקרים ולמחוק הודעות) או למצב 'מנוי רגיל' שצופה ומגיב באמצעות חשבון Google.");

        builder.setPositiveButton("מעבר למצב מנהל 👑", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                isAdmin = true;
                prefs.edit().putBoolean(KEY_IS_ADMIN, true).apply();
                refreshRoleUI();
                adapter.notifyDataSetChanged();
                Toast.makeText(MainActivity.this, "עברת למצב מנהל הערוץ. כעת אתה רשאי לפרסם!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("מעבר למצב מנוי 👥", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                isAdmin = false;
                prefs.edit().putBoolean(KEY_IS_ADMIN, false).apply();
                refreshRoleUI();
                adapter.notifyDataSetChanged();
                Toast.makeText(MainActivity.this, "עברת למצב מנוי רגיל בערוץ.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNeutralButton("ביטול", null);
        builder.show();
    }

    private void showGoogleSignInDialog() {
        // Realistic Google Sign-In interactive selector
        final String[] demoGoogleAccounts = new String[]{
                "yossi.cohen@gmail.com (יוסי כהן)",
                "noa.levi@gmail.com (נועה לוי)",
                "daniel.israel@gmail.com (דניאל ישראל)",
                "הזן חשבון Google חדש..."
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("בחר חשבון Google להתחברות");
        builder.setItems(demoGoogleAccounts, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 3) {
                    showCustomGoogleInput();
                } else {
                    String selected = demoGoogleAccounts[which];
                    int emailEnd = selected.indexOf(" ");
                    String email = selected.substring(0, emailEnd);
                    int nameStart = selected.indexOf("(") + 1;
                    int nameEnd = selected.indexOf(")");
                    String name = selected.substring(nameStart, nameEnd);

                    performLogin(email, name);
                }
            }
        });
        builder.setNegativeButton("ביטול", null);
        builder.show();
    }

    private void showCustomGoogleInput() {
        final EditText input = new EditText(this);
        input.setHint("your.name@gmail.com");
        input.setPadding(dp(16), dp(16), dp(16), dp(16));

        new AlertDialog.Builder(this)
                .setTitle("התחברות עם חשבון Google")
                .setMessage("הזן את כתובת ה-Gmail שלך:")
                .setView(input)
                .setPositiveButton("התחבר", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String email = input.getText().toString().trim();
                        if (email.contains("@")) {
                            String name = email.split("@")[0];
                            performLogin(email, name);
                        } else {
                            Toast.makeText(MainActivity.this, "נא להזין כתובת דוא\"ל תקינה", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void performLogin(String email, String name) {
        currentUserEmail = email;
        currentUserName = name;
        prefs.edit()
                .putString(KEY_USER_EMAIL, currentUserEmail)
                .putString(KEY_USER_NAME, currentUserName)
                .apply();
        refreshRoleUI();
        loadPosts();
        Toast.makeText(this, "התחברת בהצלחה כ-" + name + "! 🎉", Toast.LENGTH_SHORT).show();
    }

    private void showSignOutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("התנתקות")
                .setMessage("האם ברצונך להתנתק מחשבון " + currentUserEmail + "?")
                .setPositiveButton("התנתק", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        currentUserEmail = null;
                        currentUserName = null;
                        prefs.edit()
                                .remove(KEY_USER_EMAIL)
                                .remove(KEY_USER_NAME)
                                .apply();
                        refreshRoleUI();
                        loadPosts();
                        Toast.makeText(MainActivity.this, "התנתקת מהערוץ", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void showCreatePostDialog() {
        ScrollView sv = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(16), dp(20), dp(16));
        sv.addView(content);

        final EditText etPostContent = new EditText(this);
        etPostContent.setHint("תוכן ההודעה או העדכון...");
        etPostContent.setMinLines(3);
        etPostContent.setBackgroundResource(R.drawable.bg_input_field);
        etPostContent.setPadding(dp(12), dp(12), dp(12), dp(12));
        content.addView(etPostContent);

        // Post Type Selector
        TextView tvType = new TextView(this);
        tvType.setText("סוג העדכון:");
        tvType.setTypeface(Typeface.DEFAULT_BOLD);
        tvType.setPadding(0, dp(12), 0, dp(6));
        content.addView(tvType);

        final CheckBox cbPoll = new CheckBox(this);
        cbPoll.setText("צרף סקר הצבעה לעדכון 📊");
        content.addView(cbPoll);

        final LinearLayout pollFields = new LinearLayout(this);
        pollFields.setOrientation(LinearLayout.VERTICAL);
        pollFields.setVisibility(View.GONE);

        final EditText etPollQ = new EditText(this);
        etPollQ.setHint("שאלת הסקר...");
        etPollQ.setBackgroundResource(R.drawable.bg_input_field);
        etPollQ.setPadding(dp(10), dp(8), dp(10), dp(8));
        LinearLayout.LayoutParams qLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        qLp.setMargins(0, dp(6), 0, dp(6));
        pollFields.addView(etPollQ, qLp);

        final EditText etOpt1 = new EditText(this);
        etOpt1.setHint("אפשרות ראשונה...");
        etOpt1.setBackgroundResource(R.drawable.bg_input_field);
        etOpt1.setPadding(dp(10), dp(8), dp(10), dp(8));
        LinearLayout.LayoutParams opt1Lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        opt1Lp.setMargins(0, 0, 0, dp(6));
        pollFields.addView(etOpt1, opt1Lp);

        final EditText etOpt2 = new EditText(this);
        etOpt2.setHint("אפשרות שנייה...");
        etOpt2.setBackgroundResource(R.drawable.bg_input_field);
        etOpt2.setPadding(dp(10), dp(8), dp(10), dp(8));
        pollFields.addView(etOpt2);

        content.addView(pollFields);

        cbPoll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pollFields.setVisibility(cbPoll.isChecked() ? View.VISIBLE : View.GONE);
            }
        });

        new AlertDialog.Builder(this)
                .setTitle("📢 יצירת עדכון חדש לערוץ")
                .setView(sv)
                .setPositiveButton("פרסם", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String postText = etPostContent.getText().toString().trim();
                        if (TextUtils.isEmpty(postText)) {
                            Toast.makeText(MainActivity.this, "תוכן ההודעה אינו יכול להיות ריק", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String category = "עדכון";
                        String pollQ = null;
                        String opt1 = null;
                        String opt2 = null;

                        if (cbPoll.isChecked()) {
                            category = "סקר";
                            pollQ = etPollQ.getText().toString().trim();
                            opt1 = etOpt1.getText().toString().trim();
                            opt2 = etOpt2.getText().toString().trim();
                            if (TextUtils.isEmpty(pollQ) || TextUtils.isEmpty(opt1) || TextUtils.isEmpty(opt2)) {
                                Toast.makeText(MainActivity.this, "נא למלא את כל שדות הסקר", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }

                        dbHelper.addPost(postText, category, pollQ, opt1, opt2);
                        Toast.makeText(MainActivity.this, "העדכון פורסם בהצלחה!", Toast.LENGTH_SHORT).show();
                        loadPosts();
                    }
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void loadPosts() {
        displayedPosts.clear();
        android.database.sqlite.SQLiteDatabase db = dbHelper.getReadableDatabase();

        String selection = null;
        List<String> argsList = new ArrayList<String>();

        if (currentTab == 1) { // Pinned only
            selection = DatabaseHelper.COL_IS_PINNED + "= 1";
        } else if (currentTab == 2) { // Saved only
            if (currentUserEmail == null) {
                adapter.notifyDataSetChanged();
                tvEmpty.setText("עליך להתחבר עם Google כדי לראות פוסטים שמורים");
                tvEmpty.setVisibility(View.VISIBLE);
                return;
            }
            selection = DatabaseHelper.COL_ID + " IN (SELECT " + DatabaseHelper.COL_S_POST_ID +
                    " FROM " + DatabaseHelper.TABLE_SAVED + " WHERE " + DatabaseHelper.COL_S_USER + "=?)";
            argsList.add(currentUserEmail);
        }

        if (!TextUtils.isEmpty(currentFilter)) {
            String searchClause = "(" + DatabaseHelper.COL_CONTENT + " LIKE ? OR " +
                    DatabaseHelper.COL_POLL_QUESTION + " LIKE ?)";
            if (selection == null) {
                selection = searchClause;
            } else {
                selection = selection + " AND " + searchClause;
            }
            argsList.add("%" + currentFilter + "%");
            argsList.add("%" + currentFilter + "%");
        }

        String[] selectionArgs = argsList.isEmpty() ? null : argsList.toArray(new String[0]);

        android.database.Cursor c = db.query(
                DatabaseHelper.TABLE_POSTS,
                null,
                selection,
                selectionArgs,
                null,
                null,
                DatabaseHelper.COL_IS_PINNED + " DESC, " + DatabaseHelper.COL_TIMESTAMP + " DESC"
        );

        if (c.moveToFirst()) {
            do {
                Post post = new Post();
                post.id = c.getLong(c.getColumnIndex(DatabaseHelper.COL_ID));
                post.content = c.getString(c.getColumnIndex(DatabaseHelper.COL_CONTENT));
                post.author = c.getString(c.getColumnIndex(DatabaseHelper.COL_AUTHOR));
                post.timestamp = c.getLong(c.getColumnIndex(DatabaseHelper.COL_TIMESTAMP));
                post.isPinned = c.getInt(c.getColumnIndex(DatabaseHelper.COL_IS_PINNED)) == 1;
                post.category = c.getString(c.getColumnIndex(DatabaseHelper.COL_CATEGORY));
                post.pollQuestion = c.getString(c.getColumnIndex(DatabaseHelper.COL_POLL_QUESTION));
                post.pollOpt1 = c.getString(c.getColumnIndex(DatabaseHelper.COL_POLL_OPT1));
                post.pollOpt2 = c.getString(c.getColumnIndex(DatabaseHelper.COL_POLL_OPT2));
                post.pollVotes1 = c.getInt(c.getColumnIndex(DatabaseHelper.COL_POLL_VOTES1));
                post.pollVotes2 = c.getInt(c.getColumnIndex(DatabaseHelper.COL_POLL_VOTES2));
                post.views = c.getInt(c.getColumnIndex(DatabaseHelper.COL_VIEWS));
                displayedPosts.add(post);
            } while (c.moveToNext());
        }
        c.close();

        adapter.notifyDataSetChanged();
        if (displayedPosts.isEmpty()) {
            tvEmpty.setText(getString(R.string.no_posts));
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            tvEmpty.setVisibility(View.GONE);
        }
    }

    private void showCommentsDialog(final Post post) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("תגובות לעדכון 💬");

        LinearLayout mainView = new LinearLayout(this);
        mainView.setOrientation(LinearLayout.VERTICAL);
        mainView.setPadding(dp(16), dp(12), dp(16), dp(12));

        // Comments list
        final LinearLayout commentsContainer = new LinearLayout(this);
        commentsContainer.setOrientation(LinearLayout.VERTICAL);

        ScrollView sv = new ScrollView(this);
        sv.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(240)));
        sv.addView(commentsContainer);
        mainView.addView(sv);

        refreshCommentsList(post.id, commentsContainer);

        // Add Comment Input
        LinearLayout inputLayout = new LinearLayout(this);
        inputLayout.setOrientation(LinearLayout.HORIZONTAL);
        inputLayout.setPadding(0, dp(12), 0, 0);

        final EditText etNewComment = new EditText(this);
        etNewComment.setHint(getString(R.string.add_comment_hint));
        etNewComment.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        etNewComment.setBackgroundResource(R.drawable.bg_input_field);
        etNewComment.setPadding(dp(12), dp(8), dp(12), dp(8));
        LinearLayout.LayoutParams etLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        inputLayout.addView(etNewComment, etLp);

        Button btnSend = new Button(this);
        btnSend.setText("שלח");
        btnSend.setTextColor(Color.WHITE);
        btnSend.setBackgroundResource(R.drawable.bg_button_primary);
        btnSend.setPadding(dp(14), dp(6), dp(14), dp(6));
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentUserEmail == null) {
                    Toast.makeText(MainActivity.this, "יש להתחבר עם Google כדי להגיב", Toast.LENGTH_SHORT).show();
                    showGoogleSignInDialog();
                    return;
                }
                String text = etNewComment.getText().toString().trim();
                if (TextUtils.isEmpty(text)) return;

                dbHelper.addComment(post.id, currentUserName, text);
                etNewComment.setText("");
                refreshCommentsList(post.id, commentsContainer);
                adapter.notifyDataSetChanged();
            }
        });
        inputLayout.addView(btnSend);

        mainView.addView(inputLayout);
        builder.setView(mainView);
        builder.setPositiveButton("סגור", null);
        builder.show();
    }

    private void refreshCommentsList(long postId, LinearLayout container) {
        container.removeAllViews();
        List<DatabaseHelper.CommentItem> comments = dbHelper.getComments(postId);
        if (comments.isEmpty()) {
            TextView emptyTv = new TextView(this);
            emptyTv.setText("עדיין אין תגובות. היה הראשון להגיב!");
            emptyTv.setPadding(dp(8), dp(16), dp(8), dp(16));
            emptyTv.setTextColor(Color.parseColor("#9E9E9E"));
            emptyTv.setGravity(Gravity.CENTER);
            container.addView(emptyTv);
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm, dd/MM", Locale.getDefault());
        for (DatabaseHelper.CommentItem c : comments) {
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setBackgroundResource(R.drawable.bg_card);
            item.setPadding(dp(12), dp(8), dp(12), dp(8));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, dp(4), 0, dp(4));
            item.setLayoutParams(lp);

            // Author + Time
            LinearLayout top = new LinearLayout(this);
            top.setOrientation(LinearLayout.HORIZONTAL);

            TextView authorTv = new TextView(this);
            authorTv.setText(c.author);
            authorTv.setTypeface(Typeface.DEFAULT_BOLD);
            authorTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            authorTv.setTextColor(Color.parseColor("#1565C0"));
            LinearLayout.LayoutParams aLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            top.addView(authorTv, aLp);

            TextView timeTv = new TextView(this);
            timeTv.setText(sdf.format(new Date(c.timestamp)));
            timeTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            timeTv.setTextColor(Color.parseColor("#9E9E9E"));
            top.addView(timeTv);

            item.addView(top);

            TextView bodyTv = new TextView(this);
            bodyTv.setText(c.content);
            bodyTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            bodyTv.setTextColor(Color.parseColor("#212121"));
            bodyTv.setPadding(0, dp(4), 0, 0);
            item.addView(bodyTv);

            container.addView(item);
        }
    }

    private class PostsAdapter extends BaseAdapter {

        private SimpleDateFormat sdf = new SimpleDateFormat("d בMMMM, HH:mm", new Locale("he", "IL"));

        @Override
        public int getCount() {
            return displayedPosts.size();
        }

        @Override
        public Object getItem(int position) {
            return displayedPosts.get(position);
        }

        @Override
        public long getItemId(int position) {
            return displayedPosts.get(position).id;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            final Post post = displayedPosts.get(position);

            LinearLayout card = new LinearLayout(MainActivity.this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(post.isPinned ? R.drawable.bg_pinned_card : R.drawable.bg_card);
            card.setPadding(dp(16), dp(14), dp(16), dp(14));

            // Top Header: Tag, Pinned Icon, Date & Admin Menu
            LinearLayout headerRow = new LinearLayout(MainActivity.this);
            headerRow.setOrientation(LinearLayout.HORIZONTAL);
            headerRow.setGravity(Gravity.CENTER_VERTICAL);

            // Badge / Tag
            TextView badge = new TextView(MainActivity.this);
            badge.setText(post.isPinned ? "📌 נעוץ • " + post.category : post.category);
            badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            badge.setTextColor(post.isPinned ? Color.parseColor("#E65100") : Color.parseColor("#1565C0"));
            badge.setTypeface(Typeface.DEFAULT_BOLD);
            LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            headerRow.addView(badge, badgeLp);

            // Timestamp
            TextView tvTime = new TextView(MainActivity.this);
            tvTime.setText(sdf.format(new Date(post.timestamp)));
            tvTime.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            tvTime.setTextColor(Color.parseColor("#9E9E9E"));
            headerRow.addView(tvTime);

            // Admin Options Icon
            if (isAdmin) {
                TextView menuBtn = new TextView(MainActivity.this);
                menuBtn.setText(" ⚙️");
                menuBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                menuBtn.setPadding(dp(6), 0, dp(6), 0);
                menuBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showPostAdminOptions(post);
                    }
                });
                headerRow.addView(menuBtn);
            }

            card.addView(headerRow);

            // Post Text
            TextView tvContent = new TextView(MainActivity.this);
            tvContent.setText(post.content);
            tvContent.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            tvContent.setTextColor(Color.parseColor("#212121"));
            tvContent.setLineSpacing(dp(3), 1.1f);
            tvContent.setPadding(0, dp(8), 0, dp(10));
            card.addView(tvContent);

            // If it's a Poll, render Poll view
            if (!TextUtils.isEmpty(post.pollQuestion)) {
                card.addView(createPollView(post));
            }

            // Reactions Bar (Emoji counters)
            card.addView(createReactionsBar(post));

            // Footer: Views, Comments count, Bookmark
            LinearLayout footer = new LinearLayout(MainActivity.this);
            footer.setOrientation(LinearLayout.HORIZONTAL);
            footer.setGravity(Gravity.CENTER_VERTICAL);
            footer.setPadding(0, dp(10), 0, 0);

            // Views
            TextView tvViews = new TextView(MainActivity.this);
            tvViews.setText("👁️ " + post.views);
            tvViews.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            tvViews.setTextColor(Color.parseColor("#78909C"));
            footer.addView(tvViews);

            // Spacer
            View spacer = new View(MainActivity.this);
            LinearLayout.LayoutParams spLp = new LinearLayout.LayoutParams(0, 1, 1.0f);
            footer.addView(spacer, spLp);

            // Comments Button
            int commentsCount = dbHelper.getCommentCount(post.id);
            Button btnComments = new Button(MainActivity.this);
            btnComments.setText("💬 " + (commentsCount > 0 ? commentsCount + " תגובות" : "תגובות"));
            btnComments.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            btnComments.setBackgroundResource(R.drawable.bg_chip);
            btnComments.setTextColor(Color.parseColor("#37474F"));
            btnComments.setPadding(dp(12), dp(4), dp(12), dp(4));
            btnComments.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showCommentsDialog(post);
                }
            });
            footer.addView(btnComments);

            // Bookmark Button
            final boolean isSaved = dbHelper.isSaved(post.id, currentUserEmail);
            final Button btnSave = new Button(MainActivity.this);
            btnSave.setText(isSaved ? "⭐ שמור" : "☆ שמירה");
            btnSave.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            btnSave.setBackgroundResource(R.drawable.bg_chip);
            btnSave.setTextColor(isSaved ? Color.parseColor("#F57F17") : Color.parseColor("#37474F"));
            LinearLayout.LayoutParams saveLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            saveLp.setMargins(dp(8), 0, 0, 0);
            btnSave.setLayoutParams(saveLp);
            btnSave.setPadding(dp(12), dp(4), dp(12), dp(4));
            btnSave.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (currentUserEmail == null) {
                        Toast.makeText(MainActivity.this, "יש להתחבר עם Google כדי לשמור פוסטים", Toast.LENGTH_SHORT).show();
                        showGoogleSignInDialog();
                        return;
                    }
                    boolean nowSaved = dbHelper.toggleSaved(post.id, currentUserEmail);
                    btnSave.setText(nowSaved ? "⭐ שמור" : "☆ שמירה");
                    btnSave.setTextColor(nowSaved ? Color.parseColor("#F57F17") : Color.parseColor("#37474F"));
                    if (currentTab == 2) {
                        loadPosts();
                    }
                }
            });
            footer.addView(btnSave);

            card.addView(footer);

            return card;
        }
    }

    private View createPollView(final Post post) {
        LinearLayout pollBox = new LinearLayout(this);
        pollBox.setOrientation(LinearLayout.VERTICAL);
        pollBox.setBackgroundColor(Color.parseColor("#F8F9FA"));
        pollBox.setPadding(dp(12), dp(10), dp(12), dp(10));

        TextView qTv = new TextView(this);
        qTv.setText("📊 " + post.pollQuestion);
        qTv.setTypeface(Typeface.DEFAULT_BOLD);
        qTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        qTv.setTextColor(Color.parseColor("#263238"));
        qTv.setPadding(0, 0, 0, dp(8));
        pollBox.addView(qTv);

        final int totalVotes = post.pollVotes1 + post.pollVotes2;
        int pct1 = totalVotes > 0 ? (post.pollVotes1 * 100 / totalVotes) : 0;
        int pct2 = totalVotes > 0 ? (post.pollVotes2 * 100 / totalVotes) : 0;

        // Option 1
        Button opt1Btn = new Button(this);
        opt1Btn.setText(post.pollOpt1 + " (" + pct1 + "%)");
        opt1Btn.setBackgroundResource(R.drawable.bg_button_google);
        opt1Btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        opt1Btn.setGravity(Gravity.CENTER);
        opt1Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentUserEmail == null) {
                    Toast.makeText(MainActivity.this, "יש להתחבר עם Google כדי להצביע בסקר", Toast.LENGTH_SHORT).show();
                    showGoogleSignInDialog();
                    return;
                }
                dbHelper.votePoll(post.id, 1);
                loadPosts();
                Toast.makeText(MainActivity.this, "הצבעתך נקלטה! ✅", Toast.LENGTH_SHORT).show();
            }
        });
        pollBox.addView(opt1Btn);

        // Option 2
        Button opt2Btn = new Button(this);
        opt2Btn.setText(post.pollOpt2 + " (" + pct2 + "%)");
        opt2Btn.setBackgroundResource(R.drawable.bg_button_google);
        opt2Btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        opt2Btn.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams opt2Lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        opt2Lp.setMargins(0, dp(6), 0, 0);
        opt2Btn.setLayoutParams(opt2Lp);
        opt2Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentUserEmail == null) {
                    Toast.makeText(MainActivity.this, "יש להתחבר עם Google כדי להצביע בסקר", Toast.LENGTH_SHORT).show();
                    showGoogleSignInDialog();
                    return;
                }
                dbHelper.votePoll(post.id, 2);
                loadPosts();
                Toast.makeText(MainActivity.this, "הצבעתך נקלטה! ✅", Toast.LENGTH_SHORT).show();
            }
        });
        pollBox.addView(opt2Btn);

        return pollBox;
    }

    private View createReactionsBar(final Post post) {
        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setPadding(0, dp(6), 0, dp(6));

        final String[] emojis = new String[]{"👍", "❤️", "🔥", "👏", "💡"};

        for (final String emoji : emojis) {
            int count = dbHelper.getReactionCount(post.id, emoji);
            boolean reacted = dbHelper.hasUserReacted(post.id, currentUserEmail, emoji);

            final Button btn = new Button(this);
            btn.setText(emoji + (count > 0 ? " " + count : ""));
            btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            btn.setBackgroundResource(reacted ? R.drawable.bg_reaction_active : R.drawable.bg_reaction);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, dp(6), 0);
            btn.setLayoutParams(lp);
            btn.setPadding(dp(10), dp(2), dp(10), dp(2));

            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (currentUserEmail == null) {
                        Toast.makeText(MainActivity.this, "יש להתחבר עם Google כדי להוסיף Reaction", Toast.LENGTH_SHORT).show();
                        showGoogleSignInDialog();
                        return;
                    }
                    dbHelper.toggleReaction(post.id, currentUserEmail, emoji);
                    loadPosts();
                }
            });

            layout.addView(btn);
        }

        hsv.addView(layout);
        return hsv;
    }

    private void showPostAdminOptions(final Post post) {
        final String[] options = new String[]{
                post.isPinned ? "בטל נעיצה 📌" : "נעץ בראש הערוץ 📌",
                "מחק עדכון 🗑️"
        };

        new AlertDialog.Builder(this)
                .setTitle("ניהול עדכון (מנהל בלבד)")
                .setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            dbHelper.togglePin(post.id, post.isPinned);
                            loadPosts();
                        } else if (which == 1) {
                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("מחיקת עדכון")
                                    .setMessage("האם אתה בטוח שברצונך למחוק עדכון זה?")
                                    .setPositiveButton("מחק", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface d, int w) {
                                            dbHelper.deletePost(post.id);
                                            loadPosts();
                                            Toast.makeText(MainActivity.this, "העדכון נמחק", Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .setNegativeButton("ביטול", null)
                                    .show();
                        }
                    }
                })
                .show();
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }
}
