package cn.cordys.crm.system.excel.domain;

import cn.cordys.excel.domain.ExcelDataFactory;
import java.util.Locale;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * @author wx
 */
public class UserExcelDataFactory implements ExcelDataFactory {

  @Override
  public Class<?> getExcelDataByLocal() {
    Locale locale = LocaleContextHolder.getLocale();
    if (Locale.US.toString().equalsIgnoreCase(locale.toString())) {
      return UserExcelDataUs.class;
    }
    return UserExcelDataCn.class;
  }

  public UserExcelData getUserExcelDataLocal() {
    Locale locale = LocaleContextHolder.getLocale();
    if (Locale.US.toString().equalsIgnoreCase(locale.toString())) {
      return new UserExcelDataUs();
    }
    return new UserExcelDataCn();
  }
}
