package test.best.com.wifitransfer;

import android.Manifest;
import android.os.Environment;

import java.io.File;

/**
 * Created by masel on 2016/10/10.
 */

public class Constants {

    public static final int HTTP_PORT = 12345;
    public static final String DIR_IN_SDCARD = "ApkFolder";
    public static final int MSG_DIALOG_DISMISS = 0;
    public static final int MSG_UPLOAF_APK = 1;
    public static final int MSG_DELETE_APK = 0;
    public static final File DIR = new File(Environment.getExternalStorageDirectory() + File.separator + Constants.DIR_IN_SDCARD);

    public static final class RxBusEventType {
        public static final String POPUP_MENU_DIALOG_SHOW_DISMISS = "POPUP MENU DIALOG SHOW DISMISS";
        public static final String WIFI_CONNECT_CHANGE_EVENT = "WIFI CONNECT CHANGE EVENT";
        public static final String LOAD_BOOK_LIST = "LOAD BOOK LIST";
    }

    public static final String ACTION_START_WEB_SERVICE = "START_WEB_SERVICE";
    public static final String ACTION_STOP_WEB_SERVICE = "STOP_WEB_SERVICE";

    public static String[] allpermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE};

    public static final String TEXT_CONTENT_TYPE = "text/html;charset=utf-8";
    public static final String CSS_CONTENT_TYPE = "text/css;charset=utf-8";
    public static final String BINARY_CONTENT_TYPE = "application/octet-stream";
    public static final String JS_CONTENT_TYPE = "application/javascript";
    public static final String PNG_CONTENT_TYPE = "application/x-png";
    public static final String JPG_CONTENT_TYPE = "application/jpeg";
    public static final String SWF_CONTENT_TYPE = "application/x-shockwave-flash";
    public static final String WOFF_CONTENT_TYPE = "application/x-font-woff";
    public static final String TTF_CONTENT_TYPE = "application/x-font-truetype";
    public static final String SVG_CONTENT_TYPE = "image/svg+xml";
    public static final String EOT_CONTENT_TYPE = "image/vnd.ms-fontobject";
    public static final String MP3_CONTENT_TYPE = "audio/mp3";
    public static final String MP4_CONTENT_TYPE = "video/mpeg4";
}
