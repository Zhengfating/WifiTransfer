package test.best.com.wifitransfer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Parcelable;
import android.util.Log;

import com.hwangjr.rxbus.RxBus;

public class WifiConnectChangedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        //接受是否已经联网
        if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())){
            Parcelable parcelableExtra = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            if (parcelableExtra != null){
                NetworkInfo networkInfo = (NetworkInfo) parcelableExtra;
                RxBus.get().post(Constants.RxBusEventType.WIFI_CONNECT_CHANGE_EVENT, networkInfo.getState());
            }
        }

        //接收安装或卸载广播
        if (intent.getAction().equals("android.intent.action.PACKAGE_ADDED") || intent.getAction().equals("android.intent.action.PACKAGE_REMOVED")) {
            RxBus.get().post(Constants.RxBusEventType.LOAD_BOOK_LIST, 0);
        }

    }
}
