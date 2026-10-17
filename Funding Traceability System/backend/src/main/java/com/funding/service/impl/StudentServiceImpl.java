package com.funding.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.funding.common.BusinessException;
import com.funding.common.UserContext;
import com.funding.entity.Student;
import com.funding.mapper.StudentMapper;
import com.funding.service.StudentService;
import com.funding.util.ExcelUtils;
import com.funding.vo.OptionVO;
import com.funding.vo.StudentVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 学生服务实现
 * SPONSOR 查询一律按当前登录 userId 过滤 sponsor_id
 */
@Service
public class StudentServiceImpl implements StudentService {

    @Autowired
    private StudentMapper studentMapper;

    @Override
    public IPage<StudentVO> page(Integer page, Integer size, String keyword) {
        Page<StudentVO> p = new Page<>(page, size);
        Long sponsorId = null;
        if ("SPONSOR".equals(UserContext.getRole())) {
            sponsorId = UserContext.getUserId();
        }
        return studentMapper.selectPageWithSponsor(p, keyword, sponsorId);
    }

    @Override
    public StudentVO getById(Long id) {
        StudentVO student = studentMapper.selectByIdWithSponsor(id);
        if (student == null) {
            return null;
        }
        // 数据隔离：SPONSOR 只能查看自己名下学生
        if ("SPONSOR".equals(UserContext.getRole())
                && (student.getSponsorId() == null || !student.getSponsorId().equals(UserContext.getUserId()))) {
            throw new BusinessException(403, "权限不足");
        }
        return student;
    }

    @Override
    public void saveStudent(Student student) {
        // 新增时 id 由数据库自增，避免外部传入
        student.setId(null);
        studentMapper.insert(student);
    }

    @Override
    public void updateStudent(Student student) {
        studentMapper.updateById(student);
    }

    @Override
    public void deleteStudent(Long id) {
        studentMapper.deleteById(id);
    }

    @Override
    public List<OptionVO> options() {
        Long sponsorId = null;
        if ("SPONSOR".equals(UserContext.getRole())) {
            sponsorId = UserContext.getUserId();
        }
        return studentMapper.selectOptions(sponsorId);
    }

    @Override
    public void export(HttpServletResponse response) throws Exception {
        // export 仅 AUDIT_ADMIN 可调用（Controller 已 @RequirePermission("student:manage")），导出全部
        List<StudentVO> list = studentMapper.selectAllWithSponsor();
        List<String> headers = Arrays.asList(
                "ID", "姓名", "性别", "身份证号", "学校", "年级",
                "家庭住址", "家庭情况", "电话", "资助人", "创建时间");
        List<List<Object>> rows = new ArrayList<>();
        for (StudentVO s : list) {
            List<Object> row = new ArrayList<>();
            row.add(s.getId());
            row.add(s.getName());
            row.add(s.getGender());
            row.add(s.getIdCard());
            row.add(s.getSchool());
            row.add(s.getGrade());
            row.add(s.getFamilyAddress());
            row.add(s.getFamilySituation());
            row.add(s.getPhone());
            row.add(s.getSponsorName());
            row.add(s.getCreateTime() != null ? s.getCreateTime().toString() : "");
            rows.add(row);
        }
        ExcelUtils.writeWorkbook(response, "学生列表.xlsx", headers, rows);
    }
}
