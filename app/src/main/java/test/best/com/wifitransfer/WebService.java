package test.best.com.wifitransfer;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import com.hwangjr.rxbus.RxBus;
import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.http.body.MultipartFormDataBody;
import com.koushikdutta.async.http.body.Part;
import com.koushikdutta.async.http.body.UrlEncodedFormBody;
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
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.DecimalFormat;


public class WebService extends Service {

    private AsyncHttpServer server = new AsyncHttpServer();
    private AsyncServer mAsyncServer = new AsyncServer();
    private FileUploadHolder fileUploadHolder = new FileUploadHolder();

    public WebService() {
    }

    public static void start(Context context) {
        Intent intent = new Intent(context, WebService.class);
        intent.setAction(Constants.ACTION_START_WEB_SERVICE);
        context.startService(intent);
    }

    public static void stop(Context context) {
        Intent intent = new Intent(context, WebService.class);
        intent.setAction(Constants.ACTION_STOP_WEB_SERVICE);
        context.stopService(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null){
            String action = intent.getAction();
            if (Constants.ACTION_START_WEB_SERVICE.equals(action)){
                startServer();
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (server != null) {
            server.stop();
        }
        if (mAsyncServer != null) {
            mAsyncServer.stop();
        }
    }

    private void startServer(){
        //初始化html页面
        server.get("/images/.*", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                sendResources(request, response);
            }
        });
        server.get("/scripts/.*", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                sendResources(request, response);
            }
        });
        server.get("/css/.*", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                sendResources(request, response);
            }
        });

        //初始化页面
        server.get("/", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                try {
                    response.send(getIndexContent());
                } catch (IOException e) {
                    e.printStackTrace();
                    response.code(500).end();
                }
            }
        });

        //客服端显示已上传的APK包
        server.get("/files", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                JSONArray array = new JSONArray();
                File dir = Constants.DIR;
                if (dir.exists() && dir.isDirectory()){
                    String[] fileNames = dir.list();
                    if (fileNames != null){
                        for (String fileName : fileNames){
                            File file = new File(dir, fileName);
                            if (file.exists() && file.isFile()){
                                try {
                                    JSONObject jsonObject = new JSONObject();
                                    jsonObject.put("name", fileName);
                                    long fileLen = file.length();
                                    DecimalFormat df = new DecimalFormat("0.00");
                                    if (fileLen > 1024 * 1024){
                                        jsonObject.put("size", df.format(fileLen * 1f / 1024 / 1024) + "MB");
                                    }else if (fileLen < 1024){
                                        jsonObject.put("size", df.format(fileLen * 1f / 1024) + "KB");
                                    }else {
                                        jsonObject.put("size", fileLen + "B");
                                    }
                                    array.put(jsonObject);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
            }
        });

        //客服端删除APK包
        server.post("/files/.*", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                UrlEncodedFormBody body = (UrlEncodedFormBody) request.getBody();
                if ("delete".equalsIgnoreCase(body.get().getString("_method"))){
                    String path = request.getPath().replace("/files/", "");
                    try {
                        path = URLDecoder.decode(path, "utf-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    File file = new File(Constants.DIR, path);
                    if (file.exists() && file.isFile()){
                        file.delete();
                        RxBus.get().post(Constants.RxBusEventType.LOAD_BOOK_LIST, Constants.MSG_DELETE_APK);
                    }
                }
                response.end();
            }
        });

        //客服端下载APK包
        server.get("/files/.*", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                String path = request.getPath().replace("/files/", "");
                try {
                    path = URLDecoder.decode(path, "utf-8");
                }catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                File file = new File(Constants.DIR, path);
                if (file.exists() && file.isFile()){
                    try {
                        response.getHeaders().add("Content-Disposition", "attachment;filename=" + URLEncoder.encode(file.getName(), "utf-8"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    response.sendFile(file);
                    return;
                }
                response.code(404).send("Not found!");
            }
        });

        //客服端上传APK包
        server.post("/files", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, final AsyncHttpServerResponse response) {
                final MultipartFormDataBody body = (MultipartFormDataBody) request.getBody();
                body.setMultipartCallback(new MultipartFormDataBody.MultipartCallback() {
                    @Override
                    public void onPart(Part part) {
                        if (part.isFile()){
                            body.setDataCallback(new DataCallback() {
                                @Override
                                public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
                                    fileUploadHolder.write(bb.getAllByteArray());
                                    bb.recycle();
                                }
                            });
                        }else {
                            if (body.getDataCallback() == null){
                                body.setDataCallback(new DataCallback() {
                                    @Override
                                    public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
                                        try {
                                            String fileName = URLDecoder.decode(new String(bb.getAllByteArray()), "UTF-8");
                                            fileUploadHolder.setFileName(fileName);
                                        } catch (UnsupportedEncodingException e) {
                                            e.printStackTrace();
                                        }
                                        bb.recycle();
                                    }
                                });
                            }
                        }
                    }
                });
                request.setEndCallback(new CompletedCallback() {
                    @Override
                    public void onCompleted(Exception ex) {
                        fileUploadHolder.reset();
                        response.end();
                        RxBus.get().post(Constants.RxBusEventType.LOAD_BOOK_LIST, Constants.MSG_UPLOAF_APK);
                    }
                });
            }
        });

        server.get("/progress/.*", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                JSONObject res = new JSONObject();
                String path = request.getPath().replace("/progress/", "");
                if (path.equals(fileUploadHolder.fileName)) {
                    try {
                        res.put("fileName", fileUploadHolder.fileName);
                        res.put("size", fileUploadHolder.totalSize);
                        res.put("progress", fileUploadHolder.fileOutPutStream == null ? 1 : 0.1);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                response.send(res);
            }
        });

        //开启服务并监听
        server.listen(mAsyncServer, Constants.HTTP_PORT);
    }

    private String getIndexContent() throws IOException {
        BufferedInputStream bInputStream = null;
        try {
            bInputStream = new BufferedInputStream(getAssets().open("wifi/index.html"));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int len = 0;
            byte[] tmp = new byte[10240];
            while ((len = bInputStream.read(tmp)) > 0) {
                baos.write(tmp, 0, len);
            }
            //从这个输出流返回字节数组
            return new String(baos.toByteArray(), "utf-8");
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (bInputStream != null) {
                try {
                    bInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void sendResources(final AsyncHttpServerRequest request, final AsyncHttpServerResponse response) {
        try {
            String fullPath = request.getPath();
            fullPath = fullPath.replace("%20", " ");
            String resourceName = fullPath;
            if (resourceName.startsWith("/")) {
                resourceName = resourceName.substring(1);
            }
            if (resourceName.indexOf("?") > 0) {
                resourceName = resourceName.substring(0, resourceName.indexOf("?"));
            }
            if (!TextUtils.isEmpty(getContentTypeByResourceName(resourceName))) {
                response.setContentType(getContentTypeByResourceName(resourceName));
            }
            BufferedInputStream bInputStream = new BufferedInputStream(getAssets().open("wifi/" + resourceName));
            response.sendStream(bInputStream, bInputStream.available());
        } catch (IOException e) {
            e.printStackTrace();
            response.code(404).end();
            return;
        }
    }

    private String getContentTypeByResourceName(String resourceName) {
        if (resourceName.endsWith(".css")) {
            return Constants.CSS_CONTENT_TYPE;
        } else if (resourceName.endsWith(".js")) {
            return Constants.JS_CONTENT_TYPE;
        } else if (resourceName.endsWith(".swf")) {
            return Constants.SWF_CONTENT_TYPE;
        } else if (resourceName.endsWith(".png")) {
            return Constants.PNG_CONTENT_TYPE;
        } else if (resourceName.endsWith(".jpg") || resourceName.endsWith(".jpeg")) {
            return Constants.JPG_CONTENT_TYPE;
        } else if (resourceName.endsWith(".woff")) {
            return Constants.WOFF_CONTENT_TYPE;
        } else if (resourceName.endsWith(".ttf")) {
            return Constants.TTF_CONTENT_TYPE;
        } else if (resourceName.endsWith(".svg")) {
            return Constants.SVG_CONTENT_TYPE;
        } else if (resourceName.endsWith(".eot")) {
            return Constants.EOT_CONTENT_TYPE;
        } else if (resourceName.endsWith(".mp3")) {
            return Constants.MP3_CONTENT_TYPE;
        } else if (resourceName.endsWith(".mp4")) {
            return Constants.MP4_CONTENT_TYPE;
        }
        return "";
    }
}
