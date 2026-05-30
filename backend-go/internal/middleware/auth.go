package middleware

import (
	"time"

	"github.com/gin-gonic/gin"
	"github.com/golang-jwt/jwt/v5"
	"go.uber.org/zap"
)

// AuthMiddleware JWT 认证中间件
func AuthMiddleware() gin.HandlerFunc {
	return func(c *gin.Context) {
		// 从 Header 获取 token
		tokenString := c.GetHeader("Authorization")
		if tokenString == "" {
			c.AbortWithStatusJSON(401, gin.H{"error": "missing authorization header"})
			return
		}

		// 去掉 Bearer 前缀
		if len(tokenString) > 7 && tokenString[:7] == "Bearer " {
			tokenString = tokenString[7:]
		}

		// 解析 token
		token, err := jwt.Parse(tokenString, func(token *jwt.Token) (interface{}, error) {
			return []byte(getJWTSecret()), nil
		})

		if err != nil || !token.Valid {
			c.AbortWithStatusJSON(401, gin.H{"error": "invalid token"})
			return
		}

		// 提取 claims
		if claims, ok := token.Claims.(jwt.MapClaims); ok {
			userID := claims["user_id"].(string)
			orgID := claims["org_id"].(string)
			
			// 将用户信息存入上下文
			c.Set("user_id", userID)
			c.Set("org_id", orgID)
			c.Next()
		} else {
			c.AbortWithStatusJSON(401, gin.H{"error": "invalid token claims"})
		}
	}
}

// PermissionMiddleware 权限检查中间件
func PermissionMiddleware(permission string) gin.HandlerFunc {
	return func(c *gin.Context) {
		// 从上下文获取 Casbin enforcer
		enforcer, exists := c.Get("casbin")
		if !exists {
			c.AbortWithStatusJSON(500, gin.H{"error": "casbin not initialized"})
			return
		}

		e := enforcer.(*casbin.Enforcer)
		userID := c.GetString("user_id")

		allowed, err := e.Enforce(userID, permission)
		if err != nil {
			c.AbortWithStatusJSON(500, gin.H{"error": "permission check failed"})
			return
		}

		if !allowed {
			c.AbortWithStatusJSON(403, gin.H{"error": "permission denied"})
			return
		}

		c.Next()
	}
}

// OperationLogMiddleware 操作日志中间件
func OperationLogMiddleware(module, action string) gin.HandlerFunc {
	return func(c *gin.Context) {
		// 记录请求前信息
		startTime := time.Now()
		
		c.Next()
		
		// 记录响应后信息
		duration := time.Since(startTime)
		
		// TODO: 异步记录操作日志到数据库
		// logService.AsyncLog(&OperationLog{
		//     UserID:     c.GetString("user_id"),
		//     Module:     module,
		//     Action:     action,
		//     Method:     c.Request.Method,
		//     Path:       c.Request.URL.Path,
		//     Status:     c.Writer.Status(),
		//     Duration:   duration.Milliseconds(),
		//     IP:         c.ClientIP(),
		//     UserAgent:  c.Request.UserAgent(),
		// })
	}
}

// LoggerMiddleware 日志中间件
func LoggerMiddleware(logger *zap.Logger) gin.HandlerFunc {
	return func(c *gin.Context) {
		start := time.Now()
		path := c.Request.URL.Path
		
		c.Next()
		
		latency := time.Since(start)
		status := c.Writer.Status()
		
		logger.Info("HTTP request",
			zap.Int("status", status),
			zap.String("method", c.Request.Method),
			zap.String("path", path),
			zap.String("ip", c.ClientIP()),
			zap.Duration("latency", latency),
		)
	}
}

// RecoveryMiddleware 恢复中间件
func RecoveryMiddleware(logger *zap.Logger) gin.HandlerFunc {
	return func(c *gin.Context) {
		defer func() {
			if err := recover(); err != nil {
				logger.Error("Panic recovered", zap.Any("error", err))
				c.AbortWithStatusJSON(500, gin.H{"error": "internal server error"})
			}
		}()
		c.Next()
	}
}

// CorsMiddleware CORS 中间件
func CorsMiddleware() gin.HandlerFunc {
	return func(c *gin.Context) {
		c.Header("Access-Control-Allow-Origin", "*")
		c.Header("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS")
		c.Header("Access-Control-Allow-Headers", "Origin, Content-Type, Authorization, X-Requested-With")
		c.Header("Access-Control-Max-Age", "86400")
		
		if c.Request.Method == "OPTIONS" {
			c.AbortWithStatus(204)
			return
		}
		
		c.Next()
	}
}

// InitCasbin 初始化 Casbin
func InitCasbin(db *gorm.DB) (*casbin.Enforcer, error) {
	// 使用 GORM 适配器存储策略
	adapter, err := gormadapter.NewAdapterByDB(db)
	if err != nil {
		return nil, err
	}

	// 加载 RBAC 模型
	enforcer, err := casbin.NewEnforcer("./configs/rbac_model.conf", adapter)
	if err != nil {
		return nil, err
	}

	// 加载策略
	if err := enforcer.LoadPolicy(); err != nil {
		return nil, err
	}

	return enforcer, nil
}

// getJWTSecret 获取 JWT 密钥
func getJWTSecret() string {
	// TODO: 从配置中读取
	return "your-secret-key-change-in-production"
}
