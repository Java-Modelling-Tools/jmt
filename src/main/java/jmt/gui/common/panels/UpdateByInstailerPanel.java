package jmt.gui.common.panels;
import org.w3c.dom.Document;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateByInstailerPanel {

    private static JFrame parentFrame;

    public UpdateByInstailerPanel(JFrame parentFrame) {
        this.parentFrame = parentFrame;
    }

    public static void checkForUpdates(String currentVersion) {
        String latestVersion = getLatestVersionFromSourceForge();
        boolean isUpdateAvailable = isUpdateAvailable(currentVersion, latestVersion);
        if (isUpdateAvailable) {
            promptUserToUpdate();
        } else {
            JOptionPane.showMessageDialog(parentFrame,
                    "Your software is up to date.",
                    "No Update Available",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }
    private static String getLatestVersionFromSourceForge() {
        String rssUrl = "https://sourceforge.net/projects/jmt/rss";
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(new InputSource(new URL(rssUrl).openStream()));
            doc.getDocumentElement().normalize();
            NodeList itemList = doc.getElementsByTagName("item");
            String latestVersion = "";

            for (int i = 0; i < itemList.getLength(); i++) {
                Element item = (Element) itemList.item(i);
                String titleText = item.getElementsByTagName("title").item(0).getTextContent();
                if (titleText.matches(".*\\d+\\.\\d+\\.\\d+.*")) {
                    latestVersion = titleText.replaceAll(".*?(\\d+\\.\\d+\\.\\d+).*", "$1");
                    break;
                }
            }
            return latestVersion;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static boolean isUpdateAvailable(String currentVersion, String latestVersion) {
        return compareVersions(currentVersion, latestVersion) < 0;
    }

    public static int compareVersions(String v1, String v2) {
        // Remove non-numeric characters (like 'BETA') for a basic numeric comparison
        v1 = v1.replaceAll("[^0-9.]", "");
        v2 = v2.replaceAll("[^0-9.]", "");

        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");

        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            int num1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int num2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
            if (num1 > num2) return 1; // v1 is newer
            if (num1 < num2) return -1; // v2 is newer
        }
        return 0;
    }

    private static void promptUserToUpdate() {
        int response = JOptionPane.showConfirmDialog(parentFrame,
                "An update is available. Would you like to download and install it now?",
                "Update Available",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE);

        if (response == JOptionPane.YES_OPTION) {
            downloadAndUpdate();
        }
    }

    private static void downloadAndUpdate() {
        // use thread to download the update without using lambda
        Thread downloadThread = new Thread(new Runnable() {
            @Override
            public void run() {
                downloadUpdate();
            }
        });
        downloadThread.start();
    }
    private static void downloadUpdate() {
        String downloadUrl = "https://sourceforge.net/projects/jmt/files/latest/download";
        String homeDir = System.getProperty("user.home");
        String saveDir = homeDir + File.separator + "downloads";
        File directory = new File(saveDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        String savePath = saveDir + File.separator + "update.jar";
        System.out.println(savePath);
        // Download the update
        try {
            URL url = new URL(downloadUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            int responseCode = connection.getResponseCode();
            // Check if the request was successful
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Read data from the connection
                InputStream input = new BufferedInputStream(connection.getInputStream());
                FileOutputStream output = new FileOutputStream(savePath);

                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
                output.close();
                input.close();
                connection.disconnect();

                int response = JOptionPane.showConfirmDialog(parentFrame,
                        "The update has been downloaded successfully. Would you like to install it now?",
                        "Download Complete",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.INFORMATION_MESSAGE);

                if (response == JOptionPane.YES_OPTION) {
                    // Run the downloaded file to complete the installation
                    Runtime.getRuntime().exec("cmd /c start " + savePath);
                    System.exit(0);
                }
            } else {
                JOptionPane.showMessageDialog(parentFrame,
                        "An error occurred while downloading the update. Please try again later.",
                        "Download Error",
                        JOptionPane.ERROR_MESSAGE);

            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(parentFrame,
                    "An error occurred while downloading the update. Please try again later.",
                    "Download Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
}