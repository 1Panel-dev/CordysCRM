package cn.cordys.file.engine;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;
import java.util.List;

public class S3Repository implements FileRepository {
    @Override
    public String saveFile(MultipartFile file, FileRequest request) throws Exception {
        return "";
    }

    @Override
    public void saveFile(byte[] bytes, FileRequest request) throws Exception {

    }

    @Override
    public String saveFile(InputStream inputStream, FileRequest request) throws Exception {
        return "";
    }

    @Override
    public void delete(FileRequest request) throws Exception {

    }

    @Override
    public void deleteFolder(FileRequest request, boolean onlyDir) throws Exception {

    }

    @Override
    public byte[] getFile(FileRequest request) throws Exception {
        return new byte[0];
    }

    @Override
    public InputStream getFileAsStream(FileRequest request) throws Exception {
        return null;
    }

    @Override
    public void downloadFile(FileRequest request, String localPath) throws Exception {

    }

    @Override
    public ResponseEntity<Resource> downloadFile(FileRequest request) throws Exception {
        return null;
    }

    @Override
    public List<String> getFolderFileNames(FileRequest request) throws Exception {
        return List.of();
    }

    @Override
    public List<File> getFolderFiles(FileRequest request) throws Exception {
        return List.of();
    }

    @Override
    public void copyFile(FileCopyRequest request) throws Exception {

    }

    @Override
    public long getFileSize(FileRequest request) throws Exception {
        return 0;
    }
}
