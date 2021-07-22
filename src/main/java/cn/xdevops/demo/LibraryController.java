package cn.xdevops.demo;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import java.security.Principal;

@Controller
public class LibraryController {

    private final BookRepository bookRepository;

    public LibraryController(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @GetMapping("/books")
    @RolesAllowed({"user", "admin"})
    public String getBooks(Model model, Principal principal) {
        model.addAttribute("books", bookRepository.readAll());
        model.addAttribute("name", principal.getName());
        return "books";
    }

    @GetMapping("/manager")
    @RolesAllowed("admin")
    public String manageBooks(Model model, HttpServletRequest request) {
        model.addAttribute("books", bookRepository.readAll());
        String userName = SecurityUtils.getIDToken(request).getGivenName();
        model.addAttribute("name", userName);
        return "manager";
    }
}
