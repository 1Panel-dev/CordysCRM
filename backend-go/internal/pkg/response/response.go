package response

import (
	"net/http"

	"github.com/gin-gonic/gin"
)

// Response 统一响应结构
type Response[T any] struct {
	Code    int         `json:"code"`
	Message string      `json:"message"`
	Data    T           `json:"data,omitempty"`
}

// APIError API 错误
type APIError struct {
	Code    int
	Message string
	Err     error
}

func (e *APIError) Error() string {
	return e.Message
}

func (e *APIError) WithError(err error) *APIError {
	e.Err = err
	return e
}

func (e *APIError) WithMessage(msg string) *APIError {
	e.Message = msg
	return e
}

// 预定义错误码
var (
	ErrSuccess          = &APIError{Code: 0, Message: "success"}
	ErrInvalidParams    = &APIError{Code: 400, Message: "invalid parameters"}
	ErrUnauthorized     = &APIError{Code: 401, Message: "unauthorized"}
	ErrPermissionDenied = &APIError{Code: 403, Message: "permission denied"}
	ErrNotFound         = &APIError{Code: 404, Message: "resource not found"}
	ErrInternalServer   = &APIError{Code: 500, Message: "internal server error"}
)

// Success 成功响应
func Success[T any](c *gin.Context, data T) {
	c.JSON(http.StatusOK, Response[T]{
		Code:    ErrSuccess.Code,
		Message: ErrSuccess.Message,
		Data:    data,
	})
}

// Error 错误响应
func Error(c *gin.Context, err *APIError) {
	c.JSON(http.StatusOK, Response[interface{}]{
		Code:    err.Code,
		Message: err.Message,
	})
}

// ErrorResponse HTTP 状态码错误响应
func ErrorResponse(c *gin.Context, statusCode int, message string) {
	c.JSON(statusCode, gin.H{
		"error": message,
	})
}
