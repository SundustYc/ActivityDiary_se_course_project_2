package de.rampro.activitydiary.ui.main;



import android.Manifest;
import android.app.SearchManager;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.format.DateFormat;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import de.rampro.activitydiary.ActivityDiaryApplication;
import de.rampro.activitydiary.BuildConfig;
import de.rampro.activitydiary.R;
import de.rampro.activitydiary.db.ActivityDiaryContract;
import de.rampro.activitydiary.helpers.ActivityHelper;
import de.rampro.activitydiary.helpers.DateHelper;
import de.rampro.activitydiary.helpers.GraphicsHelper;
import de.rampro.activitydiary.helpers.ImageRecordHelper;
import de.rampro.activitydiary.helpers.TimeSpanFormatter;
import de.rampro.activitydiary.helpers.VideoRecordHelper;
import de.rampro.activitydiary.model.DetailViewModel;
import de.rampro.activitydiary.model.DiaryActivity;
import de.rampro.activitydiary.ui.generic.BaseActivity;
import de.rampro.activitydiary.ui.generic.EditActivity;
import de.rampro.activitydiary.ui.history.HistoryDetailActivity;
import de.rampro.activitydiary.ui.settings.SettingsActivity;
import de.rampro.activitydiary.db.VideoDb;

public class MainActivity extends BaseActivity implements
        SelectRecyclerViewAdapter.SelectListener,
        ActivityHelper.DataChangedListener,
        NoteEditDialog.NoteEditDialogListener,
        View.OnLongClickListener,
        SearchView.OnQueryTextListener,
        SearchView.OnCloseListener
{
    /**
     *MainActivity 根据fragments的切换显示大部分用户界面
     */
    private static final String TAG = MainActivity.class.getSimpleName();
    //在日志输出或调试信息中标识出相关类的名称

    private static final int REQUEST_IMAGE_CAPTURE = 1; //图片请求
    private static final int REQUEST_VIDEO_CAPTURE = 2;

    private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 4711;//存储权限

    private static final int QUERY_CURRENT_ACTIVITY_STATS = 1;
    private static final int QUERY_CURRENT_ACTIVITY_TOTAL = 2;
    //活动的统计
    private DetailViewModel viewModel;//详细信息的视图模型
    private String mCurrentPhotoPath;//当前图片的路径

    private String mCurrentVideoPath;
    private RecyclerView selectRecyclerView;//选择的视图
    private StaggeredGridLayoutManager selectorLayoutManager;//选择的布局管理器
    private SelectRecyclerViewAdapter selectAdapter;//选择的适配器
    private String filter = "";//过滤器
    private int searchRowCount, normalRowCount;//搜索的行数和正常的行数
    private FloatingActionButton fabNoteEdit;//编辑的按钮

    private FloatingActionButton fabAttachPicture;//附加图片的按钮
    private FloatingActionButton fabAttachVideo;//附加视频的按钮
    private SearchView searchView;//搜索的视图
    private MenuItem searchMenuItem;//搜索的菜单项
    private ViewPager viewPager;//视图的翻页
    private TabLayout tabLayout;//标签的布局
    private View headerView;//头部的视图

    private VideoDb videoDb;

    VideoRecordHelper videoRecordHelper = new VideoRecordHelper();
    /**
     *设置搜索时某些组件的显示与隐藏
     */
    private void setSearchMode(boolean searchMode){

        if(searchMode)
        {
            headerView.setVisibility(View.GONE);//隐藏头部的视图
            fabNoteEdit.hide();//隐藏编辑的按钮
            fabAttachPicture.hide();//隐藏附加图片的按钮
            fabAttachVideo.hide();
            //设置软键盘的模式
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            //设置布局管理器的每行的列数
            ((StaggeredGridLayoutManager) Objects.requireNonNull(selectRecyclerView.getLayoutManager())).setSpanCount(searchRowCount);
        }
        else
        {
            //设置布局管理器的每行的列数
            ((StaggeredGridLayoutManager) Objects.requireNonNull(selectRecyclerView.getLayoutManager())).setSpanCount(normalRowCount);
            //设置软键盘的模式
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
            headerView.setVisibility(View.VISIBLE);//显示头部的视图
            fabNoteEdit.show();//显示编辑的按钮
            fabAttachPicture.show();//显示附加图片的按钮
            fabAttachVideo.show();
        }

    }

    //异步查询处理程序
    private MainAsyncQueryHandler mQHandler =
            new MainAsyncQueryHandler(ActivityDiaryApplication.getAppContext().getContentResolver());


    /**
     *在活动被销毁时调用
     *保存当前图片的路径
     *调用超类来保存视图层次结构/实例状态
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("currentVideoPath", mCurrentVideoPath);
        outState.putString("currentPhotoPath", mCurrentPhotoPath);
        super.onSaveInstanceState(outState);
    }

    /**
     *在活动创建时调用
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        /**
         *原(已弃用)：viewModel = ViewModelProviders.of(this).get(DetailViewModel.class);
         *设置内容视图
         *使用 ViewModelProvider 创建并获取 DetailViewModel 的实例
         *ViewModel 用于存储与 UI 相关的数据，以便在配置更改（如屏幕旋转）时保持数据的一致性。
         */
        videoDb = new VideoDb(this);
        viewModel = new ViewModelProvider(this).get(DetailViewModel.class);
        if (savedInstanceState != null) {
            // 从保存的实例状态恢复当前图片的路径
            mCurrentPhotoPath = savedInstanceState.getString("currentPhotoPath");
            mCurrentVideoPath = savedInstanceState.getString("currentVideoPath");
        }
        //获取布局填充器，用于将 XML 布局文件转换为 View 对象。
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        //使用布局填充器将 R.layout.activity_main_content 转换为 View 对象。这是设置活动内容视图的一部分
        View contentView = inflater.inflate(R.layout.activity_main_content, null, false);
        //设置活动的内容视图为 contentView
        setContent(contentView);

        //通过查找 XML 布局文件中定义的视图 ID 获取对应的视图对象。
        headerView = findViewById(R.id.header_area);
        tabLayout = findViewById(R.id.tablayout);
        viewPager = findViewById(R.id.viewpager);
        selectRecyclerView = findViewById(R.id.select_recycler);
        View selector = findViewById(R.id.activity_background);

        //设置视图翻页   详见setupViewPager方法
        setupViewPager(viewPager);
        //设置标签布局与视图翻页的关联
        tabLayout.setupWithViewPager(viewPager);
        //设置长按事件的监听器
        selector.setOnLongClickListener(this);
        /**
         *设置选择的适配器
         *设置选择的监听器
         *TODO: get rid of this setting?
         */
        selector.setOnClickListener(v -> {

            if(PreferenceManager
                    .getDefaultSharedPreferences(ActivityDiaryApplication.getAppContext())
                    .getBoolean(SettingsActivity.KEY_PREF_DISABLE_CURRENT, true)){
                ActivityHelper.helper.setCurrentActivity(null);
            }else{
                Intent i = new Intent(MainActivity.this, HistoryDetailActivity.class);
                startActivity(i);
            }
        });

        TypedValue value = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.listPreferredItemHeightSmall, value, true);

        android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        normalRowCount = (int)Math.floor((metrics.heightPixels / value.getDimension(metrics) - 2) / 2);
        searchRowCount = normalRowCount - 2;
        if(searchRowCount <= 0) searchRowCount = 1;

        selectorLayoutManager = new StaggeredGridLayoutManager(normalRowCount, StaggeredGridLayoutManager.HORIZONTAL);
        selectRecyclerView.setLayoutManager(selectorLayoutManager);
        Objects.requireNonNull(getSupportActionBar()).setSubtitle(getResources().getString(R.string.activity_subtitle_main));

        likelihoodSort();

        // 处理Edit按钮
        fabNoteEdit = findViewById(R.id.fab_edit_note);
        fabNoteEdit.setOnClickListener(v -> {
            if(viewModel.currentActivity().getValue() != null) {
                NoteEditDialog dialog = new NoteEditDialog();
                dialog.setText(viewModel.mNote.getValue());
                dialog.show(getSupportFragmentManager(), "NoteEditDialogFragment");
            }
            else
                Toast.makeText(MainActivity.this, getResources().getString(R.string.no_active_activity_error), Toast.LENGTH_LONG).show();
        });

        // 处理Picture按钮
        fabAttachPicture = findViewById(R.id.fab_attach_picture);
        fabAttachPicture.setOnClickListener(v -> {
            if(viewModel.currentActivity().getValue()!= null)
            {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null)
                {
                    File photoFile = null;
                    try
                    {
                        photoFile = createImageFile();
                        Log.i(TAG, "create file for image capture " + (photoFile == null ? "" : photoFile.getAbsolutePath()));
                    }
                    catch (IOException ex)
                    {
                        Toast.makeText(MainActivity.this, getResources().getString(R.string.camera_error), Toast.LENGTH_LONG).show();
                    }
                    if (photoFile != null)
                    {
                        mCurrentPhotoPath = photoFile.getAbsolutePath();
                        Uri photoUri = FileProvider.getUriForFile(MainActivity.this,
                                "de.rampro.activitydiary.fileprovider",
                                photoFile);
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                        takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                    }
                }
                else
                    Toast.makeText(MainActivity.this, getResources().getString(R.string.no_available_cameras), Toast.LENGTH_LONG).show();
            }
            else
                Toast.makeText(MainActivity.this, getResources().getString(R.string.no_active_activity_error), Toast.LENGTH_LONG).show();
        });

        fabNoteEdit.show();
        PackageManager pm = getPackageManager();

        if(pm.hasSystemFeature(PackageManager.FEATURE_CAMERA))
            fabAttachPicture.show();
        else
            fabAttachPicture.hide();

        fabAttachVideo = findViewById(R.id.fab_attach_video);
        fabAttachVideo.setOnClickListener(v -> {
            if(viewModel.currentActivity().getValue()!= null)
            {
                Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                File videoFile = null;
                try
                {
                    videoFile = createVideoFile();
                    Log.i(TAG, "create file for video capture " + (videoFile == null ? "" : videoFile.getAbsolutePath()));
                }
                catch (IOException ex)
                {
                    Toast.makeText(MainActivity.this, getResources().getString(R.string.video_error), Toast.LENGTH_LONG).show();
                }
                if (videoFile != null)
                {
                    mCurrentVideoPath=videoFile.getAbsolutePath();
                    Uri videoUri = FileProvider.getUriForFile(MainActivity.this,
                            "de.rampro.activitydiary.fileprovider",
                            videoFile);
                    takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, videoUri);
                    startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE);
                }
            }
            else
                Toast.makeText(MainActivity.this, getResources().getString(R.string.no_active_activity_error), Toast.LENGTH_LONG).show();
        });


        //获取Intent、验证操作并获取搜索查询
        Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            filterActivityView(query);
        }

        // 在这里调用onActivityChanged会重新加载统计数据并重新填充视图模型,违背了视图模型的理念
        onActivityChanged(); // 在最后执行此操作，以确保在完成数据加载之前没有加载器

    }

    /**
     *创建一个文件用于保存图片
     */
    private File createImageFile() throws IOException
    {
        //文件名
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "IMG_";
        imageFileName += viewModel.currentActivity().getValue().getName();
        imageFileName += "_";
        imageFileName += timeStamp;
        File storageDir;
        //检查是否有存储权限
        int permissionCheck = ContextCompat.checkSelfPermission(ActivityDiaryApplication.getAppContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if(permissionCheck == PackageManager.PERMISSION_GRANTED)
        {
            storageDir = ImageRecordHelper.imageStorageDirectory();
        }
        else
        {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE))
            {
                Toast.makeText(this,R.string.perm_write_external_storage_xplain, Toast.LENGTH_LONG).show();
            }
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
            storageDir = null;
        }

        if(storageDir != null)
        {
            File image = new File(storageDir, imageFileName + ".jpg");
            //image.createNewFile();
            return image;
        }
        else
            return null;
    }


    private File createVideoFile() throws IOException {
        File storageDir;
        if (ContextCompat.checkSelfPermission(ActivityDiaryApplication.getAppContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
        {
            storageDir = videoRecordHelper.videoStorageDirectory();
        }
        else
        {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE))
            {
                Toast.makeText(this, R.string.perm_write_external_storage_xplain, Toast.LENGTH_LONG).show();
            }
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
            storageDir = null;
        }
        if (storageDir != null)
        {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String videoFileName = "Video_";
            videoFileName += viewModel.currentActivity().getValue().getName();
            videoFileName += "_";
            videoFileName += timeStamp;
            File video = new File(storageDir, videoFileName + ".mp4");
            return video;
        }
        else
            return null;
    }

    @Override
    public void onResume() {
        mNavigationView.getMenu().findItem(R.id.nav_main).setChecked(true);
        ActivityHelper.helper.registerDataChangeListener(this);
        onActivityChanged(); //刷新当前活动数据
        super.onResume();
        selectAdapter.notifyDataSetChanged(); // redraw全部的RecyclerView
        ActivityHelper.helper.evaluateAllConditions();
    }

    /**
     * 用户离开当前活动时调用
     */
    @Override
    public void onPause() {

        ActivityHelper.helper.unregisterDataChangeListener(this);
        super.onPause();
    }

    /**
     *长按的处理
     */
    @Override
    public boolean onLongClick(View view) {
        Intent i = new Intent(MainActivity.this, EditActivity.class);
        //创建一个Intent
        if(viewModel.currentActivity().getValue() != null) {
            i.putExtra("activityID", Objects.requireNonNull(viewModel.currentActivity().getValue()).getId());
            //将当前活动的ID放入Intent中
        }
        startActivity(i);
        //启动活动
        return true;
    }

    /**
     * 长按Item的处理
     */
    @Override
    public boolean onItemLongClick(int adapterPosition){

        Intent i = new Intent(MainActivity.this, EditActivity.class);
        i.putExtra("activityID", selectAdapter.item(adapterPosition).getId());
        startActivity(i);
        return true;
    }

    /**
     * 点击Item的处理
     */
    @Override
    public void onItemClick(int adapterPosition) {
        DiaryActivity newAct = selectAdapter.item(adapterPosition);
        if(newAct != ActivityHelper.helper.getCurrentActivity()) {
            ActivityHelper.helper.setCurrentActivity(newAct);
            searchView.setQuery("", false);
            searchView.setIconified(true);
            SpannableStringBuilder snackBarText = new SpannableStringBuilder();
            snackBarText.append(newAct.getName());
            int end = snackBarText.length();
            snackBarText.setSpan(new ForegroundColorSpan(newAct.getColor()), 0, end, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            snackBarText.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, end, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            snackBarText.setSpan(new RelativeSizeSpan((float) 1.4152), 0, end, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            //SnackBar:在移动设备的屏幕底部显示一条简短消息,出现在屏幕上所有其他元素的上方
            Snackbar undoSnackBar = Snackbar.make(findViewById(R.id.main_layout),
                    snackBarText, Snackbar.LENGTH_LONG);
            undoSnackBar.setAction(R.string.action_undo, new View.OnClickListener()
            {
                /**
                 *Called when a view has been clicked
                 */
                @Override
                public void onClick(View v) {
                    Log.v(TAG, "UNDO Activity Selection");
                    ActivityHelper.helper.undoLastActivitySelection();
                }
            });
            undoSnackBar.show();
        }else{
            //单击列表中的当前活动，终止该活动
            ActivityHelper.helper.setCurrentActivity(null);
        }
    }

    public void onActivityChanged(){
        DiaryActivity newAct = ActivityHelper.helper.getCurrentActivity();
        viewModel.mCurrentActivity.setValue(newAct);
        if(newAct != null) {
            mQHandler.startQuery(QUERY_CURRENT_ACTIVITY_STATS, null,
                    ActivityDiaryContract.DiaryActivity.CONTENT_URI,
                    new String[] {
                            ActivityDiaryContract.DiaryActivity._ID,
                            ActivityDiaryContract.DiaryActivity.NAME,
                            ActivityDiaryContract.DiaryActivity.X_AVG_DURATION,
                            ActivityDiaryContract.DiaryActivity.X_START_OF_LAST
                    },
                    ActivityDiaryContract.DiaryActivity._DELETED + " = 0 AND "
                    + ActivityDiaryContract.DiaryActivity._ID + " = ?",
                    new String[] {
                            Integer.toString(newAct.getId())
                    },
                    null);
            queryAllTotals();
        }

        viewModel.setCurrentDiaryUri(ActivityHelper.helper.getCurrentDiaryUri());
        TextView aName = findViewById(R.id.activity_name);
        // TODO: move this logic into the DetailViewModel??

        viewModel.mAvgDuration.setValue("-");
        viewModel.mStartOfLast.setValue("-");
        viewModel.mTotalToday.setValue("-");

        if(viewModel.currentActivity().getValue() != null) {
            aName.setText(Objects.requireNonNull(viewModel.currentActivity().getValue()).getName());
            findViewById(R.id.activity_background).setBackgroundColor(Objects.requireNonNull(viewModel.currentActivity().getValue()).getColor());
            aName.setTextColor(GraphicsHelper.textColorOnBackground(Objects.requireNonNull(viewModel.currentActivity().getValue()).getColor()));
            viewModel.mNote.setValue(ActivityHelper.helper.getCurrentNote());
        }else{
            int col;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                col = ActivityDiaryApplication.getAppContext().getResources().getColor(R.color.colorPrimary, null);
            }else {
                col = ActivityDiaryApplication.getAppContext().getResources().getColor(R.color.colorPrimary);
            }
            aName.setText(getResources().getString(R.string.activity_title_no_selected_act));
            findViewById(R.id.activity_background).setBackgroundColor(col);
            aName.setTextColor(GraphicsHelper.textColorOnBackground(col));
            viewModel.mDuration.setValue("-");
            viewModel.mNote.setValue("");
        }
        //在 mQHelper 中查询完成后更新统计量
        selectorLayoutManager.scrollToPosition(0);
    }

    public void queryAllTotals() {
        // TODO: move this into the DetailStatFragment
        DiaryActivity a = viewModel.mCurrentActivity.getValue();
        if(a != null) {
            int id = a.getId();

            long end = System.currentTimeMillis();
            queryTotal(Calendar.DAY_OF_YEAR, end, id);
            queryTotal(Calendar.WEEK_OF_YEAR, end, id);
            queryTotal(Calendar.MONTH, end, id);
        }
    }

    private void queryTotal(int field, long end, int actID) {
        Calendar calStart = DateHelper.startOf(field, end);
        long start = calStart.getTimeInMillis();
        Uri u = ActivityDiaryContract.DiaryStats.CONTENT_URI;
        u = Uri.withAppendedPath(u, Long.toString(start));
        u = Uri.withAppendedPath(u, Long.toString(end));

        mQHandler.startQuery(QUERY_CURRENT_ACTIVITY_TOTAL, new StatParam(field, end),
                u,
                new String[] {
                        ActivityDiaryContract.DiaryStats.DURATION
                },
                ActivityDiaryContract.DiaryActivity.TABLE_NAME + "." + ActivityDiaryContract.DiaryActivity._ID
                        + " = ?",
                new String[] {
                        Integer.toString(actID)
                },
                null);
    }


    @Override
    public void onActivityOrderChanged() {
        // 只有在不进行搜索时才会进行可能性排序(由于可能性而更改活动顺序时调用)
        if(filter.length() == 0){
            likelihoodSort();
        }
    }

    /**
     *Called when the data has changed.
     */
    @Override
    public void onActivityDataChanged() {
        selectAdapter.notifyDataSetChanged();
    }

    @Override
    public void onActivityDataChanged(DiaryActivity activity){
        selectAdapter.notifyItemChanged(selectAdapter.positionOf(activity));
    }

    /**
     *Called on addition of an activity.
     */
    @Override
    public void onActivityAdded(DiaryActivity activity) {
        /* no need to add it, as due to the reevaluation of the conditions the order change will happen */
    }

    /**
     *在删除活动时调用
     */
    @Override
    public void onActivityRemoved(DiaryActivity activity) {
        selectAdapter.notifyDataSetChanged();
    }

    /**
     * 当数据发生变化时调用
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        // 获取SearchView并设置可搜索的配置
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchMenuItem = menu.findItem(R.id.action_filter);
        searchView = (SearchView) searchMenuItem.getActionView();
        // 假设当前活动为可搜索活动，设置该SearchView显示搜索按钮
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setOnCloseListener(this);
        searchView.setOnQueryTextListener(this);
        // setOnSuggestionListener -> for selection of a suggestion
        // setSuggestionsAdapter
        searchView.setOnSearchClickListener(v -> setSearchMode(true));
        return true;
    }

    /**
     *Called when the user selects an item from the options menu.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.action_add_activity:
                startActivity(new Intent(this, EditActivity.class));
                break;
            /* filtering is handled by the SearchView widget
            case R.id.action_filter:
            */
        }
        return super.onOptionsItemSelected(item);
    }

    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            filterActivityView(query);
        }

        if (intent.hasExtra("SELECT_ACTIVITY_WITH_ID")) {
            int id = intent.getIntExtra("SELECT_ACTIVITY_WITH_ID", -1);
            ActivityHelper.helper.setCurrentActivity(ActivityHelper.helper.activityWithId(id));
        }
    }

    private void filterActivityView(String query){
        this.filter = query;
        if(filter.length() == 0){
            likelihoodSort();
        }else {
            ArrayList<DiaryActivity> filtered = ActivityHelper.helper.sortedActivities(query);

            selectAdapter = new SelectRecyclerViewAdapter(MainActivity.this, filtered);
            selectRecyclerView.swapAdapter(selectAdapter, false);
            selectRecyclerView.scrollToPosition(0);
        }
    }

    private void likelihoodSort() {
        selectAdapter = new SelectRecyclerViewAdapter(MainActivity.this, ActivityHelper.helper.getActivities());
        selectRecyclerView.swapAdapter(selectAdapter, false);
    }

    @Override
    public boolean onClose() {
        setSearchMode(false);
        likelihoodSort();
        return false; // clear and close the search
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        setSearchMode(false);
        return false;
    }

    /**
     * 当查询文本更改时调用
     */
    @Override
    public boolean onQueryTextChange(String newText) {
        filterActivityView(newText);
        return true;
    }

    /**
     *当用户点击对话框的取消按钮时调用
     */
    @Override
    public void onNoteEditPositiveClock(String str, DialogFragment dialog) {
        ContentValues values = new ContentValues();
        values.put(ActivityDiaryContract.Diary.NOTE, str);

        mQHandler.startUpdate(0,
                null,
                viewModel.getCurrentDiaryUri(),
                values,
                null, null);

        viewModel.mNote.postValue(str);
        ActivityHelper.helper.setCurrentNote(str);
    }

    /**
     *当活动返回结果时调用
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            if (mCurrentPhotoPath != null && viewModel.getCurrentDiaryUri() != null) {
                Uri photoURI = FileProvider.getUriForFile(MainActivity.this,
                        "de.rampro.activitydiary.fileprovider",
                        new File(mCurrentPhotoPath));
                ContentValues values = new ContentValues();
                values.put(ActivityDiaryContract.DiaryImage.URI, photoURI.toString());
                values.put(ActivityDiaryContract.DiaryImage.DIARY_ID, viewModel.getCurrentDiaryUri().getLastPathSegment());

                mQHandler.startInsert(0,
                        null,
                        ActivityDiaryContract.DiaryImage.CONTENT_URI,
                        values);

                if (PreferenceManager
                        .getDefaultSharedPreferences(ActivityDiaryApplication.getAppContext())
                        .getBoolean(SettingsActivity.KEY_PREF_TAG_IMAGES, true)) {
                    try {
                        ExifInterface exifInterface = new ExifInterface(mCurrentPhotoPath);
                        if (viewModel.currentActivity().getValue() != null) {
                            exifInterface.setAttribute(ExifInterface.TAG_USER_COMMENT, Objects.requireNonNull(viewModel.currentActivity().getValue()).getName());
                            exifInterface.saveAttributes();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "writing exif data to " + mCurrentPhotoPath + " failed", e);
                    }
                }
            }
        }
        if (requestCode == REQUEST_VIDEO_CAPTURE && resultCode == RESULT_OK) {
            Uri videoUri = FileProvider.getUriForFile(MainActivity.this,
                    "de.rampro.activitydiary.fileprovider",
                    new File(mCurrentVideoPath));
            String activityId = Long.toString(viewModel.mDiaryEntryId.getValue());
            try {
                videoDb.insertVideo(activityId, videoUri);
            }
            catch (Exception e)
            {
                Log.e(TAG, "Error in insertVideo: " + e.getMessage());
                throw e;
            }
        }
    }


    private void setupViewPager(ViewPager viewPager) {
        /**
         *设置视图翻页的适配器
         */
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new DetailStatFragement(), getResources().getString(R.string.fragment_detail_stats_title));
        adapter.addFragment(new DetailNoteFragment(), getResources().getString(R.string.fragment_detail_note_title));
        adapter.addFragment(new DetailPictureFragement(), getResources().getString(R.string.fragment_detail_pictures_title));
        viewPager.setAdapter(adapter);
    }

    /**
     *视图翻页的适配器
     */
    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();
        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }

    /**
     *异步查询处理程序
     */
    private class MainAsyncQueryHandler extends AsyncQueryHandler{

        public MainAsyncQueryHandler(ContentResolver cr) {
            super(cr);
        }
        /**
         * 启动异步查询时调用
         */
        @Override
        public void startQuery(int token, Object cookie, Uri uri, String[] projection, String selection, String[] selectionArgs, String orderBy) {
            super.startQuery(token, cookie, uri, projection, selection, selectionArgs, orderBy);
        }

        /**
         * 当异步查询完成时调用
         */
        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor)
        {
            super.onQueryComplete(token, cookie, cursor);
            if ((cursor != null) && cursor.moveToFirst())
            {
                if (token == QUERY_CURRENT_ACTIVITY_STATS) {
                    int xAvgDuration = cursor.getColumnIndex(ActivityDiaryContract.DiaryActivity.X_AVG_DURATION);
                    long avg=0;
                    if(xAvgDuration >= 0)
                    {
                        avg = cursor.getLong(xAvgDuration);
                    }
                    viewModel.mAvgDuration.setValue(getResources().
                            getString(R.string.avg_duration_description, TimeSpanFormatter.format(avg)));
                    }
                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(ActivityDiaryApplication.getAppContext());
                    String formatString = sharedPref.getString(SettingsActivity.KEY_PREF_DATETIME_FORMAT,
                            getResources().getString(R.string.default_datetime_format));
                    int xStartOfLast = cursor.getColumnIndex(ActivityDiaryContract.DiaryActivity.X_START_OF_LAST);
                    long start;
                    if(xStartOfLast >= 0) {
                        start = cursor.getLong(xStartOfLast);
                        viewModel.mStartOfLast.setValue(getResources().
                                getString(R.string.last_done_description, DateFormat.format(formatString, start)));
                    }
            }
            else if(token == QUERY_CURRENT_ACTIVITY_TOTAL)
                {
                    StatParam p = (StatParam)cookie;
                    int DURATION= 0;
                    if (cursor != null) {
                        DURATION = cursor.getColumnIndex(ActivityDiaryContract.DiaryStats.DURATION);
                    }
                    long total=0;
                    if(DURATION >= 0)
                    {
                        if (cursor != null) {
                            total = cursor.getLong(DURATION);
                        }
                    }
                    String x = DateHelper.dateFormat(p.field).format(p.end);
                    x = x + ": " + TimeSpanFormatter.format(total);
                    switch(p.field){
                        case Calendar.DAY_OF_YEAR:
                            viewModel.mTotalToday.setValue(x);
                            break;
                        case Calendar.WEEK_OF_YEAR:
                            viewModel.mTotalWeek.setValue(x);
                            break;
                        case Calendar.MONTH:
                            viewModel.mTotalMonth.setValue(x);
                            break;
                    }
                }
        }
    }
}

/**
 * 活动的统计
 */
class StatParam {
    public int field;
    public long end;
    public StatParam(int field, long end)
    {
        this.field = field;
        this.end = end;
    }
}
