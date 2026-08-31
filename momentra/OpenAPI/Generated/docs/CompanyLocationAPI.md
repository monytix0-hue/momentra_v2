# CompanyLocationAPI

All URIs are relative to */v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**createCompanyLocation**](CompanyLocationAPI.md#createcompanylocation) | **POST** /companies/{companyId}/locations | Create company location
[**listCompanyLocations**](CompanyLocationAPI.md#listcompanylocations) | **GET** /companies/{companyId}/locations | List company locations
[**updateCompanyLocation**](CompanyLocationAPI.md#updatecompanylocation) | **PATCH** /companies/{companyId}/locations/{locationId} | Update company location


# **createCompanyLocation**
```swift
    open class func createCompanyLocation(companyId: UUID, idempotencyKey: String, body: AnyCodable, xCorrelationId: UUID? = nil, completion: @escaping (_ data: CommandEnvelope?, _ error: Error?) -> Void)
```

Create company location

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import MomentraAPI

let companyId = 987 // UUID | 
let idempotencyKey = "idempotencyKey_example" // String | 
let body = "TODO" // AnyCodable | 
let xCorrelationId = 987 // UUID | Optional client correlation ID; echoed in responses. (optional)

// Create company location
CompanyLocationAPI.createCompanyLocation(companyId: companyId, idempotencyKey: idempotencyKey, body: body, xCorrelationId: xCorrelationId) { (response, error) in
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
 **companyId** | **UUID** |  | 
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

# **listCompanyLocations**
```swift
    open class func listCompanyLocations(companyId: UUID, xCorrelationId: UUID? = nil, completion: @escaping (_ data: ProjectionEnvelope?, _ error: Error?) -> Void)
```

List company locations

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import MomentraAPI

let companyId = 987 // UUID | 
let xCorrelationId = 987 // UUID | Optional client correlation ID; echoed in responses. (optional)

// List company locations
CompanyLocationAPI.listCompanyLocations(companyId: companyId, xCorrelationId: xCorrelationId) { (response, error) in
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
 **companyId** | **UUID** |  | 
 **xCorrelationId** | **UUID** | Optional client correlation ID; echoed in responses. | [optional] 

### Return type

[**ProjectionEnvelope**](ProjectionEnvelope.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **updateCompanyLocation**
```swift
    open class func updateCompanyLocation(companyId: UUID, locationId: UUID, idempotencyKey: String, body: AnyCodable, xCorrelationId: UUID? = nil, completion: @escaping (_ data: CommandEnvelope?, _ error: Error?) -> Void)
```

Update company location

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import MomentraAPI

let companyId = 987 // UUID | 
let locationId = 987 // UUID | 
let idempotencyKey = "idempotencyKey_example" // String | 
let body = "TODO" // AnyCodable | 
let xCorrelationId = 987 // UUID | Optional client correlation ID; echoed in responses. (optional)

// Update company location
CompanyLocationAPI.updateCompanyLocation(companyId: companyId, locationId: locationId, idempotencyKey: idempotencyKey, body: body, xCorrelationId: xCorrelationId) { (response, error) in
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
 **companyId** | **UUID** |  | 
 **locationId** | **UUID** |  | 
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

