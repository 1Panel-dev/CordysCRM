package cn.cordys.crm.system.job;

import cn.cordys.common.uid.SerialNumGenerator;
import cn.cordys.crm.system.domain.ModuleForm;
import cn.cordys.mybatis.BaseMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.function.BiPredicate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SerialNumberCleanupJobTest {

    @Test
    @SuppressWarnings("unchecked")
    void checksCurrentFormOwnershipByOrganizationAndCachesEachLookupWithinTheRun() {
        SerialNumGenerator generator = mock(SerialNumGenerator.class);
        BaseMapper<ModuleForm> mapper = mock(BaseMapper.class);
        when(mapper.selectOne(any(ModuleForm.class))).thenAnswer(invocation -> {
            ModuleForm example = invocation.getArgument(0);
            return "org".equals(example.getOrganizationId()) && List.of("contract", "custom-form").contains(example.getFormKey())
                    ? new ModuleForm() : null;
        });
        SerialNumberCleanupJob job = new SerialNumberCleanupJob();
        ReflectionTestUtils.setField(job, "serialNumGenerator", generator);
        ReflectionTestUtils.setField(job, "moduleFormMapper", mapper);

        job.execute();

        ArgumentCaptor<BiPredicate<String, String>> lookup = ArgumentCaptor.forClass(BiPredicate.class);
        verify(generator).clean(lookup.capture());
        assertTrue(lookup.getValue().test("org", "contract"));
        assertTrue(lookup.getValue().test("org", "contract"));
        assertTrue(lookup.getValue().test("org", "custom-form"));
        assertFalse(lookup.getValue().test("org", "customformdata"));
        assertFalse(lookup.getValue().test("other-org", "contract"));
        verify(mapper, times(4)).selectOne(any(ModuleForm.class));

        // 下一次任务重新查询，不能继续使用上次任务缓存的表单存在状态。
        job.execute();
        verify(generator, times(2)).clean(lookup.capture());
        when(mapper.selectOne(any(ModuleForm.class))).thenReturn(null);
        assertFalse(lookup.getValue().test("org", "contract"));
        verify(mapper, times(5)).selectOne(any(ModuleForm.class));
    }
}
