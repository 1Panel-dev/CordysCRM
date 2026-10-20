package cn.cordys.crm.system.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.system.domain.Attachment;
import cn.cordys.file.engine.DefaultRepositoryDir;
import cn.cordys.file.engine.FileRequest;
import cn.cordys.file.engine.StorageType;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jodconverter.core.DocumentConverter;
import org.jodconverter.core.document.DefaultDocumentFormatRegistry;
import org.jodconverter.core.document.DocumentFamily;
import org.jodconverter.core.document.DocumentFormat;
import org.jodconverter.core.office.OfficeManager;
import org.jodconverter.local.LocalConverter;
import org.jodconverter.local.office.LocalOfficeManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * 文件预览服务：将附件统一转换为 PDF 后返回前端预览。
 *
 */
@Service
@Slf4j
public class FilePreviewService {

    /**
     * 需要按文本处理(探测编码 + UTF-8 重编码)的类型
     */
    private static final Set<String> TEXT_TYPES = Set.of("txt", "md", "markdown", "csv");

    /**
     * 本身即 PDF，无需转换
     */
    private static final Set<String> PASSTHROUGH_TYPES = Set.of("pdf");

    /**
     * 交由 LibreOffice(按扩展名识别过滤器的文档/图片类型)
     */
    private static final Set<String> CONVERT_TYPES = Set.of(
            "doc", "docx", "odt", "ott", "rtf", "dotx",
            "xls", "xlsx", "xlsm", "ods", "ots",
            "ppt", "pptx", "odp", "otp",
            "png", "jpg", "jpeg", "gif", "bmp", "tif", "tiff");

    /**
     * 支持预览的全部类型
     */
    private static final Set<String> SUPPORTED_TYPES = Stream.of(TEXT_TYPES, PASSTHROUGH_TYPES, CONVERT_TYPES)
            .flatMap(Set::stream)
            .collect(Collectors.toUnmodifiableSet());

    /**
     * CSV 导入过滤器的字符集代码：76 = UTF-8
     */
    private static final String CSV_UTF8_CHARSET_CODE = "76";

    /**
     * 文本过滤器(强制 UTF-8，避免弹出编码选择框/按系统 locale 解析)
     */
    private static final String TEXT_FILTER_NAME = "Text (encoded)";

    private static final String TEXT_FILTER_OPTIONS = "UTF8";

    @Resource
    private BaseMapper<Attachment> attachmentMapper;

    @Resource
    private FileCommonService fileCommonService;

    @Value("${cordys.convert.enabled:false}")
    private boolean enabled;

    @Value("${cordys.convert.office-home:}")
    private String officeHome;

    @Value("${cordys.convert.start-port:2002}")
    private int startPort;

    @Value("${cordys.convert.process-count:2}")
    private int processCount;

    @Value("${cordys.convert.process-timeout-ms:120000}")
    private long processTimeoutMs;

    @Value("${cordys.convert.max-tasks-per-process:200}")
    private int maxTasksPerProcess;

    @Value("${cordys.convert.max-file-size:104857600}")
    private long maxFileSize;

    private OfficeManager officeManager;

    private DocumentConverter converter;

    @PostConstruct
    public void init() {
        if (!enabled) {
            log.warn("文件预览转换服务未启用(cordys.convert.enabled=false)");
            return;
        }
        try {
            int count = Math.max(1, processCount);
            int[] ports = IntStream.range(startPort, startPort + count).toArray();
            LocalOfficeManager.Builder builder = LocalOfficeManager.builder()
                    .portNumbers(ports)
                    .maxTasksPerProcess(maxTasksPerProcess)
                    .processTimeout(processTimeoutMs);
            if (StringUtils.isNotBlank(officeHome)) {
                builder.officeHome(officeHome);
            }
            OfficeManager manager = builder.build();
            manager.start();
            this.officeManager = manager;
            this.converter = LocalConverter.make(manager);
            log.info("文件预览转换服务已启动, LibreOffice 端口: {}", Arrays.toString(ports));
        } catch (Exception e) {
            log.error("LibreOffice 启动失败, 文件预览转换不可用", e);
        }
    }

    @PreDestroy
    public void destroy() {
        if (officeManager != null) {
            try {
                officeManager.stop();
            } catch (Exception e) {
                log.warn("停止 LibreOffice 失败", e);
            }
        }
    }

