package de.impacgroup.swissrxlogin;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

class SwissRxWebViewClient extends WebViewClient {

    private static final String TAG = "SwissRxClient";
    private String postBackURL;
    private SwissRxWebViewListener listener;

    SwissRxWebViewClient(SwissRxWebViewListener listener, @NonNull String postBackURL) {
        this.listener = listener;
        this.postBackURL = postBackURL;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        Log.d(TAG, "shouldOverrideUrlLoading: " + request.getUrl());
        Uri uri = request.getUrl();
        if (checkURLIsOwn(uri)) {
            listener.userSignedIn();
            return false;
        }
        return super.shouldOverrideUrlLoading(view, request);
    }

    private boolean checkURLIsOwn(Uri uri) {
        return uri.toString().startsWith(postBackURL);
    }
}
