package de.impacgroup.swissrxlogin;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.widget.ProgressBar;

public class IMPSwissRxActivity extends AppCompatActivity implements SwissRxWebViewListener{

    static final String CONST_SIGNEDIN = "Rx_User_Signed_In";

    private WebView webView;
    private ProgressBar progressBar;
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

        progressBar = (ProgressBar) findViewById(getResources().getIdentifier("progressBar", "id", getPackageName()));
        progressBar.setVisibility(View.GONE);
        SwissRxWebViewClient webViewClient = new SwissRxWebViewClient(this, postBackURL);
        webView.setWebViewClient(webViewClient);
    }

    @Override
    protected void onStart() {
        super.onStart();
        final CookieManager manager = CookieManager.getInstance();
        manager.removeAllCookies(new ValueCallback<Boolean>() {
            @Override
            public void onReceiveValue(Boolean value) {
                manager.flush();
                if (webView.getUrl() != null) {
                    webView.reload();
                } else {
                    String urlPath = "https://swiss-rx-login.ch/oauth/authorize?response_type=authorization_code&client_id=" + companyId + "&redirect_uri=" + postBackURL + "&scope=anonymous";
                    webView.loadUrl(urlPath);
                    progressBar.setVisibility(View.VISIBLE);
                    finishedLoading = false;
                }
            }
        });
    }

    @Override
    public void userSignedIn() {
        Intent result = new Intent();
        result.setData(Uri.parse(CONST_SIGNEDIN));
        this.setResult(RESULT_OK, result);
        this.finish();
    }

    @Override
    public void loadingFinished() {
        progressBar.setVisibility(View.GONE);
        finishedLoading = true;
    }
}
