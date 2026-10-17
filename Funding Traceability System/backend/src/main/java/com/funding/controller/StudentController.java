package com.funding.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.funding.common.OperationLog;
import com.funding.common.RequirePermission;
import com.funding.common.Result;
import com.funding.entity.Student;
import com.funding.service.StudentService;
import com.funding.vo.OptionVO;
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
    public Result<Void> save(@RequestBody Student student) {
        studentService.saveStudent(student);
        return Result.success();
    }

    @PutMapping("/{id}")
    @RequirePermission("student:manage")
    @OperationLog(value = "修改学生", type = "student")
    public Result<Void> update(@PathVariable Long id, @RequestBody Student student) {
        student.setId(id);
        studentService.updateStudent(student);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequirePermission("student:manage")
    @OperationLog(value = "删除学生", type = "student")
    public Result<Void> delete(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return Result.success();
    }
}
