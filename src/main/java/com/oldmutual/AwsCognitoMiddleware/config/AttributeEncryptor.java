package com.oldmutual.AwsCognitoMiddleware.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * JPA attribute converter for encrypting and decrypting sensitive data.
 * Used to automatically encrypt/decrypt entity attributes.
 */
@Component
@Converter
public class AttributeEncryptor implements AttributeConverter<String, String> {

    private final StringEncryptor encryptor;

    @Autowired
    public AttributeEncryptor(@Qualifier("jasyptStringEncryptor") StringEncryptor encryptor) {
        this.encryptor = encryptor;
    }

    /**
     * Convert the attribute value to be stored in the database.
     * This method encrypts the value.
     *
     * @param attribute The attribute value to encrypt
     * @return The encrypted value
     */
    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        return encryptor.encrypt(attribute);
    }

    /**
     * Convert the stored database value to the entity attribute value.
     * This method decrypts the value.
     *
     * @param dbData The database value to decrypt
     * @return The decrypted value
     */
    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return encryptor.decrypt(dbData);
    }
}