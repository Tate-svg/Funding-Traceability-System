package com.funding.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.funding.entity.Fund;
import com.funding.vo.DashboardVO;
import com.funding.vo.FundVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

/**
 * 资金流水 Mapper
 */
public interface FundMapper extends BaseMapper<Fund> {

    String NAME_JOIN =
            "LEFT JOIN student s ON f.student_id = s.id " +
            "LEFT JOIN sys_user u ON f.sponsor_id = u.id " +
            "LEFT JOIN sys_user au ON f.audit_by = au.id " +
            "LEFT JOIN sys_user du ON f.disburse_by = du.id ";

    String NAME_COLUMNS =
            "f.*, s.mask_name AS student_name, u.real_name AS sponsor_name, " +
            "au.real_name AS auditor_name, du.real_name AS disburser_name ";

    @Select("<script>" +
            "SELECT " + NAME_COLUMNS + "FROM fund f " + NAME_JOIN +
            "<where>" +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (s.mask_name LIKE CONCAT('%',#{keyword},'%') OR f.purpose LIKE CONCAT('%',#{keyword},'%') OR f.remark LIKE CONCAT('%',#{keyword},'%'))" +
            "</if>" +
            "<if test='studentId != null'>" +
            "AND f.student_id = #{studentId}" +
            "</if>" +
            "<if test='sponsorId != null'>" +
            "AND f.sponsor_id = #{sponsorId}" +
            "</if>" +
            "<if test='status != null and status != \"\"'>" +
            "AND f.status = #{status}" +
            "</if>" +
            "</where>" +
            "ORDER BY f.id DESC" +
            "</script>")
    IPage<FundVO> selectPageWithNames(Page<FundVO> page,
                                      @Param("keyword") String keyword,
                                      @Param("studentId") Long studentId,
                                      @Param("sponsorId") Long sponsorId,
                                      @Param("status") String status);

    @Select("SELECT " + NAME_COLUMNS + "FROM fund f " + NAME_JOIN + "WHERE f.id = #{id}")
    FundVO selectByIdWithNames(@Param("id") Long id);

    @Select("SELECT " + NAME_COLUMNS + "FROM fund f " + NAME_JOIN + "ORDER BY f.id DESC")
    List<FundVO> selectAllWithNames();

    /**
     * 待拨付列表：已审核小额（&lt;阈值）或 已复核
     */
    @Select("<script>" +
            "SELECT " + NAME_COLUMNS + "FROM fund f " + NAME_JOIN +
            "WHERE (f.status = 'AUDITED' AND f.amount &lt; #{threshold}) OR f.status = 'REVIEWED' " +
            "ORDER BY f.id DESC" +
            "</script>")
    IPage<FundVO> selectPendingDisburse(Page<FundVO> page, @Param("threshold") BigDecimal threshold);

    @Select("SELECT COUNT(*) FROM fund WHERE status = 'SUBMITTED'")
    long countPendingAudit();

    @Select("SELECT COUNT(*) FROM fund WHERE voucher_no LIKE CONCAT('Voucher-', #{day}, '-%')")
    long countVoucherOfDay(@Param("day") String day);

    /**
     * 复核候选池：已审核大额 且 未被抽检过
     */
    @Select("<script>" +
            "SELECT * FROM fund WHERE status = 'AUDITED' AND amount &gt;= #{threshold} " +
            "AND id NOT IN (SELECT fund_id FROM review_task) " +
            "<if test='minAmount != null'>AND amount &gt;= #{minAmount}</if>" +
            "<if test='maxAmount != null'>AND amount &lt;= #{maxAmount}</if>" +
            "ORDER BY id" +
            "</script>")
    List<Fund> selectReviewCandidates(@Param("threshold") BigDecimal threshold,
                                      @Param("minAmount") BigDecimal minAmount,
                                      @Param("maxAmount") BigDecimal maxAmount);

    /**
     * 候选池中随机抽取 n 条
     */
    @Select("<script>" +
            "SELECT * FROM fund WHERE status = 'AUDITED' AND amount &gt;= #{threshold} " +
            "AND id NOT IN (SELECT fund_id FROM review_task) " +
            "<if test='minAmount != null'>AND amount &gt;= #{minAmount}</if>" +
            "<if test='maxAmount != null'>AND amount &lt;= #{maxAmount}</if>" +
            "ORDER BY RAND() LIMIT #{n}" +
            "</script>")
    List<Fund> selectRandomCandidates(@Param("threshold") BigDecimal threshold,
                                      @Param("minAmount") BigDecimal minAmount,
                                      @Param("maxAmount") BigDecimal maxAmount,
                                      @Param("n") int n);

    // ===== 统计相关 =====

    @Select("<script>" +
            "SELECT IFNULL(SUM(amount),0) FROM fund " +
            "<where><if test='sponsorId != null'>sponsor_id = #{sponsorId}</if></where>" +
            "</script>")
    BigDecimal selectTotalAmount(@Param("sponsorId") Long sponsorId);

    @Select("<script>" +
            "SELECT DATE_FORMAT(fund_date, '%Y-%m') AS month, IFNULL(SUM(amount),0) AS amount FROM fund " +
            "<where><if test='sponsorId != null'>sponsor_id = #{sponsorId}</if></where> " +
            "GROUP BY month ORDER BY month" +
            "</script>")
    List<DashboardVO.MonthlyFund> selectMonthlyFunds(@Param("sponsorId") Long sponsorId);

    @Select("<script>" +
            "SELECT purpose AS name, COUNT(*) AS value FROM fund " +
            "<where><if test='sponsorId != null'>sponsor_id = #{sponsorId}</if></where> " +
            "GROUP BY purpose" +
            "</script>")
    List<DashboardVO.PurposeDist> selectPurposeDistribution(@Param("sponsorId") Long sponsorId);
}
