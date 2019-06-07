//
//  SwissRxLogin.swift
//  SwissRxTestProject
//
//  Created by Felix Nievelstein on 07.06.19.
//  Copyright © 2019 impac. All rights reserved.
//

import Foundation
import UIKit

@objc(SwissRxLogin) class SwissRxLogin: CDVPlugin {
    @objc(showLogin:) func showLogin(command: CDVInvokedUrlCommand) {
        if command.arguments.count == 2, let companyId = command.arguments[0] as? String, let appId = command.arguments[1] as? String {
            presentLoginwith(companyId: companyId, appId: appId, commandId: command.callbackId)
        } else {
            print("SwissRxLogin: Invalid arguments for companyId and / or appId")
        }
    }
    
    func presentLoginwith(companyId: String, appId: String, commandId: String) {
        let loginVC = IMPSwissRxViewController(nibName: "IMPSwissRxViewController", bundle: nil)
        loginVC.companyId = companyId
        loginVC.postBackURL = appId
        loginVC.delegate = self
        loginVC.commandId = commandId
        getCurrentViewController()?.present(loginVC, animated: true, completion: nil)
    }
    
    func getCurrentViewController() -> UIViewController? {
        var currentVC: UIViewController? = nil
        if let viewController = UIApplication.shared.keyWindow?.rootViewController {
            currentVC = viewController
            while currentVC?.presentedViewController != nil
            {
                currentVC = currentVC?.presentedViewController;
            }
        }
        return currentVC
    }
}

extension SwissRxLogin: IMPSwissRxVCDelegate {
    func userSignedIn(commandId: String) {
        let result = CDVPluginResult(status: CDVCommandStatus_OK)
        self.commandDelegate.send(result, callbackId: commandId)
    }
    
    func signInFailed(commandId: String) {
        let result = CDVPluginResult(status: CDVCommandStatus_ERROR)
        self.commandDelegate.send(result, callbackId: commandId)
    }
}
