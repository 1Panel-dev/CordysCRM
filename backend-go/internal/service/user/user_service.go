package service

import (
	"context"
	"errors"
	"time"

	"github.com/golang-jwt/jwt/v5"
	"golang.org/x/crypto/bcrypt"

	"sCRM/internal/model/dto"
	"sCRM/internal/model/entity"
	"sCRM/internal/repository"
)

var (
	ErrInvalidCredentials = errors.New("invalid username or password")
	ErrUserDisabled       = errors.New("user has been disabled")
	ErrTokenExpired       = errors.New("token has expired")
)

// UserService 用户服务接口
type UserService interface {
	Login(ctx context.Context, req *dto.LoginRequest) (*dto.LoginResponse, error)
	GetUserInfo(ctx context.Context, userID uint) (*dto.UserInfo, error)
	CreateUser(ctx context.Context, req *dto.CreateUserRequest) (*entity.User, error)
	UpdateUser(ctx context.Context, id uint, req *dto.UpdateUserRequest) error
	DeleteUser(ctx context.Context, id uint) error
	GetUserByID(ctx context.Context, id uint) (*entity.User, error)
	ListUsers(ctx context.Context, query dto.UserListQuery) ([]entity.User, int64, error)
	ChangePassword(ctx context.Context, userID uint, req *dto.ChangePasswordRequest) error
	AssignRoles(ctx context.Context, userID uint, roleIDs []uint) error
}

type userService struct {
	userRepo     repository.UserRepository
	roleRepo     repository.RoleRepository
	menuRepo     repository.MenuRepository
	jwtSecret    string
	jwtExpire    int64 // seconds
}

func NewUserService(
	userRepo repository.UserRepository,
	roleRepo repository.RoleRepository,
	menuRepo repository.MenuRepository,
	jwtSecret string,
	jwtExpire int64,
) UserService {
	return &userService{
		userRepo:  userRepo,
		roleRepo:  roleRepo,
		menuRepo:  menuRepo,
		jwtSecret: jwtSecret,
		jwtExpire: jwtExpire,
	}
}

func (s *userService) Login(ctx context.Context, req *dto.LoginRequest) (*dto.LoginResponse, error) {
	// 获取用户
	user, err := s.userRepo.GetByUsername(ctx, req.Username)
	if err != nil {
		if errors.Is(err, repository.ErrUserNotFound) {
			return nil, ErrInvalidCredentials
		}
		return nil, err
	}

	// 检查用户状态
	if user.Status != 1 {
		return nil, ErrUserDisabled
	}

	// 验证密码
	if !user.CheckPassword(req.Password) {
		return nil, ErrInvalidCredentials
	}

	// 更新最后登录时间
	now := time.Now()
	user.LastLoginAt = &now
	if err := s.userRepo.Update(ctx, user); err != nil {
		return nil, err
	}

	// 获取用户角色
	roles, err := s.userRepo.GetRoles(ctx, user.ID)
	if err != nil {
		return nil, err
	}

	// 获取角色码
	roleCodes := make([]string, 0, len(roles))
	for _, role := range roles {
		roleCodes = append(roleCodes, role.Code)
	}

	// 获取权限列表
	permissions, err := s.getUserPermissions(ctx, user.ID)
	if err != nil {
		return nil, err
	}

	// 生成 JWT Token
	token, err := s.generateToken(user.ID, user.Username, roleCodes)
	if err != nil {
		return nil, err
	}

	return &dto.LoginResponse{
		Token:     token,
		ExpiresIn: s.jwtExpire,
		UserInfo: dto.UserInfo{
			ID:           user.ID,
			Username:     user.Username,
			Nickname:     user.Nickname,
			Avatar:       user.Avatar,
			Email:        user.Email,
			Phone:        user.Phone,
			DepartmentID: user.DepartmentID,
			RoleCodes:    roleCodes,
			Permissions:  permissions,
		},
	}, nil
}

func (s *userService) GetUserInfo(ctx context.Context, userID uint) (*dto.UserInfo, error) {
	user, err := s.userRepo.GetByID(ctx, userID)
	if err != nil {
		return nil, err
	}

	roles, err := s.userRepo.GetRoles(ctx, userID)
	if err != nil {
		return nil, err
	}

	roleCodes := make([]string, 0, len(roles))
	for _, role := range roles {
		roleCodes = append(roleCodes, role.Code)
	}

	permissions, err := s.getUserPermissions(ctx, userID)
	if err != nil {
		return nil, err
	}

	return &dto.UserInfo{
		ID:           user.ID,
		Username:     user.Username,
		Nickname:     user.Nickname,
		Avatar:       user.Avatar,
		Email:        user.Email,
		Phone:        user.Phone,
		DepartmentID: user.DepartmentID,
		RoleCodes:    roleCodes,
		Permissions:  permissions,
	}, nil
}

