package cn.cordys.crm.form.domain;

import jakarta.persistence.Table;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Table(name = "custom_form_role_user")
public class CustomFormRoleUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String id;
    private String roleId;
    private String userId;
    private String customFormId;
}
