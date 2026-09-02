package com.bom.service;

import com.bom.entity.Drawing;
import com.bom.entity.Requisition;
import com.bom.entity.RequisitionItem;
import com.bom.exception.BusinessException;
import com.bom.mapper.DrawingMapper;
import com.bom.mapper.RequisitionMapper;
import com.bom.util.PageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class RequisitionService {

    private static final Logger logger = LoggerFactory.getLogger(RequisitionService.class);
    private static final String BASE_STORAGE_PATH = "E:/projectsRun/uploads/";

    @Autowired
    private RequisitionMapper requisitionMapper;

    @Autowired
    private DrawingMapper drawingMapper;

    /**
     * 分页查询全部采购订单
     * @param purchaserName 采购人员名称过滤（null 表示不过滤）
     */
    public PageResult<Requisition> getAllPaged(int page, int size, String purchaserName) {
        if (page < 1) page = 1;
        if (size < 1) size = 20;
        int offset = (page - 1) * size;
        List<Requisition> list = requisitionMapper.findAllPaged(offset, size, purchaserName);
        long total = requisitionMapper.countAll(purchaserName);
        return new PageResult<>(list, total, page, size);
    }

    /**
     * 分页搜索采购订单
     * @param purchaserName 采购人员名称过滤（null 表示不过滤）
     */
    public PageResult<Requisition> searchPaged(String keyword, int page, int size, String purchaserName) {
        if (keyword == null || keyword.isEmpty()) {
            return getAllPaged(page, size, purchaserName);
        }
        if (page < 1) page = 1;
        if (size < 1) size = 20;
        int offset = (page - 1) * size;
        List<Requisition> list = requisitionMapper.searchPaged(keyword, offset, size, purchaserName);
        long total = requisitionMapper.countSearch(keyword, purchaserName);
        return new PageResult<>(list, total, page, size);
    }

    /**
     * 获取采购订单详情（含明细 + 图纸关联信息）
     */
    public Requisition getDetailById(Long id) {
        Requisition requisition = requisitionMapper.findById(id);
        if (requisition == null) {
            throw new BusinessException("采购订单不存在");
        }
        List<RequisitionItem> items = requisitionMapper.findItemsByRequisitionId(id);
        requisition.setItems(items);
        return requisition;
    }

    /**
     * 导出采购订单为 Excel（.xlsx）
     * 返回生成的 Excel 字节数组
     */
    public byte[] exportExcel(Long requisitionId) {
        Requisition requisition = getDetailById(requisitionId);

        org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
        try {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("采购订单");

            // 样式
            org.apache.poi.ss.usermodel.CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            headerStyle.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            headerStyle.setBorderLeft(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            headerStyle.setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            headerStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);

            org.apache.poi.ss.usermodel.CellStyle titleStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);

            org.apache.poi.ss.usermodel.CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            dataStyle.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            dataStyle.setBorderLeft(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            dataStyle.setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            dataStyle.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

            // 安全取值工具
            String reqNo = requisition.getRequisitionNo() != null ? requisition.getRequisitionNo() : "";
            String requester = requisition.getRequester() != null ? requisition.getRequester() : "";
            String department = requisition.getDepartment() != null ? requisition.getDepartment() : "";
            String remark = requisition.getRemark() != null ? requisition.getRemark() : "";
            String dateStr = requisition.getRequisitionDate() != null ? sdf.format(requisition.getRequisitionDate()) : "";

            // 标题行
            org.apache.poi.ss.usermodel.Row titleRow = sheet.createRow(0);
            org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("采购订单  " + reqNo);
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 5));

            // 信息行
            org.apache.poi.ss.usermodel.Row infoRow1 = sheet.createRow(1);
            infoRow1.createCell(0).setCellValue("单据编号:");
            infoRow1.getCell(0).setCellStyle(headerStyle);
            infoRow1.createCell(1).setCellValue(reqNo);
            infoRow1.createCell(2).setCellValue("单据日期:");
            infoRow1.getCell(2).setCellStyle(headerStyle);
            infoRow1.createCell(3).setCellValue(dateStr);
            infoRow1.createCell(4).setCellValue("采购人员:");
            infoRow1.getCell(4).setCellStyle(headerStyle);
            infoRow1.createCell(5).setCellValue(requester);

            org.apache.poi.ss.usermodel.Row infoRow2 = sheet.createRow(2);
            infoRow2.createCell(0).setCellValue("部门:");
            infoRow2.getCell(0).setCellStyle(headerStyle);
            infoRow2.createCell(1).setCellValue(department);
            infoRow2.createCell(2).setCellValue("备注:");
            infoRow2.getCell(2).setCellStyle(headerStyle);
            infoRow2.createCell(3).setCellValue(remark);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(2, 2, 3, 5));

            // 空行
            sheet.createRow(3);

            // 表头
            String[] headers = {"序号", "物料编码", "物料名称", "规格型号", "采购数量", "是否有图纸"};
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(4);
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 数据行
            List<RequisitionItem> items = requisition.getItems();
            if (items != null) {
                for (int i = 0; i < items.size(); i++) {
                    RequisitionItem item = items.get(i);
                    org.apache.poi.ss.usermodel.Row row = sheet.createRow(5 + i);
                    row.createCell(0).setCellValue(i + 1);
                    row.createCell(1).setCellValue(item.getMaterialCode() != null ? item.getMaterialCode() : "");
                    row.createCell(2).setCellValue(item.getMaterialName() != null ? item.getMaterialName() : "");
                    row.createCell(3).setCellValue(item.getSpecification() != null ? item.getSpecification() : "");
                    BigDecimal qty = item.getQuantity();
                    String unit = item.getUnit() != null ? item.getUnit() : "";
                    if (!unit.isEmpty()) {
                        row.createCell(4).setCellValue((qty != null ? qty.toPlainString() : "0") + unit);
                    } else {
                        row.createCell(4).setCellValue(qty != null ? qty.doubleValue() : 0);
                    }
                    row.createCell(5).setCellValue(Boolean.TRUE.equals(item.getHasDrawing()) ? "是" : "否");

                    for (int j = 0; j < 6; j++) {
                        if (row.getCell(j) != null) {
                            row.getCell(j).setCellStyle(dataStyle);
                        }
                    }
                }
            }

            // 列宽
            sheet.setColumnWidth(0, 2000);
            sheet.setColumnWidth(1, 6000);
            sheet.setColumnWidth(2, 8000);
            sheet.setColumnWidth(3, 10000);
            sheet.setColumnWidth(4, 6000);
            sheet.setColumnWidth(5, 4000);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            logger.error("导出Excel失败", e);
            throw new BusinessException("导出Excel失败: " + e.getMessage());
        } finally {
            try { workbook.close(); } catch (IOException ignored) {}
        }
    }

    /**
     * 导出采购订单中物料明细对应的 PDF 图纸，打包为 ZIP
     * 每个物料仅导出最近一次上传的 PDF 图纸
     * 返回 ZIP 文件的字节数组
     */
    public byte[] exportDrawings(Long requisitionId) {
        Requisition requisition = getDetailById(requisitionId);
        List<RequisitionItem> items = requisition.getItems();

        if (items == null || items.isEmpty()) {
            throw new BusinessException("该采购订单没有物料明细");
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            Set<String> addedNames = new HashSet<>();
            int hasDrawingCount = 0;

            for (RequisitionItem item : items) {
                if (!Boolean.TRUE.equals(item.getHasDrawing()) || item.getMaterialId() == null) {
                    continue;
                }

                // 获取该物料的所有图纸（已按 created_at DESC 排序），只取最近一次上传的 PDF
                List<Drawing> drawings = drawingMapper.findByMaterialId(item.getMaterialId());
                if (drawings == null || drawings.isEmpty()) {
                    continue;
                }

                // 找到最新的 PDF 图纸（列表已按创建时间倒序，第一个 PDF 即为最新）
                Drawing latestPdf = null;
                for (Drawing d : drawings) {
                    if (d.getFileFormat() != null && "pdf".equalsIgnoreCase(d.getFileFormat())) {
                        latestPdf = d;
                        break;
                    }
                }
                if (latestPdf == null) {
                    continue;
                }

                String fullPath = BASE_STORAGE_PATH + latestPdf.getStoragePath();
                File file = new File(fullPath);
                if (!file.exists()) {
                    logger.warn("图纸文件不存在: {}", fullPath);
                    continue;
                }

                hasDrawingCount++;

                // 文件名：物料编码_图纸名
                String materialCode = item.getMaterialCode() != null ? item.getMaterialCode() : "unknown";
                String drawingName = latestPdf.getDrawingName() != null ? latestPdf.getDrawingName() : file.getName();
                String zipEntryName = materialCode + "_" + drawingName;

                // 处理重名
                if (addedNames.contains(zipEntryName)) {
                    zipEntryName = materialCode + "_" + hasDrawingCount + "_" + drawingName;
                }
                addedNames.add(zipEntryName);

                zos.putNextEntry(new ZipEntry(zipEntryName));
                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = fis.read(buffer)) != -1) {
                        zos.write(buffer, 0, len);
                    }
                }
                zos.closeEntry();
            }

            if (hasDrawingCount == 0) {
                throw new BusinessException("该采购订单的物料暂无PDF图纸文件");
            }

            zos.finish();
            return baos.toByteArray();
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            logger.error("导出图纸ZIP失败", e);
            throw new BusinessException("导出图纸失败: " + e.getMessage());
        }
    }
}
