package vn.edu.vuabongda.product.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vn.edu.vuabongda.product.dto.ProductRequestDTO;
import vn.edu.vuabongda.product.dto.ProductResponseDTO;
import vn.edu.vuabongda.product.service.ProductService;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // CREATE
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponseDTO create(
            @Valid @RequestBody ProductRequestDTO dto
    ) {
        return productService.create(dto);
    }

    // GET ALL
    @GetMapping
    public List<ProductResponseDTO> getAll() {
        return productService.getAll();
    }

    // GET BY ID
    @GetMapping("/{id}")
    public ProductResponseDTO getById(
            @PathVariable Long id
    ) {
        return productService.getById(id);
    }

    // UPDATE
    @PutMapping("/{id}")
    public ProductResponseDTO update(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequestDTO dto
    ) {
        return productService.update(id, dto);
    }

    // DELETE
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id
    ) {
        productService.delete(id);
    }
}