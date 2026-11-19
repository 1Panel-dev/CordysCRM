package cn.cordys.aspectj.aop;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.builder.OperationLogBuilder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OperationLogSourceTest {

    @Test
    void resolvesInterfaceAndRepeatedImplementationAnnotationsWithoutDuplicates() throws NoSuchMethodException {
        Method method = LoggedService.class.getMethod("update");

        List<String> logTypes = new OperationLogSource().computeLogRecordOperations(method, LoggedService.class)
                .stream()
                .map(OperationLogBuilder::getType)
                .sorted()
                .toList();

        assertEquals(List.of("interface", "update", "validate"), logTypes);
    }

    @Test
    void resolvesConcreteMethodAnnotationOnce() throws NoSuchMethodException {
        Method method = LoggedService.class.getMethod("create");

        List<String> logTypes = new OperationLogSource().computeLogRecordOperations(method, LoggedService.class)
                .stream()
                .map(OperationLogBuilder::getType)
                .toList();

        assertEquals(List.of("create"), logTypes);
    }

    private interface Service {
        @OperationLog(type = "interface")
        void update();
    }

    private static class LoggedService implements Service {
        @Override
        @OperationLog(type = "update")
        @OperationLog(type = "validate")
        public void update() {
        }

        @OperationLog(type = "create")
        public void create() {
        }
    }
}
