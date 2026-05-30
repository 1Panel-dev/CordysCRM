package entity

import (
	"time"
	"database/sql"
)

// Opportunity 商机实体
type Opportunity struct {
	ID             uint           `gorm:"primaryKey" json:"id"`
	Name           string         `gorm:"size:100;not null" json:"name"`
	CustomerID     uint           `gorm:"index;not null" json:"customer_id"`
	OwnerID        uint           `gorm:"index;not null" json:"owner_id"`
	Amount         sql.NullFloat64 `gorm:"type:decimal(15,2)" json:"amount"`
	Stage          int            `gorm:"default:1" json:"stage"` // 1:初步接洽 2:需求分析 3:方案报价 4:谈判合同 5:赢单 6:输单
	Source         string         `gorm:"size:50" json:"source"`   // 来源
	Probability    int            `gorm:"default:0" json:"probability"` // 成功率%
	ExpectedDate   *time.Time     `json:"expected_date"`           // 预计成交日期
	CloseDate      *time.Time     `json:"close_date"`              // 实际成交日期
	Description    string         `gorm:"type:text" json:"description"`
	NextStep       string         `gorm:"size:255" json:"next_step"`
	Status         int            `gorm:"default:1" json:"status"` // 1:进行中 2:已赢单 3:已输单 4:已关闭
	CreatedAt      time.Time      `json:"created_at"`
	UpdatedAt      time.Time      `json:"updated_at"`
	
	// 关联
	Customer       *Customer      `gorm:"foreignKey:CustomerID" json:"customer,omitempty"`
	Owner          *User          `gorm:"foreignKey:OwnerID" json:"owner,omitempty"`
	Products       []OpportunityProduct `gorm:"foreignKey:OpportunityID" json:"products,omitempty"`
	FollowUps      []FollowUp     `gorm:"foreignKey:BusinessID;polymorphic:BusinessType" json:"follow_ups,omitempty"`
}

// TableName 指定表名
func (Opportunity) TableName() string {
	return "crm_opportunity"
}

// OpportunityProduct 商机产品关联
type OpportunityProduct struct {
	ID             uint      `gorm:"primaryKey" json:"id"`
	OpportunityID  uint      `gorm:"index;not null" json:"opportunity_id"`
	ProductID      uint      `gorm:"index;not null" json:"product_id"`
	Quantity       int       `gorm:"default:1" json:"quantity"`
	Price          float64   `gorm:"type:decimal(15,2);not null" json:"price"`
	Discount       float64   `gorm:"default:0" json:"discount"`
	Subtotal       float64   `gorm:"type:decimal(15,2)" json:"subtotal"`
	CreatedAt      time.Time `json:"created_at"`
	UpdatedAt      time.Time `json:"updated_at"`
	
	Product        *Product  `gorm:"foreignKey:ProductID" json:"product,omitempty"`
}

// TableName 指定表名
func (OpportunityProduct) TableName() string {
	return "crm_opportunity_product"
}

// Product 产品实体
type Product struct {
	ID          uint           `gorm:"primaryKey" json:"id"`
	Name        string         `gorm:"size:100;not null" json:"name"`
	Code        string         `gorm:"uniqueIndex;size:50;not null" json:"code"`
	CategoryID  uint           `gorm:"index" json:"category_id"`
	Spec        string         `gorm:"size:255" json:"spec"`
	Unit        string         `gorm:"size:20" json:"unit"`
	Price       float64        `gorm:"type:decimal(15,2);not null" json:"price"`
	Cost        float64        `gorm:"type:decimal(15,2)" json:"cost"`
	Stock       int            `gorm:"default:0" json:"stock"`
	Status      int            `gorm:"default:1" json:"status"` // 1:上架 0:下架
	Description string         `gorm:"type:text" json:"description"`
	CreatedAt   time.Time      `json:"created_at"`
	UpdatedAt   time.Time      `json:"updated_at"`
	
	Category    *ProductCategory `gorm:"foreignKey:CategoryID" json:"category,omitempty"`
}

