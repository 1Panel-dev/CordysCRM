package main

import (
	"context"
	"fmt"
	"log"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/gin-gonic/gin"
	"go.uber.org/zap"
	"gorm.io/gorm"

	"crm/internal/config"
	"crm/internal/handler"
	"crm/internal/handler/customer"
	"crm/internal/middleware"
	"crm/internal/repository/customer"
	customerservice "crm/internal/service/customer"
	"crm/internal/middleware/datascop"
)

var logger *zap.Logger

func main() {
	// 初始化日志
	var err error
	logger, err = zap.NewProduction()
	if err != nil {
		log.Fatalf("Failed to initialize logger: %v", err)
	}
	defer logger.Sync()

	logger.Info("Starting CRM Server...")

	// 加载配置
	cfg := config.Load()
	logger.Info("Configuration loaded", zap.String("mode", cfg.Mode))

	// 初始化数据库
	db, err := initDatabase(cfg.Database)
	if err != nil {
		logger.Fatal("Failed to initialize database", zap.Error(err))
	}
	logger.Info("Database connected")

	// 自动迁移
	if cfg.Database.AutoMigrate {
		if err := autoMigrate(db); err != nil {
			logger.Fatal("Failed to auto migrate", zap.Error(err))
		}
		logger.Info("Database migration completed")
	}

	// 初始化仓库
	customerRepo := customerrepo.NewCustomerRepository(db)

	// 初始化服务
	dataScopeSvc := datascop.NewDataScopeService(db)
	customerSvc := customerservice.NewCustomerService(customerRepo, dataScopeSvc)

	// 初始化 Handler
	handlers := &handler.Handlers{
		Customer: customer.NewCustomerHandler(customerSvc),
	}

	// 初始化 Casbin 权限
	enforcer, err := middleware.InitCasbin(db)
	if err != nil {
		logger.Fatal("Failed to initialize casbin", zap.Error(err))
	}

	// 设置 Gin 模式
	if cfg.Mode == "release" {
		gin.SetMode(gin.ReleaseMode)
	}

	// 创建路由
	r := gin.Default()

	// 注册中间件
	r.Use(middleware.LoggerMiddleware(logger))
	r.Use(middleware.RecoveryMiddleware(logger))
	r.Use(middleware.CorsMiddleware())

	// 注入 Casbin
	r.Use(func(c *gin.Context) {
		c.Set("casbin", enforcer)
		c.Next()
	})

	// 设置路由
	handler.SetupRouter(r, handlers)

	// Swagger 文档
	if cfg.Mode != "release" {
		r.GET("/swagger/*any", ginSwagger.WrapHandler(swaggerFiles.Handler))
	}

	// 健康检查
	r.GET("/health", func(c *gin.Context) {
		c.JSON(http.StatusOK, gin.H{
			"status":    "ok",
			"timestamp": time.Now().Format(time.RFC3339),
		})
	})

	// 启动服务器
	srv := &http.Server{
		Addr:         ":" + cfg.Server.Port,
		Handler:      r,
		ReadTimeout:  time.Duration(cfg.Server.ReadTimeout) * time.Second,
		WriteTimeout: time.Duration(cfg.Server.WriteTimeout) * time.Second,
		IdleTimeout:  time.Duration(cfg.Server.IdleTimeout) * time.Second,
	}

	go func() {
		logger.Info("Server starting", zap.String("port", cfg.Server.Port))
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			logger.Fatal("Failed to start server", zap.Error(err))
		}
	}()

	// 优雅关闭
	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
	<-quit

	logger.Info("Shutting down server...")
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	if err := srv.Shutdown(ctx); err != nil {
		logger.Fatal("Server forced to shutdown", zap.Error(err))
	}

	logger.Info("Server exited")
}

func initDatabase(cfg config.DatabaseConfig) (*gorm.DB, error) {
	var dsn string
	var dialector gorm.Dialector

	switch cfg.Type {
	case "mysql":
		dsn = fmt.Sprintf("%s:%s@tcp(%s:%s)/%s?charset=%s&parseTime=True&loc=Local",
			cfg.Username,
			cfg.Password,
			cfg.Host,
			cfg.Port,
			cfg.Database,
			cfg.Charset,
		)
		dialector = mysql.Open(dsn)
	case "postgres":
		dsn = fmt.Sprintf("host=%s port=%s user=%s password=%s dbname=%s sslmode=disable TimeZone=UTC",
			cfg.Host,
			cfg.Port,
			cfg.Username,
			cfg.Password,
			cfg.Database,
		)
		dialector = postgres.Open(dsn)
	default:
		return nil, fmt.Errorf("unsupported database type: %s", cfg.Type)
	}

	db, err := gorm.Open(dialector, &gorm.Config{
		Logger: loggerwriter.NewLogger(logger),
	})
	if err != nil {
		return nil, err
	}

	// 连接池配置
	sqlDB, err := db.DB()
	if err != nil {
		return nil, err
	}

	sqlDB.SetMaxIdleConns(cfg.MaxIdleConns)
	sqlDB.SetMaxOpenConns(cfg.MaxOpenConns)
	sqlDB.SetConnMaxLifetime(time.Duration(cfg.ConnMaxLifetime) * time.Second)

	return db, nil
}

func autoMigrate(db *gorm.DB) error {
	return db.AutoMigrate(
		&entity.Customer{},
		&entity.CustomerContact{},
		&entity.CustomerPool{},
		&entity.Opportunity{},
		&entity.Contract{},
		&entity.Order{},
		// 添加其他实体
	)
}
