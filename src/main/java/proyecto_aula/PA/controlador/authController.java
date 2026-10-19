package proyecto_aula.PA.controlador;
import org.springframework.beans.factory.annotation.Autowired;
import  org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;
import proyecto_aula.PA.modelo.Usuario;
import proyecto_aula.PA.repositorio.UsuarioRepository;

@Controller 
public class AuthController {
    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping("/")
    public String inicio(){
        return "redirect:/login";
    }
    @GetMapping("/login")
    public String mostrarLogin(){
        return "view/login";
    }
    @PostMapping("/login")
    public String login(@RequestParam String usuario, @RequestParam String password, HttpSession session, Model model)
    {
        Usuario u = usuarioRepository.findByUsername(usuario);
        if(u == null || !u.getPassword().equals(password)){
            model.addAttribute("error", "Usuario o contraseña incorrecta");
            return "view/login";
        }
        session.setAttribute("usuario", u.getNombre());
        return "redirect:/ocupacion";
    }
    @GetMapping("/logout")
    public String logout(HttpSession session){
        session.invalidate();
        return "redirect:/login";
    }
}
