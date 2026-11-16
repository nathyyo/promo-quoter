package com.promoquoter.promotion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/promotions")
public class PromotionController {

    static final int MAX_BATCH_SIZE = 100;

    private final PromotionService service;

    public PromotionController(PromotionService service) {
        this.service = service;
    }

    /**
     * Accepts a JSON array of promotions (types may be mixed), or a single promotion object.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<PromotionResponse> create(
            @RequestBody @NotEmpty @Size(max = MAX_BATCH_SIZE) List<@Valid CreatePromotionRequest> requests) {
        return service.create(requests);
    }

    @GetMapping
    public List<PromotionResponse> list() {
        return service.list();
    }
}
