package cn.cordys.common.resolver.field;

import cn.cordys.common.util.JSON;
import cn.cordys.common.utils.RegionUtils;
import cn.cordys.crm.system.dto.field.LocationField;
import cn.cordys.crm.system.dto.regioncode.RegionCode;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.List;

public class LocationResolver extends AbstractModuleFieldResolver<LocationField> {

    public static final String PCD = "PCD";
    public static final String PC = "PC";
    public static final String P = "P";
    public static final String C = "C";
    private static final String SPILT_STR = "-";
    private static SoftReference<List<RegionCode>> regionCache;

    public static List<RegionCode> getRegionCodes() {
        var regions = regionCache.get();
        if (regions != null) return regions;

        synchronized (LocationResolver.class) {
            regions = regionCache.get();
            if (regions == null) {
                regions = loadRegionData();
                regionCache = new SoftReference<>(regions);
            }
            return regions;
        }
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

        String regionName = StringUtils.EMPTY;
        List<RegionCode> regionCode = getRegionCodes();

        for (RegionCode country : regionCode) {
            // 检查国家编码
            if (country.getCode().equals(code)) {
                regionName = country.getName();
                break;
            }
            // 检查省级编码
            if (country.getChildren() != null) {
                for (RegionCode province : country.getChildren()) {
                    if (province.getCode().equals(code)) {
                        regionName = country.getName() + "-" + province.getName();
                        break;
                    }
                    // 检查市级编码
                    if (province.getChildren() != null) {
                        for (RegionCode city : province.getChildren()) {
                            if (city.getCode().equals(code)) {
                                regionName = country.getName() + "-" + province.getName() + "-" + city.getName();
                                break;
                            }
                            // 检查区级编码
                            if (city.getChildren() != null) {
                                for (RegionCode area : city.getChildren()) {
                                    if (area.getCode().equals(code)) {
                                        regionName = country.getName() + "-" + province.getName() + "-" + city.getName() + "-" + area.getName();
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }


        if (StringUtils.isBlank(detail)) {
            return regionName;
        }
        return regionName + SPILT_STR + detail;
    }

    @Override
    public Object text2Value(LocationField field, String text) {
        return RegionUtils.nameToCode(text);
    }
}
