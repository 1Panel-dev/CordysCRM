package io.cordys.file.engine;

import io.cordys.common.exception.GenericException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.util.FileUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * 本地文件仓库实现，提供本地文件的存储、删除、获取等操作。
 * <p>
 * 该类实现了 {@link FileRepository} 接口，具体实现了保存、删除、获取文件等方法，适用于本地存储的场景。
 * </p>
 */
@Component
public class LocalRepository implements FileRepository {

    /**
     * 保存文件到本地存储。
     *
     * @param multipartFile 要保存的文件，类型为 {@link MultipartFile}。
     * @param request       文件请求信息，包含文件的存储路径、文件名等信息。
     * @return 返回保存后的文件路径。
     * @throws IOException 如果保存文件过程中发生 I/O 错误，抛出异常。
     */
    @Override
    public String saveFile(MultipartFile multipartFile, FileRequest request) throws IOException {
        if (multipartFile == null || request == null || StringUtils.isEmpty(request.getFileName()) || StringUtils.isEmpty(request.getFolder())) {
            return null;
        }
        FileValidate.validateFileName(request.getFolder(), request.getFileName());
        createFileDir(request);
        File file = new File(getFilePath(request));
        FileUtils.copyInputStreamToFile(multipartFile.getInputStream(), file);
        return file.getPath();
    }

    /**
     * 创建文件夹目录。
     *
     * @param request 文件请求信息，包含文件夹路径信息。
     * @throws RuntimeException 如果创建目录失败，抛出运行时异常。
     */
    private void createFileDir(FileRequest request) {
        String dir = getFileDir(request);
        File fileDir = new File(dir);
        if (!fileDir.exists() && !fileDir.mkdirs()) {
            throw new RuntimeException("Failed to create directory: " + dir);
        }
    }

