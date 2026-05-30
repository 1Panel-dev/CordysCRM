# CRM 系统 Go 语言重构方案

## 一、项目概述

### 1.1 原系统分析
- **技术栈**: Spring Boot + MyBatis + Shiro + Jetty
- **代码规模**: 1481 个 Java 文件
- **核心模块**: 
  - customer (客户管理)
  - opportunity (商机管理)
  - contract (合同管理)
  - order (订单管理)
  - approval (审批管理)
  - system (系统管理)
  - integration (集成管理)

### 1.2 目标技术栈
- **语言版本**: Go 1.23+ (使用最新特性)
- **Web 框架**: Gin v1.9+
- **ORM**: GORM v1.25+
- **权限控制**: Casbin v2.65+
- **验证库**: Go Play Validator v10+
- **文档生成**: Swag v1.16+
- **配置管理**: Viper v1.18+
- **日志**: Zap v1.26+
- **数据库迁移**: Golang Migrate v4.17+

## 二、Go 特性优势

### 2.1 Go 1.23 新特性应用
```go
// 1. iter 包和 range over 函数 (Go 1.23)
func (s *CustomerService) StreamCustomers(ctx context.Context) iter.Seq[*Customer] {
    return func(yield func(*Customer) bool) {
        // 流式处理大数据
    }
}

// 2. 泛型增强
type Repository[T Entity, ID comparable] struct {
    db *gorm.DB
}

// 3. 错误包装改进
errors.Join()  // 多错误处理
```

### 2.2 性能对比
| 指标 | Java (Spring Boot) | Go 1.23 | 提升 |
|------|-------------------|---------|------|
| 内存占用 | ~800MB | ~80MB | 90%↓ |
| 启动时间 | ~15s | ~1s | 93%↓ |
| QPS (单核) | ~3000 | ~15000 | 5x↑ |
| GC 停顿 | ~50ms | ~1ms | 98%↓ |

## 三、项目结构设计

### 3.1 标准 Go 项目布局
```
backend-go/
├── cmd/
│   └── crm-server/
│       └── main.go              # 应用入口
├── internal/
│   ├── config/                  # 配置管理
│   │   ├── config.go
│   │   └── database.go
│   ├── handler/                 # HTTP 处理器 (对应 Controller)
│   │   ├── customer/
│   │   ├── opportunity/
│   │   └── contract/
│   ├── service/                 # 业务逻辑层
│   │   ├── customer/
│   │   └── ...
│   ├── repository/              # 数据访问层 (对应 Mapper)
│   │   ├── customer/
│   │   └── ...
│   ├── model/                   # 数据模型 (对应 Domain/DTO)
│   │   ├── entity/              # 数据库实体
│   │   ├── dto/                 # 数据传输对象
│   │   └── vo/                  # 视图对象
│   ├── middleware/              # 中间件 (对应 AOP)
│   │   ├── auth.go
│   │   ├── permission.go
│   │   ├── datascop.go
│   │   └── logger.go
│   ├── pkg/                     # 通用工具包
│   │   ├── response/
│   │   ├── validator/
│   │   └── utils/
│   └── integration/             # 外部集成
├── pkg/                         # 可复用公共库
│   ├── constants/
│   ├── errors/
│   └── types/
├── migrations/                  # 数据库迁移脚本
├── configs/                     # 配置文件
├── scripts/                     # 构建脚本
├── go.mod
├── go.sum
├── Makefile
└── README.md
```

## 四、核心模块转换映射

### 4.1 层级映射关系
| Java (Spring) | Go (Gin) | 说明 |
|--------------|----------|------|
| @RestController | handler + router | HTTP 接口层 |
| @Service | service | 业务逻辑层 |
| Mapper/XML | repository + GORM | 数据访问层 |
| @Domain/@Entity | model/entity | 数据模型 |
| DTO | model/dto | 数据传输对象 |
| @Aspect/AOP | middleware | 切面/中间件 |
| Shiro | Casbin + JWT | 权限控制 |
| PageHelper | GORM Paginate | 分页 |
| Lombok | 原生 struct + tags | 代码简化 |

### 4.2 关键特性实现

#### 4.2.1 权限控制 (Shiro → Casbin)
```go
// Java: @RequiresPermissions("customer:read")
// Go: 中间件方式
func PermissionMiddleware(permission string) gin.HandlerFunc {
    return func(c *gin.Context) {
        enforcer := c.MustGet("casbin").(*casbin.Enforcer)
        userID := GetUserID(c)
        
        allowed, _ := enforcer.Enforce(userID, permission)
        if !allowed {
            c.AbortWithStatusJSON(403, gin.H{"error": "permission denied"})
            return
        }
        c.Next()
    }
}

// 使用
router.GET("/customer/:id", 
    PermissionMiddleware("customer:read"),
    handler.GetCustomer)
```

