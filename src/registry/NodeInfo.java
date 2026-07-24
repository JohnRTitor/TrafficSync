package registry;

public class NodeInfo {

    private int id;
    private String host;
    private int port;

    public NodeInfo(int id, String host, int port) {
        this.id = id;
        this.host = host;
        this.port = port;
    }

    public int getId() {
        return id;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return "NodeInfo{" +
                "id=" + id +
                ", host='" + host + '\'' +
                ", port=" + port +
                '}';
    }
}
