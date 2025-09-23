package cn.cordys.common.pager;

import lombok.Data;

/**
 * 分页类，封装分页数据。
 * <p>
 * 该类用于表示分页查询结果，包括数据列表、总记录数、每页记录数和当前页码等信息。
 * </p>
 *
 * @param <T> 数据列表的类型
 */
@Data
public class Pager<T> {

    /**
     * 数据列表，分页查询结果的具体数据。
     */
    private T list;

    /**
     * 总记录数，表示符合查询条件的总条数。
     */
    private long total;

    /**
     * 每页记录数，表示每一页显示的数据条数。
     */
    private long pageSize;

    /**
     * 当前页码，表示当前显示的是哪一页。
     */
    private long current;

    /**
     * 无参构造函数，初始化一个空的分页对象。
     */
    public Pager() {
    }

    /**
     * 带参构造函数，用于初始化分页对象。
     *
     * @param list     数据列表
     * @param total    总记录数
     * @param pageSize 每页记录数
     * @param current  当前页码
     */
    public Pager(T list, long total, long pageSize, long current) {
        this.list = list;
        this.total = total;
        this.pageSize = pageSize;
        this.current = current;
    }
}