func (s *userService) CreateUser(ctx context.Context, req *dto.CreateUserRequest) (*entity.User, error) {
	user := &entity.User{
		Username:     req.Username,
		Nickname:     req.Nickname,
		Email:        req.Email,
		Phone:        req.Phone,
		DepartmentID: req.DepartmentID,
		Status:       req.Status,
	}

	// 加密密码
	if err := user.SetPassword(req.Password); err != nil {
		return nil, err
	}

	// 创建用户
	if err := s.userRepo.Create(ctx, user); err != nil {
		return nil, err
	}

	// 分配角色
	if len(req.RoleIDs) > 0 {
		if err := s.userRepo.AssignRoles(ctx, user.ID, req.RoleIDs); err != nil {
			return nil, err
		}
	}

	return user, nil
}

func (s *userService) UpdateUser(ctx context.Context, id uint, req *dto.UpdateUserRequest) error {
	user, err := s.userRepo.GetByID(ctx, id)
	if err != nil {
		return err
	}

	// 更新字段
	if req.Nickname != "" {
		user.Nickname = req.Nickname
	}
	if req.Avatar != "" {
		user.Avatar = req.Avatar
	}
	if req.Email != "" {
		user.Email = req.Email
	}
	if req.Phone != "" {
		user.Phone = req.Phone
	}
	if req.DepartmentID > 0 {
		user.DepartmentID = req.DepartmentID
	}
	if req.Status >= 0 {
		user.Status = req.Status
	}

	if err := s.userRepo.Update(ctx, user); err != nil {
		return err
	}

	// 更新角色
	if req.RoleIDs != nil {
		if err := s.userRepo.AssignRoles(ctx, id, req.RoleIDs); err != nil {
			return err
		}
	}

	return nil
}

func (s *userService) DeleteUser(ctx context.Context, id uint) error {
	return s.userRepo.Delete(ctx, id)
}

func (s *userService) GetUserByID(ctx context.Context, id uint) (*entity.User, error) {
	return s.userRepo.GetByID(ctx, id)
}

func (s *userService) ListUsers(ctx context.Context, query dto.UserListQuery) ([]entity.User, int64, error) {
	return s.userRepo.List(ctx, query)
}

func (s *userService) ChangePassword(ctx context.Context, userID uint, req *dto.ChangePasswordRequest) error {
	user, err := s.userRepo.GetByID(ctx, userID)
	if err != nil {
		return err
	}

	// 验证旧密码
	if !user.CheckPassword(req.OldPassword) {
		return errors.New("old password is wrong")
	}

	// 设置新密码
	if err := user.SetPassword(req.NewPassword); err != nil {
		return err
	}

	return s.userRepo.Update(ctx, user)
}

func (s *userService) AssignRoles(ctx context.Context, userID uint, roleIDs []uint) error {
	return s.userRepo.AssignRoles(ctx, userID, roleIDs)
}

// getUserPermissions 获取用户权限列表
func (s *userService) getUserPermissions(ctx context.Context, userID uint) ([]string, error) {
	roles, err := s.userRepo.GetRoles(ctx, userID)
	if err != nil {
		return nil, err
	}

	permissionSet := make(map[string]bool)
	for _, role := range roles {
		menus, err := s.roleRepo.GetMenus(ctx, role.ID)
		if err != nil {
			return nil, err
		}
		for _, menu := range menus {
			if menu.Permission != "" {
				permissionSet[menu.Permission] = true
			}
		}
	}

	permissions := make([]string, 0, len(permissionSet))
	for perm := range permissionSet {
		permissions = append(permissions, perm)
	}

	return permissions, nil
}

// generateToken 生成 JWT Token
func (s *userService) generateToken(userID uint, username string, roles []string) (string, error) {
	now := time.Now()
	expiresAt := now.Add(time.Duration(s.jwtExpire) * time.Second)

	claims := jwt.MapClaims{
		"user_id":   userID,
		"username":  username,
		"roles":     roles,
		"issued_at": now.Unix(),
		"exp":       expiresAt.Unix(),
	}

	token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
	return token.SignedString([]byte(s.jwtSecret))
}

