//
//  IMPProduct.swift
//  InApp-Test
//
//  Created by Felix Nievelstein on 03.02.20.
//  Copyright © 2020 Impac Gmbh. All rights reserved.
//

import Foundation
import StoreKit

struct IMPProduct: Codable {
    let id: String
    let localizedTitle: String
    let localizedDescription: String
    let price: String
    let priceLocale: Locale
    
    public static func from(skProduct: SKProduct) -> IMPProduct {
        return IMPProduct(id: skProduct.productIdentifier, localizedTitle: skProduct.localizedTitle, localizedDescription: skProduct.localizedDescription, price: String(describing: skProduct.price), priceLocale: skProduct.priceLocale)
    }
}
