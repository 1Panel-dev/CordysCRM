package customer

import (
	"github.com/gin-gonic/gin"
	"crm/internal/model/dto"
	"crm/internal/service/customer"
	"crm/internal/pkg/response"
)

// CustomerHandler 客户处理器
type CustomerHandler struct {
	service *customer.CustomerService
}

// NewCustomerHandler 创建客户处理器实例
func NewCustomerHandler(svc *customer.CustomerService) *CustomerHandler {
	return &CustomerHandler{service: svc}
}

// List 客户列表
// @Summary 客户列表
// @Tags 客户
// @Accept json
// @Produce json
// @Param request body dto.CustomerPageRequest true "查询参数"
// @Success 200 {object} response.Response[dto.Pager[dto.CustomerListResponse]]
// @Router /account/page [post]
func (h *CustomerHandler) List(c *gin.Context) {
	var req dto.CustomerPageRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		response.Error(c, response.ErrInvalidParams.WithError(err))
		return
	}

	userID := GetUserID(c)
	orgID := GetOrgID(c)

	result, err := h.service.List(c.Request.Context(), &req, userID, orgID)
	if err != nil {
		response.Error(c, response.ErrInternalServer.WithError(err))
		return
	}

	response.Success(c, result)
}

// Get 客户详情
// @Summary 客户详情
// @Tags 客户
// @Accept json
// @Produce json
// @Param id path string true "客户 ID"
// @Success 200 {object} response.Response[dto.CustomerGetResponse]
// @Router /account/get/{id} [get]
func (h *CustomerHandler) Get(c *gin.Context) {
	id := c.Param("id")
	if id == "" {
		response.Error(c, response.ErrInvalidParams.WithMessage("id is required"))
		return
	}

	userID := GetUserID(c)
	orgID := GetOrgID(c)

	result, err := h.service.GetWithDataPermission(c.Request.Context(), id, userID, orgID)
	if err != nil {
		if err.Error() == "customer not found" {
			response.Error(c, response.ErrNotFound)
			return
		}
		if err.Error() == "permission denied" {
			response.Error(c, response.ErrPermissionDenied)
			return
		}
		response.Error(c, response.ErrInternalServer.WithError(err))
		return
	}

	response.Success(c, result)
}

// Create 创建客户
// @Summary 创建客户
// @Tags 客户
// @Accept json
// @Produce json
// @Param request body dto.CustomerAddRequest true "客户信息"
// @Success 200 {object} response.Response[entity.Customer]
// @Router /account/add [post]
func (h *CustomerHandler) Create(c *gin.Context) {
	var req dto.CustomerAddRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		response.Error(c, response.ErrInvalidParams.WithError(err))
		return
	}

	userID := GetUserID(c)
	orgID := GetOrgID(c)

	result, err := h.service.Create(c.Request.Context(), &req, userID, orgID)
	if err != nil {
		response.Error(c, response.ErrInternalServer.WithError(err))
		return
	}

	response.Success(c, result)
}

// Update 更新客户
// @Summary 更新客户
// @Tags 客户
// @Accept json
// @Produce json
// @Param request body dto.CustomerUpdateRequest true "客户信息"
// @Success 200 {object} response.Response[entity.Customer]
// @Router /account/update [post]
func (h *CustomerHandler) Update(c *gin.Context) {
	var req dto.CustomerUpdateRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		response.Error(c, response.ErrInvalidParams.WithError(err))
		return
	}

	userID := GetUserID(c)
	orgID := GetOrgID(c)

	result, err := h.service.Update(c.Request.Context(), &req, userID, orgID)
	if err != nil {
		if err.Error() == "customer not found" {
			response.Error(c, response.ErrNotFound)
			return
		}
		if err.Error() == "permission denied" {
			response.Error(c, response.ErrPermissionDenied)
			return
		}
		response.Error(c, response.ErrInternalServer.WithError(err))
		return
	}

	response.Success(c, result)
}

// Delete 删除客户
// @Summary 删除客户
// @Tags 客户
// @Accept json
// @Produce json
// @Param id path string true "客户 ID"
// @Success 200 {object} response.Response[interface{}]
// @Router /account/delete/{id} [get]
func (h *CustomerHandler) Delete(c *gin.Context) {
	id := c.Param("id")
	if id == "" {
		response.Error(c, response.ErrInvalidParams.WithMessage("id is required"))
		return
	}

	userID := GetUserID(c)
	orgID := GetOrgID(c)

	err := h.service.Delete(c.Request.Context(), id, userID, orgID)
	if err != nil {
		if err.Error() == "customer not found" {
			response.Error(c, response.ErrNotFound)
			return
		}
		if err.Error() == "permission denied" {
			response.Error(c, response.ErrPermissionDenied)
			return
		}
		response.Error(c, response.ErrInternalServer.WithError(err))
		return
	}

	response.Success(c, nil)
}

// BatchTransfer 批量转移客户
// @Summary 批量转移客户
// @Tags 客户
// @Accept json
// @Produce json
// @Param request body dto.CustomerBatchTransferRequest true "转移请求"
// @Success 200 {object} response.Response[interface{}]
// @Router /account/batch/transfer [post]
func (h *CustomerHandler) BatchTransfer(c *gin.Context) {
	var req dto.CustomerBatchTransferRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		response.Error(c, response.ErrInvalidParams.WithError(err))
		return
	}

	userID := GetUserID(c)
	orgID := GetOrgID(c)

	err := h.service.BatchTransfer(c.Request.Context(), &req, userID, orgID)
	if err != nil {
		response.Error(c, response.ErrInternalServer.WithError(err))
		return
	}

	response.Success(c, nil)
}

// GetUserID 从上下文获取用户 ID
func GetUserID(c *gin.Context) string {
	if uid, exists := c.Get("user_id"); exists {
		if str, ok := uid.(string); ok {
			return str
		}
	}
	return ""
}

// GetOrgID 从上下文获取组织 ID
func GetOrgID(c *gin.Context) string {
	if oid, exists := c.Get("org_id"); exists {
		if str, ok := oid.(string); ok {
			return str
		}
	}
	return ""
}
