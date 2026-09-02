package com.bom.controller;

import com.bom.entity.Material;
import com.bom.service.AuditLogService;
import com.bom.service.MaterialService;
import com.bom.util.PageResult;
import com.bom.util.Result;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/material")
public class MaterialController {

    @Autowired
    private MaterialService materialService;

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping("/search")
    public Result search(@RequestParam(required = false) String keyword,
                         @RequestParam(defaultValue = "1") int page,
                         @RequestParam(defaultValue = "20") int size) {
        PageResult<Material> result = materialService.searchPaged(keyword, page, size);
        return Result.success(result);
    }

    @PostMapping("/import")
    public Result importExcel(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            InputStream is = file.getInputStream();
            Workbook workbook = new XSSFWorkbook(is);
            Sheet sheet = workbook.getSheetAt(0);
            List<Material> list = new ArrayList<>();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Material m = new Material();
                m.setMaterialCode(getCellStringValue(row.getCell(0)));
                m.setMaterialName(getCellStringValue(row.getCell(1)));
                m.setSpecification(getCellStringValue(row.getCell(2)));
                m.setMaterialType(getCellStringValue(row.getCell(3)));
                m.setUnit(getCellStringValue(row.getCell(4)));
                String weightStr = getCellStringValue(row.getCell(5));
                if (weightStr != null && !weightStr.isEmpty()) {
                    m.setWeight(new BigDecimal(weightStr));
                }
                m.setMaterialAttr(getCellStringValue(row.getCell(6)));
                m.setSource(getCellStringValue(row.getCell(7)));
                m.setCreatedBy(userId);
                list.add(m);
            }

            workbook.close();
            materialService.batchImport(list);
            auditLogService.log(userId, "导入", "MATERIAL", null,
                    "批量导入 " + list.size() + " 条物料", request.getRemoteAddr());
            return Result.success("导入成功，共 " + list.size() + " 条", null);
        } catch (Exception e) {
            return Result.error("导入失败: " + e.getMessage());
        }
    }

    @PostMapping
    public Result add(@RequestBody Material material, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        material.setCreatedBy(userId);
        materialService.add(material);
        auditLogService.log(userId, "新增", "MATERIAL", material.getId(),
                material.getMaterialName() + "(" + material.getMaterialCode() + ")", request.getRemoteAddr());
        return Result.success("添加成功", material);
    }

    @PutMapping
    public Result update(@RequestBody Material material, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        materialService.update(material);
        auditLogService.log(userId, "更新", "MATERIAL", material.getId(),
                material.getMaterialName() + "(" + material.getMaterialCode() + ")", request.getRemoteAddr());
        return Result.success("更新成功", null);
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Material material = materialService.getById(id);
        materialService.delete(id);
        auditLogService.log(userId, "删除", "MATERIAL", id,
                material != null ? material.getMaterialName() + "(" + material.getMaterialCode() + ")" : "id=" + id, request.getRemoteAddr());
        return Result.success("删除成功", null);
    }

    @GetMapping("/list")
    public Result list(@RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "20") int size) {
        PageResult<Material> result = materialService.getAllPaged(page, size);
        return Result.success(result);
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) return null;
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue();
    }
}
