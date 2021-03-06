package server;

import customedexceptions.UnknownFileFormatException;
import utils.StrUtils;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.StringTokenizer;

public class Communication implements Runnable{

    private static final String WWW_PATH = "C://www";

    private Socket _socket;
    private PrintWriter _out;
    private BufferedReader _in;
    private String _command;
    private String _filePath;
    private String _version;

    private String _header;
    private String _contentType;
    private String _contentLength;
    private String _contentLocation;

    private String _body;
    private String _codeMessage;
    private String _code;

    public Communication(Socket socket)
    {
        if (socket == null)
        {
            throw new IllegalArgumentException("instance of Socket can not be null, Aliboron");
        }
        _socket = socket;
    }

    private void get(String filePath, String protocol)
    {
        // check protocol
        // ouvre le fichier s'il existe
        // ecrit le contenue du fichier dans le out
        // flush

        if (!protocol.startsWith("HTTP/1.1"))
        {
            buildStatus(400);
        }
        else
        {
            try
            {
                FileInputStream fileInputStream = new FileInputStream("C:\\www\\" + filePath);
                buildStatus(200);
                _out.write("\r\n");
                readFile(fileInputStream);
            }
            catch (FileNotFoundException e)
            {
                buildStatus(404);
            }
        }
        _out.write("\r\n\r\n");
        _out.flush();

    }



    public Communication(Socket socket){
        if (socket == null)
            throw new IllegalArgumentException("instance of Socket can not be null, Aliboron");
        _socket = socket;
    }

    private void decodeRequest(){
        try {

            if (!_version.equalsIgnoreCase("HTTP/1.1")) {
                // error
            } else if (_command.equalsIgnoreCase("GET")) {
                readGetRequest();
                decodeHeaderRequest();
                readFile(_filePath);
                _contentLength = Integer.toString(_body.length());
                _contentType = StrUtils.getContentType(_filePath);
            } else if (_command.equalsIgnoreCase("PUT")) {
                readPutRequest();
                System.out.println(_filePath);
                writeFile(_filePath);
                
            } else {
                // error
            }
        } catch (UnknownFileFormatException e) {

        }
    }

    private void decodeHeaderRequest(){
        StringTokenizer stringTokenizer = new StringTokenizer(_header,"\r\n");
        System.out.println(_header);
        while(stringTokenizer.hasMoreElements()){
            String element = (String) stringTokenizer.nextElement();
            if (element.startsWith("Content-type:")){
                StringTokenizer st = new StringTokenizer(element," ");
                st.nextElement();
                _contentType = (String) st.nextElement();
            }else if(element.startsWith("Content-length:")){
                StringTokenizer st = new StringTokenizer(element," ");
                st.nextElement();
                _contentLength = (String) st.nextElement();
            }else if(element.startsWith("Content-location:")){
                StringTokenizer st = new StringTokenizer(element," ");
                st.nextElement();
                _contentLocation = (String) st.nextElement();
            }
        }
    }

    /**
     * La méthode ReadFile permet la lecture d'un fichier
     * @param filePath
     */
    private void readFile(String filePath){

        try {
            FileInputStream fileInputStream = new FileInputStream(WWW_PATH + filePath);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fileInputStream));
            StringBuilder result = new StringBuilder();
            String line;
            try {
                while ((line = reader.readLine()) != null) {
                    result.append(line + "\r\n");
                }
                reader.close();
                result.append("\r\n");
            } catch (IOException e) {
                _body = null;
            }
            _body = result.toString();
        } catch (FileNotFoundException e) {
            _body = "Error 404";
            _code = "404";
            _codeMessage = "resource not found";
        }
    }

    private void writeFile(String filePath){
        try {
            File file = new File(WWW_PATH + filePath);
            file.createNewFile();
            FileOutputStream fileOutputStream = new FileOutputStream(file,false);

            fileOutputStream.write(_body.getBytes());
            fileOutputStream.close();
            _contentLocation = filePath;
            _code = "201";
            _codeMessage = "Created";

        } catch (FileNotFoundException e) {
            _body = "Error 404";
            _code = "404";
            _codeMessage = "resource not found";
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void buildStatus(int code)
    {
        if (code == 200)
        {
            _out.write("HTTP/1.1 200 OK\r\n");
        }
        else if (code == 400)
        {
            _out.write("HTTP/1.1 400 bad request\r\n");
        }
        else if (code == 404)
        {
            _out.write("HTTP/1.1 404 resource not found\r\n");
        }
    }

    private String interprate(String message)
    {
        StringTokenizer stringTokenizer = new StringTokenizer(message, " ");
        while (stringTokenizer.hasMoreElements())
        {
            String element = (String) stringTokenizer.nextElement();
            if (element.equalsIgnoreCase("GET"))
            {
                String filePath = (String) stringTokenizer.nextElement(); // check si prochiane ele existe
                String protocol = (String) stringTokenizer.nextElement(); // check si prochiane ele existe
                get(filePath, protocol);
            }
            else if (element.equalsIgnoreCase("PUT"))
            {

            }
            else
            {

            }
        }
    }

    public void sendAnwser(){
        if(_command.equalsIgnoreCase("GET")){
            _out.write(_version + " " + _code + " " + _codeMessage + "\r\n");
            _out.write("Content-Length: " + _contentLength + "\r\n");
            _out.write("Content-Type: " + _contentType + "\r\n");
            _out.write("\r\n");
            _out.write(_body + "\r\n");
            _out.write("\r\n");

        }else if(_command.equalsIgnoreCase("PUT")){
            _out.write(_version + " " + _code + " " + _codeMessage + "\r\n");
            _out.write("Content-location: " + _contentLocation + "\r\n");
            _out.write("\r\n");
        }else{

        }
        _out.flush();

    }

    private String readRequest(){

        String message = null;
        try {
            message = _in.readLine();
            StringTokenizer stringTokenizer = new StringTokenizer(message," ");
            while(stringTokenizer.hasMoreElements()) {
                String element = (String) stringTokenizer.nextElement();
                if (element.equalsIgnoreCase("GET") || element.equalsIgnoreCase("PUT")) {
                    _command = element;
                    _filePath = (String) stringTokenizer.nextElement();
                    _version = (String) stringTokenizer.nextElement();
                    decodeRequest();
                    sendAnwser();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    private void readGetRequest(){
        String line;
        try {

            StringBuilder header = new StringBuilder();
            do {
                line = _in.readLine();
                header.append(line + "\r\n");
            } while (!line.equalsIgnoreCase(""));
            _header = header.toString();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readPutRequest(){
    	System.out.println(_contentLength);
        String line;
        try {
            StringBuilder header = new StringBuilder();
            do {
                line = _in.readLine();
                header.append(line + "\r\n");
            } while (!line.equalsIgnoreCase(""));
            _header = header.toString();
            decodeHeaderRequest();

            int i = 0;
            char[] buffer = new char[1];
            StringBuilder body = new StringBuilder();
            do{
                if (_in.read(buffer,0,buffer.length) != -1) {
                	body.append(buffer);                   	
                }
                i++;
                
            }while(i < Integer.parseInt(_contentLength));
            _body = body.toString();
            System.out.println(_contentLength);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void init(){
        _command = null;
        _filePath = null;
        _version = null;

        _header = null;
        _contentType = null;
        _contentLength = null;
        _contentLocation = null;

        _body = null;
        _codeMessage = null;
        _code = null;
    }

    @Override
    public void run() {
        try {
            _out = new PrintWriter(_socket.getOutputStream());
            _in =  new BufferedReader(new InputStreamReader(_socket.getInputStream()));

            do {
                readRequest();
                init();
            }while(true);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
