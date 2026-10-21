package cn.cordys.crm.form.mapper;

import cn.cordys.crm.form.domain.CustomForm;
import org.apache.ibatis.annotations.Param;

/**
 * @author jianxing
 */
public interface ExtCustomFormMapper {
    boolean checkAddExist(@Param("customForm") CustomForm customForm);

    boolean checkUpdateExist(@Param("customForm") CustomForm customForm);
}
