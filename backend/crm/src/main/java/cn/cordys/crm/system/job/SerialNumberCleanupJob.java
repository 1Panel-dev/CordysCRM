package cn.cordys.crm.system.job;

import cn.cordys.common.uid.SerialNumGenerator;
import cn.cordys.crm.system.domain.ModuleForm;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.quartz.anno.QuartzScheduled;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class SerialNumberCleanupJob {

    @Resource
    private SerialNumGenerator serialNumGenerator;
    @Resource
    private BaseMapper<ModuleForm> moduleFormMapper;

    @QuartzScheduled(cron = "0 0 1 * * ?")
    public void execute() {
        // 每次任务按组织、表单查询一次，既识别旧导入 key，也避免预先快照误删刚创建表单的计数。
        Map<String, Boolean> formExistence = new HashMap<>();
        serialNumGenerator.clean((orgId, formKey) -> formExistence.computeIfAbsent(orgId + ":" + formKey, ignored -> {
            ModuleForm example = new ModuleForm();
            example.setOrganizationId(orgId);
            example.setFormKey(formKey);
            return moduleFormMapper.selectOne(example) != null;
        }));
    }
}
