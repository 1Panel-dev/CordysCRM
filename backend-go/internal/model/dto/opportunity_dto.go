package dto

import "time"

// OpportunityListQuery 商机列表查询参数
type OpportunityListQuery struct {
	Page         int       `form:"page" binding:"min=1"`
	PageSize     int       `form:"page_size" binding:"min=1,max=100"`
	Name         string    `form:"name"`
	CustomerID   uint      `form:"customer_id"`
	OwnerID      uint      `form:"owner_id"`
	Stage        int       `form:"stage"`
	Status       int       `form:"status"`
	StartDate    *time.Time `form:"start_date"`
	EndDate      *time.Time `form:"end_date"`
}

// CreateOpportunityRequest 创建商机请求
type CreateOpportunityRequest struct {
	Name           string  `json:"name" binding:"required,max=100"`
	CustomerID     uint    `json:"customer_id" binding:"required"`
	Amount         float64 `json:"amount"`
	Stage          int     `json:"stage" binding:"min=1,max=6"`
	Source         string  `json:"source"`
	Probability    int     `json:"probability" binding:"min=0,max=100"`
	ExpectedDate   string  `json:"expected_date"` // RFC3339 format
	Description    string  `json:"description"`
	NextStep       string  `json:"next_step"`
}

// UpdateOpportunityRequest 更新商机请求
type UpdateOpportunityRequest struct {
	Name           string  `json:"name" binding:"omitempty,max=100"`
	Amount         float64 `json:"amount"`
	Stage          int     `json:"stage" binding:"omitempty,min=1,max=6"`
	Source         string  `json:"source"`
	Probability    int     `json:"probability" binding:"omitempty,min=0,max=100"`
	ExpectedDate   string  `json:"expected_date"`
	CloseDate      string  `json:"close_date"`
	Description    string  `json:"description"`
	NextStep       string  `json:"next_step"`
	Status         int     `json:"status" binding:"omitempty,min=1,max=4"`
}

// OpportunityProductRequest 商机产品请求
type OpportunityProductRequest struct {
	ProductID uint    `json:"product_id" binding:"required"`
	Quantity  int     `json:"quantity" binding:"required,min=1"`
	Price     float64 `json:"price" binding:"required,min=0"`
	Discount  float64 `json:"discount"`
}

// ProductListQuery 产品列表查询参数
type ProductListQuery struct {
	Page       int    `form:"page" binding:"min=1"`
	PageSize   int    `form:"page_size" binding:"min=1,max=100"`
	Name       string `form:"name"`
	Code       string `form:"code"`
	CategoryID uint   `form:"category_id"`
	Status     int    `form:"status"`
}

// CreateProductRequest 创建产品请求
type CreateProductRequest struct {
	Name        string  `json:"name" binding:"required,max=100"`
	Code        string  `json:"code" binding:"required,max=50"`
	CategoryID  uint    `json:"category_id"`
	Spec        string  `json:"spec"`
	Unit        string  `json:"unit" binding:"required"`
	Price       float64 `json:"price" binding:"required,min=0"`
	Cost        float64 `json:"cost"`
	Stock       int     `json:"stock"`
	Description string  `json:"description"`
}

// UpdateProductRequest 更新产品请求
type UpdateProductRequest struct {
	Name        string  `json:"name" binding:"omitempty,max=100"`
	Spec        string  `json:"spec"`
	Unit        string  `json:"unit"`
	Price       float64 `json:"price" binding:"omitempty,min=0"`
	Cost        float64 `json:"cost"`
	Stock       int     `json:"stock"`
	Status      int     `json:"status"`
	Description string  `json:"description"`
}

// ContractListQuery 合同列表查询参数
type ContractListQuery struct {
	Page         int       `form:"page" binding:"min=1"`
	PageSize     int       `form:"page_size" binding:"min=1,max=100"`
	ContractNo   string    `form:"contract_no"`
	Name         string    `form:"name"`
	CustomerID   uint      `form:"customer_id"`
	OwnerID      uint      `form:"owner_id"`
	Status       int       `form:"status"`
	StartDate    *time.Time `form:"start_date"`
	EndDate      *time.Time `form:"end_date"`
}

// CreateContractRequest 创建合同请求
type CreateContractRequest struct {
	Name           string  `json:"name" binding:"required,max=100"`
	CustomerID     uint    `json:"customer_id" binding:"required"`
	OpportunityID  uint    `json:"opportunity_id"`
	Amount         float64 `json:"amount" binding:"required,min=0"`
	SignedDate     string  `json:"signed_date"`
	StartDate      string  `json:"start_date"`
	EndDate        string  `json:"end_date"`
	Type           int     `json:"type" binding:"min=1,max=3"`
	PaymentTerms   string  `json:"payment_terms"`
	Remark         string  `json:"remark"`
}

