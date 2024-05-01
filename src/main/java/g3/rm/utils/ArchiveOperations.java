package g3.rm.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ArchiveOperations {

    private final Logger LOGGER = LogManager.getLogger("ArchiveOperation");
    private int parent = 0;

    public int zip(String dirPath, String zipPath) {
        File currDir = new File(dirPath);
        if (!currDir.exists())
            return -1;
        try ( FileOutputStream fileOutputStream = new FileOutputStream(zipPath);
              ZipOutputStream zipOutputStream =
                      new ZipOutputStream(new BufferedOutputStream(fileOutputStream, 2048 * 16))) {
            String absolutePath = currDir.getAbsolutePath();
            parent = absolutePath.length() + 1;
            zScanner(currDir, zipOutputStream, zipPath);
        } catch (Exception ex) {
            LOGGER.error("Error while zip data: " + ex.getMessage(), ex);
            return -1;
        }
        return 0;
    }

    private void zScanner(File dirPath, ZipOutputStream zipOutputStream, String newZipPath) {
        try {
            File zipFile = new File(newZipPath);
            File[] listFiles = dirPath.listFiles();
            if (listFiles == null) {
                return;
            }
            for (File listItem : listFiles){
                if (listItem.isDirectory()) {
                    zScanner(listItem, zipOutputStream, newZipPath);
                } else {
                    if (listItem.getName().equals(zipFile.getName())){
                        continue;
                    }
                    String absolutePath = listItem.getAbsolutePath();
                    try (FileInputStream fis = new FileInputStream(absolutePath);
                         BufferedInputStream in = new BufferedInputStream(fis)) {
                        byte[] data = new byte[1024];
                        int bytesRead;
                        String entryName = absolutePath.substring(this.parent);
                        zipOutputStream.putNextEntry(new ZipEntry(entryName));
                        while ((bytesRead = in.read(data)) != -1)
                            zipOutputStream.write(data, 0, bytesRead);
                    }
                }
                listItem.delete();
            }
        } catch (Exception ex) {
            LOGGER.error("Error while scanning data: " + ex.getMessage(), ex);
        }
    }

    public int unzip(String zipPath, String dirPath) {
        File zipFile = new File(zipPath);
        File outDir = new File(dirPath);
        if (!zipFile.exists() || !outDir.exists()) {
            return -1;
        }
        String absolutePath = outDir.getAbsolutePath();
        try (FileInputStream inputStream = new FileInputStream(zipPath);
             ZipInputStream zipInputStream = new ZipInputStream(new BufferedInputStream(inputStream))) {
            ZipEntry ze;

            while ((ze = zipInputStream.getNextEntry()) != null){
                String newFile = absolutePath + File.separator + ze.getName();
                File fdf = new File(newFile);
                String parent = fdf.getParent();
                File parentDir = new File(parent);
                if (!parentDir.exists())
                    parentDir.mkdirs();
                if (ze.isDirectory()){
                    fdf.mkdirs();
                } else {
                    fdf.createNewFile();
                    FileOutputStream f = new FileOutputStream(fdf);
                    byte[] c = new byte[512];
                    int cc;
                    while ((cc = zipInputStream.read(c)) != -1)
                        f.write(c, 0, cc);
                    f.close();
                }
            }
            zipFile.delete();
        } catch (Exception ex) {
            LOGGER.error("Error while unzip data: " + ex.getMessage(), ex);
            return -1;
        }
        return 0;
    }
}
