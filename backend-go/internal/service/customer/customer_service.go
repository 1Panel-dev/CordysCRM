package customer

import (
	"context"
	"errors"
	"fmt"

	"github.com/google/uuid"
	"crm/internal/model/dto"
	"crm/internal/model/entity"
	customerrepo "crm/internal/repository/customer"
	"crm/internal/middleware/datascop"
)

// CustomerService 客户服务
type CustomerService struct {
	repo      *customerrepo.CustomerRepository
	dataScope *datascop.DataScopeService
}

// NewCustomerService 创建客户服务实例
func NewCustomerService(repo *customerrepo.CustomerRepository, ds *datascop.DataScopeService) *CustomerService {
	return &CustomerService{
		repo:      repo,
		dataScope: ds,
	}
}

// List 客户列表
func (s *CustomerService) List(ctx context.Context, req *dto.CustomerPageRequest, userID, orgID string) (*dto.Pager[dto.CustomerListResponse], error) {
	// 获取数据权限
	scope := s.dataScope.GetDeptDataPermission(userID, orgID, req.ViewID, "customer:read")

	// 构建查询
	query := s.repo.Query(ctx)
	query = scope.Apply(query)

	// 应用筛选条件
	if req.Name != "" {
		query = query.Where("name LIKE ?", "%"+req.Name+"%")
	}
	if req.Owner != "" {
		query = query.Where("owner = ?", req.Owner)
	}
	if req.InPool != nil {
		query = query.Where("in_shared_pool = ?", *req.InPool)
	}

	// 统计总数
	total, err := s.repo.Count(ctx, query)
	if err != nil {
		return nil, fmt.Errorf("failed to count customers: %w", err)
	}

	// 分页查询
	var customers []entity.Customer
	err = query.Offset(req.GetOffset()).
		Limit(req.PageSize).
		Order("created_at DESC").
		Find(&customers).Error
	if err != nil {
		return nil, fmt.Errorf("failed to list customers: %w", err)
	}

	// 转换为 DTO
	list := make([]dto.CustomerListResponse, len(customers))
	for i, c := range customers {
		list[i] = dto.ToCustomerListResponse(&c)
	}

	return &dto.Pager[dto.CustomerListResponse]{
		List:     list,
		Total:    total,
		Current:  req.Current,
		PageSize: req.PageSize,
	}, nil
}

// GetWithDataPermission 带权限检查的详情查询
func (s *CustomerService) GetWithDataPermission(ctx context.Context, id, userID, orgID string) (*dto.CustomerGetResponse, error) {
	customer, err := s.repo.GetByID(ctx, id)
	if err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			return nil, errors.New("customer not found")
		}
		return nil, fmt.Errorf("failed to get customer: %w", err)
	}

	// 检查数据权限
	if !s.hasReadPermission(customer, userID, orgID) {
		return nil, errors.New("permission denied")
	}

	return dto.ToCustomerGetResponse(customer), nil
}

// Create 创建客户
func (s *CustomerService) Create(ctx context.Context, req *dto.CustomerAddRequest, userID, orgID string) (*entity.Customer, error) {
	customer := &entity.Customer{
		ID:             uuid.New().String(),
		Name:           req.Name,
		Owner:          userID,
		OrganizationID: orgID,
		InSharedPool:   false,
	}

	// 填充动态字段
	if req.CustomFields != nil {
		customer.CustomFields = req.CustomFields
	}

	err := s.repo.Create(ctx, customer)
	if err != nil {
		return nil, fmt.Errorf("failed to create customer: %w", err)
	}

	// TODO: 记录操作日志

	return customer, nil
}

// Update 更新客户
func (s *CustomerService) Update(ctx context.Context, req *dto.CustomerUpdateRequest, userID, orgID string) (*entity.Customer, error) {
	customer, err := s.repo.GetByID(ctx, req.ID)
	if err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			return nil, errors.New("customer not found")
		}
		return nil, fmt.Errorf("failed to get customer: %w", err)
	}

	// 检查数据权限
	if !s.hasWritePermission(customer, userID, orgID) {
		return nil, errors.New("permission denied")
	}

	// 更新字段
	if req.Name != "" {
		customer.Name = req.Name
	}
	if req.CustomFields != nil {
		customer.CustomFields = req.CustomFields
	}

	err = s.repo.Update(ctx, customer)
	if err != nil {
		return nil, fmt.Errorf("failed to update customer: %w", err)
	}

	return customer, nil
}

// Delete 删除客户
func (s *CustomerService) Delete(ctx context.Context, id, userID, orgID string) error {
	customer, err := s.repo.GetByID(ctx, id)
	if err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			return errors.New("customer not found")
		}
		return fmt.Errorf("failed to get customer: %w", err)
	}

	// 检查数据权限
	if !s.hasDeletePermission(customer, userID, orgID) {
		return errors.New("permission denied")
	}

	err = s.repo.Delete(ctx, id)
	if err != nil {
		return fmt.Errorf("failed to delete customer: %w", err)
	}

	return nil
}

// BatchTransfer 批量转移客户
func (s *CustomerService) BatchTransfer(ctx context.Context, req *dto.CustomerBatchTransferRequest, userID, orgID string) error {
	// 检查权限
	for _, id := range req.IDs {
		customer, err := s.repo.GetByID(ctx, id)
		if err != nil {
			return fmt.Errorf("customer %s not found: %w", id, err)
		}
		if !s.hasTransferPermission(customer, userID, orgID) {
			return errors.New("permission denied for some customers")
		}
	}

	err := s.repo.BatchTransfer(ctx, req.IDs, req.NewOwner)
	if err != nil {
		return fmt.Errorf("failed to batch transfer: %w", err)
	}

	return nil
}

// BatchDelete 批量删除客户
func (s *CustomerService) BatchDelete(ctx context.Context, ids []string, userID, orgID string) error {
	// 检查权限
	for _, id := range ids {
		customer, err := s.repo.GetByID(ctx, id)
		if err != nil {
			continue
		}
		if !s.hasDeletePermission(customer, userID, orgID) {
			return errors.New("permission denied for some customers")
		}
	}

	err := s.repo.BatchDelete(ctx, ids)
	if err != nil {
		return fmt.Errorf("failed to batch delete: %w", err)
	}

	return nil
}

// hasReadPermission 检查读权限
func (s *CustomerService) hasReadPermission(c *entity.Customer, userID, orgID string) bool {
	// 实现权限逻辑：负责人、协作人、上级等
	return c.Owner == userID || c.OrganizationID == orgID
}

// hasWritePermission 检查写权限
func (s *CustomerService) hasWritePermission(c *entity.Customer, userID, orgID string) bool {
	return c.Owner == userID
}

// hasDeletePermission 检查删除权限
func (s *CustomerService) hasDeletePermission(c *entity.Customer, userID, orgID string) bool {
	return c.Owner == userID
}

// hasTransferPermission 检查转移权限
func (s *CustomerService) hasTransferPermission(c *entity.Customer, userID, orgID string) bool {
	return c.Owner == userID
}