// TableName 指定表名
func (Product) TableName() string {
	return "crm_product"
}

// ProductCategory 产品分类
type ProductCategory struct {
	ID        uint      `gorm:"primaryKey" json:"id"`
	Name      string    `gorm:"size:50;not null" json:"name"`
	ParentID  uint      `gorm:"index" json:"parent_id"`
	Sort      int       `gorm:"default:0" json:"sort"`
	Status    int       `gorm:"default:1" json:"status"`
	CreatedAt time.Time `json:"created_at"`
	UpdatedAt time.Time `json:"updated_at"`
}

// TableName 指定表名
func (ProductCategory) TableName() string {
	return "crm_product_category"
}

// Contract 合同实体
type Contract struct {
	ID             uint           `gorm:"primaryKey" json:"id"`
	ContractNo     string         `gorm:"uniqueIndex;size:50;not null" json:"contract_no"`
	Name           string         `gorm:"size:100;not null" json:"name"`
	CustomerID     uint           `gorm:"index;not null" json:"customer_id"`
	OpportunityID  uint           `gorm:"index" json:"opportunity_id"`
	OwnerID        uint           `gorm:"index;not null" json:"owner_id"`
	Amount         float64        `gorm:"type:decimal(15,2);not null" json:"amount"`
	SignedDate     *time.Time     `json:"signed_date"`
	StartDate      *time.Time     `json:"start_date"`
	EndDate        *time.Time     `json:"end_date"`
	Status         int            `gorm:"default:1" json:"status"` // 1:草稿 2:审批中 3:已生效 4:已归档 5:已终止
	Type           int            `gorm:"default:1" json:"type"`   // 1:销售合同 2:采购合同 3:其他
	PaymentTerms   string         `gorm:"type:text" json:"payment_terms"`
	Attachment     string         `gorm:"size:255" json:"attachment"`
	Remark         string         `gorm:"type:text" json:"remark"`
	CreatedAt      time.Time      `json:"created_at"`
	UpdatedAt      time.Time      `json:"updated_at"`
	
	Customer       *Customer      `gorm:"foreignKey:CustomerID" json:"customer,omitempty"`
	Opportunity    *Opportunity   `gorm:"foreignKey:OpportunityID" json:"opportunity,omitempty"`
	Owner          *User          `gorm:"foreignKey:OwnerID" json:"owner,omitempty"`
	Orders         []Order        `gorm:"foreignKey:ContractID" json:"orders,omitempty"`
}

// TableName 指定表名
func (Contract) TableName() string {
	return "crm_contract"
}

// Order 订单实体
type Order struct {
	ID             uint           `gorm:"primaryKey" json:"id"`
	OrderNo        string         `gorm:"uniqueIndex;size:50;not null" json:"order_no"`
	ContractID     uint           `gorm:"index;not null" json:"contract_id"`
	CustomerID     uint           `gorm:"index;not null" json:"customer_id"`
	OwnerID        uint           `gorm:"index;not null" json:"owner_id"`
	Amount         float64        `gorm:"type:decimal(15,2);not null" json:"amount"`
	PaidAmount     float64        `gorm:"type:decimal(15,2);default:0" json:"paid_amount"`
	Status         int            `gorm:"default:1" json:"status"` // 1:待付款 2:部分付款 3:已付款 4:已发货 5:已完成 6:已取消
	PaymentDate    *time.Time     `json:"payment_date"`
	DeliveryDate   *time.Time     `json:"delivery_date"`
	Remark         string         `gorm:"type:text" json:"remark"`
	CreatedAt      time.Time      `json:"created_at"`
	UpdatedAt      time.Time      `json:"updated_at"`
	
	Contract       *Contract      `gorm:"foreignKey:ContractID" json:"contract,omitempty"`
	Customer       *Customer      `gorm:"foreignKey:CustomerID" json:"customer,omitempty"`
	Owner          *User          `gorm:"foreignKey:OwnerID" json:"owner,omitempty"`
	Items          []OrderItem    `gorm:"foreignKey:OrderID" json:"items,omitempty"`
	Payments       []Payment      `gorm:"foreignKey:OrderID" json:"payments,omitempty"`
}

