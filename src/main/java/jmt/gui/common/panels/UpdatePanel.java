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
import java.nio.charset.StandardCharsets;

public class UpdatePanel {

    private static JFrame parentFrame;
    private static JDialog progressDialog;

    public UpdatePanel(JFrame parentFrame) {
        this.parentFrame = parentFrame;
    }

    public static void checkForUpdates(String currentVersion) {
        String latestVersion = getLatestVersionFromSourceForge();
        boolean isUpdateAvailable = isUpdateAvailable(currentVersion, latestVersion);
        if (isUpdateAvailable) {
            promptUserToUpdate(latestVersion,currentVersion);
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
                if (titleText.matches(".*\\d+\\.\\d+\\.\\d+(-BETA\\d+)?/.*")) {
                    latestVersion = titleText.replaceAll(".*/JMT-(\\d+\\.\\d+\\.\\d+(-BETA\\d+)?)/.*", "$1");
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
        String[] parts1 = v1.split("-BETA");
        String[] parts2 = v2.split("-BETA");

        String numeric1 = parts1[0];
        String numeric2 = parts2[0];

        String[] numParts1 = numeric1.split("\\.");
        String[] numParts2 = numeric2.split("\\.");

        int length = Math.max(numParts1.length, numParts2.length);
        for (int i = 0; i < length; i++) {
            int num1 = i < numParts1.length ? Integer.parseInt(numParts1[i]) : 0;
            int num2 = i < numParts2.length ? Integer.parseInt(numParts2[i]) : 0;
            if (num1 > num2) return 1; // v1 is newer
            if (num1 < num2) return -1; // v2 is newer
        }

        boolean isBeta1 = parts1.length > 1;
        boolean isBeta2 = parts2.length > 1;

        if (isBeta1 && !isBeta2) return -1; // v1 is beta, v2 is official version
        if (!isBeta1 && isBeta2) return 1;  // v1 is official version, v2 is beta
        if (isBeta1 && isBeta2) {
            String beta1 = parts1[1];
            String beta2 = parts2[1];
            return beta1.compareTo(beta2); // Compare beta tags
        }

        return 0; // Both versions are identical
    }

    private static void promptUserToUpdate(String latestVersion,String currentVersion) {
        Object[] options = {"Yes, update now", "No, remind me later", "Open download page"};
        int response = JOptionPane.showOptionDialog(parentFrame,
                "An update is available. Would you like to download and install it now?",
                "Update Available",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                options,
                options[0]);

        if (response == JOptionPane.YES_OPTION) {
            showProgressDialog();
            downloadAndUpdate(latestVersion,currentVersion);
        } else if (response == JOptionPane.CANCEL_OPTION) {
            openDownloadPage();
        }
    }

    private static void showProgressDialog() {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                JOptionPane optionPane = new JOptionPane("The update is being downloaded in the background, " +
                        "this may take several minutes. You can now close this window.",
                        JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION, null, new Object[]{}, null);
                progressDialog = optionPane.createDialog(parentFrame, "Download in Progress");
                progressDialog.setModal(false);
                progressDialog.setVisible(true);
                return null;
            }
        };
        worker.execute();
    }

