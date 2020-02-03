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
}

class IMPStoreManager: NSObject, SKPaymentTransactionObserver {
    
    private let productIDs: Set<ProductID>
    public weak var delegate: IMPStoreManagerDelegate?
    public var products: [SKProduct] = []
    
    public init(productIDs: Set<ProductID>) {
      self.productIDs = productIDs
      super.init()
      SKPaymentQueue.default().add(self)
    }
    
    func loadProducts() {
        print(productIDs)
        let productsRequest = SKProductsRequest(productIdentifiers: productIDs)
        productsRequest.delegate = self
        productsRequest.start()
    }
    
    func paymentQueue(_ queue: SKPaymentQueue, updatedTransactions transactions: [SKPaymentTransaction]) {
        for transaction in transactions {
            switch transaction.transactionState {
            case .purchased:
                print("To Validate")
                if let receipt = loadReceipt() {
                    checkReceipt(receipt: receipt) { (success) in
                        if success {
                            queue.finishTransaction(transaction)
                        }
                    }
                }
            case .deferred:
                print("Warten auf Genehmigung des Kaufs durch Dritte")
            case .restored:
                print("Restored")
            default:
                print(transaction)
            }
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
    
    private func checkReceipt(receipt: String, completion: @escaping (Bool) -> Void ) {
        
        completion(true)
    }
    
    public func buyProduct(productId: String) {
        if let mProduct = products.first(where: { (prod) -> Bool in
            return prod.productIdentifier == productId
        }) {
            let payment = SKMutablePayment(product: mProduct)
            SKPaymentQueue.default().add(payment)
        }
    }
    
}

extension IMPStoreManager: SKProductsRequestDelegate {
    
    func productsRequest(_ request: SKProductsRequest, didReceive response: SKProductsResponse) {
        print(response.products)
        var products: [IMPProduct] = []
        for product in response.products {
            products.append(IMPProduct(id: product.productIdentifier, localizedTitle: product.localizedTitle, localizedDescription: product.localizedDescription, price: product.price, priceLocale: product.priceLocale))
        }
        
        for invalidId in response.invalidProductIdentifiers {
            print(invalidId)
        }
        delegate?.productsLoaded(products: products)
    }
}
