public interface AuthService {
    void start();
    String getNickByLoginPass(String login, String pass);
    boolean reg(String login, String pass, String nick);
    void stop();
}
