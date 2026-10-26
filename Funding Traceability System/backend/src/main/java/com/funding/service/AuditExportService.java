package com.funding.service;

import javax.servlet.http.HttpServletResponse;

/**
 * 审计溯源单据导出（仅审计专用）
 * 导出件由服务端渲染为图片，并在整页叠加「当前操作者账号 + 导出时间」半透明水印
 */
public interface AuditExportService {

    /**
     * @param format  pdf / png
     * @param bizType 业务类型筛选（可空）
     * @param bizId   业务单据 ID（可空）
     * @param node    业务节点筛选（可空）
     * @param keyword 摘要 / 操作人关键词（可空）
     */
    void export(HttpServletResponse response, String format, String bizType, Long bizId,
                String node, String keyword) throws Exception;
}
