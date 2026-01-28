package cn.cordys.crm.integration.sqlbot.service;

import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.constants.RoleDataScope;
import cn.cordys.common.constants.ThirdConfigTypeConstants;
import cn.cordys.common.dto.BaseTreeNode;
import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.common.dto.RoleDataScopeDTO;
import cn.cordys.common.dto.RolePermissionDTO;
import cn.cordys.common.permission.PermissionCache;
import cn.cordys.common.response.handler.RestControllerExceptionHandler;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.common.util.CodingUtils;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.integration.sqlbot.constant.SQLBotTable;
import cn.cordys.crm.integration.sqlbot.dto.*;
import cn.cordys.crm.integration.sqlbot.handler.TablePermissionHandler;
import cn.cordys.crm.integration.sqlbot.handler.TablePermissionHandlerFactory;
import cn.cordys.crm.integration.sqlbot.mapper.ExtDataSourceMapper;
import cn.cordys.crm.system.domain.OrganizationConfigDetail;
import cn.cordys.crm.system.domain.OrganizationUser;
import cn.cordys.crm.system.domain.RoleScopeDept;
import cn.cordys.crm.system.service.DepartmentService;
import cn.cordys.crm.system.service.RoleService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/** 数据源服务，用于处理数据库连接和表结构信息 */
@Service
@Slf4j
public class DataSourceService {

  private static final DatabaseConfig MYSQL_CONFIG =
      new DatabaseConfig("jdbc:mysql://", List.of("sys_", "qrtz_"), 3306);
  // MySQL JDBC URL解析正则
  private static final Pattern MYSQL_URL_PATTERN =
      Pattern.compile("jdbc:mysql://([^:/]+)(?::(\\d+))?/([^?]+)(?:\\?(.*))?");
  private final String url;
  private final String username;
  private final String password;
  @Resource private DataScopeService dataScopeService;
  @Resource private PermissionCache permissionCache;
  @Resource private DepartmentService departmentService;
  @Resource private RoleService roleService;

  @Value("${sqlbot.encrypt:false}")
  private boolean encryptEnabled;

  @Value("${sqlbot.aes-key:${random.value}}")
  private String aesKey;

  @Value("${sqlbot.aes-iv:${random.value}}")
  private String aesIv;

  @Resource private ExtDataSourceMapper extDataSourceMapper;
  @Resource private BaseMapper<OrganizationConfigDetail> organizationConfigDetailMapper;

  /** 构造函数依赖注入 */
  public DataSourceService(
      @Value("${spring.datasource.url}") String url,
      @Value("${sqlbot.datasource.username:${spring.datasource.username}}") String username,
      @Value("${sqlbot.datasource.password:${spring.datasource.password}}") String password) {
    this.url = url;
    this.username = username;
    this.password = password;
  }

  private void aesEncryptDataSource(DataSourceDTO dataSource) {
    if (BooleanUtils.isTrue(encryptEnabled)) {
      dataSource.setHost(aesEncrypt(dataSource.getHost()));
      dataSource.setUser(aesEncrypt(dataSource.getUser()));
      dataSource.setPassword(aesEncrypt(dataSource.getPassword()));
      dataSource.setDataBase(aesEncrypt(dataSource.getDataBase()));
      dataSource.setSchema(aesEncrypt(dataSource.getSchema()));
    }
  }

  private String aesEncrypt(String text) {
    return CodingUtils.aesCBCEncrypt(text, aesKey, aesIv);
  }

