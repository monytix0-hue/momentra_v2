# AuthAPI

All URIs are relative to */v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getMe**](AuthAPI.md#getme) | **GET** /me | Bootstrap authenticated identity


# **getMe**
```swift
    open class func getMe(completion: @escaping (_ data: ProjectionEnvelope?, _ error: Error?) -> Void)
```

Bootstrap authenticated identity

### Example
```swift
// The following code samples are still beta. For any issue, please report via http://github.com/OpenAPITools/openapi-generator/issues/new
import MomentraAPI


// Bootstrap authenticated identity
AuthAPI.getMe() { (response, error) in
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

