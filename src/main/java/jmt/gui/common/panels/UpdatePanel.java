package jmt.gui.common.panels;

import org.w3c.dom.Document;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class UpdatePanel {

    private static JFrame parentFrame;

    public UpdatePanel(JFrame parentFrame) {
        this.parentFrame = parentFrame;
    }

    public static void checkForUpdates(String currentVersion) {
        String latestVersion = getLatestVersionFromSourceForge();
        boolean isUpdateAvailable = isUpdateAvailable(currentVersion, latestVersion);
        if (isUpdateAvailable) {
            promptUserToUpdate(latestVersion);
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
        // Remove non-numeric characters (like 'BETA') for a basic numeric comparisond
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

    private static void promptUserToUpdate(String latestVersion) {
        int response = JOptionPane.showConfirmDialog(parentFrame,
                "An update is available. Would you like to download and install it now?",
                "Update Available",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE);

        if (response == JOptionPane.YES_OPTION) {
            downloadAndUpdate(latestVersion);
        }
    }

    private static void downloadAndUpdate(final String latestVersion) {
        // use thread to download the update without using lambda
        Thread downloadThread = new Thread(new Runnable() {
            @Override
            public void run() {
                downloadUpdate(latestVersion);
            }
        });
        downloadThread.start();
    }
    private static void downloadUpdate(String latestVersion) {
        try {
            String downloadUrl = "https://sourceforge.net/projects/jmt/files/jmt/JMT-" + latestVersion + "/JMT-singlejar-" + latestVersion + ".jar/download";
            String tempDir = System.getProperty("java.io.tmpdir");
            String tempDownloadPath = tempDir + File.separator + "JMT-new.jar";
            String oldFilePath = System.getProperty("user.dir") + File.separator + "jmt.jar";

            URL url = new URL(downloadUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream input = new BufferedInputStream(connection.getInputStream());
                FileOutputStream output = new FileOutputStream(tempDownloadPath);
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
                output.close();
                input.close();
                connection.disconnect();

                int response = JOptionPane.showConfirmDialog(null,
                        "The update has been downloaded successfully. it will be installed now. the application will be restarted.",
                        "Download Complete",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.INFORMATION_MESSAGE);

                if (response == JOptionPane.YES_OPTION) {
                    String batchScript =
                            "move /Y \"" + tempDownloadPath + "\" \"" + oldFilePath + "\"\n"
                            + "start javaw -jar \"" + oldFilePath + "\"\n"
                            + "del \"%~f0\"";

                    String batchFilePath = tempDir + File.separator + "update.bat";
                    Files.write(Paths.get(batchFilePath), batchScript.getBytes(Charset.forName("GBK")));

                    // Create a VBScript to elevate the batch execution
                    String vbsPath = tempDir + File.separator + "elevate.vbs";
                    String vbsScript = "Set UAC = CreateObject(\"Shell.Application\")\n"
                            + "UAC.ShellExecute \"" + batchFilePath + "\", \"\", \"\", \"runas\", 1";
                    Files.write(Paths.get(vbsPath), vbsScript.getBytes(Charset.forName("GBK")));

                    Runtime.getRuntime().exec("wscript " + vbsPath);
                    System.exit(0);
                }
            } else {
                JOptionPane.showMessageDialog(null,
                        "An error occurred while downloading the update. Please try again later.",
                        "Download Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "An error occurred while downloading the update. Please try again later.",
                    "Download Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }




}
