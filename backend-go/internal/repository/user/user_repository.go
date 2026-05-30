package repository

import (
	"context"
	"errors"

	"gorm.io/gorm"

	"sCRM/internal/model/entity"
	"sCRM/internal/model/dto"
	"sCRM/pkg/util"
)

var (
	ErrUserNotFound     = errors.New("user not found")
	ErrUsernameExists   = errors.New("username already exists")
	ErrPasswordWrong    = errors.New("password is wrong")
)

// UserRepository 用户仓库接口
type UserRepository interface {
	Create(ctx context.Context, user *entity.User) error
	GetByID(ctx context.Context, id uint) (*entity.User, error)
	GetByUsername(ctx context.Context, username string) (*entity.User, error)
	Update(ctx context.Context, user *entity.User) error
	Delete(ctx context.Context, id uint) error
	List(ctx context.Context, query dto.UserListQuery) ([]entity.User, int64, error)
	AssignRoles(ctx context.Context, userID uint, roleIDs []uint) error
	GetRoles(ctx context.Context, userID uint) ([]entity.Role, error)
}

type userRepository struct {
	db *gorm.DB
}

func NewUserRepository(db *gorm.DB) UserRepository {
	return &userRepository{db: db}
}

func (r *userRepository) Create(ctx context.Context, user *entity.User) error {
	// 检查用户名是否存在
	var count int64
	if err := r.db.WithContext(ctx).Model(&entity.User{}).Where("username = ?", user.Username).Count(&count).Error; err != nil {
		return err
	}
	if count > 0 {
		return ErrUsernameExists
	}
	
	return r.db.WithContext(ctx).Create(user).Error
}

func (r *userRepository) GetByID(ctx context.Context, id uint) (*entity.User, error) {
	var user entity.User
	err := r.db.WithContext(ctx).
		Preload("RoleIDs").
		First(&user, id).Error
	if err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			return nil, ErrUserNotFound
		}
		return nil, err
	}
	return &user, nil
}

func (r *userRepository) GetByUsername(ctx context.Context, username string) (*entity.User, error) {
	var user entity.User
	err := r.db.WithContext(ctx).
		Preload("RoleIDs").
		Where("username = ?", username).
		First(&user).Error
	if err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			return nil, ErrUserNotFound
		}
		return nil, err
	}
	return &user, nil
}

func (r *userRepository) Update(ctx context.Context, user *entity.User) error {
	return r.db.WithContext(ctx).Save(user).Error
}

func (r *userRepository) Delete(ctx context.Context, id uint) error {
	return r.db.WithContext(ctx).Delete(&entity.User{}, id).Error
}

func (r *userRepository) List(ctx context.Context, query dto.UserListQuery) ([]entity.User, int64, error) {
	var users []entity.User
	var total int64
	
	tx := r.db.WithContext(ctx).Model(&entity.User{})
	
	// 条件查询
	if query.Username != "" {
		tx = tx.Where("username LIKE ?", "%"+query.Username+"%")
	}
	if query.Nickname != "" {
		tx = tx.Where("nickname LIKE ?", "%"+query.Nickname+"%")
	}
	if query.DepartmentID > 0 {
		tx = tx.Where("department_id = ?", query.DepartmentID)
	}
	if query.Status >= 0 {
		tx = tx.Where("status = ?", query.Status)
	}
	
	// 总数
	if err := tx.Count(&total).Error; err != nil {
		return nil, 0, err
	}
	
	// 分页
	offset := (query.Page - 1) * query.PageSize
	if err := tx.Offset(offset).Limit(query.PageSize).Find(&users).Error; err != nil {
		return nil, 0, err
	}
	
	return users, total, nil
}

func (r *userRepository) AssignRoles(ctx context.Context, userID uint, roleIDs []uint) error {
	return r.db.WithContext(ctx).Transaction(func(tx *gorm.DB) error {
		// 删除旧关联
		if err := tx.Where("user_id = ?", userID).Delete(&UserRole{}).Error; err != nil {
			return err
		}
		
		// 添加新关联
		if len(roleIDs) > 0 {
			userRoles := make([]UserRole, 0, len(roleIDs))
			for _, roleID := range roleIDs {
				userRoles = append(userRoles, UserRole{
					UserID: userID,
					RoleID: roleID,
				})
			}
			if err := tx.Create(&userRoles).Error; err != nil {
				return err
			}
		}
		return nil
	})
}

func (r *userRepository) GetRoles(ctx context.Context, userID uint) ([]entity.Role, error) {
	var roles []entity.Role
	err := r.db.WithContext(ctx).
		Joins("JOIN user_roles ON user_roles.role_id = sys_role.id").
		Where("user_roles.user_id = ?", userID).
		Find(&roles).Error
	return roles, err
}

