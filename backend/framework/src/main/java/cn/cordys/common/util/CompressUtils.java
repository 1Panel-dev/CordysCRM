package cn.cordys.common.util;

import org.apache.commons.codec.binary.Base64;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.*;

public class CompressUtils {

    private CompressUtils() {
    }

    /***
     * Zip压缩
     *
     * @param data 待压缩数据
     * @return 压缩后数据
     */
    public static Object zip(Object data) {
        if (!(data instanceof byte[] temp)) {
            return data;
        }

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(bos)) {
            ZipEntry entry = new ZipEntry("zip");
            zip.putNextEntry(entry);
            zip.write(temp);
            zip.closeEntry();
            return bos.toByteArray();
        } catch (Exception e) {
            return data;
        }
    }


    private static File getFile(String filePath) throws IOException {
        // 创建文件对象
        File file = new File(filePath);

        // 如果文件不存在，则尝试创建
        if (!file.exists() && !file.createNewFile()) {
            throw new IOException("Failed to create the file: " + filePath);
        }

        // 返回文件
        return file;
    }

    public static FileOutputStream getFileStream(File file) throws FileNotFoundException {
        return new FileOutputStream(file);
    }

    private static void zipFile(File file, ZipOutputStream zipOutputStream) throws IOException {
        if (file.exists() && file.isFile()) {
            try (FileInputStream fis = new FileInputStream(file);
                 BufferedInputStream bis = new BufferedInputStream(fis)) {
                ZipEntry entry = new ZipEntry(file.getName());
                zipOutputStream.putNextEntry(entry);

                // 使用固定大小的缓冲区
                final int BUFFER_SIZE = 10 * 1024 * 1024; // 10MB
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;

                // 循环读取数据并写入压缩流
                while ((bytesRead = bis.read(buffer)) != -1) {
                    zipOutputStream.write(buffer, 0, bytesRead);
                }

                zipOutputStream.closeEntry(); // 关闭当前 ZipEntry
            }
        }
    }


    /**
     * 将多个文件压缩
     *
     * @param zipFilePath 压缩文件所在路径
     * @param fileList    要压缩的文件
     */
    public static File zipFiles(String zipFilePath, List<File> fileList) throws IOException {
        File zipFile = getFile(zipFilePath);
        // 文件输出流
        FileOutputStream outputStream = getFileStream(zipFile);
        // 压缩流
        ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);

        // 压缩列表中的文件
        for (File file : fileList) {
            zipFile(file, zipOutputStream);
        }
        // 关闭压缩流、文件流
        zipOutputStream.close();
        outputStream.close();
        return zipFile;
    }

    /**
     * 将多个文件压缩至指定路径
     *
     * @param fileList    待压缩的文件列表
     * @param zipFilePath 压缩文件路径
     * @return 返回压缩好的文件
     */
    public static File zipFilesToPath(String zipFilePath, List<File> fileList) throws IOException {
        File zipFile = new File(zipFilePath);

        // 创建输出流和压缩流
        try (FileOutputStream outputStream = new FileOutputStream(zipFile);
             ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {

            // 遍历文件列表进行压缩
            for (File file : fileList) {
                if (file != null && file.exists() && file.isFile()) {
                    zipFile(file, zipOutputStream);
                }
            }
        }

        return zipFile;
    }


    /***
     * Zip解压
     *
     * @param data 待解压数据
     * @return 解压后数据
     */
    public static Object unzip(Object data) {
        if (!(data instanceof byte[] temp)) {
            return data;
        }

        try (ByteArrayInputStream bis = new ByteArrayInputStream(temp);
             ZipInputStream zip = new ZipInputStream(bis)) {

            ByteArrayOutputStream bas = new ByteArrayOutputStream();

            // 处理压缩包中的每个条目
            while (zip.getNextEntry() != null) {
                byte[] buf = new byte[1024];
                int num;

                // 读取数据到缓冲区并写入输出流
                while ((num = zip.read(buf)) != -1) {
                    bas.write(buf, 0, num);
                }
            }

            return bas.toByteArray();
        } catch (Exception e) {
            return data;
        }
    }


    public static Object zipString(Object data) {
        if (!(data instanceof String)) {
            return data;
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            try (DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(out)) {
                deflaterOutputStream.write(((String) data).getBytes(StandardCharsets.UTF_8));
            }
            return Base64.encodeBase64String(out.toByteArray());
        } catch (Exception e) {
            return data;
        }
    }

    public static Object unzipString(Object data) {
        if (!(data instanceof String)) {
            return data;
        }
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            try (OutputStream outputStream = new InflaterOutputStream(os)) {
                outputStream.write(Base64.decodeBase64((String) data));
            }
            return os.toString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            return data;
        }
    }
}
