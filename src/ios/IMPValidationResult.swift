//
//  IMPValidationResult.swift
//  InApp-Test
//
//  Created by Felix Nievelstein on 04.05.20.
//  Copyright © 2020 Impac Gmbh. All rights reserved.
//

import UIKit

struct IMPValidationResult: Codable {
    let success: Bool
    let userViolation: Bool
    let originalTransactionIds: [String]?
}
