# CircleAPI

All URIs are relative to */v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getLife360**](CircleAPI.md#getlife360) | **GET** /life360 | Circle (Life360) projection read


# **getLife360**
```swift
    open class func getLife360(completion: @escaping (_ data: ProjectionEnvelope?, _ error: Error?) -> Void)
```

Circle (Life360) projection read

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import MomentraAPI


// Circle (Life360) projection read
CircleAPI.getLife360() { (response, error) in
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

