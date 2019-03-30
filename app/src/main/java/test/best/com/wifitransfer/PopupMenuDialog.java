package test.best.com.wifitransfer;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.hwangjr.rxbus.RxBus;
import com.hwangjr.rxbus.annotation.Subscribe;
import com.hwangjr.rxbus.annotation.Tag;

public class PopupMenuDialog extends Dialog{

    private TextView mTxtTitle;
    private TextView mTxtSubTitle;
    private ImageView mImgLanState;
    private TextView mTxtStateHint;
    private TextView mTxtAddress;
    private Button mBtnWifiSettings;
    private Button mBtnCancel;
    private View mButtonSplitLine;

    private Context context;

    public PopupMenuDialog(Context context) {
        super(context, R.style.PopupMenuDialogStyle);
        this.context = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_popup_menu_dialog);
        //按空白处不能取消动画
        setCanceledOnTouchOutside(false);
        //弹出后会点击屏幕或物理返回键，dialog不消失
        setCancelable(false);
        //初始化界面控件
        initView();
        RxBus.get().register(this);
        //Dialog位置
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        //获取屏幕宽度
        DisplayMetrics dm = new DisplayMetrics();
        getWindow().getWindowManager().getDefaultDisplay().getMetrics(dm);
        lp.width = dm.widthPixels;
        getWindow().setAttributes(lp);
        getWindow().setGravity(Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);

    }

    /**
     * 初始化界面控件
     */
    private void initView() {
        mTxtTitle = findViewById(R.id.popup_menu_title);
        mTxtSubTitle = findViewById(R.id.popup_menu_subtitle);
        mImgLanState = findViewById(R.id.shared_wifi_state);
        mTxtStateHint = findViewById(R.id.shared_wifi_state_hint);
        mTxtAddress = findViewById(R.id.shared_wifi_address);
        mBtnWifiSettings = findViewById(R.id.shared_wifi_settings);
        mBtnCancel = findViewById(R.id.shared_wifi_cancel);
        mButtonSplitLine = findViewById(R.id.shared_wifi_button_split_line);

        mBtnWifiSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                context.startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
            }
        });

        mBtnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

    }

    @Override
    public void show() {
        super.show();
        checkWifiState(WifiUtils.getWifiConnectState(context));
        WebService.start(context);
    }

    @Subscribe(tags = {@Tag(Constants.RxBusEventType.WIFI_CONNECT_CHANGE_EVENT)})
    public void onWifiConnectStateChanged(NetworkInfo.State state) {
        checkWifiState(state);
    }

    private void checkWifiState(NetworkInfo.State state) {
        if (state == NetworkInfo.State.CONNECTED || state == NetworkInfo.State.CONNECTING) {
            if (state == NetworkInfo.State.CONNECTED) {
                String ip = WifiUtils.getWifiIp(context);
                if (!TextUtils.isEmpty(ip)) {
                    onWifiConnected(ip);
                    return;
                }
            }
            onWifiConnecting();
            return;
        }
        onWifiDisconnected();
    }

    private void onWifiConnected(String ipAddr) {
        mTxtTitle.setText(R.string.wlan_enabled);
        mTxtTitle.setTextColor(context.getResources().getColor(R.color.colorWifiConnected));
        mTxtSubTitle.setVisibility(View.GONE);
        mImgLanState.setImageResource(R.drawable.shared_wifi_enable);
        mTxtStateHint.setText(R.string.pls_input_the_following_address_in_pc_browser);
        mTxtAddress.setVisibility(View.VISIBLE);
        mTxtAddress.setText(String.format(context.getString(R.string.http_address), ipAddr, Constants.HTTP_PORT));
        mButtonSplitLine.setVisibility(View.GONE);
        mBtnWifiSettings.setVisibility(View.GONE);
    }

    private void onWifiConnecting() {
        mTxtTitle.setText(R.string.wlan_enabled);
        mTxtTitle.setTextColor(context.getResources().getColor(R.color.colorWifiConnected));
        mTxtSubTitle.setVisibility(View.GONE);
        mImgLanState.setImageResource(R.drawable.shared_wifi_enable);
        mTxtStateHint.setText(R.string.retrofit_wlan_address);
        mTxtAddress.setVisibility(View.GONE);
        mButtonSplitLine.setVisibility(View.GONE);
        mBtnWifiSettings.setVisibility(View.GONE);
    }

    private void onWifiDisconnected() {
        mTxtTitle.setText(R.string.wlan_disabled);
        mTxtTitle.setTextColor(context.getResources().getColor(android.R.color.black));
        mTxtSubTitle.setVisibility(View.VISIBLE);
        mImgLanState.setImageResource(R.drawable.shared_wifi_shut_down);
        mTxtStateHint.setText(R.string.fail_to_start_http_service);
        mTxtAddress.setVisibility(View.GONE);
        mButtonSplitLine.setVisibility(View.VISIBLE);
        mBtnWifiSettings.setVisibility(View.VISIBLE);
    }

    @Override
    public void dismiss() {
        super.dismiss();
        RxBus.get().post(Constants.RxBusEventType.POPUP_MENU_DIALOG_SHOW_DISMISS, Constants.MSG_DIALOG_DISMISS);
        RxBus.get().unregister(this);

        WebService.stop(context);
    }
}
