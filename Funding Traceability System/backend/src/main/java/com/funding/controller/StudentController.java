package com.funding.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.funding.common.OperationLog;
import com.funding.common.RequirePermission;
import com.funding.common.Result;
import com.funding.service.StudentService;
import com.funding.vo.OptionVO;
import com.funding.vo.StudentSaveRequest;
import com.funding.vo.StudentVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 学生接口
 */
@RestController
@RequestMapping("/api/students")
public class StudentController {

    @Autowired
    private StudentService studentService;

    @Autowired
    private com.funding.service.StudentAttachmentService attachmentService;

    @GetMapping
    public Result<IPage<StudentVO>> page(@RequestParam(defaultValue = "1") Integer page,
                                          @RequestParam(defaultValue = "10") Integer size,
                                          @RequestParam(required = false) String keyword) {
        return Result.success(studentService.page(page, size, keyword));
    }

    @GetMapping("/options")
    public Result<List<OptionVO>> options() {
        return Result.success(studentService.options());
    }

    @GetMapping("/export")
    @RequirePermission("student:manage")
    @OperationLog(value = "导出学生档案", type = "student")
    public void export(HttpServletResponse response) throws Exception {
        studentService.export(response);
    }

    @GetMapping("/{id}")
    public Result<StudentVO> getById(@PathVariable Long id) {
        return Result.success(studentService.getById(id));
    }

    @PostMapping
    @RequirePermission("student:manage")
    @OperationLog(value = "新增学生", type = "student")
    public Result<Void> save(@RequestBody StudentSaveRequest request) {
        studentService.saveStudent(request);
        return Result.success();
    }

    @PutMapping("/{id}")
    @RequirePermission("student:manage")
    @OperationLog(value = "修改学生", type = "student")
    public Result<Void> update(@PathVariable Long id, @RequestBody StudentSaveRequest request) {
        studentService.updateStudent(id, request);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequirePermission("student:manage")
    @OperationLog(value = "删除学生", type = "student")
    public Result<Void> delete(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return Result.success();
    }

    /**
     * 上传证明材料：自动脱敏扫描，返回命中区间供前端预览调整
     */
    @PostMapping("/{id}/attachments")
    @RequirePermission("student:manage")
    @OperationLog(value = "上传学生证明材料", type = "student")
    public Result<java.util.Map<String, Object>> uploadAttachment(@PathVariable Long id,
                                                                  @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        return Result.success(attachmentService.upload(id, file));
    }

    /**
     * 确认脱敏文本入库：仍含未打码敏感内容则拒绝
     */
    @PostMapping("/{id}/attachments/{attId}/confirm")
    @RequirePermission("student:manage")
    @OperationLog(value = "确认材料脱敏", type = "student")
    public Result<Void> confirmAttachment(@PathVariable Long id, @PathVariable Long attId,
                                          @RequestBody java.util.Map<String, String> body) {
        attachmentService.confirm(id, attId, body.get("maskedText"));
        return Result.success();
    }

    /**
     * 某学生的材料列表
     */
    @GetMapping("/{id}/attachments")
    @RequirePermission("student:manage")
    public Result<java.util.List<com.funding.entity.StudentAttachment>> listAttachments(@PathVariable Long id) {
        return Result.success(attachmentService.listByStudent(id));
    }
}