    private static void closeProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dispose();
        }
    }

    private static void downloadAndUpdate(final String latestVersion,final String currentVersion) {
        Thread downloadThread = new Thread(new Runnable() {
            @Override
            public void run() {
                downloadUpdate(latestVersion,currentVersion);
            }
        });
        downloadThread.start();
    }
    private static void downloadUpdate(String latestVersion,String currentVersion) {
        try {
            String downloadUrl = "https://sourceforge.net/projects/jmt/files/jmt/JMT-" + latestVersion + "/JMT-singlejar-" + latestVersion + ".jar/download";
            String tempDir = System.getProperty("java.io.tmpdir");
            String tempDownloadPath = tempDir + File.separator + "JMT-new.jar";
            String oldFilePath = System.getProperty("user.dir") + File.separator + "JMT.jar";
            String oldFilePath2 = System.getProperty("user.dir") + File.separator + "JMT-singlejar-"+ currentVersion + ".jar";
            String newFilePath = System.getProperty("user.dir") + File.separator + "JMT-singlejar-" + latestVersion + ".jar";

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

                closeProgressDialog();

                int response = JOptionPane.showConfirmDialog(null,
                        "The update has been downloaded successfully. It will be installed now. You might need to restart JMT.",
                        "Download Complete",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.INFORMATION_MESSAGE);

                if (response == JOptionPane.YES_OPTION) {
                    if (isWindows()) {
                        executeWindowsUpdateScript(tempDownloadPath, oldFilePath, oldFilePath2,tempDir);
                    } else if (isLinuxOrMac()) {
                        executeLinuxUpdateScript(tempDownloadPath, oldFilePath, oldFilePath2,newFilePath,tempDir);
                    } else {
                        JOptionPane.showMessageDialog(null,
                                "Unsupported operating system.",
                                "Update Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                    System.exit(0);
                }
            } else {
                closeProgressDialog();
                JOptionPane.showMessageDialog(null,
                        "An error occurred while downloading the update. Please try again later.",
                        "Download Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            closeProgressDialog();
            JOptionPane.showMessageDialog(null,
                    "An error occurred while downloading the update. Please try again later.",
                    "Download Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private static void executeWindowsUpdateScript(String tempDownloadPath, String oldFilePath, String oldFilePath2,String tempDir) throws IOException {
        String batchScript = null;
        File nextFile = new File(oldFilePath);
        if (nextFile.exists()) {
            batchScript =
                    "move /Y \"" + tempDownloadPath + "\" \"" + oldFilePath + "\"\n"
                            + "start javaw -jar \"" + oldFilePath + "\"\n"
                            + "del \"%~f0\"";

        }
        else {
            batchScript =
                    "move /Y \"" + tempDownloadPath + "\" \"" + oldFilePath2 + "\"\n"
                            + "start javaw -jar \"" + oldFilePath2 + "\"\n"
                            + "del \"%~f0\"";
        }
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

    private static void executeLinuxUpdateScript(String tempDownloadPath, String oldFilePath, String oldFilePath2,String newFilePath, String tempDir) throws IOException {
        File nextFile = new File(oldFilePath);
        String bashScript = null;
        if (nextFile.exists()) {
            bashScript =
                    "#!/bin/bash\n"
                            + "echo \"Moving new JMT jar to the application directory\"\n"
                            + "mv \"" + tempDownloadPath + "\" \"" + oldFilePath + "\"\n"
                            + "echo \"Please restart the application\"\n"
                            + "echo \"Deleting the update script\"\n"
                            + "rm -- \"$0\"";

        }
        else {
            bashScript =
                    "#!/bin/bash\n"
                            + "echo \"Moving new JMT jar to the application directory\"\n"
                            + "mv \"" + tempDownloadPath + "\" \"" + newFilePath + "\"\n"
                            //delete old jar
                            + "rm -- \"" + oldFilePath2 + "\"\n"
                            + "echo \"Please restart the application\"\n"
                            + "echo \"Deleting the update script\"\n"
                            + "rm -- \"$0\"";
        }
        String bashFilePath = tempDir + File.separator + "update.sh";
        Files.write(Paths.get(bashFilePath), bashScript.getBytes(StandardCharsets.UTF_8));

        new File(bashFilePath).setExecutable(true);

        if (new File(bashFilePath).exists()) {
            System.out.println("Bash script file written successfully: " + bashFilePath);
        } else {
            System.out.println("Failed to write bash script file: " + bashFilePath);
        }

        try {
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", "pkexec bash \"" + bashFilePath + "\"");
            pb.inheritIO();
            Process process = pb.start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Failed to execute the update script.", "Update Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void openDownloadPage() {
        try {
            String url = "https://sourceforge.net/projects/jmt/files/latest/download";
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    private static boolean isLinuxOrMac() {
        return System.getProperty("os.name").toLowerCase().contains("nix") ||
                System.getProperty("os.name").toLowerCase().contains("nux") ||
                System.getProperty("os.name").toLowerCase().contains("mac");
    }
}
