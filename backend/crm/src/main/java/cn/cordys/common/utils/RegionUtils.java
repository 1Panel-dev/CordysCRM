package cn.cordys.common.utils;

import cn.cordys.common.resolver.field.LocationResolver;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.system.dto.regioncode.RegionCode;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * @author song-cc-rock
 */
public class RegionUtils {

    private static final String SPILT_STR = "-";
    private static SoftReference<List<RegionCode>> regionCodeRef;

    /**
     * get all regions
     *
     * @return regions
     */
    public static List<RegionCode> getRegionCodes() {
        List<RegionCode> regions;

        if (regionCodeRef == null || (regions = regionCodeRef.get()) == null) {
            synchronized (LocationResolver.class) {
                if (regionCodeRef == null || (regions = regionCodeRef.get()) == null) {
                    regions = loadRegionData();
                    regionCodeRef = new SoftReference<>(regions);
                }
            }
        }
        return regions;
    }

    /**
     * load region
     *
     * @return region list
     */
    private static List<RegionCode> loadRegionData() {
        try (InputStream is = LocationResolver.class.getClassLoader()
                .getResourceAsStream("region/region.json")) {
            return JSON.parseObject(is, new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new RuntimeException("加载行政区划数据失败", e);
        }
    }

    /**
     * 映射地址与编码
     * @param str 映射字符串
     * @param nameToCode 是否名称转编码
     * @return 映射结果
     */
    public static String mapping(String str, boolean nameToCode) {
        if (StringUtils.isBlank(str)) {
            return StringUtils.EMPTY;
        }

        Queue<String> queue = new LinkedList<>();
        CollectionUtils.addAll(queue, str.split(SPILT_STR));
        List<RegionCode> regionCodes = getRegionCodes();
        StringBuilder result = new StringBuilder();

        RegionCode matched = findMatch(regionCodes, queue.peek(), nameToCode);
        while (matched != null && !queue.isEmpty()) {
            result = new StringBuilder(nameToCode ? matched.getCode() : matched.getName());
            queue.poll();
            matched = matched.getChildren() == null ? null : findMatch(matched.getChildren(), queue.peek(), nameToCode);
        }

        // 保留分隔符, 后续取详细地址
        result.append(SPILT_STR);
        queue.forEach(result::append);

        return result.toString();
    }

    /**
     * find match region code
     */
    private static RegionCode findMatch(List<RegionCode> list, String value, boolean nameToCode) {
        if (StringUtils.isBlank(value) || CollectionUtils.isEmpty(list)) {
            return null;
        }
        for (RegionCode item : list) {
            boolean matched = nameToCode ? Strings.CS.equals(item.getName(), value)
                    : Strings.CS.equals(item.getCode(), value);
            if (matched) {
                return item;
            }
        }
        return null;
    }

    /**
     * code => address
     *
     * @param codeStr 地址编码
     * @return address
     */
    public static String codeToName(String codeStr) {
        String code = getCode(codeStr);
        if (code == null) {
            return null;
        }
        List<RegionCode> regionCodes = getRegionCodes();
        return getRegionFullName(code, regionCodes);
    }

    public static String getCode(String codeStr) {
        if (StringUtils.isBlank(codeStr)) {
            return null;
        }
        String code = codeStr.split(SPILT_STR)[0];
        return StringUtils.isBlank(code) ? null : code;
    }

    private static String getRegionFullName(String code, List<RegionCode> regionCodes) {
        if (StringUtils.isBlank(code)) {
            return null;
        }
        for (RegionCode regionCode : regionCodes) {
            if (Strings.CS.equals(regionCode.getCode(), code)) {
                return regionCode.getName();
            }

            List<RegionCode> children = regionCode.getChildren();
            if (CollectionUtils.isNotEmpty(children)) {
                String regionFullName = getRegionFullName(code, children);
                if (regionFullName != null && !Strings.CS.equals(regionFullName, code)) {
                    return regionCode.getName() + SPILT_STR + regionFullName;
                }
            }
        }
        return code;
    }
}
