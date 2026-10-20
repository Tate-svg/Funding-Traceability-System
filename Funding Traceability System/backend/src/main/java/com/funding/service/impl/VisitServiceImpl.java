package com.funding.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.funding.common.BusinessException;
import com.funding.common.UserContext;
import com.funding.entity.Visit;
import com.funding.mapper.VisitMapper;
import com.funding.service.VisitService;
import com.funding.util.ExcelUtils;
import com.funding.vo.VisitVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 回访服务实现
 * SPONSOR 查询一律按当前登录 userId 过滤 sponsor_id
 */
@Service
public class VisitServiceImpl implements VisitService {

    @Autowired
    private VisitMapper visitMapper;

    @Autowired
    private com.funding.common.MaskingGuard maskingGuard;

    @Override
    public IPage<VisitVO> page(Integer page, Integer size, Long studentId, String keyword) {
        Page<VisitVO> p = new Page<>(page, size);
        Long sponsorId = null;
        if ("SPONSOR".equals(UserContext.getRole())) {
            sponsorId = UserContext.getUserId();
        }
        return visitMapper.selectPageWithNames(p, keyword, studentId, sponsorId);
    }

    @Override
    public VisitVO getById(Long id) {
        VisitVO visit = visitMapper.selectByIdWithNames(id);
        if (visit == null) {
            return null;
        }
        if ("SPONSOR".equals(UserContext.getRole())
                && (visit.getSponsorId() == null || !visit.getSponsorId().equals(UserContext.getUserId()))) {
            throw new BusinessException(403, "权限不足");
        }
        return visit;
    }

    @Override
    public void saveVisit(Visit visit) {
        visit.setId(null);
        // 二次脱敏校验：回访自由文本不得夹带未打码敏感信息
        maskingGuard.check("POST /api/visits", "回访内容", visit.getContent());
        maskingGuard.check("POST /api/visits", "情况说明", visit.getSituation());
        maskingGuard.check("POST /api/visits", "存在问题", visit.getProblem());
        visitMapper.insert(visit);
    }

    @Override
    public void updateVisit(Visit visit) {
        maskingGuard.check("PUT /api/visits/" + visit.getId(), "回访内容", visit.getContent());
        maskingGuard.check("PUT /api/visits/" + visit.getId(), "情况说明", visit.getSituation());
        maskingGuard.check("PUT /api/visits/" + visit.getId(), "存在问题", visit.getProblem());
        visitMapper.updateById(visit);
    }

    @Override
    public void deleteVisit(Long id) {
        visitMapper.deleteById(id);
    }

    @Override
    public void export(HttpServletResponse response) throws Exception {
        List<VisitVO> list = visitMapper.selectAllWithNames();
        List<String> headers = Arrays.asList(
                "ID", "学生", "资助人", "回访日期", "回访内容", "回访情况", "问题记录", "回访人", "创建时间");
        List<List<Object>> rows = new ArrayList<>();
        for (VisitVO v : list) {
            List<Object> row = new ArrayList<>();
            row.add(v.getId());
            row.add(v.getStudentName());
            row.add(v.getSponsorName());
            row.add(v.getVisitDate() != null ? v.getVisitDate().toString() : "");
            row.add(v.getContent());
            row.add(v.getSituation());
            row.add(v.getProblem());
            row.add(v.getVisitor());
            row.add(v.getCreateTime() != null ? v.getCreateTime().toString() : "");
            rows.add(row);
        }
        ExcelUtils.writeWorkbook(response, "回访记录.xlsx", headers, rows);
    }
}
