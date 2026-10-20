package com.funding.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.funding.common.BusinessException;
import com.funding.common.MaskingGuard;
import com.funding.common.UserContext;
import com.funding.entity.PrivacyProfile;
import com.funding.entity.Student;
import com.funding.mapper.StudentMapper;
import com.funding.privacymapper.PrivacyProfileMapper;
import com.funding.service.StudentService;
import com.funding.util.CryptoUtils;
import com.funding.util.ExcelUtils;
import com.funding.vo.OptionVO;
import com.funding.vo.StudentSaveRequest;
import com.funding.vo.StudentVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 学生服务实现
 * SPONSOR 查询一律按当前登录 userId 过滤 sponsor_id；
 * 隐私字段加密写隐私库（双库隔离），业务库只落虚拟ID + 脱敏名 + 非敏感字段
 */
@Service
public class StudentServiceImpl implements StudentService {

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private PrivacyProfileMapper privacyProfileMapper;

    @Autowired
    private MaskingGuard maskingGuard;

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
    public void saveStudent(StudentSaveRequest request) {
        // 二次脱敏校验：家庭情况自由文本不得夹带未打码敏感信息
        maskingGuard.check("POST /api/students", "家庭情况", request.getFamilySituation());

        // 业务库：虚拟ID + 脱敏名 + 非敏感字段
        Student student = new Student();
        student.setVirtualId(nextVirtualId());
        student.setMaskName(maskName(request.getName()));
        student.setGender(request.getGender());
        student.setSchool(request.getSchool());
        student.setGrade(request.getGrade());
        student.setSponsorId(request.getSponsorId());
        studentMapper.insert(student);

        // 隐私库：五字段 AES 加密落库（失败时提示重试，业务库记录可删除重建）
        try {
            PrivacyProfile profile = new PrivacyProfile();
            profile.setVirtualId(student.getVirtualId());
            profile.setNameEnc(CryptoUtils.encrypt(request.getName()));
            profile.setIdCardEnc(CryptoUtils.encrypt(request.getIdCard()));
            profile.setPhoneEnc(CryptoUtils.encrypt(request.getPhone()));
            profile.setFamilyAddressEnc(CryptoUtils.encrypt(request.getFamilyAddress()));
            profile.setFamilySituationEnc(CryptoUtils.encrypt(request.getFamilySituation()));
            privacyProfileMapper.insert(profile);
        } catch (Exception e) {
            throw new BusinessException(500, "隐私档案写入失败，请删除该学生后重新录入");
        }
    }

    @Override
    public void updateStudent(Long id, StudentSaveRequest request) {
        maskingGuard.check("PUT /api/students/" + id, "家庭情况", request.getFamilySituation());

        Student existing = studentMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(404, "学生不存在");
        }
        Student update = new Student();
        update.setId(id);
        // 姓名为空表示不修改隐私，脱敏名保持不变
        if (request.getName() != null && !request.getName().isEmpty()) {
            update.setMaskName(maskName(request.getName()));
        }
        update.setGender(request.getGender());
        update.setSchool(request.getSchool());
        update.setGrade(request.getGrade());
        update.setSponsorId(request.getSponsorId());
        studentMapper.updateById(update);

        PrivacyProfile profile = privacyProfileMapper.selectOne(
                new QueryWrapper<PrivacyProfile>().eq("virtual_id", existing.getVirtualId()));
        if (profile != null) {
            // 隐私字段留空则保持原值，只更新本次填写的字段
            if (request.getName() != null && !request.getName().isEmpty()) {
                profile.setNameEnc(CryptoUtils.encrypt(request.getName()));
            }
            if (request.getIdCard() != null && !request.getIdCard().isEmpty()) {
                profile.setIdCardEnc(CryptoUtils.encrypt(request.getIdCard()));
            }
            if (request.getPhone() != null && !request.getPhone().isEmpty()) {
                profile.setPhoneEnc(CryptoUtils.encrypt(request.getPhone()));
            }
            if (request.getFamilyAddress() != null && !request.getFamilyAddress().isEmpty()) {
                profile.setFamilyAddressEnc(CryptoUtils.encrypt(request.getFamilyAddress()));
            }
            if (request.getFamilySituation() != null && !request.getFamilySituation().isEmpty()) {
                profile.setFamilySituationEnc(CryptoUtils.encrypt(request.getFamilySituation()));
            }
            privacyProfileMapper.updateById(profile);
        }
    }

    @Override
    public void deleteStudent(Long id) {
        Student existing = studentMapper.selectById(id);
        studentMapper.deleteById(id);
        // 级联删除隐私档案，保证隐私数据不留存
        if (existing != null && existing.getVirtualId() != null) {
            privacyProfileMapper.delete(
                    new QueryWrapper<PrivacyProfile>().eq("virtual_id", existing.getVirtualId()));
        }
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
        // export 仅 AUDIT_ADMIN 可调用；导出即脱敏态（隐私明文不落业务库，导出自然无明文）
        List<StudentVO> list = studentMapper.selectAllWithSponsor();
        List<String> headers = Arrays.asList(
                "ID", "虚拟ID", "姓名(脱敏)", "性别", "学校", "年级", "资助人", "创建时间");
        List<List<Object>> rows = new ArrayList<>();
        for (StudentVO s : list) {
            List<Object> row = new ArrayList<>();
            row.add(s.getId());
            row.add(s.getVirtualId());
            row.add(s.getMaskName());
            row.add(s.getGender());
            row.add(s.getSchool());
            row.add(s.getGrade());
            row.add(s.getSponsorName());
            row.add(s.getCreateTime() != null ? s.getCreateTime().toString() : "");
            rows.add(row);
        }
        ExcelUtils.writeWorkbook(response, "学生列表.xlsx", headers, rows);
    }

    /** 生成全局唯一虚拟ID：STU- + 12位hex */
    private String nextVirtualId() {
        String vid;
        do {
            vid = "STU-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        } while (studentMapper.selectCount(new QueryWrapper<Student>().eq("virtual_id", vid)) > 0);
        return vid;
    }

    /** 脱敏展示名：保留首字，如 李明 -> 李** */
    private String maskName(String name) {
        if (name == null || name.isEmpty()) {
            return "***";
        }
        return name.substring(0, 1) + "**";
    }
}
