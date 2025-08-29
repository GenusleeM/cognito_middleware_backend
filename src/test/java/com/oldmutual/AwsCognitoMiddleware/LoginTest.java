package com.oldmutual.AwsCognitoMiddleware;

import com.oldmutual.AwsCognitoMiddleware.model.CognitoAppConfig;
import com.oldmutual.AwsCognitoMiddleware.service.CognitoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test to verify authentication with an existing user in the Cognito user pool.
 */
@SpringBootTest
public class LoginTest {

    @Autowired
    private CognitoService cognitoService;

    private final String TEST_EMAIL = "genusleem@oldmutual.co.zw";
    private final String TEST_PASSWORD = "Test@123";
    private final String USER_POOL_ID = "eu-west-1_eugpZQ2pa";
    private final String CLIENT_ID = "67sh3qrs9kkekr23fj2li9ntlt";
    private final String AWS_REGION = "eu-west-1";

    @BeforeEach
    public void setup() {
        // Create a mock request with the app config
        MockHttpServletRequest request = new MockHttpServletRequest();
        
        // Create a test app config
        CognitoAppConfig appConfig = new CognitoAppConfig();
        appConfig.setAppKey(UUID.randomUUID());
        appConfig.setAppName("mylmutual-test");
        appConfig.setAwsRegion(AWS_REGION);
        appConfig.setUserPoolId(USER_POOL_ID);
        appConfig.setClientId(CLIENT_ID);
        appConfig.setEnabled(true);
        
        // Set the app config in the request
        request.setAttribute("appConfig", appConfig);
        
        // Set the request context
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
    
    @AfterEach
    public void cleanup() {
        // Clear the request context
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    public void testAuthenticateExistingUser() {
        try {
            System.out.println("[DEBUG_LOG] Attempting to authenticate user: " + TEST_EMAIL);
            
            // Call the service to authenticate
            InitiateAuthResponse response = cognitoService.authenticateUser(TEST_EMAIL, TEST_PASSWORD);
            
            // Verify the response
            assertNotNull(response, "Authentication response should not be null");
            
            AuthenticationResultType authResult = response.authenticationResult();
            assertNotNull(authResult, "Authentication result should not be null");
            
            System.out.println("[DEBUG_LOG] Authentication successful!");
            System.out.println("[DEBUG_LOG] Access Token: " + authResult.accessToken().substring(0, 20) + "...");
            System.out.println("[DEBUG_LOG] ID Token: " + authResult.idToken().substring(0, 20) + "...");
            System.out.println("[DEBUG_LOG] Refresh Token: " + authResult.refreshToken().substring(0, 20) + "...");
            System.out.println("[DEBUG_LOG] Expires In: " + authResult.expiresIn());
            
        } catch (Exception e) {
            System.out.println("[DEBUG_LOG] Authentication failed with error: " + e.getMessage());
            e.printStackTrace();
            throw e; // Re-throw to fail the test
        }
    }
}