  /**
   * 获取当前数据库模式信息
   *
   * @return 数据源DTO列表
   */
  public SQLBotDTO getDatabaseSchema(String userId, String orgId) {
    try {
      // 验证是否启用了 SQL Bot 功能
      List<OrganizationConfigDetail> organizationConfigDetails =
          organizationConfigDetailMapper.selectListByLambda(
              new LambdaQueryWrapper<OrganizationConfigDetail>()
                  .eq(OrganizationConfigDetail::getType, ThirdConfigTypeConstants.SQLBOT.name())
                  .eq(OrganizationConfigDetail::getEnable, true));
      if (CollectionUtils.isEmpty(organizationConfigDetails)) {
        throw new IllegalArgumentException("当前组织未配置 SQL Bot 功能");
      }

      var databaseName = extractDatabaseName();
      var allTables =
          Objects.requireNonNull(CommonBeanFactory.getBean(DataSourceService.class))
              .tableList(databaseName);

      Map<String, SQLBotTable> sqlBotTableMap =
          Arrays.stream(SQLBotTable.values())
              .collect(Collectors.toMap(SQLBotTable::getTableName, Function.identity()));

      // 获取权限信息
      List<RolePermissionDTO> rolePermissions = permissionCache.getRolePermissions(userId, orgId);
      // 添加虚拟表
      appendVirtualTable(allTables, sqlBotTableMap.values());
      // 过滤符合条件的表
      var filteredTables = filterTable(userId, allTables, sqlBotTableMap, rolePermissions);

      Map<String, DeptDataPermissionDTO> deptDataPermissionMap =
          getDeptDataPermissionMap(userId, orgId, rolePermissions);

      filteredTables.forEach(
          table -> {
            SQLBotTable tableInfo = sqlBotTableMap.get(table.getName());
            TablePermissionHandler tablePermissionHandler =
                TablePermissionHandlerFactory.getTableHandler(tableInfo);
            if (tablePermissionHandler != null) {
              // 权限处理器添加sql过滤
              String permission = tableInfo.getPermission();
              DeptDataPermissionDTO dataPermission = deptDataPermissionMap.get(permission);
              TableHandleParam tableHandleParam =
                  new TableHandleParam(userId, orgId, tableInfo, dataPermission);
              tablePermissionHandler.handleTable(table, tableHandleParam);
            }
            if (StringUtils.isNotBlank(table.getSql())) {
              table.setSql(table.getSql().replace("\n", " "));
            }
          });

      var dataSourceDTO = new DataSourceDTO();
      populateDataSourceProperties(dataSourceDTO);
      dataSourceDTO.setTables(filteredTables);
      aesEncryptDataSource(dataSourceDTO);

      return SQLBotDTO.builder().code(0).data(List.of(dataSourceDTO)).build();
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      return SQLBotDTO.builder()
          .code(1)
          .message("获取数据库结构失败: " + RestControllerExceptionHandler.getStackTraceAsString(e))
          .build();
    }
  }

  @Cacheable(value = "table_schema_cache", key = "#databaseName", unless = "#result == null")
  public List<TableDTO> tableList(String databaseName) {
    var schemaJson = extDataSourceMapper.selectSchemaByDbName(databaseName);
    return JSON.parseArray(schemaJson, TableDTO.class);
  }

  private boolean isAdmin(String userId) {
    return Strings.CS.equals(userId, InternalUser.ADMIN.getValue());
  }

  /**
   * 过滤符合条件的表
   *
   * @param filteredTables
   * @param sqlBotTableMap
   * @param rolePermissions
   * @return
   */
  private List<TableDTO> filterTable(
      String userId,
      List<TableDTO> filteredTables,
      Map<String, SQLBotTable> sqlBotTableMap,
      List<RolePermissionDTO> rolePermissions) {
    Set<String> permissionIds =
        rolePermissions.stream()
            .map(RolePermissionDTO::getPermissions)
            .flatMap(Set::stream)
            .collect(Collectors.toSet());

    Map<String, TableDTO> tableMap =
        filteredTables.stream().collect(Collectors.toMap(TableDTO::getName, Function.identity()));

    // 过滤符合条件的表
    return filteredTables.stream()
        .filter(
            table -> {
              SQLBotTable sqlBotTable = sqlBotTableMap.get(table.getName());
              if (sqlBotTable == null) {
                return false;
              }

              if (StringUtils.isNotBlank(sqlBotTable.getCopyFromTableName())) {
                table.setFields(tableMap.get(sqlBotTable.getCopyFromTableName()).getFields());
              }

              if (isAdmin(userId)) {
                return true;
              }

              // 替换表注释
              table.setComment(sqlBotTableMap.get(table.getName()).getDescription());

              if (CollectionUtils.isNotEmpty(sqlBotTable.getDisableFields())) {
                // 过滤禁用的字段
                List<FieldDTO> filterFields =
                    table.getFields().stream()
                        .filter(field -> !sqlBotTable.getDisableFields().contains(field.getName()))
                        .collect(Collectors.toList());
                table.setFields(filterFields);
              }
              // 检查权限,null 表示不需要权限
              return sqlBotTable.getPermission() == null
                  || permissionIds.contains(sqlBotTable.getPermission());
            })
        .collect(Collectors.toList());
  }

