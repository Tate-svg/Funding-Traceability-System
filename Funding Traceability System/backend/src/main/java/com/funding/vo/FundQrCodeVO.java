package com.funding.vo;

import lombok.Data;

/**
 * 资助单溯源核验二维码（内部接口返回）
 * 仅包含单号、状态与二维码图片，不含学生隐私字段
 */
@Data
public class FundQrCodeVO {

    private Long fundId;

    /** 业务单号：已生成凭证号优先，否则为单据编号 */
    private String businessNo;

    /** 核验码（ID + 签名），拼接到公开核验页地址后 */
    private String verifyCode;

    /** 扫码后打开的公开核验页地址 */
    private String verifyUrl;

    /** PNG 二维码（data URL，可直接用于 img） */
    private String qrImage;

    private String status;

    private String statusText;
}
