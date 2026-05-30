package handler

import (
	"crm/internal/handler/customer"
)

// Handlers 所有 Handler 的集合
type Handlers struct {
	Customer *customer.CustomerHandler
	// 添加其他 Handler
	// Opportunity *opportunity.OpportunityHandler
	// Contract    *contract.ContractHandler
	// Order       *order.OrderHandler
}
