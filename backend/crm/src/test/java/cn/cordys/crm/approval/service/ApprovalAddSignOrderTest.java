package cn.cordys.crm.approval.service;

import cn.cordys.common.uid.impl.DefaultUidGenerator;
import cn.cordys.common.uid.worker.WorkerIdAssigner;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.crm.approval.constants.ApprovalAddSignType;
import cn.cordys.crm.approval.constants.ApprovalStatus;
import cn.cordys.crm.approval.constants.ApprovalTaskType;
import cn.cordys.crm.approval.domain.ApprovalAddSignTask;
import cn.cordys.crm.approval.domain.ApprovalTask;
import cn.cordys.crm.approval.dto.AddSignSortInfo;
import cn.cordys.crm.approval.mapper.ExtApprovalTaskMapper;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApprovalAddSignOrderTest {

    private static ApplicationContext previousContext;

    private final ApprovalActionService service = new ApprovalActionService();
    private final List<ApprovalAddSignTask> signs = new ArrayList<>();
    private final Map<String, ApprovalTask> tasks = new HashMap<>();
    private ExtApprovalTaskMapper extTaskMapper;

    @BeforeAll
    static void initIdGenerator() {
        previousContext = (ApplicationContext) ReflectionTestUtils.getField(CommonBeanFactory.class, "context");
        DefaultUidGenerator generator = new DefaultUidGenerator();
        ReflectionTestUtils.setField(generator, "workerIdAssigner", (WorkerIdAssigner) () -> 1L);
        generator.init();
        ApplicationContext context = mock(ApplicationContext.class);
        when(context.getBean(DefaultUidGenerator.class)).thenReturn(generator);
        new CommonBeanFactory().setApplicationContext(context);
    }

    @AfterAll
    static void restoreContext() {
        new CommonBeanFactory().setApplicationContext(previousContext);
    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        BaseMapper<ApprovalAddSignTask> signMapper = mock(BaseMapper.class);
        BaseMapper<ApprovalTask> taskMapper = mock(BaseMapper.class);
        extTaskMapper = mock(ExtApprovalTaskMapper.class);
        ReflectionTestUtils.setField(service, "approvalAddSignTaskMapper", signMapper);
        ReflectionTestUtils.setField(service, "approvalTaskMapper", taskMapper);
        ReflectionTestUtils.setField(service, "extApprovalTaskMapper", extTaskMapper);
        when(signMapper.selectOne(any(ApprovalAddSignTask.class))).thenAnswer(invocation -> {
            ApprovalAddSignTask criteria = invocation.getArgument(0);
            return signs.stream().filter(sign -> Objects.equals(sign.getTaskId(), criteria.getTaskId()))
                    .findFirst().orElse(null);
        });
        when(signMapper.selectListByLambda(any())).thenAnswer(invocation -> {
            LambdaQueryWrapper<ApprovalAddSignTask> wrapper = invocation.getArgument(0);
            Object rootId = wrapper.getParams().get("root_task_id_eq");
            Long currentSort = (Long) wrapper.getParams().get("sort_gt");
            return signs.stream().filter(sign -> Objects.equals(sign.getRootTaskId(), rootId))
                    .filter(sign -> currentSort == null || sign.getSort() > currentSort)
                    .sorted(Comparator.comparing(ApprovalAddSignTask::getSort)
                            .thenComparing(ApprovalAddSignTask::getId))
                    .toList();
        });
        when(taskMapper.selectByPrimaryKey(any())).thenAnswer(invocation -> tasks.get(invocation.getArgument(0)));
        task("A", ApprovalTaskType.NL);
    }

    @Test
    void mixedNestedSignResumesItsParentInsteadOfReplayingCompletedPredecessor() {
        addSign("A", "B", ApprovalAddSignType.BEFORE);
        addSign("B", "C", ApprovalAddSignType.BEFORE);
        addSign("C", "D", ApprovalAddSignType.AFTER);
        addSign("D", "E", ApprovalAddSignType.BEFORE);

        assertEquals(List.of("C", "E", "D", "B"), orderedTasks());
        tasks.get("E").setStatus(ApprovalStatus.APPROVED.name());
        ApprovalTask next = ReflectionTestUtils.invokeMethod(service, "getNextAddSignTask", "E");

        assertNotNull(next);
        assertEquals("D", next.getApproverId());
        assertEquals(ApprovalStatus.APPROVING.name(), next.getStatus());
        assertNotEquals("D", next.getId());
        assertEquals(ApprovalStatus.APPROVED.name(), tasks.get("C").getStatus());
        assertTrue(signs.stream().anyMatch(sign -> "C".equals(sign.getTaskId())));
        verify(extTaskMapper).updateRootNext("D", next.getId());
    }

    @ParameterizedTest
    @EnumSource(ApprovalAddSignType.class)
    void repeatedDenseInsertionsKeepUniqueSortAndTheExpectedSuccessor(ApprovalAddSignType type) {
        addSign("A", "B", ApprovalAddSignType.BEFORE);
        addSign("B", "C", ApprovalAddSignType.BEFORE);
        addSign("C", "D", ApprovalAddSignType.AFTER);
        List<String> expected = new ArrayList<>(List.of("C", "D", "B"));
        String source = "D";
        for (int i = 0; i < 32; i++) {
            String child = "dense-" + i;
            expected.add(expected.indexOf(source) + (type == ApprovalAddSignType.BEFORE ? 0 : 1), child);
            addSign(source, child, type);
            source = child;
            assertEquals(expected, orderedTasks());
            assertEquals(signs.size(), signs.stream().map(ApprovalAddSignTask::getSort).distinct().count());
        }

        tasks.get(source).setStatus(ApprovalStatus.APPROVED.name());
        ApprovalTask next = ReflectionTestUtils.invokeMethod(service, "getNextAddSignTask", source);

        assertNotNull(next);
        assertEquals(expected.get(expected.indexOf(source) + 1), next.getApproverId());
        assertEquals(ApprovalStatus.APPROVED.name(), tasks.get("C").getStatus());
    }

    @ParameterizedTest
    @EnumSource(ApprovalAddSignType.class)
    void restoredRootAppendsNewSignAfterItsCompletedSignChain(ApprovalAddSignType type) {
        addSign("A", "B", ApprovalAddSignType.BEFORE);
        tasks.get("B").setStatus(ApprovalStatus.APPROVED.name());
        task("A-restored", ApprovalTaskType.NL);
        signs.getFirst().setRootTaskId("A-restored");
        signs.getFirst().setSignTaskId("A-restored");

        addSign("A-restored", "C", type);

        assertEquals(List.of("B", "C"), orderedTasks());
        assertEquals(100L, signs.getFirst().getSort());
        assertEquals(200L, signs.getLast().getSort());
        assertEquals("A-restored", signs.getLast().getRootTaskId());
    }

    private void addSign(String sourceId, String taskId, ApprovalAddSignType type) {
        AddSignSortInfo info = ReflectionTestUtils.invokeMethod(service, "calculateAddSignSort", sourceId,
                type.name());
        assertNotNull(info);
        ApprovalAddSignTask sign = new ApprovalAddSignTask();
        sign.setId("relation-" + taskId);
        sign.setTaskId(taskId);
        sign.setSignTaskId(sourceId);
        sign.setRootTaskId(info.getRootTaskId());
        sign.setType(type.name());
        sign.setSort(info.getSort());
        signs.add(sign);
        tasks.get(sourceId).setStatus(type == ApprovalAddSignType.BEFORE
                ? ApprovalStatus.PENDING.name() : ApprovalStatus.APPROVED.name());
        task(taskId, ApprovalTaskType.SN);
    }

    private void task(String id, ApprovalTaskType type) {
        ApprovalTask task = new ApprovalTask();
        task.setId(id);
        task.setInstanceId("instance-1");
        task.setNodeId("NODE001");
        task.setApproverId(id);
        task.setNodeRound(1);
        task.setType(type.name());
        task.setStatus(ApprovalStatus.APPROVING.name());
        tasks.put(id, task);
    }

    private List<String> orderedTasks() {
        return signs.stream().sorted(Comparator.comparing(ApprovalAddSignTask::getSort))
                .map(ApprovalAddSignTask::getTaskId).toList();
    }
}
