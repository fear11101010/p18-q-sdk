package com.dtca.busvalidator.busvalidatorsdk.helper;

import java.util.Arrays;

import lombok.Getter;

@Getter
public enum ServiceCode {
    //        0x1001   1st Issue
//        0x2002   2nd Issue
//        0x9002   2nd Issue Cancel
//        0xA101   Personal Info Update
//        0x6002   Recharge (TOM)
//        0x6002   Recharge (TVM)
//        0x9202   Cancel of Recharge
//        0x5004   SVC Entry(PG)
//                0x5412   SVC Cancel of Entry(TOM)
//                0x5210   SVC Exit (No Penalty) - PG
//        0xC101   Re-Issue Registration (due to damage)
//        0x3102   Re-Issue Personalized Damaged
//        0xC001   Re-Issue Registration (due to lost)
//        0x3002   Re-Issue Personalized Lost
//        0x8202   Refund/Return of SVC
//        0x8102   Refund (Lost Card Found/Collect the Card/Deposit Only)
//        0xB001   Blacklist Hit SVC-Customer Operation(PG)
//        0xB001   Blacklist Hit SVC-Customer Operation(TVM)
//        0xB001   Blacklist Hit SVC-Staff Operation(TOM)

    RIDE("ride","D220","D320"),
    ALIGHT("alight","D630","D730"),
    ENTRY("entry","5004","5005"),
    EXIT("exit","5210","5211"),
//    EXIT_NO_PENALTY("exit","5210"),
    RECHARGE("recharge","6002"),
    CANCEL_OF_RECHARGE("cancel of recharge","9202"),
    BLACKLIST("blacklist","B001"),
    ENTRY_CANCEL("Cancel of Entry(TOM)","5412"),
    FIRST_ISSUE("1st issue","1001"),
    SECOND_ISSUE("2nd issue","2002","2102"),
    SECOND_ISSUE_CANCEL("2nd issue cancel","9002"),
    REISSUE("reissue","C101","3101"),
    INFO_UPDATE("Information update","A101");


    private final String name;
    private final String[] codes;
    ServiceCode(String serviceName,String... serviceCodes){
        this.name = serviceName;
        this.codes = serviceCodes;
    }

    public static String getServiceNameByCode(String code){
        String serviceName = null;
        for(ServiceCode serviceCode:ServiceCode.values()){
            serviceName = Arrays.stream(serviceCode.getCodes()).filter(c-> c.equals(code)).toString();
        }
        return serviceName;
    }
}
