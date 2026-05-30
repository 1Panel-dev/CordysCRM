package customerrepo

import (
	"context"

	"gorm.io/gorm"
	"crm/internal/model/entity"
)

// CustomerRepository 客户数据访问层
type CustomerRepository struct {
	db *gorm.DB
}

// NewCustomerRepository 创建客户仓库实例
func NewCustomerRepository(db *gorm.DB) *CustomerRepository {
	return &CustomerRepository{db: db}
}

// DB 获取数据库连接
func (r *CustomerRepository) DB() *gorm.DB {
	return r.db
}

// Query 构建查询
func (r *CustomerRepository) Query(ctx context.Context) *gorm.DB {
	return r.db.WithContext(ctx).Model(&entity.Customer{})
}

// GetByID 根据 ID 查询
func (r *CustomerRepository) GetByID(ctx context.Context, id string) (*entity.Customer, error) {
	var customer entity.Customer
	err := r.db.WithContext(ctx).First(&customer, "id = ?", id).Error
	if err != nil {
		return nil, err
	}
	return &customer, nil
}

// GetByIDs 批量查询
func (r *CustomerRepository) GetByIDs(ctx context.Context, ids []string) ([]entity.Customer, error) {
	var customers []entity.Customer
	err := r.db.WithContext(ctx).Where("id IN ?", ids).Find(&customers).Error
	if err != nil {
		return nil, err
	}
	return customers, nil
}

// Create 创建记录
func (r *CustomerRepository) Create(ctx context.Context, customer *entity.Customer) error {
	return r.db.WithContext(ctx).Create(customer).Error
}

// Update 更新记录
func (r *CustomerRepository) Update(ctx context.Context, customer *entity.Customer) error {
	return r.db.WithContext(ctx).Save(customer).Error
}

// Delete 删除记录
func (r *CustomerRepository) Delete(ctx context.Context, id string) error {
	return r.db.WithContext(ctx).Delete(&entity.Customer{}, "id = ?", id).Error
}

// BatchDelete 批量删除
func (r *CustomerRepository) BatchDelete(ctx context.Context, ids []string) error {
	return r.db.WithContext(ctx).Delete(&entity.Customer{}, "id IN ?", ids).Error
}

// BatchTransfer 批量转移负责人
func (r *CustomerRepository) BatchTransfer(ctx context.Context, ids []string, newOwner string) error {
	return r.db.WithContext(ctx).
		Model(&entity.Customer{}).
		Where("id IN ?", ids).
		Update("owner", newOwner).Error
}

// BatchUpdateToPool 批量移入公海
func (r *CustomerRepository) BatchUpdateToPool(ctx context.Context, ids []string, poolID string, reasonID *string) error {
	updates := map[string]interface{}{
		"in_shared_pool": true,
		"pool_id":        poolID,
	}
	if reasonID != nil {
		updates["reason_id"] = *reasonID
	}
	return r.db.WithContext(ctx).
		Model(&entity.Customer{}).
		Where("id IN ?", ids).
		Updates(updates).Error
}

// Count 统计数量
func (r *CustomerRepository) Count(ctx context.Context, query *gorm.DB) (int64, error) {
	var total int64
	err := query.Count(&total).Error
	return total, err
}
