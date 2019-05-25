package client;

import CustomedExceptions.UnknownFileFormatException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import utils.strUtils;

public class Client
{

    private static Socket OpenSocket(String adIP, int port)
    {
        Socket sc = null;
        try
        {
            sc = new Socket(InetAddress.getByName(adIP), port);
        }
        catch (UnknownHostException ex)
        {
            System.out.println("Erreur au niveau du port ");
            return sc;
        }
        catch (IOException ex)
        {
            System.out.println("Erreur au niveau de l'ouverture du Socket");
            return sc;
        }
        return sc;
    }

    /**
     *
     * @param ipServer
     * @param port
     * @param fileName
     * @param localFilePath
     * @return int :
     * 0 : Tout s'est bien passé.
     * -1 : Problème de flux
     * -2 : erreur de fermeture de socket
     * -3 : Erreur lors de l'ouverture du socket (Methode OpenSocket())
     * -4 : UnknownFileFormatException ex
     * -5 : Impossible de lire/ouvrir le fichier
     * -6 : Impossible d'écrire le fichier spécifié dans un flux
     */
    public static int PUT(String ipServer, int port, String fileName, String localFilePath)
    {
        byte[] fileInBytes = null;
        String fileInString = null;
        boolean isImg;
        try
        {
            isImg = strUtils.isImg(fileName);
        }
        catch (UnknownFileFormatException ex)
        {
            System.out.println("Exception : " + ex);
            return -4;
        }
        catch (Exception e)
        {
            // impossible d'ouvrir / lire le fichier
            System.out.println("Exception : Impossible de lire/ouvrir le fichier.\n" + e);
            return -5;
        }
        if (isImg)
        {
            try
            {
                File file = new File(localFilePath);
                fileInBytes = readFileToByteArray(file);
            }
            catch (Exception e)
            {
                // impossible d'ouvrir / lire le fichier
                System.out.println("Exception : Impossible de lire/ouvrir le fichier.\n" + e);
                return -5;
            }
        }
        else
        {
            try
            {
                fileInString = strUtils.readFileAsString(localFilePath);
            }
            catch (IOException ex)
            {
                // impossible d'ouvrir / lire le fichier
                System.out.println("Exception : Impossible de lire/ouvrir le fichier.\n" + ex);
                return -5;
            }
        }

        Socket sock = OpenSocket(ipServer, port);
        if (sock == null)
        {
            // -3 en cas d'erreur d'ouverture
            System.out.println("Erreur lors de l'ouverture du socket (Methode OpenSocket())");
            return -3;
        }
        try
        {
            OutputStream outputStream = sock.getOutputStream();
            // Envoyer la requête vers le serveur : "écrire sur le document 'doc.html'
            // Le serveur répond. Sa répojnse contient le document ou la raison du refus
            //outputStream.write()
            String url = "http://" + ipServer + ":" + port;
            String httpPUTRequest = "PUT /" + fileName + " HTTP/1.1 \r\n";
            httpPUTRequest += "Host: " + ipServer;
            try
            {
                httpPUTRequest += "Content-type: " + strUtils.getContentType(fileName) + " \r\n";
            }
            catch (UnknownFileFormatException ex)
            {
                // -4 si format de fichier non géré par l'application
                System.out.println("Exception : " + ex);
                sock.close();
                return -4;
            }
            if (isImg && fileInBytes != null)
            {
                httpPUTRequest += "Content-length: " + fileInBytes.length + "\r\n\n";
                for (int i = 0; i < fileInBytes.length; i++)
                {
                    httpPUTRequest += fileInBytes[i];
                }
            }
            else if (!isImg && fileInString != null)
            {
                httpPUTRequest += "Content-length: " + fileInString.length() + "\r\n\n";
                httpPUTRequest += fileInString;
            }
            else
            {
                System.out.println("Exception : Impossible d'écrire le fichier"
                        + "spécifié dans un flux.");
                sock.close();
                return -6;
            }

            // écriture et envoi
            System.out.println(httpPUTRequest);
            outputStream.write(httpPUTRequest.getBytes());
            outputStream.flush();
        }
        catch (IOException ex)
        {
            // -1 en cas de problème de flux
            System.out.println("Erreur lors d'utilisation d'un flux "
                    + "sortant.\nRapport d'exception : " + ex);
            try
            {
                sock.close();
            }
            catch (IOException exSock)
            {
                // -2 en cas d'erreur de fermeture de socket
                System.out.println("Erreur lors de la fermeture du Socket. "
                        + "\nIP serveur : " + ipServer + "\nPort serveur : "
                        + port + " \nRapport d'exception complet : " + exSock);
                return -2;
            }
            return -1;
        }
        // Fermeture de la connexion si tout s'est bien passé
        try
        {
            sock.close();
        }
        catch (IOException ex)
        {
            // -2 en cas d'erreur de fermeture de socket
            System.out.println("Erreur lors de la fermeture du Socket. "
                    + "\nIP serveur : " + ipServer + "\nPort serveur : "
                    + port + " \nRapport d'exception complet : " + ex);
            return -2;
        }

        // Le PUT s'est bien passé. Le fichier a été écrit sur le serveur.
        return 0;
    }

    /**
     * This method uses java.io.FileInputStream to read file content into a byte
     * array
     * https://netjs.blogspot.com/2015/11/how-to-convert-file-to-byte-array-java.html
     *
     * @param file
     * @return
     */
    private static byte[] readFileToByteArray(File file)
    {
        FileInputStream fis = null;
        // Creating a byte array using the length of the file
        // file.length returns long which is cast to int
        byte[] bArray = new byte[(int) file.length()];
        try
        {
            fis = new FileInputStream(file);
            fis.read(bArray);
            fis.close();
        }
        catch (IOException ioEx)
        {
            ioEx.printStackTrace();
            System.out.println(ioEx);
        }
        return bArray;
    }

}
