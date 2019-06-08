package server;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static java.lang.System.exit;

class Server {

    private ServerSocket serverSocket;
    public ArrayList<ClientHandler> clientHandlers;
    static ArrayList<User> users;


    Server() {

        try {
            serverSocket = new ServerSocket(8000);
        } catch (IOException e) {
            e.printStackTrace();
        }

        File mainDirectory = new File("Users");

        if (!mainDirectory.exists()) {
            System.out.println("creating main directory: " + mainDirectory.getName());
            boolean result = false;

            try {
                if(mainDirectory.mkdir()) {
                    result = true;
                }
            } catch (SecurityException se) {
                System.out.println("problem in creating folders.35 (security problem)");
                exit(1);
            }
            if (result) {
                System.out.println("DIR created");
            } else {
                System.out.println("problem in creating folders.40");
                exit(1);
            }
        } else {
            System.out.println("folder existed. deleting folder ...");

            deleteDirectory("Users");

            System.out.println("directory created");
        }

        clientHandlers = new ArrayList<>();
        users = new ArrayList<>();

        System.gc();
    }

    void startServer() {

        while (true) {

            try {
                clientHandlers.add(new ClientHandler(serverSocket.accept())); // starting thread in clienthandlers constructor
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }

    void deleteDirectory(String filePath) {
        File file = new File(filePath);
        for (String fileName: file.list()) {
            File f = new File(filePath + '/' + fileName);
            if (f.isFile()) {
                f.delete();
            } else {
                deleteDirectory(filePath + '/' + fileName);
                f.delete();
            }
        }
    }

}