  /**
   * 添加虚拟表
   *
   * @param filteredTables
   * @param sqlBotTables
   */
  private void appendVirtualTable(
      List<TableDTO> filteredTables, Collection<SQLBotTable> sqlBotTables) {
    Set<String> tableNames =
        filteredTables.stream().map(TableDTO::getName).collect(Collectors.toSet());
    sqlBotTables.forEach(
        sqlBotTable -> {
          // 如果表不存在，则将虚拟表添加到结果集中
          if (!tableNames.contains(sqlBotTable.getTableName())) {
            TableDTO addTable = new TableDTO();
            addTable.setName(sqlBotTable.getTableName());
            addTable.setComment(sqlBotTable.getDescription());
            filteredTables.add(addTable);
          }
        });
  }

  private Map<String, DeptDataPermissionDTO> getDeptDataPermissionMap(
      String userId, String orgId, List<RolePermissionDTO> rolePermissions) {
    Map<String, DeptDataPermissionDTO> deptDataPermissionMap = new HashMap<>();

    // DataScope和角色的映射
    Map<String, List<RolePermissionDTO>> dataScopeRoleMap =
        rolePermissions.stream()
            .collect(Collectors.groupingBy(RoleDataScopeDTO::getDataScope, Collectors.toList()));

    // 本部门角色
    List<RolePermissionDTO> userDeptRoles =
        dataScopeRoleMap.get(RoleDataScope.DEPT_AND_CHILD.name());
    // 指定部门角色
    List<RolePermissionDTO> customDeptRoles =
        dataScopeRoleMap.get(RoleDataScope.DEPT_CUSTOM.name());

    if (isAdmin(userId)) {
      return deptDataPermissionMap;
    }

    List<BaseTreeNode> tree = List.of();
    if (CollectionUtils.isNotEmpty(userDeptRoles) || CollectionUtils.isNotEmpty(customDeptRoles)) {
      // 查询部门树
      tree = departmentService.getTree(orgId);
    }

    List<String> currentUserDeptIds = List.of();
    if (CollectionUtils.isNotEmpty(userDeptRoles)) {
      // 查看用户部门及其子部门数据
      OrganizationUser organizationUser = dataScopeService.getOrganizationUser(userId, orgId);
      currentUserDeptIds =
          dataScopeService.getDeptIdsWithChild(tree, Set.of(organizationUser.getDepartmentId()));
    }

    Map<String, List<String>> customDeptRoleDeptMap = new HashMap<>();
    if (CollectionUtils.isNotEmpty(customDeptRoles)) {
      // 查看指定部门及其子部门数据
      List<String> customDeptRolesIds =
          customDeptRoles.stream().map(RoleDataScopeDTO::getId).toList();

      List<RoleScopeDept> roleScopeDeptList =
          roleService.getRoleScopeDeptByRoleIds(customDeptRolesIds);
      roleScopeDeptList.forEach(
          roleScopeDept -> {
            customDeptRoleDeptMap.putIfAbsent(roleScopeDept.getRoleId(), new ArrayList<>());
            customDeptRoleDeptMap
                .get(roleScopeDept.getRoleId())
                .add(roleScopeDept.getDepartmentId());
          });
    }

    for (String dataScopePermission : SQLBotTable.getDataScopePermissions()) {
      DeptDataPermissionDTO deptDataPermission = new DeptDataPermissionDTO();
      deptDataPermissionMap.put(dataScopePermission, deptDataPermission);
      boolean hasAllPermission =
          dataScopeService.hasDataScopePermission(
              dataScopeRoleMap, RoleDataScope.ALL.name(), dataScopePermission);
      if (hasAllPermission) {
        deptDataPermission.setAll(true);
        continue;
      }
      if (CollectionUtils.isNotEmpty(userDeptRoles)) {
        // 添加当前用户的部门及子部门ID
        deptDataPermission.getDeptIds().addAll(currentUserDeptIds);
      }
      if (CollectionUtils.isNotEmpty(customDeptRoles)) {
        List<String> parentDeptIds = new ArrayList<>();
        for (RolePermissionDTO customDeptRole : customDeptRoles) {
          if (CollectionUtils.isNotEmpty(customDeptRoleDeptMap.get(customDeptRole.getId()))) {
            // 获取指定部门角色中的部门Id
            List<String> deptIds = customDeptRoleDeptMap.get(customDeptRole.getId());
            parentDeptIds.addAll(deptIds);
          }
        }
        // 获取部门及子部门ID
        List<String> deptIds =
            dataScopeService.getDeptIdsWithChild(tree, new HashSet<>(parentDeptIds));
        deptDataPermission.getDeptIds().addAll(deptIds);
      }
    }
    return deptDataPermissionMap;
  }

