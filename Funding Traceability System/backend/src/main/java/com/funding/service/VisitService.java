package com.funding.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.funding.entity.Visit;
import com.funding.vo.VisitVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;

/**
 * 回访服务
 */
public interface VisitService {

    IPage<VisitVO> page(Integer page, Integer size, Long studentId, Long fundId, String keyword);

    VisitVO getById(Long id);

    void saveVisit(Visit visit);

    void updateVisit(Visit visit);

    void deleteVisit(Long id);

    /**
     * 上传回访凭证材料（照片/访谈记录等），自动脱敏后存储并回写 masked_material
     *
     * @param manuallyMasked 图片/文档类材料是否已人工脱敏并确认
     * @return 脱敏后存储的文件名
     */
    String uploadMaterial(Long id, MultipartFile file, boolean manuallyMasked) throws Exception;

    /**
     * 下载脱敏后的回访凭证材料
     */
    void downloadMaterial(Long id, HttpServletResponse response) throws Exception;

    void export(HttpServletResponse response) throws Exception;
}
