package com.oldmutual.AwsCognitoMiddleware.controller;

import com.oldmutual.AwsCognitoMiddleware.dto.RegisterRequest;
import com.oldmutual.AwsCognitoMiddleware.service.CognitoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpResponse;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {

    @Mock
    private CognitoService cognitoService;

    @InjectMocks
    private AuthController authController;

    @Captor
    private ArgumentCaptor<Map<String, String>> attributesCaptor;

    @Test
    public void testRegisterWithCustomAttributes() {
        // Prepare test data
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("Password123!");
        
        // Add custom attributes
        Map<String, String> attributes = new HashMap<>();
        attributes.put("name", "John Doe");
        attributes.put("custom:phone_number", "+1234567890");
        attributes.put("custom:National_ID", "ID12345");
        attributes.put("custom:passport_number", "PASS12345");
        attributes.put("custom:dob", "1990-01-01");
        attributes.put("custom:nationality", "USA");
        attributes.put("custom:country_of_origin", "USA");
        attributes.put("custom:verification_status", "verified");
        request.setAttributes(attributes);
        
        // Mock the service response
        SignUpResponse mockResponse = SignUpResponse.builder()
                .userSub("test-user-sub-123")
                .userConfirmed(false)
                .build();
        
        when(cognitoService.registerUser(anyString(), anyString(), any())).thenReturn(mockResponse);
        
        // Call the controller method
        authController.register(request);
        
        // Verify that the service was called with the correct parameters
        verify(cognitoService).registerUser(
                eq("test@example.com"), 
                eq("Password123!"), 
                attributesCaptor.capture()
        );
        
        // Verify that all attributes were passed correctly
        Map<String, String> capturedAttributes = attributesCaptor.getValue();
        assertEquals(8, capturedAttributes.size());
        assertEquals("John Doe", capturedAttributes.get("name"));
        assertEquals("+1234567890", capturedAttributes.get("custom:phone_number"));
        assertEquals("ID12345", capturedAttributes.get("custom:National_ID"));
        assertEquals("PASS12345", capturedAttributes.get("custom:passport_number"));
        assertEquals("1990-01-01", capturedAttributes.get("custom:dob"));
        assertEquals("USA", capturedAttributes.get("custom:nationality"));
        assertEquals("USA", capturedAttributes.get("custom:country_of_origin"));
        assertEquals("verified", capturedAttributes.get("custom:verification_status"));
        
        System.out.println("[DEBUG_LOG] All custom attributes were passed correctly to the service");
    }
}