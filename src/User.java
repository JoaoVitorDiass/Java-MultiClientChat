import java.io.*;
import java.net.*;

public class User {

    private String _name;
    private Socket _connectionSocket;
    private BufferedReader _dataReceived;
    private DataOutputStream _dataSent;
    private boolean _isConnected;
    private boolean _isAccepted;
    private boolean _hasVotted;

    public User(String name, Socket connectionSocket, boolean isConnected) throws IOException {
        this._name = name;
        this._connectionSocket = connectionSocket;
        this._isConnected = isConnected;
        this._dataReceived = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));;
        this._dataSent = new DataOutputStream(connectionSocket.getOutputStream());;
        this._isAccepted = true;
    }

    public String getName() {
        return _name;
    }

    public void setName(String name) {
        this._name = name;
    }

    public Socket getConnectionSocket() {
        return _connectionSocket;
    }

    public void setConnectionSocket(Socket connectionSocket) {
        this._connectionSocket = connectionSocket;
    }

    public BufferedReader getDataReceived() {
        return _dataReceived;
    }

    public void setDataReceived(BufferedReader dataReceived) {
        this._dataReceived = dataReceived;
    }

    public DataOutputStream getDataSent() {
        return _dataSent;
    }

    public void setDataSent(DataOutputStream dataSent) {
        this._dataSent = dataSent;
    }

    public boolean isIsConnected() {
        return _isConnected;
    }

    public void setIsConnected(boolean isConnected) {
        this._isConnected = isConnected;
    }

    public boolean isAccepted() {
        return _isAccepted;
    }

    public void setIsAccepted(boolean isAccepted) {
        this._isAccepted = isAccepted;
    }

    public boolean isHasVotted() {
        return _hasVotted;
    }

    public void setHasVotted(boolean hasVotted) {
        this._hasVotted = hasVotted;
    }
}
