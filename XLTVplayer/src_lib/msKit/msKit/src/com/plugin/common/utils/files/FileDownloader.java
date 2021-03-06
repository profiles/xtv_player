package com.plugin.common.utils.files;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;
import com.plugin.common.download.image.ImageDownloader;
import com.plugin.common.utils.*;
import com.plugin.common.utils.CustomThreadPool.TaskWrapper;
import com.plugin.common.utils.CustomThreadPool.ThreadPoolSnapShot;
import com.plugin.common.utils.files.DiskManager.DiskCacheType;
import com.plugin.internet.InternetUtils;
import com.plugin.internet.interfaces.HttpRequestHookListener;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class FileDownloader extends SingleInstanceBase implements Runnable, Destroyable, HttpRequestHookListener {

    private static final String TAG = FileDownloader.class.getSimpleName();
    protected static final boolean DEBUG = true & UtilsConfig.UTILS_DEBUG;

    protected static final boolean SUPPORT_RANGED = true;

    protected static final boolean RUNTIME_CLOSE_SUPPORTED = false;

    protected String INPUT_STREAM_CACHE_PATH = null;

    /**
     * 当下载一个文件的时候，通过一个URL生成下载文件的本地的文件名字
     * 
     * @author michael
     * 
     */
    public static interface DownloadFilenameCreateListener {
        /**
         * 为一个下载URL生成本地的文件路径，注意：要保证生成文件名字的唯一性 生成的文件会下载到 big_file_cache 文件夹下面
         * 
         * @param downloadUrl
         * @return
         */
        String onFilenameCreateWithDownloadUrl(String downloadUrl);
    }

    private final class DefaultDownloadUrlEncodeListener implements DownloadFilenameCreateListener {

        @Override
        public String onFilenameCreateWithDownloadUrl(String downloadUrl) {
            int pos = downloadUrl.lastIndexOf(".");
            int sliptor = downloadUrl.lastIndexOf(File.separator);
            if (pos != -1 && sliptor != -1 && pos > sliptor) {
                String prefix = downloadUrl.substring(0, pos);
                return StringUtils.stringHashCode((prefix.replace(":", "+").replace("/", "_").replace(".", "-")
                        + downloadUrl.substring(pos)));
            }
            return StringUtils.stringHashCode(downloadUrl.replace(":", "+").replace("/", "_").replace(".", "-"));
        }

    }

    protected DownloadFilenameCreateListener mDefaultDownloadFilenameCreateListener = new DefaultDownloadUrlEncodeListener();
    protected DownloadFilenameCreateListener mDownloadFilenameCreateListener = mDefaultDownloadFilenameCreateListener;

    public static final int DOWNLOAD_SUCCESS = 10001;
    public static final int DOWNLOAD_FAILED = 20001;

    public static interface DownloadListener {

        void onDownloadProcess(int fileSize, int downloadSize);

        void onDownloadFinished(int status, Object response);
    }

    protected static class DownloadListenerObj {

        public final DownloadListener mDownloadListener;

        public final String mFileUrl;

        public final int code;

        DownloadListenerObj(String url, DownloadListener listener) {
            mDownloadListener = listener;
            mFileUrl = url;
            code = mFileUrl.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            DownloadListenerObj downloadObj = (DownloadListenerObj) obj;
            if (downloadObj.code == code && downloadObj.mDownloadListener == mDownloadListener) {
                return true;
            }

            return false;
        }
    }

    protected List<DownloadListenerObj> mListenerList;

    public static class DownloadRequest {
        public static final int STATUS_NORMAL = 1000;
        public static final int STATUS_CANCEL = 1001;

        public enum DOWNLOAD_TYPE {
            RAW, IMAGE
        }

        protected String mDownloadUrl;
        protected int mUrlHashCode;
        protected DOWNLOAD_TYPE mType;
        protected int mStatus;

        protected AtomicBoolean requestIsOperating = new AtomicBoolean(false);

        public DownloadRequest(String downloadUrl) {
            this(DOWNLOAD_TYPE.RAW, downloadUrl);
        }

        public DownloadRequest(DOWNLOAD_TYPE type, String downloadUrl) {
            if (TextUtils.isEmpty(downloadUrl)) {
                throw new IllegalArgumentException("download url can't be empty");
            }

            mDownloadUrl = downloadUrl;
            mType = type;
            mStatus = STATUS_NORMAL;
            mUrlHashCode = mDownloadUrl.hashCode();
        }

        public void cancelDownload() {
            mStatus = STATUS_CANCEL;
        }

        public String getmDownloadUrl() {
            return mDownloadUrl;
        }

        public int getmUrlHashCode() {
            return mUrlHashCode;
        }

        public DOWNLOAD_TYPE getmType() {
            return mType;
        }

        public int getmStatus() {
            return mStatus;
        }

        public AtomicBoolean getRequestIsOperating() {
            return requestIsOperating;
        }

        @Override
        public String toString() {
            return "DownloadRequest [mDownloadUrl=" + mDownloadUrl + ", mUrlHashCode=" + mUrlHashCode + ", mType="
                    + mType + ", mStatus=" + mStatus + ", requestIsOperating=" + requestIsOperating + "]";
        }

    }

    public static class DownloadResponse {
        /**
         * 下载的URL
         */
        protected String mDownloadUrl;

        /**
         * 下载的图片的本地存储路径，如果文件下载成功，那么此路劲指向的就是真正的本地图片储存路径，如果文件下载 失败，或是没有下载完成，那么为空。
         */
        protected String mLocalRawPath;

        /**
         * 下载请求的Request对象
         */
        protected DownloadRequest mRequest;

        protected DownloadResponse() {
        }

        protected DownloadResponse(String downloadUrl, String rawPath, DownloadRequest request) {
            mDownloadUrl = downloadUrl;
            mLocalRawPath = rawPath;
            mRequest = request;
        }

        public String getDownloadUrl() {
            return mDownloadUrl;
        }

        public String getRawLocalPath() {
            return mLocalRawPath;
        }

        public DownloadRequest getRequest() {
            return mRequest;
        }

        @Override
        public String toString() {
            return "DownloadResponse [mDownloadUrl=" + mDownloadUrl + ", mLocalRawPath=" + mLocalRawPath
                    + ", mRequest=" + mRequest + "]";
        }

    }

    public static interface WorkListener {
        void onProcessWork(Runnable r);
    }

    public static final int NOTIFY_DOWNLOAD_SUCCESS = -20000;
    public static final int NOTIFY_DOWNLOAD_FAILED = -40000;

    protected static final int DEFAULT_KEEPALIVE = 5 * 1000;

    protected final NotifyHandlerObserver mSuccessHandler = new NotifyHandlerObserver(NOTIFY_DOWNLOAD_SUCCESS);
    protected final NotifyHandlerObserver mFailedHandler = new NotifyHandlerObserver(NOTIFY_DOWNLOAD_FAILED);
    protected Object objLock = new Object();
    protected boolean bIsStop = true;

    protected boolean bIsWaiting = false;
    protected ArrayList<DownloadRequest> mRequestList;
    protected Context mContext;
    protected long mKeepAlive;

    private WorkListener mWorkListener = new WorkListener() {
        @Override
        public void onProcessWork(Runnable r) {
            if (r != null) {
                CustomThreadPool.getInstance().excuteWithSpecialThread(FileDownloader.class.getSimpleName(),
                        new TaskWrapper(r));
            }
        }
    };

    public static FileDownloader getInstance(Context context) {
        return SingleInstanceBase.getInstance(FileDownloader.class);
    }

    public void setDownloadDir(String dirFullPath) {
        if (!TextUtils.isEmpty(dirFullPath)) {
            File dirFile = new File(dirFullPath);
            if (dirFile.exists() && dirFile.isFile()) {
                dirFile.delete();
            }

            boolean mkSuccess = false;
            if (!dirFile.exists()) {
                mkSuccess = dirFile.mkdirs();
            } else {
                mkSuccess = true;
            }

            if (mkSuccess) {
                INPUT_STREAM_CACHE_PATH = dirFullPath;
                return;
            }
        }

        throw new IllegalArgumentException("Can't make dir : " + dirFullPath);
    }

    protected FileDownloader() {
        super();
    }

    private void processWorks() {
        mWorkListener.onProcessWork(this);
    }

    public void registeSuccessHandler(Handler handler) {
        mSuccessHandler.registeObserver(handler);
    }

    public void registeFailedHandler(Handler handler) {
        mFailedHandler.registeObserver(handler);
    }

    public void unRegisteSuccessHandler(Handler handler) {
        mSuccessHandler.unRegisteObserver(handler);
    }

    public void unRegisteFailedHandler(Handler handler) {
        mFailedHandler.unRegisteObserver(handler);
    }

    public DownloadFilenameCreateListener setDownloadUrlEncodeListener(DownloadFilenameCreateListener l) {
        DownloadFilenameCreateListener ret = mDownloadFilenameCreateListener;
        mDownloadFilenameCreateListener = l;

        return ret;
    }

    public synchronized Boolean isStopped() {
        return bIsStop;
    }

    public boolean postRequest(DownloadRequest request, DownloadListener l) {
        if (mRequestList == null || request == null || TextUtils.isEmpty(request.mDownloadUrl) || l == null) {
            return false;
        }

        DownloadListenerObj downloadObj = new DownloadListenerObj(request.mDownloadUrl, l);
        boolean contain = false;
        synchronized (mListenerList) {
            for (DownloadListenerObj obj : mListenerList) {
                if (downloadObj.equals(obj)) {
                    contain = true;
                }
            }
            if (!contain) {
                mListenerList.add(downloadObj);
            }
        }

        return postRequest(request);
    }

    public boolean postRequest(DownloadRequest request) {
        if (mRequestList == null || request == null || TextUtils.isEmpty(request.mDownloadUrl)) {
            return false;
        }

        if (DEBUG) {
            UtilsConfig.LOGD_WITH_TIME("<<<<< [[postRequest]] >>>>> ::::::::: " + request.toString());
        }
        synchronized (mRequestList) {
            boolean contain = false;
            for (DownloadRequest r : mRequestList) {
                if (r.mUrlHashCode == request.mUrlHashCode) {
                    contain = true;
                    break;
                }
            }
            if (!contain) {
                // mRequestList.add(request);
                // 将最新添加的任务放在下载队列的最前面
                mRequestList.add(0, request);

                if (DEBUG) {
                    UtilsConfig.LOGD("postRequest, add request : " + request.toString() + " into download list");
                }
            }
            bIsStop = false;

            ThreadPoolSnapShot tss = CustomThreadPool.getInstance().getSpecialThreadSnapShot(
                    ImageDownloader.class.getSimpleName());
            if (tss == null) {
                return false;
            } else {
                if (tss.taskCount < tss.ALLOWED_MAX_TAKS) {
                    if (DEBUG) {
                        UtilsConfig.LOGD("entry into [[postRequest]] to start process ");
                    }
                    processWorks();
                }
            }
        }
        if (DEBUG) {
            UtilsConfig.LOGD_WITH_TIME("<<<<< [[postRequest]]  end synchronized (mRequestList) >>>>>");
        }

        synchronized (objLock) {
            if (bIsWaiting) {
                bIsWaiting = false;

                if (DEBUG) {
                    UtilsConfig.LOGD("try to notify download process begin");
                }
                objLock.notify();
            }
        }

        if (DEBUG) {
            UtilsConfig.LOGD_WITH_TIME("<<<<< [[postRequest]]  end synchronized (objLock) >>>>>");
        }

        return true;
    }

    protected boolean checkInputStreamDownloadFile(String filePath) {
        return true;
    }

    protected DownloadResponse tryToHandleDownloadFile(String downloadLocalPath, DownloadRequest request) {
        DownloadResponse response = new DownloadResponse();
        response.mDownloadUrl = request.mDownloadUrl;
        response.mLocalRawPath = downloadLocalPath;
        response.mRequest = request;

        return response;
    }

    private void waitforUrl() {
        try {
            synchronized (objLock) {
                if (mRequestList.size() == 0 && !bIsWaiting) {
                    bIsWaiting = true;

                    if (DEBUG) {
                        UtilsConfig.LOGD("entry into [[waitforUrl]] for " + DEFAULT_KEEPALIVE + "ms");
                    }
                    objLock.wait(mKeepAlive);

                    if (DEBUG) {
                        UtilsConfig.LOGD("leave [[waitforUrl]] for " + DEFAULT_KEEPALIVE + "ms");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (DEBUG) {
                UtilsConfig.LOGD("Excption : ", e);
            }
        }
        bIsWaiting = false;
    }

    @Override
    public void onCheckRequestHeaders(String requestUrl, HttpRequestBase request) {
        if (request == null) {
            throw new IllegalArgumentException("Http Request is null");
        }

        if (SUPPORT_RANGED) {
            // 目前只有大文件下载才会做此接口回调，在此回调中可以增加断点续传
            if (request instanceof HttpGet) {
                String saveFile = mDownloadFilenameCreateListener != null ? mDownloadFilenameCreateListener
                        .onFilenameCreateWithDownloadUrl(requestUrl) : mDefaultDownloadFilenameCreateListener
                        .onFilenameCreateWithDownloadUrl(requestUrl);
                File bigCacheFile = new File(INPUT_STREAM_CACHE_PATH);
                if (!bigCacheFile.exists() || !bigCacheFile.isDirectory()) {
                    bigCacheFile.delete();
                    bigCacheFile.mkdirs();
                }

                File tempFile = new File(INPUT_STREAM_CACHE_PATH + saveFile);
                long fileSize = 0;
                if (tempFile.exists()) {
                    fileSize = tempFile.length();
                } else {
                    fileSize = 0;
                }

                request.addHeader("RANGE", "bytes=" + fileSize + "-");
            }
        }
    }

    @Override
    public String onInputStreamReturn(String requestUrl, InputStream is) {
        if (!UtilsRuntime.isSDCardReady()) {
            UtilsConfig.LOGD("return because unmount the sdcard");
            return null;
        }

        if (DEBUG) {
            UtilsConfig.LOGD("");
            UtilsConfig.LOGD("//-------------------------------------------------");
            UtilsConfig.LOGD("||");
            UtilsConfig.LOGD("|| [[FileDownloader::onInputStreamReturn]] : ");
            UtilsConfig.LOGD("||      try to download [[BIG]] file with url : " + requestUrl);
            UtilsConfig.LOGD("||");
            UtilsConfig.LOGD("\\-------------------------------------------------");
            UtilsConfig.LOGD("");
        }
        
        if (is != null) {
            String saveUrl = mDownloadFilenameCreateListener != null ? mDownloadFilenameCreateListener
                    .onFilenameCreateWithDownloadUrl(requestUrl) : mDefaultDownloadFilenameCreateListener
                    .onFilenameCreateWithDownloadUrl(requestUrl);
            File bigCacheFile = new File(INPUT_STREAM_CACHE_PATH);
            if (!bigCacheFile.exists() || !bigCacheFile.isDirectory()) {
                bigCacheFile.delete();
                bigCacheFile.mkdirs();
            }

            long curTime = 0;
            if (DEBUG) {
                UtilsConfig.LOGD("try to download from is to local path = " + INPUT_STREAM_CACHE_PATH + saveUrl
                        + " for orgin URL : " + requestUrl);
                curTime = System.currentTimeMillis();
            }

            // download file
            int totalSize = 0;
            try {
                totalSize = is.available();
            } catch (Exception e) {
                e.printStackTrace();
            }

            long downloadSize = 0;
            String savePath = null;
            String targetPath = INPUT_STREAM_CACHE_PATH + saveUrl;
            byte[] buffer = new byte[4096 * 2];
            File f = new File(targetPath);
            int len;
            OutputStream os = null;
            boolean isClosed = false;
            
            try {
                if (f.exists()) {
                    downloadSize = f.length();
                }

                os = new FileOutputStream(f, true);
                while ((len = is.read(buffer)) != -1) {
                    os.write(buffer, 0, len);

                    // add listener to Notify UI
                    downloadSize += len;
                    handleProcess(requestUrl, totalSize, (int) downloadSize);

                    if (RUNTIME_CLOSE_SUPPORTED) {
                        DownloadRequest r = findCacelRequest(requestUrl);
                        if (r != null && r.mStatus == DownloadRequest.STATUS_CANCEL) {
                            UtilsConfig.LOGD("try to close is >>>>>>>>>>>>>>>>>>>>");
                            is.close();
                            isClosed = true;
                        }
                    }
                }
                savePath = targetPath;
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                if (os != null) {
                    try {
                        os.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                buffer = null;
            }
            // end download

            try {
                if (!isClosed) {
                    is.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (!isClosed && !TextUtils.isEmpty(savePath) && checkInputStreamDownloadFile(savePath)) {
                if (DEBUG) {
                    long successTime = System.currentTimeMillis();
                    UtilsConfig.LOGD("[[onInputStreamReturn]] save Request url : " + saveUrl
                            + " success ||||||| and the saved file size : "
                            + FileUtil.convertStorage(new File(savePath).length()) + ", save cost time = "
                            + (successTime - curTime) + "ms");
                }

                return savePath;
            } else {
                // 遗留文件，用于下次的断点下载
                if (DEBUG) {
                    UtilsConfig.LOGD("===== failed to downlaod requestUrl : " + requestUrl
                            + " beacuse the debug 断点 =====");
                }
                return null;
            }
        } else {
            if (DEBUG) {
                UtilsConfig.LOGD("===== failed to downlaod requestUrl : " + requestUrl
                        + " beacuse requestUrl is NULL =====");
            }
        }

        return null;
    }

    @Override
    public void onDestroy() {
        mSuccessHandler.removeAllObserver();
        mFailedHandler.removeAllObserver();
        synchronized (mRequestList) {
            mRequestList.clear();
        }

        synchronized (mListenerList) {
            mListenerList.clear();
        }
    }

    @Override
    public void run() {
        while (!bIsStop) {
            waitforUrl();

            if (DEBUG) {
                UtilsConfig.LOGD_WITH_TIME("<<<<< [[run]] >>>>>");
            }
            synchronized (mRequestList) {
                if (mRequestList.size() == 0) {
                    // bIsRunning = false;
                    bIsStop = true;
                    break;
                }
            }

            if (DEBUG) {
                UtilsConfig.LOGD_WITH_TIME("<<<<< [[run]]  end synchronized (mRequestList) >>>>>");
            }

            DownloadRequest request = null;
            try {
                request = findRequestCanOperate(mRequestList);
                if (request == null) {
                    bIsStop = true;
                }
                if (request != null && request.mStatus != DownloadRequest.STATUS_CANCEL) {
                    if (DEBUG) {
                        UtilsConfig.LOGD("================ <<" + Thread.currentThread().getName() + ">> working on : ");
                        UtilsConfig.LOGD("begin operate one request : " + request.toString());
                        UtilsConfig.LOGD("============================================");
                    }

                    String cacheFile = InternetUtils.requestBigResourceWithCache(mContext, request.mDownloadUrl);
                    if (DEBUG) {
                        UtilsConfig.LOGD("----- after get the cache file : " + cacheFile + " =======");
                    }
                    if (!TextUtils.isEmpty(cacheFile)) {
                        DownloadResponse response = tryToHandleDownloadFile(cacheFile, request);
                        if (response != null) {
                            // notify success
                            mSuccessHandler.notifyAll(-1, -1, response);
                            handleResponseByListener(DOWNLOAD_SUCCESS, request.mDownloadUrl, response, false);
                            removeRequest(request);
                            continue;
                        }
                    }

                    if (request.getmStatus() != DownloadRequest.STATUS_CANCEL) {
                        handleResponseByListener(DOWNLOAD_FAILED, request.mDownloadUrl, request, false);
                        mFailedHandler.notifyAll(-1, -1, request);
                    } else {
                        handleResponseByListener(DOWNLOAD_FAILED, request.mDownloadUrl, request, true);
                    }

                    if (DEBUG) {
                        UtilsConfig.LOGD("success end operate one request : " + request);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (DEBUG) {
                    UtilsConfig.LOGD("Exception : ", e);
                    UtilsConfig.LOGD("exception end operate one request : " + request);
                }

                if (request.getmStatus() != DownloadRequest.STATUS_CANCEL) {
                    handleResponseByListener(DOWNLOAD_FAILED, request.mDownloadUrl, request, false);
                    mFailedHandler.notifyAll(-1, -1, request);
                } else {
                    handleResponseByListener(DOWNLOAD_FAILED, request.mDownloadUrl, request, true);
                }
            }

            removeRequest(request);
        }

        System.gc();
    }

    private void handleResponseByListener(int status, String fetchUrl, Object notfiyObj, boolean ignoreNotify) {
        if (mListenerList.size() > 0) {
            int curCode = fetchUrl.hashCode();
            LinkedList<DownloadListenerObj> removeObj = new LinkedList<DownloadListenerObj>();
            synchronized (mListenerList) {
                for (DownloadListenerObj d : mListenerList) {
                    if (d.code == curCode) {
                        if (!ignoreNotify) {
                            d.mDownloadListener.onDownloadFinished(status, notfiyObj);
                        }
                        removeObj.add(d);
                    }
                }
                mListenerList.removeAll(removeObj);
            }
        }
    }

    private DownloadRequest findRequestCanOperate(ArrayList<DownloadRequest> requestList) {
        if (DEBUG) {
            UtilsConfig.LOGD_WITH_TIME("<<<<< [[findRequestCanOperate]] >>>>>");
        }

        synchronized (requestList) {
            for (DownloadRequest r : requestList) {
                if (!r.requestIsOperating.get()) {
                    r.requestIsOperating.set(true);

                    if (DEBUG) {
                        UtilsConfig.LOGD_WITH_TIME("<<<<< [[findRequestCanOperate]] end findRequestCanOperate >>>>>");
                    }
                    return r;
                }
            }

            return null;
        }
    }

    private void removeRequest(DownloadRequest r) {
        synchronized (mRequestList) {
            mRequestList.remove(r);
        }
    }

    private void handleProcess(String requestUrl, int fileSize, int downloadSize) {
        int hashCode = requestUrl.hashCode();
        for (DownloadListenerObj l : mListenerList) {
            if (l.code == hashCode && l.mDownloadListener != null) {
                //TODO: should lock the add by main thread and sub thread
                l.mDownloadListener.onDownloadProcess(fileSize, downloadSize);
            }
        }
    }

    private DownloadRequest findCacelRequest(String requestUrl) {
        int hashCode = requestUrl.hashCode();
        synchronized (mRequestList) {
            for (DownloadRequest r : mRequestList) {
                if (r.mUrlHashCode == hashCode) {
                    return r;
                }
            }
        }

        return null;
    }

    @Override
    protected void init(Context context) {
        INPUT_STREAM_CACHE_PATH = DiskManager.tryToFetchCachePathByTypeBinding(DiskCacheType.INPUTSTREAM_BIG_FILE_CACHE);
        mContext = context.getApplicationContext();
        mRequestList = new ArrayList<DownloadRequest>();
        bIsStop = false;
        mKeepAlive = DEFAULT_KEEPALIVE;
        mListenerList = new LinkedList<DownloadListenerObj>();
        InternetUtils.setHttpReturnListener(mContext, this);
    }

}
