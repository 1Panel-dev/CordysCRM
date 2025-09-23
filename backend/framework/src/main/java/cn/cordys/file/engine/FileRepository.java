package cn.cordys.file.engine;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;
import java.util.List;

/**
 * 文件仓库接口，定义了文件操作（如保存、删除、下载、获取）的通用方法。
 * <p>
 * 实现该接口的类负责提供具体的文件存储和管理操作。
 * </p>
 */
public interface FileRepository {

    /**
     * 保存文件到文件仓库。
     *
     * @param file    要保存的文件，类型为 {@link MultipartFile}。
     * @param request 文件请求信息，包含文件的元数据。
     *
     * @return 返回保存后的文件标识符（通常为文件路径或唯一ID）。
     *
     * @throws Exception 如果保存文件过程中发生错误，抛出异常。
     */
    String saveFile(MultipartFile file, FileRequest request) throws Exception;

    /**
     * 保存字节数组表示的文件到文件仓库。
     *
     * @param bytes   文件内容的字节数组。
     * @param request 文件请求信息，包含文件的元数据。
     *
     * @throws Exception 如果保存文件过程中发生错误，抛出异常。
     */
    void saveFile(byte[] bytes, FileRequest request) throws Exception;

    /**
     * 保存文件流表示的文件到文件仓库。
     *
     * @param inputStream 文件内容的输入流。
     * @param request     文件请求信息，包含文件的元数据。
     *
     * @return 返回保存后的文件标识符（通常为文件路径或唯一ID）。
     *
     * @throws Exception 如果保存文件过程中发生错误，抛出异常。
     */
    String saveFile(InputStream inputStream, FileRequest request) throws Exception;

    /**
     * 删除指定文件。
     *
     * @param request 文件请求信息，包含待删除文件的标识符或路径。
     *
     * @throws Exception 如果删除文件过程中发生错误，抛出异常。
     */
    void delete(FileRequest request) throws Exception;

    /**
     * 删除指定文件夹及其所有内容。
     *
     * @param request 文件请求信息，包含待删除文件夹的标识符或路径。
     *
     * @throws Exception 如果删除文件夹过程中发生错误，抛出异常。
     */
    void deleteFolder(FileRequest request) throws Exception;

    /**
     * 获取指定文件的字节内容。对于大文件，不建议使用此方法。
     *
     * @param request 文件请求信息，包含待获取内容的文件标识符或路径。
     *
     * @return 返回文件的字节数组。
     *
     * @throws Exception 如果获取文件内容过程中发生错误，抛出异常。
     */
    byte[] getFile(FileRequest request) throws Exception;

    /**
     * 获取指定文件的输入流。用于流式处理文件内容。
     *
     * @param request 文件请求信息，包含待获取内容的文件标识符或路径。
     *
     * @return 返回文件内容的输入流。
     *
     * @throws Exception 如果获取文件输入流过程中发生错误，抛出异常。
     */
    InputStream getFileAsStream(FileRequest request) throws Exception;

    /**
     * 以流式方式下载文件，通过逐块下载的方式节省内存。
     *
     * @param request   文件请求信息，包含待下载的文件标识符或路径。
     * @param localPath 下载到本地的文件路径。
     *
     * @throws Exception 如果下载文件过程中发生错误，抛出异常。
     */
    void downloadFile(FileRequest request, String localPath) throws Exception;


    /**
     * 以流式方式下载文件，通过逐块下载的方式节省内存。
     *
     * @param request 文件请求信息，包含待下载的文件标识符或路径。
     *
     * @throws Exception 如果下载文件过程中发生错误，抛出异常。
     */
    ResponseEntity<Resource> downloadFile(FileRequest request) throws Exception;

    /**
     * 获取指定文件夹下所有文件的文件名列表。
     *
     * @param request 文件请求信息，包含目标文件夹的标识符或路径。
     *
     * @return 返回文件夹中所有文件的文件名列表。
     *
     * @throws Exception 如果获取文件夹文件列表过程中发生错误，抛出异常。
     */
    List<String> getFolderFileNames(FileRequest request) throws Exception;

    /**
     * 获取指定文件夹下所有文件的文件列表。
     *
     * @param request 文件请求信息，包含目标文件夹的标识符或路径。
     *
     * @return 返回文件夹中所有文件列表。
     *
     * @throws Exception 如果获取文件夹文件列表过程中发生错误，抛出异常。
     */
    List<File> getFolderFiles(FileRequest request) throws Exception;

    /**
     * 复制文件到指定目录。
     *
     * @param request 文件复制请求信息，包含源文件和目标目录信息。
     *
     * @throws Exception 如果复制文件过程中发生错误，抛出异常。
     */
    void copyFile(FileCopyRequest request) throws Exception;

    /**
     * 获取指定文件的大小（字节数）。
     *
     * @param request 文件请求信息，包含待获取大小的文件标识符或路径。
     *
     * @return 返回文件的大小，以字节为单位。
     *
     * @throws Exception 如果获取文件大小过程中发生错误，抛出异常。
     */
    long getFileSize(FileRequest request) throws Exception;
}
