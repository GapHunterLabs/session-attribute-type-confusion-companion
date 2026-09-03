import javax.servlet.http.HttpSession;

class Cat {}

class Writer {
    void write(HttpSession session) {
        session.setAttribute("profile", new Cat());
    }
}
