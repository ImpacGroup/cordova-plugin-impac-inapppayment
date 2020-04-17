//
//  IMPCordovaStoreFacade.swift
//  InApp-Test
//
//  Created by Felix Nievelstein on 03.02.20.
//  Copyright © 2020 Impac Gmbh. All rights reserved.
//

import Foundation

struct IMPUpdateMessage: Codable {
    let prodcut: IMPProduct?
    let status: String
}

@objc (ImpacInappPayment) class ImpacInappPayment: CDVPlugin {
    
    private var loadProductsCallbackId: String?
    private var onUpdateCallbackId: String?
    private var config: IMPValidationConfig?
    
    @objc(canMakePayments:) func canMakePayments(command: CDVInvokedUrlCommand) {
        let result = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: IMPStoreManager.shared.canMakePayments())
        self.commandDelegate.send(result, callbackId: command.callbackId)
    }
    
    /**
     Sets the product ids which should be supported. IMPORTANT: This must be called as early as possible. Otherwise you will get no feedback on your subscriptions.
     */
    @objc(setIds:) func setIds(command: CDVInvokedUrlCommand) {
        IMPStoreManager.shared.delegate = self
        if command.arguments.count == 1, let ids = command.arguments[0] as? [String] {
            IMPStoreManager.shared.set(productIDs: Set(ids))
        } else {
            print("ImpacInappPayment: Invalid arguments, missing string array with ids")
        }
    }
    
    /**
     Adds listener to changes for the registered product ids. IMPORTANT: This must be called directly after setIds. Otherwise you will get no feedback on your subscriptions.
     */
    @objc(onUpdate:) func onUpdate(command: CDVInvokedUrlCommand) {
        IMPStoreManager.shared.delegate = self
        onUpdateCallbackId = command.callbackId
    }
    
    /**
    Adds the configuration for the validation. The configuration must have an access token and url to validate against.
     */
    @objc(setValidation:) func setValidation(command: CDVInvokedUrlCommand) {
        IMPStoreManager.shared.delegate = self
        if command.arguments.count == 2, let accessToken = command.arguments[0] as? String, let url = command.arguments[1] as? String {
            config = IMPValidationConfig(url: url, accessToken: accessToken)
            IMPStoreManager.shared.setValidationConfig(config: config!)
        } else {
            print("ImpacInappPayment: Invalid arguments, missing accessToken and url")
        }
    }
    
    /**
     Returns a list of products based on the registrated product ids.
     */
    @objc(getProducts:) func getProducts(command: CDVInvokedUrlCommand) {
        IMPStoreManager.shared.delegate = self
        loadProductsCallbackId = command.callbackId
        IMPStoreManager.shared.loadProducts()
    }
    
    /**
     Buy product by id. Note that product id must be in one of the registrated ids.
     */
    @objc(buyProduct:) func buyProduct(command: CDVInvokedUrlCommand) {
        IMPStoreManager.shared.delegate = self
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
    
    @objc(refreshStatus:) func refreshStatus(command: CDVInvokedUrlCommand) {
        IMPStoreManager.shared.delegate = self
        IMPStoreManager.shared.refreshStatus()
    }
    
    @objc(restorePurchases:) func restorePurchases(command: CDVInvokedUrlCommand) {
        IMPStoreManager.shared.delegate = self
        IMPStoreManager.shared.restorePurchase()
        
    }
}

extension ImpacInappPayment: IMPStoreManagerDelegate {
    
    func refreshedReceipt(receipt: String) {
        let message = IMPUpdateMessage(prodcut: nil, status: "refreshedReceipt")
        sendUpdateMessage(message: message)
    }
    
    func userViolation(receipt: String, product: IMPProduct?) {
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
        let message = IMPUpdateMessage(prodcut: product, status: "finished")
        sendUpdateMessage(message: message)
    }
    
    func didPauseTransaction(product: IMPProduct) {
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
