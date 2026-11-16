package com.promoquoter.cart;

import com.promoquoter.idempotency.IdempotencyService;
import com.promoquoter.idempotency.IdempotentResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cart")
public class CartController {

    static final String IDEMPOTENCY_KEY = "Idempotency-Key";
    static final String IDEMPOTENT_REPLAYED = "Idempotent-Replayed";

    private final CartService service;
    private final IdempotencyService idempotency;

    public CartController(CartService service, IdempotencyService idempotency) {
        this.service = service;
        this.idempotency = idempotency;
    }

    @PostMapping("/quote")
    public QuoteResponse quote(@RequestBody @Valid CartRequest cart) {
        return service.quote(cart);
    }

    /**
     * Confirms the cart. With an {@code Idempotency-Key}, repeating the request returns the original
     * order instead of placing a new one; the {@code Idempotent-Replayed} header tells the two apart.
     */
    @PostMapping("/confirm")
    public ResponseEntity<ConfirmResponse> confirm(
            @RequestBody @Valid CartRequest cart,
            @RequestHeader(name = IDEMPOTENCY_KEY, required = false)
            @Pattern(regexp = "[\\x21-\\x7E]{1,255}",
                    message = "must be 1-255 visible ASCII characters without spaces") String idempotencyKey) {
        if (idempotencyKey == null) {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.confirm(cart));
        }
        IdempotentResult<ConfirmResponse> result = idempotency.execute(
                idempotencyKey, cart.fingerprint(), ConfirmResponse.class, () -> service.confirm(cart));
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(IDEMPOTENT_REPLAYED, String.valueOf(result.replayed()))
                .body(result.value());
    }
}
