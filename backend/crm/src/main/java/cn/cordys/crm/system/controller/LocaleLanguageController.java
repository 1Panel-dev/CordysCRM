package cn.cordys.crm.system.controller;

import cn.cordys.crm.system.dto.request.LocaleLanguageRequest;
import cn.cordys.crm.system.service.LocalLanguageService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "本地语言")
@RestController
public class LocaleLanguageController {

  @Resource private LocalLanguageService localLanguageService;

  @PostMapping("/locale-language/change")
  @Operation(summary = "修改本地语言")
  public void localeLanguageChange(@Validated @RequestBody LocaleLanguageRequest request) {
    localLanguageService.localeLanguageChange(request, SessionUtils.getUserId());
  }
}
