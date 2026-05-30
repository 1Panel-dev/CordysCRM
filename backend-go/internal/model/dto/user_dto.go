package dto

import "time"

// LoginRequest 登录请求
type LoginRequest struct {
	Username string `json:"username" binding:"required"`
	Password string `json:"password" binding:"required"`
	Captcha  string `json:"captcha"`
}

// LoginResponse 登录响应
type LoginResponse struct {
	Token      string   `json:"token"`
	ExpiresIn  int64    `json:"expires_in"`
	UserInfo   UserInfo `json:"user_info"`
}

// UserInfo 用户信息
type UserInfo struct {
	ID           uint     `json:"id"`
	Username     string   `json:"username"`
	Nickname     string   `json:"nickname"`
	Avatar       string   `json:"avatar"`
	Email        string   `json:"email"`
	Phone        string   `json:"phone"`
	DepartmentID uint     `json:"department_id"`
	RoleCodes    []string `json:"role_codes"`
	Permissions  []string `json:"permissions"`
}

// CreateUserRequest 创建用户请求
type CreateUserRequest struct {
	Username     string `json:"username" binding:"required,min=3,max=50"`
	Password     string `json:"password" binding:"required,min=6,max=20"`
	Nickname     string `json:"nickname" binding:"required,max=50"`
	Email        string `json:"email" binding:"omitempty,email"`
	Phone        string `json:"phone" binding:"omitempty"`
	DepartmentID uint   `json:"department_id"`
	RoleIDs      []uint `json:"role_ids"`
	Status       int    `json:"status"`
}

// UpdateUserRequest 更新用户请求
type UpdateUserRequest struct {
	Nickname     string `json:"nickname" binding:"omitempty,max=50"`
	Avatar       string `json:"avatar" binding:"omitempty"`
	Email        string `json:"email" binding:"omitempty,email"`
	Phone        string `json:"phone" binding:"omitempty"`
	DepartmentID uint   `json:"department_id"`
	RoleIDs      []uint `json:"role_ids"`
	Status       int    `json:"status"`
}

// ChangePasswordRequest 修改密码请求
type ChangePasswordRequest struct {
	OldPassword string `json:"old_password" binding:"required"`
	NewPassword string `json:"new_password" binding:"required,min=6,max=20"`
}

// UserListQuery 用户列表查询参数
type UserListQuery struct {
	Page         int    `form:"page" binding:"min=1"`
	PageSize     int    `form:"page_size" binding:"min=1,max=100"`
	Username     string `form:"username"`
	Nickname     string `form:"nickname"`
	DepartmentID uint   `form:"department_id"`
	Status       int    `form:"status"`
}

// CreateDepartmentRequest 创建部门请求
type CreateDepartmentRequest struct {
	Name     string `json:"name" binding:"required,max=50"`
	ParentID uint   `json:"parent_id"`
	LeaderID uint   `json:"leader_id"`
	Sort     int    `json:"sort"`
}

// UpdateDepartmentRequest 更新部门请求
type UpdateDepartmentRequest struct {
	Name     string `json:"name" binding:"omitempty,max=50"`
	ParentID uint   `json:"parent_id"`
	LeaderID uint   `json:"leader_id"`
	Sort     int    `json:"sort"`
	Status   int    `json:"status"`
}

// CreateRoleRequest 创建角色请求
type CreateRoleRequest struct {
	Name        string   `json:"name" binding:"required,max=50"`
	Code        string   `json:"code" binding:"required,max=50"`
	Description string   `json:"description"`
	MenuIDs     []uint   `json:"menu_ids"`
}

// UpdateRoleRequest 更新角色请求
type UpdateRoleRequest struct {
	Name        string   `json:"name" binding:"omitempty,max=50"`
	Description string   `json:"description"`
	MenuIDs     []uint   `json:"menu_ids"`
	Status      int      `json:"status"`
}

// MenuTreeResponse 菜单树响应
type MenuTreeResponse struct {
	ID        uint                 `json:"id"`
	Name      string               `json:"name"`
	Path      string               `json:"path"`
	Component string               `json:"component"`
	Icon      string               `json:"icon"`
	Sort      int                  `json:"sort"`
	Type      int                  `json:"type"`
	Permission string              `json:"permission"`
	Children  []MenuTreeResponse   `json:"children"`
}

// AssignMenuRequest 分配菜单请求
type AssignMenuRequest struct {
	MenuIDs []uint `json:"menu_ids" binding:"required"`
}

// DeptTreeResponse 部门树响应
type DeptTreeResponse struct {
	ID       uint            `json:"id"`
	Name     string          `json:"name"`
	ParentID uint            `json:"parent_id"`
	LeaderID uint            `json:"leader_id"`
	Children []DeptTreeResponse `json:"children"`
}

// UserProfileUpdate 更新个人资料
type UserProfileUpdate struct {
	Nickname string `json:"nickname" binding:"omitempty,max=50"`
	Avatar   string `json:"avatar" binding:"omitempty"`
	Email    string `json:"email" binding:"omitempty,email"`
	Phone    string `json:"phone" binding:"omitempty"`
}

// OperationLogDTO 操作日志 DTO
type OperationLogDTO struct {
	ID           uint      `json:"id"`
	UserID       uint      `json:"user_id"`
	Username     string    `json:"username"`
	Module       string    `json:"module"`
	Action       string    `json:"action"`
	Method       string    `json:"method"`
	URL          string    `json:"url"`
	Params       string    `json:"params"`
	Result       string    `json:"result"`
	Status       int       `json:"status"`
	IP           string    `json:"ip"`
	UserAgent    string    `json:"user_agent"`
	Duration     int64     `json:"duration"` // ms
	CreatedAt    time.Time `json:"created_at"`
}
