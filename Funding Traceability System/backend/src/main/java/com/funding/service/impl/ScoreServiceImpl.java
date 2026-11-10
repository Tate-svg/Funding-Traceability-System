package com.funding.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.funding.common.BusinessException;
import com.funding.common.UserContext;
import com.funding.entity.Score;
import com.funding.entity.ScoreDetail;
import com.funding.entity.ScoreItem;
import com.funding.mapper.ScoreDetailMapper;
import com.funding.mapper.ScoreItemMapper;
import com.funding.mapper.ScoreMapper;
import com.funding.service.ScoreService;
import com.funding.util.ExcelUtils;
import com.funding.vo.ScoreDetailVO;
import com.funding.vo.ScoreVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * 评分服务实现
 * 保存评分时按 score_item.weight 加权自动计算 total_score（忽略请求中的 totalScore）
 */
@Service
public class ScoreServiceImpl implements ScoreService {

    @Autowired
    private ScoreMapper scoreMapper;
    @Autowired
    private ScoreDetailMapper scoreDetailMapper;
    @Autowired
    private ScoreItemMapper scoreItemMapper;

    @Autowired
    private com.funding.mapper.FundMapper fundMapper;

    @Override
    public List<com.funding.vo.OptionVO> fundOptions(Long studentId, Long sponsorId, String keyword) {
        QueryWrapper<com.funding.entity.Fund> query = new QueryWrapper<com.funding.entity.Fund>()
                .eq("student_id", studentId).eq("sponsor_id", sponsorId);
        if (keyword != null && !keyword.trim().isEmpty()) {
            String term = keyword.trim();
            query.and(w -> w.like("voucher_no", term).or().apply("CAST(id AS CHAR) = {0}", term));
        }
        return fundMapper.selectList(query.orderByDesc("id").last("LIMIT 50")).stream()
                .map(f -> new com.funding.vo.OptionVO(f.getId(), "#" + f.getId() + " "
                        + (f.getVoucherNo() == null ? "待生成凭证" : f.getVoucherNo()) + " / " + f.getAmount() + "元"))
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public IPage<ScoreVO> page(Integer page, Integer size, Long studentId, Long fundId) {
        Page<ScoreVO> p = new Page<>(page, size);
        Long sponsorId = null;
        if ("SPONSOR".equals(UserContext.getRole())) {
            sponsorId = UserContext.getUserId();
        }
        return scoreMapper.selectPageWithNames(p, studentId, sponsorId, fundId);
    }

    @Override
    public ScoreVO getById(Long id) {
        ScoreVO score = scoreMapper.selectByIdWithNames(id);
        if (score == null) {
            return null;
        }
        if ("SPONSOR".equals(UserContext.getRole())
                && (score.getSponsorId() == null || !score.getSponsorId().equals(UserContext.getUserId()))) {
            throw new BusinessException(403, "权限不足");
        }
        // 查询明细（联表 score_item）
        List<ScoreDetailVO> details = scoreDetailMapper.selectByScoreIdWithItem(id);
        score.setDetails(details);
        return score;
    }

    /** 新增使用当前规则，编辑使用已保存快照；客户端名称、权重和总分均不可信。 */
    private BigDecimal calcTotal(List<ScoreDetailVO> details, List<ScoreItem> items) {
        if (items == null || items.isEmpty()) throw new BusinessException(400, "请先配置评分指标");
        BigDecimal hundred = new BigDecimal("100");
        Map<Long, ScoreItem> rules = new HashMap<>();
        BigDecimal sum = BigDecimal.ZERO;
        for (ScoreItem item : items) {
            if (item.getWeight() == null || item.getWeight().signum() < 0 || item.getWeight().compareTo(hundred) > 0) {
                throw new BusinessException(400, "指标权重须在0到100之间");
            }
            rules.put(item.getId(), item);
            sum = sum.add(item.getWeight());
        }
        if (sum.compareTo(hundred) != 0) throw new BusinessException(400, "指标权重合计须为100%");
        if (details == null || details.size() != rules.size()) throw new BusinessException(400, "请填写全部评分指标");
        Set<Long> seen = new HashSet<>();
        BigDecimal total = BigDecimal.ZERO;
        for (ScoreDetailVO d : details) {
            if (d == null || !rules.containsKey(d.getItemId()) || !seen.add(d.getItemId())) {
                throw new BusinessException(400, "评分指标不存在或重复，请刷新后重试");
            }
            if (d.getItemScore() == null || d.getItemScore().signum() < 0 || d.getItemScore().compareTo(hundred) > 0) {
                throw new BusinessException(400, "每项得分必须填写且在0到100之间");
            }
            total = total.add(d.getItemScore().multiply(rules.get(d.getItemId()).getWeight())
                    .divide(hundred, 2, RoundingMode.HALF_UP));
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private void validateFund(ScoreVO vo) {
        if (vo.getFundId() == null) throw new BusinessException(400, "请选择关联资助单");
        // 和资助单编辑/删除共用行锁，避免绑定后资助关系被并发改写。
        com.funding.entity.Fund fund = fundMapper.selectOne(new QueryWrapper<com.funding.entity.Fund>()
                .eq("id", vo.getFundId()).last("FOR UPDATE"));
        if (fund == null) throw new BusinessException(400, "关联资助单不存在");
        if (!java.util.Objects.equals(fund.getStudentId(), vo.getStudentId())
                || !java.util.Objects.equals(fund.getSponsorId(), vo.getSponsorId())) {
            throw new BusinessException(400, "资助单与评分的学生、资助人不一致");
        }
    }

    private String ruleVersion(List<ScoreItem> rules) {
        StringBuilder canonical = new StringBuilder();
        rules.stream().sorted(java.util.Comparator.comparing(ScoreItem::getId)).forEach(i -> canonical
                .append(i.getId()).append(':')
                .append(java.util.Base64.getEncoder().encodeToString(i.getName().getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .append(':').append(i.getWeight().stripTrailingZeros().toPlainString()).append(';'));
        return com.funding.util.HashUtils.sha256(canonical.toString());
    }

    private List<ScoreItem> snapshotRules(List<ScoreDetail> saved) {
        List<ScoreItem> items = new ArrayList<>();
        for (ScoreDetail d : saved) {
            if (d.getItemNameSnapshot() == null || d.getWeightSnapshot() == null) {
                throw new BusinessException(409, "评分规则快照不完整，请核查数据，不得使用当前规则代替");
            }
            ScoreItem i = new ScoreItem(); i.setId(d.getItemId());
            i.setName(d.getItemNameSnapshot()); i.setWeight(d.getWeightSnapshot()); items.add(i);
        }
        return items;
    }

    private void insertDetails(Long scoreId, List<ScoreDetailVO> input, List<ScoreItem> rules) {
        Map<Long, ScoreItem> byId = new HashMap<>();
        rules.forEach(i -> byId.put(i.getId(), i));
        for (ScoreDetailVO d : input) {
            ScoreDetail row = new ScoreDetail(); row.setScoreId(scoreId); row.setItemId(d.getItemId());
            row.setItemScore(d.getItemScore()); row.setItemNameSnapshot(byId.get(d.getItemId()).getName());
            row.setWeightSnapshot(byId.get(d.getItemId()).getWeight()); scoreDetailMapper.insert(row);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveScore(ScoreVO vo) {
        validateFund(vo);
        List<ScoreItem> rules = scoreItemMapper.selectList(null);
        BigDecimal total = calcTotal(vo.getDetails(), rules);
        Score score = new Score(); score.setStudentId(vo.getStudentId()); score.setSponsorId(vo.getSponsorId());
        score.setFundId(vo.getFundId()); score.setScoreDate(vo.getScoreDate()); score.setRemark(vo.getRemark());
        score.setTotalScore(total); score.setRuleVersion(ruleVersion(rules));
        score.setRuleCapturedAt(java.time.LocalDateTime.now().withNano(0));
        scoreMapper.insert(score);
        insertDetails(score.getId(), vo.getDetails(), rules);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateScore(ScoreVO vo) {
        validateFund(vo);
        Score old = scoreMapper.selectOne(new QueryWrapper<Score>().eq("id", vo.getId()).last("FOR UPDATE"));
        if (old == null) throw new BusinessException(404, "评分不存在");
        if (!java.util.Objects.equals(old.getStudentId(), vo.getStudentId())
                || !java.util.Objects.equals(old.getSponsorId(), vo.getSponsorId())) {
            throw new BusinessException(400, "历史评分的学生和资助人不可改写，请新建评分");
        }
        List<ScoreDetail> saved = scoreDetailMapper.selectList(new QueryWrapper<ScoreDetail>().eq("score_id", old.getId()));
        Score update = new Score(); update.setId(old.getId()); update.setFundId(vo.getFundId()); update.setRemark(vo.getRemark());
        if (old.getRuleVersion() == null) {
            // 历史规则无法还原：仅允许补关联和备注，不重算、不重写旧分数。
            if (vo.getDetails() != null) {
                Map<Long, BigDecimal> previous = new HashMap<>(); saved.forEach(d -> previous.put(d.getItemId(), d.getItemScore()));
                Set<Long> seen = new HashSet<>();
                if (vo.getDetails().size() != saved.size()) throw legacyError();
                for (ScoreDetailVO d : vo.getDetails()) {
                    if (d == null || !seen.add(d.getItemId()) || d.getItemScore() == null
                            || previous.get(d.getItemId()) == null || previous.get(d.getItemId()).compareTo(d.getItemScore()) != 0) throw legacyError();
                }
            }
            if (vo.getScoreDate() != null && !vo.getScoreDate().equals(old.getScoreDate())) throw legacyError();
            scoreMapper.updateById(update);
            return;
        }
        List<ScoreItem> rules = snapshotRules(saved);
        if (!old.getRuleVersion().equals(ruleVersion(rules))) throw new BusinessException(409, "规则快照校验失败，请核查数据");
        update.setScoreDate(vo.getScoreDate()); update.setTotalScore(calcTotal(vo.getDetails(), rules));
        scoreMapper.updateById(update);
        scoreDetailMapper.delete(new QueryWrapper<ScoreDetail>().eq("score_id", old.getId()));
        insertDetails(old.getId(), vo.getDetails(), rules);
    }

    private BusinessException legacyError() {
        return new BusinessException(400, "旧评分未留存规则，仅可补充资助单关联和备注；调整得分请新建评分");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteScore(Long id) {
        scoreMapper.deleteById(id);
        scoreDetailMapper.delete(new QueryWrapper<ScoreDetail>().eq("score_id", id));
    }

    @Override
    public void export(HttpServletResponse response) throws Exception {
        List<ScoreVO> list = scoreMapper.selectAllWithNames();
        List<String> headers = Arrays.asList(
                "ID", "学生", "资助人", "评分日期", "总分", "各指标分", "备注", "创建时间", "关联资助单", "规则版本", "规则留存时间");
        List<List<Object>> rows = new ArrayList<>();
        for (ScoreVO s : list) {
            // 查询明细拼装"学习情况:90,生活改善:85,..."到一列
            List<ScoreDetailVO> details = scoreDetailMapper.selectByScoreIdWithItem(s.getId());
            List<Object> row = new ArrayList<>();
            row.add(s.getId());
            row.add(s.getStudentName());
            row.add(s.getSponsorName());
            row.add(s.getScoreDate() != null ? s.getScoreDate().toString() : "");
            row.add(s.getTotalScore());
            StringBuilder sb = new StringBuilder();
            for (ScoreDetailVO d : details) {
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(d.getItemName()).append(":").append(d.getItemScore()).append("（权重:")
                        .append(d.getWeight() == null ? "未留存" : d.getWeight()).append("）");
            }
            row.add(sb.toString());
            row.add(s.getRemark() != null ? s.getRemark() : "");
            row.add(s.getCreateTime() != null ? s.getCreateTime().toString() : "");
            row.add(s.getFundNo() == null ? "历史未关联" : s.getFundNo());
            row.add(s.getRuleVersion() == null ? "历史规则未留存" : s.getRuleVersion());
            row.add(s.getRuleCapturedAt() == null ? "" : s.getRuleCapturedAt().toString());
            rows.add(row);
        }
        ExcelUtils.writeWorkbook(response, "评分记录.xlsx", headers, rows);
    }
}
