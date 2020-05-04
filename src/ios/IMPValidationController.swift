//
//  IMPValidationController.swift
//  InApp-Test
//
//  Created by Felix Nievelstein on 03.02.20.
//  Copyright © 2020 Impac Gmbh. All rights reserved.
//

import Foundation

enum HTTPMethod : String
{
    case post = "POST"
    case get = "GET"
    case put = "PUT"
}

class IMPValidationController: NSObject {
    
    public func validateReceipt(receipt: String, validationInfo: IMPValidationConfig, completion: @escaping (_ success: Bool, _ result: IMPValidationResult?) -> Void) {
        let headers = ["Authorization": "\(validationInfo.authorizationType) \(validationInfo.accessString)", "Content-Type": "application/json"]
        if let json = getJSONObject(data: ["receipt" : receipt]) {
            performRequestWith(url: validationInfo.url, method: .post, parameters: json, headers: headers) { (_ result, _ error) in
                if let mResult = result, let validationResult = IMPValidationController.getObject(data: mResult, retClass: IMPValidationResult.self) as? IMPValidationResult {
                    completion(true, validationResult)
                } else {
                    completion(false, nil)
                }
            }
        } else {
            completion(false, nil)
        }
    }        

    // MARK - Hanlde api call native without external framework.
    
    private func performRequestWith(url: String, method: HTTPMethod, parameters: Any?, headers: [String: String], completion: @escaping (_ response: Data?, _ error: [String: Any]?)-> Void) {
        if let myUrl = URL(string: url)
        {
            let request = NSMutableURLRequest(url: myUrl, cachePolicy: .reloadIgnoringLocalCacheData, timeoutInterval: 60.0)
            request.httpMethod = method.rawValue
            request.allHTTPHeaderFields = headers
            request.httpBody = parameters as? Data != nil ? parameters as? Data : nil
                      
            let session = URLSession.shared
            let dataTask = session.dataTask(with: request as URLRequest, completionHandler: {[weak self] (data, response, error) in
               guard let strongSelf = self else {return}
               
               if error == nil, let httpResponse = response as? HTTPURLResponse
               {
                    strongSelf.handleResponseCode(responseCode: httpResponse.statusCode, repeatCall: false, requestIsGetToken: false, completion: { (success, repeatCall, error) in
                                              
                       if let codeError = error
                       {
                           completion(nil, codeError)
                       }
                       else if success
                       {
                           if let responseData = data
                           {
                                completion(responseData, nil)
                           }
                       }
                       else
                       {
                           completion(nil, ["error": "Not authorized"])
                       }
                   })
               }
               else
               {
                   if let errorDescription = error?.localizedDescription{
                       completion(nil, ["error": errorDescription])
                   }else{
                    completion(nil, ["error": "Not authorized"])
                   }
                   
               }
           })
           dataTask.resume()
           
       }
       else
       {
           completion(nil, ["error": "Not authorized"])
       }
       
   }
    
    //Prüft den Status Code und reagiert auf den StatusCode entsrpechend.
    private func handleResponseCode(responseCode: Int, repeatCall: Bool, requestIsGetToken: Bool, completion: @escaping (_ success: Bool, _ repeat: Bool,_ error: [String: String]?) -> Void)
    {
        switch responseCode
        {
        case 200, 201, 202:
            completion(true, false, nil)
        case 401:
            completion(false, false, ["error": "Not authorized"])
        case 403:
            completion(false, false, ["error": "Client has no permission"])
        case 500:
            completion(false, false, ["error": "Server error"])
        default:
            completion(false, false, ["error": "Unknown error"])
        }
    }

    // Json object to any
    private static func getObject<T: Codable>(data: Data, retClass: T.Type) -> Any?
    {
        do
        {
            let decoder = JSONDecoder()
            return try decoder.decode(retClass, from: data)
        } catch {
            return nil
        }
    }

    // Any to json object
    func getJSONObject(data: Any) -> Data?
    {
        do
        {
            return try JSONSerialization.data(withJSONObject: data, options: JSONSerialization.WritingOptions())
        }
        catch
        {
            return nil
        }
    }
}
