package com.funding.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.funding.common.BusinessException;
import com.funding.common.UserContext;
import com.funding.entity.Fund;
import com.funding.entity.ReviewTask;
import com.funding.mapper.FundMapper;
import com.funding.mapper.ReviewTaskMapper;
import com.funding.service.ReviewTaskService;
import com.funding.vo.ReviewTaskVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 大额复核抽检服务实现
 */
@Service
public class ReviewTaskServiceImpl implements ReviewTaskService {

    @Autowired
    private ReviewTaskMapper reviewTaskMapper;

    @Autowired
    private FundMapper fundMapper;

    @Value("${funding.review.threshold:5000}")
    private BigDecimal reviewThreshold;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> draw(BigDecimal minAmount, BigDecimal maxAmount, BigDecimal ratio) {
        if (ratio == null || ratio.compareTo(BigDecimal.ONE) < 0 || ratio.compareTo(new BigDecimal("100")) > 0) {
            throw new BusinessException(400, "抽检比例须在 1~100 之间");
        }
        // 1. 候选池
        List<Fund> candidates = fundMapper.selectReviewCandidates(reviewThreshold, minAmount, maxAmount);
        if (candidates.isEmpty()) {
            throw new BusinessException(400, "当前无符合条件的大额待复核单");
        }
        // 2. 抽取数量：max(1, ceil(候选数 × ratio/100))
        int total = candidates.size();
        int n = BigDecimal.valueOf(total).multiply(ratio)
                .divide(new BigDecimal("100"), 0, RoundingMode.CEILING).intValue();
        n = Math.max(1, Math.min(n, total));
        List<Fund> drawn = fundMapper.selectRandomCandidates(reviewThreshold, minAmount, maxAmount, n);

        // 3. 批量插入复核任务
        String batchNo = "B" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        Set<Long> drawnIds = new HashSet<>();
        for (Fund f : drawn) {
            ReviewTask task = new ReviewTask();
            task.setFundId(f.getId());
            task.setBatchNo(batchNo);
            task.setSampleRatio(ratio);
            task.setStatus(ReviewTask.STATUS_PENDING);
            reviewTaskMapper.insert(task);
            drawnIds.add(f.getId());
        }

        // 4. 轮空放行：同批候选未抽中者条件 UPDATE 为 REVIEWED
        for (Fund f : candidates) {
            if (drawnIds.contains(f.getId())) {
                continue;
            }
            UpdateWrapper<Fund> uw = new UpdateWrapper<>();
            uw.eq("id", f.getId()).eq("status", Fund.STATUS_AUDITED);
            uw.set("status", Fund.STATUS_REVIEWED);
            fundMapper.update(null, uw);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("batchNo", batchNo);
        result.put("total", total);
        result.put("drawn", drawn.size());
        return result;
    }

    @Override
    public IPage<ReviewTaskVO> page(Integer page, Integer size, String batchNo, String status) {
        Page<ReviewTaskVO> p = new Page<>(page, size);
        return reviewTaskMapper.selectPageWithFund(p, batchNo, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finish(Long id, String result, String remark) {
        ReviewTask task = reviewTaskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(404, "复核任务不存在");
        }
        boolean pass = "PASS".equals(result);

        // 条件 UPDATE 任务状态，防重复复核
        UpdateWrapper<ReviewTask> uw = new UpdateWrapper<>();
        uw.eq("id", id).eq("status", ReviewTask.STATUS_PENDING);
        uw.set("status", pass ? ReviewTask.STATUS_PASS : ReviewTask.STATUS_FAIL);
        uw.set("reviewer_id", UserContext.getUserId());
        uw.set("review_remark", remark);
        uw.set("review_time", LocalDateTime.now());
        int rows = reviewTaskMapper.update(null, uw);
        if (rows == 0) {
            throw new BusinessException(400, "任务状态已变化，请刷新");
        }

        // 联动资金单：PASS → REVIEWED；FAIL → 打回 SUBMITTED
        UpdateWrapper<Fund> fuw = new UpdateWrapper<>();
        fuw.eq("id", task.getFundId()).eq("status", Fund.STATUS_AUDITED);
        fuw.set("status", pass ? Fund.STATUS_REVIEWED : Fund.STATUS_SUBMITTED);
        int fundRows = fundMapper.update(null, fuw);
        if (fundRows == 0) {
            throw new BusinessException(400, "资金单状态已变化，请刷新");
        }
    }
}