// ParseToken 解析 JWT Token
func ParseToken(tokenString string, jwtSecret string) (jwt.MapClaims, error) {
	token, err := jwt.Parse(tokenString, func(token *jwt.Token) (interface{}, error) {
		if _, ok := token.Method.(*jwt.SigningMethodHMAC); !ok {
			return nil, errors.New("unexpected signing method")
		}
		return []byte(jwtSecret), nil
	})

	if err != nil {
		return nil, err
	}

	if claims, ok := token.Claims.(jwt.MapClaims); ok && token.Valid {
		return claims, nil
	}

	return nil, ErrTokenExpired
}

// DepartmentService 部门服务接口
type DepartmentService interface {
	CreateDepartment(ctx context.Context, req *dto.CreateDepartmentRequest) (*entity.Department, error)
	UpdateDepartment(ctx context.Context, id uint, req *dto.UpdateDepartmentRequest) error
	DeleteDepartment(ctx context.Context, id uint) error
	GetDepartmentTree(ctx context.Context) ([]dto.DeptTreeResponse, error)
}

type departmentService struct {
	deptRepo repository.DepartmentRepository
}

func NewDepartmentService(deptRepo repository.DepartmentRepository) DepartmentService {
	return &departmentService{deptRepo: deptRepo}
}

func (s *departmentService) CreateDepartment(ctx context.Context, req *dto.CreateDepartmentRequest) (*entity.Department, error) {
	dept := &entity.Department{
		Name:     req.Name,
		ParentID: req.ParentID,
		LeaderID: req.LeaderID,
		Sort:     req.Sort,
		Status:   1,
	}

	if err := s.deptRepo.Create(ctx, dept); err != nil {
		return nil, err
	}

	return dept, nil
}

func (s *departmentService) UpdateDepartment(ctx context.Context, id uint, req *dto.UpdateDepartmentRequest) error {
	dept, err := s.deptRepo.GetByID(ctx, id)
	if err != nil {
		return err
	}

	if req.Name != "" {
		dept.Name = req.Name
	}
	if req.ParentID > 0 || req.Name == "" {
		dept.ParentID = req.ParentID
	}
	if req.LeaderID > 0 || req.Name == "" {
		dept.LeaderID = req.LeaderID
	}
	if req.Sort >= 0 || req.Name == "" {
		dept.Sort = req.Sort
	}
	if req.Status >= 0 {
		dept.Status = req.Status
	}

	return s.deptRepo.Update(ctx, dept)
}

func (s *departmentService) DeleteDepartment(ctx context.Context, id uint) error {
	return s.deptRepo.Delete(ctx, id)
}

func (s *departmentService) GetDepartmentTree(ctx context.Context) ([]dto.DeptTreeResponse, error) {
	return s.deptRepo.GetTree(ctx)
}

// RoleService 角色服务接口
type RoleService interface {
	CreateRole(ctx context.Context, req *dto.CreateRoleRequest) (*entity.Role, error)
	UpdateRole(ctx context.Context, id uint, req *dto.UpdateRoleRequest) error
	DeleteRole(ctx context.Context, id uint) error
	ListRoles(ctx context.Context) ([]entity.Role, error)
	AssignMenus(ctx context.Context, roleID uint, menuIDs []uint) error
	GetRoleMenus(ctx context.Context, roleID uint) ([]entity.Menu, error)
}

type roleService struct {
	roleRepo repository.RoleRepository
	menuRepo repository.MenuRepository
}

func NewRoleService(roleRepo repository.RoleRepository, menuRepo repository.MenuRepository) RoleService {
	return &roleService{
		roleRepo: roleRepo,
		menuRepo: menuRepo,
	}
}

func (s *roleService) CreateRole(ctx context.Context, req *dto.CreateRoleRequest) (*entity.Role, error) {
	role := &entity.Role{
		Name:        req.Name,
		Code:        req.Code,
		Description: req.Description,
		Status:      1,
	}

	if err := s.roleRepo.Create(ctx, role); err != nil {
		return nil, err
	}

	// 分配菜单
	if len(req.MenuIDs) > 0 {
		if err := s.roleRepo.AssignMenus(ctx, role.ID, req.MenuIDs); err != nil {
			return nil, err
		}
	}

	return role, nil
}

