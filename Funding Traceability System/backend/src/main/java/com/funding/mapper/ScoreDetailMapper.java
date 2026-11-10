package com.funding.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.funding.entity.ScoreDetail;
import com.funding.vo.DashboardVO;
import com.funding.vo.ScoreDetailVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 评分明细 Mapper
 */
public interface ScoreDetailMapper extends BaseMapper<ScoreDetail> {

    /**
     * 只读取评分时的快照；旧记录不使用当前规则冒充历史规则。
     */
    @Select("SELECT sd.id, sd.score_id, sd.item_id, " +
            "COALESCE(sd.item_name_snapshot, CONCAT('历史指标#', sd.item_id, '（名称未留存）')) AS item_name, " +
            "sd.weight_snapshot AS weight, " +
            "sd.item_score " +
            "FROM score_detail sd " +
            "WHERE sd.score_id = #{scoreId} ORDER BY sd.id")
    List<ScoreDetailVO> selectByScoreIdWithItem(@Param("scoreId") Long scoreId);

    /**
     * 统计各指标的平均分（按资助人过滤）
     */
    @Select("<script>" +
            "SELECT CONCAT(COALESCE(sd.item_name_snapshot, CONCAT('历史指标#',sd.item_id)), " +
            "CASE WHEN s.rule_version IS NULL THEN '（规则未留存）' ELSE CONCAT('〔',LEFT(s.rule_version,8),'〕') END) AS item_name, IFNULL(AVG(sd.item_score),0) AS avg " +
            "FROM score_detail sd " +

            "LEFT JOIN score s ON sd.score_id = s.id " +
            "<where><if test='sponsorId != null'>s.sponsor_id = #{sponsorId}</if></where> " +
            "GROUP BY s.rule_version, sd.item_id, sd.item_name_snapshot" +
            "</script>")
    List<DashboardVO.ScoreAvg> selectScoreAverages(@Param("sponsorId") Long sponsorId);
}
