package client;

import constants.Constants;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

import static java.lang.System.exit;

public class Client {

    static String nameOfFile;
    static long sizeOfFile;

    public static void main(String[] args) {

        DataInputStream read = null;
        DataOutputStream write = null;

        try {
            Socket socket = new Socket("localhost", 8000);
            read = new DataInputStream(socket.getInputStream());
            write = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            System.out.println("Client:19~22");
            exit(1);
        }

        Scanner in = new Scanner(System.in);

        String command;
        while (true) {
            System.out.println("please enter your command");
            command = in.nextLine();
            String response = "";
            try {
                write.writeUTF(command);
                write.flush();
                label:
                while (true) {
                    response = read.readUTF();
                    switch (response) {
                        case Constants.END_OF_PROCESS:
                            break label;
                        case Constants.NEED_OF_USER_INPUT:
                            write.writeUTF(in.nextLine());
                            write.flush();
                            break;
                        case Constants.UPLOAD_FILE:
                            uploadFile(in.nextLine(), write, read);
                            break;
                        case Constants.SEND_FILE:
                            sendFile(write, read);
                            break;
                        case Constants.DOWNLOAD_FILE:
                            getFile(read, write, in);
                            break;
                        default:
                            System.out.println(response);
                            break;
                    }

                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }

    private static void getFile(DataInputStream read, DataOutputStream write, Scanner in) throws IOException {

        String username = read.readUTF();
        String fileName = read.readUTF();

        File dir = new File("downloads/" + username);
        if (! dir.exists()) {
            dir.mkdirs();
        }
        File file = new File("downloads/" + username + '/' + fileName);

        if (file.exists()) {
            System.out.println("there is a file with this name. what to do ?\nReplace / Keep / Cancel ?");

            label :
            while(true) {
                switch (in.nextLine().toLowerCase()) {
                    case "replace":
                        file.delete();
                        break label;
                    case "keep":
                        do {
                            fileName = fileName.substring(0, fileName.lastIndexOf('.')) + "-Duplicate" + fileName.substring(fileName.lastIndexOf('.'));
                            file = new File("downloads/" + username + '/' + fileName);
                        } while (file.exists());
                        break label;
                    case "cancel":
                        System.out.println("canceled process");
                        write.writeBoolean(false);
                        write.flush();
                        return;
                    default:
                        System.out.println("bad input try again");
                        System.out.println("Replace / Keep / Cancel ?");
                }
            }
        }

        write.writeBoolean(trueن);
        write.flush();

        FileOutputStream fos = new FileOutputStream(file);
        byte[] bytes;

        while (true) {

            int size = read.readInt();

            if (size == 0) {
                break;
            }

            bytes = new byte[size];

            read.read(bytes, 0, size);

            fos.write(bytes);
            fos.flush();

            System.gc();
        }

        fos.close();

    }

    private static void sendFile(DataOutputStream write, DataInputStream read) throws IOException {

        File file = new File(nameOfFile);
        FileInputStream fis = new FileInputStream(file);
        int i = 0;
        while (sizeOfFile > 0) {

            if (sizeOfFile < Constants.BUFFER_SIZE) {

                write.writeInt((int) sizeOfFile);
                write.flush();

                byte[] data = new byte[(int) sizeOfFile];
                fis.read(data, 0, (int) sizeOfFile);

                sizeOfFile = 0;

                write.write(data);
                write.flush();
            } else {

                write.writeInt(Constants.BUFFER_SIZE);
                write.flush();

                byte[] data = new byte[Constants.BUFFER_SIZE];
                fis.read(data, 0, Constants.BUFFER_SIZE);

                sizeOfFile -= Constants.BUFFER_SIZE;

                write.write(data);
                write.flush();
            }

            System.gc();
        }

        write.writeInt(0);
        write.flush();

        System.out.println("transfer done.");
    }

    private static void uploadFile(String path, DataOutputStream write, DataInputStream read) throws IOException {
        File file = new File(path);
        if (!file.exists()) {
            write.writeUTF("no");
            write.flush();
        } else {
            write.writeUTF("yes");
            write.flush();

            sizeOfFile = file.length();

            String fileName = path.substring(path.lastIndexOf('/') + 1);
            write.writeUTF(fileName);
            write.flush();

            nameOfFile = fileName;
        }
    }

}