func (s *roleService) UpdateRole(ctx context.Context, id uint, req *dto.UpdateRoleRequest) error {
	role, err := s.roleRepo.GetByID(ctx, id)
	if err != nil {
		return err
	}

	if req.Name != "" {
		role.Name = req.Name
	}
	if req.Description != "" {
		role.Description = req.Description
	}
	if req.Status >= 0 {
		role.Status = req.Status
	}

	if err := s.roleRepo.Update(ctx, role); err != nil {
		return err
	}

	// 更新菜单
	if req.MenuIDs != nil {
		if err := s.roleRepo.AssignMenus(ctx, id, req.MenuIDs); err != nil {
			return err
		}
	}

	return nil
}

func (s *roleService) DeleteRole(ctx context.Context, id uint) error {
	return s.roleRepo.Delete(ctx, id)
}

func (s *roleService) ListRoles(ctx context.Context) ([]entity.Role, error) {
	return s.roleRepo.List(ctx)
}

func (s *roleService) AssignMenus(ctx context.Context, roleID uint, menuIDs []uint) error {
	return s.roleRepo.AssignMenus(ctx, roleID, menuIDs)
}

func (s *roleService) GetRoleMenus(ctx context.Context, roleID uint) ([]entity.Menu, error) {
	return s.roleRepo.GetMenus(ctx, roleID)
}

// MenuService 菜单服务接口
type MenuService interface {
	CreateMenu(ctx context.Context, menu *entity.Menu) error
	UpdateMenu(ctx context.Context, id uint, menu *entity.Menu) error
	DeleteMenu(ctx context.Context, id uint) error
	GetMenuTree(ctx context.Context) ([]dto.MenuTreeResponse, error)
	GetUserMenus(ctx context.Context, userID uint) ([]dto.MenuTreeResponse, error)
}

type menuService struct {
	menuRepo repository.MenuRepository
}

func NewMenuService(menuRepo repository.MenuRepository) MenuService {
	return &menuService{menuRepo: menuRepo}
}

func (s *menuService) CreateMenu(ctx context.Context, menu *entity.Menu) error {
	return s.menuRepo.Create(ctx, menu)
}

func (s *menuService) UpdateMenu(ctx context.Context, id uint, menu *entity.Menu) error {
	existing, err := s.menuRepo.GetByID(ctx, id)
	if err != nil {
		return err
	}

	if menu.Name != "" {
		existing.Name = menu.Name
	}
	if menu.Path != "" {
		existing.Path = menu.Path
	}
	if menu.Component != "" {
		existing.Component = menu.Component
	}
	if menu.Icon != "" {
		existing.Icon = menu.Icon
	}
	if menu.Sort >= 0 {
		existing.Sort = menu.Sort
	}
	if menu.Type > 0 {
		existing.Type = menu.Type
	}
	if menu.Permission != "" {
		existing.Permission = menu.Permission
	}
	if menu.Status >= 0 {
		existing.Status = menu.Status
	}

	return s.menuRepo.Update(ctx, existing)
}

func (s *menuService) DeleteMenu(ctx context.Context, id uint) error {
	return s.menuRepo.Delete(ctx, id)
}

func (s *menuService) GetMenuTree(ctx context.Context) ([]dto.MenuTreeResponse, error) {
	return s.menuRepo.GetTree(ctx)
}

func (s *menuService) GetUserMenus(ctx context.Context, userID uint) ([]dto.MenuTreeResponse, error) {
	menus, err := s.menuRepo.GetByUserID(ctx, userID)
	if err != nil {
		return nil, err
	}

	return buildMenuTreeFromEntities(menus), nil
}

// buildMenuTreeFromEntities 从实体构建菜单树
func buildMenuTreeFromEntities(menus []entity.Menu) []dto.MenuTreeResponse {
	treeMap := make(map[uint]*dto.MenuTreeResponse)
	var rootNodes []dto.MenuTreeResponse

	for _, menu := range menus {
		node := dto.MenuTreeResponse{
			ID:         menu.ID,
			Name:       menu.Name,
			Path:       menu.Path,
			Component:  menu.Component,
			Icon:       menu.Icon,
			Sort:       menu.Sort,
			Type:       menu.Type,
			Permission: menu.Permission,
			Children:   []dto.MenuTreeResponse{},
		}
		treeMap[menu.ID] = &node

		if menu.ParentID == 0 {
			rootNodes = append(rootNodes, node)
		} else {
			if parent, ok := treeMap[menu.ParentID]; ok {
				parent.Children = append(parent.Children, node)
			}
		}
	}

	return rootNodes
}
