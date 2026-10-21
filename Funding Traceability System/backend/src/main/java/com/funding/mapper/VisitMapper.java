package com.funding.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.funding.entity.Visit;
import com.funding.vo.VisitVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 回访记录 Mapper
 */
public interface VisitMapper extends BaseMapper<Visit> {

    /**
     * 学生姓名取业务库脱敏名 mask_name，敏感明文存于隐私库，不在此联查
     */
    String NAME_JOIN =
            "LEFT JOIN student s ON v.student_id = s.id " +
            "LEFT JOIN sys_user u ON v.sponsor_id = u.id " +
            "LEFT JOIN fund f ON v.fund_id = f.id ";

    String NAME_COLUMNS =
            "v.*, s.mask_name AS student_name, u.real_name AS sponsor_name, f.voucher_no AS voucher_no ";

    @Select("<script>" +
            "SELECT " + NAME_COLUMNS + "FROM visit v " + NAME_JOIN +
            "<where>" +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (s.mask_name LIKE CONCAT('%',#{keyword},'%') OR v.content LIKE CONCAT('%',#{keyword},'%') OR v.situation LIKE CONCAT('%',#{keyword},'%') OR v.visitor LIKE CONCAT('%',#{keyword},'%'))" +
            "</if>" +
            "<if test='studentId != null'>" +
            "AND v.student_id = #{studentId}" +
            "</if>" +
            "<if test='sponsorId != null'>" +
            "AND v.sponsor_id = #{sponsorId}" +
            "</if>" +
            "<if test='fundId != null'>" +
            "AND v.fund_id = #{fundId}" +
            "</if>" +
            "</where>" +
            "ORDER BY v.id DESC" +
            "</script>")
    IPage<VisitVO> selectPageWithNames(Page<VisitVO> page,
                                       @Param("keyword") String keyword,
                                       @Param("studentId") Long studentId,
                                       @Param("sponsorId") Long sponsorId,
                                       @Param("fundId") Long fundId);

    @Select("SELECT " + NAME_COLUMNS + "FROM visit v " + NAME_JOIN + "WHERE v.id = #{id}")
    VisitVO selectByIdWithNames(@Param("id") Long id);

    @Select("SELECT " + NAME_COLUMNS + "FROM visit v " + NAME_JOIN + "ORDER BY v.id DESC")
    List<VisitVO> selectAllWithNames();
}
