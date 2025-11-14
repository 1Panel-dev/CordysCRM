package cn.cordys.common.resolver.field;

import cn.cordys.common.util.JSON;
import cn.cordys.common.utils.RegionUtils;
import cn.cordys.crm.system.dto.field.LocationField;
import cn.cordys.crm.system.dto.regioncode.RegionCode;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class LocationResolver extends AbstractModuleFieldResolver<LocationField> {

    public static final String PCD = "PCD";
    public static final String PC = "PC";
    public static final String P = "P";
    public static final String C = "C";
    private static final String SPILT_STR = "-";
    private static SoftReference<List<RegionCode>> regionCodeRef;

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


    private static List<RegionCode> loadRegionData() {
        try (InputStream is = LocationResolver.class.getClassLoader()
                .getResourceAsStream("region/region.json")) {
            return JSON.parseObject(is, new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new RuntimeException("加载行政区划数据失败", e);
        }
    }

    @Override
    public void validate(LocationField customField, Object value) {

    }


    @Override
    public Object trans2Value(LocationField locationField, String value) {
        if (StringUtils.isBlank(value)) {
            return StringUtils.EMPTY;
        }

        if (!value.contains(SPILT_STR)) {
            return StringUtils.EMPTY;
        }

        //编码
        String code = value.substring(0, value.indexOf(SPILT_STR));
        //描述
        String detail = value.substring(value.indexOf(SPILT_STR) + 1);
        //解析名称
        String regionName = resolveRegionName(code);

        if (StringUtils.isBlank(detail)) {
            return regionName;
        }
        return regionName + SPILT_STR + detail;
    }

    private String resolveRegionName(String code) {
        List<RegionCode> regionCode = getRegionCodes();
        return resolveRegionName(regionCode, code, new ArrayDeque<>());
    }

    private String resolveRegionName(List<RegionCode> nodes, String code, Deque<String> path) {
        if (nodes == null || nodes.isEmpty()) {
            return StringUtils.EMPTY;
        }
        for (RegionCode node : nodes) {
            path.addLast(node.getName());
            if (code.equals(node.getCode())) {
                return String.join(SPILT_STR, path);
            }
            String childResult = resolveRegionName(node.getChildren(), code, path);
            if (StringUtils.isNotBlank(childResult)) {
                return childResult;
            }
            path.removeLast();
        }
        return StringUtils.EMPTY;
    }

    @Override
    public Object text2Value(LocationField field, String text) {
        return RegionUtils.nameToCode(text);
    }
}
