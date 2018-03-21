package org.wildfly.swarm.ts.hollow.jar.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;

import org.wildfly.swarm.ts.hollow.jar.web.jpa.Greeting;

@WebServlet(urlPatterns ="/jpa")
public class JPAServlet extends HttpServlet{

    @PersistenceContext (unitName = "primary")
    private EntityManager em;

    @Resource
    private UserTransaction tx;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            tx.begin();
            Greeting greeting = new Greeting();
            greeting.setText("Hello from JPAServlet");
            em.persist(greeting);
            em.flush();
            tx.commit();
        } catch (Exception e) {
            throw new ServletException(e);
        }

        PrintWriter w = resp.getWriter();
        List<Greeting> greetings = em.createNamedQuery("Greeting.findAll", Greeting.class).getResultList();
        greetings.forEach(w::println);
    }

}