// TableName 指定表名
func (Order) TableName() string {
	return "crm_order"
}

// OrderItem 订单明细
type OrderItem struct {
	ID        uint      `gorm:"primaryKey" json:"id"`
	OrderID   uint      `gorm:"index;not null" json:"order_id"`
	ProductID uint      `gorm:"index;not null" json:"product_id"`
	Quantity  int       `gorm:"not null" json:"quantity"`
	Price     float64   `gorm:"type:decimal(15,2);not null" json:"price"`
	Subtotal  float64   `gorm:"type:decimal(15,2)" json:"subtotal"`
	CreatedAt time.Time `json:"created_at"`
	
	Product   *Product  `gorm:"foreignKey:ProductID" json:"product,omitempty"`
}

// TableName 指定表名
func (OrderItem) TableName() string {
	return "crm_order_item"
}

// Payment 回款记录
type Payment struct {
	ID           uint           `gorm:"primaryKey" json:"id"`
	OrderID      uint           `gorm:"index;not null" json:"order_id"`
	PaymentNo    string         `gorm:"uniqueIndex;size:50" json:"payment_no"`
	Amount       float64        `gorm:"type:decimal(15,2);not null" json:"amount"`
	PaymentDate  time.Time      `gorm:"not null" json:"payment_date"`
	PaymentMethod int           `gorm:"default:1" json:"payment_method"` // 1:银行转账 2:支票 3:现金 4:其他
	BankAccount  string         `gorm:"size:100" json:"bank_account"`
	Remark       string         `gorm:"type:text" json:"remark"`
	CreatedAt    time.Time      `json:"created_at"`
	UpdatedAt    time.Time      `json:"updated_at"`
	
	Order        *Order         `gorm:"foreignKey:OrderID" json:"order,omitempty"`
}

// TableName 指定表名
func (Payment) TableName() string {
	return "crm_payment"
}

// FollowUp 跟进记录
type FollowUp struct {
	ID           uint           `gorm:"primaryKey" json:"id"`
	BusinessType int            `gorm:"not null" json:"business_type"` // 1:客户 2:商机 3:合同
	BusinessID   uint           `gorm:"index;not null" json:"business_id"`
	OwnerID      uint           `gorm:"index;not null" json:"owner_id"`
	Content      string         `gorm:"type:text;not null" json:"content"`
	NextTime     *time.Time     `json:"next_time"`
	Attachment   string         `gorm:"size:255" json:"attachment"`
	CreatedAt    time.Time      `json:"created_at"`
	UpdatedAt    time.Time      `json:"updated_at"`
	
	Owner        *User          `gorm:"foreignKey:OwnerID" json:"owner,omitempty"`
}

// TableName 指定表名
func (FollowUp) TableName() string {
	return "crm_follow_up"
}

// PublicPool 公海池
type PublicPool struct {
	ID           uint           `gorm:"primaryKey" json:"id"`
	CustomerID   uint           `gorm:"uniqueIndex;not null" json:"customer_id"`
	Reason       int            `gorm:"default:1" json:"reason"` // 1:主动释放 2:超时回收 3:离职交接
	PreviousOwnerID uint        `gorm:"not null" json:"previous_owner_id"`
	ClaimedAt    *time.Time     `json:"claimed_at"`
	ClaimedByID  *uint          `json:"claimed_by_id"`
	ExpiresAt    time.Time      `json:"expires_at"`
	Status       int            `gorm:"default:1" json:"status"` // 1:可领取 2:已领取
	CreatedAt    time.Time      `json:"created_at"`
	UpdatedAt    time.Time      `json:"updated_at"`
	
	Customer     *Customer      `gorm:"foreignKey:CustomerID" json:"customer,omitempty"`
}

// TableName 指定表名
func (PublicPool) TableName() string {
	return "crm_public_pool"
}
