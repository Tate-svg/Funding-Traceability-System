package com.funding.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.funding.vo.ScoreVO;

import javax.servlet.http.HttpServletResponse;

/**
 * 评分服务
 */
public interface ScoreService {

    default IPage<ScoreVO> page(Integer page, Integer size, Long studentId) {
        return page(page, size, studentId, null);
    }

    IPage<ScoreVO> page(Integer page, Integer size, Long studentId, Long fundId);

    java.util.List<com.funding.vo.OptionVO> fundOptions(Long studentId, Long sponsorId, String keyword);

    ScoreVO getById(Long id);

    /**
     * 新增评分（后端按 score_item.weight 加权自动算 total_score）
     */
    void saveScore(ScoreVO scoreVO);

    /**
     * 更新评分（重新计算 total_score，替换全部明细）
     */
    void updateScore(ScoreVO scoreVO);

    void deleteScore(Long id);

    void export(HttpServletResponse response) throws Exception;
}
