package com.funding.common;

import org.springframework.stereotype.Component;

@Component
public class SensitiveMaskUtil{
    //银行卡脱敏
    public String maskBankCard(String bankCard, boolean needSecondMask){
        if(!needSecondMask){
            //开关关闭 返回完整卡号
            return bankCard;
        }
        //开启二次遮挡，隐藏后几位
        if(bankCard == null || bankCard.length()<8) return bankCard;
        return bankCard.substring(0,4)+"********"+bankCard.substring(bankCard.length()-4);
    }
    //家庭住址脱敏
    public String maskAddress(String address, boolean needSecondMask){
        if(!needSecondMask){
            return address;
        }
        //示例只保留省市，隐藏门牌号
        //你自己按业务调整
        if(address == null) return "";
        //简单示例
        return address.substring(0,3)+" ****";
    }
}