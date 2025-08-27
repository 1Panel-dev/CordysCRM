package cn.cordys.listener;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 自定义错误控制器类，用于处理应用中的错误页面请求。
 * <p>
 * 该控制器会将所有错误页面的请求重定向到网站的根页面（"/"）。
 * </p>
 */
@Controller
public class CustomError implements ErrorController {

    /**
     * 错误处理方法，当发生错误时，会将请求重定向到根页面。
     *
     * @return 重定向到根页面
     */
    @GetMapping("/error")
    public String redirectRoot() {
        return "redirect:/";
    }
}
