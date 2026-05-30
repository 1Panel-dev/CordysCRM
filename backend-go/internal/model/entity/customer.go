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

// TableName 指定表名
func (Customer) TableName() string {
	return "customer"
}
