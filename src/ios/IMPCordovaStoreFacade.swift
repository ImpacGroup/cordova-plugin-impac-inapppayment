//
//  IMPCordovaStoreFacade.swift
//  InApp-Test
//
//  Created by Felix Nievelstein on 03.02.20.
//  Copyright © 2020 Impac Gmbh. All rights reserved.
//

import Foundation
import Cordova

@objc (ImpacInappPayment) class ImpacInappPayment: CDVPlugin {
    @objc(getProducts:) func getProducts(command: CDVInvokedUrlCommand) {
        if command.arguments.count == 2, let companyId = command.arguments[0] as? String, let appId = command.arguments[1] as? String {
            
        } else {
            print("ImpacInappPayment: Invalid arguments")
        }
    }
}
