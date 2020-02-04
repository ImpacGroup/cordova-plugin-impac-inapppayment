    //
//  StoreManager.swift
//  InApp-Test
//
//  Created by Felix Nievelstein on 03.02.20.
//  Copyright © 2020 Impac Gmbh. All rights reserved.
//

import Foundation
import StoreKit

public typealias ProductID = String
    
protocol IMPStoreManagerDelegate: class {
    func productsLoaded(products: [IMPProduct])
    
    // Get called if a transaction process finished.
    func finishedPurchasingProcess(success: Bool)
    
    // Get called if a transaction end for the moment. As Example waiting for approvel for transaction.
    func didPauseTransaction()
    
    // Get called if a user violation accured for a receipt.
    func userViolation(receipt: String)
}

class IMPStoreManager: NSObject, SKPaymentTransactionObserver {
    
    public weak var delegate: IMPStoreManagerDelegate?
    public static let shared = IMPStoreManager()
    public var products: [SKProduct] = []
    
    private let validationController = IMPValidationController()
    private var productIDs: Set<ProductID>
    private var currentConfig: IMPValidationConfig?
    
    private override init() { productIDs = [] }
    
    public func set(productIDs: Set<ProductID>) {
      self.productIDs = productIDs
      SKPaymentQueue.default().add(self)
    }
    
    public func loadProducts() {
        let productsRequest = SKProductsRequest(productIdentifiers: productIDs)
        productsRequest.delegate = self
        productsRequest.start()
    }
        
    public func canMakePayments() -> Bool {
        return SKPaymentQueue.canMakePayments()
    }
    
    func paymentQueue(_ queue: SKPaymentQueue, updatedTransactions transactions: [SKPaymentTransaction]) {
        for transaction in transactions {
            switch transaction.transactionState {
            case .purchased:
                print("To Validate")
                if let receipt = loadReceipt(), let config = currentConfig {
                    checkReceipt(receipt: receipt, config: config) { [weak self] (success) in
                        guard let strongSelf = self else { return }
                        queue.finishTransaction(transaction)
                        if success {
                            strongSelf.endTransaction(success: success)
                        }
                    }
                }
            case .deferred:
                DispatchQueue.main.async { [weak self] in
                    guard let strongSelf = self else { return }
                    strongSelf.delegate?.didPauseTransaction()
                }
            case .restored:
                print("Restored")
            case .failed:
                endTransaction(success: false)
            default:
                print(transaction)
            }
        }
    }
    
    private func endTransaction(success: Bool) {
        DispatchQueue.main.async { [weak self] in
            guard let strongSelf = self else { return }
            strongSelf.delegate?.finishedPurchasingProcess(success: success)
        }
    }
    
    private func loadReceipt() -> String? {
        if let appStoreReceiptURL = Bundle.main.appStoreReceiptURL, FileManager.default.fileExists(atPath: appStoreReceiptURL.path) {
            do {
                let rawReceiptData = try Data.init(contentsOf: appStoreReceiptURL)
                let receiptData = rawReceiptData.base64EncodedString()
                print(receiptData)
                return receiptData
            } catch {
                print(error)
            }
        }
        return nil
    }
    
    private func checkReceipt(receipt: String, config: IMPValidationConfig, completion: @escaping (Bool) -> Void ) {
        validationController.validateReceipt(receipt: receipt, validationInfo: config) { [weak self] (success, userViolation) in
            completion(success)
            guard let strongSelf = self else { return }
            if userViolation {
                DispatchQueue.main.async {
                    strongSelf.delegate?.userViolation(receipt: receipt)
                }
            }
        }
    }
    
    public func buyProduct(productId: String, config: IMPValidationConfig) {
        currentConfig = config
        if let mProduct = products.first(where: { (prod) -> Bool in
            return prod.productIdentifier == productId
        }) {
            let payment = SKMutablePayment(product: mProduct)
            SKPaymentQueue.default().add(payment)
        }
    }
    
    public func setValidationConfig(config: IMPValidationConfig) {
        currentConfig = config
    }
    
}

extension IMPStoreManager: SKProductsRequestDelegate {
    
    func productsRequest(_ request: SKProductsRequest, didReceive response: SKProductsResponse) {
        var products: [IMPProduct] = []
        self.products = response.products
        for product in response.products {
            products.append(IMPProduct(id: product.productIdentifier, localizedTitle: product.localizedTitle, localizedDescription: product.localizedDescription, price: product.price, priceLocale: product.priceLocale))
        }
        
        if response.invalidProductIdentifiers.count > 0 {
            print("Invalid product ids:")
            for invalidId in response.invalidProductIdentifiers {
                print(invalidId)
            }
        }
        
        DispatchQueue.main.async { [weak self] in
            guard let strongSelf = self else { return }
            strongSelf.delegate?.productsLoaded(products: products)
        }
    }
}
