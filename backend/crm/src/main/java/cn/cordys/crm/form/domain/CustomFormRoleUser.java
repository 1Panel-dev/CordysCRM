package cn.cordys.crm.form.domain;

import cn.cordys.common.domain.BaseModel;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "custom_form_role_user")
public class CustomFormRoleUser extends BaseModel {
    private String roleId;
    private String userId;
}
