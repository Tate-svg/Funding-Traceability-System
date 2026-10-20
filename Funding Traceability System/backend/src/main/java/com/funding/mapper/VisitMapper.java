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

    @Select("<script>" +
            "SELECT v.*, s.mask_name AS student_name, u.real_name AS sponsor_name FROM visit v " +
            "LEFT JOIN student s ON v.student_id = s.id " +
            "LEFT JOIN sys_user u ON v.sponsor_id = u.id " +
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
            "</where>" +
            "ORDER BY v.id DESC" +
            "</script>")
    IPage<VisitVO> selectPageWithNames(Page<VisitVO> page,
                                       @Param("keyword") String keyword,
                                       @Param("studentId") Long studentId,
                                       @Param("sponsorId") Long sponsorId);

    @Select("SELECT v.*, s.mask_name AS student_name, u.real_name AS sponsor_name FROM visit v " +
            "LEFT JOIN student s ON v.student_id = s.id " +
            "LEFT JOIN sys_user u ON v.sponsor_id = u.id WHERE v.id = #{id}")
    VisitVO selectByIdWithNames(@Param("id") Long id);

    @Select("SELECT v.*, s.mask_name AS student_name, u.real_name AS sponsor_name FROM visit v " +
            "LEFT JOIN student s ON v.student_id = s.id " +
            "LEFT JOIN sys_user u ON v.sponsor_id = u.id ORDER BY v.id DESC")
    List<VisitVO> selectAllWithNames();
}
