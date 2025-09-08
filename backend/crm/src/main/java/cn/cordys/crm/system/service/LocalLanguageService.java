package cn.cordys.crm.system.service;

import cn.cordys.crm.system.domain.User;
import cn.cordys.crm.system.dto.request.LocaleLanguageRequest;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class LocalLanguageService {

    @Resource
    private BaseMapper<User> userMapper;


    public void localeLanguageChange(LocaleLanguageRequest request, String userId) {
        User user = new User();
        user.setId(userId);
        user.setLanguage(request.getLanguage());
        userMapper.update(user);
    }
}
