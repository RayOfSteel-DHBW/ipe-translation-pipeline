package com.translation.services;

import com.translation.Constants;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class DownloadService {
    private static final Logger logger = Logger.getLogger(DownloadService.class.getName());
    
    public void downloadToDirectory(String targetDirectory) throws Exception {
        logger.info("Starting download to directory: " + targetDirectory);
        
        File targetDir = new File(targetDirectory);
        if (!targetDir.exists()) {
            if (!targetDir.mkdirs()) {
                throw new Exception("Failed to create target directory: " + targetDirectory);
            }
        }
        
        List<String> pdfUrls = scrapeCoursePage(Constants.COURSE_URL);
        logger.info("Found " + pdfUrls.size() + " PDF links");
        
        for (int i = 0; i < pdfUrls.size(); i++) {
            String url = pdfUrls.get(i);
            String filename = getFilenameFromUrl(url);
            File outputFile = new File(targetDir, filename);
            
            logger.info(String.format("[%d/%d] Downloading %s", i + 1, pdfUrls.size(), filename));
            
            if (downloadFile(url, outputFile)) {
                logger.info("Successfully downloaded: " + filename);
            } else {
                logger.warning("Failed to download: " + filename);
            }
        }
        
        logger.info("Download completed. Files saved to: " + targetDir.getAbsolutePath());
    }
    
    private List<String> scrapeCoursePage(String courseUrl) throws Exception {
        logger.info("Scraping course page: " + courseUrl);
        
        Document doc = Jsoup.connect(courseUrl).get();
        Elements links = doc.select("a[href]");
        
        Set<String> uniqueUrls = new HashSet<>();
        
        for (Element link : links) {
            String href = link.attr("href");
            if (href.endsWith(".pdf") || href.contains(".pdf?")) {
                String absoluteUrl = link.attr("abs:href");
                uniqueUrls.add(absoluteUrl);
            }
        }
        
        return new ArrayList<>(uniqueUrls);
    }
    
    private boolean downloadFile(String urlString, File outputFile) {
        try {
            URL url = URI.create(urlString).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);
            
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                logger.warning("HTTP " + responseCode + " for URL: " + urlString);
                return false;
            }
            
            try (InputStream inputStream = connection.getInputStream();
                 FileOutputStream outputStream = new FileOutputStream(outputFile)) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
            
            return true;
            
        } catch (Exception e) {
            logger.warning("Download failed for " + urlString + ": " + e.getMessage());
            return false;
        }
    }
    
    private String getFilenameFromUrl(String urlString) {
        try {
            URL url = URI.create(urlString).toURL();
            String path = url.getPath();
            String filename = path.substring(path.lastIndexOf('/') + 1);
            
            if (filename.isEmpty() || !filename.endsWith(".pdf")) {
                filename = "document_" + Math.abs(urlString.hashCode()) + ".pdf";
            }
            
            return filename;
        } catch (Exception e) {
            return "document_" + Math.abs(urlString.hashCode()) + ".pdf";
        }
    }
    
    public boolean validateDownloadedFiles(String directory) {
        File dir = new File(directory);
        
        if (!dir.exists() || !dir.isDirectory()) {
            logger.warning("Download directory does not exist: " + directory);
            return false;
        }
        
        File[] files = dir.listFiles((dir1, name) -> name.toLowerCase().endsWith(".pdf"));
        
        if (files == null || files.length == 0) {
            logger.warning("No PDF files found in directory: " + directory);
            return false;
        }
        
        logger.info("Found " + files.length + " PDF files for processing");
        
        return true;
    }
}