    /**
     * 保存字节数组表示的文件到本地存储。
     *
     * @param bytes   文件的字节数组。
     * @param request 文件请求信息，包含文件存储路径、文件名等。
     * @throws IOException 如果保存文件过程中发生 I/O 错误，抛出异常。
     */
    @Override
    public void saveFile(byte[] bytes, FileRequest request) throws IOException {
        File file = new File(getFilePath(request));
        // 检查父目录是否存在，如果不存在则创建它
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            boolean dirsCreated = parentDir.mkdirs(); // 创建父目录
            if (!dirsCreated) {
                throw new IOException("Failed to create directories: " + parentDir.getAbsolutePath());
            }
        }
        try (OutputStream ops = new FileOutputStream(file)) {
            ops.write(bytes);
        }
    }

    /**
     * 保存输入流表示的文件到本地存储。
     *
     * @param inputStream 文件内容的输入流。
     * @param request     文件请求信息，包含文件存储路径、文件名等。
     * @return 返回保存后的文件路径。
     * @throws Exception 如果保存文件过程中发生错误，抛出异常。
     */
    @Override
    public String saveFile(InputStream inputStream, FileRequest request) throws Exception {
        File file = new File(getFilePath(request));
        FileUtils.copyInputStreamToFile(inputStream, file);
        return file.getPath();
    }

    /**
     * 删除文件。
     *
     * @param request 文件请求信息，包含待删除文件的路径、文件名等。
     * @throws Exception 如果删除文件过程中发生错误，抛出异常。
     */
    @Override
    public void delete(FileRequest request) throws Exception {
        String path = StringUtils.join(getFilePath(request));
        File file = new File(path);
        FileUtil.deleteContents(file);
        if (file.exists() && !file.delete()) {
            throw new RuntimeException("Failed to delete file: " + path);
        }
    }

    /**
     * 删除指定文件夹及其所有内容。
     *
     * @param request 文件请求信息，包含待删除文件夹路径的文件请求。
     * @throws Exception 如果删除文件夹过程中发生错误，抛出异常。
     */
    @Override
    public void deleteFolder(FileRequest request) throws Exception {
        FileValidate.validateFileName(request.getFolder(), request.getFileName());
        this.delete(request);
    }

    /**
     * 获取文件的字节内容。
     *
     * @param request 文件请求信息，包含文件路径和文件名。
     * @return 返回文件的字节数组。
     * @throws Exception 如果获取文件内容过程中发生错误，抛出异常。
     */
    @Override
    public byte[] getFile(FileRequest request) throws Exception {
        File file = new File(getFilePath(request));
        return Files.readAllBytes(file.toPath());
    }

    /**
     * 获取文件的输入流。
     *
     * @param request 文件请求信息，包含文件路径和文件名。
     * @return 返回文件内容的输入流。
     * @throws Exception 如果获取文件输入流过程中发生错误，抛出异常。
     */
    @Override
    public InputStream getFileAsStream(FileRequest request) throws Exception {
        return new FileInputStream(getFilePath(request));
    }

    /**
     * 下载文件到指定的本地路径（未实现）。
     *
     * @param request   文件请求信息，包含待下载文件的路径。
     * @param localPath 下载到本地的路径。
     * @throws UnsupportedOperationException 本方法未实现，调用时会抛出异常。
     */
    @Override
    public void downloadFile(FileRequest request, String localPath) {
        throw new UnsupportedOperationException("Download file is not supported in LocalFileRepository.");
    }

    /**
     * 获取指定文件夹下的文件名列表（未实现）。
     *
     * @param request 文件请求信息，包含文件夹路径。
     * @return 返回文件夹中文件的文件名列表。
     */
    @Override
    public List<String> getFolderFileNames(FileRequest request) {
        return null;
        // 返回 null 或者未实现逻辑
    }

    /**
     * 获取指定文件夹下的文件列表
     * @param request 文件请求信息，包含目标文件夹的标识符或路径。
     * @return 文件列表
     */
    @Override
    public List<File> getFolderFiles(FileRequest request) {
        File file = new File(getFilePath(request));
        if (!file.isDirectory()) {
            throw new GenericException("The specified path is not a directory.");
        }
        File[] files = file.listFiles();
        return files != null ? List.of(files) : null;
    }

    /**
     * 复制文件到指定目录
     *
     * @param request 文件复制请求信息。
     * @throws GenericException 如果复制文件时发生不支持的操作，抛出系统异常。
     */
    @Override
    public void copyFile(FileCopyRequest request) throws Exception {
        Files.createDirectories(Paths.get(getFolderWithDefaultDir(request.getDestFolder())));
        Files.copy(Paths.get(getFolderWithDefaultDir(request.getSourceFolder()), request.getFileName()),
                Paths.get(getFolderWithDefaultDir(request.getDestFolder()), request.getFileName()), StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * 获取指定文件的大小。
     *
     * @param request 文件请求信息，包含待获取大小的文件路径和文件名。
     * @return 返回文件的大小（字节数）。
     * @throws Exception 如果获取文件大小过程中发生错误，抛出异常。
     */
    @Override
    public long getFileSize(FileRequest request) throws Exception {
        File file = new File(getFilePath(request));
        return file.length();
    }

    /**
     * 获取文件的完整路径。
     *
     * @param request 文件请求信息，包含文件夹路径和文件名。
     * @return 返回文件的完整路径。
     */
    private String getFilePath(FileRequest request) {
        FileValidate.validateFileName(request.getFolder(), request.getFileName());
        return StringUtils.join(DefaultRepositoryDir.getDefaultDir(), getFileDir(request), "/", request.getFileName());
    }

    /**
     * 获取文件所在的文件夹路径。
     *
     * @param request 文件请求信息，包含文件夹路径。
     * @return 返回文件夹路径。
     */
    private String getFileDir(FileRequest request) {
        FileValidate.validateFileName(request.getFolder(), request.getFileName());
        return request.getFolder();
    }

    private String getFolderWithDefaultDir(String folder) {
        return StringUtils.join(DefaultRepositoryDir.getDefaultDir(), folder);
    }
}
