# PersonalAPI

All URIs are relative to */v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**createFutureItem**](PersonalAPI.md#createfutureitem) | **POST** /moments/{momentId}/future-items | Create future item
[**createLifestyleActivity**](PersonalAPI.md#createlifestyleactivity) | **POST** /moments/{momentId}/lifestyle-activities | Create lifestyle activity
[**getPersonalActivity**](PersonalAPI.md#getpersonalactivity) | **GET** /personal/activity | Personal activity timeline
[**getPersonalAttention**](PersonalAPI.md#getpersonalattention) | **GET** /personal/attention | Personal attention projection
[**getPersonalLife**](PersonalAPI.md#getpersonallife) | **GET** /personal/life | Personal life projection
[**getPersonalMemory**](PersonalAPI.md#getpersonalmemory) | **GET** /personal/memory | Personal memory projection
[**getPersonalPulse**](PersonalAPI.md#getpersonalpulse) | **GET** /personal/pulse | Personal pulse projection
[**listPersonalMoments**](PersonalAPI.md#listpersonalmoments) | **GET** /personal/moments | Personal moments list
[**recordObservation**](PersonalAPI.md#recordobservation) | **POST** /moments/{momentId}/observations | Record observation
[**recordRelationshipActivity**](PersonalAPI.md#recordrelationshipactivity) | **POST** /moments/{momentId}/relationship-activities | Record relationship activity


# **createFutureItem**
```swift
    open class func createFutureItem(momentId: UUID, idempotencyKey: String, body: AnyCodable, xCorrelationId: UUID? = nil, completion: @escaping (_ data: CommandEnvelope?, _ error: Error?) -> Void)
```

Create future item

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import MomentraAPI

let momentId = 987 // UUID | 
let idempotencyKey = "idempotencyKey_example" // String | 
let body = "TODO" // AnyCodable | 
let xCorrelationId = 987 // UUID | Optional client correlation ID; echoed in responses. (optional)

// Create future item
PersonalAPI.createFutureItem(momentId: momentId, idempotencyKey: idempotencyKey, body: body, xCorrelationId: xCorrelationId) { (response, error) in
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

# **createLifestyleActivity**
```swift
    open class func createLifestyleActivity(momentId: UUID, idempotencyKey: String, body: AnyCodable, xCorrelationId: UUID? = nil, completion: @escaping (_ data: CommandEnvelope?, _ error: Error?) -> Void)
```

Create lifestyle activity

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import MomentraAPI

let momentId = 987 // UUID | 
let idempotencyKey = "idempotencyKey_example" // String | 
let body = "TODO" // AnyCodable | 
let xCorrelationId = 987 // UUID | Optional client correlation ID; echoed in responses. (optional)

// Create lifestyle activity
PersonalAPI.createLifestyleActivity(momentId: momentId, idempotencyKey: idempotencyKey, body: body, xCorrelationId: xCorrelationId) { (response, error) in
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
PersonalAPI.getPersonalActivity(cursor: cursor, limit: limit, xCorrelationId: xCorrelationId) { (response, error) in
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

# **getPersonalAttention**
```swift
    open class func getPersonalAttention(completion: @escaping (_ data: ProjectionEnvelope?, _ error: Error?) -> Void)
```

Personal attention projection

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import MomentraAPI


// Personal attention projection
PersonalAPI.getPersonalAttention() { (response, error) in
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
This endpoint does not need any parameter.

### Return type

[**ProjectionEnvelope**](ProjectionEnvelope.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getPersonalLife**
```swift
    open class func getPersonalLife(completion: @escaping (_ data: ProjectionEnvelope?, _ error: Error?) -> Void)
```

Personal life projection

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import MomentraAPI


// Personal life projection
PersonalAPI.getPersonalLife() { (response, error) in
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
This endpoint does not need any parameter.

### Return type

[**ProjectionEnvelope**](ProjectionEnvelope.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getPersonalMemory**
```swift
    open class func getPersonalMemory(completion: @escaping (_ data: ProjectionEnvelope?, _ error: Error?) -> Void)
```

Personal memory projection

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import MomentraAPI


// Personal memory projection
PersonalAPI.getPersonalMemory() { (response, error) in
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
This endpoint does not need any parameter.

### Return type

[**ProjectionEnvelope**](ProjectionEnvelope.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getPersonalPulse**
```swift
    open class func getPersonalPulse(completion: @escaping (_ data: ProjectionEnvelope?, _ error: Error?) -> Void)
```

Personal pulse projection

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import MomentraAPI


// Personal pulse projection
PersonalAPI.getPersonalPulse() { (response, error) in
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
This endpoint does not need any parameter.

### Return type

[**ProjectionEnvelope**](ProjectionEnvelope.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **listPersonalMoments**
```swift
    open class func listPersonalMoments(cursor: String? = nil, limit: Int? = nil, xCorrelationId: UUID? = nil, completion: @escaping (_ data: ProjectionEnvelope?, _ error: Error?) -> Void)
```

Personal moments list

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import MomentraAPI

let cursor = "cursor_example" // String |  (optional)
let limit = 987 // Int |  (optional) (default to 20)
let xCorrelationId = 987 // UUID | Optional client correlation ID; echoed in responses. (optional)

// Personal moments list
PersonalAPI.listPersonalMoments(cursor: cursor, limit: limit, xCorrelationId: xCorrelationId) { (response, error) in
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

# **recordObservation**
```swift
    open class func recordObservation(momentId: UUID, idempotencyKey: String, body: AnyCodable, xCorrelationId: UUID? = nil, completion: @escaping (_ data: CommandEnvelope?, _ error: Error?) -> Void)
```

Record observation

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import MomentraAPI

let momentId = 987 // UUID | 
let idempotencyKey = "idempotencyKey_example" // String | 
let body = "TODO" // AnyCodable | 
let xCorrelationId = 987 // UUID | Optional client correlation ID; echoed in responses. (optional)

// Record observation
PersonalAPI.recordObservation(momentId: momentId, idempotencyKey: idempotencyKey, body: body, xCorrelationId: xCorrelationId) { (response, error) in
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

# **recordRelationshipActivity**
```swift
    open class func recordRelationshipActivity(momentId: UUID, idempotencyKey: String, body: AnyCodable, xCorrelationId: UUID? = nil, completion: @escaping (_ data: CommandEnvelope?, _ error: Error?) -> Void)
```

Record relationship activity

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import MomentraAPI

let momentId = 987 // UUID | 
let idempotencyKey = "idempotencyKey_example" // String | 
let body = "TODO" // AnyCodable | 
let xCorrelationId = 987 // UUID | Optional client correlation ID; echoed in responses. (optional)

// Record relationship activity
PersonalAPI.recordRelationshipActivity(momentId: momentId, idempotencyKey: idempotencyKey, body: body, xCorrelationId: xCorrelationId) { (response, error) in
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

