package io.cordys.file.engine;

import lombok.Data;

/**
 * 复制文件请求类，表示文件复制操作的请求参数。
 * <p>
 * 该类继承自 {@link FileRequest}，包含了复制文件时需要的相关信息，如目标目录和文件名称。
 * </p>
 */
@Data
public class FileCopyRequest extends FileRequest {

    /**
     * 复制的目标文件夹路径。
     * <p>
     * 此字段指示文件应该被复制到的目标目录。必须提供有效的目录路径。
     * </p>
     */
    private String copyFolder;

    /**
     * 复制的文件名称。
     * <p>
     * 此字段指示复制后的文件名称。可以与原文件名称相同或不同。
     * </p>
     */
    private String copyfileName;
}
