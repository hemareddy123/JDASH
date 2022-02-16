package util.module;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FileManager {

    private static final Logger logger = LoggerFactory.getLogger(FileManager.class);

    public static boolean writeBytes(String fileName, byte[] data) {
        if (fileName == null) { return false; }

        BufferedOutputStream bufferedOutputStream = null;
        try {
            bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(fileName));
            bufferedOutputStream.write(data);
            return true;
        } catch (Exception e) {
            logger.warn("[FileManager] Fail to write the file. (fileName={})", fileName);
            return false;
        } finally {
            try {
                if (bufferedOutputStream != null) {
                    bufferedOutputStream.close();
                }
            } catch (Exception e) {
                logger.warn("[FileManager] Fail to close the buffer stream. (fileName={})", fileName);
            }
        }
    }

    public static byte[] readAllBytes(String fileName) {
        if (fileName == null) { return null; }

        BufferedInputStream bufferedInputStream = null;
        try {
            bufferedInputStream = new BufferedInputStream(new FileInputStream(fileName));
            return bufferedInputStream.readAllBytes();
        } catch (Exception e) {
            logger.warn("[FileManager] Fail to read the file. (fileName={})", fileName);
            return null;
        } finally {
            try {
                if (bufferedInputStream != null) {
                    bufferedInputStream.close();
                }
            } catch (IOException e) {
                logger.warn("[FileManager] Fail to close the buffer stream. (fileName={})", fileName);
            }
        }
    }

    public static List<String> readAllLines(String fileName) {
        if (fileName == null) { return null; }

        BufferedReader bufferedReader = null;
        List<String> lines = new ArrayList<>();
        try {
            bufferedReader = new BufferedReader(new FileReader(fileName));
            String line;
            while( (line = bufferedReader.readLine()) != null ) {
                lines.add(line);
            }
            return lines;
        } catch (Exception e) {
            logger.warn("[FileManager] Fail to read the file. (fileName={})", fileName);
            return lines;
        } finally {
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
            } catch (IOException e) {
                logger.warn("[FileManager] Fail to close the buffer reader. (fileName={})", fileName);
            }
        }
    }

    public static String concatFilePath(String from, String to) {
        if (from == null) { return null; }
        if (to == null) { return from; }

        String resultPath = from.trim();
        if (!to.startsWith(File.separator)) {
            if (!resultPath.endsWith(File.separator)) {
                resultPath += File.separator;
            }
        } else {
            if (resultPath.endsWith(File.separator)) {
                resultPath = resultPath.substring(0, resultPath.lastIndexOf("/"));
            }
        }

        resultPath += to.trim();
        return resultPath;
    }

    public static String getParentPathFromUri(String uri) {
        if (uri == null) { return null; }
        if (!uri.contains("/")) { return uri; }
        return uri.substring(0, uri.lastIndexOf("/")).trim();
    }

    public static String getFileNameWithExtensionFromUri(String uri) {
        if (uri == null) { return null; }
        if (!uri.contains("/")) { return uri; }

        int lastSlashIndex = uri.lastIndexOf("/");
        if (lastSlashIndex == (uri.length() - 1)) { return null; }
        return uri.substring(uri.lastIndexOf("/") + 1).trim();
    }

    public static String getFileNameFromUri(String uri) {
        uri = getFileNameWithExtensionFromUri(uri);
        if (uri == null) { return null; }
        if (!uri.contains(".")) { return uri; }

        uri = uri.substring(0, uri.lastIndexOf(".")).trim();
        return uri;
    }

    public static void deleteFile(String path) {
        File file = new File(path);
        if (!file.exists()) {
            logger.warn("[FileManager] Fail to delete the file. File is not exist. (path={})", path);
            return;
        }

        try {
            if (file.isDirectory()) {
                FileUtils.deleteDirectory(file);
            } else {
                FileUtils.delete(file);
            }
            logger.debug("[FileManager] Success to delete the file. (path={})", path);
        } catch (Exception e) {
            logger.warn("[FileManager] Fail to delete the file. (path={})", path, e);
        }
    }

}
