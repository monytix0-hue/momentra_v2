# ActivityAPI

All URIs are relative to */v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getMomentActivity**](ActivityAPI.md#getmomentactivity) | **GET** /moments/{momentId}/activity | Moment activity timeline
[**getPersonalActivity**](ActivityAPI.md#getpersonalactivity) | **GET** /personal/activity | Personal activity timeline


# **getMomentActivity**
```swift
    open class func getMomentActivity(momentId: UUID, cursor: String? = nil, limit: Int? = nil, xCorrelationId: UUID? = nil, completion: @escaping (_ data: ProjectionEnvelope?, _ error: Error?) -> Void)
```

Moment activity timeline

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import MomentraAPI

let momentId = 987 // UUID | 
let cursor = "cursor_example" // String |  (optional)
let limit = 987 // Int |  (optional) (default to 20)
let xCorrelationId = 987 // UUID | Optional client correlation ID; echoed in responses. (optional)

// Moment activity timeline
ActivityAPI.getMomentActivity(momentId: momentId, cursor: cursor, limit: limit, xCorrelationId: xCorrelationId) { (response, error) in
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
 **momentId** | **UUID** |  | 
 **cursor** | **String** |  | [optional] 
 **limit** | **Int** |  | [optional] [default to 20]
 **xCorrelationId** | **UUID** | Optional client correlation ID; echoed in responses. | [optional] 

### Return type

[**ProjectionEnvelope**](ProjectionEnvelope.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getPersonalActivity**
```swift
    open class func getPersonalActivity(cursor: String? = nil, limit: Int? = nil, xCorrelationId: UUID? = nil, completion: @escaping (_ data: ProjectionEnvelope?, _ error: Error?) -> Void)
```

Personal activity timeline

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import MomentraAPI

let cursor = "cursor_example" // String |  (optional)
let limit = 987 // Int |  (optional) (default to 20)
let xCorrelationId = 987 // UUID | Optional client correlation ID; echoed in responses. (optional)

// Personal activity timeline
ActivityAPI.getPersonalActivity(cursor: cursor, limit: limit, xCorrelationId: xCorrelationId) { (response, error) in
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
 **cursor** | **String** |  | [optional] 
 **limit** | **Int** |  | [optional] [default to 20]
 **xCorrelationId** | **UUID** | Optional client correlation ID; echoed in responses. | [optional] 

### Return type

[**ProjectionEnvelope**](ProjectionEnvelope.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

