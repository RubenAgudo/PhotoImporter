package org.ragudosa;

import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;

public class PhotoImporter {

    private static final Logger logger = LogManager.getLogger(PhotoImporter.class);
    private String source;
    private String destination;

    public static void main(String[] args) {

        try {
            new PhotoImporter().run(args);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void run(String[] args) throws IOException {

        Options options = createParameters();

        // create the parser
        CommandLineParser parser = new DefaultParser();

        try {
            // parse the command line arguments
            CommandLine line = parser.parse(options, args);

            if (line.hasOption("help")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("photoimporter", options);
            }

            this.source = line.getOptionValue("source-folder");

            this.destination = line.getOptionValue("destination-folder");
            boolean deleteSourceFolder = line.hasOption("deleteSourceFolder");
            copyFiles(deleteSourceFolder);
            deleteEmptyFolders(deleteSourceFolder);

        } catch (ParseException exp) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("photoimporter", options);
        }

    }

    private void deleteEmptyFolders(Boolean deleteFolders) throws IOException {
        Files.walk(Paths.get(this.source)).forEach(filePath -> {
            if (Files.isDirectory(filePath, LinkOption.NOFOLLOW_LINKS)) {

                File directory = filePath.toFile();
                if (deleteFolders) {
                    if (directory.list().length == 0) {
                        directory.delete();
                    }
                }
            }
        });
    }

    private void copyFiles(Boolean deleteSourceFolder) throws IOException {

        Files.walk(Paths.get(this.source)).forEach(filePath -> {
            if (Files.isRegularFile(filePath)) {
                try {
                    File jpegFile = filePath.toFile();
                    processFile(filePath, jpegFile);

                    if (deleteSourceFolder) {
                        deleteFile(jpegFile);
                    }

                } catch (ImageProcessingException | IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void processFile(Path filePath, File jpegFile) throws ImageProcessingException, IOException {

        Metadata metadata = ImageMetadataReader.readMetadata(jpegFile);

        // obtain the Exif directory
        ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);

        if (directory == null) {
            logger.warn("File " + jpegFile.getAbsolutePath() + " skipped, no EXIF metadata found");
            return;
        }

        // query the tag's value
        Date date = directory.getDateOriginal();

        if (date != null) {
            copyFile(this.destination, filePath, jpegFile, date);

        } else {
            logger.warn("File " + jpegFile.getAbsolutePath() + " skipped, missing original date");
        }
    }

    private void deleteFile(File fileToDelete) throws IOException {
        if (!fileToDelete.isDirectory()) {
            fileToDelete.delete();
            logger.info("Deleted " + fileToDelete.getAbsolutePath());
        }
    }

    private Options createParameters() {
        Option help = new Option("help", "print this message");
        Option version = new Option("version", "print the version information and exit");
        Option deleteSourceFolder = new Option("deleteSourceFolder", "Deletes the source files");

        Option source = Option.builder("s").argName("folder").hasArg().desc("Source folder to process")
                .longOpt("source-folder").required().build();

        Option destination = Option.builder("d").argName("folder").hasArg()
                .desc("Where to copy the pictures specified in source").longOpt("destination-folder").required()
                .build();

        Options options = new Options();

        options.addOption(help);
        options.addOption(version);
        options.addOption(source);
        options.addOption(destination);
        options.addOption(deleteSourceFolder);

        return options;
    }

    private void copyFile(String destination, Path filePath, File jpegFile, Date date) throws IOException {

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        String year = String.valueOf(calendar.get(Calendar.YEAR));
        String month = String.format("%02d", calendar.get(Calendar.MONTH) + 1);
        String day = String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH));

        String newJpegName = new SimpleDateFormat("yyMMdd_hhmmss").format(date) + ".jpg";

        Path destinationPath = Paths.get(destination, year, month, day);
        File destinationFolder = new File(destinationPath.toString());
        destinationFolder.mkdirs();

        destinationPath = Paths.get(destination, year, month, day, newJpegName);

        Files.copy(filePath, destinationPath, REPLACE_EXISTING, COPY_ATTRIBUTES);
        logger.info(filePath.toString() + " -> " + destinationPath.toString());
    }
}
