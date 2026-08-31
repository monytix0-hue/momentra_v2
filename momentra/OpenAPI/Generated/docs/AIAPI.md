# AIAPI

All URIs are relative to */v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**executeActionProposal**](AIAPI.md#executeactionproposal) | **POST** /ai/action-proposals/{actionProposalId}/execute | Execute AI action proposal


# **executeActionProposal**
```swift
    open class func executeActionProposal(actionProposalId: UUID, idempotencyKey: String, body: AnyCodable, xCorrelationId: UUID? = nil, completion: @escaping (_ data: CommandEnvelope?, _ error: Error?) -> Void)
```

Execute AI action proposal

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import MomentraAPI

let actionProposalId = 987 // UUID | 
let idempotencyKey = "idempotencyKey_example" // String | 
let body = "TODO" // AnyCodable | 
let xCorrelationId = 987 // UUID | Optional client correlation ID; echoed in responses. (optional)

// Execute AI action proposal
AIAPI.executeActionProposal(actionProposalId: actionProposalId, idempotencyKey: idempotencyKey, body: body, xCorrelationId: xCorrelationId) { (response, error) in
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
 **actionProposalId** | **UUID** |  | 
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

