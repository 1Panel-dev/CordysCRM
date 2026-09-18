package cn.cordys.crm.clue.service;

import cn.cordys.crm.clue.domain.Clue;
import cn.cordys.mybatis.BaseMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CluePoolServiceTest {

    @Test
    @SuppressWarnings("unchecked")
    void checkNoPickIgnoresTransitionedClue() {
        BaseMapper<Clue> clueMapper = mock(BaseMapper.class);
        CluePoolService service = new CluePoolService();
        ReflectionTestUtils.setField(service, "clueMapper", clueMapper);

        Clue clue = new Clue();
        when(clueMapper.selectListByLambda(any())).thenReturn(List.of(clue));
        assertTrue(service.checkNoPick("pool-id"));

        clue.setTransitionId("external-customer-id");
        assertFalse(service.checkNoPick("pool-id"));
    }
}
