package cn.cordys.crm.form.domain;

import cn.cordys.common.domain.BaseResourceField;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "custom_form_data_field_blob")
public class CustomFormDataFieldBlob extends BaseResourceField {
}
