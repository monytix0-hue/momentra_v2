# DevicesAPI

All URIs are relative to */v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**registerDevice**](DevicesAPI.md#registerdevice) | **POST** /me/devices | Register push device
[**revokeDevice**](DevicesAPI.md#revokedevice) | **DELETE** /me/devices/{deviceId} | Revoke push device


# **registerDevice**
```swift
    open class func registerDevice(idempotencyKey: String, body: AnyCodable, xCorrelationId: UUID? = nil, completion: @escaping (_ data: CommandEnvelope?, _ error: Error?) -> Void)
```

Register push device

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import MomentraAPI

let idempotencyKey = "idempotencyKey_example" // String | 
let body = "TODO" // AnyCodable | 
let xCorrelationId = 987 // UUID | Optional client correlation ID; echoed in responses. (optional)

// Register push device
DevicesAPI.registerDevice(idempotencyKey: idempotencyKey, body: body, xCorrelationId: xCorrelationId) { (response, error) in
    guard error == nil else {
        print(error)
        return
    }

    if (response) {
        dump(response)
    }
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **idempotencyKey** | **String** |  | 
 **body** | **AnyCodable** |  | 
 **xCorrelationId** | **UUID** | Optional client correlation ID; echoed in responses. | [optional] 

### Return type

[**CommandEnvelope**](CommandEnvelope.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **revokeDevice**
```swift
    open class func revokeDevice(deviceId: String, xCorrelationId: UUID? = nil, completion: @escaping (_ data: CommandEnvelope?, _ error: Error?) -> Void)
```

Revoke push device

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import MomentraAPI

let deviceId = "deviceId_example" // String | 
let xCorrelationId = 987 // UUID | Optional client correlation ID; echoed in responses. (optional)

// Revoke push device
DevicesAPI.revokeDevice(deviceId: deviceId, xCorrelationId: xCorrelationId) { (response, error) in
    guard error == nil else {
        print(error)
        return
    }

    if (response) {
        dump(response)
    }
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **deviceId** | **String** |  | 
 **xCorrelationId** | **UUID** | Optional client correlation ID; echoed in responses. | [optional] 

### Return type

[**CommandEnvelope**](CommandEnvelope.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

