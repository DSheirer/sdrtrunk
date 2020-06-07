package io.github.dsheirer.util;

import org.rauschig.jarchivelib.ArchiveFormat;
import org.rauschig.jarchivelib.Archiver;
import org.rauschig.jarchivelib.ArchiverFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * This utility compresses a list of files to standard ZIP format file.
 * It is able to compress all sub files and sub directories, recursively.
 *
 * @author www.codejava.net
 */
public class ZipUtility
{
    /**
     * A constants for buffer size used to read/write data
     */
    private static final int BUFFER_SIZE = 4096;

    /**
     * Compresses a list of files to a destination zip file
     *
     * @param listFiles A collection of files and directories
     * @param destZipFile The path of the destination zip file
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void zip(List<File> listFiles, String destZipFile) throws FileNotFoundException,
        IOException
    {
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(destZipFile));
        for(File file : listFiles)
        {
            if(file.isDirectory())
            {
                zipDirectory(file, file.getName(), zos);
            }
            else
            {
                zipFile(file, zos);
            }
        }
        zos.flush();
        zos.close();
    }

    /**
     * Compresses files represented in an array of paths
     *
     * @param files a String array containing file paths
     * @param destZipFile The path of the destination zip file
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void zip(String[] files, String destZipFile) throws FileNotFoundException, IOException
    {
        List<File> listFiles = new ArrayList<File>();

        for(int i = 0; i < files.length; i++)
        {
            listFiles.add(new File(files[i]));
        }

        zip(listFiles, destZipFile);
    }

    /**
     * Adds a directory to the current zip output stream
     *
     * @param folder the directory to be  added
     * @param parentFolder the path of parent directory
     * @param zos the current zip output stream
     * @throws FileNotFoundException
     * @throws IOException
     */
    private void zipDirectory(File folder, String parentFolder, ZipOutputStream zos) throws
        FileNotFoundException, IOException
    {
        if(folder == null || folder.listFiles() == null)
        {
            return;
        }

        for(File file : folder.listFiles())
        {
            if(file.exists())
            {
                if(file.isDirectory())
                {
                    zipDirectory(file, parentFolder + "/" + file.getName(), zos);
                    continue;
                }

                zos.putNextEntry(new ZipEntry(parentFolder + "/" + file.getName()));
                BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
                long bytesRead = 0;
                byte[] bytesIn = new byte[BUFFER_SIZE];
                int read = 0;
                while((read = bis.read(bytesIn)) != -1)
                {
                    zos.write(bytesIn, 0, read);
                    bytesRead += read;
                }
                zos.closeEntry();
            }
        }
    }

    /**
     * Adds a file to the current zip output stream
     *
     * @param file the file to be added
     * @param zos the current zip output stream
     * @throws FileNotFoundException
     * @throws IOException
     */
    private void zipFile(File file, ZipOutputStream zos) throws FileNotFoundException, IOException
    {
        zos.putNextEntry(new ZipEntry(file.getName()));
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
        long bytesRead = 0;
        byte[] bytesIn = new byte[BUFFER_SIZE];
        int read = 0;
        while((read = bis.read(bytesIn)) != -1)
        {
            zos.write(bytesIn, 0, read);
            bytesRead += read;
        }
        zos.closeEntry();
    }

    /**
     * Unzips the specified file into the directory where it's located
     *
     * @param zipFile to unzip
     * @return path to where the file was unzipped.
     * @throws IOException if there are errors unzipping
     */
    public static Path unzip(Path zipFile) throws IOException
    {
        Path parent = zipFile.getParent();
        Archiver archiver = ArchiverFactory.createArchiver(ArchiveFormat.ZIP);
        archiver.extract(zipFile.toFile(), parent.toFile());
        return parent;
    }
}