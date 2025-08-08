package io.cordys.common.resolver.field;


import io.cordys.common.util.JSON;
import io.cordys.crm.system.dto.field.InputMultipleField;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TextMultipleResolver extends AbstractModuleFieldResolver<InputMultipleField> {

    @Override
    public void validate(InputMultipleField inputMultipleField, Object value) {
        validateRequired(inputMultipleField, value);
        validateArray(inputMultipleField.getName(), value);
    }

    @Override
    public String parse2String(InputMultipleField inputMultipleField, Object value) {
        return getJsonString(value);
    }

    @Override
    public Object parse2Value(InputMultipleField inputMultipleField, String value) {
        return parse2Array(value);
    }


    @Override
	public Object trans2Value(InputMultipleField inputMultipleField, String value) {
        if (StringUtils.isEmpty(value)) {
            return StringUtils.EMPTY;
        }
        return String.join(",", JSON.parseArray(value));
    }

    @Override
    public Object text2Value(InputMultipleField field, String text) {
        if (StringUtils.isBlank(text) || StringUtils.equals(text, "[]")) {
            return StringUtils.EMPTY;
        }
        List<String> textList = parseFakeJsonArray(text);
        Set<String> correctTags = getCorrectFormatInput(textList);
        return CollectionUtils.isEmpty(correctTags) ? StringUtils.EMPTY : new ArrayList<>(correctTags);
    }

    /**
     * 要求标签最多10个, 且每个标签长度不超过64个字符
     * @param textList 标签列表
     * @return 符合要求的标签集合
     */
    private Set<String> getCorrectFormatInput(List<String> textList) {
        return textList.stream()
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .filter(text -> text.length() <= 64)
                .distinct()
                .limit(10)
                .collect(Collectors.toSet());
    }
}
