//
//  SwissRxLogin.swift
//  SwissRxTestProject
//
//  Created by Felix Nievelstein on 07.06.19.
//  Copyright © 2019 impac. All rights reserved.
//

import Foundation
import UIKit
import Cordova

class SwissRxLogin: CDVPlugin {
    func showLogin(command: CDVInvokedUrlCommand) {
        print(command)
        let alert = UIAlertController(title: "Test Alert", message: "\(command)", preferredStyle: .alert)
        getCurrentViewController()?.present(alert, animated: true, completion: nil)
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
