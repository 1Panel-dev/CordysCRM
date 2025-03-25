package io.cordys.file.engine;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 复制文件请求类，表示文件复制操作的请求参数。
 * <p>
 * 该类继承自 {@link FileRequest}，包含了复制文件时需要的相关信息，如目标目录和文件名称。
 * </p>
 */
@Data
@AllArgsConstructor
public class FileCopyRequest extends FileRequest {

    /**
     * 复制的文件来源目录。
     */
    private String sourceFolder;

    /**
     * 复制的文件目标目录。
     */
    private String destFolder;

    /**
     * 复制的文件名称。
     */
    private String fileName;
}
