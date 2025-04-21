package io.cordys.common.response.handler;

import io.cordys.common.exception.GenericException;
import io.cordys.common.exception.IResultCode;
import io.cordys.common.response.result.CrmHttpResultCode;
import io.cordys.common.util.LogUtils;
import io.cordys.common.util.ServiceUtils;
import io.cordys.common.util.Translator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.UnauthorizedException;
import org.apache.shiro.lang.ShiroException;
import org.eclipse.jetty.io.EofException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器，处理各类异常并返回统一格式的错误响应。
 */
@RestControllerAdvice
public class RestControllerExceptionHandler {

    /**
     * 处理数据校验异常，返回具体字段的校验信息。
     *
     * @param ex MethodArgumentNotValidException 异常
     * @return ResultHolder 返回封装的错误信息，HTTP 状态码 400
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResultHolder handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return ResultHolder.error(CrmHttpResultCode.VALIDATE_FAILED.getCode(),
                CrmHttpResultCode.VALIDATE_FAILED.getMessage(), errors);
    }

    /**
     * 处理请求方法不支持的异常，返回 HTTP 状态码 405。
     *
     * @param response  HttpServletResponse 响应
     * @param exception 异常信息
     * @return ResultHolder 返回错误信息
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResultHolder handleHttpRequestMethodNotSupportedException(HttpServletResponse response, Exception exception) {
        response.setStatus(HttpStatus.METHOD_NOT_ALLOWED.value());
        return ResultHolder.error(HttpStatus.METHOD_NOT_ALLOWED.value(), exception.getMessage());
    }

    /**
     * 处理 MSException 异常，根据 errorCode 设置 HTTP 状态码和业务状态码。
     *
     * @param e MSException 异常
     * @return ResponseEntity 返回响应实体，包含错误信息
     */
    @ExceptionHandler(GenericException.class)
    public ResponseEntity<ResultHolder> handlerGenericException(GenericException e) {
        IResultCode errorCode = e.getErrorCode();
        if (errorCode == null) {
            // 未设置 errorCode，返回内部服务器错误
            return ResponseEntity.internalServerError()
                    .body(ResultHolder.error(CrmHttpResultCode.FAILED.getCode(), e.getMessage()));
        }

        int code = errorCode.getCode();
        String message = errorCode.getMessage();
        message = Translator.get(message, message);

        if (errorCode instanceof CrmHttpResultCode) {
            // 如果是 CrmHttpResultCode 类型，使用其状态码的后三位作为 HTTP 状态码
            if (errorCode.equals(CrmHttpResultCode.NOT_FOUND)) {
                message = getNotFoundMessage(message);
            }
            return ResponseEntity.status(code % 1000)
                    .body(ResultHolder.error(code, message, e.getMessage()));
        } else {
            // 其他类型的错误，返回 500 状态码
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResultHolder.error(code, Translator.get(message, message), e.getMessage()));
        }
    }

    /**
     * 处理 NOT_FOUND 异常，拼接资源名称以提供更详细的错误信息。
     *
     * @param message 错误信息模板
     * @return String 拼接后的错误信息
     */
    private static String getNotFoundMessage(String message) {
        String resourceName = ServiceUtils.getResourceName();
        if (StringUtils.isNotBlank(resourceName)) {
            message = String.format(message, Translator.get(resourceName, resourceName));
        } else {
            message = String.format(message, Translator.get("resource.name"));
        }
        ServiceUtils.clearResourceName();
        return message;
    }

    /**
     * 处理所有类型的异常，返回 HTTP 状态码 500 并格式化异常栈信息。
     *
     * @param e Exception 异常
     * @return ResponseEntity 返回响应实体，包含错误信息
     */
    @ExceptionHandler({Exception.class})
    public ResponseEntity<ResultHolder> handleException(Exception e) {
        return ResponseEntity.internalServerError()
                .body(ResultHolder.error(CrmHttpResultCode.FAILED.getCode(),
                        e.getMessage(), getStackTraceAsString(e)));
    }

    /**
     * 处理 EOF 异常，判断请求路径并返回适当的响应。
     *
     * @param request HttpServletRequest 请求
     * @param e       异常信息
     * @return ResponseEntity 返回响应实体，包含错误信息
     */
    @ExceptionHandler({EofException.class})
    public ResponseEntity<Object> handleEofException(HttpServletRequest request, Exception e) {
        String requestURI = request.getRequestURI();
        if (StringUtils.startsWith(requestURI, "/assets")
                || StringUtils.startsWith(requestURI, "/fonts")
                || StringUtils.startsWith(requestURI, "/images")
                || StringUtils.startsWith(requestURI, "/templates")) {
            return ResponseEntity.internalServerError().body(null);
        }
        return ResponseEntity.internalServerError()
                .body(ResultHolder.error(CrmHttpResultCode.FAILED.getCode(),
                        e.getMessage(), getStackTraceAsString(e)));
    }

    /**
     * 处理 Shiro 异常，返回 HTTP 状态码 401。
     *
     * @param request   HttpServletRequest 请求
     * @param response  HttpServletResponse 响应
     * @param exception 异常信息
     * @return ResultHolder 返回错误信息
     */
    @ExceptionHandler(ShiroException.class)
    public ResultHolder exceptionHandler(HttpServletRequest request, HttpServletResponse response, Exception exception) {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        return ResultHolder.error(HttpStatus.UNAUTHORIZED.value(), exception.getMessage());
    }

    /**
     * 处理 Shiro 未授权异常，返回 HTTP 状态码 403。
     *
     * @param request   HttpServletRequest 请求
     * @param response  HttpServletResponse 响应
     * @param exception 异常信息
     * @return ResultHolder 返回错误信息
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResultHolder unauthorizedExceptionHandler(HttpServletRequest request, HttpServletResponse response, Exception exception) {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        return ResultHolder.error(HttpStatus.FORBIDDEN.value(), exception.getMessage());
    }

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public ResultHolder asyncRequestNotUsableExceptionHandler(HttpServletRequest request, HttpServletResponse response, Exception exception) {
        return null;
    }


    /**
     * 格式化异常栈信息。
     *
     * @param e Exception 异常
     * @return String 异常栈的字符串表示
     */
    public static String getStackTraceAsString(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw, true));
        LogUtils.error(sw.toString());
        return sw.toString();
    }
}
