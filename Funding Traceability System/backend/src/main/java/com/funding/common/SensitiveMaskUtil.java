package com.funding.common;

import org.springframework.stereotype.Component;

@Component
public class SensitiveMaskUtil {

    // 银行卡脱敏：保留前4后4
    public String maskBankCard(String bankCard, boolean needSecondMask) {
        if (!needSecondMask) {
            return bankCard;
        }
        if (bankCard == null || bankCard.length() < 8) {
            return bankCard;
        }
        return bankCard.substring(0, 4) + "********" + bankCard.substring(bankCard.length() - 4);
    }

    // 家庭住址脱敏：仅保留前3个字符（省/市），其余隐藏
    public String maskAddress(String address, boolean needSecondMask) {
        if (!needSecondMask || address == null || address.trim().isEmpty()) {
            return address;
        }
        if (address.length() <= 3) {
            return address.charAt(0) + "***";
        }
        return address.substring(0, 3) + " ****";
    }

    // 手机号二次遮挡：保留前3后2
    public String maskPhone(String phone, boolean needSecondMask) {
        if (!needSecondMask || phone == null || phone.trim().isEmpty()) {
            return phone;
        }
        if (phone.length() < 7) {
            return "****";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 2);
    }
}
