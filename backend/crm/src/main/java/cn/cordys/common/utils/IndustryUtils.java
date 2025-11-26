package cn.cordys.common.utils;

import cn.cordys.common.resolver.field.IndustryResolver;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.system.dto.form.IndustryDict;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.*;

/**
 * @author song-cc-rock
 */
public class IndustryUtils {

    /**
     * split str
     */
    private static final String SPILT_STR = "-";
    private static SoftReference<List<IndustryDict>> industryRef;

    /**
     * 获取所有行业分类
     *
     * @return categories 行业分类
     */
    public static List<IndustryDict> getIndustries() {
        List<IndustryDict> industries;
        if (industryRef == null || (industries = industryRef.get()) == null) {
            synchronized (IndustryResolver.class) {
                if (industryRef == null || (industries = industryRef.get()) == null) {
                    industries = loadData();
                    industryRef = new SoftReference<>(industries);
                }
            }
        }
        return industries;
    }

    /**
     * 加载行业分类字典数据
     *
     * @return 行业分类树
     */
    private static List<IndustryDict> loadData() {
        try (InputStream is = IndustryResolver.class.getClassLoader()
                .getResourceAsStream("dict/industry.json")) {
            return JSON.parseObject(is, new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new RuntimeException("加载行业分类失败", e);
        }
    }

    /**
     * 行业分类映射
     *
     * @param str 解析字符串
     * @param nameToCode 是否名称转编码
     * @return 编码
     */
    public static String mapping(String str, boolean nameToCode) {
        if (StringUtils.isBlank(str)) {
            return StringUtils.EMPTY;
        }

        List<String> path = new ArrayList<>();
        Queue<String> findQueue = new LinkedList<>();
        CollectionUtils.addAll(findQueue, str.split(SPILT_STR));
        List<IndustryDict> industries = getIndustries();
        boolean found = findRecursive(industries, findQueue, path, nameToCode);
        return found ? String.join(SPILT_STR, path) : StringUtils.EMPTY;
    }

    /**
     * 递归查找行业分类路径
     * @param industryTree 行业分类树
     * @param findQueue 查找队列
     * @param path 路径
     * @return 是否找到
     */
    private static boolean findRecursive(List<IndustryDict> industryTree, Queue<String> findQueue, List<String> path, boolean nameToCode) {
        if (CollectionUtils.isEmpty(industryTree) || findQueue.isEmpty()) {
            return false;
        }
        String current = findQueue.peek();
        for (IndustryDict industry : industryTree) {
            boolean match = nameToCode ? Strings.CS.equals(industry.getLabel(), current)
                    : Strings.CS.equals(industry.getValue(), current);
            if (match) {
                // 层级匹配, 记录编码, 弹出当前层级
                path.add(nameToCode ? industry.getValue() : industry.getLabel());
                findQueue.poll();
                // 队列为空, 直接返回成功
                if (findQueue.isEmpty()) {
                    return true;
                }
                // 递归查找下一级
                return findRecursive(industry.getChildren(), findQueue, path, nameToCode);
            }
        }
        return false;
    }


    public static List<String> getValues(List<IndustryDict> tree, List<String> targetValues) {
        List<String> result = new ArrayList<>();

        for (String target : targetValues) {
            List<String> values = getValuesForSingle(tree, target);
            if (values != null) {
                result.addAll(values);
            }
        }
        return result;
    }

    /**
     * 查找单个 value，并返回其所有子节点 value
     */
    private static List<String> getValuesForSingle(List<IndustryDict> tree, String targetValue) {
        for (IndustryDict node : tree) {
            if (node.getValue().equals(targetValue)) {
                List<String> result = new ArrayList<>();
                collectValues(node, result);
                return result;
            }

            if (node.getChildren() != null) {
                List<String> res = getValuesForSingle(node.getChildren(), targetValue);
                if (res != null) return res;
            }
        }
        return null;
    }

    /**
     * 收集节点自身和所有子节点的 value
     */
    private static void collectValues(IndustryDict node, List<String> result) {
        result.add(node.getValue());

        if (node.getChildren() != null) {
            for (IndustryDict child : node.getChildren()) {
                collectValues(child, result);
            }
        }
    }
}
