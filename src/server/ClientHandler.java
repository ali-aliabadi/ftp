package server;

import constants.Constants;

import java.io.*;
import java.net.Socket;


class ClientHandler extends Thread {

    private Socket socket;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
    boolean isLogged = false;

    public ClientHandler(Socket socket) {

        this.socket = socket;

        try {
            dataInputStream = new DataInputStream(socket.getInputStream());
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        start();

    }

    @Override
    public void run() {
        bigWhile:
        while (true) {
            String command = "";
            try {
                command = dataInputStream.readUTF();
            } catch (IOException e) {
                System.out.println("clientHandler:38~39");
                if (socket.isClosed() && !socket.isConnected()) {
                    break;
                }
            }
            try {
                switch (command.toLowerCase()) {
                    case "login":
                        loginUser();
                        break;
                    case "signup":
                    case "sign up":
                        signUpUser();
                        break;
                    case "upload":
                        uploadFile();
                        break;
                    case "download":
                        downloadFile();
                        break;
                    case "logout":
                    case "log out":
                        logoutUser();
                        break;
                    case "end":
                        break bigWhile;
                    default:
                        badInput();
                }
            } catch (IOException e) {
                System.out.println("ClientHandler:47~70");
            }
        }
    }

    private void badInput() throws IOException {

        dataOutputStream.writeUTF("unknown command");
        dataOutputStream.flush();

        dataOutputStream.writeUTF(Constants.END_OF_PROCESS);
        dataOutputStream.flush();

    }

    private void logoutUser() throws IOException {

        user.setOnline(false);
        user = null;

        dataOutputStream.writeUTF("done");
        dataOutputStream.flush();

        dataOutputStream.writeUTF(Constants.END_OF_PROCESS);
        dataOutputStream.flush();

    }

    private void downloadFile() throws IOException {

        if (user == null) {
            dataOutputStream.writeUTF("please login/signup first");
            dataOutputStream.flush();

            dataOutputStream.writeUTF(Constants.END_OF_PROCESS);
            dataOutputStream.flush();
            return;
        }

        dataOutputStream.writeUTF("please input name of the file (complete name)");
        dataOutputStream.flush();

        dataOutputStream.writeUTF(Constants.NEED_OF_USER_INPUT);
        dataOutputStream.flush();

        String fn = dataInputStream.readUTF();
        File file = new File("Users/" + user.getUsername() + '/' + fn);

        if (file.exists()) {
            dataOutputStream.writeUTF(Constants.DOWNLOAD_FILE);
            dataOutputStream.flush();

            if (! sendFile(file)) {
                return;
            }
        } else {
            dataOutputStream.writeUTF("this file was not uploaded");
            dataOutputStream.flush();

            dataOutputStream.writeUTF(Constants.END_OF_PROCESS);
            dataOutputStream.flush();

            return;
        }

        dataOutputStream.writeUTF("file downloaded\ncheck in the folder with your name...");
        dataOutputStream.flush();

        dataOutputStream.writeUTF(Constants.END_OF_PROCESS);
        dataOutputStream.flush();
    }

    private boolean sendFile(File file) throws IOException {

        dataOutputStream.writeUTF(user.getUsername());
        dataOutputStream.flush();

        dataOutputStream.writeUTF(file.getName());
        dataOutputStream.flush();

        if (! dataInputStream.readBoolean()) {
            return false;
        }

        FileInputStream fis = new FileInputStream(file);

        long sizeOfFile = file.length();

        while (sizeOfFile > 0) {

            if (sizeOfFile < Constants.BUFFER_SIZE) {

                dataOutputStream.writeInt((int) sizeOfFile);
                dataOutputStream.flush();

                byte[] data = new byte[(int) sizeOfFile];
                fis.read(data, 0, (int) sizeOfFile);

                sizeOfFile = 0;

                dataOutputStream.write(data);
                dataOutputStream.flush();
            } else {

                dataOutputStream.writeInt(Constants.BUFFER_SIZE);
                dataOutputStream.flush();

                byte[] data = new byte[Constants.BUFFER_SIZE];
                fis.read(data, 0, Constants.BUFFER_SIZE);

                sizeOfFile -= Constants.BUFFER_SIZE;

                dataOutputStream.write(data);
                dataOutputStream.flush();
            }

            System.gc();
        }

        dataOutputStream.writeInt(0);
        dataOutputStream.flush();

        return true;
    }

    private void uploadFile() throws IOException {

        if (user == null) {
            dataOutputStream.writeUTF("please login/signup first");
            dataOutputStream.flush();

            dataOutputStream.writeUTF(Constants.END_OF_PROCESS);
            dataOutputStream.flush();
            return;
        }

        dataOutputStream.writeUTF("please input path of the file");
        dataOutputStream.flush();

        dataOutputStream.writeUTF(Constants.UPLOAD_FILE);
        dataOutputStream.flush();

        String isExisted = dataInputStream.readUTF();

        if (isExisted.equals("yes")) {
            String fileName = dataInputStream.readUTF();
            File file = new File("Users/" + user.getUsername() + '/' + fileName);
            if (file.exists()) {
                dataOutputStream.writeUTF("there is a file with this name. what to do ?\nReplace / Keep / Cancel ?");
                dataOutputStream.flush();

                dataOutputStream.writeUTF(Constants.NEED_OF_USER_INPUT);
                dataOutputStream.flush();

                label :
                while(true) {
                    switch (dataInputStream.readUTF().toLowerCase()) {
                        case "replace":
                            file.delete();
                            dataOutputStream.writeUTF((Constants.SEND_FILE));
                            dataOutputStream.flush();
                            getFile(fileName);
                            break label;
                        case "keep":
                            do {
                                fileName = fileName.substring(0, fileName.lastIndexOf('.')) + "-Duplicate" + fileName.substring(fileName.lastIndexOf('.'));
                                file = new File("Users/" + user.getUsername() + '/' + fileName);
                            } while (file.exists());
                            dataOutputStream.writeUTF((Constants.SEND_FILE));
                            dataOutputStream.flush();
                            getFile(fileName);
                            break label;
                        case "cancel":
                            dataOutputStream.writeUTF("canceled process");
                            dataOutputStream.flush();

                            dataOutputStream.writeUTF(Constants.END_OF_PROCESS);
                            dataOutputStream.flush();
                            break label;
                        default:
                            dataOutputStream.writeUTF("bad input, try again ...");
                            dataOutputStream.flush();

                            dataOutputStream.writeUTF("Replace / Keep / Cancel ?");
                            dataOutputStream.flush();

                            dataOutputStream.writeUTF(Constants.NEED_OF_USER_INPUT);
                            dataOutputStream.flush();
                    }
                }
            } else {
                dataOutputStream.writeUTF(Constants.SEND_FILE);
                dataOutputStream.flush();
                getFile(fileName);
            }
        } else {

            dataOutputStream.writeUTF("File not existed");
            dataOutputStream.flush();

            dataOutputStream.writeUTF(Constants.END_OF_PROCESS);
            dataOutputStream.flush();
        }
    }

    private void getFile(String fileName) throws IOException {

        File file = new File("Users/" + user.getUsername() + '/' + fileName);
        FileOutputStream fos = new FileOutputStream(file);
        byte[] bytes;

        while (true) {
            int size = dataInputStream.readInt();
            if (size == 0) {
                break;
            }

            bytes = new byte[size];

            dataInputStream.read(bytes, 0, size);

            fos.write(bytes);
            fos.flush();

            System.gc();
        }

        fos.close();

        dataOutputStream.writeUTF(Constants.END_OF_PROCESS);
        dataOutputStream.flush();
    }

    private void signUpUser() throws IOException {

        if (user != null) {
            dataOutputStream.writeUTF("please logout first");
            dataOutputStream.flush();

            dataOutputStream.writeUTF(Constants.END_OF_PROCESS);
            dataOutputStream.flush();
            return;
        }
        dataOutputStream.writeUTF("please input your username");
        dataOutputStream.flush();

        boolean flag;

        while (true) {
            dataOutputStream.writeUTF(Constants.NEED_OF_USER_INPUT);
            dataOutputStream.flush();
            flag = false;
            String username = dataInputStream.readUTF();
            for (User u : Server.users) {
                if (u.getUsername().equals(username)) {
                    dataOutputStream.writeUTF("already taken, try sth else...");
                    dataOutputStream.flush();
                    flag = true;
                    break;
                }
            }
            if (!flag) {
                dataOutputStream.writeUTF("please input your password");
                dataOutputStream.flush();
                while (true) {
                    dataOutputStream.writeUTF(Constants.NEED_OF_USER_INPUT);
                    dataOutputStream.flush();
                    String password = dataInputStream.readUTF();
                    if (valid(password)) {
                        user = new User(username, password);
                        Server.users.add(user);
                        new File("Users/" + username).mkdir();
                        dataOutputStream.writeUTF("signed up successfully");
                        dataOutputStream.flush();

                        dataOutputStream.writeUTF(Constants.END_OF_PROCESS);
                        dataOutputStream.flush();

                        return;
                    }
                    dataOutputStream.writeUTF("weak password, try again");
                    dataOutputStream.flush();
                }
            }
        }
    }

    private boolean valid(String password) {
        boolean flagN = false, flagC = false;
        if (password.length() < 6) {
            return false;
        }
        for (int i = 0; i < password.length(); i++) {
            if ((int) password.charAt(i) <= 57 && (int) password.charAt(i) >= 48) {
                flagN = true;
            }
            if ((int) password.charAt(i) <= 122 && (int) password.charAt(i) >= 97) {
                flagC = true;
            }
        }
        return flagC && flagN;
    }

    private void loginUser() throws IOException {

        if (user != null) {
            dataOutputStream.writeUTF("already entered");
            dataOutputStream.flush();

            dataOutputStream.writeUTF(Constants.END_OF_PROCESS);
            dataOutputStream.flush();

            return;
        }

        dataOutputStream.writeUTF("please input your username");
        dataOutputStream.flush();

        bigWhile:
        while (true) {
            dataOutputStream.writeUTF(Constants.NEED_OF_USER_INPUT);
            dataOutputStream.flush();
            String username = dataInputStream.readUTF();
            for (User u : Server.users) {
                if (u.getUsername().equals(username)) {
                    if (u.isOnline()) {
                        dataOutputStream.writeUTF("failed, user is online");
                        dataOutputStream.flush();

                        dataOutputStream.writeUTF(Constants.END_OF_PROCESS);
                        dataOutputStream.flush();

                        return;
                    }
                    dataOutputStream.writeUTF("please input your password");
                    dataOutputStream.flush();
                    while (true) {
                        dataOutputStream.writeUTF(Constants.NEED_OF_USER_INPUT);
                        dataOutputStream.flush();
                        String password = dataInputStream.readUTF();
                        if (u.getPassword().equals(password)) {
                            this.user = u;
                            u.setOnline(true);
                            break bigWhile;
                        }
                        dataOutputStream.writeUTF("bad input, try again...");
                        dataOutputStream.flush();
                    }
                }
            }
            dataOutputStream.writeUTF("no match found, try again...");
            dataOutputStream.flush();
        }

        dataOutputStream.writeUTF("you are in :)");
        dataOutputStream.flush();

        dataOutputStream.writeUTF(Constants.END_OF_PROCESS);
        dataOutputStream.flush();

    }
}