#### 4.2.2 数据范围权限
```go
// Java: dataScopeService.getDeptDataPermission()
// Go: Service 层注入
type CustomerService struct {
    repo      *CustomerRepository
    dataScope *DataScopeService
}

func (s *CustomerService) List(ctx context.Context, req *ListRequest, userID string) (*Pager[Customer], error) {
    // 获取数据权限
    scope := s.dataScope.GetDeptDataPermission(userID, req.ViewID)
    
    // 应用数据范围过滤
    query := s.repo.DB().WithContext(ctx)
    query = scope.Apply(query) // 自动添加组织/部门过滤条件
    
    var customers []Customer
    var total int64
    query.Model(&Customer{}).Count(&total)
    query.Offset(req.Offset()).Limit(req.PageSize).Find(&customers)
    
    return &Pager[Customer]{List: customers, Total: total}, nil
}
```

#### 4.2.3 操作日志 (AOP → Middleware + Context)
```go
// Java: @OperationLog + Aspect
// Go: 中间件 + Context
type OperationLogMiddleware struct {
    logger *zap.Logger
    service *OperationLogService
}

func (m *OperationLogMiddleware) Handle() gin.HandlerFunc {
    return func(c *gin.Context) {
        // 记录请求前
        logCtx := &OperationLogContext{
            UserID:     GetUserID(c),
            Module:     c.GetString("log_module"),
            ActionType: c.Request.Method,
            StartTime:  time.Now(),
        }
        c.Set("op_log_ctx", logCtx)
        
        c.Next()
        
        // 记录响应后
        logCtx.ResponseStatus = c.Writer.Status()
        logCtx.Duration = time.Since(logCtx.StartTime)
        m.service.AsyncLog(logCtx)
    }
}

// 使用
router.POST("/customer/add",
    OperationLogMiddleware.WithModule(LogModuleCustomer),
    handler.CreateCustomer)
```

## 五、核心代码示例

### 5.1 客户管理模块 - Handler (对应 Controller)
```go
// internal/handler/customer/customer_handler.go
package customer

import (
    "github.com/gin-gonic/gin"
    "crm/internal/model/dto"
    "crm/internal/service/customer"
    "crm/internal/pkg/response"
)

type CustomerHandler struct {
    service *customer.CustomerService
}

func NewCustomerHandler(svc *customer.CustomerService) *CustomerHandler {
    return &CustomerHandler{service: svc}
}

// List 客户列表 godoc
// @Summary 客户列表
// @Tags 客户
// @Accept json
// @Produce json
// @Param request body dto.CustomerPageRequest true "查询参数"
// @Success 200 {object} response.Pager[dto.CustomerListResponse]
// @Router /account/page [post]
func (h *CustomerHandler) List(c *gin.Context) {
    var req dto.CustomerPageRequest
    if err := c.ShouldBindJSON(&req); err != nil {
        response.Error(c, err)
        return
    }
    
    userID := GetUserID(c)
    orgID := GetOrgID(c)
    
    result, err := h.service.List(c.Request.Context(), &req, userID, orgID)
    if err != nil {
        response.Error(c, err)
        return
    }
    
    response.Success(c, result)
}

// Get 客户详情
func (h *CustomerHandler) Get(c *gin.Context) {
    id := c.Param("id")
    userID := GetUserID(c)
    orgID := GetOrgID(c)
    
    result, err := h.service.GetWithDataPermission(c.Request.Context(), id, userID, orgID)
    if err != nil {
        response.Error(c, err)
        return
    }
    
    response.Success(c, result)
}

// Create 创建客户
func (h *CustomerHandler) Create(c *gin.Context) {
    var req dto.CustomerAddRequest
    if err := c.ShouldBindJSON(&req); err != nil {
        response.Error(c, err)
        return
    }
    
    userID := GetUserID(c)
    orgID := GetOrgID(c)
    
    result, err := h.service.Create(c.Request.Context(), &req, userID, orgID)
    if err != nil {
        response.Error(c, err)
        return
    }
    
    response.Success(c, result)
}
```

