//
//  SKErrorHelper.swift
//  InApp-Test
//
//  Created by Felix Nievelstein on 29.04.20.
//  Copyright © 2020 Impac Gmbh. All rights reserved.
//

import Foundation
import StoreKit

class IMPSKErrorHelper {
    class func getDescriptionFor(code: SKError.Code) -> String? {
        switch code {
        case SKError.paymentCancelled:
             return "The user canceled a payment request."
        case SKError.clientInvalid:
            return "The client is not allowed to perform the attempted action."
        case SKError.paymentInvalid:
            return "One of the payment parameters was not recognized by the App Store."
        case SKError.paymentNotAllowed:
            return "The user is not allowed to authorize payments."
        case SKError.storeProductNotAvailable:
            return "The requested product is not available in the store."
        case SKError.cloudServicePermissionDenied:
            return "The user has not allowed access to Cloud service information."
        case SKError.cloudServiceRevoked:
            return "The user has revoked permission to use this cloud service."
        case SKError.privacyAcknowledgementRequired:
            return "The user has not yet acknowledged Apple’s privacy policy for Apple Music."
        case SKError.unauthorizedRequestData:
            return "The app is attempting to use a property for which it does not have the required entitlement."
        case SKError.invalidOfferIdentifier:
            return "The offer identifier is invalid."
        case SKError.invalidOfferIdentifier:
            return "The price you specified in App Store Connect is no longer valid."
        case SKError.invalidSignature:
            return "The signature in a payment discount is not valid."
        case SKError.missingOfferParams:
            return "Parameters are missing in a payment discount."
        default:
            return "Unknown or unexpected error occurred."
        }
    }
}
