package me.ihaq.winbin;

import com.google.gson.GsonBuilder;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;
import me.ihaq.winbin.file.CustomFile;
import me.ihaq.winbin.file.files.ConfigFile;
import me.ihaq.winbin.util.WebUtils;
import org.apache.http.HttpStatus;
import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

public enum WinBin {
    INSTANCE;

    private final String NAME = getClass().getSimpleName();
    private final Image image = new ImageIcon(getClass().getResource("/pastebin.png")).getImage();
    private final File directory = new File(System.getenv("APPDATA") + "/" + NAME);
    private final CustomFile configFile = new ConfigFile(new GsonBuilder().setPrettyPrinting().create(), new File(directory, "config.json"));

    public String pasteBinKey;

    public void start() {
        registerKeyListener();
        createConfig();
        createPopupMenu();
    }

    private void registerKeyListener() {

        // setting GlobalScreen logger level to warning.
        Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.WARNING);
        logger.setUseParentHandlers(false);

        try {
            GlobalScreen.registerNativeHook();
        } catch (NativeHookException e) {
            e.printStackTrace();
            shutdown(1);
        }

        GlobalScreen.addNativeKeyListener(new NativeKeyListener() {

            // CTRL + SHIFT + V
            private int[] keys = {0, 0, 0};

            @Override
            public void nativeKeyPressed(NativeKeyEvent e) {
                setKeyStatus(e.getKeyCode(), 1);

                // checking if the proper key combo is pressed
                if (keys[0] == 1 && keys[1] == 1 && keys[2] == 1) {
                    try {
                        WebUtils.makeNewPaste(new Callback<String>() {

                            @Override
                            public void completed(HttpResponse<String> httpResponse) {

                                if (httpResponse.getStatus() == HttpStatus.SC_OK
                                        && httpResponse.getBody().contains("pastebin.com")) {

                                    String link = httpResponse.getBody();

                                    try {
                                        Desktop.getDesktop().browse(new URL(link).toURI());
                                    } catch (IOException | URISyntaxException e1) {
                                        e1.printStackTrace();
                                        showErrorWindow("An error occurred while opening the link. Here is the link: " + link);
                                    }

                                } else {
                                    showErrorWindow("Message: " + httpResponse.getBody());
                                }
                            }

                            @Override
                            public void failed(UnirestException e) {
                                showErrorWindow("An error occurred while connecting to Pastebin.");
                            }

                            @Override
                            public void cancelled() {

                            }

                        }, (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor));
                    } catch (UnsupportedFlavorException | IOException e1) {
                        e1.printStackTrace();
                        showErrorWindow("An error occurred while making you copied text into a Pastebin paste.");
                    }
                }
            }

            @Override
            public void nativeKeyReleased(NativeKeyEvent e) {
                setKeyStatus(e.getKeyCode(), 0);
            }

            private void setKeyStatus(int key, int status) {
                if (key == NativeKeyEvent.VC_CONTROL)
                    keys[0] = status;

                if (key == NativeKeyEvent.VC_SHIFT)
                    keys[1] = status;

                if (key == NativeKeyEvent.VC_V)
                    keys[2] = status;
            }

            @Override
            public void nativeKeyTyped(NativeKeyEvent e) {
            }
        });
    }

    // loading data from the config file
    private void createConfig() {

        if (!directory.exists())
            directory.mkdirs();

        configFile.makeDirectory();

        try {
            configFile.loadFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createPopupMenu() {

        //checking for support
        if (!SystemTray.isSupported()) {
            JOptionPane.showMessageDialog(null, "SystemTray is not supported on your platform, you will not be able to use this program.", "Winbin - Error", JOptionPane.ERROR_MESSAGE);
            shutdown(0);
        }

        // creating PopupMenu object
        PopupMenu trayPopupMenu = new PopupMenu();

        // exit button
        MenuItem close = new MenuItem("Exit");
        close.addActionListener(e -> {
            try {
                configFile.saveFile();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            shutdown(0);
        });
        trayPopupMenu.add(close);

        TrayIcon trayIcon = new TrayIcon(image, NAME, trayPopupMenu);
        trayIcon.setImageAutoSize(true);

        try {
            SystemTray.getSystemTray().add(trayIcon);
        } catch (AWTException awtException) {
            awtException.printStackTrace();
        }
    }

    private void shutdown(int code) {
        try {
            GlobalScreen.unregisterNativeHook();
        } catch (NativeHookException e) {
            e.printStackTrace();
        }
        System.exit(code);
    }

    private void showErrorWindow(String message) {
        JOptionPane.showMessageDialog(null, message, "Winbin - Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        WinBin.INSTANCE.start();
    }
}