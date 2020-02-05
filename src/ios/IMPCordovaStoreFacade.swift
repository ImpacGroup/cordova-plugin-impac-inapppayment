//
//  IMPCordovaStoreFacade.swift
//  InApp-Test
//
//  Created by Felix Nievelstein on 03.02.20.
//  Copyright © 2020 Impac Gmbh. All rights reserved.
//

import Foundation
import Cordova

struct updateMessage: Codable {
    let prodcut: IMPProduct
    let status: String
}

@objc (ImpacInappPayment) class ImpacInappPayment: CDVPlugin {
    
    private var loadProductsCallbackId: String?
    private var onUpdateCallbackId: String?
    
    @objc(setIds:) func setIds(command: CDVInvokedUrlCommand) {
        if command.arguments.count == 1, let ids = command.arguments[0] as? Set<String> {
            IMPStoreManager.shared.set(productIDs: ids)
        } else {
            print("ImpacInappPayment: Invalid arguments, missing string array with ids")
        }
    }
    
    @objc(onUpdate:) func onUpdate(command: CDVInvokedUrlCommand) {
        onUpdateCallbackId = command.callbackId
    }
    
    @objc(getProducts:) func getProducts(command: CDVInvokedUrlCommand) {
        IMPStoreManager.shared.delegate = self
        loadProductsCallbackId = command.callbackId
        IMPStoreManager.shared.loadProducts()
    }
    
    @objc(buyProduct:) func buyProduct(command: CDVInvokedUrlCommand) {
        if command.arguments.count == 3, let productId = command.arguments[0] as? String, let accessToken = command.arguments[1] as? String, let url = command.arguments[2] as? String {
            IMPStoreManager.shared.buyProduct(productId: productId, config: IMPValidationConfig(url: url, accessToken: accessToken))
        }
    }
}

extension ImpacInappPayment: IMPStoreManagerDelegate {
    
    func userViolation(receipt: String, product: IMPProduct) {
        let result = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: product.id)
        result?.keepCallback = true
        self.commandDelegate.send(result, callbackId: onUpdateCallbackId)
        loadProductsCallbackId = nil
    }
    
    func finishedPurchasingProcess(success: Bool, product: IMPProduct) {
        let result = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: product.id)
        result?.keepCallback = true
        self.commandDelegate.send(result, callbackId: onUpdateCallbackId)
        loadProductsCallbackId = nil
    }
    
    func didPauseTransaction(product: IMPProduct) {
        let result = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: product.id)
        result?.keepCallback = true
    }
    
    func productsLoaded(products: [IMPProduct]) {
        let result = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: products)
        self.commandDelegate.send(result, callbackId: loadProductsCallbackId)
        loadProductsCallbackId = nil
    }
    
}
