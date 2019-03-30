package test.best.com.wifitransfer;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.ActionBar;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.widget.Toast;

import com.hwangjr.rxbus.RxBus;
import com.hwangjr.rxbus.annotation.Subscribe;
import com.hwangjr.rxbus.annotation.Tag;
import com.hwangjr.rxbus.thread.EventThread;
import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import static test.best.com.wifitransfer.AppInstallOrDelete.getIconFromPackageName;

public class MainActivity extends AppCompatActivity implements Animator.AnimatorListener{

    private  Toolbar toolbar;
    private  FloatingActionButton fab;
    private RecyclerView mAppList;
    private SwipeRefreshLayout swipeRefresh;
    private List<InfoModel> mApps = new ArrayList<>();
    private AppAdapter myAppAdapter;
    private AppInstallOrDelete appIOD;
    private WifiConnectChangedReceiver mWifiConnectChangedReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        setSupportActionBar(toolbar);
        //开启广播
        registerWifiConnectChangedReceiver();
        initRecyclerView();

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (requestPermission()) {
                    ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(fab, "translationY", 0, fab.getHeight() * 2).setDuration(200);
                    objectAnimator.setInterpolator(new AccelerateInterpolator());
                    objectAnimator.addListener(MainActivity.this);
                    objectAnimator.start();
                }
            }
        });

    }

    private void init(){
        toolbar = findViewById(R.id.toolbar);
        fab = findViewById(R.id.fab);
        mAppList = findViewById(R.id.recyclerview);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        appIOD = new AppInstallOrDelete(this);
        RxBus.get().register(this);
        //初始化广播
        mWifiConnectChangedReceiver = new WifiConnectChangedReceiver();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //加载menu布局
        getMenuInflater().inflate(R.menu.item_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.test_menu:
                if (!mApps.isEmpty()) {
                    showDialog();
                } else {
                    Toast.makeText(MainActivity.this, "暂无可删内容", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                    break;
        }
        return false;
    }

    private void showDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("温馨提示:");
        builder.setMessage("确定全部删除吗？");
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                appIOD.deleteAll();
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        builder.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        RxBus.get().unregister(this);
        unregisterReceiver(mWifiConnectChangedReceiver);
    }

    @Subscribe(tags = {@Tag(Constants.RxBusEventType.POPUP_MENU_DIALOG_SHOW_DISMISS)})
    public void onPopupMenuDialogDismiss(Integer type) {
        if (type == Constants.MSG_DIALOG_DISMISS) {
            ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(fab, "translationY", fab.getHeight() * 2, 0).setDuration(200L);
            objectAnimator.setInterpolator(new AccelerateInterpolator());
            objectAnimator.start();
        }
    }

    @Subscribe(thread = EventThread.IO, tags = {@Tag(Constants.RxBusEventType.LOAD_BOOK_LIST)})
    public void loadAppList(Integer type){
        appIOD.queryAppList(mApps);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                myAppAdapter.notifyDataSetChanged();
                swipeRefresh.setRefreshing(false);
            }
        });
    }

    private void initRecyclerView(){
        appIOD.queryAppList(mApps);
        myAppAdapter = new AppAdapter(mApps, this);
        mAppList.setHasFixedSize(true);
        mAppList.setLayoutManager(new LinearLayoutManager(this));
        mAppList.setAdapter(myAppAdapter);

        swipeRefresh.setColorSchemeColors(ContextCompat.getColor(this, R.color.holo_blue_bright),
                ContextCompat.getColor(this, R.color.holo_green_light),
                ContextCompat.getColor(this, R.color.holo_orange_light),
                ContextCompat.getColor(this, R.color.holo_red_light));

        mAppList.addItemDecoration(new ItemButtomDecoration(this, 10));
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                RxBus.get().post(Constants.RxBusEventType.LOAD_BOOK_LIST, 0);
            }
        });
    }

    @Override
    public void onAnimationStart(Animator animation) {
        PopupMenuDialog dialog = new PopupMenuDialog(this);
        dialog.show();
    }

    @Override
    public void onAnimationEnd(Animator animation) {

    }

    @Override
    public void onAnimationCancel(Animator animation) {

    }

    @Override
    public void onAnimationRepeat(Animator animation) {

    }

    private boolean requestPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            boolean needapply = false;
            for (int i = 0; i < Constants.allpermissions.length; i++) {
                int chechpermission = ContextCompat.checkSelfPermission(getApplicationContext(),
                        Constants.allpermissions[i]);
                if (chechpermission != PackageManager.PERMISSION_GRANTED) {
                    needapply = true;
                }
            }
            if (needapply) {
                ActivityCompat.requestPermissions(MainActivity.this, Constants.allpermissions, 1);
            }
            return !needapply;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int i = 0; i < grantResults.length; i++) {
            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this, permissions[i] + "已授权", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, permissions[i] + "授权通过方能使用", Toast.LENGTH_SHORT).show();
            }
        }
    }

    void registerWifiConnectChangedReceiver() {
        IntentFilter intentFilter = new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(mWifiConnectChangedReceiver, intentFilter);
    }
}
