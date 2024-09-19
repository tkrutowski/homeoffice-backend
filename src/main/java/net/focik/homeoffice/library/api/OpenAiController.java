package net.focik.homeoffice.library.api;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.focik.homeoffice.library.api.dto.SeriesDto;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

//@AllArgsConstructor
@Log4j2
@RestController
@RequestMapping("/api/v1/library/ai")
public class OpenAiController {

    private final ChatModel chatModel;
    private final ChatClient chatClient;

    public OpenAiController(ChatModel chatModel, ChatClient.Builder builder) {
        this.chatModel = chatModel;
        this.chatClient = builder.build();
    }
    @GetMapping("/findbook6")
    public String getActorFilmsName(@RequestParam(value = "link", defaultValue = "https://lubimyczytac.pl/ksiazka/4461563/bunt") String link ) {
        String text = """
                znajdz na tej stronie {link}  link do strony gdzie znajdują się wszystkie książki z cyklu (serii).
                                
                Jeżeli książka należy do jakiegoścyklu (serii) to :
                -Sprawdz czy znalezionego linku czy na pewno prowadzi do strony ze wszystkimi książkami z cyklu (serii),

                Jeżeli cykl nie istnieje zwróć pusty obiekt
                """;
        PromptTemplate promptTemplate = new PromptTemplate(text);
        Prompt prompt = promptTemplate.create(Map.of("link", link));
        System.out.println(prompt.getContents());
        return chatModel.call(prompt).getResult().getOutput().getContent();
    }

    @GetMapping("/findbook5")
    public SeriesDto getActorFilmsByName(@RequestParam(value = "link", defaultValue = "https://lubimyczytac.pl/ksiazka/4461563/bunt") String link ) {
        String text = """
                znajdz na tej stronie {link}  link do strony gdzie znajdują się wszystkie książki z cyklu (serii).
                                
                Jeżeli książka należy do jakiegoścyklu (serii) to :
                -Sprawdz czy znalezionego linku czy na pewno prowadzi do strony ze wszystkimi książkami z cyklu (serii),

                Jeżeli cykl nie istnieje zwróć pusty obiekt
                """;
        PromptTemplate promptTemplate = new PromptTemplate(text);
        Message message = promptTemplate.createMessage(Map.of("link", link));
        return chatClient.prompt()
                .user(message.getContent())
                .call()
                .entity(SeriesDto.class);
    }

    //prompt
    @GetMapping("/findbook4")
    String getById4(@RequestParam(value = "genre", defaultValue = "tech") String genre) {
        String message = """
                 List 10 of the most popular YouTubers in {genre} along with their current subscriber counts.
                 If you don't know the answer, just say "I don't know".\s
                \s""";

        PromptTemplate promptTemplate = new PromptTemplate(message);
        Prompt prompt =  promptTemplate.create(Map.of("genre", genre));
        return chatModel.call(prompt).getResult().getOutput().getContent();
    }


    //prompt template
    @GetMapping("/findbook3")
    String getById3(@RequestParam(value = "link", defaultValue = "https://lubimyczytac.pl/ksiazka/4461563/bunt") String link) {
        Resource bookPromptResource = new ClassPathResource("prompts/newbook.st");
        String message = """
                znajdz na tej stronie {link}  link do strony gdzie znajdują się wszystkie strony z cyklu (serii).
                                
                Jeżeli książka należy do jakiegoścyklu (serii) to :
                -Sprawdz czy znalezionego linku czy na pewno prowadzi do strony ze wszystkimi książkami z cyklu (serii),
                - zwróć odpowiedz w postaci json:
                {
                        "title": ,
                        "description": ,
                        "url":
                  }.
                                
                Jeżeli cykl nie istnieje zwróć pusty json
                """;
//        PromptTemplate promptTemplate = new PromptTemplate(bookPromptResource);
        PromptTemplate promptTemplate = new PromptTemplate(message);
        Prompt prompt = promptTemplate.create(Map.of("link", link));
        return chatModel.call(prompt).getResult().getOutput().getContent();
    }

    //to samo
    @GetMapping("/findbook2")
//    @PreAuthorize("hasAnyAuthority('LIBRARY_READ_ALL','LIBRARY_READ')")
    String getById2(@RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {
//        Book book = findBookUseCase.findBook(id);
//        return new ResponseEntity<>(BookApiDto.builder().build(), HttpStatus.OK);
        return chatModel.call(new Prompt(message)).getResult().getOutput().getContent();
    }

    @GetMapping("/findbook")
//    @PreAuthorize("hasAnyAuthority('LIBRARY_READ_ALL','LIBRARY_READ')")
    String getById(@RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {
//        Book book = findBookUseCase.findBook(id);
//        return new ResponseEntity<>(BookApiDto.builder().build(), HttpStatus.OK);
        return chatModel.call(message);
    }
}

