//
//  IMPSwissRxViewController.swift
//  SwissRxTestProject
//
//  Created by Felix Nievelstein on 07.06.19.
//  Copyright © 2019 impac. All rights reserved.
//

import UIKit
import WebKit

protocol IMPSwissRxVCDelegate: class {
    func userSignedIn(commandId: String)
    func signInFailed(commandId: String)
}

class IMPSwissRxViewController: UIViewController {

    @IBOutlet weak var toolBar: UIToolbar!
    @IBOutlet weak var activityIndicator: UIActivityIndicatorView!
    
    var wkWebView: WKWebView!
    var isFinishedLoading = false
    var postBackURL = ""
    var companyId = ""
    var commandId = ""
    
    weak var delegate: IMPSwissRxVCDelegate?
    
    
    override func viewDidLoad() {
        super.viewDidLoad()
        self.wkWebView = WKWebView(frame: CGRect(x: 0, y: 0, width: self.view.frame.width, height: self.view.frame.height))
        view.insertSubview(wkWebView, belowSubview: toolBar)
        wkWebView.clipsToBounds = true
        wkWebView.navigationDelegate = self
        wkWebView.uiDelegate = self
        wkWebView.translatesAutoresizingMaskIntoConstraints = false
        configureConstraintsForView(constraintView: wkWebView)
    }
    
    func loadConnection() {
        activityIndicator.startAnimating()
        activityIndicator.isHidden = false
        if wkWebView.url != nil {
            wkWebView.reload()
        } else {
            if let url = loadStartURL()
            {
                wkWebView.load(URLRequest(url: url))
            }
        }
    }
    
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        
        let dataStore = WKWebsiteDataStore.default()
        dataStore.fetchDataRecords(ofTypes: WKWebsiteDataStore.allWebsiteDataTypes()) { records in
            dataStore.removeData(ofTypes: WKWebsiteDataStore.allWebsiteDataTypes(), for: records.filter { $0.displayName.contains("swiss-rx-login") }, completionHandler: {[weak self]  in
                self?.loadConnection()
            });
        }
        
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        NotificationCenter.default.removeObserver(self)
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
    }
    
    /**
     Set Autolayout for WKWebView
     */
    func configureConstraintsForView(constraintView: UIView)
    {
        let leftConst = NSLayoutConstraint(item: constraintView, attribute: .leading, relatedBy: .equal, toItem: self.view, attribute: .leading, multiplier: 1.0, constant: 0)
        let rightConst = NSLayoutConstraint(item: self.view!, attribute: .trailing, relatedBy: .equal, toItem: constraintView, attribute: .trailing, multiplier: 1.0, constant: 0)
        let bottomConst = NSLayoutConstraint(item: self.view!, attribute: .bottom, relatedBy: .equal, toItem: constraintView, attribute: .bottom, multiplier: 1.0, constant: 0)
        let topConst = NSLayoutConstraint(item: constraintView, attribute: .top, relatedBy: .equal, toItem: self.topLayoutGuide, attribute: .bottom, multiplier: 1.0, constant: 0)
        self.view.addConstraints([leftConst, rightConst, topConst, bottomConst])
    }
    
    /**
     Read URL From Propertylist and open with webView.
     */
    func loadStartURL() -> URL?
    {
        let url = URL(string: "https://swiss-rx-login.ch/oauth/authorize?response_type=authorization_code&client_id=" + companyId + "&redirect_uri=" + postBackURL + "&scope=anonymous")
        return url
    }
    
    @IBAction func cancelSignInButtonPressed(_ sender: Any) {
        delegate?.signInFailed(commandId: commandId)
        dismiss(animated: true, completion: nil)
    }
    
    @IBAction func refreshButtonPressed(_ sender: Any) {
        loadConnection()
    }
}

extension IMPSwissRxViewController: WKNavigationDelegate, WKUIDelegate {
    
    func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        isFinishedLoading = true
        activityIndicator.stopAnimating()
        view.backgroundColor = UIColor(red: 254 / 255, green: 233 / 255, blue: 235 / 255, alpha: 1.0)
    }
    
    func webView(_ webView: WKWebView, decidePolicyFor navigationAction: WKNavigationAction, decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
        
        if let myURL = navigationAction.request.url
        {
            if checkURLisOnWhiteList(url: myURL)
            {
                decisionHandler(.allow)
            }
            else
            {
                decisionHandler(.cancel)
                delegate?.userSignedIn(commandId: commandId)
                dismiss(animated: true, completion: nil)
            }
        }
        else
        {
            decisionHandler(.allow)
        }
    }
    
    /**
     Check if a url is on a whitelist and should be opened in the app. If false url should be open in external browser.
     */
    func checkURLisOnWhiteList(url: URL) -> Bool
    {
        return !url.absoluteString.starts(with: postBackURL)
    }
    
    /**
     Opens a url in external Browser
     */
    func openURLInSafari(url: URL)
    {
        if UIApplication.shared.canOpenURL(url)
        {
            UIApplication.shared.open(url, options: [:], completionHandler: nil)
        }
    }
    
    //MARK: - Java Alerts
    // WKWebView dosnt show Alerts and Panels from java script, native only.
    
    func webView(_ webView: WKWebView, runJavaScriptAlertPanelWithMessage message: String, initiatedByFrame frame: WKFrameInfo, completionHandler: @escaping () -> Void) {
        
        let alertController = UIAlertController(title: nil, message: message, preferredStyle: .alert)
        
        alertController.addAction(UIAlertAction(title: "OK", style: .default, handler: { (action) in
            completionHandler()
        }))
        
        present(alertController, animated: true, completion: nil)
    }
    
    func webView(_ webView: WKWebView, runJavaScriptConfirmPanelWithMessage message: String, initiatedByFrame frame: WKFrameInfo, completionHandler: @escaping (Bool) -> Void) {
        
        
        let alertController = UIAlertController(title: nil, message: message, preferredStyle: .alert)
        
        alertController.addAction(UIAlertAction(title: "OK", style: .default, handler: { (action) in
            completionHandler(true)
        }))
        
        alertController.addAction(UIAlertAction(title: "Cancel", style: .default, handler: { (action) in
            completionHandler(false)
        }))
        
        present(alertController, animated: true, completion: nil)
    }
    
    func webView(_ webView: WKWebView, runJavaScriptTextInputPanelWithPrompt prompt: String, defaultText: String?, initiatedByFrame frame: WKFrameInfo, completionHandler: @escaping (String?) -> Void) {
        
        let alertController = UIAlertController(title: nil, message: prompt, preferredStyle: .alert)
        
        alertController.addTextField { (textField ) in
            textField.text = defaultText
        }
        
        alertController.addAction(UIAlertAction(title: "OK", style: .default, handler: { (action) in
            let input = alertController.textFields?.first!.text
            completionHandler(input)
        }))
        
        alertController.addAction(UIAlertAction(title: "Cancel", style: .default, handler: { (action) in
            completionHandler(nil)
        }))
        
        present(alertController, animated: true, completion: nil)
    }
}

