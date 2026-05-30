package cn.cordys.crm.contract.excel.domain;

import cn.cordys.excel.domain.ExcelDataFactory;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;


public class BusinessTitleExcelDataFactory implements ExcelDataFactory {

    @Override
    public Class<?> getExcelDataByLocal() {
        Locale locale = LocaleContextHolder.getLocale();
        if (Locale.US.toString().equalsIgnoreCase(locale.toString())) {
            return BusinessTitleExcelDataUs.class;
        }
        return BusinessTitleExcelDataCn.class;
    }

    public BusinessTitleExcelData getExcelDataLocal() {
        Locale locale = LocaleContextHolder.getLocale();
        if (Locale.US.toString().equalsIgnoreCase(locale.toString())) {
            return new BusinessTitleExcelDataUs();
        }
        return new BusinessTitleExcelDataCn();
    }

}
