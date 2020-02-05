//
//  IMPCordovaStoreFacade.swift
//  InApp-Test
//
//  Created by Felix Nievelstein on 03.02.20.
//  Copyright © 2020 Impac Gmbh. All rights reserved.
//

import Foundation

struct IMPUpdateMessage: Codable {
    let prodcut: IMPProduct
    let status: String
}

@objc (ImpacInappPayment) class ImpacInappPayment: CDVPlugin {
    
    private var loadProductsCallbackId: String?
    private var onUpdateCallbackId: String?
    private var config: IMPValidationConfig?
    
    @objc(setIds:) func setIds(command: CDVInvokedUrlCommand) {
        if command.arguments.count == 1, let ids = command.arguments[0] as? [String] {
            IMPStoreManager.shared.set(productIDs: Set(ids))
        } else {
            print("ImpacInappPayment: Invalid arguments, missing string array with ids")
        }
    }
    
    @objc(onUpdate:) func onUpdate(command: CDVInvokedUrlCommand) {
        onUpdateCallbackId = command.callbackId
    }
    
    @objc(setValidation:) func setValidation(command: CDVInvokedUrlCommand) {
        if command.arguments.count == 2, let accessToken = command.arguments[0] as? String, let url = command.arguments[1] as? String {
            config = IMPValidationConfig(url: url, accessToken: accessToken)
            IMPStoreManager.shared.setValidationConfig(config: config!)
        } else {
            print("ImpacInappPayment: Invalid arguments, missing accessToken and url")
        }
    }
    
    @objc(getProducts:) func getProducts(command: CDVInvokedUrlCommand) {
        IMPStoreManager.shared.delegate = self
        loadProductsCallbackId = command.callbackId
        IMPStoreManager.shared.loadProducts()
    }
    
    @objc(buyProduct:) func buyProduct(command: CDVInvokedUrlCommand) {
        if command.arguments.count == 1, let productId = command.arguments[0] as? String {
            if let mConfig = config {
                IMPStoreManager.shared.buyProduct(productId: productId, config: mConfig)
            } else {
                print("ImpacInappPayment: Missing validation configuration. Did you set validation configuration?")
            }
        } else {
            print("ImpacInappPayment: Invalid arguments, missing product id")
        }
    }
}

extension ImpacInappPayment: IMPStoreManagerDelegate {
    
    func userViolation(receipt: String, product: IMPProduct) {
        print("userViolation")
        let message = IMPUpdateMessage(prodcut: product, status: "userViolation")
        sendUpdateMessage(message: message)
    }
    
    private func sendUpdateMessage(message: IMPUpdateMessage) {
        do {
            let jsonData = try JSONEncoder().encode(message)
            let result = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: String(data: jsonData, encoding: .utf8))
            result?.keepCallback = true
            self.commandDelegate.send(result, callbackId: onUpdateCallbackId)
        } catch  {
            print(error)
        }
    }
    
    func finishedPurchasingProcess(success: Bool, product: IMPProduct) {
        print("finishedPurchasingProcess")
        let message = IMPUpdateMessage(prodcut: product, status: "finished")
        sendUpdateMessage(message: message)
    }
    
    func didPauseTransaction(product: IMPProduct) {
        print("didPauseTransaction")
        let message = IMPUpdateMessage(prodcut: product, status: "didPause")
        sendUpdateMessage(message: message)
    }
    
    func productsLoaded(products: [IMPProduct]) {
        do {
            let jsonData = try JSONEncoder().encode(products)
            let result = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: String(data: jsonData, encoding: .utf8))
            self.commandDelegate.send(result, callbackId: loadProductsCallbackId)
            loadProductsCallbackId = nil
        } catch  {
            print(error)
        }
    }
    
}
