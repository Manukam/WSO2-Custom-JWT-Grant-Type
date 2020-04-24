# WSO2-Custom-JWT-Grant-Type
 
> A Custom Grant Type for WSO2 Identity Server that only allow one device to be used per user. 

## Build Setup

* Navigate to the project root directory and execute the following.
``` bash
  mvn clean install
```
* Navigate to the `/target` folder in the project directory and copy and paste the `WSO2-Custom-Grant-Type-1.0-SNAPSHOT` to `IS_HOME/repository/components/lib`

* Navigate to `IS_HOME/repository/conf/identity` and open the **identity.xml** file and add the following lines.
- To configure the Custom Grant type as a new grant type in WSO2 Identity Server. Add the following configuration under **SupportedGrantTypes**
```
<SupportedGrantType>
    <GrantTypeName>CustomSingleDeviceGrantType</GrantTypeName>
    <GrantTypeHandlerImplClass>org.wso2.custom.jwt.grant.type.CustomGrantHandler</GrantTypeHandlerImplClass>
    <GrantTypeValidatorImplClass>org.wso2.custom.jwt.grant.type.CustomGrantValidator</GrantTypeValidatorImplClass>
</SupportedGrantType>
``` 
 - To define the name of the **parameter** the UUID is sent in the request and the **claim URI** of the claim UUID should be saved against in the Identity Server side. Add the following under **OAuth** in **identity.xml**
```
<SingleDeviceConfigs claimURI="http://wso2.org/claims/organization" clientParameter="uuidClient"></SingleDeviceConfigs>
``` 
* Save the file.

* Start the WSO2 IS server and create a Service Provider and configure a OAuth/OpenID Connect Configuration.

* The CustomSingleDeviceGrantType will be visible in the allowed grant types. Add the CustomSingleDeviceGrantType to the allowed grant types by ticking it.

* Now invoke the token endpoint with the grant_type set as CustomSingleDeviceGrantType and with the Client UUID and the authorization code. A sample request is shown below.

```
curl -k -X POST https://localhost:9443/oauth2/token -u <client_id>:<client_secret> -H 'Content-Type: application/x-www-form-urlencoded' -d 'grant_type=CustomSingleDeviceGrantType&assertion=<jwtassertion>&uuidClient=<client_UUID>' -H 'Content-Type: application/x-www-form-urlencoded' https://localhost:9443/oauth2/token?scope=openid'
```

