# GroupAPI

All URIs are relative to */v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getGroupMomentActions**](GroupAPI.md#getgroupmomentactions) | **GET** /group/moments/{momentId}/actions | Group moment actions projection
[**getGroupMomentFinance**](GroupAPI.md#getgroupmomentfinance) | **GET** /group/moments/{momentId}/finance | Group moment finance projection
[**getGroupMomentLife**](GroupAPI.md#getgroupmomentlife) | **GET** /group/moments/{momentId}/life | Group moment life projection
[**getGroupMomentMemory**](GroupAPI.md#getgroupmomentmemory) | **GET** /group/moments/{momentId}/memory | Group moment memory projection
[**getGroupMomentPulse**](GroupAPI.md#getgroupmomentpulse) | **GET** /group/moments/{momentId}/pulse | Group moment pulse projection
[**listGroupMoments**](GroupAPI.md#listgroupmoments) | **GET** /group/moments | Group moments list


# **getGroupMomentActions**
```swift
    open class func getGroupMomentActions(momentId: UUID, xCorrelationId: UUID? = nil, completion: @escaping (_ data: ProjectionEnvelope?, _ error: Error?) -> Void)
```

Group moment actions projection

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import MomentraAPI

let momentId = 987 // UUID | 
let xCorrelationId = 987 // UUID | Optional client correlation ID; echoed in responses. (optional)

// Group moment actions projection
GroupAPI.getGroupMomentActions(momentId: momentId, xCorrelationId: xCorrelationId) { (response, error) in
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
 **xCorrelationId** | **UUID** | Optional client correlation ID; echoed in responses. | [optional] 

### Return type

[**ProjectionEnvelope**](ProjectionEnvelope.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getGroupMomentFinance**
```swift
    open class func getGroupMomentFinance(momentId: UUID, xCorrelationId: UUID? = nil, completion: @escaping (_ data: ProjectionEnvelope?, _ error: Error?) -> Void)
```

Group moment finance projection

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import MomentraAPI

let momentId = 987 // UUID | 
let xCorrelationId = 987 // UUID | Optional client correlation ID; echoed in responses. (optional)

// Group moment finance projection
GroupAPI.getGroupMomentFinance(momentId: momentId, xCorrelationId: xCorrelationId) { (response, error) in
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
 **xCorrelationId** | **UUID** | Optional client correlation ID; echoed in responses. | [optional] 

### Return type

[**ProjectionEnvelope**](ProjectionEnvelope.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getGroupMomentLife**
```swift
    open class func getGroupMomentLife(momentId: UUID, xCorrelationId: UUID? = nil, completion: @escaping (_ data: ProjectionEnvelope?, _ error: Error?) -> Void)
```

Group moment life projection

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import MomentraAPI

let momentId = 987 // UUID | 
let xCorrelationId = 987 // UUID | Optional client correlation ID; echoed in responses. (optional)

// Group moment life projection
GroupAPI.getGroupMomentLife(momentId: momentId, xCorrelationId: xCorrelationId) { (response, error) in
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
 **xCorrelationId** | **UUID** | Optional client correlation ID; echoed in responses. | [optional] 

### Return type

[**ProjectionEnvelope**](ProjectionEnvelope.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getGroupMomentMemory**
```swift
    open class func getGroupMomentMemory(momentId: UUID, xCorrelationId: UUID? = nil, completion: @escaping (_ data: ProjectionEnvelope?, _ error: Error?) -> Void)
```

Group moment memory projection

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import MomentraAPI

let momentId = 987 // UUID | 
let xCorrelationId = 987 // UUID | Optional client correlation ID; echoed in responses. (optional)

// Group moment memory projection
GroupAPI.getGroupMomentMemory(momentId: momentId, xCorrelationId: xCorrelationId) { (response, error) in
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
 **xCorrelationId** | **UUID** | Optional client correlation ID; echoed in responses. | [optional] 

### Return type

[**ProjectionEnvelope**](ProjectionEnvelope.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getGroupMomentPulse**
```swift
    open class func getGroupMomentPulse(momentId: UUID, xCorrelationId: UUID? = nil, completion: @escaping (_ data: ProjectionEnvelope?, _ error: Error?) -> Void)
```

Group moment pulse projection

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import MomentraAPI

let momentId = 987 // UUID | 
let xCorrelationId = 987 // UUID | Optional client correlation ID; echoed in responses. (optional)

// Group moment pulse projection
GroupAPI.getGroupMomentPulse(momentId: momentId, xCorrelationId: xCorrelationId) { (response, error) in
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
 **xCorrelationId** | **UUID** | Optional client correlation ID; echoed in responses. | [optional] 

### Return type

[**ProjectionEnvelope**](ProjectionEnvelope.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **listGroupMoments**
```swift
    open class func listGroupMoments(cursor: String? = nil, limit: Int? = nil, xCorrelationId: UUID? = nil, completion: @escaping (_ data: ProjectionEnvelope?, _ error: Error?) -> Void)
```

Group moments list

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import MomentraAPI

let cursor = "cursor_example" // String |  (optional)
let limit = 987 // Int |  (optional) (default to 20)
let xCorrelationId = 987 // UUID | Optional client correlation ID; echoed in responses. (optional)

// Group moments list
GroupAPI.listGroupMoments(cursor: cursor, limit: limit, xCorrelationId: xCorrelationId) { (response, error) in
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

