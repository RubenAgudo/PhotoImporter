package org.ragudosa;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;

import org.apache.commons.cli.Options;
import org.apache.logging.log4j.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Date;

import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class PhotoImporter {
    private static final Logger logger = LogManager.getLogger(PhotoImporter.class);

    public static void main(String[] args) {
        try {
            new PhotoImporter().run(args);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void run(String[] args) throws IOException {

    	boolean result = parseParameters(args);
    	
        if(args.length < 3) {
            logger.error("Usage: \n\n" +
                    "java -jar PhotoImporter.jar sourceFolder destinationFolder deleteSourceFolder \n\n"  +
                    "Example: \n\n" +
                    "java -jar PhotoImporter.jar C:\\users\\yourUser\\Desktop D:\\Pictures 1");
            return;
        }

        String source = args[0];
        String destination = args[1];
        String deleteSource = args[2];

        int numberOfTotalFiles = new File(Paths.get(source).toString()).list().length;

        Files.walk(Paths.get(source)).forEach(filePath -> {
            if (Files.isRegularFile(filePath)) {
                File jpegFile = filePath.toFile();
                try {
                    Metadata metadata = ImageMetadataReader.readMetadata(jpegFile);

                    // obtain the Exif directory
                    ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);

                    // query the tag's value
                    Date date = directory.getDateOriginal();

                    if(date != null) {
                        copyFile(destination, filePath, jpegFile, date);
                    } else {
                        logger.warn("File " + jpegFile.getAbsolutePath() + " skipped, missing the date");
                    }

                } catch (ImageProcessingException | IOException e) {
                    e.printStackTrace();
                }

            }
        });

    }

    private boolean parseParameters(String[] args) {
		Options options = new Options();
		options.addOption("s", true, "Source folder from where to copy the photos");
		options.addOption("t", true, "Folder where to copy the photos");
		return false;
	}

	private void copyFile(String destination, Path filePath, File jpegFile, Date date) throws IOException {

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        String year = String.valueOf(calendar.get(Calendar.YEAR));
        String month = String.valueOf(calendar.get(Calendar.MONTH));
        String day = String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));

        Path path = Paths.get(destination, year, month, day);
        File destinationFolder = new File(path.toString());
        destinationFolder.mkdirs();

        path = Paths.get(destination, year, month, day, jpegFile.getName());

        Files.copy(filePath, path, REPLACE_EXISTING, COPY_ATTRIBUTES);
        logger.info("Moved " + filePath.toString() + " -> " + path.toString());
    }
}
