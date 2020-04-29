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
    func finishedPurchasingProcess(success: Bool, product: IMPProduct, error: String?)
    
    // Get called if a transaction end for the moment. As Example waiting for approvel for transaction.
    func didPauseTransaction(product: IMPProduct)
    
    // Get called if a user violation accured for a receipt.
    func userViolation(receipt: String, product: IMPProduct?)
    
    // Get called if a user violation accured for a receipt.
    func refreshedReceipt(receipt: String)
}

class IMPStoreManager: NSObject, SKPaymentTransactionObserver {
    
    public weak var delegate: IMPStoreManagerDelegate?
    public static let shared = IMPStoreManager()
    public var products: [SKProduct] = []
    
    private let validationController = IMPValidationController()
    private var productIDs: Set<ProductID>
    private var currentConfig: IMPValidationConfig?
    
    private override init() { productIDs = [] }
    private let openValidationKey = "IMPStoreManagerOpenValidation"
    
    public func set(productIDs: Set<ProductID>) {
      self.productIDs = productIDs
      SKPaymentQueue.default().add(self)
    }
    
    /**
     Load all products
     */
    public func loadProducts() {
        let productsRequest = SKProductsRequest(productIdentifiers: productIDs)
        productsRequest.delegate = self
        productsRequest.start()
    }
        
    /**
     Return if the current user can make a purchase
     */
    public func canMakePayments() -> Bool {
        return SKPaymentQueue.canMakePayments()
    }
    
    /**
     Refresh Receipts
     */
    public func refreshStatus() {
        if let _ = loadReceipt() {
            let refreshRequest = SKReceiptRefreshRequest()
            refreshRequest.delegate = self
            refreshRequest.start()
        } else {
            print("No receipt to refresh")
        }
    }
    
    /**
     Buy a producta by id. Needs the validation configuration
     */
    public func buyProduct(productId: String, config: IMPValidationConfig) {
        currentConfig = config
        if let mProduct = getProductBy(id: productId) {
            let payment = SKMutablePayment(product: mProduct)
            SKPaymentQueue.default().add(payment)
        }
    }
    
    /**
     Setup the configuration for the server communication to validate purchase
     */
    public func setValidationConfig(config: IMPValidationConfig) {
        currentConfig = config
        performOpenValidation()
    }
    
    public func restorePurchase() {
        SKPaymentQueue.default().restoreCompletedTransactions()
    }
    
    
    func paymentQueue(_ queue: SKPaymentQueue, updatedTransactions transactions: [SKPaymentTransaction]) {
        for transaction in transactions {
            switch transaction.transactionState {
            case .purchased, .restored:
                if let receipt = loadReceipt(), let config = currentConfig, let product = getProductBy(id: transaction.payment.productIdentifier) {
                    checkReceipt(receipt: receipt, for: product, config: config) { [weak self] (success, isValid) in
                        guard let strongSelf = self else { return }
                        queue.finishTransaction(transaction)
                        if !success {
                            strongSelf.storeOpenValidation()
                        }
                        strongSelf.endTransaction(success: success, product: product, error: nil)
                    }
                }
            case .deferred:
                DispatchQueue.main.async { [weak self] in
                    guard let strongSelf = self else { return }
                    if let mProduct = strongSelf.getProductBy(id: transaction.payment.productIdentifier) {
                        strongSelf.delegate?.didPauseTransaction(product: IMPProduct.from(skProduct: mProduct))
                    }
                }
            case .failed:
                if let mProduct = getProductBy(id: transaction.payment.productIdentifier) {
                    var errorString: String? = nil
                    if let error = transaction.error as? SKError {
                        if error.code != SKError.paymentCancelled {
                            errorString = IMPSKErrorHelper.getDescriptionFor(code: error.code)
                        }
                    }
                    endTransaction(success: false, product: mProduct, error: errorString)
                }
            default:
                break
            }
        }
    }
    
    private func endTransaction(success: Bool, product: SKProduct, error: String?) {
        DispatchQueue.main.async { [weak self] in
            guard let strongSelf = self else { return }
            strongSelf.delegate?.finishedPurchasingProcess(success: success, product: IMPProduct.from(skProduct: product), error: error)
        }
    }
    
    private func loadReceipt() -> String? {
        if let appStoreReceiptURL = Bundle.main.appStoreReceiptURL, FileManager.default.fileExists(atPath: appStoreReceiptURL.path) {
            do {
                let rawReceiptData = try Data.init(contentsOf: appStoreReceiptURL)
                let receiptData = rawReceiptData.base64EncodedString()
                return receiptData
            } catch {
                print(error)
            }
        }
        return nil
    }
    
    private func checkReceipt(receipt: String, for product: SKProduct?, config: IMPValidationConfig, completion: @escaping (_ success: Bool, _ isValid: Bool) -> Void ) {
        validationController.validateReceipt(receipt: receipt, validationInfo: config) { [weak self] (success, userViolation, isValid) in
            if userViolation {
                guard let strongSelf = self else { return }
                DispatchQueue.main.async {
                    strongSelf.delegate?.userViolation(receipt: receipt, product: product != nil ? IMPProduct.from(skProduct: product!) : nil)
                }
            }
            completion(success, isValid)
        }
    }
    
    private func getProductBy(id: String) -> SKProduct? {
        return products.first(where: { (prod) -> Bool in
            return prod.productIdentifier == id
        })
    }
    
    /**
     Returns the product to a given product id. Returns nil if no product found for id.
     */
    public func getIMPProductBy(id: String) -> IMPProduct? {
        guard let skprod = getProductBy(id: id) else { return nil }
        return IMPProduct.from(skProduct: skprod)
    }
    
    /**
     Store that the current receipt needs validation at next app resume.
     */
    private func storeOpenValidation() {
        UserDefaults.standard.set(true, forKey: openValidationKey)
    }
    
    private func performOpenValidation() {
        if UserDefaults.standard.bool(forKey: openValidationKey) {
            if let receipt = loadReceipt(), let config = currentConfig{
                validationController.validateReceipt(receipt: receipt, validationInfo: config) { [weak self] (success, userViolation, isValid) in
                    guard let strongSelf = self else { return }
                    if (success) {
                        UserDefaults.standard.set(false, forKey: strongSelf.openValidationKey)
                    }
                }
            } else {
                print("No receipt to validate")
            }
        }
    }
    
}

extension IMPStoreManager: SKProductsRequestDelegate {
    
    func productsRequest(_ request: SKProductsRequest, didReceive response: SKProductsResponse) {
        var products: [IMPProduct] = []
        self.products = response.products
        for product in response.products {
            products.append(IMPProduct.from(skProduct: product))
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
    
    func requestDidFinish(_ request: SKRequest) {
        if let _ = request as? SKReceiptRefreshRequest, let receipt = loadReceipt(), let config = currentConfig {
            checkReceipt(receipt: receipt, for: nil, config: config) { [weak self] (success, isValid) in
                guard let strongSelf = self else { return }
                DispatchQueue.main.async {
                    strongSelf.delegate?.refreshedReceipt(receipt: receipt)
                }
            }
        }
    }
    
    func request(_ request: SKRequest, didFailWithError error: Error) {
        print(error)
    }
}
