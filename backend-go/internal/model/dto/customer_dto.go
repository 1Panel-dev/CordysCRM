package dto

import (
	"time"

	"crm/internal/model/entity"
)

// BasePageRequest 基础分页请求
type BasePageRequest struct {
	Current  int    `json:"current"`
	PageSize int    `json:"page_size"`
}

func (r *BasePageRequest) GetOffset() int {
	if r.Current <= 0 {
		r.Current = 1
	}
	if r.PageSize <= 0 {
		r.PageSize = 10
	}
	return (r.Current - 1) * r.PageSize
}

// CustomerPageRequest 客户分页请求
type CustomerPageRequest struct {
	BasePageRequest
	Name       string `json:"name"`
	Owner      string `json:"owner"`
	ViewID     string `json:"view_id"`
	PoolID     string `json:"pool_id"`
	InPool     *bool  `json:"in_pool"`
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

// CustomerGetResponse 客户详情响应
type CustomerGetResponse struct {
	CustomerListResponse
	CustomFields map[string]interface{} `json:"custom_fields,omitempty"`
	ReasonName   string                 `json:"reason_name"`
}

// CustomerAddRequest 客户新增请求
type CustomerAddRequest struct {
	Name         string                 `json:"name" binding:"required,min=2,max=255"`
	CustomFields map[string]interface{} `json:"custom_fields"`
}

// CustomerUpdateRequest 客户更新请求
type CustomerUpdateRequest struct {
	ID           string                 `json:"id" binding:"required"`
	Name         string                 `json:"name" binding:"omitempty,min=2,max=255"`
	CustomFields map[string]interface{} `json:"custom_fields"`
}

// CustomerBatchTransferRequest 批量转移请求
type CustomerBatchTransferRequest struct {
	IDs       []string `json:"ids" binding:"required,min=1"`
	NewOwner  string   `json:"new_owner" binding:"required"`
	ReasonID  *string  `json:"reason_id"`
}

// Pager 分页响应
type Pager[T any] struct {
	List     []T   `json:"list"`
	Total    int64 `json:"total"`
	Current  int   `json:"current"`
	PageSize int   `json:"page_size"`
}

// ToCustomerListResponse 实体转列表 DTO
func ToCustomerListResponse(c *entity.Customer) CustomerListResponse {
	resp := CustomerListResponse{
		ID:             c.ID,
		Name:           c.Name,
		Owner:          c.Owner,
		CollectionTime: c.CollectionTime,
		InSharedPool:   c.InSharedPool,
		Follower:       c.Follower,
		FollowTime:     c.FollowTime,
		CreatedAt:      c.CreatedAt,
	}
	return resp
}

// ToCustomerGetResponse 实体转详情 DTO
func ToCustomerGetResponse(c *entity.Customer) CustomerGetResponse {
	return CustomerGetResponse{
		CustomerListResponse: ToCustomerListResponse(c),
		CustomFields:         c.CustomFields,
	}
}