// UserRole 用户角色关联表模型
type UserRole struct {
	UserID uint `gorm:"primaryKey"`
	RoleID uint `gorm:"primaryKey"`
}

func (UserRole) TableName() string {
	return "user_roles"
}

// DepartmentRepository 部门仓库接口
type DepartmentRepository interface {
	Create(ctx context.Context, dept *entity.Department) error
	GetByID(ctx context.Context, id uint) (*entity.Department, error)
	Update(ctx context.Context, dept *entity.Department) error
	Delete(ctx context.Context, id uint) error
	List(ctx context.Context) ([]entity.Department, error)
	GetTree(ctx context.Context) ([]dto.DeptTreeResponse, error)
}

type departmentRepository struct {
	db *gorm.DB
}

func NewDepartmentRepository(db *gorm.DB) DepartmentRepository {
	return &departmentRepository{db: db}
}

func (r *departmentRepository) Create(ctx context.Context, dept *entity.Department) error {
	return r.db.WithContext(ctx).Create(dept).Error
}

func (r *departmentRepository) GetByID(ctx context.Context, id uint) (*entity.Department, error) {
	var dept entity.Department
	err := r.db.WithContext(ctx).First(&dept, id).Error
	if err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			return nil, errors.New("department not found")
		}
		return nil, err
	}
	return &dept, nil
}

func (r *departmentRepository) Update(ctx context.Context, dept *entity.Department) error {
	return r.db.WithContext(ctx).Save(dept).Error
}

func (r *departmentRepository) Delete(ctx context.Context, id uint) error {
	return r.db.WithContext(ctx).Delete(&entity.Department{}, id).Error
}

func (r *departmentRepository) List(ctx context.Context) ([]entity.Department, error) {
	var depts []entity.Department
	err := r.db.WithContext(ctx).Order("sort ASC, id ASC").Find(&depts).Error
	return depts, err
}

func (r *departmentRepository) GetTree(ctx context.Context) ([]dto.DeptTreeResponse, error) {
	var depts []entity.Department
	if err := r.db.WithContext(ctx).Order("sort ASC, id ASC").Find(&depts).Error; err != nil {
		return nil, err
	}
	
	// 构建树形结构
	treeMap := make(map[uint]*dto.DeptTreeResponse)
	var rootNodes []dto.DeptTreeResponse
	
	for _, dept := range depts {
		node := dto.DeptTreeResponse{
			ID:       dept.ID,
			Name:     dept.Name,
			ParentID: dept.ParentID,
			LeaderID: dept.LeaderID,
			Children: []dto.DeptTreeResponse{},
		}
		treeMap[dept.ID] = &node
		
		if dept.ParentID == 0 {
			rootNodes = append(rootNodes, node)
		} else {
			if parent, ok := treeMap[dept.ParentID]; ok {
				parent.Children = append(parent.Children, node)
			}
		}
	}
	
	return rootNodes, nil
}

// RoleRepository 角色仓库接口
type RoleRepository interface {
	Create(ctx context.Context, role *entity.Role) error
	GetByID(ctx context.Context, id uint) (*entity.Role, error)
	GetByCode(ctx context.Context, code string) (*entity.Role, error)
	Update(ctx context.Context, role *entity.Role) error
	Delete(ctx context.Context, id uint) error
	List(ctx context.Context) ([]entity.Role, error)
	AssignMenus(ctx context.Context, roleID uint, menuIDs []uint) error
	GetMenus(ctx context.Context, roleID uint) ([]entity.Menu, error)
}

type roleRepository struct {
	db *gorm.DB
}

func NewRoleRepository(db *gorm.DB) RoleRepository {
	return &roleRepository{db: db}
}

func (r *roleRepository) Create(ctx context.Context, role *entity.Role) error {
	return r.db.WithContext(ctx).Create(role).Error
}

func (r *roleRepository) GetByID(ctx context.Context, id uint) (*entity.Role, error) {
	var role entity.Role
	err := r.db.WithContext(ctx).Preload("Menus").First(&role, id).Error
	if err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			return nil, errors.New("role not found")
		}
		return nil, err
	}
	return &role, nil
}

func (r *roleRepository) GetByCode(ctx context.Context, code string) (*entity.Role, error) {
	var role entity.Role
	err := r.db.WithContext(ctx).Where("code = ?", code).First(&role).Error
	if err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			return nil, errors.New("role not found")
		}
		return nil, err
	}
	return &role, nil
}

