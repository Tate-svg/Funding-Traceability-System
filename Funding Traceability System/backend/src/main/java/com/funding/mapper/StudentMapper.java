package com.funding.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.funding.entity.Student;
import com.funding.vo.OptionVO;
import com.funding.vo.StudentVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 学生 Mapper
 */
public interface StudentMapper extends BaseMapper<Student> {

    /**
     * 分页查询（联表 sys_user 获取 sponsorName）
     */
    @Select("<script>" +
            "SELECT s.*, u.real_name AS sponsor_name FROM student s " +
            "LEFT JOIN sys_user u ON s.sponsor_id = u.id " +
            "<where>" +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (s.mask_name LIKE CONCAT('%',#{keyword},'%') OR s.school LIKE CONCAT('%',#{keyword},'%') OR s.virtual_id LIKE CONCAT('%',#{keyword},'%'))" +
            "</if>" +
            "<if test='sponsorId != null'>" +
            "AND s.sponsor_id = #{sponsorId}" +
            "</if>" +
            "</where>" +
            "ORDER BY s.id DESC" +
            "</script>")
    IPage<StudentVO> selectPageWithSponsor(Page<StudentVO> page,
                                           @Param("keyword") String keyword,
                                           @Param("sponsorId") Long sponsorId);

    /**
     * 单条查询（联表）
     */
    @Select("SELECT s.*, u.real_name AS sponsor_name FROM student s " +
            "LEFT JOIN sys_user u ON s.sponsor_id = u.id WHERE s.id = #{id}")
    StudentVO selectByIdWithSponsor(@Param("id") Long id);

    /**
     * 下拉选项
     */
    @Select("<script>" +
            "SELECT s.id, s.mask_name AS name FROM student s " +
            "<where>" +
            "<if test='sponsorId != null'>s.sponsor_id = #{sponsorId}</if>" +
            "</where>" +
            "ORDER BY s.id" +
            "</script>")
    List<OptionVO> selectOptions(@Param("sponsorId") Long sponsorId);

    /**
     * 全量查询（导出用，ADMIN 可见全部）
     */
    @Select("SELECT s.*, u.real_name AS sponsor_name FROM student s " +
            "LEFT JOIN sys_user u ON s.sponsor_id = u.id ORDER BY s.id DESC")
    List<StudentVO> selectAllWithSponsor();
}