// UpdateContractRequest 更新合同请求
type UpdateContractRequest struct {
	Name           string  `json:"name" binding:"omitempty,max=100"`
	Amount         float64 `json:"amount"`
	SignedDate     string  `json:"signed_date"`
	StartDate      string  `json:"start_date"`
	EndDate        string  `json:"end_date"`
	Status         int     `json:"status"`
	Type           int     `json:"type"`
	PaymentTerms   string  `json:"payment_terms"`
	Remark         string  `json:"remark"`
}

// OrderListQuery 订单列表查询参数
type OrderListQuery struct {
	Page         int       `form:"page" binding:"min=1"`
	PageSize     int       `form:"page_size" binding:"min=1,max=100"`
	OrderNo      string    `form:"order_no"`
	ContractID   uint      `form:"contract_id"`
	CustomerID   uint      `form:"customer_id"`
	OwnerID      uint      `form:"owner_id"`
	Status       int       `form:"status"`
	StartDate    *time.Time `form:"start_date"`
	EndDate      *time.Time `form:"end_date"`
}

// CreateOrderRequest 创建订单请求
type CreateOrderRequest struct {
	ContractID   uint              `json:"contract_id" binding:"required"`
	CustomerID   uint              `json:"customer_id" binding:"required"`
	Amount       float64           `json:"amount" binding:"required,min=0"`
	Items        []OrderItemRequest `json:"items" binding:"required,min=1"`
	DeliveryDate string            `json:"delivery_date"`
	Remark       string            `json:"remark"`
}

// OrderItemRequest 订单明细请求
type OrderItemRequest struct {
	ProductID uint    `json:"product_id" binding:"required"`
	Quantity  int     `json:"quantity" binding:"required,min=1"`
	Price     float64 `json:"price" binding:"required,min=0"`
}

// UpdateOrderRequest 更新订单请求
type UpdateOrderRequest struct {
	Amount       float64 `json:"amount"`
	Status       int     `json:"status" binding:"omitempty,min=1,max=6"`
	PaymentDate  string  `json:"payment_date"`
	DeliveryDate string  `json:"delivery_date"`
	Remark       string  `json:"remark"`
}

// PaymentListQuery 回款列表查询参数
type PaymentListQuery struct {
	Page        int       `form:"page" binding:"min=1"`
	PageSize    int       `form:"page_size" binding:"min=1,max=100"`
	OrderID     uint      `form:"order_id"`
	PaymentNo   string    `form:"payment_no"`
	StartDate   *time.Time `form:"start_date"`
	EndDate     *time.Time `form:"end_date"`
}

// CreatePaymentRequest 创建回款请求
type CreatePaymentRequest struct {
	OrderID       uint    `json:"order_id" binding:"required"`
	Amount        float64 `json:"amount" binding:"required,min=0"`
	PaymentDate   string  `json:"payment_date" binding:"required"`
	PaymentMethod int     `json:"payment_method" binding:"min=1,max=4"`
	BankAccount   string  `json:"bank_account"`
	Remark        string  `json:"remark"`
}

// UpdatePaymentRequest 更新回款请求
type UpdatePaymentRequest struct {
	Amount        float64 `json:"amount"`
	PaymentDate   string  `json:"payment_date"`
	PaymentMethod int     `json:"payment_method"`
	BankAccount   string  `json:"bank_account"`
	Remark        string  `json:"remark"`
}

// FollowUpListQuery 跟进记录查询参数
type FollowUpListQuery struct {
	Page         int       `form:"page" binding:"min=1"`
	PageSize     int       `form:"page_size" binding:"min=1,max=100"`
	BusinessType int       `form:"business_type"`
	BusinessID   uint      `form:"business_id"`
	OwnerID      uint      `form:"owner_id"`
	StartDate    *time.Time `form:"start_date"`
	EndDate      *time.Time `form:"end_date"`
}

// CreateFollowUpRequest 创建跟进记录请求
type CreateFollowUpRequest struct {
	BusinessType int    `json:"business_type" binding:"required,min=1,max=3"`
	BusinessID   uint   `json:"business_id" binding:"required"`
	Content      string `json:"content" binding:"required"`
	NextTime     string `json:"next_time"`
	Attachment   string `json:"attachment"`
}

// UpdateFollowUpRequest 更新跟进记录请求
type UpdateFollowUpRequest struct {
	Content    string `json:"content"`
	NextTime   string `json:"next_time"`
	Attachment string `json:"attachment"`
}

// PublicPoolListQuery 公海池查询参数
type PublicPoolListQuery struct {
	Page         int       `form:"page" binding:"min=1"`
	PageSize     int       `form:"page_size" binding:"min=1,max=100"`
	CustomerName string    `form:"customer_name"`
	Reason       int       `form:"reason"`
	Status       int       `form:"status"`
}

// ClaimCustomerRequest 领取客户请求
type ClaimCustomerRequest struct {
	CustomerID uint `json:"customer_id" binding:"required"`
}

// ReleaseCustomerRequest 释放客户请求
type ReleaseCustomerRequest struct {
	CustomerID uint   `json:"customer_id" binding:"required"`
	Reason     int    `json:"reason" binding:"required,min=1,max=3"`
	Remark     string `json:"remark"`
}
