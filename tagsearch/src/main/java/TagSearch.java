import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.iptc.IptcDirectory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TagSearch {
    public static void main(String[] args) {
        try {
            //String path = args[0];
            //String keyword = args[1];

            String path = "c:/beispielbilder";

            String keyword = "maus";

            try (Stream<Path> paths = Files.walk(Paths.get(path))) {
                List<File> keywords = paths
                        .filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".jpg"))
                        .map(Path::toFile)
                        .filter(file -> {
                            System.out.println(file);
                                    Metadata metadata = null;
                                    try {
                                        metadata = ImageMetadataReader.readMetadata(file);
                                    } catch (ImageProcessingException | IOException e) {
                                        e.printStackTrace();
                                    }
                                    return metadata.getDirectoriesOfType(IptcDirectory.class)
                                            .stream()
                                            .map(Directory::getTags)
                                            .flatMap(Collection::stream)
                                            .filter(tag -> tag.getTagName().equals("Keywords"))
                                            .anyMatch(tag -> tag.getDescription().toLowerCase().contains(keyword.toLowerCase()));

                                }
                        )
                        .collect(Collectors.toList());


                System.out.println(keywords);


            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
