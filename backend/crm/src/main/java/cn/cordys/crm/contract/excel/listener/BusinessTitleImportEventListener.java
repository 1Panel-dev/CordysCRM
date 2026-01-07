package cn.cordys.crm.contract.excel.listener;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.contract.excel.constants.BusinessTitleImportFiled;
import cn.cordys.excel.domain.ExcelErrData;
import cn.idev.excel.context.AnalysisContext;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Strings;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
public class BusinessTitleImportEventListener<T> extends BusinessTitleCheckEventListener {

    @Getter
    private final List<T> dataList;
    @Getter
    protected List<ExcelErrData> errList = new ArrayList<>();
    @Getter
    private int successCount;
    protected static final int BATCH_SIZE = 2000;
    protected final String userId;
    private final Consumer<List<T>> consumer;
    /**
     * method cache
     */
    private final Map<String, Method> methodCache = new HashMap<>();
    /**
     * 业务实体
     */
    private final Class<T> entityClass;

    public BusinessTitleImportEventListener(Class<?> clazz, Class<T> entityClass, Map<String, Boolean> requiredFieldMap, String orgId, String userId, Consumer<List<T>> consumer) {
        super(clazz, requiredFieldMap, orgId);
        this.entityClass = entityClass;
        this.userId = userId;
        this.consumer = consumer;
        this.dataList = new ArrayList<>(BATCH_SIZE);
        // 缓存方法, 频繁反射有开销
        cacheSetterMethods();
    }


    @Override
    public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
        super.invokeHeadMap(headMap, context);
    }

    @Override
    public void invoke(Map<Integer, String> data, AnalysisContext analysisContext) {
        super.invoke(data, analysisContext);
        Integer rowIndex = analysisContext.readRowHolder().getRowIndex();
        if (this.errRows.contains(rowIndex)) {
            // 有错误跳过该行
            return;
        }
        try {
            T t = entityClass.getDeclaredConstructor().newInstance();
            String rowKey = IDGenerator.nextStr();
            setInternal(t, rowKey);
            headMap.forEach((k, v) -> {
                try {
                    setPropertyValue(t, data.get(k), BusinessTitleImportFiled.fromHeader(v).getValue());
                } catch (Exception e) {
                    log.error("导入错误, 无法设置字段值. {}", e.getMessage());
                    throw new GenericException(e);
                }
            });
            dataList.add(t);
        } catch (Exception e) {
            log.error("导入错误, 原因: {}", e.getMessage());
            throw new GenericException(Translator.getWithArgs("import.error", rowIndex + 1).concat(" " + e.getMessage()));
        }

        if (dataList.size() >= BATCH_SIZE) {
            batchProcessData();
        }

    }

    private void setPropertyValue(T instance, Object value, String fieldName) throws Exception {
        Method setter = methodCache.get("set:" + fieldName);
        if (setter != null) {
            setter.invoke(instance, value);
        }
    }


    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        if (CollectionUtils.isNotEmpty(this.dataList)) {
            batchProcessData();
        }
        log.info("数据导入完成, 总行数: {}", successCount);
    }

    /**
     * 批量入库操作
     */
    private void batchProcessData() {
        try {
            // 执行入库
            consumer.accept(this.dataList);
        } catch (Exception e) {
            // 入库异常,不影响后续批次
            log.error("批量插入异常: {}", e.getCause().getMessage());
            throw new GenericException(e.getCause());
        } finally {
            // 批次插入成功, 统计&&清理
            successCount += this.dataList.size();
            this.dataList.clear();
        }
    }


    /**
     * 缓存entity setter
     */
    private void cacheSetterMethods() {
        for (Method method : entityClass.getMethods()) {
            if (method.getName().startsWith("set") && method.getParameterCount() == 1) {
                String fieldName = method.getName().substring(3);
                String property = Character.toLowerCase(fieldName.charAt(0)) + fieldName.substring(1);
                methodCache.put("set:" + property, method);
            } else if (method.getParameterCount() == 0 && Strings.CS.equals(method.getName(), "getId")) {
                methodCache.put("get:id", method);
            }
        }
    }


    /**
     * 设置entity内部字段
     *
     * @param instance 实例对象
     * @param rowKey   唯一Key
     * @throws Exception 异常
     */
    private void setInternal(T instance, String rowKey) throws Exception {
        methodCache.get("set:id").invoke(instance, rowKey);
        methodCache.get("set:createUser").invoke(instance, userId);
        methodCache.get("set:createTime").invoke(instance, System.currentTimeMillis());
        methodCache.get("set:updateUser").invoke(instance, userId);
        methodCache.get("set:updateTime").invoke(instance, System.currentTimeMillis());
        methodCache.get("set:organizationId").invoke(instance, orgId);
    }

}
