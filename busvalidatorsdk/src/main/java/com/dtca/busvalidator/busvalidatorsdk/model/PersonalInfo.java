package com.dtca.busvalidator.busvalidatorsdk.model;

import java.util.Arrays;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder

/**
 * Data model representing personal information stored on a FeliCa transit card.
 * <p>
 * This class contains optional personal data about the card holder including
 * name, contact information, and date of birth. This information is used for
 * personalized cards and customer service purposes.
 * </p>
 * 
 * <h3>Privacy Note:</h3>
 * <p>
 * Personal information is optional and subject to privacy regulations.
 * Not all cards contain this data. Handle this information in compliance
 * with applicable data protection laws.
 * </p>
 * 
 * <h3>Data Fields:</h3>
 * <ul>
 *   <li>Name: Cardholder's name (up to 48 bytes)</li>
 *   <li>Phone: Contact number (6 bytes)</li>
 *   <li>Birthday: Date of birth (2 bytes, encoded)</li>
 * </ul>
 * 
 * @author DTCA Bus Validator SDK
 * @version 1.0
 * @since 2025-08-18
 */
public class PersonalInfo {
    /**
     * length 48 byte
     */
    byte[] name;
    /**
     * length 6 byte
     */
    byte[] phone;
    /**
     * length 2 byte
     */
    byte[] birthday;
    /**
     * length 4 byte
     */
    byte[] employeeNumber;
    /**
     * length 1 byte
     */
    byte personalAttribution;
    /**
     * length 3 byte
     */
    byte[] reserved;

    public static PersonalInfo generateData(byte[] data){
        return PersonalInfo.builder()
                .name(Arrays.copyOfRange(data,0,48))
                .phone(Arrays.copyOfRange(data,48,48+6))
                .birthday(Arrays.copyOfRange(data,48+6,48+6+2))
                .employeeNumber(Arrays.copyOfRange(data,48+6+2,48+6+2+4))
                .personalAttribution(data[48+6+2+4])
                .employeeNumber(Arrays.copyOfRange(data,48+6+2+4+1,48+6+2+4+1+3))
                .build();
    }
}
