package cn.cordys.file.engine;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.util.Translator;
import org.apache.commons.lang3.StringUtils;

import java.io.File;

public class FileValidate {

    /**
     * Validates the file names to ensure they do not contain illegal characters.
     *
     * @param fileNames The file names to validate.
     *
     * @throws GenericException if any file name is invalid.
     */
    public static void validateFileName(String... fileNames) {
        if (fileNames != null) {
            for (String fileName : fileNames) {
                if (StringUtils.isNotBlank(fileName) && fileName.contains("." + File.separator)) {
                    throw new GenericException(Translator.get("invalid_parameter"));
                }
            }
        }
    }
}