    /**
     * 将附件转换为 PDF 返回，供前端预览
     *
     * @param attachmentId 附件ID
     *
     * @return PDF 资源(inline)
     */
    public ResponseEntity<org.springframework.core.io.Resource> preview(String attachmentId) {
        SourceFile source = resolveSource(attachmentId);
        try {
            String ext = extensionOf(source.name());
            if (!SUPPORTED_TYPES.contains(ext)) {
                throw new GenericException(Translator.get("convert.unsupported.type", "不支持预览的文件类型: " + ext));
            }
            byte[] pdfBytes;
            if (PASSTHROUGH_TYPES.contains(ext)) {
                pdfBytes = FileUtils.readFileToByteArray(source.file());
            } else {
                requireConverter();
                File target = convertToPdf(source.file(), ext);
                try {
                    pdfBytes = FileUtils.readFileToByteArray(target);
                } finally {
                    FileUtils.deleteQuietly(target);
                }
            }
            String pdfName = baseName(source.name()) + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodeName(pdfName))
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(pdfBytes.length)
                    .body(new ByteArrayResource(pdfBytes));
        } catch (GenericException e) {
            throw e;
        } catch (Exception e) {
            log.error("文件预览转换失败, attachmentId: {}", attachmentId, e);
            throw new GenericException(Translator.get("convert.failed", "文件预览转换失败"));
        } finally {
            if (source.temporary()) {
                FileUtils.deleteQuietly(source.file());
            }
        }
    }

    /**
     * 定位附件对应的本地文件
     *
     * @param attachmentId 附件ID
     *
     * @return 源文件信息
     */
    private SourceFile resolveSource(String attachmentId) {
        Attachment attachment = attachmentMapper.selectByPrimaryKey(attachmentId);
        if (attachment == null) {
            // 临时附件(尚未转存): 取临时目录下的文件
            FileRequest request = new FileRequest(DefaultRepositoryDir.getTempFileDir(attachmentId), StorageType.LOCAL.name(), null);
            List<File> files = fileCommonService.getFolderFiles(request);
            if (CollectionUtils.isEmpty(files)) {
                throw new GenericException(Translator.get("convert.file.not.exist", "文件不存在或已被删除"));
            }
            File file = files.getFirst();
            checkSize(file.length());
            return new SourceFile(file, file.getName(), false);
        }
        if (attachment.getSize() != null) {
            checkSize(attachment.getSize());
        }
        // 正式附件: 通过文件引擎读取为本地临时文件(兼容 S3 等非本地存储)
        String storage = StringUtils.isNotBlank(attachment.getStorage()) ? attachment.getStorage() : StorageType.LOCAL.name();
        String folder = DefaultRepositoryDir.getTransferFileDir(attachment.getOrganizationId(), attachment.getResourceId(), attachment.getId());
        FileRequest request = new FileRequest(folder, storage, attachment.getName());
        try (InputStream in = fileCommonService.getFileInputStream(request)) {
            if (in == null) {
                throw new GenericException(Translator.get("convert.file.not.exist", "文件不存在或已被删除"));
            }
            String ext = extensionOf(attachment.getName());
            String suffix = ext.isEmpty() ? ".tmp" : "." + ext;
            File temp = File.createTempFile("preview-src-", suffix);
            FileUtils.copyInputStreamToFile(in, temp);
            return new SourceFile(temp, attachment.getName(), true);
        } catch (GenericException e) {
            throw e;
        } catch (Exception e) {
            log.error("读取附件文件失败, attachmentId: {}", attachmentId, e);
            throw new GenericException(Translator.get("convert.file.not.exist", "文件不存在或已被删除"));
        }
    }

    /**
     * 转换为 PDF
     *
     * @param source 源文件
     * @param ext    扩展名
     *
     * @return 转换后的 PDF 临时文件(调用方负责删除)
     */
    private File convertToPdf(File source, String ext) throws Exception {
        File input = source;
        File textTemp = null;
        try {
            DocumentFormat sourceFormat = null;
            if (TEXT_TYPES.contains(ext)) {
                textTemp = toUtf8File(source, ext);
                input = textTemp;
                sourceFormat = textSourceFormat(ext, textTemp);
            }
            File target = File.createTempFile("preview-pdf-", ".pdf");
            try {
                if (sourceFormat == null) {
                    converter.convert(input).to(target).as(DefaultDocumentFormatRegistry.PDF).execute();
                } else {
                    converter.convert(input).as(sourceFormat).to(target).as(DefaultDocumentFormatRegistry.PDF).execute();
                }
            } catch (Exception e) {
                FileUtils.deleteQuietly(target);
                throw e;
            }
            return target;
        } finally {
            FileUtils.deleteQuietly(textTemp);
        }
    }

    /**
     * 按类型构造源文件格式(决定 LibreOffice 的导入过滤器)
     *
     * @param ext      扩展名
     * @param utf8File UTF-8 文本文件
     *
     * @return 文档格式
     */
    private DocumentFormat textSourceFormat(String ext, File utf8File) {
        if ("csv".equals(ext)) {
            String options = detectDelimiter(utf8File) + ",34," + CSV_UTF8_CHARSET_CODE + ",1";
            return DocumentFormat.builder()
                    .name("CSV")
                    .extension("csv")
                    .mediaType("text/csv")
                    .inputFamily(DocumentFamily.SPREADSHEET)
                    .loadFilterName("Text - txt - csv (StarCalc)")
                    .loadFilterOptions(options)
                    .build();
        }
        return DocumentFormat.builder()
                .name("Plain Text")
                .extension(ext)
                .mediaType("text/plain")
                .inputFamily(DocumentFamily.TEXT)
                .loadFilterName(TEXT_FILTER_NAME)
                .loadFilterOptions(TEXT_FILTER_OPTIONS)
                .build();
    }

    /**
     * 将文本文件按探测到的编码解码后重写为 UTF-8
     *
     * @param source 源文件
     * @param ext    扩展名
     *
     * @return UTF-8 临时文件(调用方负责删除)
     */
    private File toUtf8File(File source, String ext) throws Exception {
        byte[] bytes = Files.readAllBytes(source.toPath());
        Charset charset = detectCharset(bytes);
        String content = new String(bytes, charset);
        if (!content.isEmpty() && content.charAt(0) == 0xFEFF) {
            content = content.substring(1);
        }
        File temp = File.createTempFile("preview-txt-", "." + ext);
        Files.writeString(temp.toPath(), content, StandardCharsets.UTF_8);
        return temp;
    }

    /**
     * 探测文本编码: BOM 优先，其次严格 UTF-8，最后中文常见 GB18030 兜底
     *
     * @param bytes 文件字节
     *
     * @return 编码
     */
    private Charset detectCharset(byte[] bytes) {
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF) {
            return StandardCharsets.UTF_8;
        }
        if (bytes.length >= 2 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xFE) {
            return StandardCharsets.UTF_16LE;
        }
        if (bytes.length >= 2 && (bytes[0] & 0xFF) == 0xFE && (bytes[1] & 0xFF) == 0xFF) {
            return StandardCharsets.UTF_16BE;
        }
        if (isValidUtf8(bytes)) {
            return StandardCharsets.UTF_8;
        }
        if (Charset.isSupported("GB18030")) {
            return Charset.forName("GB18030");
        }
        return StandardCharsets.ISO_8859_1;
    }

    /**
     * 严格校验是否为合法 UTF-8
     */
    private boolean isValidUtf8(byte[] bytes) {
        try {
            StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes));
            return true;
        } catch (CharacterCodingException e) {
            return false;
        }
    }

    /**
     * 探测 CSV 分隔符，返回 ASCII 码(9=Tab, 44=逗号, 59=分号)
     *
     * @param utf8File UTF-8 文本文件
     *
     * @return 分隔符 ASCII 码
     */
    private int detectDelimiter(File utf8File) {
        try {
            List<String> lines = Files.readAllLines(utf8File.toPath(), StandardCharsets.UTF_8);
            int comma = 0;
            int semicolon = 0;
            int tab = 0;
            int scanned = 0;
            for (String line : lines) {
                if (line.isEmpty()) {
                    continue;
                }
                comma += countOutsideQuotes(line, ',');
                semicolon += countOutsideQuotes(line, ';');
                tab += countOutsideQuotes(line, '\t');
                if (++scanned >= 10) {
                    break;
                }
            }
            if (tab > comma && tab > semicolon) {
                return 9;
            }
            if (semicolon > comma) {
                return 59;
            }
            return 44;
        } catch (Exception e) {
            return 44;
        }
    }

    private int countOutsideQuotes(String line, char target) {
        boolean inQuotes = false;
        int count = 0;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                inQuotes = !inQuotes;
            } else if (ch == target && !inQuotes) {
                count++;
            }
        }
        return count;
    }

    private void requireConverter() {
        if (converter == null) {
            throw new GenericException(Translator.get("convert.not.available", "文档转换服务未启用或启动失败"));
        }
    }

    private void checkSize(long size) {
        if (size > maxFileSize) {
            throw new GenericException(Translator.get("convert.file.too.large", "文件过大, 无法预览"));
        }
    }

    private String extensionOf(String fileName) {
        int index = fileName.lastIndexOf('.');
        return index >= 0 && index < fileName.length() - 1 ? fileName.substring(index + 1).toLowerCase() : "";
    }

    private String baseName(String fileName) {
        int index = fileName.lastIndexOf('.');
        return index > 0 ? fileName.substring(0, index) : fileName;
    }

    private String encodeName(String fileName) {
        return URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private record SourceFile(File file, String name, boolean temporary) {
    }
}
