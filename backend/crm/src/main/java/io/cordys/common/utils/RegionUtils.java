package io.cordys.common.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import io.cordys.common.resolver.field.LocationResolver;
import io.cordys.common.util.JSON;
import io.cordys.crm.system.dto.regioncode.RegionCode;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class RegionUtils {

    private static final String SPILT_STR = "-";
    private static SoftReference<List<RegionCode>> regionCodeRef;

    public static List<RegionCode> getRegionCodes() {
        List<RegionCode> regions = null;

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

    private static List<RegionCode> loadRegionData() {
        try (InputStream is = LocationResolver.class.getClassLoader()
                .getResourceAsStream("region/region.json")) {
            return JSON.parseObject(is, new TypeReference<List<RegionCode>>() {
            });
        } catch (Exception e) {
            throw new RuntimeException("加载行政区划数据失败", e);
        }
    }

    public static String nameToCode(String name) {
        if (StringUtils.isBlank(name)) {
            return StringUtils.EMPTY;
        }

        if (!name.contains(SPILT_STR)) {
            return StringUtils.EMPTY;
        }

        String code = StringUtils.EMPTY;
        Queue<String> queue = new LinkedList<>();
        CollectionUtils.addAll(queue, name.split(SPILT_STR));
        List<RegionCode> regionCode = getRegionCodes();

        for (RegionCode country : regionCode) {
            if (!code.isEmpty()) {
                break;
            }
            if (country.getName().equals(queue.peek())) {
                code = country.getCode();
                queue.poll();
                if (country.getChildren() != null) {
                    for (RegionCode province : country.getChildren()) {
                        if (province.getName().equals(queue.peek())) {
                            if (province.getName().equals(queue.peek())) {
                                code = province.getCode();
                                queue.poll();
                            }
                        }

                        if (province.getChildren() != null) {
                            for (RegionCode city : province.getChildren()) {
                                if (city.getName().equals(queue.peek())) {
                                    code = city.getCode();
                                    queue.poll();
                                }

                                if (city.getChildren() != null) {
                                    for (RegionCode area : city.getChildren()) {
                                        if (area.getName().equals(queue.peek())) {
                                            code = area.getCode();
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

        if (!queue.isEmpty()) {
            for (String s : queue) {
                code += SPILT_STR + s;
            }
        }

        return code;
    }
}
