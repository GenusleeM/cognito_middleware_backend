package com.oldmutual.AwsCognitoMiddleware.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic API response wrapper.
 * 
 * @param <T> The type of data contained in the response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    
    private boolean success;
    private String message;
    private T data;
    
    /**
     * Create a successful response with data.
     * 
     * @param data The response data
     * @param message The success message
     * @return A successful API response
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, message, data);
    }
    
    /**
     * Create a successful response with just a message.
     * 
     * @param message The success message
     * @return A successful API response
     */
    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(true, message, null);
    }
    
    /**
     * Create an error response.
     * 
     * @param message The error message
     * @return An error API response
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null);
    }
    
    /**
     * Create an error response with data.
     * 
     * @param message The error message
     * @param data The error data
     * @return An error API response with data
     */
    public static <T> ApiResponse<T> error(String message, T data) {
        return new ApiResponse<>(false, message, data);
    }
}