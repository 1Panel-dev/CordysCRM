package handler

import (
	"github.com/gin-gonic/gin"
	"crm/internal/middleware"
	"crm/internal/handler/customer"
)

// SetupRouter 设置路由
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

			account.POST("/batch/delete",
				middleware.PermissionMiddleware("customer:delete"),
				handlers.Customer.BatchDelete)
		}

		// 联系人管理
		// contact := v1.Group("/contact")
		// {
		//     contact.POST("/page", handlers.Contact.List)
		//     ...
		// }

		// 商机管理
		// opportunity := v1.Group("/opportunity")
		// {
		//     opportunity.POST("/page", handlers.Opportunity.List)
		//     ...
		// }

		// 合同管理
		// contract := v1.Group("/contract")
		// {
		//     contract.POST("/page", handlers.Contract.List)
		//     ...
		// }

		// 订单管理
		// order := v1.Group("/order")
		// {
		//     order.POST("/page", handlers.Order.List)
		//     ...
		// }
	}
}