  /** 从数据库URL中提取数据库名称 */
  private String extractDatabaseName() {
    var matcher = MYSQL_URL_PATTERN.matcher(url);
    if (matcher.find() && matcher.groupCount() >= 3) {
      return matcher.group(3);
    }

    // 回退方法
    var parts = url.split("/");
    var dbPart = parts[parts.length - 1];
    return dbPart.contains("?") ? dbPart.substring(0, dbPart.indexOf("?")) : dbPart;
  }

  /** 填充数据源属性 */
  private void populateDataSourceProperties(DataSourceDTO dataSourceDTO) {
    var matcher = MYSQL_URL_PATTERN.matcher(url);

    if (matcher.find() && matcher.groupCount() >= 3) {
      var host = matcher.group(1);
      var port =
          matcher.group(2) != null
              ? Integer.parseInt(matcher.group(2))
              : MYSQL_CONFIG.defaultPort();
      var database = matcher.group(3);
      var params = matcher.groupCount() >= 4 && matcher.group(4) != null ? matcher.group(4) : "";
      dataSourceDTO.setHost(host);
      dataSourceDTO.setPort(port);
      dataSourceDTO.setUser(username);
      dataSourceDTO.setPassword(password);
      dataSourceDTO.setType("mysql");
      dataSourceDTO.setName(database);
      dataSourceDTO.setDataBase(database);
      dataSourceDTO.setExtraParams(params);
    } else {
      fallbackPopulateDataSourceProperties(dataSourceDTO);
    }
  }

  /** 回退方法：填充数据源属性 */
  private void fallbackPopulateDataSourceProperties(DataSourceDTO dataSourceDTO) {
    var strippedUrl = url.replace(MYSQL_CONFIG.jdbcPrefix(), "");
    var urlParts = strippedUrl.split("\\?", 2);

    var hostPortDb = urlParts[0];
    var extraParams = urlParts.length > 1 ? urlParts[1] : "";

    var hostAndRest = hostPortDb.split("/", 2);
    var hostPort = hostAndRest[0].split(":", 2);

    var host = hostPort[0];
    var port = hostPort.length > 1 ? parsePortSafely(hostPort[1]) : MYSQL_CONFIG.defaultPort();
    dataSourceDTO.setHost(host);
    dataSourceDTO.setPort(port);
    dataSourceDTO.setUser(username);
    dataSourceDTO.setPassword(password);
    dataSourceDTO.setDataBase(extractDatabaseName());
    dataSourceDTO.setExtraParams(extraParams);
  }

  /** 安全解析端口号 */
  private int parsePortSafely(String portStr) {
    try {
      return Integer.parseInt(portStr);
    } catch (NumberFormatException e) {
      return MYSQL_CONFIG.defaultPort();
    }
  }

  /** MySQL数据库配置 */
  private record DatabaseConfig(
      String jdbcPrefix, List<String> excludedTablePrefixes, int defaultPort) {}
}
