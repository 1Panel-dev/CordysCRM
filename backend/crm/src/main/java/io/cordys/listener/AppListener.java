package io.cordys.listener;

import io.cordys.common.uid.impl.DefaultUidGenerator;
import io.cordys.common.util.CommonBeanFactory;
import io.cordys.common.util.HikariCPUtils;
import io.cordys.common.util.LogUtils;
import io.cordys.common.util.rsa.RsaKey;
import io.cordys.common.util.rsa.RsaUtils;
import io.cordys.file.engine.*;
import io.cordys.crm.system.service.ExtScheduleService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.SerializationUtils;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
class AppListener implements ApplicationRunner {
    @Resource
    private DefaultUidGenerator uidGenerator;

    @Resource
    private ExtScheduleService extScheduleService;

    /**
     * 应用启动后执行的初始化方法。
     * <p>
     * 此方法会依次初始化唯一 ID 生成器、MinIO 配置和 RSA 配置。
     * </p>
     *
     * @param args 启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        LogUtils.info("===== 开始初始化配置 =====");

        // 初始化唯一ID生成器
        uidGenerator.init();

        // 初始化RSA配置
        LogUtils.info("初始化RSA配置");
        initializeRsaConfiguration();

        LogUtils.info("初始化定时任务");
        extScheduleService.startEnableSchedules();

        HikariCPUtils.printHikariCPStatus();

        LogUtils.info("===== 完成初始化配置 =====");
    }

    /**
     * 初始化 RSA 配置。
     * <p>
     * 此方法首先尝试加载现有的 RSA 密钥。如果不存在，则生成新的 RSA 密钥并保存到文件系统。
     * </p>
     */
    private void initializeRsaConfiguration() {
        FileRequest fileRequest = new FileRequest();
        fileRequest.setFileName("rsa.key");
        fileRequest.setFolder(DefaultRepositoryDir.getSystemRootDir());
        FileRepository fileRepository = FileCenter.getDefaultRepository();

        try {
            byte[] rsaFile = fileRepository.getFile(fileRequest);
            if (rsaFile != null) {
                // 如果RSA密钥文件存在，反序列化并设置密钥
                RsaKey rsaKey = SerializationUtils.deserialize(rsaFile);
                RsaUtils.setRsaKey(rsaKey);
                return;
            }
        } catch (Exception e) {
            LogUtils.error("获取RSA配置失败", e);
        }

        try {
            // 如果RSA密钥文件不存在，生成新的RSA密钥并保存
            RsaKey rsaKey = RsaUtils.getRsaKey();
            byte[] rsaKeyBytes = SerializationUtils.serialize(rsaKey);
            fileRepository.saveFile(rsaKeyBytes, fileRequest);
            RsaUtils.setRsaKey(rsaKey);
        } catch (Exception e) {
            LogUtils.error("初始化RSA配置失败", e);
        }
    }
}