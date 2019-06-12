package de.impacgroup.swissrxlogin;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;

public class IMPSwissRxActivity extends AppCompatActivity implements SwissRxWebViewListener{

    private static final String TAG = "IMPSwissRxActivity";
    private WebView webView;
    private boolean finishedLoading = false;
    private String companyId = "";
    private String postBackURL = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent.getExtras() != null && intent.hasExtra(SwissRxLogin.CONST_APPID) && intent.hasExtra(SwissRxLogin.CONST_COMPANYID)){
            companyId = intent.getStringExtra(SwissRxLogin.CONST_COMPANYID);
            postBackURL = intent.getStringExtra(SwissRxLogin.CONST_APPID);
        }
        setContentView(getResources().getIdentifier("activity_impswiss_rx", "layout", getPackageName()));

        webView = (WebView) findViewById(getResources().getIdentifier("webView", "id", getPackageName()));
        webView.getSettings().setJavaScriptEnabled(true);

        SwissRxWebViewClient webViewClient = new SwissRxWebViewClient(this);
        webView.setWebViewClient(webViewClient);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (webView.getUrl() != null) {
            webView.reload();
        } else {
            String urlPath = "https://swiss-rx-login.ch/oauth/authorize?response_type=authorization_code&client_id=" + companyId + "&redirect_uri=" + postBackURL + "&scope=anonymous";
            webView.loadUrl(urlPath);
            finishedLoading = false;
        }
    }
}
