package entity

import (
	"time"

	"golang.org/x/crypto/bcrypt"
)

// User 用户实体
type User struct {
	ID           uint      `gorm:"primaryKey" json:"id"`
	Username     string    `gorm:"uniqueIndex;size:50;not null" json:"username"`
	Password     string    `gorm:"size:255;not null" json:"-"`
	Nickname     string    `gorm:"size:50" json:"nickname"`
	Avatar       string    `gorm:"size:255" json:"avatar"`
	Email        string    `gorm:"size:100" json:"email"`
	Phone        string    `gorm:"size:20" json:"phone"`
	DepartmentID uint      `gorm:"index" json:"department_id"`
	RoleIDs      []uint    `gorm:"many2many:user_roles;" json:"role_ids"`
	Status       int       `gorm:"default:1" json:"status"` // 1:正常 0:禁用
	LastLoginAt  *time.Time `json:"last_login_at"`
	CreatedAt    time.Time `json:"created_at"`
	UpdatedAt    time.Time `json:"updated_at"`
}

// TableName 指定表名
func (User) TableName() string {
	return "sys_user"
}

// SetPassword 加密密码
func (u *User) SetPassword(password string) error {
	hashed, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
	if err != nil {
		return err
	}
	u.Password = string(hashed)
	return nil
}

// CheckPassword 验证密码
func (u *User) CheckPassword(password string) bool {
	err := bcrypt.CompareHashAndPassword([]byte(u.Password), []byte(password))
	return err == nil
}

// Department 部门实体
type Department struct {
	ID       uint   `gorm:"primaryKey" json:"id"`
	Name     string `gorm:"size:50;not null" json:"name"`
	ParentID uint   `gorm:"index" json:"parent_id"`
	LeaderID uint   `json:"leader_id"`
	Sort     int    `gorm:"default:0" json:"sort"`
	Status   int    `gorm:"default:1" json:"status"`
	CreatedAt time.Time `json:"created_at"`
	UpdatedAt time.Time `json:"updated_at"`
}

// TableName 指定表名
func (Department) TableName() string {
	return "sys_department"
}

// Role 角色实体
type Role struct {
	ID          uint      `gorm:"primaryKey" json:"id"`
	Name        string    `gorm:"uniqueIndex;size:50;not null" json:"name"`
	Code        string    `gorm:"uniqueIndex;size:50;not null" json:"code"`
	Description string    `gorm:"size:255" json:"description"`
	Status      int       `gorm:"default:1" json:"status"`
	CreatedAt   time.Time `json:"created_at"`
	UpdatedAt   time.Time `json:"updated_at"`
	Menus       []Menu    `gorm:"many2many:role_menus;" json:"menus"`
}

// TableName 指定表名
func (Role) TableName() string {
	return "sys_role"
}

// Menu 菜单/权限实体
type Menu struct {
	ID       uint   `gorm:"primaryKey" json:"id"`
	Name     string `gorm:"size:50;not null" json:"name"`
	Path     string `gorm:"size:255" json:"path"`
	Component string `gorm:"size:255" json:"component"`
	Icon     string `gorm:"size:50" json:"icon"`
	Sort     int    `gorm:"default:0" json:"sort"`
	ParentID uint   `gorm:"index" json:"parent_id"`
	Type     int    `gorm:"default:1" json:"type"` // 1:目录 2:菜单 3:按钮
	Permission string `gorm:"size:100" json:"permission"`
	Status   int    `gorm:"default:1" json:"status"`
	CreatedAt time.Time `json:"created_at"`
	UpdatedAt time.Time `json:"updated_at"`
}

// TableName 指定表名
func (Menu) TableName() string {
	return "sys_menu"
}
