package cn.cordys.crm.system.service;

import cn.cordys.crm.system.constants.LicenseStatus;
import cn.cordys.crm.system.dto.VersionInfoDTO;
import cn.cordys.crm.system.utils.ArchUtils;
import cn.cordys.crm.system.utils.CSHttpClient;
import cn.cordys.crm.system.utils.CopyrightUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Year;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
public class SystemService {

    private static final String CACHE_COPYRIGHT = "copyright_cache";
    private static final String CACHE_FORM = "form_cache";
    private static final String CACHE_TABLE_SCHEMA = "table_schema_cache";
    private static final String PRODUCT = "CORDYS";
    private static final String EDITION_CE = "ce";
    private static final String EDITION_EE = "ee";
    private static final String PERMISSION_CACHE = "permission_cache";

    private final CacheManager cacheManager;
    private final LicenseService licenseService;

    public SystemService(CacheManager cacheManager, LicenseService licenseService) {
        this.cacheManager = cacheManager;
        this.licenseService = licenseService;
    }

    private static String baseVersion(String v) {
        if (StringUtils.isBlank(v)) return "";
        String core = StringUtils.substringBefore(v.trim(), "-");
        core = StringUtils.substringBefore(core, "+");
        if (core.startsWith("v") || core.startsWith("V")) {
            core = core.substring(1);
        }
        return core;
    }

    private static boolean isNumericSemver(String v) {
        String core = baseVersion(v);
        if (StringUtils.isBlank(core)) return false;
        String[] parts = StringUtils.split(core, '.');
        if (parts == null || parts.length == 0) return false;
        for (String p : parts) {
            if (!StringUtils.isNumeric(p)) return false;
        }
        return true;
    }

    private static int compareSemverCore(String v1, String v2) {
        String[] a = StringUtils.split(v1, '.');
        String[] b = StringUtils.split(v2, '.');
        int al = a == null ? 0 : a.length;
        int bl = b == null ? 0 : b.length;
        int len = Math.max(al, bl);
        for (int i = 0; i < len; i++) {
            int ai = i < al ? safeParseInt(a[i]) : 0;
            int bi = i < bl ? safeParseInt(b[i]) : 0;
            int cmp = Integer.compare(ai, bi);
            if (cmp != 0) return cmp;
        }
        return 0;
    }

    private static int safeParseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return 0;
        }
    }

    @Cacheable(value = CACHE_COPYRIGHT)
    public VersionInfoDTO getVersion() {
        // 支持系统属性覆盖，环境变量退化，最后为 unknown
        String current = StringUtils.defaultIfBlank(System.getenv("CRM_VERSION"), "unknown");
        String currentBase = baseVersion(current);

        String edition = getEdition();
        CSHttpClient.DownloadInfo info = null;
        try {
            info = CSHttpClient.sendGetRequest(Map.of(
                    "currentVersion", currentBase,
                    "majorVersion", "v1",
                    "edition", edition,
                    "product", PRODUCT
            ));
        } catch (Exception e) {
            log.info("检查版本信息失败: {}", e.getMessage());
        }

        String latest = info != null
                ? StringUtils.defaultIfBlank(info.getLatestVersion(), current)
                : current;
        String latestBase = baseVersion(latest);

        boolean comparable = isNumericSemver(currentBase) && isNumericSemver(latestBase);
        boolean hasNew = comparable && compareSemverCore(latestBase, currentBase) > 0;

        int year = info != null ? info.getYear() : Year.now().getValue();

        return VersionInfoDTO.builder()
                .currentVersion(current)
                .latestVersion(hasNew ? latest : "v" + currentBase)
                .copyright(CopyrightUtils.getCopyright(year))
                .hasNewVersion(hasNew)
                .architecture(ArchUtils.getNormalizedArch())
                .build();
    }

    /**
     * 清理表单相关缓存
     * <p>清理表单缓存和表结构缓存，确保应用使用最新的表单配置和结构</p>
     */
    public void clearFormCache() {
        if (cacheManager == null) {
            log.info("CacheManager 未初始化，跳过清理。");
            return;
        }
        String[] cacheNames = {CACHE_FORM, CACHE_TABLE_SCHEMA, CACHE_COPYRIGHT, PERMISSION_CACHE};
        for (String cacheName : cacheNames) {
            try {
                Cache cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                }
            } catch (Exception e) {
                log.info("清理缓存失败：{} - {}", cacheName, e.getMessage());
            }
        }
    }

    private String getEdition() {
        try {
            String status = Objects.toString(licenseService.validate().getStatus(), "");
            return Strings.CI.equals(status, LicenseStatus.NOT_FOUND.getName()) ? EDITION_CE : EDITION_EE;
        } catch (Exception e) {
            // 许可异常时默认为 CE
            log.info("获取许可状态失败，默认使用 CE：{}", e.getMessage());
            return EDITION_CE;
        }
    }
}