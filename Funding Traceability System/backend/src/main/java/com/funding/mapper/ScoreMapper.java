package com.funding.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.funding.entity.Score;
import com.funding.vo.ScoreVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 评分记录 Mapper
 */
public interface ScoreMapper extends BaseMapper<Score> {

    @Select("<script>" +
            "SELECT sc.*, COALESCE(f.voucher_no, CONCAT('FUND-', sc.fund_id)) AS fund_no, s.mask_name AS student_name, u.real_name AS sponsor_name FROM score sc " +
            "LEFT JOIN fund f ON sc.fund_id = f.id " +
            "LEFT JOIN student s ON sc.student_id = s.id " +
            "LEFT JOIN sys_user u ON sc.sponsor_id = u.id " +
            "<where>" +
            "<if test='studentId != null'>" +
            "AND sc.student_id = #{studentId}" +
            "</if>" +
            "<if test='sponsorId != null'>" +
            "AND sc.sponsor_id = #{sponsorId}" +
            "</if>" +
            "<if test='fundId != null'>AND sc.fund_id = #{fundId}</if>" +
            "</where>" +
            "ORDER BY sc.id DESC" +
            "</script>")
    IPage<ScoreVO> selectPageWithNames(Page<ScoreVO> page,
                                       @Param("studentId") Long studentId,
                                       @Param("sponsorId") Long sponsorId,
                                       @Param("fundId") Long fundId);

    @Select("SELECT sc.*, COALESCE(f.voucher_no, CONCAT('FUND-', sc.fund_id)) AS fund_no, s.mask_name AS student_name, u.real_name AS sponsor_name FROM score sc " +
            "LEFT JOIN fund f ON sc.fund_id = f.id " +
            "LEFT JOIN student s ON sc.student_id = s.id " +
            "LEFT JOIN sys_user u ON sc.sponsor_id = u.id WHERE sc.id = #{id}")
    ScoreVO selectByIdWithNames(@Param("id") Long id);

    @Select("SELECT sc.*, COALESCE(f.voucher_no, CONCAT('FUND-', sc.fund_id)) AS fund_no, s.mask_name AS student_name, u.real_name AS sponsor_name FROM score sc " +
            "LEFT JOIN fund f ON sc.fund_id = f.id " +
            "LEFT JOIN student s ON sc.student_id = s.id " +
            "LEFT JOIN sys_user u ON sc.sponsor_id = u.id ORDER BY sc.id DESC")
    List<ScoreVO> selectAllWithNames();
}
