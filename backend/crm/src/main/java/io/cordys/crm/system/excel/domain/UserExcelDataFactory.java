package io.cordys.crm.system.excel.domain;

import io.cordys.excel.domain.ExcelDataFactory;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

/**
 * @author wx
 */
public class UserExcelDataFactory implements ExcelDataFactory {

    @Override
    public Class getExcelDataByLocal() {
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
