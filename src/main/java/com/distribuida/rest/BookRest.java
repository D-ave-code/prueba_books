package com.distribuida.rest;

import com.distribuida.clientes.authors.AuthorRestProxy;
import com.distribuida.clientes.authors.AuthorsCliente;
import com.distribuida.db.Book;
import com.distribuida.dtos.BookDto;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
@Path("/books")

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)

public class BookRest {



    @PersistenceContext(unitName = "Microservicios")
    private EntityManager em;

    @RestClient
    @Inject AuthorRestProxy proxyAuthor;

    /**
     * GET          /books/{id}     buscar un libro por ID
     * GET          /books          listar todos
     * POST         /books          insertar
     * PUT/PATCH    /books/{id}     actualizar
     * DELETE       /books/{id}     eliminar
     */

    @GET
    @Path("/{id}")
    @Operation(summary = "Devuelve un libro buscando por ID",
            description = "Retorna un objeto libro")
    @APIResponse(description = "JSON con el libro solicitado",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Book.class)))
    @APIResponse(responseCode = "204", description = "Libro no encontrado")
    public Book findById(@PathParam("id") Integer id) {
        return em.find(Book.class, Integer.valueOf(id));
    }
    @GET
    @Operation(summary = "Devuelve un JSON con los libros en la DB",
            description = "Retorna todos los libros")
    @APIResponse(description = "Libros encontrados",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Book.class)))
    @APIResponse(responseCode = "204", description = "No existen libros")

    public List<Book> findAll() {
        return em.createNamedQuery("findAll", Book.class).getResultList();

    }
    @POST
    @Transactional
    @Operation(summary = "Agrega un libro",
            description = "Crea un objeto libro")
    @RequestBody(description = "Crea un nuevo libro",
            content = @Content(mediaType = "application/json",
    schema = @Schema(implementation = Book.class)))

    public void insert(Book book) {
        em.persist(book);
    }
    @PUT
    @Path("/{id}")
    @Transactional
    @Operation(summary = "Edita un libro",
            description = "Edita un objeto libro")
    @RequestBody(description = "Edita un nuevo libro",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Book.class)))
    public void update(Book book, @PathParam("id") Integer id) {
        Book bookS = em.find(Book.class, Integer.valueOf(id));
        bookS.setAuthor(book.getAuthor());
        bookS.setTitle(book.getTitle());
        bookS.setIsbn(book.getIsbn());
        bookS.setPrice(book.getPrice());
        em.persist(bookS);
    }
    @DELETE
    @Path("/{id}")
    @Transactional
    @Operation(summary = "Borra un libro",
            description = "Borra un objeto libro")
    @RequestBody(description = "Borra un nuevo libro",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Book.class)))
    public void delete( @PathParam("id") Integer id ) {
        Book book = em.find(Book.class, Integer.valueOf(id));
        em.remove(book);
    }
    @GET
    @Path("/all")
    @Operation(summary = "Devuelve un JSON con los libros en la DB y el nombre de sus authores",
            description = "Retorna todos los libros con sus authores")
    @APIResponse(description = "Libros encontrados",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = BookDto.class)))
    @APIResponse(responseCode = "204", description = "No existen libros")
    public List<BookDto> findAllCompleto() throws Exception {
        var books = em.createNamedQuery("findAll", Book.class).getResultList();

        List<BookDto> ret = books.stream()
                .map(s -> {
                    System.out.println("*********buscando " + s.getId() );

                    AuthorsCliente author = proxyAuthor.findById(s.getId().longValue());
                    return new BookDto(
                            s.getId(),
                            s.getIsbn(),
                            s.getTitle(),
                            s.getAuthor(),
                            s.getPrice(),
                            String.format("%s, %s", author.getLastName(), author.getFirtName())
                    );
                })
                .collect(Collectors.toList());

        return ret;
    }

}
