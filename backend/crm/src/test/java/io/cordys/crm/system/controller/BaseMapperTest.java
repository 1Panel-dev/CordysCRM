package io.cordys.crm.system.controller;

import io.cordys.crm.system.domain.User;
import io.cordys.mybatis.BaseMapper;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BaseMapperTest {
    @Mock
    private BaseMapper<User> baseMapper;

    private User user;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        user = new User();
        user.setId("admin");
        user.setEmail("test@a.com");
        user.setPassword("password123");
    }

    @Test
    @Order(1)
    public void testInsert() {
        when(baseMapper.insert(user)).thenReturn(1);
        Integer result = baseMapper.insert(user);
        assertEquals(1, result);
        verify(baseMapper, times(1)).insert(user);
    }

    @Test
    @Order(2)
    public void testBatchInsert() {
        List<User> users = Collections.singletonList(user);
        when(baseMapper.batchInsert(users)).thenReturn(1);
        Integer result = baseMapper.batchInsert(users);
        assertEquals(1, result);
        verify(baseMapper, times(1)).batchInsert(users);
    }

    @Test
    @Order(3)
    public void testUpdateById() {
        when(baseMapper.updateById(user)).thenReturn(1);
        Integer result = baseMapper.updateById(user);
        assertEquals(1, result);
        verify(baseMapper, times(1)).updateById(user);
    }

    @Test
    @Order(4)
    public void testUpdate() {
        when(baseMapper.update(user)).thenReturn(1);
        Integer result = baseMapper.update(user);
        assertEquals(1, result);
        verify(baseMapper, times(1)).update(user);
    }

    @Test
    @Order(5)
    public void testDeleteByPrimaryKey() {
        when(baseMapper.deleteByPrimaryKey(1L)).thenReturn(1);
        Integer result = baseMapper.deleteByPrimaryKey(1L);
        assertEquals(1, result);
        verify(baseMapper, times(1)).deleteByPrimaryKey(1L);
    }

    @Test
    @Order(6)
    public void testDeleteByCriteria() {
        when(baseMapper.delete(user)).thenReturn(1);
        Integer result = baseMapper.delete(user);
        assertEquals(1, result);
        verify(baseMapper, times(1)).delete(user);
    }

    @Test
    @Order(7)
    public void testSelectByPrimaryKey() {
        when(baseMapper.selectByPrimaryKey(1L)).thenReturn(user);
        User result = baseMapper.selectByPrimaryKey(1L);
        assertNotNull(result);
        assertEquals(user.getEmail(), result.getEmail());
        verify(baseMapper, times(1)).selectByPrimaryKey(1L);
    }

    @Test
    @Order(8)
    public void testSelectAll() {
        when(baseMapper.selectAll("username")).thenReturn(Collections.singletonList(user));
        List<User> result = baseMapper.selectAll("username");
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(baseMapper, times(1)).selectAll("username");
    }

    @Test
    @Order(9)
    public void testSelect() {
        when(baseMapper.select(user)).thenReturn(Collections.singletonList(user));
        List<User> result = baseMapper.select(user);
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(baseMapper, times(1)).select(user);
    }

    @Test
    @Order(10)
    public void testSelectOne() {
        when(baseMapper.selectOne(user)).thenReturn(user);
        User result = baseMapper.selectOne(user);
        assertNotNull(result);
        assertEquals(user.getEmail(), result.getEmail());
        verify(baseMapper, times(1)).selectOne(user);
    }

    @Test
    @Order(11)
    public void testSelectByColumn() {
        Serializable[] ids = {1L};
        when(baseMapper.selectByColumn("id", ids)).thenReturn(Collections.singletonList(user));
        List<User> result = baseMapper.selectByColumn("id", ids);
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(baseMapper, times(1)).selectByColumn("id", ids);
    }

    @Test
    @Order(12)
    public void testCountByExample() {
        when(baseMapper.countByExample(user)).thenReturn(1L);
        Long result = baseMapper.countByExample(user);
        assertEquals(1L, result);
        verify(baseMapper, times(1)).countByExample(user);
    }

    @Test
    @Order(13)
    public void testQuery() {
        Function<BaseMapper.SQL, BaseMapper.SQL> sqlBuild = sql -> sql;
        when(baseMapper.query(sqlBuild, user)).thenReturn(Collections.singletonList(user));
        List<User> result = baseMapper.query(sqlBuild, user);
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(baseMapper, times(1)).query(sqlBuild, user);
    }

    @Test
    @Order(14)
    public void testExist() {
        when(baseMapper.exist(user)).thenReturn(true);
        boolean result = baseMapper.exist(user);
        assertTrue(result);
        verify(baseMapper, times(1)).exist(user);
    }
}