### 5.2 客户管理模块 - Service
```go
// internal/service/customer/customer_service.go
package customer

import (
    "context"
    "errors"
    "crm/internal/model/entity"
    "crm/internal/model/dto"
    "crm/internal/repository/customer"
    "crm/internal/middleware/datascop"
)

type CustomerService struct {
    repo      *customer.CustomerRepository
    dataScope *datascop.DataScopeService
}

func NewCustomerService(repo *customer.CustomerRepository, ds *datascop.DataScopeService) *CustomerService {
    return &CustomerService{repo: repo, dataScope: ds}
}

// List 客户列表
func (s *CustomerService) List(ctx context.Context, req *dto.CustomerPageRequest, userID, orgID string) (*dto.Pager[dto.CustomerListResponse], error) {
    // 获取数据权限
    scope := s.dataScope.GetDeptDataPermission(userID, orgID, req.ViewID, "customer:read")
    
    // 构建查询
    query := s.repo.Query(ctx)
    query = scope.Apply(query)
    
    // 应用筛选条件
    if req.Name != "" {
        query = query.Where("name LIKE ?", "%"+req.Name+"%")
    }
    
    // 分页查询
    var customers []entity.Customer
    total, err := query.Count()
    if err != nil {
        return nil, err
    }
    
    err = query.Offset(req.GetOffset()).Limit(req.PageSize).Order("created_at DESC").Find(&customers).Error
    if err != nil {
        return nil, err
    }
    
    // 转换为 DTO
    list := make([]dto.CustomerListResponse, len(customers))
    for i, c := range customers {
        list[i] = dto.ToCustomerListResponse(&c)
    }
    
    return &dto.Pager[dto.CustomerListResponse]{
        List:  list,
        Total: total,
        Current: req.Current,
        PageSize: req.PageSize,
    }, nil
}

// GetWithDataPermission 带权限检查的详情查询
func (s *CustomerService) GetWithDataPermission(ctx context.Context, id, userID, orgID string) (*dto.CustomerGetResponse, error) {
    customer, err := s.repo.GetByID(ctx, id)
    if err != nil {
        return nil, errors.New("customer not found")
    }
    
    // 检查数据权限
    if !s.hasReadPermission(customer, userID, orgID) {
        return nil, errors.New("permission denied")
    }
    
    return dto.ToCustomerGetResponse(customer), nil
}

// Create 创建客户
func (s *CustomerService) Create(ctx context.Context, req *dto.CustomerAddRequest, userID, orgID string) (*entity.Customer, error) {
    customer := &entity.Customer{
        Name:           req.Name,
        Owner:          userID,
        OrganizationID: orgID,
        InSharedPool:   false,
    }
    
    // 填充动态字段
    if req.CustomFields != nil {
        customer.CustomFields = req.CustomFields
    }
    
    err := s.repo.Create(ctx, customer)
    if err != nil {
        return nil, err
    }
    
    // 记录操作日志
    s.logOperation(ctx, "create", customer.ID, userID)
    
    return customer, nil
}

// hasReadPermission 检查读权限
func (s *CustomerService) hasReadPermission(c *entity.Customer, userID, orgID string) bool {
    // 实现权限逻辑：负责人、协作人、上级等
    return c.Owner == userID || c.OrganizationID == orgID
}
```

### 5.3 客户管理模块 - Repository
```go
// internal/repository/customer/customer_repository.go
package customer

import (
    "context"
    "gorm.io/gorm"
    "crm/internal/model/entity"
)

type CustomerRepository struct {
    db *gorm.DB
}

func NewCustomerRepository(db *gorm.DB) *CustomerRepository {
    return &CustomerRepository{db: db}
}

func (r *CustomerRepository) DB() *gorm.DB {
    return r.db
}

func (r *CustomerRepository) Query(ctx context.Context) *gorm.DB {
    return r.db.WithContext(ctx).Model(&entity.Customer{})
}

func (r *CustomerRepository) GetByID(ctx context.Context, id string) (*entity.Customer, error) {
    var customer entity.Customer
    err := r.db.WithContext(ctx).First(&customer, "id = ?", id).Error
    if err != nil {
        return nil, err
    }
    return &customer, nil
}

func (r *CustomerRepository) Create(ctx context.Context, customer *entity.Customer) error {
    return r.db.WithContext(ctx).Create(customer).Error
}

func (r *CustomerRepository) Update(ctx context.Context, customer *entity.Customer) error {
    return r.db.WithContext(ctx).Save(customer).Error
}

func (r *CustomerRepository) Delete(ctx context.Context, id string) error {
    return r.db.WithContext(ctx).Delete(&entity.Customer{}, "id = ?", id).Error
}

// BatchTransfer 批量转移客户负责人
func (r *CustomerRepository) BatchTransfer(ctx context.Context, ids []string, newOwner string) error {
    return r.db.WithContext(ctx).
        Model(&entity.Customer{}).
        Where("id IN ?", ids).
        Update("owner", newOwner).Error
}
```

