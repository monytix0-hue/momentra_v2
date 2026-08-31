# BusinessAPI

All URIs are relative to */v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**createCompany**](BusinessAPI.md#createcompany) | **POST** /companies | Create company
[**createCompanyLocation**](BusinessAPI.md#createcompanylocation) | **POST** /companies/{companyId}/locations | Create company location
[**createCompanyTeam**](BusinessAPI.md#createcompanyteam) | **POST** /companies/{companyId}/teams | Create company team
[**getBusinessMomentActions**](BusinessAPI.md#getbusinessmomentactions) | **GET** /business/moments/{momentId}/actions | Business moment actions projection
[**getBusinessMomentFinance**](BusinessAPI.md#getbusinessmomentfinance) | **GET** /business/moments/{momentId}/finance | Business moment finance projection
[**getBusinessMomentLife**](BusinessAPI.md#getbusinessmomentlife) | **GET** /business/moments/{momentId}/life | Business moment life projection
[**getBusinessMomentMemory**](BusinessAPI.md#getbusinessmomentmemory) | **GET** /business/moments/{momentId}/memory | Business moment memory projection
[**getBusinessMomentPulse**](BusinessAPI.md#getbusinessmomentpulse) | **GET** /business/moments/{momentId}/pulse | Business moment pulse projection
[**getCompany**](BusinessAPI.md#getcompany) | **GET** /companies/{companyId} | Get company
[**listBusinessMoments**](BusinessAPI.md#listbusinessmoments) | **GET** /business/moments | Business moments list
[**listCompanies**](BusinessAPI.md#listcompanies) | **GET** /companies | List authorized companies
[**listCompanyLocations**](BusinessAPI.md#listcompanylocations) | **GET** /companies/{companyId}/locations | List company locations
[**listCompanyTeams**](BusinessAPI.md#listcompanyteams) | **GET** /companies/{companyId}/teams | List company teams
[**updateCompany**](BusinessAPI.md#updatecompany) | **PATCH** /companies/{companyId} | Update company
[**updateCompanyLocation**](BusinessAPI.md#updatecompanylocation) | **PATCH** /companies/{companyId}/locations/{locationId} | Update company location


# **createCompany**
```swift
    open class func createCompany(idempotencyKey: String, body: AnyCodable, xCorrelationId: UUID? = nil, completion: @escaping (_ data: CommandEnvelope?, _ error: Error?) -> Void)
```

Create company

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import MomentraAPI

let idempotencyKey = "idempotencyKey_example" // String | 
let body = "TODO" // AnyCodable | 
let xCorrelationId = 987 // UUID | Optional client correlation ID; echoed in responses. (optional)

// Create company
BusinessAPI.createCompany(idempotencyKey: idempotencyKey, body: body, xCorrelationId: xCorrelationId) { (response, error) in
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
BusinessAPI.createCompanyLocation(companyId: companyId, idempotencyKey: idempotencyKey, body: body, xCorrelationId: xCorrelationId) { (response, error) in
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

# **createCompanyTeam**
```swift
    open class func createCompanyTeam(companyId: UUID, idempotencyKey: String, body: AnyCodable, xCorrelationId: UUID? = nil, completion: @escaping (_ data: CommandEnvelope?, _ error: Error?) -> Void)
```

Create company team

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import MomentraAPI

let companyId = 987 // UUID | 
let idempotencyKey = "idempotencyKey_example" // String | 
let body = "TODO" // AnyCodable | 
let xCorrelationId = 987 // UUID | Optional client correlation ID; echoed in responses. (optional)

// Create company team
BusinessAPI.createCompanyTeam(companyId: companyId, idempotencyKey: idempotencyKey, body: body, xCorrelationId: xCorrelationId) { (response, error) in
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

# **getBusinessMomentActions**
```swift
    open class func getBusinessMomentActions(momentId: UUID, xCorrelationId: UUID? = nil, completion: @escaping (_ data: ProjectionEnvelope?, _ error: Error?) -> Void)
```

Business moment actions projection

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import MomentraAPI

let momentId = 987 // UUID | 
let xCorrelationId = 987 // UUID | Optional client correlation ID; echoed in responses. (optional)

// Business moment actions projection
BusinessAPI.getBusinessMomentActions(momentId: momentId, xCorrelationId: xCorrelationId) { (response, error) in
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

# **getBusinessMomentFinance**
```swift
    open class func getBusinessMomentFinance(momentId: UUID, xCorrelationId: UUID? = nil, completion: @escaping (_ data: ProjectionEnvelope?, _ error: Error?) -> Void)
```

Business moment finance projection

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import MomentraAPI

let momentId = 987 // UUID | 
let xCorrelationId = 987 // UUID | Optional client correlation ID; echoed in responses. (optional)

// Business moment finance projection
BusinessAPI.getBusinessMomentFinance(momentId: momentId, xCorrelationId: xCorrelationId) { (response, error) in
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

# **getBusinessMomentLife**
```swift
    open class func getBusinessMomentLife(momentId: UUID, xCorrelationId: UUID? = nil, completion: @escaping (_ data: ProjectionEnvelope?, _ error: Error?) -> Void)
```

Business moment life projection

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import MomentraAPI

let momentId = 987 // UUID | 
let xCorrelationId = 987 // UUID | Optional client correlation ID; echoed in responses. (optional)

// Business moment life projection
BusinessAPI.getBusinessMomentLife(momentId: momentId, xCorrelationId: xCorrelationId) { (response, error) in
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

# **getBusinessMomentMemory**
```swift
    open class func getBusinessMomentMemory(momentId: UUID, xCorrelationId: UUID? = nil, completion: @escaping (_ data: ProjectionEnvelope?, _ error: Error?) -> Void)
```

Business moment memory projection

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import MomentraAPI

let momentId = 987 // UUID | 
let xCorrelationId = 987 // UUID | Optional client correlation ID; echoed in responses. (optional)

// Business moment memory projection
BusinessAPI.getBusinessMomentMemory(momentId: momentId, xCorrelationId: xCorrelationId) { (response, error) in
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

# **getBusinessMomentPulse**
```swift
    open class func getBusinessMomentPulse(momentId: UUID, xCorrelationId: UUID? = nil, completion: @escaping (_ data: ProjectionEnvelope?, _ error: Error?) -> Void)
```

Business moment pulse projection

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import MomentraAPI

let momentId = 987 // UUID | 
let xCorrelationId = 987 // UUID | Optional client correlation ID; echoed in responses. (optional)

// Business moment pulse projection
BusinessAPI.getBusinessMomentPulse(momentId: momentId, xCorrelationId: xCorrelationId) { (response, error) in
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

# **getCompany**
```swift
    open class func getCompany(companyId: UUID, xCorrelationId: UUID? = nil, completion: @escaping (_ data: ProjectionEnvelope?, _ error: Error?) -> Void)
```

Get company

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import MomentraAPI

let companyId = 987 // UUID | 
let xCorrelationId = 987 // UUID | Optional client correlation ID; echoed in responses. (optional)

// Get company
BusinessAPI.getCompany(companyId: companyId, xCorrelationId: xCorrelationId) { (response, error) in
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

# **listBusinessMoments**
```swift
    open class func listBusinessMoments(cursor: String? = nil, limit: Int? = nil, xCorrelationId: UUID? = nil, completion: @escaping (_ data: ProjectionEnvelope?, _ error: Error?) -> Void)
```

Business moments list

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import MomentraAPI

let cursor = "cursor_example" // String |  (optional)
let limit = 987 // Int |  (optional) (default to 20)
let xCorrelationId = 987 // UUID | Optional client correlation ID; echoed in responses. (optional)

// Business moments list
BusinessAPI.listBusinessMoments(cursor: cursor, limit: limit, xCorrelationId: xCorrelationId) { (response, error) in
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

# **listCompanies**
```swift
    open class func listCompanies(completion: @escaping (_ data: ProjectionEnvelope?, _ error: Error?) -> Void)
```

List authorized companies

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import MomentraAPI


// List authorized companies
BusinessAPI.listCompanies() { (response, error) in
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
BusinessAPI.listCompanyLocations(companyId: companyId, xCorrelationId: xCorrelationId) { (response, error) in
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

# **listCompanyTeams**
```swift
    open class func listCompanyTeams(companyId: UUID, xCorrelationId: UUID? = nil, completion: @escaping (_ data: ProjectionEnvelope?, _ error: Error?) -> Void)
```

List company teams

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import MomentraAPI

let companyId = 987 // UUID | 
let xCorrelationId = 987 // UUID | Optional client correlation ID; echoed in responses. (optional)

// List company teams
BusinessAPI.listCompanyTeams(companyId: companyId, xCorrelationId: xCorrelationId) { (response, error) in
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

# **updateCompany**
```swift
    open class func updateCompany(companyId: UUID, idempotencyKey: String, body: AnyCodable, xCorrelationId: UUID? = nil, completion: @escaping (_ data: CommandEnvelope?, _ error: Error?) -> Void)
```

Update company

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import MomentraAPI

let companyId = 987 // UUID | 
let idempotencyKey = "idempotencyKey_example" // String | 
let body = "TODO" // AnyCodable | 
let xCorrelationId = 987 // UUID | Optional client correlation ID; echoed in responses. (optional)

// Update company
BusinessAPI.updateCompany(companyId: companyId, idempotencyKey: idempotencyKey, body: body, xCorrelationId: xCorrelationId) { (response, error) in
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
BusinessAPI.updateCompanyLocation(companyId: companyId, locationId: locationId, idempotencyKey: idempotencyKey, body: body, xCorrelationId: xCorrelationId) { (response, error) in
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

