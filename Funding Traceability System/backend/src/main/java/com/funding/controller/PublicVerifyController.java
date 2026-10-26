package com.funding.controller;

import com.funding.common.BusinessException;
import com.funding.common.Result;
import com.funding.service.FundVerifyService;
import com.funding.vo.PublicVerifyVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

/**
 * 扫码公开核验接口（无需登录）
 *
 * 与系统内部业务接口完全隔离：仅此一个只读接口对外，且只返回
 * 业务单号、资助金额、审批节点时间与业务状态，不含任何学生隐私信息。
 * 该路径在 WebMvcConfig 中列入 JwtInterceptor / RoleInterceptor 白名单。
 */
@RestController
@RequestMapping("/api/public/verify")
public class PublicVerifyController {

    @Autowired
    private FundVerifyService fundVerifyService;

    /**
     * 扫码核验：核验码格式 {fundId}.{sign}
     * 每次请求实时读取主业务数据，禁止缓存
     */
    @GetMapping
    public Result<PublicVerifyVO> verify(@RequestParam("code") String code, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("X-Robots-Tag", "noindex, nofollow");
        try {
            return Result.success(fundVerifyService.publicVerify(code));
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        }
    }
}