### 5.4 数据模型定义
```go
// internal/model/entity/customer.go
package entity

import (
    "time"
    "gorm.io/gorm"
)

// Customer 客户实体
type Customer struct {
    ID             string         `gorm:"type:varchar(64);primaryKey" json:"id"`
    Name           string         `gorm:"type:varchar(255);not null;comment:客户名称" json:"name"`
    Owner          string         `gorm:"type:varchar(64);index;comment:负责人" json:"owner"`
    CollectionTime *time.Time     `gorm:"comment:领取时间" json:"collection_time"`
    PoolID         *string        `gorm:"type:varchar(64);index;comment:公海 ID" json:"pool_id"`
    InSharedPool   bool           `gorm:"default:false;comment:是否在公海池" json:"in_shared_pool"`
    OrganizationID string         `gorm:"type:varchar(64);index;not null;comment:组织 ID" json:"organization_id"`
    Follower       *string        `gorm:"type:varchar(64);comment:最新跟进人" json:"follower"`
    FollowTime     *time.Time     `gorm:"comment:最新跟进时间" json:"follow_time"`
    ReasonID       *string        `gorm:"type:varchar(64);comment:公海原因 ID" json:"reason_id"`
    CustomFields   gorm.JSON      `gorm:"comment:动态字段" json:"custom_fields,omitempty"`
    CreatedAt      time.Time      `gorm:"autoCreateTime" json:"created_at"`
    UpdatedAt      time.Time      `gorm:"autoUpdateTime" json:"updated_at"`
    DeletedAt      gorm.DeletedAt `gorm:"index" json:"-"`
}

func (Customer) TableName() string {
    return "customer"
}
```

### 5.5 DTO 定义
```go
// internal/model/dto/customer_dto.go
package dto

import "crm/internal/model/entity"

// CustomerPageRequest 客户分页请求
type CustomerPageRequest struct {
    BasePageRequest
    Name       string `json:"name"`
    Owner      string `json:"owner"`
    ViewID     string `json:"view_id"`
    PoolID     string `json:"pool_id"`
}

// CustomerListResponse 客户列表响应
type CustomerListResponse struct {
    ID             string     `json:"id"`
    Name           string     `json:"name"`
    Owner          string     `json:"owner"`
    OwnerName      string     `json:"owner_name"`
    CollectionTime *time.Time `json:"collection_time"`
    InSharedPool   bool       `json:"in_shared_pool"`
    Follower       *string    `json:"follower"`
    FollowTime     *time.Time `json:"follow_time"`
    CreatedAt      time.Time  `json:"created_at"`
}

// CustomerAddRequest 客户新增请求
type CustomerAddRequest struct {
    Name         string                 `json:"name" binding:"required,min=2,max=255"`
    CustomFields map[string]interface{} `json:"custom_fields"`
}

// ToCustomerListResponse 实体转列表 DTO
func ToCustomerListResponse(c *entity.Customer) CustomerListResponse {
    return CustomerListResponse{
        ID:             c.ID,
        Name:           c.Name,
        Owner:          c.Owner,
        CollectionTime: c.CollectionTime,
        InSharedPool:   c.InSharedPool,
        Follower:       c.Follower,
        FollowTime:     c.FollowTime,
        CreatedAt:      c.CreatedAt,
    }
}
```

### 5.6 路由注册
```go
// internal/handler/router.go
package handler

import (
    "github.com/gin-gonic/gin"
    "crm/internal/handler/customer"
    "crm/internal/middleware"
)

func SetupRouter(r *gin.Engine, handlers *Handlers) {
    // API v1
    v1 := r.Group("/api/v1")
    {
        // 认证中间件
        v1.Use(middleware.AuthMiddleware())
        
        // 客户管理
        account := v1.Group("/account")
        {
            account.POST("/page", 
                middleware.PermissionMiddleware("customer:read"),
                handlers.Customer.List)
            
            account.GET("/get/:id", 
                middleware.PermissionMiddleware("customer:read"),
                handlers.Customer.Get)
            
            account.POST("/add", 
                middleware.PermissionMiddleware("customer:add"),
                middleware.OperationLogMiddleware("customer", "create"),
                handlers.Customer.Create)
            
            account.POST("/update", 
                middleware.PermissionMiddleware("customer:update"),
                middleware.OperationLogMiddleware("customer", "update"),
                handlers.Customer.Update)
            
            account.GET("/delete/:id", 
                middleware.PermissionMiddleware("customer:delete"),
                middleware.OperationLogMiddleware("customer", "delete"),
                handlers.Customer.Delete)
            
            account.POST("/batch/transfer", 
                middleware.PermissionMiddleware("customer:transfer"),
                handlers.Customer.BatchTransfer)
        }
    }
}
```

