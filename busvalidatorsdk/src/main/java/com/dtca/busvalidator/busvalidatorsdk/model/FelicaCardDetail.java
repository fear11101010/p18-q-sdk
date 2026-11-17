package com.dtca.busvalidator.busvalidatorsdk.model;

import java.util.List;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
/**
 * Comprehensive data model representing all information stored on a FeliCa transit card.
 * <p>
 * This class aggregates all the various data blocks and information types that can be
 * read from a FeliCa smart card used in the bus validator system. It provides a unified
 * view of card identity, balance, transaction history, and access logs.
 * </p>
 * 
 * <h3>Contained Information:</h3>
 * <ul>
 *   <li>General card identification and type</li>
 *   <li>Issuer and operator information</li>
 *   <li>Personal information (when available)</li>
 *   <li>Card attributes and settings</li>
 *   <li>Electronic purse (balance) data</li>
 *   <li>Transaction and access logs</li>
 * </ul>
 * 
 * @author DTCA Bus Validator SDK
 * @version 1.0
 * @since 2025-10-07
 * @see GeneralInfo
 * @see IssuerInfo
 * @see PersonalInfo
 * @see AttributeInfo
 * @see EPurseInfo
 * @see OperatorInfo
 * @see StoredLogInformation
 * @see GateAccessLogInformation
 */
public class FelicaCardDetail {
    private GeneralInfo generalInfo;
    private final IssuerInfo issuerInfo;
    private PersonalInfo personalInfo;
    private final AttributeInfo attributeInfo;
    private final EPurseInfo ePurseInfo;
    private final OperatorInfo operatorInfo;
    private final StoredLogInformation storedLogInformation;
    private final List<StoredLogInformation> storedLogInformationList;
    private final GateAccessLogInformation gateAccessLogInformation;
    private final GateAccessLogInformationForTransfer gateAccessLogInformationForTransfer;
}
