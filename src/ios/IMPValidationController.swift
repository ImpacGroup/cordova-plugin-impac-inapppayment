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
    
    public func validateReceipt(receipt: String, validationInfo: IMPValidationConfig, completion: @escaping (_ success: Bool, _ userViolation: Bool) -> Void) {
        let headers = ["Authorization": "Bearer \(validationInfo.accessToken)", "Content-Type": "application/json"]
        if let json = getJSONObject(data: ["receipt" : receipt]) {
            performRequestWith(url: validationInfo.url, method: .post, parameters: json, headers: headers) { (result) in
                let success = result["success"] as! Bool
                let userViolation = result["userViolation"] as! Bool
                completion(success, userViolation)
            }
        } else {
            completion(false, false)
        }
    }

    // MARK - Hanlde api call native without external framework.
    
    private func performRequestWith(url: String, method: HTTPMethod, parameters: Any?, headers: [String: String], completion: @escaping (_ response: [String: Any])-> Void) {
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
                           completion(codeError)
                       }
                       else if success
                       {
                           if let responseData = data
                           {
                               if let responeDict = strongSelf.getAnyForJSONObject(data: responseData) as? [String: Any]
                               {
                                   completion(responeDict)
                               }
                               else
                               {
                                   completion(["data":responseData])
                               }
                           }
                       }
                       else
                       {
                           completion(["error": "Not authorized"])
                       }
                   })
               }
               else
               {
                   if let errorDescription = error?.localizedDescription{
                       completion(["error": errorDescription])
                   }else{
                       completion(["error": "Not authorized"])
                   }
                   
               }
           })
           dataTask.resume()
           
       }
       else
       {
           completion(["error": "Not authorized"])
       }
       
   }
    
    //Prüft den Status Code und reagiert auf den StatusCode entsrpechend.
    private func handleResponseCode(responseCode: Int, repeatCall: Bool, requestIsGetToken: Bool, completion: @escaping (_ success: Bool, _ repeat: Bool,_ error: [String: String]?) -> Void)
    {
        print(responseCode)
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
    private func getAnyForJSONObject(data: Data) -> Any?
    {
        do
        {
            return try JSONSerialization.jsonObject(with: data, options: JSONSerialization.ReadingOptions())
        }
        catch
        {
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
