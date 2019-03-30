package test.best.com.wifitransfer;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.util.DisplayMetrics;

import com.hwangjr.rxbus.RxBus;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class AppInstallOrDelete {

    private static Context context;

    public AppInstallOrDelete(Context context) {
        this.context = context;
    }

    //安装
    public static void installApkFile(File file, Context context) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        //兼容7.0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Uri contentUri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
            intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
        } else {
            intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        if (context.getPackageManager().queryIntentActivities(intent, 0).size() > 0) {
            context.startActivity(intent);
        }
    }

    //卸载
    public static void delete(String packageName, Context context) {
        Uri uri = Uri.fromParts("package", packageName, null);
        Intent intent = new Intent(Intent.ACTION_DELETE, uri);
        context.startActivity(intent);
    }

    //获取APP包名
    public String getApplicationName(String packageName) {
        PackageManager packageManager = null;
        ApplicationInfo applicationInfo = null;
        try {
            packageManager = context.getApplicationContext().getPackageManager();
            applicationInfo = packageManager.getApplicationInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            applicationInfo = null;
        }
        if (packageManager != null && applicationInfo != null) {
            String applicationName =
                    (String) packageManager.getApplicationLabel(applicationInfo);
            return applicationName;
        }
        return packageName;
    }

    //获取APP图标
    public synchronized static Drawable getIconFromPackageName(String packageName) {
        PackageManager pm = context.getPackageManager();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            try {
                PackageInfo pi = pm.getPackageInfo(packageName, 0);
                Context otherAppCtx = context.createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY);
                int displayMetrics[] = {DisplayMetrics.DENSITY_XXXHIGH, DisplayMetrics.DENSITY_XXHIGH, DisplayMetrics.DENSITY_XHIGH, DisplayMetrics.DENSITY_HIGH, DisplayMetrics.DENSITY_TV};
                for (int displayMetric : displayMetrics) {
                    try {
                        Drawable d = otherAppCtx.getResources().getDrawableForDensity(pi.applicationInfo.icon, displayMetric);
                        if (d != null) {
                            return d;
                        }
                    } catch (Resources.NotFoundException e) {
                        continue;
                    }
                }
            } catch (Exception e) {
                // Handle Error here
            }
        }
        ApplicationInfo appInfo = null;
        try {
            appInfo = pm.getApplicationInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
        return appInfo.loadIcon(pm);
    }

    //APP大小
    public String getFileSize(long length) {
        DecimalFormat df = new DecimalFormat("######0.00");
        double d1 = 3.23456;
        double d2 = 0.0;
        double d3 = 2.0;
        df.format(d1);
        df.format(d2);
        df.format(d3);
        //KB
        long l = length / 1000;
        if (l < 1024) {
            return df.format(l) + "KB";
        } else if (l < 1024 * 1024.f) {
            return df.format((l / 1024.f)) + "MB";
        }
        return df.format(l / 1024.f / 1024.f) + "GB";
    }

    /**
     * 判断相对应的APP是否存在
     *
     * @param packageName(包名)(若想判断QQ，则改为com.tencent.mobileqq，若想判断微信，则改为com.tencent.mm)
     * @return
     */
    public boolean isAvilible(String packageName) {

        PackageManager packageManager = context.getPackageManager();
        //获取手机系统的所有APP包名，然后进行一一比较
        List<PackageInfo> pinfo = packageManager.getInstalledPackages(0);
        for (int i = 0; i < pinfo.size(); i++) {
            if (((PackageInfo) pinfo.get(i)).packageName
                    .equalsIgnoreCase(packageName)){
                return true;
            }
        }
        return false;
    }

    //获取apk信息
    public void handleApk(String path, long length, List<InfoModel> list){
        InfoModel infoModel = new InfoModel();
        String archiveFilePath = path;
        //获取路径为path的APK信息
        PackageManager pm = context.getPackageManager();
        PackageInfo pkgInfo = pm.getPackageArchiveInfo(archiveFilePath, 0);
        if (pkgInfo != null){
            ApplicationInfo appInfo = pkgInfo.applicationInfo;
            appInfo.sourceDir = archiveFilePath;
            appInfo.publicSourceDir = archiveFilePath;
            //得到安装包名称
            String packageName = appInfo.packageName;
            //得到版本信息
            String version = pkgInfo.versionName;
            Drawable icon = pm.getApplicationIcon(appInfo);
            String appName = pm.getApplicationLabel(appInfo).toString();
            if (TextUtils.isEmpty(appName)){
                appName = getApplicationName(packageName);
            }
            if (icon == null){
                // 获得应用程序图标
                icon = getIconFromPackageName(packageName);
            }
            infoModel.setName(appName);
            infoModel.setPackageName(packageName);
            infoModel.setPath(path);
            infoModel.setSize(getFileSize(length));
            infoModel.setVersion(version);
            infoModel.setIcon(icon);
            infoModel.setInstalled(isAvilible(packageName));
            list.add(infoModel);
        }
    }

    //删除所有文件
    public void deleteAll() {
        File dir = Constants.DIR;
        if (dir.exists() && dir.isDirectory()) {
            File[] fileNames = dir.listFiles();
            if (fileNames != null) {
                for (File fileName : fileNames) {
                    fileName.delete();
                }
            }
        }
        RxBus.get().post(Constants.RxBusEventType.LOAD_BOOK_LIST, 0);
    }

    //查询文件夹中的所有APK
    public void queryAppList(List<InfoModel> list){
        if (list != null){
            list.clear();
        }
        File dir = Constants.DIR;
        if (dir.exists() && dir.isDirectory()){
            File[] fileNames = dir.listFiles();
            if (fileNames != null){
                for (File fileName : fileNames){
                    handleApk(fileName.getAbsolutePath(), fileName.length(), list);
                }
            }
        }
    }
}
