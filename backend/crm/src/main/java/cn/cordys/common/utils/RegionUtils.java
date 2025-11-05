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
     * address => code
     *
     * @param name address
     * @return code
     */
    public static String nameToCode(String name) {
        if (StringUtils.isBlank(name)) {
            return StringUtils.EMPTY;
        }

        StringBuilder code = new StringBuilder(StringUtils.EMPTY);
        Queue<String> queue = new LinkedList<>();
        CollectionUtils.addAll(queue, name.split(SPILT_STR));
        List<RegionCode> regionCode = getRegionCodes();

        for (RegionCode country : regionCode) {
            if (!code.isEmpty()) {
                break;
            }
            if (country.getName().equals(queue.peek())) {
                code = new StringBuilder(country.getCode());
                queue.poll();
                if (country.getChildren() != null) {
                    for (RegionCode province : country.getChildren()) {
                        if (province.getName().equals(queue.peek())) {
                            code = new StringBuilder(province.getCode());
                            queue.poll();
                        }

                        if (province.getChildren() != null) {
                            for (RegionCode city : province.getChildren()) {
                                if (city.getName().equals(queue.peek())) {
                                    code = new StringBuilder(city.getCode());
                                    queue.poll();
                                }

                                if (city.getChildren() != null) {
                                    for (RegionCode area : city.getChildren()) {
                                        if (area.getName().equals(queue.peek())) {
                                            code = new StringBuilder(area.getCode());
                                            queue.poll();
                                        }
                                    }
                                }

                            }
                        }
                    }
                }
            }
        }

        // Keep spilt for parse
        code.append(SPILT_STR);
        // Append detailed address
        if (!queue.isEmpty()) {
            for (String s : queue) {
                code.append(s);
            }
        }

        return code.toString();
    }

    /**
     * code => address
     *
     * @param codeStr
     * @return address
     */
    public static String codeToName(String codeStr) {
        String code = getCode(codeStr);
        if (code == null) return null;
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
