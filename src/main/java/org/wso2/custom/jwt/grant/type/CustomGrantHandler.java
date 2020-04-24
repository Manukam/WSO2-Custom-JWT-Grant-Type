package org.wso2.custom.jwt.grant.type;

import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.core.util.IdentityConfigParser;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.carbon.identity.oauth2.grant.jwt.JWTBearerGrantHandler;
import org.wso2.carbon.identity.oauth2.model.RequestParameter;
import org.wso2.carbon.identity.oauth2.token.OAuthTokenReqMessageContext;
import org.wso2.carbon.user.api.UserStoreException;

import javax.xml.namespace.QName;
import java.util.Iterator;

public class CustomGrantHandler extends JWTBearerGrantHandler {
    private static final Log log = LogFactory.getLog(CustomGrantHandler.class);
    public static final String CONFIG_ELEM_OAUTH = "OAuth";
    public static final String SINGLE_DEVICE_CONFIG = "SingleDeviceConfigs";
    public static String CLIENT_UUID_PARAM;
    public static String USER_CLAIM;
    public static final String CLAIM_URI = "claimURI";
    public static final String CLIENT_PARAMETER = "clientParameter";

    @Override
    public boolean validateGrant(OAuthTokenReqMessageContext tokReqMsgCtx) throws IdentityOAuth2Exception {
        String uuidClient = null;  //UUID sent by Client
        String uuidIS; //UUID in IS side
        if (CLIENT_UUID_PARAM == null || USER_CLAIM == null) {
            readPropertiesFromFile();
        }
        for (RequestParameter parameter : tokReqMsgCtx.getOauth2AccessTokenReqDTO().getRequestParameters()) {
            if (CLIENT_UUID_PARAM.equals(parameter.getKey())) {
                uuidClient = parameter.getValue()[0];
            }
        }
        super.validateGrant(tokReqMsgCtx);
        try {
            uuidIS = CarbonContext.getThreadLocalCarbonContext().getUserRealm().getUserStoreManager()
                    .getUserClaimValue(tokReqMsgCtx.getAuthorizedUser().getUserName(), USER_CLAIM, null);
        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            if (log.isDebugEnabled()) {
                log.debug("User Store Exception", e);
            }
            throw new IdentityOAuth2Exception("Invalid User Cannot find user store claim");
        }

        if (uuidClient != null) {//Checking if UUID sent by the user is null
            if (uuidIS != null) {
                if (uuidIS.equals(uuidClient)) {
                    return true;   //valid user from same device
                } else {
                    throw new IdentityOAuth2Exception("Invalid Login.Cannot login with multiple devices. Please contact Bank");//new device invalid user
                }
            } else {
                //New User
                try {
                    CarbonContext.getThreadLocalCarbonContext().getUserRealm().getUserStoreManager().setUserClaimValue(tokReqMsgCtx.getAuthorizedUser().getUserName(), USER_CLAIM, uuidClient, null);
                    if (log.isDebugEnabled()) {
                        log.debug("User Claim for Single Device validation initialized with the new value");
                    }
                } catch (UserStoreException e) {
                    if (log.isDebugEnabled()) {
                        log.debug("User Store Exception when saving the new UUID claim", e);
                    }
                    throw new IdentityOAuth2Exception("User Store Exception when saving the new UUID claim");
                }
            }
        } else {
            //Invalid Request
            throw new IdentityOAuth2Exception("Invalid Login. Please contact the Bank");
        }
        return true;
    }


    private void readPropertiesFromFile() {
        IdentityConfigParser configParser = IdentityConfigParser.getInstance();
        OMElement oauthElem = configParser.getConfigElement(CONFIG_ELEM_OAUTH);

        if (oauthElem == null) {
            log.error("OAuth element is not available.");
            return;
        }
        parseSingleDeviceConfig(oauthElem);
    }

    private void parseSingleDeviceConfig(OMElement singleDeviceConfig) {
        if (singleDeviceConfig == null) {
            if (log.isDebugEnabled()) {
                log.debug("Single Device Validation Configs can not be found");
            }
            return;
        }

        Iterator validators = singleDeviceConfig.getChildrenWithLocalName(SINGLE_DEVICE_CONFIG);
        if (validators != null) {
            for (; validators.hasNext(); ) {
                OMElement validator = (OMElement) validators.next();
                if (validator != null) {
                    CLIENT_UUID_PARAM = validator.getAttributeValue(new QName(CLIENT_PARAMETER));
                    USER_CLAIM = validator.getAttributeValue(new QName(CLAIM_URI));
                }
            }
        }
    }
}