func (r *roleRepository) Update(ctx context.Context, role *entity.Role) error {
	return r.db.WithContext(ctx).Save(role).Error
}

func (r *roleRepository) Delete(ctx context.Context, id uint) error {
	return r.db.WithContext(ctx).Delete(&entity.Role{}, id).Error
}

func (r *roleRepository) List(ctx context.Context) ([]entity.Role, error) {
	var roles []entity.Role
	err := r.db.WithContext(ctx).Order("id ASC").Find(&roles).Error
	return roles, err
}

func (r *roleRepository) AssignMenus(ctx context.Context, roleID uint, menuIDs []uint) error {
	return r.db.WithContext(ctx).Transaction(func(tx *gorm.DB) error {
		// 删除旧关联
		if err := tx.Where("role_id = ?", roleID).Delete(&RoleMenu{}).Error; err != nil {
			return err
		}
		
		// 添加新关联
		if len(menuIDs) > 0 {
			roleMenus := make([]RoleMenu, 0, len(menuIDs))
			for _, menuID := range menuIDs {
				roleMenus = append(roleMenus, RoleMenu{
					RoleID: roleID,
					MenuID: menuID,
				})
			}
			if err := tx.Create(&roleMenus).Error; err != nil {
				return err
			}
		}
		return nil
	})
}

func (r *roleRepository) GetMenus(ctx context.Context, roleID uint) ([]entity.Menu, error) {
	var menus []entity.Menu
	err := r.db.WithContext(ctx).
		Joins("JOIN role_menus ON role_menus.menu_id = sys_menu.id").
		Where("role_menus.role_id = ?", roleID).
		Order("sys_menu.sort ASC").
		Find(&menus).Error
	return menus, err
}

// RoleMenu 角色菜单关联表模型
type RoleMenu struct {
	RoleID uint `gorm:"primaryKey"`
	MenuID uint `gorm:"primaryKey"`
}

func (RoleMenu) TableName() string {
	return "role_menus"
}

// MenuRepository 菜单仓库接口
type MenuRepository interface {
	Create(ctx context.Context, menu *entity.Menu) error
	GetByID(ctx context.Context, id uint) (*entity.Menu, error)
	Update(ctx context.Context, menu *entity.Menu) error
	Delete(ctx context.Context, id uint) error
	List(ctx context.Context) ([]entity.Menu, error)
	GetTree(ctx context.Context) ([]dto.MenuTreeResponse, error)
	GetByUserID(ctx context.Context, userID uint) ([]entity.Menu, error)
}

type menuRepository struct {
	db *gorm.DB
}

func NewMenuRepository(db *gorm.DB) MenuRepository {
	return &menuRepository{db: db}
}

func (r *menuRepository) Create(ctx context.Context, menu *entity.Menu) error {
	return r.db.WithContext(ctx).Create(menu).Error
}

func (r *menuRepository) GetByID(ctx context.Context, id uint) (*entity.Menu, error) {
	var menu entity.Menu
	err := r.db.WithContext(ctx).First(&menu, id).Error
	if err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			return nil, errors.New("menu not found")
		}
		return nil, err
	}
	return &menu, nil
}

func (r *menuRepository) Update(ctx context.Context, menu *entity.Menu) error {
	return r.db.WithContext(ctx).Save(menu).Error
}

func (r *menuRepository) Delete(ctx context.Context, id uint) error {
	return r.db.WithContext(ctx).Delete(&entity.Menu{}, id).Error
}

func (r *menuRepository) List(ctx context.Context) ([]entity.Menu, error) {
	var menus []entity.Menu
	err := r.db.WithContext(ctx).Order("sort ASC, id ASC").Find(&menus).Error
	return menus, err
}

func (r *menuRepository) GetTree(ctx context.Context) ([]dto.MenuTreeResponse, error) {
	var menus []entity.Menu
	if err := r.db.WithContext(ctx).Where("status = ?", 1).Order("sort ASC, id ASC").Find(&menus).Error; err != nil {
		return nil, err
	}
	
	return util.BuildMenuTree(menus), nil
}

func (r *menuRepository) GetByUserID(ctx context.Context, userID uint) ([]entity.Menu, error) {
	var menus []entity.Menu
	err := r.db.WithContext(ctx).
		Joins("LEFT JOIN role_menus ON role_menus.menu_id = sys_menu.id").
		Joins("LEFT JOIN user_roles ON user_roles.role_id = role_menus.role_id").
		Where("user_roles.user_id = ? AND sys_menu.status = ?", userID, 1).
		Order("sys_menu.sort ASC").
		Find(&menus).Error
	return menus, err
}
