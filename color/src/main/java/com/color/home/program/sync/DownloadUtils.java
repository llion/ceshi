package com.color.home.program.sync;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.color.home.android.providers.downloads.Downloads;
import com.color.home.network.CustomHttpClient;

public class DownloadUtils {
    private final static String TAG = "DownloadUtils";
    private static final boolean DBG = false;;

    /**
     * Query and return files with status failed download.
     */
    public static int queryStatusUnsuccessfull(ContentResolver resolver) {
        final Cursor cursor = resolver.query(Downloads.Impl.CONTENT_URI,
                new String[] { Downloads.Impl.COLUMN_STATUS }, Downloads.Impl.COLUMN_STATUS + " != " + Downloads.Impl.STATUS_SUCCESS, null,
                null);
        try {

            if (DBG) {
                if (cursor.moveToNext())
                    Log.d(TAG, "queryStatusUnsuccessfull. [" + cursor.getString(cursor.getColumnIndex("status")));
            }

            return cursor.getCount();
        } finally {
            cursor.close();
        }
    }

    /**
     * @param uri
     * @param toPath
     * @return content length, 0 on failed.
     */
    public static long downloadFileTo(String uri, String toPath) {
        HttpClient httpClient = CustomHttpClient.getHttpClient();
        try {
            Uri remoteUriNeedEscape = Uri.parse(uri);
            Uri escapedUri = remoteUriNeedEscape.buildUpon().path(remoteUriNeedEscape.getPath()).build();
            if (DBG)
                Log.i(TAG, "fetchFile. remoteUriNeedEscape=" + remoteUriNeedEscape + ", escapedUri=" + escapedUri.toString());

            HttpGet request = new HttpGet(escapedUri.toString());
            HttpParams params = new BasicHttpParams();
            HttpConnectionParams.setSoTimeout(params, 3000); // 10 secs
            request.setParams(params);

            HttpResponse response = httpClient.execute(request);
            StatusLine status = response.getStatusLine();

            if (DBG) {
                Header[] allHeaders = response.getAllHeaders();
                for (Header header : allHeaders) {
                    if (DBG)
                        Log.i(TAG, "downloadFileTo. header name=" + header.getName() + ", value=" + header.getValue());
                }
                Log.d(TAG, "Statusline: " + status);
                Log.d(TAG, "Statuscode: " + status.getStatusCode());
            }

            HttpEntity entity = response.getEntity();
            if (entity == null || status.getStatusCode() != 200) {
                if (DBG)
                    Log.i(TAG, "downloadFileTo. no entity or error.");
            } else {
                BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(toPath));
                if (DBG) {
                    Log.d(TAG, "Length: " + entity.getContentLength());
                    Log.d(TAG, "type: " + entity.getContentType());
                }
                entity.writeTo(bout);
                bout.flush();
                bout.close();
                return entity.getContentLength();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error e=" + e + ", downloading file=" + uri + ", to=" + toPath, e);
        }
        return 0L;
    }
}