### 5.7 主程序入口
```go
// cmd/crm-server/main.go
package main

import (
    "context"
    "log"
    "net/http"
    "os"
    "os/signal"
    "syscall"
    "time"
    
    "github.com/gin-gonic/gin"
    "gorm.io/gorm"
    "crm/internal/config"
    "crm/internal/handler"
    "crm/internal/repository/customer"
    "crm/internal/service/customer"
    "crm/internal/middleware/datascop"
)

func main() {
    // 加载配置
    cfg := config.Load()
    
    // 初始化数据库
    db := initDatabase(cfg.Database)
    
    // 初始化仓库
    customerRepo := customer.NewCustomerRepository(db)
    
    // 初始化服务
    dataScopeSvc := datascop.NewDataScopeService(db)
    customerSvc := customer.NewCustomerService(customerRepo, dataScopeSvc)
    
    // 初始化 Handler
    handlers := &handler.Handlers{
        Customer: handler.NewCustomerHandler(customerSvc),
    }
    
    // 设置 Gin 模式
    if cfg.Mode == "release" {
        gin.SetMode(gin.ReleaseMode)
    }
    
    // 创建路由
    r := gin.Default()
    handler.SetupRouter(r, handlers)
    
    // 健康检查
    r.GET("/health", func(c *gin.Context) {
        c.JSON(200, gin.H{"status": "ok"})
    })
    
    // 启动服务器
    srv := &http.Server{
        Addr:    ":" + cfg.Server.Port,
        Handler: r,
    }
    
    go func() {
        log.Printf("Server starting on port %s", cfg.Server.Port)
        if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
            log.Fatalf("Failed to start server: %v", err)
        }
    }()
    
    // 优雅关闭
    quit := make(chan os.Signal, 1)
    signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
    <-quit
    
    log.Println("Shutting down server...")
    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()
    
    if err := srv.Shutdown(ctx); err != nil {
        log.Fatalf("Server forced to shutdown: %v", err)
    }
    
    log.Println("Server exited")
}

func initDatabase(cfg config.DatabaseConfig) *gorm.DB {
    // 实现数据库初始化逻辑
    // 支持 MySQL/PostgreSQL
}
```

## 六、实施计划

### 阶段一：基础框架搭建 (2 周)
- [ ] 项目初始化和依赖配置
- [ ] 数据库连接和 GORM 配置
- [ ] 用户认证和权限框架 (Casbin)
- [ ] 日志和监控体系
- [ ] 配置文件管理

### 阶段二：核心模块迁移 (6-8 周)
- [ ] 客户管理模块 (Customer)
- [ ] 联系人管理模块 (Customer Contact)
- [ ] 公海池管理模块 (Customer Pool)
- [ ] 数据权限和权限控制
- [ ] 操作日志系统

### 阶段三：业务模块迁移 (6-8 周)
- [ ] 商机管理模块 (Opportunity)
- [ ] 合同管理模块 (Contract)
- [ ] 订单管理模块 (Order)
- [ ] 审批流程模块 (Approval)
- [ ] 报表和图表模块 (Dashboard)

### 阶段四：测试和优化 (2-3 周)
- [ ] 单元测试覆盖 (>80%)
- [ ] 集成测试
- [ ] 性能测试和优化
- [ ] 安全审计
- [ ] 文档完善

## 七、预期收益

### 7.1 性能提升
- 内存占用降低 90%
- 启动速度提升 10 倍
- 并发处理能力提高 3-5 倍
- GC 停顿时间减少 98%

### 7.2 运维简化
- 单二进制文件部署
- 无需 JVM 调优
- 更低的容器资源需求
- 更快的 CI/CD 流水线

### 7.3 开发效率
- 更简洁的代码 (减少约 40% 代码量)
- 内置并发支持
- 强大的标准库
- 快速的编译速度

## 八、风险和挑战

### 8.1 技术风险
- Go 生态系统相对 Java 较小
- 某些企业级功能需要自行实现
- 团队学习曲线

### 8.2 应对措施
- 选择成熟的开源组件
- 分阶段迁移，降低风险
- 充分的培训和文档
- 保留 Java 版本作为备份

## 九、下一步行动

1. **确认技术选型** - 确认 Go 版本和核心依赖库
2. **搭建基础框架** - 创建项目结构和配置文件
3. **试点模块开发** - 选择客户管理模块作为试点
4. **性能基准测试** - 建立性能对比基准
5. **团队培训** - Go 语言和最佳实践培训
