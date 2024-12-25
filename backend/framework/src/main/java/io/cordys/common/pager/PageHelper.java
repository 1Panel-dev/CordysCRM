package io.cordys.common.pager;

import com.github.pagehelper.Page;

/**
 * 分页工具类，提供分页信息设置功能。
 * <p>
 * 该类用于将 PageHelper 分页对象转换为自定义的分页对象 {@link Pager}。
 * </p>
 */
public class PageHelper {

    /**
     * 设置分页信息并返回自定义的分页对象。
     * <p>
     * 此方法将 PageHelper 的分页数据（如当前页、每页记录数、总记录数）转换为自定义的 {@link Pager} 对象。
     * </p>
     *
     * @param page PageHelper 分页对象，包含分页相关的信息
     * @param list 分页查询结果数据列表
     * @param <T>  数据列表的类型
     * @return 包含分页信息的自定义分页对象 {@link Pager}
     * @throws RuntimeException 如果设置分页信息时发生错误，抛出运行时异常
     */
    public static <T> Pager<T> setPageInfo(Page page, T list) {
        try {
            Pager<T> pager = new Pager<>();
            pager.setList(list);
            pager.setPageSize(page.getPageSize());
            pager.setCurrent(page.getPageNum());
            pager.setTotal(page.getTotal());
            return pager;
        } catch (Exception e) {
            throw new RuntimeException("保存当前页码数据时发生错误！", e);
        }
    }
}
