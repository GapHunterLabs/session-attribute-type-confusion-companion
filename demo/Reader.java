import javax.servlet.http.HttpSession;

class Dog {}

class Reader {
    void read(HttpSession session) {
        Dog d = (Dog) session.getAttribute("profile");
    }
}
