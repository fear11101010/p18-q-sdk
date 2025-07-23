package com.dtca.busvalidator.busvalidatorsdk.model;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class FelicaCardDetail {
    private GeneralInfo generalInfo;
    private final IssuerInfo issuerInfo;
    private PersonalInfo personalInfo;
    private final AttributeInfo attributeInfo;
    private final EPurseInfo ePurseInfo;
    private final OperatorInfo operatorInfo;
    private final StoredLogInformation storedLogInformation;
    private final GateAccessLogInformation gateAccessLogInformation;
    private final GateAccessLogInformationForTransfer gateAccessLogInformationForTransfer;
}
