package com.example.Inventory.Management.Entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Supplier name cannot be blank")
    private String name;

    @NotBlank(message = "Contact number is required")
    private String contactNumber;

    @Email(message = "Invalid email format")
    private String email;

    @ManyToMany(mappedBy = "suppliers")
    @Builder.Default
    private Set<Product> products = new HashSet<>();
}
