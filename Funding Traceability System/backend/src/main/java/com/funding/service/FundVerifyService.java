package com.funding.service;

import com.funding.vo.FundQrCodeVO;
import com.funding.vo.PublicVerifyVO;

/**
 * 资助凭证溯源核验服务
 * 内部：登录用户生成二维码；公开：扫码凭核验码查询（不含隐私）
 */
public interface FundVerifyService {

    /**
     * 生成（或重新获取）某资助单的核验二维码
     * 仅登录用户可调用，SPONSOR 仅限本人资助单
     */
    FundQrCodeVO qrCode(Long fundId);

    /**
     * 扫码公开核验：按核验码返回单号、金额、节点时间与状态
     * 无需登录，每次查询实时读取主业务数据
     */
    PublicVerifyVO publicVerify(String code);
}
