package com.bom.service;

import com.bom.entity.Drawing;
import com.bom.entity.Material;
import com.bom.erp.ErpMaterialSyncService;
import com.bom.exception.BusinessException;
import com.bom.mapper.DrawingMapper;
import com.bom.mapper.MaterialMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class DrawingService {

    private static final Logger logger = LoggerFactory.getLogger(DrawingService.class);
    private static final String BASE_STORAGE_PATH = "E:/projectsRun/uploads/";

    /** ZIP解压支持的文件扩展名：PDF图纸、三维模型、工程图 */
    private static final Set<String> SUPPORTED_EXTENSIONS = new HashSet<>(Arrays.asList(
            "pdf", "sldprt", "sldasm", "slddrw"
    ));

    /** ZIP最大解压条目数，防止ZIP炸弹 */
    private static final int MAX_ZIP_ENTRIES = 500;

    @Autowired
    private DrawingMapper drawingMapper;

    @Autowired
    private MaterialMapper materialMapper;

    @Autowired
    private ErpMaterialSyncService erpMaterialSyncService;

    /** ERP 图纸标记异步推送线程池：上传不被 ERP 调用阻塞 */
    private final ExecutorService erpNotifyExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "erp-drawing-flag-notify");
        t.setDaemon(true);
        return t;
    });

    /** 已排队推送 ERP 图纸标记的物料编码（同物料去重，避免批量上传时重复推送） */
    private final java.util.Set<String> pendingErpFlagCodes = ConcurrentHashMap.newKeySet();

    /**
     * 上传文件（支持ZIP压缩包或单个文件）
     * <p>
     * 如果是 .zip 文件，自动解压并保存其中的 .pdf / .sldprt / .sldasm / .slddrw 文件，
     * 每个文件创建一条 Drawing 记录。
     * 如果是单个文件，直接保存。
     *
     * @param file       上传的文件
     * @param bomNodeId  BOM节点ID（保留参数，暂不使用）
     * @param materialId 关联物料ID
     * @param uploadedBy 上传用户ID
     * @return 所有创建的Drawing记录列表
     */
    @Transactional
    public List<Drawing> upload(MultipartFile file, Long bomNodeId, Long materialId, Long uploadedBy) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new BusinessException("无法获取文件名");
        }

        String lowerName = originalFilename.toLowerCase();
        if (lowerName.endsWith(".zip")) {
            List<Drawing> drawings = uploadZip(file, materialId, uploadedBy);
            return drawings;
        } else {
            Drawing drawing = uploadSingleFile(file, materialId, uploadedBy);
            List<Drawing> result = new ArrayList<>();
            result.add(drawing);
            return result;
        }
    }

    /**
     * 上传ZIP压缩包，解压并保存其中的图纸文件
     */
    private List<Drawing> uploadZip(MultipartFile file, Long materialId, Long uploadedBy) {
        List<Drawing> result = new ArrayList<>();

        // 生成日期目录
        String dateDir = new SimpleDateFormat("yyyy/MM/dd").format(new Date());
        String storageDir = BASE_STORAGE_PATH + dateDir;
        File dir = new File(storageDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // ZIP文件名编码：Windows下创建的ZIP通常使用GBK编码，需指定字符集避免中文乱码
        try (ZipInputStream zis = new ZipInputStream(file.getInputStream(), Charset.forName("GBK"))) {
            ZipEntry entry;
            int entryCount = 0;

            while ((entry = zis.getNextEntry()) != null) {
                if (entryCount++ > MAX_ZIP_ENTRIES) {
                    logger.warn("ZIP文件条目超过上限{}，停止解压", MAX_ZIP_ENTRIES);
                    break;
                }

                // 跳过目录
                if (entry.isDirectory()) {
                    continue;
                }

                // 获取文件名（去除路径前缀）
                String entryName = entry.getName();
                String fileName = entryName;
                int lastSlash = entryName.replace('\\', '/').lastIndexOf('/');
                if (lastSlash >= 0) {
                    fileName = entryName.substring(lastSlash + 1);
                }

                // 检查扩展名
                String lowerFileName = fileName.toLowerCase();
                String extension = "";
                if (lowerFileName.contains(".")) {
                    extension = lowerFileName.substring(lowerFileName.lastIndexOf(".") + 1);
                }

                if (!SUPPORTED_EXTENSIONS.contains(extension)) {
                    logger.debug("跳过不支持的文件: {}", fileName);
                    continue;
                }

                // 保存文件到磁盘
                String savedFilename = UUID.randomUUID().toString().replace("-", "") + "." + extension;
                String storagePath = dateDir + "/" + savedFilename;
                String fullPath = BASE_STORAGE_PATH + storagePath;

                File outFile = new File(fullPath);
                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                }

                // 创建Drawing记录
                Drawing drawing = new Drawing();
                drawing.setDrawingCode("DRW-" + System.currentTimeMillis() + "-" + result.size());
                drawing.setDrawingName(fileName);
                drawing.setFileFormat(extension);
                drawing.setFileSize(outFile.length());
                drawing.setStoragePath(storagePath);
                drawing.setPreviewPath(null);
                drawing.setPreviewStatus("NONE");
                drawing.setLatestVersion("V1.0");
                drawing.setMaterialId(materialId);
                drawing.setCreatedBy(uploadedBy);
                drawing.setCreatedAt(new Date());
                drawingMapper.insert(drawing);

                result.add(drawing);
                logger.info("ZIP解压上传成功: fileName={}, format={}", fileName, extension);

                zis.closeEntry();
            }
        } catch (IOException e) {
            logger.error("ZIP解压失败", e);
            throw new BusinessException("ZIP解压失败: " + e.getMessage());
        }

        if (result.isEmpty()) {
            throw new BusinessException("压缩包内未找到支持的文件（需包含 .pdf / .sldprt / .sldasm / .slddrw 文件）");
        }

        // ZIP 解压上传成功后，同步 ERP 图纸标记
        notifyErpHaveDrawing(materialId, true);

        logger.info("ZIP上传完成，共解压 {} 个图纸文件，materialId={}", result.size(), materialId);
        return result;
    }

    /**
     * 上传单个文件
     */
    private Drawing uploadSingleFile(MultipartFile file, Long materialId, Long uploadedBy) {
        try {
            String dateDir = new SimpleDateFormat("yyyy/MM/dd").format(new Date());
            String storageDir = BASE_STORAGE_PATH + dateDir;
            File dir = new File(storageDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String savedFilename = UUID.randomUUID().toString().replace("-", "") + extension;
            String storagePath = dateDir + "/" + savedFilename;
            String fullPath = BASE_STORAGE_PATH + storagePath;

            file.transferTo(new File(fullPath));

            String drawingCode = "DRW-" + System.currentTimeMillis();
            Drawing drawing = new Drawing();
            drawing.setDrawingCode(drawingCode);
            drawing.setDrawingName(originalFilename);
            drawing.setFileFormat(extension.replace(".", "").toLowerCase());
            drawing.setFileSize(file.getSize());
            drawing.setStoragePath(storagePath);
            drawing.setPreviewPath(null);
            drawing.setPreviewStatus("NONE");
            drawing.setLatestVersion("V1.0");
            drawing.setMaterialId(materialId);
            drawing.setCreatedBy(uploadedBy);
            drawing.setCreatedAt(new Date());
            drawingMapper.insert(drawing);

            // 上传成功后，同步 ERP 图纸标记（CU_HaveDrawing=1）
            notifyErpHaveDrawing(materialId, true);

            logger.info("图纸上传成功: drawingCode={}, fileName={}", drawingCode, originalFilename);
            return drawing;
        } catch (IOException e) {
            logger.error("文件上传失败", e);
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 批量上传多个图纸文件，根据文件名中的图号自动匹配物料。
     * <p>
     * 文件命名规范："ED2.1-1325-10001-25 框架床身(内置机箱）25轨.pdf"
     * 第一个空格前的部分为图号，用于匹配物料的 drawing_no 或 specification 字段。
     *
     * @param files      多个上传文件
     * @param uploadedBy 上传用户ID
     * @return 匹配结果统计（total/matched/unmatched/skipped/details/drawings）
     */
    public Map<String, Object> batchUpload(MultipartFile[] files, Long uploadedBy) {
        if (files == null || files.length == 0) {
            throw new BusinessException("上传文件不能为空");
        }

        List<Map<String, Object>> details = new ArrayList<>();
        List<Drawing> uploadedDrawings = new ArrayList<>();
        int matched = 0;
        int unmatched = 0;
        int skipped = 0;

        // 缓存：图号 → 物料，避免重复查询
        Map<String, Material> materialCache = new HashMap<>();

        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];
            String fileName = file.getOriginalFilename();
            if (fileName == null) {
                fileName = "unknown_" + i;
            }

            // 检查扩展名
            String extension = getExtension(fileName);
            if (!SUPPORTED_EXTENSIONS.contains(extension)) {
                skipped++;
                logger.warn("批量上传：跳过不支持的文件格式: {}", fileName);
                continue;
            }

            // 提取图号（文件名第一个空格前的部分，去掉扩展名）
            String drawingNo = extractDrawingNo(fileName);

            // 查找匹配物料
            Material material = null;
            if (drawingNo != null && !drawingNo.isEmpty()) {
                material = materialCache.get(drawingNo);
                if (material == null && !materialCache.containsKey(drawingNo)) {
                    material = materialMapper.findByDrawingNo(drawingNo);
                    materialCache.put(drawingNo, material);
                }
            }

            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("fileName", fileName);
            detail.put("drawingNo", drawingNo);
            detail.put("format", extension);

            if (material != null) {
                matched++;
                detail.put("matched", true);
                detail.put("materialId", material.getId());
                detail.put("materialName", material.getMaterialName());
                detail.put("materialCode", material.getMaterialCode());

                try {
                    Drawing drawing = uploadSingleFile(file, material.getId(), uploadedBy);
                    uploadedDrawings.add(drawing);
                    detail.put("drawingId", drawing.getId());
                } catch (Exception e) {
                    logger.error("批量上传：文件保存失败: {}", fileName, e);
                    detail.put("matched", false);
                    detail.put("error", e.getMessage());
                    unmatched++;
                    matched--;
                }
            } else {
                unmatched++;
                detail.put("matched", false);
                detail.put("error", "未找到图号匹配的物料");
            }

            details.add(detail);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", files.length);
        result.put("matched", matched);
        result.put("unmatched", unmatched);
        result.put("skipped", skipped);
        result.put("details", details);
        result.put("drawings", uploadedDrawings);

        logger.info("批量上传完成：共{}个文件，匹配{}个，未匹配{}个，跳过{}个",
                files.length, matched, unmatched, skipped);
        return result;
    }

    /**
     * 上传图纸后通知 ERP：该物料存在图纸（CU_HaveDrawing=1）。
     * 同步失败不阻断本地上传，仅记录日志并收集警告信息。
     */
    private void notifyErpHaveDrawing(Long materialId, boolean haveDrawing) {
        if (materialId == null) {
            return;
        }
        erpNotifyExecutor.submit(() -> {
            try {
                Material material = materialMapper.findById(materialId);
                if (material == null || material.getMaterialCode() == null || material.getMaterialCode().isEmpty()) {
                    logger.warn("上传成功但物料不存在或缺少物料编码，跳过 ERP 图纸标记同步: materialId={}", materialId);
                    return;
                }
                String code = material.getMaterialCode();
                if (!pendingErpFlagCodes.add(code)) {
                    logger.debug("ERP 图纸标记推送已排队，跳过重复物料: {}", code);
                    return;
                }
                try {
                    erpMaterialSyncService.syncHaveDrawingFlag(code, haveDrawing);
                } finally {
                    pendingErpFlagCodes.remove(code);
                }
            } catch (Exception e) {
                logger.error("ERP图纸标记同步失败 materialId={}", materialId, e);
            }
        });
    }

    /**
     * 从文件名中提取图号。
     * 规则：去掉扩展名后，取第一个空格前的部分。
     * 例如："ED2.1-1325-10001-25 框架床身(内置机箱）25轨.pdf" → "ED2.1-1325-10001-25"
     */
    private String extractDrawingNo(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }
        // 去掉扩展名
        String nameWithoutExt = fileName;
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            nameWithoutExt = fileName.substring(0, dotIndex);
        }
        // 取第一个空格前的部分
        int spaceIndex = nameWithoutExt.indexOf(' ');
        if (spaceIndex > 0) {
            return nameWithoutExt.substring(0, spaceIndex).trim();
        }
        // 没有空格则用整个文件名（去掉扩展名）
        return nameWithoutExt.trim();
    }

    /**
     * 获取文件扩展名（小写，不含点）
     */
    private String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    public String download(Long drawingId) {
        Drawing drawing = drawingMapper.findById(drawingId);
        if (drawing == null) {
            throw new BusinessException("图纸不存在");
        }
        return BASE_STORAGE_PATH + drawing.getStoragePath();
    }

    public List<Drawing> getByBomNodeId(Long bomNodeId) {
        return drawingMapper.findByBomNodeId(bomNodeId);
    }

    public List<Drawing> getByMaterialId(Long materialId) {
        return drawingMapper.findByMaterialId(materialId);
    }

    public List<Drawing> search(String keyword) {
        return drawingMapper.search(keyword);
    }

    public Drawing getById(Long id) {
        return drawingMapper.findById(id);
    }
}
