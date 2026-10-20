package com.funding.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.funding.entity.ReviewTask;
import com.funding.vo.ReviewTaskVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 复核抽检任务 Mapper
 */
public interface ReviewTaskMapper extends BaseMapper<ReviewTask> {

    @Select("<script>" +
            "SELECT t.*, f.amount, f.voucher_no, f.status AS fund_status, s.mask_name AS student_name, u.real_name AS sponsor_name " +
            "FROM review_task t " +
            "LEFT JOIN fund f ON t.fund_id = f.id " +
            "LEFT JOIN student s ON f.student_id = s.id " +
            "LEFT JOIN sys_user u ON f.sponsor_id = u.id " +
            "<where>" +
            "<if test='batchNo != null and batchNo != \"\"'>" +
            "AND t.batch_no = #{batchNo}" +
            "</if>" +
            "<if test='status != null and status != \"\"'>" +
            "AND t.status = #{status}" +
            "</if>" +
            "</where>" +
            "ORDER BY t.id DESC" +
            "</script>")
    IPage<ReviewTaskVO> selectPageWithFund(Page<ReviewTaskVO> page,
                                           @Param("batchNo") String batchNo,
                                           @Param("status") String status);
}
