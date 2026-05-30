package cn.cordys.file.engine;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件请求类，封装了文件相关的请求信息。
 * <p>
 * 该类用于存储与文件操作相关的基本信息，如文件所在文件夹、存储类型和文件名等。
 * </p>
 */
@Data
@NoArgsConstructor
public class FileRequest {

    /**
     * 文件所在的文件夹路径。
     * <p>
     * 此字段指示文件存储的目录，通常用于定位文件。
     * </p>
     */
    private String folder;

    /**
     * 存储类型，指示文件存储的方式或位置。
     * <p>
     * 例如，可以是 "S3"、"LOCAL" 等存储类型的标识符。
     * </p>
     */
    private String storage;

    /**
     * 文件名称。
     * <p>
     * 此字段指示文件的名称，通常与文件的后缀（如 .txt、.jpg 等）一起确定文件的完整标识。
     * </p>
     */
    private String fileName;

    /**
     * 构造函数，创建一个包含文件夹路径、存储类型和文件名的文件请求对象。
     *
     * @param folder   文件所在的文件夹路径。
     * @param storage  存储类型，指示文件存储的位置或方式。
     * @param fileName 文件的名称。
     */
    public FileRequest(String folder, String storage, String fileName) {
        this.folder = folder;
        this.storage = storage;
        this.fileName = fileName;
    }
}
